from fastapi import APIRouter, HTTPException
from datetime import datetime
import hashlib
from typing import Literal, Optional, List

from pydantic import BaseModel

from models.dto import SuggestionRequest, SuggestionDetailResponse, SuggestionCreateResponse
from services.code_service import load_original_code_by_path
from services.gemini_service import generate_code_suggestion, GEMINI_MODEL
from services.mindmap_service import derive_map_id, generate_node_key
from services.arangodb_service import (
    db, insert_document, document_exists, get_document_by_key
)
from services.suggestion_service import (
    gather_files_by_label,
    gather_files_by_node_key,
    create_code_suggestion_node,       # 단일 파일용 (유지)
    resolve_scope_nodes_from_prompt,   # 기존 함수 (필터 미적용 시 아래에서 보정)
    upsert_code_suggestion_aggregate,  # 대표 1노드 저장
)

router = APIRouter()

# -------------------- auto (프롬프트로 라벨 자동 추론 · 집계 저장) --------------------
class SuggestionAutoRequest(BaseModel):
    repo_url: str
    prompt: str
    include_children: bool = True
    max_files: int = 12
    return_code: bool = False


class AutoScopeItem(BaseModel):
    scope_label: str
    scope_node_key: str
    file_path: str
    suggestion_key: Optional[str] = None
    node_key: Optional[str] = None
    status: str
    error: Optional[str] = None
    code: Optional[str] = None


class SuggestionAutoResponse(BaseModel):
    map_id: str
    prompt: str
    chosen_scopes: List[str]
    candidates: List[str]
    total_target_files: int
    created: int
    items: List[AutoScopeItem]
    aggregate_node_key: Optional[str] = None


