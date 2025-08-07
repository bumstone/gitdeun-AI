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

    data = {
        "_key": hashlib.sha1(repo_url.encode()).hexdigest(),
        "repo_url": repo_url,
        "description": repo_obj.description,
        "default_branch": repo_obj.default_branch,
        "last_commit": repo_obj.get_commits()[0].commit.author.date.isoformat()
    }

    insert_document("repositories", data)
    return data
