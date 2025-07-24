# routers/github.py

from fastapi import APIRouter, Path
from models.dto import CodeParseRequest
from services.github_service import parse_code_by_language, save_parsed_code_to_arango

router = APIRouter()

@router.post("/parse-and-save/{repo_id}")
def parse_and_save_code(repo_id: str, payload: CodeParseRequest):
    parse_result = parse_code_by_language(payload.language, payload.code)
    save_result = save_parsed_code_to_arango(
        repo_id=repo_id,
        filename=payload.filename,
        language=payload.language,
        parse_result=parse_result
    )
    return {"result": save_result, "parsed": parse_result}
