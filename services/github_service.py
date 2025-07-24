# services/github_service.py

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
