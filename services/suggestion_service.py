# services/suggestion_service.py
from __future__ import annotations

import hashlib
import re
from datetime import datetime

from services.code_service import load_original_code_by_path
from services.gemini_service import generate_code_suggestion, GEMINI_MODEL
from services.mindmap_service import generate_node_key, derive_map_id
from services.arangodb_service import (
    insert_document,
    document_exists,
)
from typing import List, Tuple, Dict, Optional, Literal, Any
from services.arangodb_service import db


# 지원 확장자
CODE_EXTS = (".java", ".kt", ".py", ".ts", ".js", ".go", ".rb", ".cs", ".cpp")


# ------------------------------------------------------------
# (A) 파일 경로/이름 보조
# ------------------------------------------------------------
def _extract_filename_from_prompt(prompt: str, SUPPORTED_EXT=None) -> Optional[str]:
    # 기본값 보정
    if SUPPORTED_EXT is None:
        SUPPORTED_EXT = CODE_EXTS
    pat = r'([A-Za-z0-9_\-\/\.]+(?:' + '|'.join(map(re.escape, SUPPORTED_EXT)) + r'))'
    m = re.search(pat, prompt)
    if m:
        return m.group(1).split("/")[-1]
    return None


def _find_paths_by_filename(repo_id: str, filename: str) -> List[str]:
    paths = list(db.aql.execute("""
      FOR f IN repo_files
        FILTER f.repo_id == @repo_id
          AND (f.path LIKE CONCAT('%/', @fn) OR f.path == @fn OR f.path LIKE CONCAT('%', @fn))
      RETURN f.path
    """, bind_vars={"repo_id": repo_id, "fn": filename}))
    def score(p: str) -> tuple:
        main_boost = -10 if "src/main" in p else 0
        test_penalty = +10 if "/test/" in p or "src/test" in p else 0
        return (main_boost + test_penalty, len(p))
    return sorted(set(paths), key=score)


def _auto_choose_path(candidates: List[str]) -> Optional[str]:
    if not candidates:
        return None
    for p in candidates:
        if "src/main" in p and "/test/" not in p:
            return p
    for p in candidates:
        if "src/main" in p:
            return p
    for p in candidates:
        if "/test/" not in p:
            return p
    return candidates[0]


def _resolve_to_full_path(repo_id: str, name_or_path: str) -> Optional[str]:
    if not name_or_path:
        return None
    if "/" in name_or_path:
        return name_or_path
    rows = list(db.aql.execute("""
      FOR f IN repo_files
        FILTER f.repo_id == @repo_id
          AND (f.path LIKE CONCAT('%/', @fn)
               OR f.path == @fn
               OR f.path LIKE CONCAT('%', @fn))
      RETURN f.path
    """, bind_vars={"repo_id": repo_id, "fn": name_or_path}))
    if not rows:
        return None
    def score(p: str):
        return (-10 if "src/main" in p else 0) + (10 if "/test/" in p or "src/test" in p else 0), len(p)
    return sorted(set(rows), key=score)[0]


def resolve_file_path_auto(repo_url: str, prompt: str, source_node_key: Optional[str] = None) -> Tuple[Optional[str], List[str], str]:
    repo_id = derive_map_id(repo_url)
    # source_node_key가 있으면 그 노드의 파일을 우선 사용
    if source_node_key:
        hit = list(db.aql.execute("""
          FOR n IN mindmap_nodes
            FILTER n._key == @k
            LIMIT 1
            RETURN n
        """, bind_vars={"k": source_node_key}))
        if hit:
            files = hit[0].get("related_files") or []
            if files:
                full = _resolve_to_full_path(repo_id, files[0]) or files[0]
                return full, files, "source_node_related_file"
    # 프롬프트에서 파일명 추출
    filename = _extract_filename_from_prompt(prompt)
    if filename:
        paths = _find_paths_by_filename(repo_id, filename)
        if not paths:
            return None, [], "prompt_filename_not_found"
        chosen = _auto_choose_path(paths)
        return chosen, paths, "prompt_filename_auto_chosen"
    return None, [], "no_filename"


