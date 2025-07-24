import re

def parse_typescript_code(code: str):
    functions = re.findall(r'function\s+([a-zA-Z0-9_]+)', code)
    arrow_funcs = re.findall(r'const\s+([a-zA-Z0-9_]+)\s*=\s*\(.*?\)\s*=>', code)
    interfaces = re.findall(r'interface\s+([a-zA-Z0-9_]+)', code)
    classes = re.findall(r'class\s+([a-zA-Z0-9_]+)', code)
    imports = re.findall(r'import\s+.*?\s+from\s+[\'"](.+?)[\'"]', code)

    return {
        "language": "TypeScript",
        "functions": functions + arrow_funcs,
        "interfaces": interfaces,
        "classes": classes,
        "imports": imports
    }
