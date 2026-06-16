import os

def fix_hilt(file_path):
    with open(file_path, 'r') as f:
        content = f.read()
        
    if 'alias(libs.plugins.dagger.hilt.android)' in content:
        content = content.replace('alias(libs.plugins.dagger.hilt.android)', '// alias(libs.plugins.dagger.hilt.android)')
        
        # Add apply plugin after plugins block
        plugins_end = content.find('}') + 1
        content = content[:plugins_end] + '\n\napply(plugin = "com.google.dagger.hilt.android")\n' + content[plugins_end:]
        
        with open(file_path, 'w') as f:
            f.write(content)

for root, _, files in os.walk('.'):
    for file in files:
        if file == 'build.gradle.kts' and root != '.':
            fix_hilt(os.path.join(root, file))

print("Fixed hilt application")
