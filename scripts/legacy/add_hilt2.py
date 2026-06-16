with open("app/build.gradle.kts", "r") as f:
    content = f.read()

content = content.replace("plugins {\n", "plugins {\n    alias(libs.plugins.dagger.hilt.android)\n")
content = content.replace("dependencies {\n", "dependencies {\n    implementation(libs.hilt.android)\n    ksp(libs.hilt.compiler)\n    implementation(libs.androidx.hilt.navigation.compose)\n    implementation(project(\":core:model\"))\n    implementation(project(\":core:database\"))\n    implementation(project(\":core:data\"))\n    implementation(project(\":core:network\"))\n    implementation(project(\":core:ui\"))\n    implementation(project(\":feature:ui\"))\n")

with open("app/build.gradle.kts", "w") as f:
    f.write(content)
