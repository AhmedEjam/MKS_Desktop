path = 'feature/ui/src/main/java/com/ahmedyejam/mks/ui/quiz/CompilerViewModel.kt'
with open(path, 'r') as f:
    content = f.read()

if 'import dagger.hilt.android.qualifiers.ApplicationContext' not in content:
    lines = content.split('\n')
    for i, line in enumerate(lines):
        if line.startswith('import '):
            lines.insert(i, 'import dagger.hilt.android.qualifiers.ApplicationContext')
            break
    with open(path, 'w') as f:
        f.write('\n'.join(lines))
