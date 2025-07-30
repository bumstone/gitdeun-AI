import os
import re
import json
import google.generativeai as genai
from dotenv import load_dotenv

# .env에서 API 키 로드
load_dotenv()
api_key = os.getenv("GEMINI_API_KEY")

# API 키 확인 및 설정
if not api_key:
    raise ValueError("GEMINI_API_KEY가 .env에 설정되어 있지 않습니다.")
genai.configure(api_key=api_key)

# 모델 이름 상수화
GEMINI_MODEL = "models/gemini-2.5-pro"

# 텍스트 생성 함수
def request_gemini(prompt: str) -> str:
    try:
        model = genai.GenerativeModel(GEMINI_MODEL)
        response = model.generate_content(prompt)
        return response.text.strip()
    except Exception as e:
        return f"Gemini 응답 실패: {str(e)}"

# 코드 생성/요약용 함수
def generate_code_from_prompt(prompt: str) -> str:
    return request_gemini(prompt)

# 마인드맵 요약 함수
def summarize_code(code: str) -> dict:
    PROMPT_TEMPLATE = """
    당신은 시니어 소프트웨어 아키텍트입니다.
    아래 코드를 읽고 다음과 같은 마인드맵 JSON을 생성하세요.

    - JSON 형태 예시:
    {{
      "node": "클래스명.함수명",
      "children": ["기능1", "기능2", "기능3"]
    }}

    아래 코드의 기능을 위 형식에 맞춰 요약해주세요.
    코드:
    ```
    {code}
    ```
    """

    try:
        prompt = PROMPT_TEMPLATE.format(code=code)
        result = request_gemini(prompt)

        print("📌 Gemini 응답 결과:")
        print(result)

        # 수정된 정규표현식: ```json 블록 내의 JSON 추출
        match = re.search(r'```json\s*({.*?})\s*```', result, re.DOTALL)
        if not match:
            return {
                "error": "유효한 JSON 블록을 찾을 수 없습니다.",
                "gemini_result": result
            }

        json_str = match.group(1)  # 괄호 그룹 내부만 추출
        parsed = json.loads(json_str)
        return parsed

    except json.JSONDecodeError as e:
        return {
            "error": f"JSON 파싱 오류: {str(e)}",
            "gemini_result": result
        }
    except Exception as e:
        print(f"❗ summarize_code 예외: {str(e)}")
        return {
            "error": f"예외 발생: {str(e)}",
            "gemini_result": result
        }
