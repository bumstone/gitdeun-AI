# services/code_service.py
# 변경 핵심:
# - 기존엔 code_analysis/code_files/로컬/RAW 순서로 찾았음
# - 이제는 repo_files(DB 저장소) → code_analysis(content 백워드 호환) 순으로 탐색

from typing import Optional
from arango.exceptions import AQLQueryExecuteError, ArangoServerError

from services.arangodb_service import get_repo_file_content, db
from services.github_service import get_repo_id_from_url


def load_original_code_by_path(repo_url: str, file_path: str) -> Optional[str]:
    """
    1) repo_files에서 본문 조회 (신규 권장 경로)
    2) (백워드 호환) code_analysis.content 조회
    """
    # 1) repo_files 본문
    repo_id = get_repo_id_from_url(repo_url)
    content = get_repo_file_content(repo_id, file_path)
    if content:
        return content

    # 2) code_analysis.content (과거 데이터 호환)
    try:
        cursor = db.aql.execute(
            """
            FOR c IN code_analysis
              FILTER c.repo_id == @repo_id AND c.filename == @file_path
              LIMIT 1
              RETURN c.content
            """,
            bind_vars={"repo_id": repo_id, "file_path": file_path},
        )
        for v in cursor:
            if isinstance(v, str) and v.strip():
                return v
    except (AQLQueryExecuteError, ArangoServerError):
        pass
    except Exception:
        pass

    return None
