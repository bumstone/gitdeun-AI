# routers/content.py
from fastapi import APIRouter, HTTPException, Query
from fastapi.responses import PlainTextResponse
from pydantic import BaseModel
from typing import Optional, List, Tuple

# 환경변수/접속정보는 services 모듈에서만 관리
from services.arangodb_service import (
    db,                   # AQL 실행용 (이미 env로 초기화됨)
    get_repo_file_content,  # content만 가져오는 헬퍼
    list_repo_files,        # 파일 목록(메타) 헬퍼
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
