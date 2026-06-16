import os
import hashlib
from collections import defaultdict

def get_hash(filepath):
    hasher = hashlib.md5()
    with open(filepath, 'rb') as f:
        buf = f.read()
        hasher.update(buf)
    return hasher.hexdigest()

file_hashes = defaultdict(list)
file_names = defaultdict(list)

for root, dirs, files in os.walk("."):
    if ".git" in root or "build" in root or ".gradle" in root or "schemas" in root or ".idea" in root:
        continue
    for f in files:
        if not f.endswith(".kt") and not f.endswith(".xml"):
            continue
        path = os.path.join(root, f)
        file_hashes[get_hash(path)].append(path)
        file_names[f].append(path)

print("=== EXACT DUPLICATE FILES ===")
for h, paths in file_hashes.items():
    if len(paths) > 1:
        print(f"Duplicate content found in {len(paths)} files:")
        for p in paths:
            print("  - " + p)

print("\n=== FILES WITH EXACT SAME NAME (POTENTIAL DUPLICATES) ===")
for name, paths in file_names.items():
    if len(paths) > 1 and "Theme" not in name and "Type" not in name and "Color" not in name:
        print(f"File '{name}' exists in multiple locations:")
        for p in paths:
            print("  - " + p)
