# services/mindmap_service.py
from services.arangodb_service import insert_document

def save_mindmap_nodes_recursively(repo_url: str, mode: str, node: dict):
    """
    마인드맵 노드를 계층적으로 ArangoDB에 저장.
    :param repo_url: GitHub 저장소 URL
    :param mode: 분석 모드 (예: DEV, CHK)
    :param node: 현재 노드 (예: {"node": "Answer", "children": [...]})
    """
    node_name = node.get("node")
    children = node.get("children", [])

    doc = {
        "repo_url": repo_url,
        "mode": mode,
        "node": node_name,
        "children": [child.get("node") for child in children]
    }

    insert_document("mindmap_nodes", doc)

    for child in children:
        save_mindmap_nodes_recursively(repo_url, mode, child)
