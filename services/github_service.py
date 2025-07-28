import requests
import zipfile
import io
import os
from datetime import datetime

from parser.python_parser import parse_python_code
from parser.javascript_parser import parse_js_code
from parser.java_parser import parse_java_code
from parser.go_parser import parse_go_code
from parser.ruby_parser import parse_ruby_code
from parser.typescript_parser import parse_typescript_code
from parser.cpp_parser import parse_cpp_code
from parser.csharp_parser import parse_csharp_code
from parser.kotlin_parser import parse_kotlin_code

from services.arangodb_service import insert_document

# 언어 감지
def detect_language_from_filename(filename: str) -> str:
    filename = filename.lower()
    if filename.endswith(".py"): return "python"
    elif filename.endswith(".js"): return "javascript"
    elif filename.endswith(".ts"): return "typescript"
    elif filename.endswith(".java"): return "java"
    elif filename.endswith(".kt"): return "kotlin"
    elif filename.endswith(".go"): return "go"
    elif filename.endswith(".rb"): return "ruby"
    elif filename.endswith((".cpp", ".cc", ".cxx")): return "cpp"
    elif filename.endswith(".cs"): return "csharp"
    else: return "unknown"

# 언어별 파서 연결
def parse_code_by_language(language: str, code: str) -> dict:
    language = language.lower()
    if language == "python": return parse_python_code(code)
    elif language == "javascript": return parse_js_code(code)
    elif language == "typescript": return parse_typescript_code(code)
    elif language == "java": return parse_java_code(code)
    elif language == "go": return parse_go_code(code)
    elif language == "ruby": return parse_ruby_code(code)
    elif language == "cpp": return parse_cpp_code(code)
    elif language == "csharp": return parse_csharp_code(code)
    elif language == "kotlin": return parse_kotlin_code(code)
    else: return {"functions": [], "classes": [], "imports": [], "variables": []}

# 저장소 ID 추출
def get_repo_id_from_url(repo_url: str) -> str:
    return repo_url.rstrip('/').split('/')[-1]

# 저장소 ZIP에서 파일 리스트 추출
def load_repository_files(repo_url: str):
    repo_name = repo_url.split("/")[-1]
    owner = repo_url.split("/")[-2]
    zip_url = f"https://github.com/{owner}/{repo_name}/archive/refs/heads/main.zip"

    try:
        response = requests.get(zip_url)
        if response.status_code != 200:
            return []

        repo_path = f"./repos/{repo_name}"
        os.makedirs(repo_path, exist_ok=True)

        with zipfile.ZipFile(io.BytesIO(response.content)) as zip_file:
            zip_file.extractall(repo_path)
            return zip_file.namelist()
    except Exception as e:
        print("ZIP 다운로드 실패:", e)
        return []

# 저장소 ZIP 내부 파일 내용 읽기
def read_file_from_unzipped_repo(repo_url: str, file_path: str) -> str:
    repo_name = repo_url.rstrip('/').split('/')[-1]
    base_dir = f"./repos/{repo_name}"

    # 실제 압축 해제된 하위 폴더 찾기 (예: HyetaekOn-BE-main)
    subdirs = [d for d in os.listdir(base_dir) if os.path.isdir(os.path.join(base_dir, d))]
    if not subdirs:
        print("⚠️ 압축 해제된 디렉토리가 존재하지 않음.")
        return ""

    extract_subdir = os.path.join(base_dir, subdirs[0])

    # file_path가 이미 'HyetaekOn-BE-main/...' 를 포함하고 있으면 제거
    if file_path.startswith(subdirs[0] + "/"):
        file_path = file_path[len(subdirs[0]) + 1:]

    full_path = os.path.join(extract_subdir, file_path)
    print("🔍 시도 중인 파일 경로:", full_path)

    try:
        with open(full_path, "r", encoding="utf-8", errors="ignore") as f:
            return f.read()
    except Exception as e:
        print("❌ 파일 읽기 실패:", full_path)
        return ""



# ArangoDB 저장
def save_parsed_code_to_arango(repo_id: str, filename: str, language: str, parse_result: dict):
    safe_key = f"{repo_id}_{filename.replace('/', '__')}"
    doc = {
        "_key": safe_key,
        "repo_id": repo_id,
        "filename": filename,
        "language": language,
        "functions": parse_result.get("functions", []),
        "classes": parse_result.get("classes", []),
        "imports": parse_result.get("imports", []),
        "variables": parse_result.get("variables", []),
        "created_at": datetime.utcnow().isoformat() + "Z"
    }
    return insert_document("code_analysis", doc)
