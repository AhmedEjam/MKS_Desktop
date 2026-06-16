import os
import re

for root, dirs, files in os.walk('feature/ui/src/main/java'):
    for file in files:
        if file.endswith('.kt'):
            path = os.path.join(root, file)
            with open(path, 'r') as f:
                content = f.read()
            
            # Find all import javax.inject.Inject
            # Replace all occurrences with empty string, then add it once at the top of imports
            if content.count('import javax.inject.Inject') > 1:
                lines = content.split('\n')
                new_lines = []
                found_inject = False
                for line in lines:
                    if 'import javax.inject.Inject' in line:
                        if not found_inject:
                            new_lines.append(line)
                            found_inject = True
                    else:
                        new_lines.append(line)
                with open(path, 'w') as f:
                    f.write('\n'.join(new_lines))
                print(f"Fixed duplicate import in {path}")
