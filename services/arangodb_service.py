# services/arangodb_service.py
# 변경 핵심:
# - 로컬 디스크 대신 ArangoDB에 "파일 본문(content)"를 저장하는 repo_files 컬렉션 추가
# - repos 메타 컬렉션 추가
# - code_analysis는 그대로 유지(파싱 결과 저장)
# - 공통 insert/get 유틸을 안전한 upsert 형태로 개선(409 방지)
# - STARTS_WITH/ENDSWITH 미지원 환경을 위해 LIKE/CONCAT 사용
# - 인덱스 추가로 조회 성능 보강
# - ✅ ARANGODB_URL(.env) 우선 적용: ngrok/https 등 외부 터널 URL을 그대로 사용

from datetime import datetime
from typing import List, Optional
import logging
import os

from arango import ArangoClient
from arango.exceptions import (
    AQLQueryExecuteError,
    ArangoServerError,
    DocumentInsertError,
)

from config import (
    ARANGODB_HOST, ARANGODB_PORT,
    ARANGODB_USERNAME, ARANGODB_PASSWORD, ARANGODB_DB
)

# -----------------------------------
# 클라이언트 초기화 (ARANGODB_URL 우선)
# -----------------------------------
ARANGODB_URL = os.getenv("ARANGODB_URL")  # 예: https://xxxx.ngrok-free.app
EFFECTIVE_HOSTS = ARANGODB_URL or f"http://{ARANGODB_HOST}:{ARANGODB_PORT}"

logging.getLogger().setLevel("INFO")
logging.info(
    f"[ARANGO CONF] hosts={EFFECTIVE_HOSTS} user={ARANGODB_USERNAME} db={ARANGODB_DB}"
)

# hosts 에는 http://host:port 또는 https://도 가능
client = ArangoClient(hosts=EFFECTIVE_HOSTS)
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
    """
    공통 insert → 안전한 upsert.
    - 같은 _key 재삽입 시 409가 나지 않도록 선확인 + overwrite_mode="replace" 적용
    - 동시성으로 1210이 발생하면 update로 마무리
    """
    ensure_collections()
    if not db.has_collection(collection_name):
        if collection_name.endswith("_edges") or collection_name == "mindmap_edges":
            db.create_collection(collection_name, edge=True)
        else:
            db.create_collection(collection_name)

    collection = db.collection(collection_name)

    # _key가 있고 이미 존재하면 update 멱등 처리
    if "_key" in data and collection.has(data["_key"]):
        return collection.update(data)

    try:
        # 일부 버전에선 overwrite=True만으로 replace 보장이 안 됨 → overwrite_mode 명시
        return collection.insert(
            data,
            overwrite=True,
            overwrite_mode="replace",  # 핵심!
        )
    except DocumentInsertError as e:
        # 드물게 경합으로 unique constraint가 다시 나면 update로 마무리
        if "unique constraint violated" in str(e) and "_key" in data and collection.has(data["_key"]):
            return collection.update(data)
        raise


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

def get_repo_url_by_id(map_id: str) -> Optional[str]:
    """repos 컬렉션에서 repo_url 조회"""
    ensure_collections()
    if not db.has_collection("repos"):
        return None
    doc = db.collection("repos").get(map_id)
    return doc.get("repo_url") if doc else None

def delete_mindmap(map_id: str, also_recommendations: bool = True) -> dict:
    """해당 map_id의 마인드맵(노드/엣지) 제거. 선택적으로 code_recommendations도 정리."""
    ensure_collections()
    aql = """
    LET e = (FOR x IN mindmap_edges FILTER x.map_id == @map_id REMOVE x IN mindmap_edges RETURN 1)
    LET n = (FOR x IN mindmap_nodes FILTER x.map_id == @map_id REMOVE x IN mindmap_nodes RETURN 1)
    LET r = (
      FOR x IN code_recommendations
        FILTER x.map_id == @map_id
        REMOVE x IN code_recommendations
        RETURN 1
    )
    RETURN {
      edges_removed: LENGTH(e),
      nodes_removed: LENGTH(n),
      recs_removed: LENGTH(r)
    }
    """
    # also_recommendations=false면 recs는 세지지 않도록 별도 분기
    if not also_recommendations:
        aql = """
        LET e = (FOR x IN mindmap_edges FILTER x.map_id == @map_id REMOVE x IN mindmap_edges RETURN 1)
        LET n = (FOR x IN mindmap_nodes FILTER x.map_id == @map_id REMOVE x IN mindmap_nodes RETURN 1)
        RETURN { edges_removed: LENGTH(e), nodes_removed: LENGTH(n), recs_removed: 0 }
        """
    res = list(db.aql.execute(aql, bind_vars={"map_id": map_id}))
    return res[0] if res else {"edges_removed": 0, "nodes_removed": 0, "recs_removed": 0}

