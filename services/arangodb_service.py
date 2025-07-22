# services/arangodb_service.py
from database.arangodb_client import db

def create_mindmap_node(collection_name: str, data: dict):
    """
    컬렉션에 노드 삽입. 존재하지 않으면 생성함.
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
    컬렉션 내 모든 노드 반환
    """
    try:
        if not db.has_collection(collection_name):
            return {"error": "Collection not found"}
        collection = db.collection(collection_name)
        return list(collection.all())
    except Exception as e:
        return {"error": str(e)}
