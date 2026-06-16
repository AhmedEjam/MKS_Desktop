import os
import re
import glob

# Precomputed map of method -> Repository
repo_dir = "core/data/src/main/java/com/ahmedyejam/mks/data/repository"
repos = ["BookRepository.kt", "QuizRepository.kt", "KnowledgeRepository.kt", "StudyRepository.kt", "WorkspaceRepository.kt", "AssetRepository.kt"]

method_map = {}
for repo in repos:
    path = os.path.join(repo_dir, repo)
    if not os.path.exists(path): continue
    with open(path, "r") as f: content = f.read()
    matches = re.findall(r"(?:suspend\s+)?fun\s+([a-zA-Z0-9_]+)\s*\(", content)
    repo_name = repo.replace(".kt", "")
    for m in matches: method_map[m] = repo_name

# Some additional methods handled directly by Book/Quiz repos that might be delegated
# Let's do a pass over all ViewModels
ui_dir = "feature/ui/src/main/java/com/ahmedyejam/mks/ui"
kt_files = glob.glob(f"{ui_dir}/**/*.kt", recursive=True)

for kt in kt_files:
    with open(kt, "r") as f:
        content = f.read()
    
    if "MksRepository" not in content: continue

    # Find all repository.methodName calls
    # or mksRepository.methodName
    # or repository.insertBook
    called_methods = re.findall(r"(?:repository|mksRepository|appModule\.repository)\.([a-zA-Z0-9_]+)", content)
    needed_repos = set()
    for m in called_methods:
        if m in method_map:
            needed_repos.add(method_map[m])
        else:
            print(f"Warning: {m} not found in method map for {kt}")

    if not needed_repos:
        # Maybe it's just imported?
        pass

    # Update Imports
    content = re.sub(r"import com\.ahmedyejam\.mks\.data\.repository\.MksRepository\n?", "", content)
    new_imports = "\n".join([f"import com.ahmedyejam.mks.data.repository.{r}" for r in needed_repos])
    # Add new imports near other imports
    if needed_repos:
        content = re.sub(r"(import com\.ahmedyejam\.mks\.[^\n]+)", f"\\1\n{new_imports}", content, count=1)

    # Update Constructor Injection
    # Look for: class XViewModel @Inject constructor(private val repository: MksRepository...)
    # Or: class XViewModel @Inject constructor(\n private val repository: MksRepository\n)
    constructor_pattern = re.compile(r"class\s+([A-zA-Z0-9_]+ViewModel)\s*@Inject\s*constructor\s*\((.*?)\)", re.DOTALL)
    
    def repl_constructor(match):
        vm_name = match.group(1)
        params = match.group(2)
        # Remove repository: MksRepository
        # Remove appModule.repository
        params_list = [p.strip() for p in params.split(",") if "MksRepository" not in p and p.strip()]
        for r in needed_repos:
            var_name = r[0].lower() + r[1:]
            params_list.append(f"private val {var_name}: {r}")
        new_params = ",\n    ".join(params_list)
        return f"class {vm_name} @Inject constructor(\n    {new_params}\n)"

    content = constructor_pattern.sub(repl_constructor, content)

    # Replace usages: repository.method -> varName.method
    for m in set(called_methods):
        if m in method_map:
            repo_var = method_map[m][0].lower() + method_map[m][1:]
            content = re.sub(r"(?:repository|mksRepository|appModule\.repository)\." + m + r"\b", f"{repo_var}.{m}", content)

    # Clean up any "private val repository: MksRepository = appModule.repository" that might not be in constructor
    content = re.sub(r"private\s+val\s+(?:repository|mksRepository)\s*:\s*MksRepository\s*(?:=\s*appModule\.repository)?\s*\n?", "", content)

    with open(kt, "w") as f:
        f.write(content)

print("ViewModel refactor script completed.")
