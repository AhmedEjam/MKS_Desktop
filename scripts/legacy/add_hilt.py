import os

def add_hilt_to_file(path):
    with open(path, "r") as f:
        content = f.read()
    
    if "apply(plugin = \"com.google.dagger.hilt.android\")" not in content:
        content = content.replace("android {\n", "apply(plugin = \"com.google.dagger.hilt.android\")\n\nandroid {\n")
    
    if "libs.hilt.android" not in content:
        content = content.replace("dependencies {\n", "dependencies {\n    implementation(libs.hilt.android)\n    ksp(libs.hilt.compiler)\n")
        if path == "app/build.gradle.kts" or path == "feature/ui/build.gradle.kts":
            content = content.replace("ksp(libs.hilt.compiler)\n", "ksp(libs.hilt.compiler)\n    implementation(libs.hilt.navigation.compose)\n")
    
    with open(path, "w") as f:
        f.write(content)

add_hilt_to_file("app/build.gradle.kts")
add_hilt_to_file("core/data/build.gradle.kts")
add_hilt_to_file("core/database/build.gradle.kts")
add_hilt_to_file("core/network/build.gradle.kts")
add_hilt_to_file("feature/ui/build.gradle.kts")

