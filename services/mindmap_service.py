import hashlib
from arango import ArangoClient
from concurrent.futures import ThreadPoolExecutor
from config import ARANGODB_USERNAME, ARANGODB_PASSWORD, ARANGODB_DB
from services.arangodb_service import insert_document, document_exists

client = ArangoClient()
db = client.db(ARANGODB_DB, username=ARANGODB_USERNAME, password=ARANGODB_PASSWORD)

def generate_node_key(repo_url: str, mode: str, label: str) -> str:
    """
    repo_url + mode + label 조합으로 고유 key 생성 (재실행 시에도 동일 기능이면 동일 key)
    """
    raw = f"{repo_url}_{mode}_{label}".encode("utf-8")
    return hashlib.md5(raw).hexdigest()[:12]

def save_mindmap_nodes_recursively(repo_url: str, mode: str, node: dict, parent_key: str = None):
    node_label = node.get("node")
    children = node.get("children", [])
    related_files = node.get("related_files", [])

    # 고유 node_key 생성
    node_key = generate_node_key(repo_url, mode, node_label)

    # mindmap_nodes 저장
    if not document_exists("mindmap_nodes", node_key):
        insert_document("mindmap_nodes", {
            "_key": node_key,
            "repo_url": repo_url,
            "mode": mode,
            "label": node_label,
            "related_files": related_files or []
        })

    # mindmap_edges 저장
    if parent_key:
        edge_key = f"{parent_key}__{node_key}".replace("/", "_")
        if not document_exists("mindmap_edges", edge_key):
            insert_document("mindmap_edges", {
                "_key": edge_key,
                "_from": f"mindmap_nodes/{parent_key}",
                "_to": f"mindmap_nodes/{node_key}"
            })

    # 자식 노드 재귀 저장 (병렬 처리)
    with ThreadPoolExecutor() as executor:
        futures = []
        for child in children:
            futures.append(executor.submit(
                save_mindmap_nodes_recursively,
                repo_url, mode, child, parent_key=node_key
            ))
        for future in futures:
            try:
                future.result()
            except Exception as e:
                print(f"❗ Error saving child node: {e}")

def get_mindmap_graph(repo_url: str):
    """
    repo_url 기반으로 mindmap_edges와 mindmap_nodes를 조인해서
    프론트에서 바로 사용 가능한 마인드맵 그래프 데이터 반환
    """
    aql = """
    FOR edge IN mindmap_edges
        LET from_node = DOCUMENT(edge._from)
        LET to_node = DOCUMENT(edge._to)
        FILTER from_node != null AND from_node.repo_url == @repo_url
        RETURN {
            from_key: PARSE_IDENTIFIER(edge._from).key,
            from_label: from_node.label,
            from_related_files: from_node.related_files,
            to_key: PARSE_IDENTIFIER(edge._to).key,
            to_label: to_node.label,
            to_related_files: to_node.related_files
        }
    """
    cursor = db.aql.execute(aql, bind_vars={"repo_url": repo_url})
    edges = list(cursor)

    # 노드 목록 추출 (중복 제거)
    nodes_dict = {}
    for e in edges:
        if e["from_key"] not in nodes_dict:
            nodes_dict[e["from_key"]] = {
                "key": e["from_key"],
                "label": e["from_label"],
                "related_files": e.get("from_related_files", [])
            }
        if e["to_key"] not in nodes_dict:
            nodes_dict[e["to_key"]] = {
                "key": e["to_key"],
                "label": e["to_label"],
                "related_files": e.get("to_related_files", [])
            }

    return {
        "nodes": list(nodes_dict.values()),
        "edges": [
            {
                "from": e["from_key"],
                "to": e["to_key"]
            } for e in edges
        ]
    }

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

    db.collection("mindmap_nodes")
    db.collection("mindmap_edges")