@router.post(
    "/{map_id}/auto",
    response_model=SuggestionAutoResponse,
    summary="프롬프트만으로 스코프(라벨) 자동 추론 → 관련 파일들에 제안 일괄 생성 (집계 저장)"
)
def create_suggestions_auto(map_id: str, req: SuggestionAutoRequest):
    if req.repo_url and derive_map_id(req.repo_url) != map_id:
        raise HTTPException(400, "map_id and repo_url mismatch")

    scopes = resolve_scope_nodes_from_prompt(map_id, req.prompt, top_n=3)
    if not scopes:
        raise HTTPException(422, detail={"error": "scope_not_found", "message": "프롬프트에서 스코프(라벨)를 찾지 못했습니다."})

    # 1) 최상위 스코프
    top = scopes[0]
    chosen_label = top["label"]
    chosen_key = top["key"]
    candidates = [f"{s['label']}({s['score']})" for s in scopes]

    # 2) 만약 집계/제안/파일 노드가 선택되었다면 → 부모 스코프로 승격
    doc = get_document_by_key("mindmap_nodes", chosen_key) or {}
    node_type = (doc.get("node_type") or "").upper() if doc else ""
    if node_type in {"AGGREGATED_SUGGESTIONS"} or chosen_label.startswith("코드 추천"):
        parent = list(db.aql.execute("""
          FOR e IN mindmap_edges
            FILTER e.map_id == @map_id AND e._to == @toId
            LIMIT 1
            LET p = DOCUMENT(e._from)
            RETURN { key: SPLIT(e._from, "/")[1], label: p.label }
        """, bind_vars={"map_id": map_id, "toId": f"mindmap_nodes/{chosen_key}"}))
        if parent:
            chosen_key = parent[0]["key"]
            chosen_label = parent[0]["label"]

    # 3) 파일 대상 수집
    targets = gather_files_by_node_key(
        map_id=map_id,
        start_node_key=chosen_key,
        include_children=req.include_children,
        max_files=req.max_files
    )
    if not targets:
        return SuggestionAutoResponse(
            map_id=map_id,
            prompt=req.prompt,
            chosen_scopes=[chosen_label],
            candidates=candidates,
            total_target_files=0,
            created=0,
            items=[],
            aggregate_node_key=None,
        )

    # 4) 코드 생성 루프 (빈 코드면 skipped + 이유)
    items: List[AutoScopeItem] = []
    agg_items: List[dict] = []
    created = 0
    p8 = hashlib.md5(req.prompt.encode("utf-8")).hexdigest()[:8]

    for source_node_key, file_path in targets:
        original = load_original_code_by_path(req.repo_url, file_path)
        if not original:
            items.append(AutoScopeItem(
                scope_label=chosen_label,
                scope_node_key=source_node_key,
                file_path=file_path,
                status="skipped",
                error="Original code not found"
            ))
            continue

        ai_resp = generate_code_suggestion(file_path=file_path, original_code=original, prompt=req.prompt)
        code = (ai_resp.get("code") or "").strip()
        if not code:
            items.append(AutoScopeItem(
                scope_label=chosen_label,
                scope_node_key=source_node_key,
                file_path=file_path,
                status="skipped",
                error=ai_resp.get("error") or "No changes detected for this file (prompt mismatch or field not found).",
                code=None
            ))
            continue

        b = code.encode("utf-8")
        if len(b) > 80_000:
            code = b[:80_000].decode("utf-8", errors="ignore")

        suggestion_key = hashlib.md5(f"{req.repo_url}_{file_path}_{p8}".encode("utf-8")).hexdigest()[:12]

        if not document_exists("code_recommendations", suggestion_key):
            insert_document("code_recommendations", {
                "_key": suggestion_key,
                "map_id": map_id,
                "repo_url": req.repo_url,
                "file_path": file_path,
                "source_node_key": source_node_key,
                "prompt": req.prompt,
                "code": code,
                "summary": ai_resp.get("summary", ""),
                "rationale": ai_resp.get("rationale", ""),
                "ai_status": "success",
                "model": GEMINI_MODEL,
                "created_at": datetime.utcnow().isoformat() + "Z",
                "origin": "ai",
                "ai_generated": True
            })

        created += 1
        items.append(AutoScopeItem(
            scope_label=chosen_label,
            scope_node_key=source_node_key,
            file_path=file_path,
            suggestion_key=suggestion_key,
            node_key=None,
            status="created",
            code=code if req.return_code else None
        ))
        agg_item = {"file_path": file_path, "suggestion_key": suggestion_key, "status": "created"}
        if req.return_code:
            agg_item["code"] = code
        agg_items.append(agg_item)

    # 5) 대표 1노드로 집계 저장 (부모=선정된 스코프 노드)
    agg = upsert_code_suggestion_aggregate(
        map_id=map_id,
        parent_key=chosen_key,
        repo_url=req.repo_url,
        items=agg_items,
        label=f"코드 추천 · {chosen_label}",
        idempotency_key=None,
    )

    return SuggestionAutoResponse(
        map_id=map_id,
        prompt=req.prompt,
        chosen_scopes=[chosen_label],   # "코드 추천 · ..."가 아니라 실제 스코프명
        candidates=candidates,
        total_target_files=len(targets),
        created=created,
        items=items,
        aggregate_node_key=agg["node_key"],
    )

def resolve_source_node_key(map_id: str, repo_url: str, file_path: str) -> str:
    """file_path를 참조하는 노드가 있으면 그 키를, 없으면 파일 노드를 하나 만들어 반환"""
    hit = list(db.aql.execute(
        """
        FOR n IN mindmap_nodes
          FILTER n.map_id == @map_id
          FILTER ANY rf IN n.related_files
                 SATISFIES (IS_STRING(rf) AND rf == @fp) OR (IS_OBJECT(rf) AND rf.file_path == @fp)
          END
          LIMIT 1
          RETURN n
        """,
        bind_vars={"map_id": map_id, "fp": file_path}
    ))
    if hit:
        return hit[0]["_key"]

    # 파일 노드 생성
    file_name = file_path.split("/")[-1]
    label = f"[FILE] {file_name}"
    file_node_key = generate_node_key(map_id, label)
    if not document_exists("mindmap_nodes", file_node_key):
        insert_document("mindmap_nodes", {
            "_key": file_node_key,
            "map_id": map_id,
            "repo_url": repo_url,
            "mode": "FILE",
            "label": label,
            "node_type": "file",
            "related_files": [file_path],
            "origin": "human",
            "ai_generated": False
        })
    return file_node_key

@router.get(
    "/{suggestion_key}",
    response_model=SuggestionDetailResponse,
    summary="제안 상세 조회"
)
def get_suggestion(suggestion_key: str):
    doc = get_document_by_key("code_recommendations", suggestion_key)
    if not doc:
        raise HTTPException(404, "Suggestion not found")

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


