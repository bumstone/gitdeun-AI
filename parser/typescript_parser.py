import re

FUNC_DECL = re.compile(r'(?:export\s+)?(?:async\s+)?function\s+([A-Za-z_]\w*)\s*\(', re.MULTILINE)
ARROW_CONST = re.compile(r'(?:export\s+)?(?:const|let|var)\s+([A-Za-z_]\w*)\s*(?::\s*[^=]+)?=\s*\([^)]*\)\s*=>', re.MULTILINE)
REACT_COMP = re.compile(r'(?:export\s+)?(?:const|let|var)\s+([A-Za-z_]\w*)\s*:\s*React\.[A-Za-z_][\w.<>,\s]*=\s*\([^)]*\)\s*=>', re.MULTILINE)

INTERFACE = re.compile(r'(?:export\s+)?interface\s+([A-Za-z_]\w*)', re.MULTILINE)
TYPE_ALIAS = re.compile(r'(?:export\s+)?type\s+([A-Za-z_]\w*)\s*=', re.MULTILINE)
ENUM = re.compile(r'(?:export\s+)?enum\s+([A-Za-z_]\w*)', re.MULTILINE)
CLASS = re.compile(r'(?:export\s+)?class\s+([A-Za-z_]\w*)', re.MULTILINE)

IMPORT_FROM = re.compile(r'import\s+.*?\s+from\s+[\'"]([^\'"]+)[\'"]', re.MULTILINE)
IMPORT_SIDE = re.compile(r'import\s+[\'"]([^\'"]+)[\'"]', re.MULTILINE)

def parse_typescript_code(code: str):
    functions = []
    functions += FUNC_DECL.findall(code)
    functions += ARROW_CONST.findall(code)
    functions += REACT_COMP.findall(code)

    interfaces = INTERFACE.findall(code)
    classes = CLASS.findall(code)
    types = TYPE_ALIAS.findall(code)
    enums = ENUM.findall(code)
    imports = IMPORT_FROM.findall(code) + IMPORT_SIDE.findall(code)

    return {
        "language": "TypeScript",
        "functions": functions,
        "interfaces": interfaces,
        "types": types,
        "enums": enums,
        "classes": classes,
        "imports": imports,
    }
