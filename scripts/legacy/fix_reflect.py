import os
for root, dirs, files in os.walk("."):
    if "build.gradle.kts" in files:
        path = os.path.join(root, "build.gradle.kts")
        with open(path, "r") as f:
            content = f.read()
        content = content.replace("ksp(\"org.jetbrains.kotlin:kotlin-reflect\")", "ksp(\"org.jetbrains.kotlin:kotlin-reflect:2.1.0\")")
        with open(path, "w") as f:
            f.write(content)
