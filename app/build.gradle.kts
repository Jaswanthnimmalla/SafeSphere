
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.runanywhere.startup_hackathon20"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.runanywhere.startup_hackathon20"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // For Play Store
        setProperty("archivesBaseName", "SafeSphere-v$versionName")
    }

    signingConfigs {
        create("release") {
            // Read password from gradle.properties
            // Password: SafeSphere2025
            storeFile = file("../safesphere-release.keystore")
            storePassword = "SafeSphere2025"
            keyAlias = "safesphere"
            keyPassword = "SafeSphere2025"
        }
    }

    buildTypes {
        release {
            // Enable code shrinking, obfuscation, and optimization
            isMinifyEnabled = true
            isShrinkResources = true

            // Use R8 for code optimization
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            // Sign with release key
            signingConfig = signingConfigs.getByName("release")

            // Optimize for Play Store
            isDebuggable = false
            isJniDebuggable = false
            renderscriptOptimLevel = 3
        }

        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-DEBUG"
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }

    // Optimize APK size for Play Store
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/LICENSE*"
            excludes += "/META-INF/NOTICE*"
        }
    }
}

dependencies {
    // RunAnywhere SDK - Local AARs from GitHub Release v0.1.3-alpha
    // Core SDK (4.01MB)
    implementation(files("libs/RunAnywhereKotlinSDK-release.aar"))
    // LLM Module (2.12MB) - includes llama.cpp with 7 ARM64 CPU variants
    implementation(files("libs/runanywhere-llm-llamacpp-release.aar"))

    // Required SDK dependencies (transitive dependencies from AARs)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")

    // Ktor for networking (required by SDK)
    implementation("io.ktor:ktor-client-core:3.0.3")
    implementation("io.ktor:ktor-client-okhttp:3.0.3")
    implementation("io.ktor:ktor-client-content-negotiation:3.0.3")
    implementation("io.ktor:ktor-client-logging:3.0.3")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.0.3")

    // OkHttp (required by SDK)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Retrofit (required by SDK)
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")

    // Gson (required by SDK)
    implementation("com.google.code.gson:gson:2.11.0")

    // Okio (required by SDK)
    implementation("com.squareup.okio:okio:3.9.1")

    // AndroidX WorkManager (required by SDK)
    implementation("androidx.work:work-runtime-ktx:2.10.0")

    // AndroidX Room (required by SDK)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")

    // AndroidX Security (required by SDK)
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // AndroidX Biometric - for biometric authentication
    implementation("androidx.biometric:biometric:1.2.0-alpha05")

    // QR Code generation - for desktop pairing
    implementation("com.google.zxing:core:3.5.3")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")

    // NanoHTTPD - lightweight HTTP server for local network sync
    implementation("org.nanohttpd:nanohttpd:2.3.1")
    implementation("org.nanohttpd:nanohttpd-websocket:2.3.1")

    // CameraX - for secure in-app camera
    // REMOVED - Camera feature removed to reduce app size
    // implementation("androidx.camera:camera-core:1.3.1")
    // implementation("androidx.camera:camera-camera2:1.3.1")
    // implementation("androidx.camera:camera-lifecycle:1.3.1")
    // implementation("androidx.camera:camera-view:1.3.1")

    // Standard app dependencies
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}