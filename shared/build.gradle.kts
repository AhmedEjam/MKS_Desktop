plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.compose.compiler)
    alias(libs.plugins.sqldelight)
}

kotlin {
    android {
        namespace = "com.ahmedyejam.mks.shared"
        compileSdk = 35
        minSdk = 26
    }

    jvm("desktop") {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                // Compose Multiplatform
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)
                implementation(compose.ui)
                implementation(compose.components.resources)

                // Koin DI (multiplatform)
                implementation(libs.koin.core)
                implementation(libs.koin.compose)

                // SQLDelight coroutines
                implementation(libs.sqldelight.coroutines)

                // Coroutines
                implementation(libs.kotlinx.coroutines.core)

                // Serialization
                implementation(libs.kotlinx.serialization.json)

                // DataStore
                implementation(libs.androidx.datastore.preferences)

                // Lifecycle (KMP)
                implementation(libs.androidx.lifecycle.viewmodel)
                implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
                implementation(libs.androidx.lifecycle.runtime.compose)

                // Coil (Multiplatform)
                implementation(libs.coil.mp)
            }
        }

        val androidMain by getting {
            dependencies {
                // SQLDelight Android driver
                implementation(libs.sqldelight.android.driver)

                // Coroutines Android
                implementation(libs.kotlinx.coroutines.android)

                // Koin Android
                implementation(libs.koin.android)
                implementation(libs.koin.compose.viewmodel)

                // AndroidX
                implementation(libs.androidx.core.ktx)
                implementation(libs.androidx.activity.compose)
                implementation(libs.androidx.lifecycle.viewmodel)
                implementation(libs.androidx.lifecycle.runtime.compose)
            }
        }

        val desktopMain by getting {
            dependencies {
                // SQLDelight JVM driver
                implementation(libs.sqldelight.jvm.driver)

                // Apache POI (XLSX parsing — JVM only)
                implementation(libs.poi)
                implementation(libs.poi.ooxml)

                // Coroutines Swing (for Desktop dispatcher)
                implementation(libs.kotlinx.coroutines.swing)

                // Koin (core only - no Android-specific)
                implementation(libs.koin.core)
            }
        }
    }
}

sqldelight {
    databases {
        create("MksDatabase") {
            packageName.set("com.ahmedyejam.mks.db")
        }
    }
}