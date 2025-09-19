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
from typing import List, Tuple, Dict, Optional, Literal
from services.arangodb_service import db


def _extract_filename_from_prompt(prompt: str, SUPPORTED_EXT=None) -> Optional[str]:
    # ✅ 기본값 보정: 인자를 안 주면 CODE_EXTS 사용
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
        test_penalty = +10 if "/test/" in p else 0
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

def resolve_file_path_auto(repo_url: str, prompt: str, source_node_key: Optional[str]=None) -> Tuple[Optional[str], List[str], str]:
    repo_id = derive_map_id(repo_url)
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
                return files[0], files, "source_node_related_file"
    filename = _extract_filename_from_prompt(prompt)
    if filename:
        paths = _find_paths_by_filename(repo_id, filename)
        if not paths:
            return None, [], "prompt_filename_not_found"
        chosen = _auto_choose_path(paths)
        return chosen, paths, "prompt_filename_auto_chosen"
    return None, [], "no_filename"

def create_code_suggestion_node(
    map_id: str,
    repo_url: str,
    file_path: Optional[str],
    prompt: str,
    source_node_key: Optional[str] = None,
    return_code: bool = False,                 # 👈 추가
    prefer_main_when_ambiguous: bool = True,   # 👈 (옵션) 모호시 main 우선
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
        resp["code"] = code_text    # 👈 코드 동봉
    return resp

def _normalize_kor(s: str) -> str:
    # 괄호/영문 병기 제거, 공백 정리
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
    # 전 노드 라벨/관련파일 긁어서 점수화
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
                score += 2.0                 # 라벨 매칭 가중치
        # 라벨에 '공공서비스'처럼 정확 키워드가 있으면 보너스
        if any(t.lower() == ll.replace(" ", "") for t in terms):
            score += 1.0
        if score > 0:
            scored.append({"key": r["key"], "label": label, "score": score})
    scored.sort(key=lambda x: x["score"], reverse=True)
    return scored[:top_n]

CODE_EXTS = (".java",".kt",".py",".ts",".js",".go",".rb",".cs",".cpp")

def gather_files_by_label(map_id: str, label_query: str, include_children: bool = True, max_files: int = 20) -> List[Tuple[str, str]]:
    """
    반환: [(source_node_key, full_path), ...]
    - label_query: 부분일치(LIKE)로 라벨 매칭
    - include_children=True면 해당 노드의 하위(OUTBOUND)도 포함해 related_files 수집
    - related_files가 파일명만 있어도 repo_files에서 풀경로로 해석
    """
    # 1) 시작 노드(라벨 LIKE)
    starts = list(db.aql.execute("""
      FOR n IN mindmap_nodes
        FILTER n.map_id == @map_id
          AND n.label LIKE CONCAT('%', @q, '%')
        RETURN { key: n._key, files: n.related_files }
    """, bind_vars={"map_id": map_id, "q": label_query}))

    if not starts:
        return []

    repo_id = map_id  # 규약: map_id == repo_id
    files: List[Tuple[str, str]] = []

    # 2) 자기 자신 + (선택) 자식들에서 related_files 수집
    node_iters = []
    if include_children:
        for s in starts:
            cur = db.aql.execute("""
              FOR v, e, p IN 1..3 OUTBOUND CONCAT('mindmap_nodes/', @start) mindmap_edges
                FILTER e.map_id == @map_id
                RETURN v
            """, bind_vars={"start": s["key"], "map_id": map_id})
            node_iters.append((s, list(cur)))
    else:
        node_iters = [(s, []) for s in starts]

    for s, children in node_iters:
        for n in ([s] + children):
            for fp in (n.get("related_files") or []):
                if not fp:
                    continue
                # 코드 파일만
                low = fp.lower()
                if not low.endswith(CODE_EXTS):
                    continue
                # ✅ 파일명 → 풀경로
                full = _resolve_to_full_path(repo_id, fp) or fp
                files.append((s["key"], full))

    # 3) 중복 제거 & 상한
    seen = set()
    out: List[Tuple[str, str]] = []
    for src_key, fp in files:
        k = (src_key, fp)
        if k in seen:
            continue
        seen.add(k)
        out.append((src_key, fp))
        if len(out) >= max_files:
            break
    return out

def _resolve_to_full_path(repo_id: str, name_or_path: str) -> Optional[str]:
    # 이미 경로
    if "/" in (name_or_path or ""):
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
    # src/main 우선, /test/ 패널티, 경로 짧을수록 우선
    def score(p: str):
        return (-10 if "src/main" in p else 0) + (10 if "/test/" in p or "src/test" in p else 0), len(p)
    return sorted(set(rows), key=score)[0]