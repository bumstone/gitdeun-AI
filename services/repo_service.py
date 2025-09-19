from datetime import timezone

from github import Github
from services.arangodb_service import insert_document
import hashlib

def save_repository_info(repo_url: str):
    """
    GitHub API를 통해 저장소 정보를 가져와 ArangoDB에 저장
    """
    g = Github()  # 인증 필요하면 Github("your_token")
    parts = repo_url.strip("/").split("/")
    owner, repo = parts[-2], parts[-1]
    repo_obj = g.get_repo(f"{owner}/{repo}")
    last_utc = repo_obj.get_commits()[0].commit.author.date \
        .astimezone(timezone.utc) \
        .replace(microsecond=0) \
        .isoformat() \
        .replace("+00:00", "Z")

    data = {
        "_key": hashlib.sha1(repo_url.encode()).hexdigest(),
        "repo_url": repo_url,
        "description": repo_obj.description,
        "default_branch": repo_obj.default_branch,
        "last_commit": last_utc
        # repo_obj.get_commits()[0].commit.author.date.isoformat()
    }

    insert_document("repositories", data)
    return data


def get_repository_info(repo_id: str):
    """
    저장된 저장소 정보 조회
    """
    # repo_id로 repositories 컬렉션에서 검색
    from services.arangodb_service import db

    cursor = db.aql.execute("""
        FOR repo IN repositories
            FILTER repo.repo_url LIKE CONCAT('%/', @repo_id) OR repo.repo_name == @repo_id
            SORT repo.fetched_at DESC
            LIMIT 1
            RETURN repo
    """, bind_vars={"repo_id": repo_id})

    result = list(cursor)
    if not result:
        raise Exception(f"Repository {repo_id} not found")

    return result[0]