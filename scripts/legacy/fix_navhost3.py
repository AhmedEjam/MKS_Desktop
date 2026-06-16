import re

with open('feature/ui/src/main/java/com/ahmedyejam/mks/ui/MksNavHost.kt', 'r') as f:
    content = f.read()

# Add import if missing
if 'androidx.hilt.navigation.compose.hiltViewModel' not in content:
    content = content.replace('import androidx.compose.runtime.Composable', 'import androidx.hilt.navigation.compose.hiltViewModel\nimport androidx.compose.runtime.Composable')

pattern = re.compile(
    r'=\s*viewModel\(\s*factory\s*=\s*object\s*:\s*ViewModelProvider\.Factory.*?return\s+[A-Za-z0-9_]+ViewModel\([^)]*\)\s*as\s*T\s*\}\s*\}(?:,\s*key\s*=\s*[^)\n]+)?\s*\)',
    re.DOTALL
)

new_content = pattern.sub(r'= hiltViewModel()', content)

# Replace the single line version of viewModel() without factory
pattern2 = re.compile(
    r'=\s*viewModel\(\)\s*',
    re.DOTALL
)
new_content = pattern2.sub(r'= hiltViewModel()', new_content)

count = new_content.count('ViewModelProvider.Factory')
print(f"Remaining ViewModelProvider.Factory: {count}")

with open('feature/ui/src/main/java/com/ahmedyejam/mks/ui/MksNavHost.kt', 'w') as f:
    f.write(new_content)
