path = 'app/build.gradle.kts'
with open(path, 'r') as f:
    content = f.read()

if 'org.jetbrains.kotlin.android' not in content:
    plugins_idx = content.find('plugins {')
    if plugins_idx != -1:
        insert_idx = content.find('\n', plugins_idx) + 1
        new_content = content[:insert_idx] + '    id("org.jetbrains.kotlin.android")\n' + content[insert_idx:]
        with open(path, 'w') as f:
            f.write(new_content)
        print(f"Patched {path}")
