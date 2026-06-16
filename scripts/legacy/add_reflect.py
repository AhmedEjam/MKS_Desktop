with open("core/data/build.gradle.kts", "r") as f:
    content = f.read()

content = content.replace("implementation(libs.hilt.android)", "implementation(libs.hilt.android)\n    implementation(\"org.jetbrains.kotlin:kotlin-reflect\")")

with open("core/data/build.gradle.kts", "w") as f:
    f.write(content)
