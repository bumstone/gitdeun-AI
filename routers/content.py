# routers/content.py
from fastapi import APIRouter, HTTPException, Query
from fastapi.responses import PlainTextResponse
from pydantic import BaseModel
from typing import Optional, List, Tuple, Dict, Any, Literal
from services.mindmap_service import derive_map_id

# 환경변수/접속정보는 services 모듈에서만 관리
from services.arangodb_service import (
    db,  # AQL 실행용 (이미 env로 초기화됨)
    get_repo_file_content,  # content만 가져오는 헬퍼
    list_repo_files,  # 파일 목록(메타) 헬퍼
    get_document_by_key, get_mindmap_node, find_file_path_by_filename, get_code_recommendation_by_key,
    get_latest_code_recommendation
)

router = APIRouter()

# ---------- 모델 ----------
class FileMeta(BaseModel):
    repo_id: str
    path: str
    language: Optional[str] = None
    sha: Optional[str] = None
    size: Optional[int] = None
    fetched_at: Optional[str] = None

class FileContent(FileMeta):
    content: str
    total_lines: Optional[int] = None
    start_line: Optional[int] = None
    end_line: Optional[int] = None

# ---------- 유틸 ----------
def slice_lines(text: str, start: Optional[int], end: Optional[int]) -> Tuple[str, int, int, int]:
    """1-based 라인 슬라이스 (end 포함). start/end 없으면 전체 반환."""
    lines = text.splitlines()
    total = len(lines)
    s = 1 if not start or start < 1 else start
    e = total if not end or end > total else end
    if s > e:
        s, e = 1, total
    return "\n".join(lines[s-1:e]), total, s, e

def fetch_file_doc(repo_id: str, path: str, sha: Optional[str] = None) -> Optional[dict]:
    """
    repo_files에서 (repo_id, path[, sha])로 최신 1건 문서 조회.
    sha가 없으면 fetched_at DESC로 최신값.
    """
    if sha:
        aql = """
        FOR d IN repo_files
          FILTER d.repo_id == @repo_id AND d.path == @path AND d.sha == @sha
          SORT d.fetched_at DESC
          LIMIT 1
          RETURN d
        """
        bind = {"repo_id": repo_id, "path": path, "sha": sha}
    else:
        aql = """
        FOR d IN repo_files
          FILTER d.repo_id == @repo_id AND d.path == @path
          SORT d.fetched_at DESC
          LIMIT 1
          RETURN d
        """
        bind = {"repo_id": repo_id, "path": path}
    cur = db.aql.execute(aql, bind_vars=bind)
    return next(iter(cur), None)

# ---------- 엔드포인트 ----------
@router.get("/file", response_model=FileContent, summary="repo_files에서 코드 콘텐츠 조회")
def get_file(
    repo_id: str = Query(..., description="예: HyetaekOn-BE"),
    path: str   = Query(..., description="예: src/main/java/.../SearchHistory.java"),
    sha: Optional[str] = Query(None, description="특정 커밋/버전으로 고정할 때(옵션)"),
    start_line: Optional[int] = Query(None, ge=1),
    end_line:   Optional[int] = Query(None, ge=1),
    decode_escaped: bool = Query(False, description="\\n처럼 이스케이프된 개행을 실제 개행으로 복원")
):
    """
    - 우선 문서 메타 + 본문을 통째로 시도(fetch_file_doc).
    - sha가 없거나 문서에 content가 없으면 get_repo_file_content로 fallback.
    - start_line/end_line이 있으면 해당 구간만 반환.
    """
    doc = fetch_file_doc(repo_id, path, sha)
    if not doc:
        # 그래도 없으면 404
        raise HTTPException(status_code=404, detail="file not found")

    content_full = (doc.get("content") or "")
    if not content_full:
        # 구버전 데이터 대비: content만 헬퍼로 재조회
        alt = get_repo_file_content(repo_id, path)
        content_full = alt or ""

    if decode_escaped and ("\\n" in content_full):
        # 데이터가 실제 \n 문자열로 저장된 드문 케이스 방어 (필요할 때만)
        try:
            content_full = content_full.encode("utf-8", "ignore").decode("unicode_escape")
        except Exception:
            pass

    content_out, total, s, e = slice_lines(content_full, start_line, end_line)

    return FileContent(
        repo_id=doc.get("repo_id", repo_id),
        path=doc.get("path", path),
        language=doc.get("language"),
        sha=doc.get("sha"),
        size=doc.get("size"),
        fetched_at=doc.get("fetched_at"),
        content=content_out,
        total_lines=total,
        start_line=s if (start_line or end_line) else None,
        end_line=e if (start_line or end_line) else None,
    )

@router.get("/file/raw", response_class=PlainTextResponse, summary="파일 본문만 텍스트로 반환")
def get_file_raw(
    repo_id: str,
    path: str,
    sha: Optional[str] = None,
    start_line: Optional[int] = None,
    end_line: Optional[int] = None,
    decode_escaped: bool = False,
):
    data: FileContent = get_file(repo_id, path, sha, start_line, end_line, decode_escaped)
    return PlainTextResponse(data.content, media_type="text/plain; charset=utf-8")

