# routers/suggestion.py
from fastapi import APIRouter, HTTPException
from datetime import datetime
import hashlib
from typing import Literal, Optional

from models.dto import SuggestionRequest, SuggestionDetailResponse, SuggestionCreateResponse
from services.code_service import load_original_code_by_path
from services.gemini_service import generate_code_suggestion, GEMINI_MODEL
from services.mindmap_service import derive_map_id, generate_node_key
from services.arangodb_service import (
    db, insert_document, document_exists, get_document_by_key
)

router = APIRouter()

def resolve_source_node_key(map_id: str, repo_url: str, file_path: str) -> str:
    """file_path를 참조하는 노드가 있으면 그 키를, 없으면 파일 노드를 하나 만들어 반환"""
    hit = list(db.aql.execute("""
      FOR n IN mindmap_nodes
        FILTER n.map_id == @map_id AND @fp IN n.related_files
        LIMIT 1
        RETURN n
    """, bind_vars={"map_id": map_id, "fp": file_path}))
    if hit:
        return hit[0]["_key"]

    # 파일 노드 생성
    file_name = file_path.split("/")[-1]
    label = f"[FILE] {file_name}"
    file_node_key = generate_node_key(map_id, "FILE", label)
    if not document_exists("mindmap_nodes", file_node_key):
        insert_document("mindmap_nodes", {
            "_key": file_node_key,
            "map_id": map_id,
            "repo_url": repo_url,
            "mode": "FILE",
            "label": label,
            "node_type": "file",
            "related_files": [file_path],
            # ✅ 명확히 사람이 만든 파일 노드로 표기
            "origin": "human",
            "ai_generated": False
        })
    return file_node_key

@router.post(
    "/{map_id}",
    response_model=SuggestionCreateResponse,
    summary="코드 제안 노드 생성 (map 스코프, 기존 노드 옆에 분기)"
)
def create_suggestion(map_id: str, req: SuggestionRequest):
    # map_id ↔ repo_url 일치(권장)
    if req.repo_url and derive_map_id(req.repo_url) != map_id:
        raise HTTPException(400, "map_id and repo_url mismatch")

    # 원본 코드 로드
    original = load_original_code_by_path(req.repo_url, req.file_path)
    if not original:
        raise HTTPException(404, "Original code not found")

    # source_node_key 자동/검증
    source_key: Optional[str] = req.source_node_key
    if not source_key:
        source_key = resolve_source_node_key(map_id, req.repo_url, req.file_path)
    else:
        src = get_document_by_key("mindmap_nodes", source_key)
        if not src:
            raise HTTPException(400, "source_node_key not found")
        if src.get("map_id") != map_id:
            raise HTTPException(400, "source_node_key belongs to a different map")

    # AI 제안 생성
    ai_resp = generate_code_suggestion(
        file_path=req.file_path,
        original_code=original,
        prompt=req.prompt
    )
    ai_status: Literal["success", "failed"] = "success" if ai_resp.get("code") else "failed"

    # 큰 코드 자르기(선택)
    code = ai_resp.get("code", "") or ""
    b = code.encode("utf-8")
    if len(b) > 80_000:
        code = b[:80_000].decode("utf-8", errors="ignore")

    # 멱등 키
    p8 = hashlib.md5(req.prompt.encode("utf-8")).hexdigest()[:8]
    suggestion_key = hashlib.md5(
        f"{req.repo_url}_{req.file_path}_{p8}".encode("utf-8")
    ).hexdigest()[:12]

    file_name = req.file_path.split("/")[-1]
    label = f"[AI] {file_name} 개선안 #{suggestion_key}"
    sugg_node_key = generate_node_key(map_id, "SUGG", label)
    edge_key = hashlib.md5(f"{source_key}->{sugg_node_key}".encode("utf-8")).hexdigest()[:12]

    # code_recommendations 저장(멱등) — ✅ 출처/플래그 저장
    if not document_exists("code_recommendations", suggestion_key):
        insert_document("code_recommendations", {
            "_key": suggestion_key,
            "map_id": map_id,
            "repo_url": req.repo_url,
            "file_path": req.file_path,
            "source_node_key": source_key,
            "prompt": req.prompt,
            "code": code,
            "summary": ai_resp.get("summary", ""),
            "rationale": ai_resp.get("rationale", ""),
            "ai_status": ai_status,
            "model": GEMINI_MODEL,
            "created_at": datetime.utcnow().isoformat() + "Z",
            # ✅ 구분 필드
            "origin": "ai",
            "ai_generated": True
        })

    # 제안 노드 저장(멱등) — ✅ 구분 필드 저장
    if not document_exists("mindmap_nodes", sugg_node_key):
        insert_document("mindmap_nodes", {
            "_key": sugg_node_key,
            "map_id": map_id,
            "repo_url": req.repo_url,
            "mode": "SUGG",
            "label": label,
            "node_type": "suggestion",
            "related_files": [req.file_path],
            "links": {"suggestion_key": suggestion_key},
            # ✅ 구분 필드
            "origin": "ai",
            "ai_generated": True
        })

    # 엣지 생성(멱등) — ✅ edge_type + 출처
    if not document_exists("mindmap_edges", edge_key):
        insert_document("mindmap_edges", {
            "_key": edge_key,
            "map_id": map_id,
            "_from": f"mindmap_nodes/{source_key}",
            "_to": f"mindmap_nodes/{sugg_node_key}",
            "edge_type": "suggestion",
            "origin": "ai"
        })

    return SuggestionCreateResponse(
        node_key=sugg_node_key,
        suggestion_key=suggestion_key,
        label=label,
        node_type="suggestion",
        origin="ai",
        ai_generated=True
    )

@router.get(
    "/{suggestion_key}",
    response_model=SuggestionDetailResponse,
    summary="제안 상세 조회"
)
def get_suggestion(suggestion_key: str):
    doc = get_document_by_key("code_recommendations", suggestion_key)
    if not doc:
        raise HTTPException(404, "Suggestion not found")

    # ✅ 과거 레코드(구분 필드 없음)도 안전하게 기본값 처리
    origin = doc.get("origin") or ("ai" if doc.get("model") else "human")
    ai_generated = bool(doc.get("ai_generated", origin == "ai"))

    return SuggestionDetailResponse(
        suggestion_key=doc["_key"],
        repo_url=doc["repo_url"],
        file_path=doc["file_path"],
        prompt=doc["prompt"],
        code=doc.get("code", ""),
        summary=doc.get("summary", ""),
        rationale=doc.get("rationale", ""),
        created_at=doc.get("created_at"),
        origin=origin,
        ai_generated=ai_generated,
        model=doc.get("model")
    )
