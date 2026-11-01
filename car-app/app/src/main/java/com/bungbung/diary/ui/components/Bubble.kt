package com.bungbung.diary.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bungbung.diary.domain.Mood
import com.bungbung.diary.ui.theme.AlertBg
import com.bungbung.diary.ui.theme.AlertInk
import com.bungbung.diary.ui.theme.Bubble
import com.bungbung.diary.ui.theme.BubbleBody
import com.bungbung.diary.ui.theme.BubbleInk
import com.bungbung.diary.ui.theme.CarBody
import com.bungbung.diary.ui.theme.CarInk
import com.bungbung.diary.ui.theme.CarWindow
import com.bungbung.diary.ui.theme.DisplayFont
import com.bungbung.diary.ui.theme.Warning
import com.bungbung.diary.ui.theme.stageColor

/**
 * 차 캐릭터 붕붕이. 기분에 따라 눈과 입이 바뀌어요.
 * TODO: 정식 캐릭터 일러스트가 나오면 이미지로 교체
 */
@Composable
fun CarCharacter(
    mood: Mood,
    modifier: Modifier = Modifier,
    width: Dp = 220.dp,
    body: Color = CarBody,
    window: Color = CarWindow,
    ink: Color = CarInk,
) {
    Canvas(
        modifier = modifier
            .size(width = width, height = width * (160f / 220f))
            .semantics { contentDescription = "차 캐릭터, 기분: ${mood.label}" },
    ) {
        val s = size.width / 220f
        fun p(x: Float, y: Float) = Offset(x * s, y * s)

        // 지붕 + 창문
        drawPath(Path().apply {
            moveTo(58 * s, 64 * s); quadraticTo(70 * s, 22 * s, 110 * s, 22 * s)
            quadraticTo(150 * s, 22 * s, 162 * s, 64 * s); close()
        }, body)
        drawPath(Path().apply {
            moveTo(74 * s, 62 * s); quadraticTo(84 * s, 36 * s, 110 * s, 36 * s)
            quadraticTo(136 * s, 36 * s, 146 * s, 62 * s); close()
        }, window)
        // 몸통
        drawRoundRect(body, topLeft = p(18f, 58f), size = Size(184 * s, 64 * s), cornerRadius = CornerRadius(30 * s))
        // 전조등
        drawRoundRect(Color(0xFFFFE7A3), topLeft = p(24f, 80f), size = Size(12 * s, 8 * s), cornerRadius = CornerRadius(4 * s))
        drawRoundRect(Color(0xFFFFE7A3), topLeft = p(184f, 80f), size = Size(12 * s, 8 * s), cornerRadius = CornerRadius(4 * s))
        // 바퀴
        listOf(58f, 162f).forEach { cx ->
            drawCircle(ink, 17 * s, p(cx, 124f))
            drawCircle(Color(0xFF9FB3C8), 7 * s, p(cx, 124f))
        }

        val stroke = Stroke(width = 4.5f * s, cap = StrokeCap.Round)
        // 눈
        when (mood) {
            Mood.FOCUS -> {
                drawLine(ink, p(77f, 87f), p(91f, 87f), strokeWidth = 4.5f * s, cap = StrokeCap.Round)
                drawLine(ink, p(129f, 87f), p(143f, 87f), strokeWidth = 4.5f * s, cap = StrokeCap.Round)
            }
            Mood.CELEBRATE -> {
                // 웃는 눈 ^ ^
                drawPath(Path().apply { moveTo(76 * s, 90 * s); quadraticTo(84 * s, 78 * s, 92 * s, 90 * s) }, ink, style = stroke)
                drawPath(Path().apply { moveTo(128 * s, 90 * s); quadraticTo(136 * s, 78 * s, 144 * s, 90 * s) }, ink, style = stroke)
            }
            else -> {
                drawCircle(ink, 7.5f * s, p(84f, 87f))
                drawCircle(ink, 7.5f * s, p(136f, 87f))
                drawCircle(Color.White, 2.4f * s, p(86.5f, 84.5f))
                drawCircle(Color.White, 2.4f * s, p(138.5f, 84.5f))
            }
        }
        // 볼
        if (mood == Mood.HAPPY || mood == Mood.CELEBRATE) {
            drawCircle(Color(0xFFFF9BB0).copy(alpha = 0.75f), 7 * s, p(66f, 101f))
            drawCircle(Color(0xFFFF9BB0).copy(alpha = 0.75f), 7 * s, p(154f, 101f))
        }
        // 입
        val mouth = Path().apply {
            when (mood) {
                Mood.HAPPY -> { moveTo(98 * s, 101 * s); quadraticTo(110 * s, 112 * s, 122 * s, 101 * s) }
                Mood.CELEBRATE -> { moveTo(94 * s, 99 * s); quadraticTo(110 * s, 118 * s, 126 * s, 99 * s) }
                Mood.WORRIED -> { moveTo(99 * s, 108 * s); quadraticTo(110 * s, 99 * s, 121 * s, 108 * s) }
                Mood.FOCUS -> { moveTo(101 * s, 104 * s); lineTo(119 * s, 104 * s) }
            }
        }
        drawPath(mouth, ink, style = stroke)
    }
}

