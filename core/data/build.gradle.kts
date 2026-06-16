plugins {
    alias(libs.plugins.android.library)
    // alias(libs.plugins.kotlin.android)  // AGP 9.x handles Kotlin
    alias(libs.plugins.kotlin.compose.compiler)
    alias(libs.plugins.ksp)
    // alias(libs.plugins.hilt.android)  // Replaced by Koin
}

android {
    namespace = "com.ahmedyejam.mks.core.data"
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
    api(project(":core:database"))
    api(project(":core:network"))

    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.preferences)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)

    // Koin (replaces Hilt)
    implementation(libs.koin.core)
    implementation(libs.koin.android)

    // OkHttp
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // Moshi
    implementation(libs.moshi)
    implementation(libs.moshi.kotlin)

    // Apache POI
    implementation(libs.poi)
    implementation(libs.poi.ooxml)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}