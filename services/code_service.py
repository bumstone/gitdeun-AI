# services/code_service.py
import os
import hashlib
from urllib.parse import urlparse
from typing import Optional
import requests  # 원치 않으면 raw github 폴백 블록 제거 가능

from arango import ArangoClient
from arango.exceptions import AQLQueryExecuteError, ArangoServerError
from config import ARANGODB_USERNAME, ARANGODB_PASSWORD, ARANGODB_DB

_client = ArangoClient()
_db = _client.db(ARANGODB_DB, username=ARANGODB_USERNAME, password=ARANGODB_PASSWORD)

# ----- 환경 변수 기반 로컬/원격 폴백 설정 -----
REPO_CACHE_DIR = os.getenv("REPO_CACHE_DIR", "/tmp/repos")
DEFAULT_BRANCH = os.getenv("DEFAULT_BRANCH", "main")
GITHUB_TOKEN = os.getenv("GITHUB_TOKEN")

LOCAL_ROOT_CANDIDATES = [
    REPO_CACHE_DIR,
    "/Users/jang-uk/dev/gitden-backend/repos",
    "./repos",
    "/tmp/repos",
]

def _normalize_path_for_db(file_path: str) -> str:
    """DB 저장 규칙(`/` → `__`)으로 변환"""
    return file_path.replace("/", "__").lstrip("__")

def _owner_repo_from_url(repo_url: str) -> Optional[tuple[str, str]]:
    try:
        p = urlparse(repo_url)
        parts = [s for s in p.path.split("/") if s]
        if len(parts) >= 2:
            return parts[0], parts[1].replace(".git", "")
    except Exception:
        pass
    return None

def _repo_dir_candidates(repo_url: str) -> list[str]:
    cands = []
    orp = _owner_repo_from_url(repo_url)
    if orp:
        owner, repo = orp
        slug = os.path.join(owner, repo)
        for root in LOCAL_ROOT_CANDIDATES:
            cands.append(os.path.join(root, slug))
    # 해시 키 폴더도 후보
    repokey = hashlib.md5(repo_url.encode("utf-8")).hexdigest()[:12]
    for root in LOCAL_ROOT_CANDIDATES:
        cands.append(os.path.join(root, repokey))
    # 중복 제거
    uniq = []
    for p in cands:
        if p not in uniq:
            uniq.append(p)
    return uniq

def _try_extract_content(doc: dict) -> Optional[str]:
    """문서에서 코드 본문 필드를 추정해서 꺼내기."""
    if not doc:
        return None
    # 자주 쓰이는 필드들 순서대로
    for key in ("content", "file_content", "code", "source", "text", "body"):
        if key in doc and isinstance(doc[key], str) and doc[key].strip():
            return doc[key]
    # content가 하위에 있는 경우도 방어
    if "data" in doc and isinstance(doc["data"], dict):
        for key in ("content", "file_content", "code"):
            val = doc["data"].get(key)
            if isinstance(val, str) and val.strip():
                return val
    return None

def _get_single(aql: str, bind: dict) -> Optional[dict]:
    try:
        cur = _db.aql.execute(aql, bind_vars=bind)
        for d in cur:
            return d
        return None
    except (AQLQueryExecuteError, ArangoServerError):
        return None
    except Exception:
        return None

def _get_code_from_code_analysis(repo_url: str, file_path: str) -> Optional[str]:
    """code_analysis / code_analysis_ai / code_files 순서로 조회."""
    path_db = _normalize_path_for_db(file_path)

    # 1) code_analysis: _key suffix 매칭 (예: ...__src__main__java__... 형태)
    if _db.has_collection("code_analysis"):
        doc = _get_single(
            """
            FOR c IN code_analysis
              FILTER c._key LIKE CONCAT('%', @key_suffix)
              LIMIT 1
              RETURN c
            """,
            {"key_suffix": f"__{path_db}" if not path_db.startswith("__") else path_db},
        )
        cont = _try_extract_content(doc)
        if cont:
            return cont

    # 2) code_analysis_ai: file_path or _key로 조회
    if _db.has_collection("code_analysis_ai"):
        # 2-1) file_path 필드로 조회
        doc = _get_single(
            """
            FOR c IN code_analysis_ai
              FILTER c.file_path == @fp OR c.file_path == @fp_db
              LIMIT 1
              RETURN c
            """,
            {"fp": file_path, "fp_db": path_db},
        )
        cont = _try_extract_content(doc)
        if cont:
            return cont

        # 2-2) _key suffix로도 시도
        doc = _get_single(
            """
            FOR c IN code_analysis_ai
              FILTER ENDS_WITH(c._key, @key_suffix)
              LIMIT 1
              RETURN c
            """,
            {"key_suffix": f"__{path_db}" if not path_db.startswith("__") else path_db},
        )
        cont = _try_extract_content(doc)
        if cont:
            return cont

    # 3) code_files: 표준 스키마 (repo_url + file_path)
    if _db.has_collection("code_files"):
        doc = _get_single(
            """
            FOR c IN code_files
              FILTER c.repo_url == @repo_url
                AND (c.file_path == @fp OR c.file_path == @fp_db)
              LIMIT 1
              RETURN c
            """,
            {"repo_url": repo_url, "fp": file_path, "fp_db": path_db},
        )
        cont = _try_extract_content(doc)
        if cont:
            return cont

    return None

def _get_code_from_local(repo_url: str, file_path: str) -> Optional[str]:
    """로컬 레포 캐시에서 파일 찾기 (/ → 실제 파일 경로)."""
    rel = file_path.lstrip("/")
    for root in _repo_dir_candidates(repo_url):
        candidate = os.path.join(root, rel)
        if os.path.exists(candidate) and os.path.isfile(candidate):
            try:
                with open(candidate, "rb") as f:
                    return f.read().decode("utf-8", errors="ignore")
            except Exception:
                pass
    return None

def _get_code_from_raw_github(repo_url: str, file_path: str) -> Optional[str]:
    """공개 레포면 raw.githubusercontent.com에서 바로 획득(옵션)."""
    orp = _owner_repo_from_url(repo_url)
    if not orp:
        return None
    owner, repo = orp
    rel = file_path.lstrip("/")
    raw_url = f"https://raw.githubusercontent.com/{owner}/{repo}/{DEFAULT_BRANCH}/{rel}"
    headers = {}
    if GITHUB_TOKEN:
        headers["Authorization"] = f"Bearer {GITHUB_TOKEN}"
        headers["Accept"] = "application/vnd.github+json"
    try:
        r = requests.get(raw_url, headers=headers, timeout=10)
        if r.status_code == 200:
            return r.text
    except Exception:
        pass
    return None

def load_original_code_by_path(repo_url: str, file_path: str) -> Optional[str]:
    """
    1) code_analysis / code_analysis_ai / code_files 에서 탐색(경로 자동 변환 지원)
    2) 로컬 레포 캐시
    3) raw.githubusercontent.com
    """
    # 1)
    code = _get_code_from_code_analysis(repo_url, file_path)
    if code:
        return code

    # 2)
    code = _get_code_from_local(repo_url, file_path)
    if code:
        return code

    # 3)
    code = _get_code_from_raw_github(repo_url, file_path)
    if code:
        return code

    return None
