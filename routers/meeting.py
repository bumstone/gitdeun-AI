from fastapi import APIRouter
from models.dto import MeetingSummaryRequest, MeetingMigrateRequest
from services.meeting_service import summarize_meeting, migrate_meeting

router = APIRouter()

@router.post("/summary", summary="회의록 요약", description="회의 텍스트를 받아 Gemini 모델로 요약한 결과를 반환합니다.")
def meeting_summary(req: MeetingSummaryRequest):
    summary = summarize_meeting(req.transcript)
    return {"meeting_id": req.meeting_id, "summary": summary}

@router.post("/migrate", summary="회의록 ArangoDB 이관", description="MySQL에서 받아온 회의록을 ArangoDB로 이관합니다.")
def meeting_migrate(req: MeetingMigrateRequest):
    result = migrate_meeting(req.meeting_id)
    return {"message": result}
