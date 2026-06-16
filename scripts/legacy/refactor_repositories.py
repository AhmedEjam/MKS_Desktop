import re
import os

repo_dir = "core/data/src/main/java/com/ahmedyejam/mks/data/repository/"
mks_file = os.path.join(repo_dir, "MksRepository.kt")

with open(mks_file, "r") as f:
    content = f.read()

# I will manually create the files, this is just to verify the path
print(os.path.exists(mks_file))
