import re

def parse_ruby_code(code: str):
    classes = re.findall(r'class\s+([A-Z][a-zA-Z0-9_]*)', code)
    methods = re.findall(r'def\s+([a-zA-Z0-9_!?]+)', code)
    requires = re.findall(r'require\s+[\'"](.+?)[\'"]', code)

    return {
        "language": "Ruby",
        "classes": classes,
        "methods": methods,
        "requires": requires
    }
