with open("gradle/libs.versions.toml", "r") as f:
    content = f.read()

content = content.replace('kotlin = "2.1.0"', 'kotlin = "2.0.20"')
content = content.replace('googleDevtoolsKsp = "2.1.0-1.0.29"', 'googleDevtoolsKsp = "2.0.20-1.0.25"')

with open("gradle/libs.versions.toml", "w") as f:
    f.write(content)
