from fastapi import APIRouter, HTTPException
from models.dto import AnalyzeRequest
from services.arangodb_service import get_documents_by_key_prefix, get_repo_file_content, db
from services.mindmap_service import (
    save_mindmap_nodes_recursively,
    get_mindmap_graph,
    derive_map_id,
)

router = APIRouter()

@router.post("/{map_id}/analyze", summary="마인드맵 수동 생성 (map 스코프)")
def analyze_code(map_id: str, req: AnalyzeRequest):
    # 데모용 트리
    parsed_result = {
        "node": "Answer",
        "children": [
            {"node": "delete", "children": []},
            {"node": "suspend", "children": []},
            {"node": "getDisplayContent", "children": []}
        ]
    }
    try:
        # repo_url이 왔다면 map_id와 일치 검증(옵션)
        if req.repo_url and derive_map_id(req.repo_url) != map_id:
            raise HTTPException(status_code=400, detail="map_id and repo_url mismatch")

        # repo_url은 기록용으로만 사용(없어도 됨)
        save_mindmap_nodes_recursively(req.repo_url or map_id, req.mode, parsed_result, map_id=map_id)
        return {"message": "저장 완료", "map_id": map_id}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))



@router.post("/analyze-ai", summary="Gemini로 마인드맵 생성 (빠른/간결, repo_url 기준)")
def analyze_ai_code(req: AnalyzeRequest):
    from services.gemini_service import summarize_directory_code

    try:
        # repo_id = 레포 마지막 세그먼트
        repo_id = req.repo_url.rstrip("/").split("/")[-1]

        # ★ code_analysis의 _key 프리픽스만 사용 → 매우 빠름
        files = get_documents_by_key_prefix("code_analysis", f"{repo_id}_")
        if not files:
            # 길게 안 돌리고 바로 짧게 리턴
            return {"message": "code_analysis 비어있음(0개) — 먼저 적재하세요.", "repo_id": repo_id}

        # 디렉터리 묶기 (응답엔 아무것도 안 넣음)
        grouped_files = {}
        for f in files:
            path = f.get("path", "unknown")
            dir_name = "/".join(path.split("/")[:-1]) or "root"
            grouped_files.setdefault(dir_name, []).append((path, f.get("content", "")))

        # 디렉터리별 요약 → 노드 저장 (짧은 결과만 반환)
        saved_count = 0
        for dir_name, blocks in grouped_files.items():
            result = summarize_directory_code(dir_name, blocks)
            if "error" in result:
                # 실패 디렉터리는 건너뜀 (응답을 길게 만들지 않음)
                continue
            save_mindmap_nodes_recursively(req.repo_url, req.mode, result)  # map_id는 내부에서 자동 도출
            saved_count += 1

        return {"message": f"{saved_count}개 디렉터리 분석 완료", "repo_id": repo_id}

    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@router.get("/{map_id}/graph", summary="특정 맵 그래프 반환")
def graph(map_id: str):
    try:
        data = get_mindmap_graph(map_id)
        return {
            "map_id": map_id,
            "count": len(data["nodes"]),
            "nodes": data["nodes"],
            "edges": data["edges"]
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
