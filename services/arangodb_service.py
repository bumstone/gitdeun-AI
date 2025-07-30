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

# repo_url 필터용
def get_documents_by_repo_url_prefix(collection_name: str, prefix: str):
    aql = f"""
    FOR doc IN {collection_name}
        FILTER STARTS_WITH(doc.repo_url, @prefix)
        RETURN doc
    """
    return list(db.aql.execute(aql, bind_vars={"prefix": prefix}))

# _key 필터용 (원래대로)
def get_documents_by_key_prefix(collection_name: str, prefix: str):
    aql = f"""
    FOR doc IN {collection_name}
        FILTER STARTS_WITH(doc._key, @prefix)
        RETURN doc
    """
    return list(db.aql.execute(aql, bind_vars={"prefix": prefix}))


def save_mindmap_graph(json_block: dict):
    client = ArangoClient()
    db = client.db("your_db_name", username="root", password="your_password")

    # 보장: Graph + Collections 생성
    if not db.has_graph("mindmap_graph"):
        graph = db.create_graph("mindmap_graph")
        if not db.has_collection("mindmap_nodes"):
            db.create_collection("mindmap_nodes")
        if not db.has_collection("mindmap_edges"):
            db.create_collection("mindmap_edges", edge=True)
        graph.create_edge_definition(
            edge_collection="mindmap_edges",
            from_vertex_collections=["mindmap_nodes"],
            to_vertex_collections=["mindmap_nodes"],
        )
    else:
        graph = db.graph("mindmap_graph")

    nodes = db.collection("mindmap_nodes")
    edges = db.collection("mindmap_edges")

    def insert_recursive(parent: str, children: list):
        for child in children:
            key = child if isinstance(child, str) else child["node"]
            label = key
            _key = key.replace(" ", "_").replace("(", "").replace(")", "").replace(".", "_")

            if not nodes.has(_key):
                nodes.insert({"_key": _key, "label": label}, overwrite=True)

            edges.insert({
                "_from": f"mindmap_nodes/{parent}",
                "_to": f"mindmap_nodes/{_key}",
                "relation": "has_child"
            }, overwrite=True)

            if isinstance(child, dict) and "children" in child:
                insert_recursive(_key, child["children"])

    root_key = json_block["node"].replace(" ", "_").replace(".", "_")
    if not nodes.has(root_key):
        nodes.insert({"_key": root_key, "label": json_block["node"]}, overwrite=True)

    insert_recursive(root_key, json_block.get("children", []))