from fastapi import APIRouter, Path, HTTPException
from models.dto import AnalyzeRequest
from services.arangodb_service import get_documents_by_repo_url_prefix, insert_document, get_documents_by_key_prefix
from services.mindmap_service import save_mindmap_nodes_recursively, get_mindmap_graph
from services.gemini_service import summarize_directory_code

router = APIRouter()

@router.post("/analyze", summary="마인드맵 수동 생성")
def analyze_code(req: AnalyzeRequest):
    parsed_result = {
        "node": "Answer",
        "children": [
            {"node": "delete", "children": []},
            {"node": "suspend", "children": []},
            {"node": "getDisplayContent", "children": []}
        ]
    }
    try:
        save_mindmap_nodes_recursively(req.repo_url, req.mode, parsed_result)
        return {"message": "저장 완료"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

# @router.get("/{repo_url:path}", summary="마인드맵 노드 조회")
# def get_mindmap(repo_url: str = Path(...)):
#     try:
#         docs = get_documents_by_repo_url_prefix("mindmap_nodes", prefix=repo_url)
#         return {"count": len(docs), "nodes": docs}
#     except Exception as e:
#         raise HTTPException(status_code=500, detail=str(e))

@router.post("/analyze-ai", summary="Gemini로 마인드맵 생성")
def analyze_ai_code(req: AnalyzeRequest):
    try:
        repo_id = req.repo_url.rstrip("/").split("/")[-1]
        files = get_documents_by_key_prefix("code_analysis", f"{repo_id}_")

        grouped_files = {}
        for f in files:
            path = f.get("path", "unknown")
            dir_name = "/".join(path.split("/")[:-1]) or "root"
            grouped_files.setdefault(dir_name, []).append((path, f.get("content", "")))

        saved_count = 0
        for dir_name, blocks in grouped_files.items():
            result = summarize_directory_code(dir_name, blocks)
            if "error" in result:
                print(f"Gemini 실패: {result['error']}")
                continue
            save_mindmap_nodes_recursively(req.repo_url, req.mode, result)
            saved_count += 1

        return {"message": f"{saved_count}개 디렉터리 분석 완료"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@router.get("/graph")
def get_graph(repo_url: str):
    try:
        repo_url = repo_url.rstrip("/")  # <- 이 줄 꼭 추가!
        graph_data = get_mindmap_graph(repo_url)
        return {
            "count": len(graph_data["nodes"]),
            "nodes": graph_data["nodes"],
            "edges": graph_data["edges"]
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))