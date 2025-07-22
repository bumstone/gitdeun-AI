from fastapi import APIRouter
from models.dto import GeminiLoginRequest

router = APIRouter()

@router.post("/login/gemini")
def login_gemini(req: GeminiLoginRequest):
    return {"status": "success"}

@router.get("/status")
def check_status():
    return {"is_authenticated": True}
