# Room schemas

Room schema export is enabled in `app/build.gradle.kts` and `MksDatabase`.

A networked Android development environment should run:

```bash
./gradlew :app:kspDebugKotlin
```

or a full build/test task, then commit the generated schema JSON files in this directory.
