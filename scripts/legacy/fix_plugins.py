import os

def fix_plugins(file_path):
    with open(file_path, 'r') as f:
        content = f.read()

    lines = content.split('\n')
    plugins_block_start = -1
    plugins_block_end = -1
    for i, line in enumerate(lines):
        if line.strip() == 'plugins {':
            plugins_block_start = i
        elif plugins_block_start != -1 and line.strip() == '}':
            plugins_block_end = i
            break

    if plugins_block_start != -1 and plugins_block_end != -1:
        plugins_content = lines[plugins_block_start + 1:plugins_block_end]
        
        # Extract plugins
        plugins = [p.strip() for p in plugins_content if p.strip()]
        
        # Define the exact order
        ordered_plugins = []
        
        # 1. Android Plugin
        for p in plugins:
            if 'android.application' in p or 'android.library' in p:
                ordered_plugins.append(p)
                break
                
        # 2. Kotlin Android
        ordered_plugins.append('alias(libs.plugins.kotlin.android)')
        
        # 3. Hilt
        for p in plugins:
            if 'hilt.android' in p:
                ordered_plugins.append(p)
                break
                
        # 4. Add the rest
        for p in plugins:
            if p not in ordered_plugins and 'kotlin.android' not in p:
                ordered_plugins.append(p)
                
        new_plugins_block = ['plugins {'] + ['    ' + p for p in ordered_plugins] + ['}']
        
        lines = lines[:plugins_block_start] + new_plugins_block + lines[plugins_block_end + 1:]
        
        with open(file_path, 'w') as f:
            f.write('\n'.join(lines))

for root, _, files in os.walk('.'):
    for file in files:
        if file == 'build.gradle.kts' and root != '.':
            fix_plugins(os.path.join(root, file))

print("Fixed plugin order")
