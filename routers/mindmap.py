from typing import Optional, Literal, List, Dict, Any

from fastapi import APIRouter, HTTPException, Query
from pydantic import BaseModel

from models.dto import AnalyzeRequest
from services.arangodb_service import get_documents_by_key_prefix, get_repo_file_content, db, get_repo_url_by_id, \
    delete_mindmap, get_prompt_doc, insert_prompt_doc, upsert_prompt_title, upsert_nodes_edges
from services.gemini_service import ai_expand_graph, ai_make_title
from services.mindmap_service import (
    save_mindmap_nodes_recursively,
    get_mindmap_graph,
    derive_map_id,
)
from services.suggestion_service import create_code_suggestion_node

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

class ExpandRequest(BaseModel):
    prompt: str
    mode: Optional[str] = "FEATURE"
    target_nodes: Optional[List[str]] = None
    related_files: Optional[List[str]] = None
    temperature: Optional[float] = 0.4
    idempotency_key: Optional[str] = None

class GraphResponse(BaseModel):
    mindmap_id: str
    prompt_id: str
    graph: Dict[str, List[Dict[str, Any]]]
    ui: Dict[str, Any] = {"highlight": {"ai": []}}
    saved: bool = True

class TitleRequest(BaseModel):
    prompt_id: Optional[str] = None
    max_len: Optional[int] = 48

class TitleResponse(BaseModel):
    mindmap_id: str
    prompt_id: str
    title: str
    summary: str

@router.post("/{map_id}/expand", summary="프롬프트 기반 마인드맵 확장(저장 병합 + 하이라이트 반환)", response_model=GraphResponse)
def expand_mindmap(map_id: str, req: ExpandRequest):
    from services.gemini_service import ai_expand_graph

    try:
        # 현재 그래프(프론트용) 로드 (필수는 아니지만 맥락 제공 가능)
        current = get_mindmap_graph(map_id)

        # LLM 호출 → 확장 결과
        ai_graph = ai_expand_graph(
            prompt=req.prompt,
            mode=req.mode or "FEATURE",
            current_graph=current,
            target_nodes=req.target_nodes or [],
            related_files=req.related_files or [],
            temperature=req.temperature or 0.4
        )
        # 저장(업서트) 및 변경된 노드키 수집
        changed_keys = upsert_nodes_edges(map_id, ai_graph["nodes"], ai_graph["edges"], default_mode=req.mode or "FEATURE")

        # 프롬프트 히스토리 기록
        prompt_id = insert_prompt_doc({
            "mindmap_id": map_id,
            "prompt": req.prompt,
            "mode": req.mode,
            "target_nodes": req.target_nodes,
            "related_files": req.related_files,
            "ai_summary": ai_graph.get("summary"),
            "status": "SUCCEEDED",
            "idempotency_key": req.idempotency_key
        })

        return GraphResponse(
            mindmap_id=map_id,
            prompt_id=prompt_id,
            graph={"nodes": ai_graph["nodes"], "edges": ai_graph["edges"]},
            ui={"highlight": {"ai": ai_graph.get("highlight_keys", changed_keys)}},
            saved=True
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/{map_id}/title", summary="마인드맵 + 프롬프트 요약으로 제목 생성", response_model=TitleResponse)
def make_title(map_id: str, req: TitleRequest):
    from services.gemini_service import ai_make_title

    try:
        graph = get_mindmap_graph(map_id)
        prompt_doc = get_prompt_doc(map_id, req.prompt_id)
        title, summary = ai_make_title(
            graph=graph,
            prompt=(prompt_doc or {}).get("prompt"),
            max_len=req.max_len or 48
        )
        pid = (prompt_doc or {}).get("_key") or insert_prompt_doc({
            "mindmap_id": map_id,
            "prompt": None,
            "mode": None,
            "ai_summary": summary,
            "status": "SUCCEEDED"
        })
        upsert_prompt_title(pid, title, summary)
        return TitleResponse(mindmap_id=map_id, prompt_id=pid, title=title, summary=summary)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

class PromptApplyRequest(BaseModel):
    prompt: str
    mode: Optional[str] = "FEATURE"
    target_nodes: Optional[List[str]] = None
    related_files: Optional[List[str]] = None
    temperature: Optional[float] = 0.4
    idempotency_key: Optional[str] = None
    # 코드추천 옵션
    suggest: bool = True
    max_suggestions: Optional[int] = 10   # 생성 상한
    title: bool = True
    title_max_len: Optional[int] = 48

class PromptApplyResponse(BaseModel):
    mindmap_id: str
    prompt_id: str
    added_node_keys: List[str]
    suggestions_created: int
    title: Optional[str] = None
    summary: Optional[str] = None

@router.post("/{map_id}/prompt-apply", response_model=PromptApplyResponse,
             summary="프롬프트 확장 + 코드추천 + 제목 생성까지")
def prompt_apply(map_id: str, req: PromptApplyRequest):
    try:
        # 1) 현재 그래프 로드 + 확장
        current = get_mindmap_graph(map_id)
        ai_graph = ai_expand_graph(
            prompt=req.prompt,
            mode=req.mode or "FEATURE",
            current_graph=current,
            target_nodes=req.target_nodes or [],
            related_files=req.related_files or [],
            temperature=req.temperature or 0.4
        )
        changed_keys = upsert_nodes_edges(map_id, ai_graph["nodes"], ai_graph["edges"], default_mode=req.mode or "FEATURE")

        # 프롬프트 히스토리
        prompt_id = insert_prompt_doc({
            "mindmap_id": map_id,
            "prompt": req.prompt,
            "mode": req.mode,
            "target_nodes": req.target_nodes,
            "related_files": req.related_files,
            "ai_summary": ai_graph.get("summary"),
            "status": "SUCCEEDED",
            "idempotency_key": req.idempotency_key
        })

        # 2) 코드추천 자동 생성 (신규/변경 노드들 기준)
        suggestions_created = 0
        if req.suggest:
            # 대상 파일 경로 후보: 이번에 추가된 노드들의 meta.files / related_files
            files_for_suggestion: List[tuple[str, str]] = []  # (source_node_key, file_path)
            node_by_key = {n.get("key"): n for n in ai_graph.get("nodes", [])}
            for k in (ai_graph.get("highlight_keys") or changed_keys):
                node = node_by_key.get(k) or {}
                rel_files = ((node.get("meta") or {}).get("files")) or []
                if rel_files:
                    files_for_suggestion.append((k, rel_files[0]))

            limit = req.max_suggestions or 10
            for source_key, file_path in files_for_suggestion[:limit]:
                created = create_code_suggestion_node(
                    map_id=map_id,
                    repo_url=current["nodes"][0].get("repo_url") if current.get("nodes") else None,  # 없으면 프론트에서 repo_url 전달해도 OK
                    file_path=file_path,
                    prompt=req.prompt,
                    source_node_key=source_key
                )
                if "error" not in created:
                    suggestions_created += 1

        # 3) 제목 생성
        title_text = summary_text = None
        if req.title:
            graph = get_mindmap_graph(map_id)  # 확장/추천 반영된 최신 그래프
            title_text, summary_text = ai_make_title(graph=graph, prompt=req.prompt, max_len=req.title_max_len or 48)
            upsert_prompt_title(prompt_id, title_text, summary_text)

        return PromptApplyResponse(
            mindmap_id=map_id,
            prompt_id=prompt_id,
            added_node_keys=list(set(ai_graph.get("highlight_keys", changed_keys))),
            suggestions_created=suggestions_created,
            title=title_text,
            summary=summary_text
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))