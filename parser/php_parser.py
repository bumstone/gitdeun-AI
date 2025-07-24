import re

def parse_php_code(code: str):
    functions = re.findall(r'function\s+([a-zA-Z0-9_]+)\s*\(', code)
    classes = re.findall(r'class\s+([a-zA-Z0-9_]+)', code)
    includes = re.findall(r'(?:include|require)(_once)?\s*\(?[\'"](.+?)[\'"]\)?;', code)

    return {
        "language": "PHP",
        "functions": functions,
        "classes": classes,
        "includes": [inc[1] for inc in includes]
    }
