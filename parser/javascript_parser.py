import re

def parse_js_code(code: str):
    functions = re.findall(r'function\s+([a-zA-Z0-9_]+)\s*\(', code)
    arrow_funcs = re.findall(r'const\s+([a-zA-Z0-9_]+)\s*=\s*\(', code)
    classes = re.findall(r'class\s+([a-zA-Z0-9_]+)', code)
    imports = re.findall(r'import\s+.*?\s+from\s+[\'"](.+?)[\'"]', code)

    return {
        "language": "JavaScript",
        "functions": functions + arrow_funcs,
        "classes": classes,
        "imports": imports
    }
