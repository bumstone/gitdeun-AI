from fastapi import APIRouter
from models.dto import RecommendRequest
from services.gemini_service import generate_code_from_prompt

router = APIRouter()

@router.post("")
def recommend_code(req: RecommendRequest):
    code = generate_code_from_prompt(req.prompt)
    return {"repo_id": req.repo_id, "recommended_code": code}
