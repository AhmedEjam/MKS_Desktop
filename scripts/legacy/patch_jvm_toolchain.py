import os

for root, dirs, files in os.walk('.'):
    if 'build.gradle.kts' in files and 'gradle' not in root:
        path = os.path.join(root, 'build.gradle.kts')
        with open(path, 'r') as f:
            content = f.read()
        
        if 'jvmToolchain' not in content and 'org.jetbrains.kotlin.android' in content:
            new_content = content + '\nkotlin {\n    jvmToolchain(11)\n}\n'
            with open(path, 'w') as f:
                f.write(new_content)
            print(f"Patched {path}")
