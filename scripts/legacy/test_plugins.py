with open("app/build.gradle.kts", "r") as f:
    lines = f.readlines()

new_lines = []
in_plugins = False
for line in lines:
    if line.strip() == "plugins {":
        in_plugins = True
        new_lines.append("plugins {\n")
        new_lines.append('    id("com.android.application")\n')
        new_lines.append('    id("org.jetbrains.kotlin.android")\n')
        new_lines.append('    id("com.google.dagger.hilt.android")\n')
        new_lines.append('    id("org.jetbrains.kotlin.plugin.compose")\n')
        new_lines.append('    id("org.jetbrains.kotlin.plugin.serialization")\n')
        new_lines.append('    id("com.google.devtools.ksp")\n')
    elif in_plugins and line.strip() == "}":
        in_plugins = False
        new_lines.append("}\n")
    elif not in_plugins:
        if "apply(plugin =" not in line:
            new_lines.append(line)

with open("app/build.gradle.kts", "w") as f:
    f.writelines(new_lines)
