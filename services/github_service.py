# services/github_service.py
# 변경 핵심:
# - 로컬 ./repos 사용 제거
# - GitHub ZIP을 메모리로 받아서 ArangoDB(repo_files)에 파일별로 저장
# - 코드 파일은 즉시 파싱해서 code_analysis에도 기록
# - 이후 조회(load/read)는 모두 DB에서 수행
# - 중복 키 방지(seen_keys) 추가 → 같은 파일을 한 번만 처리
# - code_analysis 저장 시 insert_document(upsert) 사용 → 409 방지

import io
import os
import zipfile
from datetime import datetime

import requests

from parser.python_parser import parse_python_code
from parser.javascript_parser import parse_js_code
from parser.java_parser import parse_java_code
from parser.go_parser import parse_go_code
from parser.ruby_parser import parse_ruby_code
from parser.typescript_parser import parse_typescript_code
from parser.cpp_parser import parse_cpp_code
from parser.csharp_parser import parse_csharp_code
from parser.kotlin_parser import parse_kotlin_code

from services.arangodb_service import (
    upsert_repo, upsert_repo_file, insert_document,
    get_repo_file_content, list_repo_files
)

# 텍스트/소스 위주로만 저장 (바이너리/대용량은 제외)
TEXT_EXT = {
    ".py", ".java", ".kt", ".js", ".ts", ".go", ".cpp", ".cc", ".cxx", ".cs", ".rb",
    ".md", ".json", ".yml", ".yaml", ".xml", ".gradle", ".properties", ".txt"
}


def get_repo_id_from_url(repo_url: str) -> str:
    return repo_url.rstrip('/').split('/')[-1]


def detect_language_from_filename(filename: str) -> str:
    fn = filename.lower()
    if fn.endswith(".py"): return "python"
    if fn.endswith(".js"): return "javascript"
    if fn.endswith(".ts"): return "typescript"
    if fn.endswith(".java"): return "java"
    if fn.endswith(".kt"): return "kotlin"
    if fn.endswith(".go"): return "go"
    if fn.endswith(".rb"): return "ruby"
    if fn.endswith((".cpp", ".cc", ".cxx")): return "cpp"
    if fn.endswith(".cs"): return "csharp"
    return "unknown"


def parse_code_by_language(language: str, code: str) -> dict:
    m = (language or "").lower()
    if m == "python": return parse_python_code(code)
    if m == "javascript": return parse_js_code(code)
    if m == "typescript": return parse_typescript_code(code)
    if m == "java": return parse_java_code(code)
    if m == "go": return parse_go_code(code)
    if m == "ruby": return parse_ruby_code(code)
    if m == "cpp": return parse_cpp_code(code)
    if m == "csharp": return parse_csharp_code(code)
    if m == "kotlin": return parse_kotlin_code(code)
    return {"functions": [], "classes": [], "imports": [], "variables": []}


def fetch_and_store_repo(repo_url: str, default_branch: str = "main") -> dict:
    """
    GitHub ZIP을 메모리로 받아 파일 단위로 ArangoDB(repo_files)에 저장하고,
    코드 파일은 즉시 파싱하여 code_analysis에도 기록.
    멱등/재실행 안전:
      - repo_files: upsert_repo_file()
      - code_analysis: insert_document() → 내부에서 upsert 처리
      - ZIP 내부 중복 파일 대비: seen_keys 으로 1회만 처리
    """
    repo_id = get_repo_id_from_url(repo_url)
    upsert_repo(repo_id, repo_url, default_branch)

    owner = repo_url.rstrip("/").split("/")[-2]
    name = repo_url.rstrip("/").split("/")[-1]
    zip_url = f"https://github.com/{owner}/{name}/archive/refs/heads/{default_branch}.zip"

    r = requests.get(zip_url, timeout=60)
    r.raise_for_status()

    files_saved = 0
    files_parsed = 0
    seen_keys = set()  # 🔸 중복 방지

    with zipfile.ZipFile(io.BytesIO(r.content)) as zf:
        for info in zf.infolist():
            if info.is_dir():
                continue

            # zip 내부 경로: "<repo>-<branch>/<path>"
            parts = info.filename.split("/", 1)
            if len(parts) < 2:
                continue
            path = parts[1]  # 레포 루트 기준 경로

            key = f"{repo_id}__{path.replace('/', '__')}"
            if key in seen_keys:
                continue
            seen_keys.add(key)

            _, ext = os.path.splitext(path)
            if ext and ext.lower() not in TEXT_EXT:
                # 바이너리/대용량은 스킵
                continue

            raw = zf.read(info.filename)
            try:
                content = raw.decode("utf-8")
            except UnicodeDecodeError:
                # 인코딩 이슈/바이너리로 판단 시 스킵
                continue

            lang = detect_language_from_filename(path)
            upsert_repo_file(repo_id, path, lang, content, size=len(raw))
            files_saved += 1

            # 파싱해서 code_analysis 기록 (분석 결과가 있을 때만)
            pr = parse_code_by_language(lang, content)
            if any(pr.values()):
                insert_document("code_analysis", {
                    "_key": key,                    # 🔸 repo_id__경로 규칙
                    "repo_id": repo_id,
                    "filename": path,
                    "language": lang,
                    "functions": pr.get("functions", []),
                    "classes": pr.get("classes", []),
                    "imports": pr.get("imports", []),
                    "variables": pr.get("variables", []),
                    "content": content,
                    "created_at": datetime.utcnow().isoformat() + "Z"
                })
                files_parsed += 1

    return {"repo_id": repo_id, "files_saved": files_saved, "files_parsed": files_parsed}


def load_repository_files(repo_url: str) -> list[str]:
    """DB에서 파일 목록 조회"""
    repo_id = get_repo_id_from_url(repo_url)
    return [f["path"] for f in list_repo_files(repo_id)]


def read_file_from_db(repo_url: str, file_path: str) -> str:
    """DB에서 파일 본문 조회"""
    repo_id = get_repo_id_from_url(repo_url)
    return get_repo_file_content(repo_id, file_path) or ""
