import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.gms.google.services)
}

// ============================================================
// Load signing properties
// ============================================================

val keystorePropertiesFile =
    rootProject.file("keystore.properties")

val keystoreProperties =
    Properties()

if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(
        FileInputStream(keystorePropertiesFile)
    )
}

android {
    namespace = "com.example.tuoicay"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.tuoicay"

        minSdk = 24
        targetSdk = 34

        versionCode = 2
        versionName = "1.0.1"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"
    }

    // ========================================================
    // Signing
    // ========================================================

    signingConfigs {
        create("release") {

            if (keystorePropertiesFile.exists()) {

                storeFile =
                    file(
                        keystoreProperties[
                            "storeFile"
                        ] as String
                    )

                storePassword =
                    keystoreProperties[
                        "storePassword"
                    ] as String

                keyAlias =
                    keystoreProperties[
                        "keyAlias"
                    ] as String

                keyPassword =
                    keystoreProperties[
                        "keyPassword"
                    ] as String
            }
        }
    }

    // ========================================================
    // Build Types
    // ========================================================

    buildTypes {

        release {

            isMinifyEnabled = false

            signingConfig =
                signingConfigs.getByName("release")

            proguardFiles(
                getDefaultProguardFile(
                    "proguard-android-optimize.txt"
                ),
                "proguard-rules.pro"
            )
        }
    }

    // ========================================================
    // Java
    // ========================================================

    compileOptions {
        sourceCompatibility =
            JavaVersion.VERSION_11

        targetCompatibility =
            JavaVersion.VERSION_11
    }

    // ========================================================
    // Kotlin
    // ========================================================

    kotlinOptions {
        jvmTarget = "11"
    }

    // ========================================================
    // Compose
    // ========================================================

    buildFeatures {
        compose = true
    }
}

// ============================================================
// Dependencies
// ============================================================

dependencies {

    implementation(libs.appcompat)

    implementation(libs.material)

    implementation(
        libs.lifecycle.runtime.ktx
    )

    implementation(
        libs.activity.compose
    )

    implementation(
        platform(libs.compose.bom)
    )

    implementation(libs.ui)

    implementation(
        libs.ui.graphics
    )

    implementation(
        libs.ui.tooling.preview
    )

    implementation(
        libs.material3
    )

    implementation(libs.activity)

    implementation(
        libs.constraintlayout
    )

    implementation(
        libs.firebase.database
    )

    implementation(
        "com.google.android.flexbox:flexbox:3.0.0"
    )

    testImplementation(
        libs.junit
    )

    androidTestImplementation(
        libs.ext.junit
    )

    androidTestImplementation(
        libs.espresso.core
    )

    androidTestImplementation(
        platform(libs.compose.bom)
    )

    androidTestImplementation(
        libs.ui.test.junit4
    )

    debugImplementation(
        libs.ui.tooling
    )

    debugImplementation(
        libs.ui.test.manifest
    )
}