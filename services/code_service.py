# services/code_service.py
# 변경 핵심:
# - 기존엔 code_analysis/code_files/로컬/RAW 순서로 찾았음
# - 이제는 repo_files(DB 저장소) → code_analysis(content 백워드 호환) 순으로 탐색

from typing import Optional, List
from services.arangodb_service import get_repo_file_content, db
from services.mindmap_service import derive_map_id

def _find_paths_by_filename(repo_id: str, filename: str) -> List[str]:
    return list(db.aql.execute("""
      FOR f IN repo_files
        FILTER f.repo_id == @repo_id
          AND (f.path LIKE CONCAT('%/', @fn)
               OR f.path == @fn
               OR f.path LIKE CONCAT('%', @fn))
      RETURN f.path
    """, bind_vars={"repo_id": repo_id, "fn": filename}))

def _choose_best_path(cands: List[str]) -> Optional[str]:
    if not cands:
        return None
    # src/main 우선, /test/ 패널티, 경로 짧을수록 우선
    def score(p: str):
        return (-10 if "src/main" in p else 0) + (10 if "/test/" in p or "src/test" in p else 0), len(p)
    return sorted(set(cands), key=score)[0]

def load_original_code_by_path(repo_url: str, file_path_or_name: str) -> Optional[str]:
    """
    file_path_or_name가 파일명만 와도 repo_files에서 후보를 찾아
    가장 적절한 풀경로를 고른 뒤 본문을 반환.
    """
    if not repo_url or not file_path_or_name:
        return None
    repo_id = derive_map_id(repo_url)

    # 이미 경로라면 그대로 시도
    if "/" in file_path_or_name:
        return get_repo_file_content(repo_id, file_path_or_name)

    # 파일명만 온 경우 → 후보 검색 후 베스트 경로 선택
    best = _choose_best_path(_find_paths_by_filename(repo_id, file_path_or_name))
    if not best:
        return None
    return get_repo_file_content(repo_id, best)