# services/meeting_service.py
from services.gemini_service import request_gemini
from services.arangodb_service import insert_document

def summarize_meeting(transcript: str) -> str:
    """
    회의록 텍스트를 Gemini로 요약
    """
    return request_gemini(transcript)

def migrate_meeting(meeting_id: str, transcript: str) -> str:
    """
    회의 텍스트를 ArangoDB로 저장
    """
    document = {
        "_key": meeting_id,
        "type": "meeting",
        "transcript": transcript
    }
    insert_document("meeting_notes", document)
    return f"회의록 {meeting_id} ArangoDB에 저장 완료"
