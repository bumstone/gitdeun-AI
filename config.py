from dotenv import load_dotenv
import os

# .env 파일에서 환경변수 불러오기
load_dotenv()

GEMINI_API_KEY = os.getenv("GEMINI_API_KEY")
ARANGODB_USERNAME = os.getenv("ARANGODB_USERNAME", "root")
ARANGODB_PASSWORD = os.getenv("ARANGODB_PASSWORD", "pass123")
ARANGODB_DB = os.getenv("ARANGODB_DB", "_system")