# ------------------------------------------------------------
# (B) 단건 제안 생성(공통)
# ------------------------------------------------------------
def create_code_suggestion_node(
    map_id: str,
    repo_url: str,
    file_path: Optional[str],
    prompt: str,
    source_node_key: Optional[str] = None,
    return_code: bool = False,
    prefer_main_when_ambiguous: bool = True,
) -> dict:
    # file_path 자동결정
    if not file_path:
        chosen, candidates, reason = resolve_file_path_auto(repo_url, prompt, source_node_key)
        if not chosen:
            return {"error": "file_path_unresolved", "reason": reason, "candidates": candidates[:10]}
        file_path = chosen

    original = load_original_code_by_path(repo_url, file_path)
    if not original:
        return {"error": "Original code not found", "file_path": file_path}

    ai_resp = generate_code_suggestion(file_path=file_path, original_code=original, prompt=prompt)
    ai_status: Literal["success", "failed"] = "success" if (ai_resp.get("code") or "").strip() else "failed"

    code_text = (ai_resp.get("code") or "")
    b = code_text.encode("utf-8")
    if len(b) > 80_000:
        code_text = b[:80_000].decode("utf-8", errors="ignore")

    p8 = hashlib.md5(prompt.encode("utf-8")).hexdigest()[:8]
    suggestion_key = hashlib.md5(f"{repo_url}_{file_path}_{p8}".encode("utf-8")).hexdigest()[:12]

    file_name = file_path.split("/")[-1]
    label = f"[AI] {file_name} 개선안 #{suggestion_key}"
    sugg_node_key = generate_node_key(map_id, "SUGG", label)

    if not document_exists("code_recommendations", suggestion_key):
        insert_document("code_recommendations", {
            "_key": suggestion_key,
            "map_id": map_id,
            "repo_url": repo_url,
            "file_path": file_path,
            "source_node_key": source_node_key,
            "prompt": prompt,
            "code": code_text,
            "summary": ai_resp.get("summary", ""),
            "rationale": ai_resp.get("rationale", ""),
            "ai_status": ai_status,
            "model": GEMINI_MODEL,
            "created_at": datetime.utcnow().isoformat() + "Z",
            "origin": "ai",
            "ai_generated": True
        })

    if not document_exists("mindmap_nodes", sugg_node_key):
        insert_document("mindmap_nodes", {
            "_key": sugg_node_key,
            "map_id": map_id,
            "repo_url": repo_url,
            "mode": "SUGG",
            "label": label,
            "node_type": "suggestion",
            "related_files": [file_path],
            "links": {"suggestion_key": suggestion_key},
            "origin": "ai",
            "ai_generated": True
        })

    if source_node_key:
        edge_key = hashlib.md5(f"{source_node_key}->{sugg_node_key}".encode("utf-8")).hexdigest()[:12]
        if not document_exists("mindmap_edges", edge_key):
            insert_document("mindmap_edges", {
                "_key": edge_key,
                "map_id": map_id,
                "_from": f"mindmap_nodes/{source_node_key}",
                "_to": f"mindmap_nodes/{sugg_node_key}",
                "edge_type": "suggestion",
                "origin": "ai"
            })

    resp = {
        "node_key": sugg_node_key,
        "suggestion_key": suggestion_key,
        "label": label,
        "node_type": "suggestion",
        "origin": "ai",
        "ai_generated": True,
        "selected_file_path": file_path,
    }
    if return_code:
        resp["code"] = code_text
    return resp


# ------------------------------------------------------------
# (C) 라벨 자동 추론 + 파일 수집
# ------------------------------------------------------------
def _normalize_kor(s: str) -> str:
    s = re.sub(r"[()]", " ", s)
    s = re.sub(r"\s+", " ", s).strip()
    return s


def _extract_scope_terms(prompt: str, max_k: int = 6) -> List[str]:
    # 핵심명사 위주 토큰 (한글/영문/숫자)
    toks = re.findall(r"[A-Za-z0-9가-힣_]+", prompt)
    stop = {"수정","삭제","추가","정리","관련","기능","필드","코드","매퍼","쿼리","테스트","해주세요","해줘","에서","의","및"}
    out = []
    seen = set()
    for t in toks:
        if len(t) <= 1:
            continue
        n = t.lower()
        if n in stop:
            continue
        if n not in seen:
            out.append(t)
            seen.add(n)
    return out[:max_k] or toks[:max_k]


def resolve_scope_nodes_from_prompt(map_id: str, prompt: str, top_n: int = 3) -> List[Dict]:
    """
    mindmap_nodes.label 과 prompt 키워드 유사도(단순 토큰 포함)로 스코프 노드 후보를 찾는다.
    반환: [{key, label, score}, ...] score desc
    """
    terms = _extract_scope_terms(_normalize_kor(prompt))
    if not terms:
        return []
    rows = list(db.aql.execute("""
      FOR n IN mindmap_nodes
        FILTER n.map_id == @map_id
        RETURN {key: n._key, label: n.label, files: n.related_files}
    """, bind_vars={"map_id": map_id}))
    scored = []
    for r in rows:
        label = (r.get("label") or "")
        ll = label.lower()
        score = 0.0
        for t in terms:
            tl = t.lower()
            if tl in ll:
                score += 2.0
        if any(t.lower() == ll.replace(" ", "") for t in terms):
            score += 1.0
        if score > 0:
            scored.append({"key": r["key"], "label": label, "score": score})
    scored.sort(key=lambda x: x["score"], reverse=True)
    return scored[:top_n]

def _hash12(s: str) -> str:
    return hashlib.md5(s.encode("utf-8")).hexdigest()[:12]


def _to_file_path(rf: Any) -> Optional[str]:
    """
    related_files 원소에서 파일 경로만 추출:
      - "src/..../A.java" 같은 문자열
      - {"file_path": "..."} 같은 객체
      - {"path": "..."} / {"filename": "..."} 폴백
    """
    if isinstance(rf, str):
        return rf
    if isinstance(rf, dict):
        return rf.get("file_path") or rf.get("path") or rf.get("filename")
    return None


