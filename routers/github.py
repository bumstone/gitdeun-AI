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

@router.get("/code-analysis/{repo_id}")
def get_code_analysis(repo_id: str):
    return get_documents_by_prefix("code_analysis", f"{repo_id}_")

@router.post("/analyze")
def analyze_repository(req: GitRepoRequest):
    repo_url = req.repo_url
    repo_id = get_repo_id_from_url(repo_url)
    mode = getattr(req, "mode", "DEV")  # 기본값 처리

    all_files = load_repository_files(repo_url)
    code_files = [f for f in all_files if f.endswith((
        ".py", ".java", ".kt", ".js", ".ts", ".go", ".cpp", ".cs", ".rb"
    ))]

    mindmap_nodes = []

    for file_path in code_files:
        code = read_file_from_unzipped_repo(repo_url, file_path)
        language = detect_language_from_filename(file_path)
        parse_result = parse_code_by_language(language, code)

        save_parsed_code_to_arango(repo_id, file_path, language, parse_result)

        for class_name in parse_result.get("classes", []):
            children = [{"node": func_name} for func_name in parse_result.get("functions", [])]
            mindmap_nodes.append({
                "repo_url": repo_url,
                "mode": mode,
                "node": class_name,
                "children": children
            })

        if not parse_result.get("classes") and parse_result.get("functions"):
            for func_name in parse_result["functions"]:
                mindmap_nodes.append({
                    "repo_url": repo_url,
                    "mode": mode,
                    "node": func_name,
                    "children": []
                })

    for node in mindmap_nodes:
        insert_document("mindmap_nodes", node)

    return {
        "repo_id": repo_id,
        "files_analyzed": len(code_files),
        "nodes_created": len(mindmap_nodes),
        "nodes": mindmap_nodes
    }
