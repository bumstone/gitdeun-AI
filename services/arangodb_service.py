# services/arangodb_service.py
# 변경 핵심:
# - 로컬 디스크 대신 ArangoDB에 "파일 본문(content)"를 저장하는 repo_files 컬렉션 추가
# - repos 메타 컬렉션 추가
# - code_analysis는 그대로 유지(파싱 결과 저장)
# - 공통 insert/get 유틸 유지
# - 인덱스 추가로 조회 성능 보강

from datetime import datetime
from typing import List, Optional

from arango import ArangoClient
from arango.exceptions import AQLQueryExecuteError, ArangoServerError

from config import (
    ARANGODB_HOST, ARANGODB_PORT,
    ARANGODB_USERNAME, ARANGODB_PASSWORD, ARANGODB_DB
)

# 호스트/포트를 환경변수로 받아 원격/로컬 모두 대응
client = ArangoClient(hosts=f"http://{ARANGODB_HOST}:{ARANGODB_PORT}")
db = client.db(ARANGODB_DB, username=ARANGODB_USERNAME, password=ARANGODB_PASSWORD)


def ensure_collections():
    """필요 컬렉션/인덱스 보장 (여러 번 호출해도 안전)"""
    for name in ["repos", "repo_files", "code_analysis", "mindmap_nodes", "mindmap_edges", "code_recommendations"]:
        if not db.has_collection(name):
            if name.endswith("_edges"):
                db.create_collection(name, edge=True)
            else:
                db.create_collection(name)

    # 인덱스 생성(중복 에러는 무시)
    try:
        rf = db.collection("repo_files")
        rf.add_hash_index(["repo_id"])
        rf.add_hash_index(["repo_id", "path"])
    except Exception:
        pass

    try:
        ca = db.collection("code_analysis")
        ca.add_hash_index(["repo_id"])
        ca.add_hash_index(["filename"])
    except Exception:
        pass


def insert_document(collection_name: str, data: dict):
    """공통 insert. overwrite=True로 멱등 처리"""
    ensure_collections()
    if not db.has_collection(collection_name):
        if collection_name.endswith("_edges") or collection_name == "mindmap_edges":
            db.create_collection(collection_name, edge=True)
        else:
            db.create_collection(collection_name)
    collection = db.collection(collection_name)
    return collection.insert(data, overwrite=True)


def document_exists(collection_name: str, key: str) -> bool:
    if not db.has_collection(collection_name):
        return False
    return db.collection(collection_name).has(key)


def get_document_by_key(collection_name: str, key: str):
    if not db.has_collection(collection_name):
        return None
    return db.collection(collection_name).get({"_key": key})


def get_documents_by_repo_url_prefix(collection_name: str, prefix: str):
    aql = f"""
    FOR doc IN {collection_name}
      /* STARTS_WITH(doc.repo_url, @prefix) 대신 LIKE로 대체 */
      FILTER doc.repo_url LIKE CONCAT(@prefix, '%')
      RETURN doc
    """
    return list(db.aql.execute(aql, bind_vars={"prefix": prefix}))



def get_documents_by_key_prefix(collection_name: str, prefix: str):
    aql = f"""
    FOR doc IN {collection_name}
      /* STARTS_WITH(doc._key, @prefix) 대신 LIKE로 대체 */
      FILTER doc._key LIKE CONCAT(@prefix, '%')
      RETURN doc
    """
    return list(db.aql.execute(aql, bind_vars={"prefix": prefix}))



# ---------- 레포/파일 저장 전용 유틸 ----------

def path_key(repo_id: str, path: str) -> str:
    """_key 규칙: repo_id__경로(슬래시는 __로)"""
    return f"{repo_id}__{path.replace('/', '__')}"


def upsert_repo(repo_id: str, repo_url: str, default_branch: str = "main"):
    """레포 메타 upsert"""
    ensure_collections()
    coll = db.collection("repos")
    doc = {
        "_key": repo_id,
        "repo_url": repo_url,
        "default_branch": default_branch,
        "fetched_at": datetime.utcnow().isoformat() + "Z",
    }
    if coll.has(repo_id):
        return coll.update(doc)
    return coll.insert(doc)


def upsert_repo_file(repo_id: str, path: str, language: str, content: str, size: int, sha: Optional[str] = None):
    """파일 본문 저장소. content까지 DB에 저장."""
    ensure_collections()
    coll = db.collection("repo_files")
    key = path_key(repo_id, path)
    doc = {
        "_key": key,
        "repo_id": repo_id,
        "path": path,              # repo 루트 기준 경로
        "language": language,
        "size": size,
        "sha": sha,
        "content": content,        # 🔸 파일 본문
        "fetched_at": datetime.utcnow().isoformat() + "Z",
    }
    if coll.has(key):
        return coll.update(doc)
    return coll.insert(doc)


def get_repo_file_content(repo_id: str, path: str) -> Optional[str]:
    """repo_files에서 본문 바로 조회"""
    ensure_collections()
    key = path_key(repo_id, path)
    if not db.collection("repo_files").has(key):
        return None
    return db.collection("repo_files").get(key).get("content")


def list_repo_files(repo_id: str) -> List[dict]:
    """레포의 파일 목록/메타 조회"""
    ensure_collections()
    aql = """
    FOR f IN repo_files
      FILTER f.repo_id == @repo_id
      RETURN { path: f.path, language: f.language, size: f.size, fetched_at: f.fetched_at }
    """
    return list(db.aql.execute(aql, bind_vars={"repo_id": repo_id}))


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
