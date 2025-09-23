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
)
from services.gemini_service import ai_make_title
from services.mindmap_service import (
    save_mindmap_nodes_recursively,
    get_mindmap_graph,
    derive_map_id, _normalize_related_files
)
router = APIRouter()

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
            # 순차 저장 + 명시적 map_id
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
        # 순차 저장
        save_mindmap_nodes_recursively(req.repo_url, result, map_id=map_id, parallel=False)
        saved_dirs += 1

    return {
        "message": "최신 배치 새로고침 완료",
        "map_id": map_id,
        "batch_time": last_iso,
        "changed_files": len(changed),
        "dirs_analyzed": saved_dirs,
    }

# 맵 삭제 (노드/엣지/추천)
@router.delete("/{map_id}", summary="해당 맵의 mindmap_nodes/edges 삭제")
def drop_map(
    map_id: str,
    also_recommendations: bool = Query(
        True, description="제안(code_recommendations)도 함께 삭제"
    ),
):
    """
    mindmap_nodes / mindmap_edges 를 비우고,
    also_recommendations=True 면 code_recommendations 에서 해당 map_id 문서도 제거.
    """
    res = delete_mindmap(map_id, also_recommendations=also_recommendations)
    return {"message": "deleted", "map_id": map_id, **res}

# 제목 생성 (그래프+프롬프트 요약 기반)

class TitleRequest(BaseModel):
    prompt_id: Optional[str] = None
    max_len: Optional[int] = 48


class TitleResponse(BaseModel):
    mindmap_id: str
    prompt_id: str
    title: str
    summary: str

@router.post(
    "/{map_id}/title",
    summary="마인드맵 + 프롬프트 요약으로 제목 생성",
    response_model=TitleResponse,
)
def make_title(map_id: str, req: TitleRequest):
    """
    - 현재 그래프와 (있다면) 프롬프트 히스토리를 바탕으로
      간결한 제목과 요약을 생성해서 prompt_doc에 업서트.
    """
    try:
        graph = get_mindmap_graph(map_id)
        prompt_doc = get_prompt_doc(map_id, req.prompt_id)

        title, summary = ai_make_title(
            graph=graph,
            prompt=(prompt_doc or {}).get("prompt"),
            max_len=req.max_len or 48,
        )

        # prompt_id가 없으면 히스토리 문서를 하나 만들어 둠
        pid = (prompt_doc or {}).get("_key") or insert_prompt_doc(
            {
                "mindmap_id": map_id,
                "prompt": None,
                "mode": None,
                "ai_summary": summary,
                "status": "SUCCEEDED",
            }
        )

        upsert_prompt_title(pid, title, summary)

        return TitleResponse(
            mindmap_id=map_id,
            prompt_id=pid,
            title=title,
            summary=summary,
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

# ------------------------------
# 특정 노드 상세 조회 (이웃/엣지 포함)
# ------------------------------
class NodeDetailResponse(BaseModel):
    map_id: str
    node: Dict[str, Any]
    in_edges: List[Dict[str, Any]]
    out_edges: List[Dict[str, Any]]
    neighbors: Dict[str, List[Dict[str, Any]]]  # {"in": [...], "out": [...]}


@router.get(
    "/{map_id}/nodes/{node_key}",
    summary="특정 노드 상세(연결 엣지·이웃 포함) 반환",
    response_model=NodeDetailResponse,
)
def get_node_detail(
    map_id: str,
    node_key: str,
    normalize_files: bool = True,  # related_files를 file_path 객체로 정규화
):
    # 1) 노드 조회
    rows = list(
        db.aql.execute(
            """
            FOR n IN mindmap_nodes
              FILTER n.map_id == @map_id AND n._key == @key
              RETURN {
                key: n._key,
                label: n.label,
                related_files: n.related_files,
                node_type: n.node_type,
                repo_url: n.repo_url
              }
            """,
            bind_vars={"map_id": map_id, "key": node_key},
        )
    )
    if not rows:
        raise HTTPException(status_code=404, detail="node not found")

    node = rows[0]
    if normalize_files:
        node["related_files"] = _normalize_related_files(map_id, node.get("related_files"))

    # 2) 엣지(OUT)
    edges_out = list(
        db.aql.execute(
            """
            FOR e IN mindmap_edges
              FILTER e.map_id == @map_id AND e._from == CONCAT('mindmap_nodes/', @key)
              LET fromKey = LAST(SPLIT(e._from, '/'))
              LET toKey   = LAST(SPLIT(e._to, '/'))
              RETURN { from: fromKey, to: toKey, edge_type: e.edge_type }
            """,
            bind_vars={"map_id": map_id, "key": node_key},
        )
    )

    # 3) 엣지(IN)
    edges_in = list(
        db.aql.execute(
            """
            FOR e IN mindmap_edges
              FILTER e.map_id == @map_id AND e._to == CONCAT('mindmap_nodes/', @key)
              LET fromKey = LAST(SPLIT(e._from, '/'))
              LET toKey   = LAST(SPLIT(e._to, '/'))
              RETURN { from: fromKey, to: toKey, edge_type: e.edge_type }
            """,
            bind_vars={"map_id": map_id, "key": node_key},
        )
    )

    # 4) 이웃 노드들 조회
    out_neighbor_keys = [e["to"] for e in edges_out]
    in_neighbor_keys = [e["from"] for e in edges_in]
    all_neighbor_keys = list({*out_neighbor_keys, *in_neighbor_keys})

    neighbors_out: List[Dict[str, Any]] = []
    neighbors_in: List[Dict[str, Any]] = []

    if all_neighbor_keys:
        neighbor_rows = list(
            db.aql.execute(
                """
                FOR n IN mindmap_nodes
                  FILTER n.map_id == @map_id AND n._key IN @keys
                  RETURN {
                    key: n._key,
                    label: n.label,
                    related_files: n.related_files,
                    node_type: n.node_type,
                    repo_url: n.repo_url
                  }
                """,
                bind_vars={"map_id": map_id, "keys": all_neighbor_keys},
            )
        )
        # key -> row 매핑
        ndict = {n["key"]: n for n in neighbor_rows}

        # 방향에 따라 분류 + 정규화
        for k in out_neighbor_keys:
            if k in ndict:
                n = dict(ndict[k])
                if normalize_files:
                    n["related_files"] = _normalize_related_files(map_id, n.get("related_files"))
                neighbors_out.append(n)

        for k in in_neighbor_keys:
            if k in ndict:
                n = dict(ndict[k])
                if normalize_files:
                    n["related_files"] = _normalize_related_files(map_id, n.get("related_files"))
                neighbors_in.append(n)

    return {
        "map_id": map_id,
        "node": node,
        "in_edges": edges_in,
        "out_edges": edges_out,
        "neighbors": {"in": neighbors_in, "out": neighbors_out},
    }

