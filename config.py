# config.py
import os
from dotenv import load_dotenv

# .env 파일에서 환경변수 불러오기 (로컬 개발)
load_dotenv()

# ===== ArangoDB =====
ARANGODB_HOST = os.getenv("ARANGODB_HOST", "127.0.0.1")
ARANGODB_PORT = int(os.getenv("ARANGODB_PORT", "8529"))
ARANGODB_USERNAME = os.getenv("ARANGODB_USERNAME", "root")
ARANGODB_PASSWORD = os.getenv("ARANGODB_PASSWORD", "")
ARANGODB_DB       = os.getenv("ARANGODB_DB", "_system")


# ===== Gemini =====
GEMINI_API_KEY = os.getenv("GEMINI_API_KEY", "")

# ===== Optional: Repo lookup / GitHub raw fallback =====
REPO_CACHE_DIR = os.getenv("REPO_CACHE_DIR", "/tmp/repos")
DEFAULT_BRANCH = os.getenv("DEFAULT_BRANCH", "main")
GITHUB_TOKEN   = os.getenv("GITHUB_TOKEN", "")

# ===== Optional: JWT =====
JWT_SECRET     = os.getenv("JWT_SECRET", "")
JWT_EXPIRES_IN = int(os.getenv("JWT_EXPIRES_IN", "3600"))

# ===== App =====
APP_ENV   = os.getenv("APP_ENV", "local")
LOG_LEVEL = os.getenv("LOG_LEVEL", "INFO")

def require_env(*keys: str):
    """필수 키가 빈 값이면 친절한 에러를 던짐. 운영에서만 켜도 됨."""
    missing = [k for k in keys if not os.getenv(k)]
    if missing:
        raise RuntimeError(f"Missing required env vars: {', '.join(missing)}")

# 예: 운영에서만 강제하고 싶으면
# if APP_ENV != "local":
#     require_env("GEMINI_API_KEY", "ARANGODB_USERNAME", "ARANGODB_PASSWORD", "ARANGODB_DB")