@router.get("/files", response_model=List[FileMeta], summary="레포의 파일 목록(간단 메타)")
def list_files(repo_id: str, q: str = Query("", description="경로 부분검색(옵션)"), limit: int = Query(200, ge=1, le=1000)):
    """
    services.list_repo_files(repo_id) 결과를 경로 부분일치로 가볍게 필터링.
    (대상 레포가 매우 크면 별도 AQL 검색을 라우터에 추가로 도입해도 됨)
    """
    result = list_repo_files(repo_id)  # [{ path, language, size, fetched_at }]
    if q:
        q_lower = q.lower()
        result = [r for r in result if q_lower in r["path"].lower()]
    result = result[:limit]
    # FileMeta 형태로 매핑
    return [
        FileMeta(
            repo_id=repo_id,
            path=r["path"],
            language=r.get("language"),
            size=r.get("size"),
            fetched_at=r.get("fetched_at"),
            sha=None,
        )
        for r in result
    ]

class CodeResponse(BaseModel):
    source: Literal["suggestion", "original"]
    code: str
    file_path: Optional[str] = None
    suggestion_key: Optional[str] = None
    node_key: Optional[str] = None

def _first_file_path_from_node(node: Dict[str, Any]) -> Optional[str]:
    rels = node.get("related_files") or []
    if not rels: return None
    first = rels[0]
    if isinstance(first, dict): return first.get("file_path")
    if isinstance(first, str):  return first
    return None

def _collect_related_items(node: Dict[str, Any]) -> List[Dict[str, Any]]:
    out = []
    for it in (node.get("related_files") or []):
        if isinstance(it, dict):
            out.append({"file_path": it.get("file_path"),
                        "suggestion_key": it.get("suggestion_key")})
        elif isinstance(it, str):
            out.append({"file_path": it, "suggestion_key": None})
    return [x for x in out if x.get("file_path")]

def _get_suggestion_code(suggestion_key: str) -> Optional[str]:
    doc = get_document_by_key("code_recommendations", suggestion_key)
    if not doc: return None
    return doc.get("code") or ""

@router.get("/content/code", response_model=CodeResponse)
def get_code(
    node_key: Optional[str] = Query(None),
    repo_id: Optional[str] = Query(None),
    path: Optional[str] = Query(None),
    prefer: Literal["suggestion", "original"] = Query("suggestion"),
):
    # 1) node_key 우선 처리
    if node_key:
        node = get_document_by_key("mindmap_nodes", node_key)
        if not node:
            raise HTTPException(404, "node not found")

        repo_url = node.get("repo_url") or ""
        derived_repo_id = derive_map_id(repo_url) if repo_url else None

        # 추천 우선
        if prefer == "suggestion":
            s_key = (node.get("links") or {}).get("suggestion_key")
            if not s_key:
                rels = _collect_related_items(node)
                s_key = rels[0]["suggestion_key"] if rels and rels[0].get("suggestion_key") else None
            if s_key:
                code = _get_suggestion_code(s_key)
                if code is not None:
                    return CodeResponse(source="suggestion", code=code,
                                        file_path=_first_file_path_from_node(node),
                                        suggestion_key=s_key, node_key=node_key)

        # 원본 폴백
        fp = path or _first_file_path_from_node(node)
        if not fp:
            raise HTTPException(404, "file_path not found on node")
        rid = repo_id or derived_repo_id
        if not rid:
            raise HTTPException(400, "repo_id missing (cannot load original)")
        original = get_repo_file_content(rid, fp)
        if original is None:
            raise HTTPException(404, "original file not found in repo_files")
        return CodeResponse(source="original", code=original, file_path=fp, node_key=node_key)

    # 2) 기존 방식: repo_id + path
    if not repo_id or not path:
        raise HTTPException(400, "either node_key or (repo_id and path) must be provided")
    original = get_repo_file_content(repo_id, path)
    if original is None:
        raise HTTPException(404, "original file not found in repo_files")
    return CodeResponse(source="original", code=original, file_path=path)

def _resolve_full_path_by_filename(map_id: str, filename: str) -> Optional[str]:
    """
    파일명만 들어왔을 때 repo_files에서 같은 map_id(repo_id) 내 경로를 찾아줌.
    여러 개면 가장 짧은 경로를 선택(휴리스틱).
    """
    rows = list(db.aql.execute(
        """
        FOR f IN repo_files
          FILTER f.repo_id == @repo_id
          LET fname = f.filename != null ? f.filename : LAST(SPLIT(f.path, "/"))
          FILTER fname == @fname
          RETURN f.path
        """,
        bind_vars={"repo_id": map_id, "fname": filename}
    ))
    if not rows:
        return None
    # 경로가 여러개면 제일 짧은 경로 고르기
    rows.sort(key=lambda p: (len(p), p))
    return rows[0]

