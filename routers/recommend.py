from fastapi import APIRouter
from models.dto import RecommendRequest
from services.gemini_service import generate_code_from_prompt

router = APIRouter()

@router.post("", summary="프롬프트 기반 코드 추천", description="Gemini 모델에 프롬프트를 전달하여 추천 코드를 생성합니다.")
def recommend_code(req: RecommendRequest):
    code = generate_code_from_prompt(req.prompt)
    return {"repo_id": req.repo_id, "recommended_code": code}
