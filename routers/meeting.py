from fastapi import APIRouter
from models.dto import MeetingSummaryRequest, MeetingMigrateRequest
from services.meeting_service import summarize_meeting, migrate_meeting

router = APIRouter()

@router.post("/summary")
def meeting_summary(req: MeetingSummaryRequest):
    summary = summarize_meeting(req.transcript)
    return {"meeting_id": req.meeting_id, "summary": summary}

@router.post("/migrate")
def meeting_migrate(req: MeetingMigrateRequest):
    result = migrate_meeting(req.meeting_id)
    return {"message": result}
