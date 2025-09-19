from fastapi import APIRouter, HTTPException
from models.dto import AnalyzeRequest, RepoInfoResponse
from services.repo_service import save_repository_info, get_repository_info

router = APIRouter()

@router.post("/github/repo-info", summary="GitHub 저장소 정보 저장")
def save_repo_info(req: AnalyzeRequest):
    """
    repo_url로부터 GitHub 저장소 정보를 가져와 ArangoDB에 저장
    """
    try:
        saved = save_repository_info(req.repo_url)
        return {
            "message": "Repository info saved successfully",
            "data": saved
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/{repo_id}/info", summary="저장소 메타데이터 조회", response_model=RepoInfoResponse)
def get_repo_info(repo_id: str):
    """
    map_id(repo명)로 저장소 정보 조회
    """
    try:
        repo_info = get_repository_info(repo_id)
        return RepoInfoResponse(**repo_info)
    except Exception as e:
        raise HTTPException(status_code=404, detail=f"Repository info not found: {str(e)}")