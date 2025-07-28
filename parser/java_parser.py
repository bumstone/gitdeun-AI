import re

def parse_java_code(code: str) -> dict:
    functions = []
    classes = []
    imports = []
    variables = []

    # ✅ 1. 클래스 추출 (public, abstract, final 포함, inner class 포함 가능)
    class_pattern = re.compile(r'\b(?:public\s+|protected\s+|private\s+)?(?:abstract\s+|final\s+)?class\s+(\w+)')
    classes = class_pattern.findall(code)

    # ✅ 2. 메서드 추출 (접근제한자 + 반환형 + 이름 + 괄호, 중괄호 시작)
    # static, throws 등 일부 키워드는 무시 가능
    method_pattern = re.compile(
        r'\b(?:public|private|protected)?\s*(?:static\s*)?(?:final\s*)?[\w\<\>\[\]]+\s+(\w+)\s*\([^)]*\)\s*\{'
    )
    functions = method_pattern.findall(code)

    # ✅ 3. import 문 추출 (import static 도 포함)
    import_pattern = re.compile(r'^\s*import\s+(?:static\s+)?([\w\.]+);', re.MULTILINE)
    imports = import_pattern.findall(code)

    # ✅ 4. 필드 변수 추출 (public/private/protected + 타입 + 변수명 [=값])
    variable_pattern = re.compile(
        r'\b(?:private|protected|public)\s+(?:static\s+)?(?:final\s+)?([\w\<\>\[\]]+)\s+(\w+)\s*(?:=.*)?;',
        re.MULTILINE
    )
    variables = [match[1] for match in variable_pattern.findall(code)]

    return {
        "functions": functions,
        "classes": classes,
        "imports": imports,
        "variables": variables
    }
