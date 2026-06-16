import sys
content = sys.stdin.read()
lines = content.split('\n')
depth = 0
for i, line in enumerate(lines):
    depth += line.count('{')
    depth -= line.count('}')
    if (line.strip().startswith('suspend fun') or line.strip().startswith('fun') or line.strip().startswith('private suspend fun')) and depth > 2:
        print(f"FIRST BAD LINE: {i+1}: depth={depth} -> {line.strip()}")
        break
print(f"Final depth: {depth}")
