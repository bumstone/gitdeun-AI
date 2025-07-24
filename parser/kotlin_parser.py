# parser/kotlin_parser.py

import re
from typing import List, Dict

def extract_kotlin_functions(code: str) -> List[str]:
    # 예: fun helloWorld() { ... }
    pattern = r'fun\s+(\w+)\s*\(.*?\)\s*{'
    return re.findall(pattern, code)

def extract_kotlin_classes(code: str) -> List[str]:
    # 예: class MyClass { ... }
    pattern = r'class\s+(\w+)\s*[{(:]'
    return re.findall(pattern, code)

def extract_kotlin_imports(code: str) -> List[str]:
    # 예: import kotlin.io.*
    pattern = r'import\s+([\w\.]+)'
    return re.findall(pattern, code)

def parse_kotlin_code(code: str) -> Dict[str, List[str]]:
    functions = extract_kotlin_functions(code)
    classes = extract_kotlin_classes(code)
    imports = extract_kotlin_imports(code)

    return {
        "functions": functions,
        "classes": classes,
        "imports": imports
    }
