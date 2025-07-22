from fastapi import APIRouter, Path
from models.dto import AnalyzeRequest
from services.arangodb_service import create_mindmap_node, get_mindmap_nodes

router = APIRouter()

@router.post("/analyze")
def analyze_code(req: AnalyzeRequest):
    # 예시로 ArangoDB에 저장
    data = {
        "repo_url": req.repo_url,
        "mode": req.mode,
        "node": "Example Root Node"
    }
    return create_mindmap_node("mindmap_nodes", data)

@router.get("/{repo_id}")
def get_mindmap(repo_id: str = Path(...)):
    return get_mindmap_nodes("mindmap_nodes")
