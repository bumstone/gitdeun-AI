from datetime import datetime
from fastapi import APIRouter
from models.dto import CodeParseRequest, GitRepoRequest
from services.arangodb_service import insert_document, get_documents_by_prefix
from services.github_service import (
    parse_code_by_language,
    save_parsed_code_to_arango,
    load_repository_files,
    get_repo_id_from_url,
    detect_language_from_filename,
    read_file_from_unzipped_repo
)

router = APIRouter()

@router.post("/parse-and-save/{repo_id}")
def parse_and_save_code(repo_id: str, payload: CodeParseRequest):
    parse_result = parse_code_by_language(payload.language, payload.code)

    safe_key = f"{repo_id}_{payload.filename.replace('/', '__')}"
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
    return {"files": load_repository_files(req.repo_url)}

@router.get("/code-analysis/{repo_id}", summary="저장된 코드 분석 결과 조회")
def get_code_analysis(repo_id: str):
    return get_documents_by_prefix("code_analysis", f"{repo_id}_")

@router.post("/analyze", summary="전체 저장소 자동 분석 및 마인드맵 생성")
def analyze_repository(req: GitRepoRequest):
    repo_url = req.repo_url
    #mode = req.mode #오류나서 지움
    repo_id = get_repo_id_from_url(repo_url)

    all_files = load_repository_files(repo_url)
    code_files = [f for f in all_files if f.endswith((".py", ".java", ".kt"))]

    mindmap_nodes = []

    for file_path in code_files:
        code = read_file_from_unzipped_repo(repo_url, file_path)
        language = detect_language_from_filename(file_path)
        parse_result = parse_code_by_language(language, code)

        safe_key = f"{repo_id}_{file_path.replace('/', '__')}"
        doc = {
            "_key": safe_key,
            "repo_id": repo_id,
            "filename": file_path,
            "language": language,
            "functions": parse_result.get("functions", []),
            "classes": parse_result.get("classes", []),
            "imports": parse_result.get("imports", []),
            "variables": parse_result.get("variables", []),
            "created_at": datetime.utcnow().isoformat() + "Z"
        }
        insert_document("code_analysis", doc)

        for class_name in parse_result.get("classes", []):
            children = [{"node": m} for m in parse_result.get("functions", [])]
            mindmap_nodes.append({
                "repo_url": repo_url,
                "mode": mode,
                "node": class_name,
                "children": children
            })

    for node in mindmap_nodes:
        insert_document("mindmap_nodes", node)

    return {"repo_id": repo_id, "files_analyzed": len(code_files), "nodes": mindmap_nodes}
