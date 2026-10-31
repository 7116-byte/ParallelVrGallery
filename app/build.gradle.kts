plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.local.parallelvrgallery"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.local.parallelvrgallery"
        minSdk = 26
        targetSdk = 35
        versionCode = 30100
        versionName = "3.1.00"
        testInstrumentationRunner = "com.local.parallelvrgallery.ViewerSmokeInstrumentation"
    }

    // Historical releases use this exact debug certificate. Never use a new host's default key.
    signingConfigs.getByName("debug") {
        storeFile = rootProject.file(".local/signing/debug.keystore")
        storePassword = "android"
        keyAlias = "androiddebugkey"
        keyPassword = "android"
    }

    buildTypes.getByName("release") {
        isDebuggable = false
        isMinifyEnabled = false
        // Build type and certificate are independent: preserve upgrade compatibility.
        signingConfig = signingConfigs.getByName("debug")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    androidResources {
        noCompress += "tflite"
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.compose.material3:material3:1.3.1")
    implementation("androidx.compose.material:material-icons-extended:1.7.6")
    implementation("androidx.compose.ui:ui:1.7.6")
    implementation("androidx.compose.ui:ui-tooling-preview:1.7.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("org.tensorflow:tensorflow-lite:2.16.1")
    implementation("org.tensorflow:tensorflow-lite-gpu-api:2.16.1")
    implementation("org.tensorflow:tensorflow-lite-gpu:2.16.1")
    debugImplementation("androidx.compose.ui:ui-tooling:1.7.6")
}
