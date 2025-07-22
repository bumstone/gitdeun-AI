# database/arangodb_client.py
from arango import ArangoClient
import os
from dotenv import load_dotenv

# 환경변수 로드
load_dotenv()

ARANGODB_USERNAME = os.getenv("ARANGODB_USERNAME", "root")
ARANGODB_PASSWORD = os.getenv("ARANGODB_PASSWORD", "pass123")
ARANGODB_DB = os.getenv("ARANGODB_DB", "_system")

# ArangoDB 클라이언트 연결
client = ArangoClient()
db = client.db(
    name=ARANGODB_DB,
    username=ARANGODB_USERNAME,
    password=ARANGODB_PASSWORD
)
