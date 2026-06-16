import sys
import os

def parse_functions(filepath):
    with open(filepath, 'r') as f:
        code = f.read()

    # Find the class declaration
    class_idx = code.find("class MksRepository")
    if class_idx == -1:
        return

    # We want to keep imports and data classes at the top.
    # Everything before 'class MksRepository' is header.
    header = code[:class_idx]

    # Now we need to parse the functions.
    # A function starts with: spaces + (optional modifiers) + 'fun ' + name
    import re
    # Match any function signature
    # This regex is simplified but works for top-level functions in a class
    func_regex = re.compile(r'^[ \t]*(?:private\s+)?(?:suspend\s+)?fun\s+([a-zA-Z0-9_]+)\s*\(', re.MULTILINE)
    
    functions = []
    for match in func_regex.finditer(code):
        start_idx = match.start()
        func_name = match.group(1)
        
        # We need to find the end of the function.
        # It could be a block body '{ ... }' or an expression body '= ...'
        # Let's find the first '{' or '=' after the signature
        
        # Find the closing parenthesis of the arguments first
        paren_count = 0
        idx = match.end() - 1
        while idx < len(code):
            if code[idx] == '(':
                paren_count += 1
            elif code[idx] == ')':
                paren_count -= 1
                if paren_count == 0:
                    idx += 1
                    break
            idx += 1
            
        # Now find the body start '{' or '='
        while idx < len(code) and code[idx] in ' \t\n\r:':
            # Skip return type
            if code[idx] == ':':
                while idx < len(code) and code[idx] not in '={\n':
                    idx += 1
            else:
                idx += 1
                
        if idx >= len(code):
            continue
            
        body_start_char = code[idx]
        
        if body_start_char == '=':
            # Expression body, ends at newline
            end_idx = code.find('\n', idx)
            if end_idx == -1:
                end_idx = len(code)
            functions.append((func_name, code[start_idx:end_idx], start_idx, end_idx))
        elif body_start_char == '{':
            # Block body, match braces
            brace_count = 0
            end_idx = idx
            while end_idx < len(code):
                if code[end_idx] == '{':
                    brace_count += 1
                elif code[end_idx] == '}':
                    brace_count -= 1
                    if brace_count == 0:
                        end_idx += 1
                        break
                end_idx += 1
            functions.append((func_name, code[start_idx:end_idx], start_idx, end_idx))

    print(f"Found {len(functions)} functions.")
    
    # Save the functions to a JSON file for analysis
    import json
    func_dict = {name: body for name, body, _, _ in functions}
    with open('scripts/functions.json', 'w') as f:
        json.dump(func_dict, f, indent=2)

parse_functions('core/data/src/main/java/com/ahmedyejam/mks/data/repository/MksRepository.kt')
