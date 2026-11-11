import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// Pleos LLM(SERVER 모드) 자격 증명 — local.properties에만 적는다. 없으면 빈 값 → LLM 비활성(템플릿만).
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
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

        buildConfigField("String", "PLEOS_CLIENT_ID", "\"${localProps.getProperty("PLEOS_CLIENT_ID", "")}\"")
        buildConfigField("String", "PLEOS_CLIENT_SECRET", "\"${localProps.getProperty("PLEOS_CLIENT_SECRET", "")}\"")
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
        buildConfig = true
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
    // https://document.pleos.ai/en/api-reference/connect-sdk-pleos/NaviHelper/initialization-and-release (AAR 3.0.5에서 시그니처 확인, 2026-11-11)
    implementation(libs.pleos.navi)
    // https://document.pleos.ai/en/api-reference/connect-sdk-pleos/LLM/make-the-first-api-call (AAR 2.1.3.2에서 시그니처 확인, 2026-11-11)
    implementation(libs.pleos.llm)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.coroutines.android)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
}
