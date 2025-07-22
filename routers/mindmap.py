from fastapi import APIRouter, Path
from models.dto import AnalyzeRequest
from services.arangodb_service import create_mindmap_node, get_mindmap_nodes

router = APIRouter()

@router.post("/analyze", summary="코드 분석 및 마인드맵 노드 저장", description="GitHub 저장소 URL과 분석 모드를 받아 마인드맵 노드를 생성합니다.")
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