/** 기분 색 무대 위에 캐릭터 + 이름 + 기분 칩 */
@Composable
fun CharacterStage(
    mood: Mood,
    name: String,
    caption: String,
    modifier: Modifier = Modifier,
    characterWidth: Dp = 220.dp,
) {
    Column(
        modifier = modifier
            .background(mood.stageColor(), MaterialTheme.shapes.large)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
    ) {
        CarCharacter(mood = mood, width = characterWidth)
        Text(name, fontFamily = DisplayFont, fontSize = 38.sp, lineHeight = 42.sp)
        Text(
            caption,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .background(Color.White.copy(alpha = 0.14f), CircleShape)
                .padding(horizontal = 16.dp, vertical = 6.dp),
        )
    }
}

/**
 * 캐릭터가 하는 말을 담는 흰 말풍선. 왼쪽에 꼬리가 달려 캐릭터 쪽을 가리켜요.
 */
@Composable
fun SpeechBubble(
    modifier: Modifier = Modifier,
    tailTop: Dp = 48.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Row(modifier = modifier) {
        Column {
            Spacer(Modifier.size(tailTop))
            Canvas(Modifier.size(width = 18.dp, height = 32.dp)) {
                drawPath(Path().apply {
                    moveTo(size.width, 0f); lineTo(0f, size.height / 2); lineTo(size.width, size.height); close()
                }, Bubble)
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .background(Bubble, MaterialTheme.shapes.large)
                .padding(horizontal = 36.dp, vertical = 30.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
            content = content,
        )
    }
}

/** 말풍선 본문 */
@Composable
fun BubbleText(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier = modifier, style = BubbleBody)
}

/** 큰 숫자 + 작은 단위 타일. "37.2 km" */
@Composable
fun StatTile(label: String, value: String, unit: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium)
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            buildAnnotatedString {
                append(value)
                withStyle(SpanStyle(fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)) { append(" $unit") }
            },
            fontFamily = DisplayFont,
            fontSize = 36.sp,
            lineHeight = 42.sp,
        )
    }
}

/** 말풍선 안 진한 알약 버튼 */
@Composable
fun BubbleButton(label: String, onClick: () -> Unit, leading: (@Composable () -> Unit)? = null) {
    Button(
        onClick = onClick,
        modifier = Modifier.heightIn(min = 60.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = BubbleInk, contentColor = Color.White),
        contentPadding = PaddingValues(horizontal = 28.dp),
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(10.dp))
        }
        Text(label, style = MaterialTheme.typography.labelLarge.copy(fontSize = 20.sp))
    }
}

/** 말풍선 안 테두리 알약 버튼 */
@Composable
fun BubbleOutlinedButton(label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.heightIn(min = 60.dp),
        shape = CircleShape,
        border = BorderStroke(2.dp, BubbleInk),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = BubbleInk),
        contentPadding = PaddingValues(horizontal = 28.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge.copy(fontSize = 20.sp))
    }
}

/** 말풍선 아래 주황 경고 띠. "왼쪽 뒤 타이어 28 psi · 이번 주 안에" */
@Composable
fun AlertStrip(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
) {
    Row(
        modifier = modifier
            .background(AlertBg, MaterialTheme.shapes.large)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        WarningIcon()
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = Color.White, fontFamily = DisplayFont)) { append(title) }
                append(" · $body")
            },
            style = MaterialTheme.typography.bodyLarge,
            color = AlertInk,
            modifier = Modifier.weight(1f),
        )
        if (actionLabel != null) {
            Button(
                onClick = onAction,
                modifier = Modifier.heightIn(min = 52.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Warning, contentColor = Color(0xFF2B1C00)),
            ) { Text(actionLabel, style = MaterialTheme.typography.labelLarge) }
        }
    }
}

@Composable
private fun WarningIcon(size: Dp = 28.dp) {
    Canvas(Modifier.size(size)) {
        val s = this.size.width / 24f
        val st = Stroke(width = 2f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawPath(Path().apply { moveTo(12 * s, 3 * s); lineTo(2 * s, 20 * s); lineTo(22 * s, 20 * s); close() }, Warning, style = st)
        drawLine(Warning, Offset(12 * s, 10 * s), Offset(12 * s, 14 * s), strokeWidth = 2f * s, cap = StrokeCap.Round)
        drawCircle(Warning, 1.2f * s, Offset(12 * s, 17 * s))
    }
}

/** 스피커 아이콘 (core 아이콘 세트에 없어서 직접 그려요). 색은 LocalContentColor를 따라가요. */
@Composable
fun SpeakerIcon(modifier: Modifier = Modifier, size: Dp = 24.dp) {
    val color = LocalContentColor.current
    Canvas(modifier.size(size)) {
        val s = this.size.width / 24f
        val st = Stroke(width = 2f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawPath(Path().apply {
            moveTo(11 * s, 5 * s); lineTo(6 * s, 9 * s); lineTo(3 * s, 9 * s); lineTo(3 * s, 15 * s)
            lineTo(6 * s, 15 * s); lineTo(11 * s, 19 * s); close()
        }, color, style = st)
        drawArc(color, -50f, 100f, false, topLeft = Offset(10.5f * s, 7f * s), size = Size(10f * s, 10f * s), style = st)
    }
}

/** 작은 동그라미 안 캐릭터 (일기장 목록용) */
@Composable
fun CharacterBadge(mood: Mood, size: Dp = 72.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .background(mood.stageColor(), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        CarCharacter(mood = mood, width = size * 0.8f)
    }
}
