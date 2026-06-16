plugins {
    alias(libs.plugins.android.library)
    // alias(libs.plugins.kotlin.android)  // AGP 9.x handles Kotlin
    alias(libs.plugins.kotlin.compose.compiler)
    alias(libs.plugins.ksp)
    // alias(libs.plugins.hilt.android)  // Replaced by Koin
}

android {
    namespace = "com.ahmedyejam.mks.core.network"
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
    api(project(":core:model"))

    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Koin (replaces Hilt)
    implementation(libs.koin.core)

    // OkHttp
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // Moshi
    implementation(libs.moshi)
    implementation(libs.moshi.kotlin)

    // Testing
    testImplementation(libs.junit)
}