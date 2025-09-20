# services/mindmap_service.py
import hashlib
from concurrent.futures import ThreadPoolExecutor
from services.arangodb_service import db, insert_document, document_exists

def derive_map_id(repo_url: str) -> str:
    if not repo_url:
        return "default"
    return repo_url.rstrip("/").split("/")[-1]

def generate_node_key(map_id: str, label: str) -> str:
    raw = f"{map_id}_{label}".encode("utf-8")
    return hashlib.md5(raw).hexdigest()[:12]

def ensure_mindmap_indexes():
    try:
        mn = db.collection("mindmap_nodes"); mn.add_hash_index(["map_id"])
    except Exception: pass
    try:
        me = db.collection("mindmap_edges"); me.add_hash_index(["map_id"])
    except Exception: pass

def save_mindmap_nodes_recursively(
    repo_url: str, node: dict,
    parent_key: str | None = None, map_id: str | None = None,
):
    ensure_mindmap_indexes()
    map_id = map_id or derive_map_id(repo_url)
    node_label = node.get("node")
    if not node_label:
        return

    children = node.get("children", [])
    related_files = node.get("related_files", [])
    node_key = generate_node_key(map_id, node_label)

    if not document_exists("mindmap_nodes", node_key):
        insert_document("mindmap_nodes", {
            "_key": node_key, "map_id": map_id,
            "repo_url": repo_url, "label": node_label, "related_files": related_files or []
        })

    if parent_key:
        edge_key = f"{parent_key}__{node_key}".replace("/", "_")
        if not document_exists("mindmap_edges", edge_key):
            insert_document("mindmap_edges", {
                "_key": edge_key, "map_id": map_id,
                "_from": f"mindmap_nodes/{parent_key}",
                "_to": f"mindmap_nodes/{node_key}",
                "relation": "contains"
            })

    with ThreadPoolExecutor(max_workers=4) as ex:  # 병렬 제한(경고 억제)
        futs = [ex.submit(save_mindmap_nodes_recursively,
                          repo_url, c, node_key, map_id)
                for c in children]
        for f in futs:
            f.result()

def get_mindmap_graph(map_id: str):
    """프론트가 제안/일반 노드를 구분 렌더링할 수 있도록 타입 필드 포함"""
    ensure_mindmap_indexes()
    edges_raw = list(db.aql.execute("""
      FOR e IN mindmap_edges
        FILTER e.map_id == @map_id
        RETURN { _from: e._from, _to: e._to, edge_type: e.edge_type }
    """, bind_vars={"map_id": map_id}))

    node_keys = set()
    for e in edges_raw:
        try:
            node_keys.add(e["_from"].split("/", 1)[1])
            node_keys.add(e["_to"].split("/", 1)[1])
        except Exception:
            pass

    nodes_list = []
    if node_keys:
        nodes_list = list(db.aql.execute("""
          FOR n IN mindmap_nodes
            FILTER n.map_id == @map_id AND n._key IN @keys
            RETURN {
              key: n._key,
              label: n.label,
              related_files: n.related_files,
              node_type: n.node_type
            }
        """, bind_vars={"map_id": map_id, "keys": list(node_keys)}))

    nodes_dict = {n["key"]: n for n in nodes_list}
    edges = []
    for e in edges_raw:
        try:
            edges.append({
                "from": e["_from"].split("/", 1)[1],
                "to": e["_to"].split("/", 1)[1],
                "edge_type": e.get("edge_type")
            })
        except Exception:
            continue

    return {"nodes": list(nodes_dict.values()), "edges": edges}

def save_mindmap_graph():
    """
    최초 실행 시 mindmap_graph 및 관련 컬렉션 생성
    """
    if not db.has_graph("mindmap_graph"):
        graph = db.create_graph("mindmap_graph")
        if not db.has_collection("mindmap_nodes"):
            db.create_collection("mindmap_nodes")
        if not db.has_collection("mindmap_edges"):
            db.create_collection("mindmap_edges", edge=True)
        graph.create_edge_definition(
            edge_collection="mindmap_edges",
            from_vertex_collections=["mindmap_nodes"],
            to_vertex_collections=["mindmap_nodes"]
        )
    else:
        graph = db.graph("mindmap_graph")

    # 인덱스 보장
    ensure_mindmap_indexes()
