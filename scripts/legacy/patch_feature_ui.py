path = 'feature/ui/build.gradle.kts'
with open(path, 'r') as f:
    content = f.read()

if 'kotlinx-coroutines-play-services' not in content:
    content += '\n    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")\n'
    with open(path, 'w') as f:
        f.write(content)
    print("Patched feature/ui/build.gradle.kts")
