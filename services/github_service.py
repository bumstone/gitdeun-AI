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

def get_repo_id_from_url(repo_url: str) -> str:
    return repo_url.rstrip('/').split('/')[-1]

def detect_language_from_filename(filename: str) -> str:
    filename = filename.lower()
    if filename.endswith(".py"):
        return "python"
    elif filename.endswith(".js"):
        return "javascript"
    elif filename.endswith(".ts"):
        return "typescript"
    elif filename.endswith(".java"):
        return "java"
    elif filename.endswith(".kt"):
        return "kotlin"
    elif filename.endswith(".go"):
        return "go"
    elif filename.endswith(".rb"):
        return "ruby"
    elif filename.endswith((".cpp", ".cc", ".cxx")):
        return "cpp"
    elif filename.endswith(".cs"):
        return "csharp"
    else:
        return "unknown"

def parse_code_by_language(language: str, code: str) -> dict:
    language = language.lower()
    if language == "python":
        return parse_python_code(code)
    elif language == "javascript":
        return parse_js_code(code)
    elif language == "typescript":
        return parse_typescript_code(code)
    elif language == "java":
        return parse_java_code(code)
    elif language == "go":
        return parse_go_code(code)
    elif language == "ruby":
        return parse_ruby_code(code)
    elif language == "cpp":
        return parse_cpp_code(code)
    elif language == "csharp":
        return parse_csharp_code(code)
    elif language == "kotlin":
        return parse_kotlin_code(code)
    else:
        return {"functions": [], "classes": [], "imports": [], "variables": []}

def load_repository_files(repo_url: str):
    repo_name = repo_url.rstrip("/").split("/")[-1]
    owner = repo_url.rstrip("/").split("/")[-2]
    zip_url = f"https://github.com/{owner}/{repo_name}/archive/refs/heads/main.zip"

    try:
        response = requests.get(zip_url)
        if response.status_code != 200:
            print("❌ ZIP 다운로드 실패:", zip_url)
            return []

        repo_path = f"./repos/{repo_name}"
        os.makedirs(repo_path, exist_ok=True)

        with zipfile.ZipFile(io.BytesIO(response.content)) as zip_file:
            zip_file.extractall(repo_path)
            return zip_file.namelist()
    except Exception as e:
        print("❌ ZIP 요청 에러:", e)
        return []

def read_file_from_unzipped_repo(repo_url: str, file_path: str) -> str:
    repo_name = repo_url.rstrip('/').split('/')[-1]
    base_dir = f"./repos/{repo_name}"

    subdirs = [d for d in os.listdir(base_dir) if os.path.isdir(os.path.join(base_dir, d))]
    if not subdirs:
        print("⚠️ 압축 해제된 디렉토리가 없음.")
        return ""

    extract_subdir = os.path.join(base_dir, subdirs[0])
    full_path = os.path.join(base_dir, file_path)
    if not os.path.exists(full_path):
        full_path = os.path.join(extract_subdir, file_path)

    print("🔍 시도 중인 파일 경로:", full_path)

    try:
        with open(full_path, "r", encoding="utf-8", errors="ignore") as f:
            return f.read()
    except Exception as e:
        print("❌ 파일 읽기 실패:", full_path, e)
        return ""

def save_parsed_code_to_arango(repo_id: str, filename: str, language: str, parse_result: dict, content: str):
    if not any(parse_result.values()) and not content.strip():
        print(f"⛔️ 내용이 없어서 저장 안함: {filename}")
        return

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
        "content": content,  # ✅ 실제 코드 텍스트 추가
        "created_at": datetime.utcnow().isoformat() + "Z"
    }
    insert_document("code_analysis", doc)
