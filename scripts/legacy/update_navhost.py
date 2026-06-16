import os
import re

file_path = 'feature/ui/src/main/java/com/ahmedyejam/mks/ui/MksNavHost.kt'
with open(file_path, 'r') as f:
    content = f.read()

# Add hiltViewModel import
content = content.replace(
    'import androidx.compose.runtime.Composable',
    'import androidx.compose.runtime.Composable\nimport androidx.hilt.navigation.compose.hiltViewModel'
)

# Remove appModule from MksNavHost arguments
content = content.replace(
    'navController: NavHostController,\n    appModule: com.ahmedyejam.mks.di.AppModule,\n    showWelcomeOnStartup: Boolean = true,',
    'navController: NavHostController,\n    showWelcomeOnStartup: Boolean = true,'
)
content = content.replace(
    'navController: NavHostController,\n    appModule: com.ahmedyejam.mks.di.AppModule,',
    'navController: NavHostController,'
)

# Use regex to find and replace all instances of viewModel(factory = ...) with hiltViewModel()
# Example: 
# val viewModel: LibraryViewModel = viewModel(
#     factory = object : ViewModelProvider.Factory {
#         override fun <T : ViewModel> create(modelClass: Class<T>): T {
#             return LibraryViewModel(appModule.repository) as T
#         }
#     }
# )
pattern = re.compile(r'val\s+(\w+)\s*:\s*(\w+ViewModel)\s*=\s*viewModel\s*\([^)]+factory\s*=\s*object\s*:\s*ViewModelProvider\.Factory\s*\{[^}]+}[^}]+}[^)]+\)', re.DOTALL)
content = pattern.sub(r'val \1: \2 = hiltViewModel()', content)

# Check if there are other viewModel factory patterns without type annotation
pattern2 = re.compile(r'val\s+(\w+)\s*=\s*viewModel\s*<\s*(\w+ViewModel)\s*>\s*\([^)]+factory\s*=\s*object\s*:\s*ViewModelProvider\.Factory\s*\{[^}]+}[^}]+}[^)]+\)', re.DOTALL)
content = pattern2.sub(r'val \1: \2 = hiltViewModel()', content)

with open(file_path, 'w') as f:
    f.write(content)
