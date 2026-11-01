package com.bungbung.diary.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bungbung.diary.R
import com.bungbung.diary.domain.Mood

// ── 캐릭터와 말풍선 테마 ───────────────────────────────────────────
// 남색 밤하늘 위에 하늘색 차 캐릭터가 말풍선으로 이야기해요.
// 차 화면은 멀리서 보니까 글자 대비를 4.5:1 이상으로 맞췄어요.

/** 동글동글한 제목·숫자 글꼴 */
val DisplayFont = FontFamily(Font(R.font.jua_regular))

/** 기본 UI·본문 글꼴 */
val UiFont = FontFamily(Font(R.font.gowun_dodum_regular))

// 상태 색
val Warning = Color(0xFFFFB74D)
val Ok = Color(0xFF8FE3B0)

// 캐릭터
val CarBody = Color(0xFF7CC4FF)
val CarWindow = Color(0xFFDDF0FF)
val CarInk = Color(0xFF10233A)

// 말풍선
val Bubble = Color(0xFFF4F8FF)
val BubbleInk = Color(0xFF10233A)

// 경고 띠
val AlertBg = Color(0xFF3A2A12)
val AlertInk = Color(0xFFFFE2B0)

/** 기분마다 캐릭터 뒤 무대 색이 바뀌어요. */
fun Mood.stageColor(): Color = when (this) {
    Mood.HAPPY -> Color(0xFF1E3A5F)
    Mood.WORRIED -> Color(0xFF4A3A16)
    Mood.CELEBRATE -> Color(0xFF352B5C)
    Mood.FOCUS -> Color(0xFF1E4636)
}

private val BubbleColors = darkColorScheme(
    primary = CarBody,
    onPrimary = Color(0xFF0F1B2A),
    primaryContainer = Color(0xFF1E3A5F),
    onPrimaryContainer = Color(0xFFEAF2FB),
    secondary = Color(0xFFFFD36E),
    onSecondary = Color(0xFF2B1C00),
    secondaryContainer = Color(0xFF1E3A5F),
    onSecondaryContainer = Color(0xFFEAF2FB),
    background = Color(0xFF0F1B2A),
    onBackground = Color(0xFFEAF2FB),
    surface = Color(0xFF182A3F),
    onSurface = Color(0xFFEAF2FB),
    surfaceVariant = Color(0xFF22374F),
    onSurfaceVariant = Color(0xFFA9BCD0),
    surfaceContainer = Color(0xFF182A3F),
    surfaceContainerHigh = Color(0xFF22374F),
    outline = Color(0xFF5D7690),
    outlineVariant = Color(0xFF2E4660),
    error = Color(0xFFFF8A80),
)

private fun TextStyle.ui() = copy(fontFamily = UiFont)

// 차 화면은 멀리서 보니까 기본보다 글자를 조금 키워요.
private val base = Typography()
private val BubbleTypography = Typography(
    displaySmall = base.displaySmall.copy(fontFamily = DisplayFont, fontSize = 44.sp),
    headlineMedium = base.headlineMedium.copy(fontFamily = DisplayFont, fontSize = 32.sp, lineHeight = 38.sp),
    headlineSmall = base.headlineSmall.copy(fontFamily = DisplayFont, fontSize = 28.sp),
    titleLarge = base.titleLarge.copy(fontFamily = DisplayFont, fontSize = 26.sp),
    titleMedium = base.titleMedium.copy(fontFamily = DisplayFont, fontSize = 21.sp),
    titleSmall = base.titleSmall.ui(),
    bodyLarge = base.bodyLarge.copy(fontFamily = UiFont, fontSize = 20.sp, lineHeight = 30.sp),
    bodyMedium = base.bodyMedium.copy(fontFamily = UiFont, fontSize = 17.sp),
    bodySmall = base.bodySmall.ui(),
    labelLarge = base.labelLarge.copy(fontFamily = UiFont, fontSize = 18.sp),
    labelMedium = base.labelMedium.ui(),
    labelSmall = base.labelSmall.ui(),
)

/** 말풍선 안 메시지 본문 */
val BubbleBody = TextStyle(fontFamily = UiFont, fontSize = 24.sp, lineHeight = 40.sp, color = BubbleInk)

private val BubbleShapes = Shapes(
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(32.dp),
)

@Composable
fun BungbungTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BubbleColors,
        typography = BubbleTypography,
        shapes = BubbleShapes,
        content = content,
    )
}