# -------------------- by-label (집계 저장) --------------------
class SuggestionByLabelRequest(BaseModel):
    repo_url: str
    label: str
    prompt: str
    include_children: bool = True
    max_files: int = 12
    return_code: bool = False


class SuggestionByLabelItem(BaseModel):
    source_node_key: str
    file_path: str
    suggestion_key: Optional[str] = None
    node_key: Optional[str] = None
    status: str
    error: Optional[str] = None
    code: Optional[str] = None


class SuggestionByLabelResponse(BaseModel):
    map_id: str
    label: str
    total_target_files: int
    created: int
    items: List[SuggestionByLabelItem]
    aggregate_node_key: Optional[str] = None


@router.post(
    "/{map_id}/by-label",
    response_model=SuggestionByLabelResponse,
    summary="라벨(및 자식) 스코프로 관련 파일들에 제안 일괄 생성 (집계 저장)"
)
def create_suggestions_by_label(map_id: str, req: SuggestionByLabelRequest):
    if req.repo_url and derive_map_id(req.repo_url) != map_id:
        raise HTTPException(400, "map_id and repo_url mismatch")

    targets = gather_files_by_label(
        map_id=map_id,
        label_query=req.label,
        include_children=req.include_children,
        max_files=req.max_files
    )
    if not targets:
        return SuggestionByLabelResponse(
            map_id=map_id, label=req.label, total_target_files=0, created=0, items=[]
        )

    items: List[SuggestionByLabelItem] = []
    agg_items: List[dict] = []
    created = 0

    # 대표 부모 노드: 첫 번째 타깃의 source_node_key 사용
    parent_key = targets[0][0]
    p8 = hashlib.md5(req.prompt.encode("utf-8")).hexdigest()[:8]

    for source_node_key, file_path in targets:
        original = load_original_code_by_path(req.repo_url, file_path)
        if not original:
            items.append(SuggestionByLabelItem(
                source_node_key=source_node_key,
                file_path=file_path,
                status="skipped",
                error="Original code not found"
            ))
            continue

        ai_resp = generate_code_suggestion(file_path=file_path, original_code=original, prompt=req.prompt)
        code = (ai_resp.get("code") or "").strip()
        if not code:
            items.append(SuggestionByLabelItem(
                source_node_key=source_node_key,
                file_path=file_path,
                status="skipped",
                error=ai_resp.get("error") or "No changes detected for this file (prompt mismatch or field not found).",
                code=None
            ))
            continue

        # 큰 코드 제한 (선택)
        b = code.encode("utf-8")
        if len(b) > 80_000:
            code = b[:80_000].decode("utf-8", errors="ignore")

        suggestion_key = hashlib.md5(f"{req.repo_url}_{file_path}_{p8}".encode("utf-8")).hexdigest()[:12]

        if not document_exists("code_recommendations", suggestion_key):
            insert_document("code_recommendations", {
                "_key": suggestion_key,
                "map_id": map_id,
                "repo_url": req.repo_url,
                "file_path": file_path,
                "source_node_key": source_node_key,
                "prompt": req.prompt,
                "code": code,
                "summary": ai_resp.get("summary", ""),
                "rationale": ai_resp.get("rationale", ""),
                "ai_status": "success",
                "model": GEMINI_MODEL,
                "created_at": datetime.utcnow().isoformat() + "Z",
                "origin": "ai",
                "ai_generated": True
            })

        created += 1
        items.append(SuggestionByLabelItem(
            source_node_key=source_node_key,
            file_path=file_path,
            suggestion_key=suggestion_key,
            node_key=None,
            status="created",
            code=code if req.return_code else None
        ))
        agg_item = {"file_path": file_path, "suggestion_key": suggestion_key, "status": "created"}
        if req.return_code:
            agg_item["code"] = code
        agg_items.append(agg_item)

    agg = upsert_code_suggestion_aggregate(
        map_id=map_id,
        parent_key=parent_key,
        repo_url=req.repo_url,
        items=agg_items,
        label=f"코드 추천 · {req.label}",
        idempotency_key=None,
    )

    return SuggestionByLabelResponse(
        map_id=map_id,
        label=req.label,
        total_target_files=len(targets),
        created=created,
        items=items,
        aggregate_node_key=agg["node_key"],
    )
