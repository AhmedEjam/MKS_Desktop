import os
import glob

def refactor_viewmodel(file_path):
    with open(file_path, 'r') as f:
        content = f.read()

    if '@HiltViewModel' in content:
        return

    # Add imports
    imports_to_add = "import dagger.hilt.android.lifecycle.HiltViewModel\nimport javax.inject.Inject\n"
    
    # Find package line and insert imports after it
    lines = content.split('\n')
    for i, line in enumerate(lines):
        if line.startswith('package '):
            lines.insert(i + 1, '\n' + imports_to_add)
            break
            
    content = '\n'.join(lines)

    # Find class declaration and replace with @HiltViewModel and @Inject constructor
    class_name = os.path.basename(file_path).replace('.kt', '')
    
    if f'class {class_name}(' in content:
        content = content.replace(f'class {class_name}(', f'@HiltViewModel\nclass {class_name} @Inject constructor(')
    elif f'class {class_name} (' in content:
        content = content.replace(f'class {class_name} (', f'@HiltViewModel\nclass {class_name} @Inject constructor(')
        
    with open(file_path, 'w') as f:
        f.write(content)

for root, _, files in os.walk('feature/ui/src/main/java/com/ahmedyejam/mks/ui'):
    for file in files:
        if file.endswith('ViewModel.kt'):
            refactor_viewmodel(os.path.join(root, file))

print("Done updating ViewModels")
