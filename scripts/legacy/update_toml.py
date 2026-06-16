with open("gradle/libs.versions.toml", "r") as f:
    content = f.read()

versions = """
hilt = "2.55"
androidxHiltNavigationCompose = "1.2.0"
"""
content = content.replace("[versions]\n", "[versions]\n" + versions)

libraries = """
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-compiler", version.ref = "hilt" }
androidx-hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version.ref = "androidxHiltNavigationCompose" }
"""
content = content.replace("[libraries]\n", "[libraries]\n" + libraries)

plugins = """
android-library = { id = "com.android.library", version.ref = "agp" }
dagger-hilt-android = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
"""
content = content.replace("[plugins]\n", "[plugins]\n" + plugins)

with open("gradle/libs.versions.toml", "w") as f:
    f.write(content)
