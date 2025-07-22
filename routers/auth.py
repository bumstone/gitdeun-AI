from fastapi import APIRouter
from models.dto import GeminiLoginRequest

router = APIRouter()

@router.post("/login/gemini", summary="Gemini API 키 로그인", description="Gemini API 키를 받아 세션 인증 상태로 만듭니다.")
def login_gemini(req: GeminiLoginRequest):
    return {"status": "success"}

@router.get("/status")
def check_status():
    return {"is_authenticated": True}
