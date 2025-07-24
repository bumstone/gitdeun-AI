import re

def parse_csharp_code(code: str):
    classes = re.findall(r'class\s+([A-Z][a-zA-Z0-9_]*)', code)
    methods = re.findall(r'(?:public|private|protected)\s+(?:static\s+)?\w+\s+([a-zA-Z0-9_]+)\s*\(', code)
    namespaces = re.findall(r'using\s+([a-zA-Z0-9_.]+);', code)

    return {
        "language": "C#",
        "classes": classes,
        "methods": methods,
        "namespaces": namespaces
    }
