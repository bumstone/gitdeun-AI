from pydantic import BaseModel

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