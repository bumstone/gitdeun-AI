import os, json, re
from dotenv import load_dotenv
import google.generativeai as genai
from typing import Dict, Any, List, Tuple

load_dotenv()
api_key = os.getenv("GEMINI_API_KEY")
if not api_key:
    raise ValueError("GEMINI_API_KEY is not set in .env")

genai.configure(api_key=api_key)
GEMINI_MODEL = "models/gemini-2.5-pro"

def request_gemini(prompt: str) -> str:
    try:
        model = genai.GenerativeModel(GEMINI_MODEL)
        response = model.generate_content(prompt)
        return response.text.strip()
    except Exception as e:
        return f"[Gemini 요청 실패] {str(e)}"

def summarize_directory_code(dir_name: str, file_blocks: list) -> dict:
    files_str = "\n".join([f"- [{name}]\n```{code}```" for name, code in file_blocks])
    PROMPT = f"""
당신은 시니어 소프트웨어 아키텍트입니다.
아래는 `{dir_name}` 디렉터리에 포함된 여러 코드 파일입니다.
이 디렉터리가 어떤 기능을 담당하는지, 그리고 포함된 기능들을 마인드맵 구조로 요약해주세요.

 JSON 출력 형식 예시:
```json
{{
  "node": "기능 이름",
  "related_files": ["파일A", "파일B"],
  "children": [
    {{
      "node": "하위 기능 이름",
      "related_files": ["파일C"],
      "children": []
    }}
  ]
}}```

{files_str}
"""
    result = request_gemini(PROMPT)
    print(f" Gemini 요약 응답 ({dir_name}):\n{result}")

    try:
        match = re.search(r"```json\s*({.*?})\s*```", result, re.DOTALL)
        json_str = match.group(1) if match else result
        return json.loads(json_str)
    except Exception as e:
        return {
            "error": f"JSON 파싱 오류: {str(e)}",
            "gemini_result": result
        }

def generate_code_from_prompt(prompt: str) -> str:
    full_prompt = f"""
 사용자 요청:
{prompt}

아래 요청을 기반으로 실제 작동 가능한 Python 예제 코드를 작성해주세요.
가능하면 FastAPI, SQLite, JWT, SQLAlchemy 등을 활용하세요.
불필요한 설명 없이 코드만 깔끔하게 제공해주세요.
"""
    return request_gemini(full_prompt)

def generate_code_suggestion(file_path: str, original_code: str, prompt: str) -> dict:
    """
    원본 코드를 수정하지 않고 프롬프트 기반으로 새로운 메서드/모듈을 제안한다.
    반환 형식(JSON):
      {
        "code": "제안 코드 전체",
        "summary": "한 줄 요약",
        "rationale": "이 제안을 하는 이유와 기대 효과"
      }
    """
    # 코드 펜스 언어 힌트
    ext = (file_path.split(".")[-1] or "").lower()
    lang_map = {
        "py": "python", "java": "java", "kt": "kotlin", "js": "javascript",
        "ts": "typescript", "go": "go", "cs": "csharp", "php": "php"
    }
    code_lang = lang_map.get(ext, "")

    # 너무 큰 파일은 잘라서 보냄 (토큰 보호)
    safe_original = (original_code or "")[:15000]

    suggestion_prompt = f"""
당신은 시니어 백엔드 개발자입니다.
다음은 `{file_path}`의 원본 코드입니다. 이 코드를 **수정하지 않고**, 사용자 요청에 맞는
새로운 기능/메서드/모듈을 **추가로 제안**하세요.

- 기존 코드는 삭제/수정하지 말고, 추가 가능한 코드만 제안합니다.
- 컴파일/빌드 가능한 완전한 예시를 선호합니다(필요한 import/annotation 포함).
- 보일러플레이트는 최소화하되 맥락상 필요한 부분은 포함합니다.
- 응답은 **반드시 JSON만** 반환하고, 그 외 텍스트를 포함하지 마세요.

JSON 스키마:
```json
{{
  "code": "제안 코드 전체",
  "summary": "한 줄 요약",
  "rationale": "이 제안을 하는 이유와 기대 효과"
}}

    # 사용자 요청
    {prompt}

    # 원본 코드 (읽기 전용)
    ```{code_lang}
    {safe_original}
    """
    result = request_gemini(suggestion_prompt)

    try:
        match = re.search(r"```json\s*({.*?})\s*```", result, re.DOTALL | re.IGNORECASE)
        json_str = match.group(1) if match else result.strip()
        parsed = json.loads(json_str)
        return {
            "code": parsed.get("code", "") or "",
            "summary": parsed.get("summary", "") or "",
            "rationale": parsed.get("rationale", "") or ""
        }
    except Exception as e:
        return {
            "code": "",
            "summary": "",
            "rationale": f"JSON 파싱 오류: {str(e)}",
            "gemini_result": result
        }


def ai_make_title(graph: Dict[str, Any], prompt: str | None, max_len: int) -> Tuple[str, str]:
    """
    그래프의 중심/최신 변경점 + 프롬프트 의도를 1줄 제목으로 압축.
    실제 구현에서는 Gemini로 요약 후 길이 제한을 적용하세요.
    """
    base = "기능 중심 마인드맵"
    if prompt:
        base = f"{base} — {prompt[:30]}".strip()
    title = base if len(base) <= max_len else (base[:max_len-1] + "…")
    summary = "현재 그래프 구조와 최근 프롬프트 의도를 종합해 핵심 기능을 요약했습니다."
    return title, summary