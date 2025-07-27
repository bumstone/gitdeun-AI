# services/arangodb_service.py
from arango import ArangoClient
from config import ARANGODB_USERNAME, ARANGODB_PASSWORD, ARANGODB_DB
from database.arangodb_client import db

def get_db():
    client = ArangoClient()
    return client.db(
        name=ARANGODB_DB,
        username=ARANGODB_USERNAME,
        password=ARANGODB_PASSWORD
    )

def insert_document(collection_name: str, data: dict):
    """
    컬렉션에 도큐먼트 삽입. 컬렉션 없으면 생성.
    """
    try:
        if not db.has_collection(collection_name):
            db.create_collection(collection_name)
        collection = db.collection(collection_name)
        return collection.insert(data)
    except Exception as e:
        return {"error": str(e)}

def get_all_documents(collection_name: str):
    """
    컬렉션의 모든 도큐먼트 반환
    """
    try:
        if not db.has_collection(collection_name):
            return {"error": "Collection not found"}
        collection = db.collection(collection_name)
        return list(collection.all())
    except Exception as e:
        return {"error": str(e)}

def get_documents_by_prefix(collection_name: str, prefix: str):
    try:
        if not db.has_collection(collection_name):
            return []
        aql = f"""
        FOR doc IN {collection_name}
            FILTER STARTS_WITH(doc._key, @prefix)
            RETURN doc
        """
        return list(db.aql.execute(aql, bind_vars={"prefix": prefix}))
    except Exception as e:
        return {"error": str(e)}

