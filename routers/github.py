# routers/github.py
# 변경 핵심:
# - /repos/fetch : GitHub ZIP → DB 저장 (repo_files + code_analysis)
# - /repos/{repo_id}/files : DB에서 파일 목록 조회
# - /analyze : DB에서 읽은 코드로 마인드맵 노드 생성
# - 기존 parse-and-save, load, code-analysis API도 유지

from datetime import datetime
from fastapi import APIRouter

from models.dto import CodeParseRequest, GitRepoRequest
from services.arangodb_service import (
    insert_document, get_documents_by_key_prefix, list_repo_files
)
from services.github_service import (
    parse_code_by_language,
    fetch_and_store_repo,
    load_repository_files,
    get_repo_id_from_url,
    detect_language_from_filename,
    read_file_from_db
)

router = APIRouter()

@router.post("/repos/fetch", summary="GitHub ZIP → ArangoDB 저장")
def fetch_repo(req: GitRepoRequest):
    return fetch_and_store_repo(req.repo_url)

@router.get("/repos/{repo_id}/files", summary="레포 파일 목록(DB)")
def list_files(repo_id: str):
    return {"files": list_repo_files(repo_id)}

@router.post("/parse-and-save/{repo_id}", summary="코드 파싱 후 code_analysis에 저장(단건)")
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

@router.post("/load", summary="레포 파일 목록(DB) - repo_url로")
def load_repository(req: GitRepoRequest):
    return {"files": load_repository_files(req.repo_url)}

@router.get("/code-analysis/{repo_id}", summary="code_analysis 문서(prefix 검색)")
def get_code_analysis(repo_id: str):
    return get_documents_by_key_prefix("code_analysis", f"{repo_id}_")

@router.post("/analyze", summary="레포 전체 분석 → 마인드맵 노드 기록")
def analyze_repository(req: GitRepoRequest):
    repo_url = req.repo_url
    repo_id = get_repo_id_from_url(repo_url)
    mode = getattr(req, "mode", "DEV")

    # DB에서 목록
    code_files = [f for f in load_repository_files(repo_url) if f.endswith((
        ".py", ".java", ".kt", ".js", ".ts", ".go", ".cpp", ".cs", ".rb", "jsx"
    ))]

    mindmap_nodes = []
    for file_path in code_files:
        code = read_file_from_db(repo_url, file_path)  # DB에서 본문 로드
        language = detect_language_from_filename(file_path)
        parse_result = parse_code_by_language(language, code)

        # (노드 구성 정책은 필요에 맞게 확장)
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
