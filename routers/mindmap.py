# routers/mindmap_router.py
from fastapi import APIRouter, Path, HTTPException
from models.dto import AnalyzeRequest
from services.arangodb_service import get_documents_by_repo_url_prefix, insert_document, get_documents_by_key_prefix
from services.mindmap_service import save_mindmap_nodes_recursively
from services.gemini_service import summarize_code
router = APIRouter()

@router.post("/analyze", summary="코드 분석 및 마인드맵 노드 저장")
def analyze_code(req: AnalyzeRequest):
    """
    GitHub 저장소 URL과 분석 모드를 받아 마인드맵 노드를 생성합니다.
    실제 분석 결과는 /github/analyze API를 통해 이미 ArangoDB에 저장된다고 가정합니다.
    이 API는 예시용으로, 분석 결과를 수동으로 전달받아 마인드맵으로 저장합니다.
    """
    # [예시 분석 결과] 실제로는 외부 분석기에서 받아온 JSON 데이터
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
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"마인드맵 저장 중 오류 발생: {str(e)}")

    return {"message": "마인드맵 노드가 성공적으로 저장되었습니다."}


@router.get("/{repo_url:path}", summary="마인드맵 노드 조회")
def get_mindmap(repo_url: str = Path(..., description="GitHub 저장소 URL")):
    """
    repo_url 전체 문자열로 mindmap_nodes 문서를 조회 (e.g., https://github.com/user/repo)
    """
    try:
        docs = get_documents_by_repo_url_prefix("mindmap_nodes", prefix=repo_url)
        return {"count": len(docs), "nodes": docs}
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"조회 중 오류 발생: {str(e)}")


@router.post("/analyze-ai", summary="Gemini로 마인드맵 자동 생성")
def analyze_ai_code(req: AnalyzeRequest):
    """
    1. code_analysis에서 전체 코드 가져오기
    2. Gemini로 기능 요약
    3. 마인드맵 노드 저장
    """
    try:
        repo_id = req.repo_url.rstrip("/").split("/")[-1]
        files = get_documents_by_key_prefix("code_analysis", f"{repo_id}_")

        saved_count = 0
        for f in files:
            code = f.get("content", "")
            if not code.strip():
                continue

            result = summarize_code(code)
            if "error" in result:
                print(f"Gemini 요약 실패: {result['error']}")
                continue

            # children 배열을 children 노드들로 변환
            children = result.get("children", [])
            if isinstance(children, list):
                result["children"] = [{"node": c, "children": []} for c in children]

            save_mindmap_nodes_recursively(req.repo_url, req.mode, result)
            saved_count += 1

        return {"message": f"총 {saved_count}개 마인드맵 노드를 생성했습니다."}

    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
