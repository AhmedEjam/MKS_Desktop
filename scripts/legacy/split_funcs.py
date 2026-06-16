import re
import json

with open("core/data/src/main/java/com/ahmedyejam/mks/data/repository/MksRepository.kt.bak", "r") as f:
    lines = f.readlines()

functions = {}
current_func_name = None
current_func_lines = []

func_start_regex = re.compile(r'^    (?:private )?(?:suspend )?fun ([a-zA-Z0-9_]+)\(')

for line in lines:
    match = func_start_regex.match(line)
    if match:
        # Save previous function
        if current_func_name:
            functions[current_func_name] = "".join(current_func_lines)
        
        current_func_name = match.group(1)
        current_func_lines = [line]
    elif current_func_name is not None:
        # Check if we hit the end of the class
        if line.startswith("}"):
            break
        current_func_lines.append(line)

# Save the last function
if current_func_name:
    functions[current_func_name] = "".join(current_func_lines)

print(f"Extracted {len(functions)} functions by indentation.")

with open("scripts/functions.json", "w") as f:
    json.dump(functions, f, indent=2)

