from arango import ArangoClient
from config import ARANGODB_USERNAME, ARANGODB_PASSWORD, ARANGODB_DB

client = ArangoClient()
db = client.db(ARANGODB_DB, username=ARANGODB_USERNAME, password=ARANGODB_PASSWORD)

def insert_document(collection_name: str, data: dict):
    if not db.has_collection(collection_name):
        if collection_name.endswith("_edges"):
            db.create_collection(collection_name, edge=True)
        else:
            db.create_collection(collection_name)
    collection = db.collection(collection_name)
    return collection.insert(data, overwrite=True)

def document_exists(collection_name: str, key: str) -> bool:
    return db.collection(collection_name).has(key)

def get_documents_by_repo_url_prefix(collection_name: str, prefix: str):
    aql = f"""
    FOR doc IN {collection_name}
        FILTER STARTS_WITH(doc.repo_url, @prefix)
        RETURN doc
    """
    return list(db.aql.execute(aql, bind_vars={"prefix": prefix}))

def get_documents_by_key_prefix(collection_name: str, prefix: str):
    aql = f"""
    FOR doc IN {collection_name}
        FILTER STARTS_WITH(doc._key, @prefix)
        RETURN doc
    """
    return list(db.aql.execute(aql, bind_vars={"prefix": prefix}))

def ensure_mindmap_graph_exists():
    if not db.has_graph("mindmap_graph"):
        graph = db.create_graph("mindmap_graph")
        db.create_collection("mindmap_nodes")
        db.create_collection("mindmap_edges", edge=True)
        graph.create_edge_definition(
            edge_collection="mindmap_edges",
            from_vertex_collections=["mindmap_nodes"],
            to_vertex_collections=["mindmap_nodes"]
        )
    else:
        if not db.has_collection("mindmap_nodes"):
            db.create_collection("mindmap_nodes")
        if not db.has_collection("mindmap_edges"):
            db.create_collection("mindmap_edges", edge=True)
