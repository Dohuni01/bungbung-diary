plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.bungbung.diary"
    // Pleos 시작 가이드: Android SDK API 34, minSdk 28 이상
    compileSdk = 34

    defaultConfig {
        applicationId = "com.bungbung.diary"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
    testOptions {
        unitTests.all {
            it.useJUnitPlatform()
            it.jvmArgs("-Dfile.encoding=UTF-8")
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.ui.tooling)

    // Pleos SDK
    // https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/Vehicle/intro
    implementation(libs.pleos.vehicle)
    // https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/SpeechToText/intro
    implementation(libs.pleos.stt)
    // https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/TextToSpeech/intro
    implementation(libs.pleos.tts)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.coroutines.android)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
}
