import re

def parse_java_code(code: str):
    classes = re.findall(r'\bclass\s+([A-Z][a-zA-Z0-9_]*)', code)
    methods = re.findall(r'(?:public|private|protected)?\s*(?:static)?\s*\w+\s+(\w+)\s*\(', code)
    imports = re.findall(r'import\s+([a-zA-Z0-9_.]+);', code)

    return {
        "language": "Java",
        "classes": classes,
        "methods": methods,
        "imports": imports
    }
