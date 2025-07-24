import ast

def parse_python_code(code: str):
    tree = ast.parse(code)
    functions = []
    classes = []
    imports = []
    variables = []

    for node in ast.walk(tree):
        if isinstance(node, ast.FunctionDef):
            functions.append(node.name)
        elif isinstance(node, ast.ClassDef):
            classes.append(node.name)
        elif isinstance(node, ast.Import):
            for alias in node.names:
                imports.append(alias.name)
        elif isinstance(node, ast.ImportFrom):
            imports.append(node.module)
        elif isinstance(node, ast.Assign):
            for target in node.targets:
                if isinstance(target, ast.Name):
                    variables.append(target.id)

    return {
        "language": "Python",
        "functions": functions,
        "classes": classes,
        "imports": imports,
        "variables": variables,
    }
