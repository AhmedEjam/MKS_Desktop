with open("app/build.gradle.kts", "r") as f:
    lines = f.readlines()

new_lines = []
for line in lines:
    if "id(\"org.jetbrains.kotlin.plugin.compose\")" in line:
        continue
    new_lines.append(line)

with open("app/build.gradle.kts", "w") as f:
    f.writelines(new_lines)
