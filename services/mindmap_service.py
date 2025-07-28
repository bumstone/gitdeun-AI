# services/arangodb_service.py
from database.arangodb_client import db  # 여기서 DB 연결 객체만 import

def create_mindmap_node(collection_name: str, data: dict):
    """
    마인드맵 노드를 ArangoDB에 삽입
    """
    try:
        if not db.has_collection(collection_name):
            db.create_collection(collection_name)
        collection = db.collection(collection_name)
        return collection.insert(data)
    except Exception as e:
        return {"error": str(e)}

def get_mindmap_nodes(collection_name: str):
    """
    특정 컬렉션의 모든 노드를 조회
    """
    try:
        if not db.has_collection(collection_name):
            return {"error": "Collection not found"}
        collection = db.collection(collection_name)
        return list(collection.all())
    except Exception as e:
        return {"error": str(e)}

