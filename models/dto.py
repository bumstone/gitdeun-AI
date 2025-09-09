from pydantic import BaseModel, Field
from typing import Optional

class GeminiLoginRequest(BaseModel):
    api_key: str

class AnalyzeRequest(BaseModel):
    repo_url: str
    mode: str  # DEV or CHK

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

class AnalyzeRequest(BaseModel):
    repo_url: str
    mode: str

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

class SuggestionDetailResponse(BaseModel):
    suggestion_key: str
    repo_url: str
    file_path: str
    prompt: str
    code: str
    summary: Optional[str] = None
    rationale: Optional[str] = None
    created_at: Optional[str] = None