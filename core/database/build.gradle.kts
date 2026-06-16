plugins {
    alias(libs.plugins.android.library)
    // alias(libs.plugins.kotlin.android)  // AGP 9.x handles Kotlin
    alias(libs.plugins.kotlin.compose.compiler)
    alias(libs.plugins.ksp)
    // alias(libs.plugins.hilt.android)  // Replaced by Koin
}

android {
    namespace = "com.ahmedyejam.mks.core.database"
    compileSdk = 35

    defaultConfig {
        minSdk = 26

        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }
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

    // Room
    api(libs.room.runtime)
    api(libs.room.ktx)
    ksp(libs.room.compiler)

    // Koin (replaces Hilt @Singleton)
    implementation(libs.koin.core)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext)
    androidTestImplementation(libs.androidx.test.runner)
}