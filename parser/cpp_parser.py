import re

def parse_cpp_code(code: str):
    classes = re.findall(r'class\s+([A-Za-z_][A-Za-z0-9_]*)', code)
    functions = re.findall(r'\b(?:int|void|float|double|char|string)\s+([a-zA-Z_][a-zA-Z0-9_]*)\s*\(', code)
    includes = re.findall(r'#include\s+[<"](.+?)[>"]', code)

    return {
        "language": "C++",
        "classes": classes,
        "functions": functions,
        "includes": includes
    }
