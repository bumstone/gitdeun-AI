import os, json, re
from dotenv import load_dotenv
import google.generativeai as genai

load_dotenv()
api_key = os.getenv("GEMINI_API_KEY")
if not api_key:
    raise ValueError("❗ GEMINI_API_KEY is not set in .env")

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

💡 JSON 출력 형식 예시:
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
    print(f"📌 Gemini 요약 응답 ({dir_name}):\n{result}")

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
💡 사용자 요청:
{prompt}

아래 요청을 기반으로 실제 작동 가능한 Python 예제 코드를 작성해주세요.
가능하면 FastAPI, SQLite, JWT, SQLAlchemy 등을 활용하세요.
불필요한 설명 없이 코드만 깔끔하게 제공해주세요.
"""
    return request_gemini(full_prompt)