# routers/suggestion_router.py
from fastapi import APIRouter, HTTPException
from datetime import datetime
import hashlib
from typing import Literal

from models.dto import (
    SuggestionRequest,
    SuggestionDetailResponse,
    SuggestionCreateResponse,
)
from services.code_service import load_original_code_by_path
from services.gemini_service import generate_code_suggestion, GEMINI_MODEL
from services.mindmap_service import generate_node_key
from services.arangodb_service import (
    insert_document,
    document_exists,
    get_document_by_key,
)

router = APIRouter()

@router.post(
    "",
    response_model=SuggestionCreateResponse,
    summary="기존 노드로부터 코드 제안 노드 생성",
    description="원본 코드는 수정하지 않고, 프롬프트 기반 제안 코드를 새 제안 노드(SUGG)로 생성합니다."
)
def create_suggestion(req: SuggestionRequest):
    """
    1) 원본 코드 로드
    2) Gemini로 제안 생성 (실패해도 기록 남김)
    3) code_recommendations 저장 (멱등)
    4) mindmap_nodes에 제안 노드(SUGG) 생성 (멱등)
    5) 기존 노드 -> 제안 노드 suggestion 엣지 생성 (멱등)
    """
    # 1) 원본 코드 로드
    original = load_original_code_by_path(req.repo_url, req.file_path)
    if not original:
        raise HTTPException(404, "Original code not found")

    # 2) AI 제안 생성
    ai_resp = generate_code_suggestion(
        file_path=req.file_path,
        original_code=original,
        prompt=req.prompt
    )
    # ai_resp 예: {"code": "...", "summary": "...", "rationale": "..."} or 실패시 {"code": "", "rationale": "JSON 파싱 오류: ...", "gemini_result": "..."}
    ai_status: Literal["success", "failed"] = "success" if ai_resp.get("code") else "failed"

    # 3) 멱등 키 계산
    p8 = hashlib.md5(req.prompt.encode("utf-8")).hexdigest()[:8]
    suggestion_key = hashlib.md5(
        f"{req.repo_url}_{req.file_path}_{p8}".encode("utf-8")
    ).hexdigest()[:12]

    file_name = req.file_path.split("/")[-1]
    label = f"[AI] {file_name} 개선안 #{suggestion_key}"
    sugg_node_key = generate_node_key(req.repo_url, "SUGG", label)
    edge_key = hashlib.md5(f"{req.source_node_key}->{sugg_node_key}".encode("utf-8")).hexdigest()[:12]

    # 4) code_recommendations 저장 (멱등)
    if not document_exists("code_recommendations", suggestion_key):
        insert_document("code_recommendations", {
            "_key": suggestion_key,
            "repo_url": req.repo_url,
            "file_path": req.file_path,
            "source_node_key": req.source_node_key,
            "prompt": req.prompt,
            "code": ai_resp.get("code", ""),
            "summary": ai_resp.get("summary", ""),
            "rationale": ai_resp.get("rationale", ""),
            "ai_status": ai_status,               # success | failed
            "model": GEMINI_MODEL,                # 사용 모델 기록
            "created_at": datetime.utcnow().isoformat() + "Z"
        })

    # 5) 제안 노드 저장 (mindmap_nodes, 멱등)
    if not document_exists("mindmap_nodes", sugg_node_key):
        insert_document("mindmap_nodes", {
            "_key": sugg_node_key,
            "repo_url": req.repo_url,
            "mode": "SUGG",
            "label": label,
            "node_type": "suggestion",            # 프론트 스타일 구분용
            "related_files": [req.file_path],
            "links": {"suggestion_key": suggestion_key}
        })

    # 6) 엣지 생성 (기존 -> 제안, edge_type='suggestion', 멱등)
    if not document_exists("mindmap_edges", edge_key):
        insert_document("mindmap_edges", {
            "_key": edge_key,
            "_from": f"mindmap_nodes/{req.source_node_key}",
            "_to": f"mindmap_nodes/{sugg_node_key}",
            "edge_type": "suggestion"
        })

    return SuggestionCreateResponse(
        node_key=sugg_node_key,
        suggestion_key=suggestion_key,
        label=label
    )


@router.get(
    "/{suggestion_key}",
    response_model=SuggestionDetailResponse,
    summary="제안 상세 조회",
    description="제안 코드/요약/근거 등의 상세 정보를 조회합니다."
)
def get_suggestion(suggestion_key: str):
    doc = get_document_by_key("code_recommendations", suggestion_key)
    if not doc:
        raise HTTPException(404, "Suggestion not found")
    return SuggestionDetailResponse(
        suggestion_key=doc["_key"],
        repo_url=doc["repo_url"],
        file_path=doc["file_path"],
        prompt=doc["prompt"],
        code=doc.get("code", ""),
        summary=doc.get("summary", ""),
        rationale=doc.get("rationale", ""),
        created_at=doc.get("created_at")
    )