@router.get("/file/by-node", summary="노드 기준 코드 본문 조회 (노드 타입별 기본 prefer, AI→원본 폴백)")
def read_code_by_node(
    node_key: str = Query(..., description="mindmap_nodes._key"),
    file_path: Optional[str] = Query(None, description="파일 경로 또는 파일명(예: Alarm.tsx)"),
    prefer: Optional[str] = Query(None, regex="^(auto|ai|original)$", description="ai|original|auto (미지정 시 노드타입으로 결정)"),
):
    """
    동작 순서:
    1) node_key로 노드 로드 → map_id/노드타입 결정
       - prefer 기본값: AGGREGATED_SUGGESTIONS → auto, 그 외 → original
    2) file_path가 파일명이라면 repo_files에서 '가장 짧은 경로'로 해석
    3) prefer != original 이면 AI 코드 시도:
       3-1) 노드.related_files 안의 suggestion_key 먼저 확인
       3-2) 없으면 code_recommendations에서 (source_node_key = node_key) 최신 1건 조회
       3-3) 실패 시 prefer=auto면 원본으로 폴백, prefer=ai면 404
    4) 원본 코드 반환(경로 재추정 1회 포함)
    """
    # 0) 노드 로드
    node = get_mindmap_node(node_key)
    if not node:
        raise HTTPException(status_code=404, detail="node not found")

    map_id: str = node.get("map_id") or derive_map_id(node.get("repo_url") or "")
    if not map_id:
        raise HTTPException(status_code=400, detail="cannot resolve map_id")

    node_type = (node or {}).get("node_type")

    # 1) prefer 기본값 결정 (노드 타입별)
    if not prefer:
        prefer = "auto" if node_type == "AGGREGATED_SUGGESTIONS" else "original"
    prefer = prefer.lower()

    # 2) file_path 해석(파일명만 온 경우 → 경로로 승격)
    fp_input = (file_path or "").strip()
    if not fp_input:
        # 노드에 파일이 1개뿐이면 그걸 기본값으로
        rel = node.get("related_files") or []
        if isinstance(rel, list) and rel:
            first = rel[0]
            if isinstance(first, dict) and first.get("file_path"):
                fp_input = first["file_path"]
            elif isinstance(first, str):
                fp_input = first
    if not fp_input:
        raise HTTPException(status_code=400, detail="file_path required or node must have related_files")

    if "/" in fp_input:
        resolved_path = fp_input
    else:
        resolved_path = find_file_path_by_filename(map_id, fp_input) or fp_input  # 못찾아도 진행

    # 3) AI 코드 우선 시도 (prefer != original)
    if prefer in ("ai", "auto"):
        # 3-1) 노드.related_files에 suggestion_key가 달린 항목이 있는지 먼저 확인
        rel = node.get("related_files") or []
        skey = None
        if isinstance(rel, list):
            for it in rel:
                if isinstance(it, dict) and it.get("suggestion_key"):
                    rp = it.get("file_path") or ""
                    # 경로 완전/말미 일치 체크
                    if rp == resolved_path or rp.endswith("/" + resolved_path) or resolved_path.endswith("/" + rp):
                        skey = it["suggestion_key"]
                        break

        # 3-2) 없으면 최신 제안을 DB에서 탐색 (⚠ None 바인딩 금지: source_node_key 있을 때만 필터)
        reco = None
        if skey:
            reco = get_code_recommendation_by_key(skey)
        if not reco:
            # 노드 기준 최신 AI 제안만 시도 (source_node_key 전달)
            try:
                reco = get_latest_code_recommendation(map_id, resolved_path, source_node_key=node_key)
            except Exception:
                # 서비스 내부 쿼리 스키마가 다를 수 있으니 조용히 스킵하고 폴백
                reco = None

        if reco and (reco.get("code") or ""):
            code_text = reco.get("code") or ""
            return {
                "node_key": node_key,
                "map_id": map_id,
                "path": resolved_path,
                "origin": "ai",
                "suggestion_key": reco["_key"],
                "code": code_text,
                "length": len(code_text),
            }

        # prefer=ai 면 여기서 끝내고 404 반환
        if prefer == "ai":
            raise HTTPException(status_code=404, detail="ai code not found")

    # 4) 원본 코드 반환
    original = get_repo_file_content(map_id, resolved_path)
    if not original:
        # 파일명이었고 매칭을 못했을 수 있으니 마지막으로 파일명 기준 한 번 더 재시도
        if "/" not in fp_input and resolved_path == fp_input:
            guess = find_file_path_by_filename(map_id, fp_input)
            if guess:
                original = get_repo_file_content(map_id, guess)
                resolved_path = guess

    if not original:
        raise HTTPException(status_code=404, detail="original code not found")

    return {
        "node_key": node_key,
        "map_id": map_id,
        "path": resolved_path,
        "origin": "original",
        "code": original,
        "length": len(original),
    }

