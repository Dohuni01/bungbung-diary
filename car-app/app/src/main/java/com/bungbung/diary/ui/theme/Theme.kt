package com.bungbung.diary.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

val Warning = Color(0xFFFFB74D)
val Ok = Color(0xFF81C784)

private val CarColors = darkColorScheme(
    primary = Color(0xFF7CC4FF),
    onPrimary = Color(0xFF00213A),
    secondary = Color(0xFFFFC76B),
    onSecondary = Color(0xFF2B1C00),
    background = Color(0xFF101418),
    onBackground = Color(0xFFE6E9EE),
    surface = Color(0xFF161B21),
    onSurface = Color(0xFFE6E9EE),
    surfaceVariant = Color(0xFF222932),
    onSurfaceVariant = Color(0xFFB8C0CA),
    error = Color(0xFFFF8A80),
)

// 차 화면은 멀리서 보니까 기본보다 글자를 조금 키워요.
private val base = Typography()
private val CarTypography = Typography(
    headlineMedium = base.headlineMedium.copy(fontSize = 30.sp),
    titleLarge = base.titleLarge.copy(fontSize = 24.sp),
    titleMedium = base.titleMedium.copy(fontSize = 20.sp),
    bodyLarge = base.bodyLarge.copy(fontSize = 20.sp, lineHeight = 30.sp),
    bodyMedium = base.bodyMedium.copy(fontSize = 17.sp),
    labelLarge = base.labelLarge.copy(fontSize = 17.sp),
)

@Composable
fun BungbungTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = CarColors, typography = CarTypography, content = content)
}
