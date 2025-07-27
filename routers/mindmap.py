from fastapi import APIRouter, Path
from models.dto import AnalyzeRequest
from services.arangodb_service import insert_document, get_all_documents, get_documents_by_prefix


router = APIRouter()

@router.post("/analyze", summary="코드 분석 및 마인드맵 노드 저장", description="GitHub 저장소 URL과 분석 모드를 받아 마인드맵 노드를 생성합니다.")
def analyze_code(req: AnalyzeRequest):
    # 예시로 ArangoDB에 저장
    data = {
        "repo_url": req.repo_url,
        "mode": req.mode,
        "node": "Example Root Node"
    }
    return insert_document("mindmap_nodes", data)

@router.get("/{repo_id}")
def get_mindmap(repo_id: str = Path(...)):
    return get_all_documents("mindmap_nodes")

def create_mindmap_node(repo_url: str, mode: str, parsed_result: dict):
    return insert_document("mindmap_nodes", {
        "repo_url": repo_url,
        "mode": mode,
        "node": parsed_result  # ✅ 실제 분석 결과 저장
    })

@router.get("/code-analysis/{repo_id}", summary="저장된 코드 분석 결과 조회")
def get_code_analysis(repo_id: str):
    return get_documents_by_prefix("code_analysis", f"{repo_id}_")