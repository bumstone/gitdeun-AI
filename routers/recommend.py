# routers/recommend.py

from fastapi import APIRouter
from models.dto import RecommendRequest
from services.gemini_service import generate_code_from_prompt

router = APIRouter()

@router.post("", summary="프롬프트 기반 코드 추천", description="사용자 프롬프트에 맞는 예제 코드를 Gemini를 통해 추천합니다.")
def recommend_code(req: RecommendRequest):
    code = generate_code_from_prompt(req.prompt)
    return {
        "repo_id": req.repo_id,
        "prompt": req.prompt,
        "recommended_code": code
    }