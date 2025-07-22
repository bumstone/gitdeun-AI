from fastapi import FastAPI
from routers import auth, mindmap, recommend, meeting

app = FastAPI(
    title="Gitdeun AI Platform",
    description="AI 기반 마인드맵 생성 및 코드 추천 시스템",
    version="1.0.0",
    openapi_tags=[
        {"name": "Authentication", "description": "Gemini API 로그인 관련"},
        {"name": "Mindmap", "description": "분석 결과 마인드맵 관련 API"},
        {"name": "Recommendation", "description": "코드 생성 추천 API"},
        {"name": "Meeting", "description": "음성 회의록 요약/이관 API"},
    ]
)

app.include_router(auth.router, prefix="/auth", tags=["Authentication"])
app.include_router(mindmap.router, prefix="/mindmap", tags=["Mindmap"])
app.include_router(recommend.router, prefix="/recommend", tags=["Recommendation"])
app.include_router(meeting.router, prefix="/meeting", tags=["Meeting"])
