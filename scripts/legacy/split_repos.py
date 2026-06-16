import re

with open('core/data/src/main/java/com/ahmedyejam/mks/data/repository/MksRepository.kt', 'r') as f:
    content = f.read()

# I will save the full content of MksRepository somewhere safe first
with open('core/data/src/main/java/com/ahmedyejam/mks/data/repository/MksRepository.kt.bak', 'w') as f:
    f.write(content)

