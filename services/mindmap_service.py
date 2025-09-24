import hashlib
import re
from concurrent.futures import ThreadPoolExecutor
from typing import Any, Dict, List, Optional

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
        mn = db.collection("mindmap_nodes")
        mn.add_hash_index(["map_id"])
    except Exception:
        pass
    try:
        me = db.collection("mindmap_edges")
        me.add_hash_index(["map_id"])
    except Exception:
        pass


def save_mindmap_nodes_recursively(
    repo_url: str,
    node: dict,
    parent_key: str | None = None,
    map_id: str | None = None,
    *,
    parallel: bool = False,         # 기본은 순차 저장(풀 고갈 방지)
    max_workers: int = 2,           # 병렬 필요시에도 저동시성만 허용
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
        insert_document(
            "mindmap_nodes",
            {
                "_key": node_key,
                "map_id": map_id,
                "repo_url": repo_url,
                "label": node_label,
                "related_files": related_files or [],
            },
        )

    if parent_key:
        edge_key = f"{parent_key}__{node_key}".replace("/", "_")
        if not document_exists("mindmap_edges", edge_key):
            insert_document(
                "mindmap_edges",
                {
                    "_key": edge_key,
                    "map_id": map_id,
                    "_from": f"mindmap_nodes/{parent_key}",
                    "_to": f"mindmap_nodes/{node_key}",
                    "edge_type": "contains",
                },
            )

    # 기본: 순차 저장 (연결풀 고갈/경합 방지)
    if not parallel:
        for c in children:
            save_mindmap_nodes_recursively(
                repo_url, c, node_key, map_id, parallel=False, max_workers=max_workers
            )
        return

    # 옵션: 병렬 저장(필요할 때만, 저동시성)
    with ThreadPoolExecutor(max_workers=max_workers) as ex:
        futs = [
            ex.submit(
                save_mindmap_nodes_recursively,
                repo_url, c, node_key, map_id, parallel=True, max_workers=max_workers
            )
            for c in children
        ]
        for f in futs:
            f.result()


# 추가: 파일 경로 정규화/추정 유틸

def _build_repo_lookup(map_id: str) -> Dict[str, Dict[str, Any]]:
    rows = list(
        db.aql.execute(
            """
      FOR rf IN repo_files
        FILTER rf.repo_id == @repo_id AND rf.path != null
        LET fname = rf.filename != null ? rf.filename : LAST(SPLIT(rf.path, "/"))
        RETURN { filename: fname, path: rf.path, language: rf.language, size: rf.size, blob_sha: rf.blob_sha }
    """,
            bind_vars={"repo_id": map_id},
        )
    )
    lookup: Dict[str, Dict[str, Any]] = {}
    for r in rows:
        fn = r.get("filename")
        if not fn:
            continue
        lookup[fn] = {
            "path": r.get("path"),
            "language": r.get("language"),
            "size": r.get("size"),
            "blob_sha": r.get("blob_sha"),
        }
    return lookup


def _split_camel(s: str) -> List[str]:
    s = re.sub(r"([a-z])([A-Z])", r"\1 \2", s)
    parts: List[str] = []
    for p in s.split():
        parts.extend(re.split(r"[_\-.]+", p))
    return [t for t in parts if t]


def _tokens_from_filename_or_path(s: str) -> List[str]:
    if not s:
        return []
    name = s.rsplit("/", 1)[-1]
    name = re.sub(r"\.[A-Za-z0-9]+$", "", name)
    parts = _split_camel(name)
    return [p.lower() for p in parts if len(p) >= 2]


def _tokens_from_label(label: str) -> List[str]:
    cand = re.findall(r"[A-Za-z][A-Za-z0-9]+", label or "")
    toks: List[str] = []
    for c in cand:
        toks.extend(_split_camel(c))
    return [t.lower() for t in toks if len(t) >= 2]


def _jaccard(a: List[str], b: List[str]) -> float:
    if not a or not b:
        return 0.0
    sa, sb = set(a), set(b)
    inter = len(sa & sb)
    union = len(sa | sb)
    return inter / union if union else 0.0


def _load_repo_index(map_id: str) -> List[Dict[str, Any]]:
    rows = list(
        db.aql.execute(
            """
      FOR rf IN repo_files
        FILTER rf.repo_id == @repo_id AND rf.path != null
        LET fname = rf.filename != null ? rf.filename : LAST(SPLIT(rf.path, "/"))
        RETURN { filename: fname, path: rf.path, language: rf.language, size: rf.size, blob_sha: rf.blob_sha }
    """,
            bind_vars={"repo_id": map_id},
        )
    )
    out: List[Dict[str, Any]] = []
    for r in rows:
        fn = r.get("filename") or ""
        path = r.get("path") or ""
        out.append(
            {
                "path": path,
                "tokens": _tokens_from_filename_or_path(fn or path),
                "language": r.get("language"),
                "size": r.get("size"),
                "blob_sha": r.get("blob_sha"),
            }
        )
    return out


def _suggest_files_from_label(
    label: str,
    repo_index: List[Dict[str, Any]],
    limit: int = 2,
    threshold: float = 0.45,
) -> List[Dict[str, Any]]:
    ltok = _tokens_from_label(label)
    if not ltok:
        return []

    scored: List[Dict[str, Any]] = []
    for f in repo_index:
        score = _jaccard(ltok, f["tokens"])
        if score >= threshold:
            scored.append({**f, "score": score})

    if not scored:
        return []

    scored.sort(key=lambda x: (-x["score"], len(x["path"])))
    picks: List[Dict[str, Any]] = []
    seen = set()
    for it in scored:
        if len(picks) >= limit:
            break
        p = it["path"]
        if p in seen:
            continue
        seen.add(p)
        picks.append(
            {
                "file_path": p,
                "language": it.get("language"),
                "size": it.get("size"),
                "blob_sha": it.get("blob_sha"),
                "suggested": True,
            }
        )
    return picks


def _normalize_related_files(map_id: str, rel) -> List[Dict[str, Any]]:
    if isinstance(rel, list) and rel and isinstance(rel[0], dict) and rel[0].get("file_path"):
        return rel

    if isinstance(rel, list) and (not rel or isinstance(rel[0], str)):
        lookup = _build_repo_lookup(map_id)
        out: List[Dict[str, Any]] = []
        seen: set[str] = set()
        for s in rel:
            sname = str(s or "")
            if not sname:
                continue

            if "/" in sname and "." in sname:
                if sname in seen:
                    continue
                seen.add(sname)
                out.append({"file_path": sname})
                continue

            doc = lookup.get(sname)
            if doc and doc.get("path"):
                fp = doc["path"]
                if fp in seen:
                    continue
                seen.add(fp)
                out.append(
                    {
                        "file_path": fp,
                        "language": doc.get("language"),
                        "size": doc.get("size"),
                        "blob_sha": doc.get("blob_sha"),
                        "suggestion_key": (doc.get("suggestion_key") or (doc.get("links") or {}).get("suggestion_key"))
                    }
                )
            else:
                if sname in seen:
                    continue
                seen.add(sname)
                out.append({"file_path": sname, "unresolved": True})
        return out

    return []


# 조회
def get_mindmap_graph(map_id: str):
    ensure_mindmap_indexes()

    edges_raw = list(
        db.aql.execute(
            """
      FOR e IN mindmap_edges
        FILTER e.map_id == @map_id
        RETURN { _from: e._from, _to: e._to, edge_type: e.edge_type }
    """,
            bind_vars={"map_id": map_id},
        )
    )

    node_keys = set()
    for e in edges_raw:
        try:
            node_keys.add(e["_from"].split("/", 1)[1])
            node_keys.add(e["_to"].split("/", 1)[1])
        except Exception:
            pass

    nodes_list: List[Dict[str, Any]] = []
    if node_keys:
        nodes_list = list(
            db.aql.execute(
                """
          FOR n IN mindmap_nodes
            FILTER n.map_id == @map_id AND n._key IN @keys
            RETURN {
              key: n._key,
              label: n.label,
              related_files: n.related_files,
              node_type: n.node_type,
              repo_url: n.repo_url
            }
        """,
                bind_vars={"map_id": map_id, "keys": list(node_keys)},
            )
        )

        for n in nodes_list:
            n["related_files"] = _normalize_related_files(map_id, n.get("related_files"))

        repo_index = _load_repo_index(map_id)
        for n in nodes_list:
            if not n.get("related_files"):
                n["related_files"] = _suggest_files_from_label(n.get("label") or "", repo_index, limit=2, threshold=0.45)

    nodes_dict = {n["key"]: n for n in nodes_list}

    edges = []
    for e in edges_raw:
        try:
            edges.append(
                {
                    "from": e["_from"].split("/", 1)[1],
                    "to": e["_to"].split("/", 1)[1],
                    "edge_type": e.get("edge_type"),
                }
            )
        except Exception:
            continue

    return {"nodes": list(nodes_dict.values()), "edges": edges}


def save_mindmap_graph():
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

    ensure_mindmap_indexes()


# 추가: 루트 노드 찾기

def find_root_node_key(map_id: str) -> Optional[str]:
    rows = list(
        db.aql.execute(
            """
            FOR n IN mindmap_nodes
              FILTER n.map_id == @map_id
              LET incoming = LENGTH(
                FOR e IN mindmap_edges
                  FILTER e.map_id == @map_id AND e._to == n._id
                  RETURN 1
              )
              FILTER incoming == 0
              LIMIT 1
              RETURN n._key
            """,
            bind_vars={"map_id": map_id},
        )
    )
    return rows[0] if rows else None
