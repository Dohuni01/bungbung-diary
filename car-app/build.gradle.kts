// 최상위 빌드 파일. 모듈별 설정은 app/build.gradle.kts에 있어요.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