def _paths_from_related(related_files: Any) -> List[str]:
    out: List[str] = []
    if isinstance(related_files, list):
        for rf in related_files:
            fp = _to_file_path(rf)
            if fp:
                out.append(fp)
    return out

def gather_files_by_node_key(
    map_id: str,
    start_node_key: str,
    include_children: bool = True,
    max_files: int = 12,
) -> List[Tuple[str, str]]:
    """
    start_node_key에서 시작해 (옵션)자식들을 따라 내려가며
    각 노드의 related_files에서 파일 경로를 추출해 (source_node_key, file_path) 리스트로 반환.
    - related_files가 ["a.java", ...] 또는 [{"file_path": "a.java"}, ...] 모두 지원
    - 중복 파일은 소문자 비교로 제거
    """
    depth = "0..3" if include_children else "0..0"
    rows = list(
        db.aql.execute(
            f"""
            FOR v IN {depth} OUTBOUND @start_id mindmap_edges
              FILTER v.map_id == @map_id
              RETURN {{ key: v._key, related_files: v.related_files }}
            """,
            bind_vars={"start_id": f"mindmap_nodes/{start_node_key}", "map_id": map_id},
        )
    )

    out: List[Tuple[str, str]] = []
    seen: set[str] = set()

    for r in rows:
        key = r.get("key")
        for fp in _paths_from_related(r.get("related_files")):
            low = fp.lower()
            if low in seen:
                continue
            seen.add(low)
            out.append((key, fp))
            if len(out) >= (max_files or 12):
                return out

    return out


def gather_files_by_label(
    map_id: str,
    label_query: str,
    include_children: bool = True,
    max_files: int = 12,
) -> List[Tuple[str, str]]:
    """
    라벨 포함 검색으로 시작 노드를 고르고, 그 노드(들)에서 파일을 수집.
    여러 노드가 매치되면 첫 노드 기준으로만 진행(기존 UX 유지).
    """
    # 라벨 매치되는 시작 노드 하나 선택
    start = list(
        db.aql.execute(
            """
            FOR n IN mindmap_nodes
              FILTER n.map_id == @map_id
              FILTER CONTAINS(n.label, @q)
              LIMIT 1
              RETURN n._key
            """,
            bind_vars={"map_id": map_id, "q": label_query},
        )
    )
    if not start:
        return []

    return gather_files_by_node_key(
        map_id=map_id,
        start_node_key=start[0],
        include_children=include_children,
        max_files=max_files,
    )

def upsert_code_suggestion_aggregate(
    *,
    map_id: str,
    parent_key: str,
    repo_url: Optional[str],
    items: List[Dict[str, Any]],     # 각 item: file_path, suggestion_key, (opt) code, status, error...
    label: str = "코드 추천",
    idempotency_key: Optional[str] = None,
) -> Dict[str, Any]:
    """
    대표 1개 노드(AGGREGATED_SUGGESTIONS)에 related_files[]로 묶어 저장.
    같은 suggestion_key(우선) 또는 file_path 기준으로 병합.
    """
    stable_label = f"SUGG_AGG::{parent_key}"
    node_key = generate_node_key(map_id, stable_label)
    now = datetime.utcnow().isoformat() + "Z"

    if not document_exists("mindmap_nodes", node_key):
        insert_document(
            "mindmap_nodes",
            {
                "_key": node_key,
                "map_id": map_id,
                "repo_url": repo_url,
                "label": f"{label} ({len(items)})",
                "node_type": "AGGREGATED_SUGGESTIONS",
                "related_files": items,
                "meta": {"idempotency_key": idempotency_key, "count": len(items), "created_at": now},
            },
        )
    else:
        col = db.collection("mindmap_nodes")
        doc = col.get(node_key) or {}
        existing = doc.get("related_files", []) or []
        by_key: Dict[str, Dict[str, Any]] = {}

        def _k(it: Dict[str, Any]) -> str:
            return (it.get("suggestion_key") or it.get("file_path") or _hash12(str(it)))

        for it in existing:
            by_key[_k(it)] = it
        for it in items:
            by_key[_k(it)] = it  # 신규/갱신

        merged = list(by_key.values())
        base_label = (doc.get("label") or label).split("(")[0].strip()
        col.update(
            {
                "_key": node_key,
                "label": f"{base_label} ({len(merged)})",
                "related_files": merged,
                "meta": {**(doc.get("meta") or {}), "count": len(merged), "updated_at": now},
            }
        )

    edge_key = f"{parent_key}__{node_key}".replace("/", "_")
    if not document_exists("mindmap_edges", edge_key):
        insert_document(
            "mindmap_edges",
            {
                "_key": edge_key,
                "map_id": map_id,
                "_from": f"mindmap_nodes/{parent_key}",
                "_to": f"mindmap_nodes/{node_key}",
                "edge_type": "SUGGESTION_AGG",
            },
        )
    return {"node_key": node_key, "count": len(items)}

