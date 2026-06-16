import os

for root, dirs, files in os.walk('core'):
    if 'build.gradle.kts' in files:
        path = os.path.join(root, 'build.gradle.kts')
        with open(path, 'r') as f:
            content = f.read()
        
        if 'org.jetbrains.kotlin.android' not in content:
            # find plugins {
            plugins_idx = content.find('plugins {')
            if plugins_idx != -1:
                # insert after plugins {
                insert_idx = content.find('\n', plugins_idx) + 1
                new_content = content[:insert_idx] + '    id("org.jetbrains.kotlin.android")\n' + content[insert_idx:]
                with open(path, 'w') as f:
                    f.write(new_content)
                print(f"Patched {path}")
