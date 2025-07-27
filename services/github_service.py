# services/github_service.py
import requests
import zipfile
import io
import os

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
from datetime import datetime


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
    document = {
        "_key": f"{repo_id}_{filename}",
        "repo_id": repo_id,
        "filename": filename,
        "language": language,
        "functions": parse_result.get("functions", []),
        "classes": parse_result.get("classes", []),
        "imports": parse_result.get("imports", []),
        "created_at": datetime.utcnow().isoformat()
    }
    return insert_document("code_analysis", document)

def load_repository_files(repo_url: str):
    """
    GitHub 저장소를 ZIP으로 다운로드하고 파일 이름 리스트 반환
    """
    if repo_url.endswith("/"):
        repo_url = repo_url[:-1]
    repo_name = repo_url.split("/")[-1]
    owner = repo_url.split("/")[-2]

    zip_url = f"https://github.com/{owner}/{repo_name}/archive/refs/heads/main.zip"

    try:
        response = requests.get(zip_url)
        if response.status_code != 200:
            return {"error": f"Failed to download ZIP. Status: {response.status_code}"}

        repo_path = f"./repos/{repo_name}"
        os.makedirs(repo_path, exist_ok=True)

        with zipfile.ZipFile(io.BytesIO(response.content)) as zip_file:
            zip_file.extractall(repo_path)
            return zip_file.namelist()
    except Exception as e:
        return {"error": str(e)}