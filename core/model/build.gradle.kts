plugins {
    alias(libs.plugins.android.library)
    // alias(libs.plugins.kotlin.android)  // AGP 9.x handles Kotlin
    alias(libs.plugins.kotlin.compose.compiler)
    // alias(libs.plugins.ksp)  // not needed for entities
    // alias(libs.plugins.hilt.android)  // not needed for entities
}

android {
    namespace = "com.ahmedyejam.mks.core.model"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines.core)

    // Room annotations (entities use Room annotations)
    api(libs.room.runtime)

    // Moshi
    implementation(libs.moshi)
    implementation(libs.moshi.kotlin)
}