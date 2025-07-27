# routers/github.py
from datetime import datetime
from fastapi import APIRouter
from models.dto import CodeParseRequest, GitRepoRequest
from services.arangodb_service import insert_document, get_documents_by_prefix
from services.github_service import (
    parse_code_by_language,
    save_parsed_code_to_arango,
    load_repository_files
)

router = APIRouter()


@router.post("/parse-and-save/{repo_id}")
def parse_and_save_code(repo_id: str, payload: CodeParseRequest):
    parse_result = parse_code_by_language(payload.language, payload.code)

    # 슬래시 제거하여 ArangoDB _key 에 사용 가능한 문자열로 변환
    safe_key = f"{repo_id}_{payload.filename.replace('/', '__')}"

    # 저장할 도큐먼트 구성
    doc = {
        "_key": safe_key,
        "repo_id": repo_id,
        "filename": payload.filename,
        "language": payload.language,
        "functions": parse_result.get("functions", []),
        "classes": parse_result.get("classes", []),
        "imports": parse_result.get("imports", []),
        "variables": parse_result.get("variables", []),
        "created_at": datetime.utcnow().isoformat() + "Z"
    }

    save_result = insert_document("code_analysis", doc)

    return {"result": save_result, "parsed": parse_result}


@router.post("/load")
def load_repository(req: GitRepoRequest):
    """
    GitHub URL을 통해 저장소 코드를 불러와 분석 준비
    """
    result = load_repository_files(req.repo_url)
    return {"files": result}

def create_mindmap_node(repo_url: str, mode: str, parsed_result: dict):
    return insert_document("mindmap_nodes", {
        "repo_url": repo_url,
        "mode": mode,
        "node": parsed_result  # ✅ 실제 분석 결과 저장
    })

@router.get("/code-analysis/{repo_id}", summary="저장된 코드 분석 결과 조회")
def get_code_analysis(repo_id: str):
    return get_documents_by_prefix("code_analysis", f"{repo_id}_")