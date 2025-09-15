from typing import Optional, Literal

from fastapi import APIRouter, HTTPException, Query
from pydantic import BaseModel

from models.dto import AnalyzeRequest
from services.arangodb_service import get_documents_by_key_prefix, get_repo_file_content, db, get_repo_url_by_id, \
    delete_mindmap
from services.mindmap_service import (
    save_mindmap_nodes_recursively,
    get_mindmap_graph,
    derive_map_id,
)

router = APIRouter()

@router.post("/{map_id}/analyze", summary="마인드맵 수동 생성 (map 스코프)")
def analyze_code(map_id: str, req: AnalyzeRequest):
    # 데모용 트리
    parsed_result = {
        "node": "Answer",
        "children": [
            {"node": "delete", "children": []},
            {"node": "suspend", "children": []},
            {"node": "getDisplayContent", "children": []}
        ]
    }
    try:
        # repo_url이 왔다면 map_id와 일치 검증(옵션)
        if req.repo_url and derive_map_id(req.repo_url) != map_id:
            raise HTTPException(status_code=400, detail="map_id and repo_url mismatch")

        # repo_url은 기록용으로만 사용(없어도 됨)
        save_mindmap_nodes_recursively(req.repo_url or map_id, req.mode, parsed_result, map_id=map_id)
        return {"message": "저장 완료", "map_id": map_id}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))



@router.post("/analyze-ai", summary="Gemini로 마인드맵 생성 (빠른/간결, repo_url 기준)")
def analyze_ai_code(req: AnalyzeRequest):
    from services.gemini_service import summarize_directory_code

    try:
        # repo_id = 레포 마지막 세그먼트
        repo_id = req.repo_url.rstrip("/").split("/")[-1]

        # ★ code_analysis의 _key 프리픽스만 사용 → 매우 빠름
        files = get_documents_by_key_prefix("code_analysis", f"{repo_id}_")
        if not files:
            # 길게 안 돌리고 바로 짧게 리턴
            return {"message": "code_analysis 비어있음(0개) — 먼저 적재하세요.", "repo_id": repo_id}

        # 디렉터리 묶기 (응답엔 아무것도 안 넣음)
        grouped_files = {}
        for f in files:
            path = f.get("path", "unknown")
            dir_name = "/".join(path.split("/")[:-1]) or "root"
            grouped_files.setdefault(dir_name, []).append((path, f.get("content", "")))

        # 디렉터리별 요약 → 노드 저장 (짧은 결과만 반환)
        saved_count = 0
        for dir_name, blocks in grouped_files.items():
            result = summarize_directory_code(dir_name, blocks)
            if "error" in result:
                # 실패 디렉터리는 건너뜀 (응답을 길게 만들지 않음)
                continue
            save_mindmap_nodes_recursively(req.repo_url, req.mode, result)  # map_id는 내부에서 자동 도출
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
            "edges": data["edges"]
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))



class RefreshLatestRequest(BaseModel):
    repo_url: str                 # 최신화할 레포 URL (필수)
    mode: str = "DEV"
    max_dirs: Optional[int] = None           # 없으면 제한 없음
    max_files_per_dir: Optional[int] = None  # 없으면 제한 없음

@router.post("/{map_id}/refresh-latest", summary="가장 최근 저장 배치만 빠르게 새로고침(짧은 응답)")
def refresh_latest(map_id: str, req: RefreshLatestRequest):
    # 0) map_id ↔ repo_url 검증(헷갈림 방지)
    if derive_map_id(req.repo_url) != map_id:
        raise HTTPException(400, "map_id and repo_url mismatch")

    # 1) 최신 코드 fetch → repo_files upsert
    try:
        from services.github_service import fetch_and_store_repo
        _ = fetch_and_store_repo(req.repo_url)
    except Exception as e:
        raise HTTPException(500, f"fetch failed: {e}")

    # 2) repo_files에서 '가장 최근 저장 시각' 구하기
    last_list = list(db.aql.execute("""
      FOR f IN repo_files
        FILTER f.repo_id == @repo_id
        SORT DATE_TIMESTAMP(f.fetched_at) DESC
        LIMIT 1
        RETURN f.fetched_at
    """, bind_vars={"repo_id": map_id}))
    if not last_list:
        return {"message": "repo_files 비어있음 — 먼저 적재하세요.", "map_id": map_id}
    last_iso = last_list[0]

    # 3) 그 '가장 최근 시각'과 동일한 파일들만 선택 (같은 배치)
    changed = list(db.aql.execute("""
      FOR f IN repo_files
        FILTER f.repo_id == @repo_id
          AND f.fetched_at == @last_iso
        FILTER LIKE(f.path, '%.py') OR LIKE(f.path, '%.java') OR LIKE(f.path, '%.kt')
           OR LIKE(f.path, '%.js') OR LIKE(f.path, '%.ts') OR LIKE(f.path, '%.go')
           OR LIKE(f.path, '%.cpp') OR LIKE(f.path, '%.cs') OR LIKE(f.path, '%.rb')
        RETURN { path: f.path, content: f.content }
    """, bind_vars={"repo_id": map_id, "last_iso": last_iso}))

    if not changed:
        return {"message": "가장 최근 배치에서 코드 파일 변경 없음", "map_id": map_id, "batch_time": last_iso, "changed_files": 0, "dirs_analyzed": 0}

    # 4) 디렉터리로 묶기 + (선택적) 제한 적용
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

    # 5) 디렉터리별 간결 요약 → 저장 (짧은 응답)
    from services.gemini_service import summarize_directory_code
    saved_dirs = 0
    for dir_name in order:
        blocks = grouped.get(dir_name, [])
        if not blocks:
            continue
        result = summarize_directory_code(dir_name, blocks)  # LLM 한 번
        if "error" in result:
            continue
        save_mindmap_nodes_recursively(req.repo_url, req.mode, result, map_id=map_id)
        saved_dirs += 1

    return {
        "message": "최신 배치 새로고침 완료",
        "map_id": map_id,
        "batch_time": last_iso,
        "changed_files": len(changed),
        "dirs_analyzed": saved_dirs
    }

# --- (B) 마인드맵 삭제 ---
@router.delete("/{map_id}", summary="해당 맵의 mindmap_nodes/edges 삭제")
def drop_map(map_id: str, also_recommendations: bool = Query(True, description="제안(code_recommendations)도 함께 삭제")):
    res = delete_mindmap(map_id, also_recommendations=also_recommendations)
    return {"message": "deleted", "map_id": map_id, **res}


