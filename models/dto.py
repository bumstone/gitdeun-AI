from pydantic import BaseModel, Field
from typing import Optional, Literal
from datetime import datetime


class GeminiLoginRequest(BaseModel):
    api_key: str

class AnalyzeRequest(BaseModel):
    repo_url: str

class RecommendRequest(BaseModel):
    repo_id: str
    prompt: str

class MeetingSummaryRequest(BaseModel):
    meeting_id: str
    transcript: str

class MeetingMigrateRequest(BaseModel):
    meeting_id: str
    transcript: str

class CodeParseRequest(BaseModel):
    filename: str
    language: str  # e.g., "python", "javascript", "java"
    code: str

class GitRepoRequest(BaseModel):
    repo_url: str
    mode: str = "DEV"  # 기본값 제공

# --- 제안용 신규 ---
class SuggestionRequest(BaseModel):
    repo_url: str = Field(..., description="예: https://github.com/org/repo")
    ##source_node_key: str = Field(..., description="클릭된 기존 노드의 _key")
    source_node_key: Optional[str] = None
    file_path: str = Field(..., description="예: src/main/java/.../AnswerController.java")
    prompt: str = Field(..., description="개선/추천을 원하는 프롬프트")

class SuggestionCreateResponse(BaseModel):
    node_key: str
    suggestion_key: str
    label: str
    # 생성 응답에도 출처 표기(프론트가 바로 구분 가능)
    node_type: Literal["suggestion"] = "suggestion"
    origin: Literal["ai", "human"] = "ai"
    ai_generated: bool = True

class SuggestionDetailResponse(BaseModel):
    suggestion_key: str
    repo_url: str
    file_path: str
    prompt: str
    code: str
    summary: Optional[str] = None
    rationale: Optional[str] = None
    created_at: Optional[str] = None
    # 구분 필드
    origin: Literal["ai", "human"] = "ai"
    ai_generated: bool = True
    model: Optional[str] = None

# --- repo 정보 조회를 위한 응답 모델 ---
class RepoInfoResponse(BaseModel):
    default_branch: str
    last_commit: datetime # Python에서는 datetime으로 받고, FastAPI가 JSON으로 변환

    class Config:
        populate_by_name = True # alias를 사용하여 필드 이름을 매핑할 수 있도록 설정