def ensure_collections():
    """필요 컬렉션/인덱스 보장 (여러 번 호출해도 안전)"""
    for name in ["repos", "repo_files", "code_analysis", "mindmap_nodes", "mindmap_edges",
                 "code_recommendations", "mindmap_prompts"]:
        if not db.has_collection(name):
            if name.endswith("_edges"):
                db.create_collection(name, edge=True)
            else:
                db.create_collection(name)

    try:
        rf = db.collection("repo_files")
        rf.add_hash_index(["repo_id"])
        rf.add_hash_index(["repo_id", "path"])
    except Exception: pass

    try:
        ca = db.collection("code_analysis")
        ca.add_hash_index(["repo_id"])
        ca.add_hash_index(["filename"])
    except Exception: pass

    try:
        mp = db.collection("mindmap_prompts")
        mp.add_hash_index(["mindmap_id"])
        mp.add_hash_index(["idempotency_key"])
        mp.add_persistent_index(["created_at"])
    except Exception: pass


# -------------------- 프롬프트 히스토리 --------------------

def insert_prompt_doc(doc: dict) -> str:
    """프롬프트 기록을 남기고 _key를 반환"""
    ensure_collections()
    import hashlib, time
    coll = db.collection("mindmap_prompts")
    idem = (doc.get("idempotency_key") or "")[:48]
    if idem:
        # 같은 idempotency_key가 있으면 재사용
        cursor = db.aql.execute("""
          FOR p IN mindmap_prompts
            FILTER p.idempotency_key == @k
            LIMIT 1
            RETURN p
        """, bind_vars={"k": idem})
        exist = next(iter(cursor), None)
        if exist:
            return exist["_key"]

    raw = f"{doc.get('mindmap_id','')}:{time.time_ns()}".encode()
    key = "p_" + hashlib.md5(raw).hexdigest()[:24]
    to_insert = {
        "_key": key,
        "mindmap_id": doc.get("mindmap_id"),
        "prompt": doc.get("prompt"),
        "mode": doc.get("mode"),
        "target_nodes": doc.get("target_nodes"),
        "related_files": doc.get("related_files"),
        "ai_summary": doc.get("ai_summary"),
        "status": doc.get("status", "SUCCEEDED"),
        "idempotency_key": idem or None,
        "created_at": datetime.utcnow().isoformat() + "Z",
        "updated_at": datetime.utcnow().isoformat() + "Z",
    }
    coll.insert(to_insert)
    return key

def get_prompt_doc(mindmap_id: str, prompt_id: Optional[str]) -> Optional[dict]:
    ensure_collections()
    coll = db.collection("mindmap_prompts")
    if prompt_id:
        if coll.has(prompt_id):
            return coll.get(prompt_id)
        return None
    # 최신 프롬프트 1건
    cursor = db.aql.execute("""
      FOR p IN mindmap_prompts
        FILTER p.mindmap_id == @mid
        SORT DATE_TIMESTAMP(p.created_at) DESC
        LIMIT 1
        RETURN p
    """, bind_vars={"mid": mindmap_id})
    return next(iter(cursor), None)

def upsert_prompt_title(prompt_id: str, title: str, summary: str):
    ensure_collections()
    coll = db.collection("mindmap_prompts")
    if coll.has(prompt_id):
        doc = coll.get(prompt_id)
        doc["title"] = title
        doc["ai_summary"] = summary
        doc["updated_at"] = datetime.utcnow().isoformat() + "Z"
        coll.update(doc)


# -------------------- 그래프 업서트 (확장용) --------------------

def upsert_nodes_edges(map_id: str, nodes: list[dict], edges: list[dict], default_mode: str = "FEATURE") -> list[str]:
    """
    nodes: [{key?, label, meta?}]  key 없으면 label 기반으로 생성 추천 X → 여기선 필수로 간주
    edges: [{from, to, type?}]
    반환: 변경/신규 노드 키 리스트
    """
    ensure_collections()
    changed: list[str] = []

    ncoll = db.collection("mindmap_nodes")
    ecoll = db.collection("mindmap_edges")

    for n in nodes:
        key = n.get("key")
        label = n.get("label") or key
        if not key:
            # 키가 없다면 label을 키로 쓰되 해시화 권장 – 여기선 안전하게 해시
            import hashlib
            raw = f"{map_id}:{default_mode}:{label}".encode("utf-8")
            key = hashlib.md5(raw).hexdigest()[:12]

        doc = {
            "_key": key,
            "map_id": map_id,
            "label": label,
            "related_files": (n.get("meta", {}) or {}).get("files", []),
            "mode": (n.get("meta", {}) or {}).get("mode", default_mode),
            "node_type": (n.get("meta", {}) or {}).get("node_type")
        }
        if ncoll.has(key):
            ncoll.update(doc)
        else:
            ncoll.insert(doc)
        changed.append(key)

    for e in edges:
        fr = e.get("from") or e.get("from_")
        to = e.get("to")
        et = e.get("type", "RELATES_TO")
        if not fr or not to:
            continue
        edge_key = f"{fr}__{to}__{et}".replace("/", "_")
        doc = {
            "_key": edge_key,
            "map_id": map_id,
            "_from": f"mindmap_nodes/{fr}",
            "_to": f"mindmap_nodes/{to}",
            "edge_type": et
        }
        if ecoll.has(edge_key):
            ecoll.update(doc)
        else:
            ecoll.insert(doc)

    return list(set(changed))