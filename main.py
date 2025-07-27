# main.py
from fastapi import FastAPI
from routers import auth, mindmap, recommend, meeting, github

app = FastAPI(
    title="Gitdeun API",
    description="FastAPI 기반 Git 저장소 분석 및 마인드맵 시스템",
    version="1.0.0",
    contact={
        "name": "Gitdeun Backend",
        "url": "https://github.com/Gitdeun/gitdeun-MM",
    },
    openapi_tags=[
        {"name": "Authentication", "description": "Gemini 로그인 및 인증 상태"},
        {"name": "Mindmap", "description": "코드 분석 및 마인드맵 노드/엣지 저장"},
        {"name": "Recommendation", "description": "Gemini 기반 코드 추천"},
        {"name": "Meeting", "description": "회의록 요약 및 마이그레이션"}
    ]
)

app.include_router(auth.router, prefix="/auth", tags=["Authentication"])
app.include_router(mindmap.router, prefix="/mindmap", tags=["Mindmap"])
app.include_router(recommend.router, prefix="/recommend", tags=["Recommendation"])
app.include_router(meeting.router, prefix="/meeting", tags=["Meeting"])
app.include_router(github.router, prefix="/github", tags=["GitHub"])
