# create_code_suggestion_node 가 만드는 제안 노드 키를 검사한다.
# 실행(저장소 최상위에서): .venv\Scripts\python -m unittest discover -s tests -v
#
# services 모듈은 import 하는 순간 ArangoDB 클라이언트(services/arangodb_service.py:36-37)와
# Gemini 설정(services/gemini_service.py:11)을 만든다. 그래서 import 전에 그 두 모듈과 code_service 를
# 가짜로 바꿔 네트워크 없이 돌린다. mindmap_service·suggestion_service 는 진짜 코드다.
# 기대 키는 hashlib 로 따로 계산한 값이다(제품 코드를 거치지 않았다).
import sys
import types
import unittest
from unittest import mock

MAP_ID = "demo-map"
REPO_URL = "https://github.com/octo/demo"
FILE_PATH = "src/main/java/demo/App.java"
PROMPT = "로그를 추가해 줘"
LABEL = "[AI] App.java 개선안 #bd04d8a3fa28"   # suggestion_key = md5(f"{REPO_URL}_{FILE_PATH}_{md5(PROMPT)[:8]}")[:12]
SUGG_NODE_KEY = "66dc1512d640"                 # md5(f"{MAP_ID}_SUGG_{LABEL}")[:12] — mode 를 없애기 전 3인자 호출이 만들던 키


def fake_services(store):
    """ArangoDB 대신 store(dict)에 (컬렉션, _key) 로 쌓는다. Gemini·원본 코드는 고정값을 돌려준다"""
    arango = types.ModuleType("services.arangodb_service")
    arango.db = None
    arango.document_exists = lambda collection, key: (collection, key) in store
    arango.insert_document = lambda collection, doc: store.__setitem__((collection, doc["_key"]), doc)
    gemini = types.ModuleType("services.gemini_service")
    gemini.GEMINI_MODEL = "fake-model"
    gemini.generate_code_suggestion = lambda **_: {"code": "class App {}", "summary": "", "rationale": ""}
    code = types.ModuleType("services.code_service")
    code.load_original_code_by_path = lambda repo_url, path: "class App {}"
    return {"services.arangodb_service": arango, "services.gemini_service": gemini, "services.code_service": code}


class CreateCodeSuggestionNodeTest(unittest.TestCase):
    def setUp(self):
        self.store = {}
        modules = mock.patch.dict(sys.modules, fake_services(self.store))
        modules.start()
        self.addCleanup(modules.stop)   # 끝나면 가짜 모듈과 이 테스트가 import 한 모듈을 모두 걷어 낸다
        for name in ("services.mindmap_service", "services.suggestion_service"):
            sys.modules.pop(name, None)

    def test_suggestion_node_key_keeps_legacy_sugg_rule(self):
        """제안 노드를 만든다 — 키는 mode 를 없애기 전 generate_node_key(map_id, "SUGG", label) 과 같다"""
        from services.suggestion_service import create_code_suggestion_node
        r = create_code_suggestion_node(MAP_ID, REPO_URL, FILE_PATH, PROMPT)
        self.assertEqual((r["label"], r["node_key"]), (LABEL, SUGG_NODE_KEY))
        self.assertEqual(self.store[("mindmap_nodes", SUGG_NODE_KEY)]["node_type"], "suggestion")

    def test_mindmap_node_with_same_label_gets_its_own_key(self):
        """마인드맵 요약이 제안 노드의 라벨을 그대로 옮겨 적어도 제안 노드에 합쳐지지 않고 따로 저장된다"""
        from services.suggestion_service import create_code_suggestion_node
        from services.mindmap_service import save_mindmap_nodes_recursively
        create_code_suggestion_node(MAP_ID, REPO_URL, FILE_PATH, PROMPT)
        save_mindmap_nodes_recursively(REPO_URL, {"node": LABEL}, map_id=MAP_ID)
        nodes = {key: doc.get("node_type") for (col, key), doc in self.store.items() if col == "mindmap_nodes"}
        self.assertEqual(nodes, {SUGG_NODE_KEY: "suggestion", "6edada7463d1": None})   # 6edada7463d1 = md5(f"{MAP_ID}_{LABEL}")[:12]


if __name__ == "__main__":
    unittest.main()
