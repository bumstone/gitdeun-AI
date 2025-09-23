from typing import Optional, Literal, List, Dict, Any

from fastapi import APIRouter, HTTPException, Query
from pydantic import BaseModel

from models.dto import AnalyzeRequest
from services.arangodb_service import (
    get_documents_by_key_prefix,
    db,
    delete_mindmap,
    get_prompt_doc,
    insert_prompt_doc,
    upsert_prompt_title,
    upsert_nodes_edges,
)
from services.gemini_service import ai_expand_graph, ai_make_title
from services.mindmap_service import (
    save_mindmap_nodes_recursively,
    get_mindmap_graph,
    derive_map_id, find_root_node_key,
)
from services.suggestion_service import create_code_suggestion_node, upsert_code_suggestion_aggregate

router = APIRouter()


@router.post("/{map_id}/analyze", summary="마인드맵 수동 생성 (map 스코프)")
def analyze_code(map_id: str, req: AnalyzeRequest):
    parsed_result = {
        "node": "Answer",
        "children": [
            {"node": "delete", "children": []},
            {"node": "suspend", "children": []},
            {"node": "getDisplayContent", "children": []},
        ],
    }
    try:
        if req.repo_url and derive_map_id(req.repo_url) != map_id:
            raise HTTPException(status_code=400, detail="map_id and repo_url mismatch")

        # ✅ 순차 저장
        save_mindmap_nodes_recursively(req.repo_url or map_id, parsed_result, map_id=map_id, parallel=False)
        return {"message": "저장 완료", "map_id": map_id}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/analyze-ai", summary="Gemini로 마인드맵 생성 (빠른/간결, repo_url 기준)")
def analyze_ai_code(req: AnalyzeRequest):
    from services.gemini_service import summarize_directory_code

    try:
        repo_id = req.repo_url.rstrip("/").split("/")[-1]

        files = get_documents_by_key_prefix("code_analysis", f"{repo_id}_")
        if not files:
            return {"message": "code_analysis 비어있음(0개) — 먼저 적재하세요.", "repo_id": repo_id}

        grouped_files = {}
        for f in files:
            path = f.get("path", "unknown")
            dir_name = "/".join(path.split("/")[:-1]) or "root"
            grouped_files.setdefault(dir_name, []).append((path, f.get("content", "")))

        saved_count = 0
        for dir_name, blocks in grouped_files.items():
            result = summarize_directory_code(dir_name, blocks)
            if "error" in result:
                continue
            # ✅ 순차 저장 + 명시적 map_id
            save_mindmap_nodes_recursively(req.repo_url, result, map_id=repo_id, parallel=False)
            saved_count += 1

        return {"message": f"{saved_count}개 디렉터리 분석 완료", "repo_id": repo_id}

    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/{map_id}/graph", summary="특정 맵 그래프 반환")
def graph(map_id: str):
    try:
        data = get_mindmap_graph(map_id)
        return {
            "map_id": map_id,
            "count": len(data["nodes"]),
            "nodes": data["nodes"],
            "edges": data["edges"],
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


class RefreshLatestRequest(BaseModel):
    repo_url: str
    max_dirs: Optional[int] = None
    max_files_per_dir: Optional[int] = None


@router.post("/{map_id}/refresh-latest", summary="가장 최근 저장 배치만 빠르게 새로고침(짧은 응답)")
def refresh_latest(map_id: str, req: RefreshLatestRequest):
    if derive_map_id(req.repo_url) != map_id:
        raise HTTPException(400, "map_id and repo_url mismatch")

    try:
        from services.github_service import fetch_and_store_repo
        _ = fetch_and_store_repo(req.repo_url)
    except Exception as e:
        raise HTTPException(500, f"fetch failed: {e}")

    last_list = list(
        db.aql.execute(
            """
      FOR f IN repo_files
        FILTER f.repo_id == @repo_id
        SORT DATE_TIMESTAMP(f.fetched_at) DESC
        LIMIT 1
        RETURN f.fetched_at
    """,
            bind_vars={"repo_id": map_id},
        )
    )
    if not last_list:
        return {"message": "repo_files 비어있음 — 먼저 적재하세요.", "map_id": map_id}
    last_iso = last_list[0]

    changed = list(
        db.aql.execute(
            """
      FOR f IN repo_files
        FILTER f.repo_id == @repo_id
          AND f.fetched_at == @last_iso
        FILTER LIKE(f.path, '%.py') OR LIKE(f.path, '%.java') OR LIKE(f.path, '%.kt')
           OR LIKE(f.path, '%.js') OR LIKE(f.path, '%.ts') OR LIKE(f.path, '%.go')
           OR LIKE(f.path, '%.cpp') OR LIKE(f.path, '%.cs') OR LIKE(f.path, '%.rb')
        RETURN { path: f.path, content: f.content }
    """,
            bind_vars={"repo_id": map_id, "last_iso": last_iso},
        )
    )

    if not changed:
        return {
            "message": "가장 최근 배치에서 코드 파일 변경 없음",
            "map_id": map_id,
            "batch_time": last_iso,
            "changed_files": 0,
            "dirs_analyzed": 0,
        }

    grouped: dict[str, list[tuple[str, str]]] = {}
    order: list[str] = []
    limit_dirs = req.max_dirs if req.max_dirs is not None else float("inf")
    limit_files = req.max_files_per_dir if req.max_files_per_dir is not None else float("inf")

    for f in changed:
        path = f.get("path") or "unknown"
        dir_name = "/".join(path.split("/")[:-1]) or "root"
        if dir_name not in grouped:
            if len(order) >= limit_dirs:
                continue
            order.append(dir_name)
            grouped[dir_name] = []
        if len(grouped[dir_name]) < limit_files:
            grouped[dir_name].append((path, f.get("content") or ""))

    from services.gemini_service import summarize_directory_code

    saved_dirs = 0
    for dir_name in order:
        blocks = grouped.get(dir_name, [])
        if not blocks:
            continue
        result = summarize_directory_code(dir_name, blocks)
        if "error" in result:
            continue
        # ✅ 순차 저장
        save_mindmap_nodes_recursively(req.repo_url, result, map_id=map_id, parallel=False)
        saved_dirs += 1

    return {
        "message": "최신 배치 새로고침 완료",
        "map_id": map_id,
        "batch_time": last_iso,
        "changed_files": len(changed),
        "dirs_analyzed": saved_dirs,
    }


# 이하 expand/title/prompt-apply는 그대로 (저장 경로는 upsert_nodes_edges 사용)
