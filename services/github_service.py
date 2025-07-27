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

def parse_code_by_language(language: str, code: str) -> dict:
    language = language.lower()
    if language == "python":
        return parse_python_code(code)
    elif language == "javascript":
        return parse_js_code(code)
    elif language == "java":
        return parse_java_code(code)
    elif language == "go":
        return parse_go_code(code)
    elif language == "ruby":
        return parse_ruby_code(code)
    elif language == "typescript":
        return parse_typescript_code(code)
    elif language in ("cpp", "c++"):
        return parse_cpp_code(code)
    elif language in ("csharp", "c#"):
        return parse_csharp_code(code)
    elif language == "kotlin":
        return parse_kotlin_code(code)
    else:
        return {"error": f"Unsupported language: {language}"}

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
        "created_at": datetime.utcnow().isoformat() + "Z"
    }
    return insert_document("code_analysis", doc)

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
    elif filename.endswith(".cpp") or filename.endswith(".cc") or filename.endswith(".cxx"):
        return "cpp"
    elif filename.endswith(".cs"):
        return "csharp"
    else:
        return "unknown"


def load_repository_files(repo_url: str):
    if repo_url.endswith("/"):
        repo_url = repo_url[:-1]
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
        return []

def read_file_from_unzipped_repo(repo_url: str, file_path: str) -> str:
    repo_name = repo_url.rstrip('/').split('/')[-1]
    extract_dir = f"./repos/{repo_name}/{repo_name}-main"
    full_path = os.path.join(extract_dir, file_path)

    try:
        with open(full_path, "r", encoding="utf-8") as f:
            return f.read()
    except Exception:
        return ""
