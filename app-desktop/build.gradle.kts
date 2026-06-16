plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose.compiler)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":shared"))

    // Compose Desktop
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)

    // Koin
    implementation(libs.koin.core)
    implementation(libs.koin.compose.viewmodel)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.swing)

    // SQLDelight JVM driver (direct for desktop)
    implementation(libs.sqldelight.jvm.driver)

    // kotlinx-serialization-json (needed by ExportManager + importer)
    implementation(libs.kotlinx.serialization.json)
}

compose.desktop {
    application {
        mainClass = "com.ahmedyejam.mks.desktop.MainKt"

        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Rpm,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb
            )
            packageName = "mks"
            packageVersion = "1.0.0"
            linux {
                iconFile.set(project.file("src/main/resources/icon.png"))
            }
        }
    }
}