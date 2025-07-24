import re

def parse_go_code(code: str):
    functions = re.findall(r'func\s+([A-Za-z0-9_]+)\s*\(', code)
    structs = re.findall(r'type\s+([A-Za-z0-9_]+)\s+struct', code)
    imports = re.findall(r'import\s+\(?\s*"(.+?)"\s*\)?', code)

    return {
        "language": "Go",
        "functions": functions,
        "structs": structs,
        "imports": imports
    }
