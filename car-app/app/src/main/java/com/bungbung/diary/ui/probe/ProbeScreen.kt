package com.bungbung.diary.ui.probe

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.reflect.InvocationTargetException
import java.util.Locale

// ── 모델 ──────────────────────────────────────────────────────────────

enum class ProbeStatus { PENDING, OK, FAIL, SKIP }

data class ProbeItem(
    val name: String,
    val api: String = "",
    val status: ProbeStatus = ProbeStatus.PENDING,
    val value: String = "",
    val note: String = "",
)

// ── AAOS 표준 API 조사 (reflection: android.car 없어도 컴파일됨) ─────

/**
 * android.car.Car 를 reflection 으로 접근해서 표준 차량 속성을 읽는다.
 * 표준 Automotive AVD에서 실행해야 의미 있는 결과가 나온다.
 */
private fun probeAaos(context: Context): List<ProbeItem> {
    val out = mutableListOf<ProbeItem>()

    // 1. android.car.Car 클래스 존재 확인
    val carClass = try {
        Class.forName("android.car.Car")
    } catch (e: ClassNotFoundException) {
        out += ProbeItem("android.car.Car", status = ProbeStatus.FAIL,
            note = "AAOS 에뮬레이터/기기가 아님 (ClassNotFoundException)")
        return out
    }
    out += ProbeItem("android.car.Car 클래스", "android.car.Car", ProbeStatus.OK)

    // 2. Car 인스턴스 생성
    val car = try {
        // createCar(Context) 또는 createCar(Context, Handler) 시도
        val method = try {
            carClass.getMethod("createCar", Context::class.java)
        } catch (_: NoSuchMethodException) {
            carClass.getMethod("createCar", Context::class.java, Handler::class.java)
        }
        if (method.parameterTypes.size == 1) {
            method.invoke(null, context)
        } else {
            method.invoke(null, context, Handler(Looper.getMainLooper()))
        }
    } catch (e: Exception) {
        out += ProbeItem("Car.createCar()", status = ProbeStatus.FAIL, note = e.unwrap())
        return out
    } ?: run {
        out += ProbeItem("Car.createCar()", status = ProbeStatus.FAIL, note = "null 반환 (Car 서비스 없음)")
        return out
    }
    out += ProbeItem("Car.createCar()", "Car.createCar(Context)", ProbeStatus.OK)

    // 3. CarPropertyManager 획득
    val pm = try {
        val propertyService = carClass.getField("PROPERTY_SERVICE").get(null) as? String ?: "property"
        carClass.getMethod("getCarManager", String::class.java).invoke(car, propertyService)
    } catch (e: Exception) {
        out += ProbeItem("CarPropertyManager", status = ProbeStatus.FAIL, note = e.unwrap())
        return out
    } ?: run {
        out += ProbeItem("CarPropertyManager", status = ProbeStatus.FAIL, note = "null 반환")
        return out
    }
    out += ProbeItem("CarPropertyManager", "Car.getCarManager(PROPERTY_SERVICE)", ProbeStatus.OK)

    val pmClass = pm.javaClass

    // 4. 속성별 읽기 시도
    fun tryInt(name: String, propId: Int): ProbeItem = try {
        val v = pmClass.getMethod("getIntProperty", Int::class.java, Int::class.java)
            .invoke(pm, propId, 0)
        ProbeItem(name, "getIntProperty($propId)", ProbeStatus.OK, value = "$v")
    } catch (e: Exception) {
        val cause = (e as? InvocationTargetException)?.cause ?: e
        ProbeItem(name, "getIntProperty($propId)", ProbeStatus.FAIL,
            note = if (cause is SecurityException) "권한 없음 → Manifest 확인" else cause.message ?: "오류")
    }

    fun tryFloat(name: String, propId: Int): ProbeItem = try {
        val v = pmClass.getMethod("getFloatProperty", Int::class.java, Int::class.java)
            .invoke(pm, propId, 0) as Float
        ProbeItem(name, "getFloatProperty($propId)", ProbeStatus.OK, value = "${"%.2f".format(v)}")
    } catch (e: Exception) {
        val cause = (e as? InvocationTargetException)?.cause ?: e
        ProbeItem(name, "getFloatProperty($propId)", ProbeStatus.FAIL,
            note = if (cause is SecurityException) "권한 없음 → Manifest 확인" else cause.message ?: "오류")
    }

    // VehiclePropertyIds 상수 (AAOS API 34 기준, 안정 값)
    out += tryInt("기어 GEAR_SELECTION", 289408000)
    out += tryFloat("속도 PERF_VEHICLE_SPEED (m/s)", 291504647)
    out += tryFloat("외기온도 ENV_OUTSIDE_TEMPERATURE (°C)", 291505923)
    out += tryFloat("누적거리 ODOMETER (km)", 291504644)
    out += tryFloat("연료 FUEL_LEVEL (%)", 291504643)
    out += tryFloat("배터리 EV_BATTERY_LEVEL (%)", 291504905)
    out += tryFloat("히터 설정온도 HVAC_TEMPERATURE_SET", 358614546)

    return out
}

private fun Exception.unwrap(): String {
    val cause = (this as? InvocationTargetException)?.cause ?: this
    return cause.message ?: javaClass.simpleName
}

private fun probePleosSdk(label: String, className: String, note: String): ProbeItem {
    return try {
        Class.forName(className)
        ProbeItem(label, className, ProbeStatus.OK, note = "클래스 로드 성공")
    } catch (_: ClassNotFoundException) {
        ProbeItem(label, className, ProbeStatus.SKIP, note = note)
    }
}

private fun probePleosVehicleSdk() = probePleosSdk(
    "Pleos Vehicle SDK",
    "ai.pleos.playground.vehicle.Vehicle",
    "libs.pleos.vehicle 의존성 또는 Maven URL 확인 필요",
)

private fun probePleosTtsSdk() = probePleosSdk(
    "Pleos TTS SDK",
    "ai.pleos.playground.tts.TextToSpeech",
    "libs.pleos.tts 의존성 또는 Maven URL 확인 필요",
)

private fun probePleosSttSdk() = probePleosSdk(
    "Pleos STT SDK",
    "ai.pleos.playground.stt.SpeechToText",
    "libs.pleos.stt 의존성 또는 Maven URL 확인 필요",
)

private val PLEOS_STATIC_ITEMS = listOf(
    ProbeItem("Pleos LLM SDK", "ai.pleos.playground.llm.LLM",
        ProbeStatus.SKIP, note = "Gradle 좌표 미공개 (API Reference만 존재) — 에뮬레이터 확인 필요"),
    ProbeItem("CarUxRestrictionsManager", "AAOS CarUxRestrictionsManager",
        ProbeStatus.SKIP, note = "Phase 2에서 DRIVING_POLICY 콜백 등록 예정"),
)

// ── 화면 ──────────────────────────────────────────────────────────────

@Composable
fun ProbeScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    var aaosItems by remember { mutableStateOf<List<ProbeItem>?>(null) }
    var ttsItem by remember { mutableStateOf<ProbeItem?>(null) }
    var ttsEngine by remember { mutableStateOf<TextToSpeech?>(null) }
    var pleosSdkItems by remember { mutableStateOf<List<ProbeItem>?>(null) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            aaosItems = probeAaos(context)
            pleosSdkItems = listOf(
                probePleosVehicleSdk(),
                probePleosTtsSdk(),
                probePleosSttSdk(),
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose { ttsEngine?.shutdown() }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text("← 뒤로") }
                Spacer(Modifier.width(8.dp))
                Text("Phase 0  API 조사", style = MaterialTheme.typography.headlineSmall)
            }
        }

        // ── AAOS 표준 ──
        item { SectionHeader("AAOS 표준 (CarPropertyManager)") }

        if (aaosItems == null) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
                    CircularProgressIndicator(Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("조사 중...", style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            items(aaosItems!!) { ProbeRow(it) }
        }

        // ── Android TTS ──
        item { SectionHeader("Android 시스템 TTS") }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ttsItem?.let { ProbeRow(it) }
                OutlinedButton(
                    onClick = {
                        ttsItem = ProbeItem("TTS 초기화 중...", status = ProbeStatus.PENDING)
                        ttsEngine?.shutdown()
                        ttsEngine = TextToSpeech(context) { status ->
                            if (status != TextToSpeech.SUCCESS) {
                                ttsItem = ProbeItem("Android TTS", "TextToSpeech", ProbeStatus.FAIL,
                                    note = "초기화 실패 (status=$status)")
                                return@TextToSpeech
                            }
                            val engine = ttsEngine ?: return@TextToSpeech
                            val langResult = engine.setLanguage(Locale.KOREAN)
                            if (langResult < TextToSpeech.LANG_AVAILABLE) {
                                ttsItem = ProbeItem("Android TTS 한국어", "TextToSpeech", ProbeStatus.FAIL,
                                    note = "한국어 미지원 (result=$langResult)")
                                return@TextToSpeech
                            }
                            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                                override fun onStart(id: String?) {
                                    ttsItem = ProbeItem("Android TTS 한국어", "TextToSpeech",
                                        ProbeStatus.PENDING, note = "발화 중...")
                                }
                                override fun onDone(id: String?) {
                                    ttsItem = ProbeItem("Android TTS 한국어", "TextToSpeech",
                                        ProbeStatus.OK, note = "한국어 발화 완료")
                                }
                                @Deprecated("Deprecated in Java")
                                override fun onError(id: String?) {
                                    ttsItem = ProbeItem("Android TTS 한국어", "TextToSpeech",
                                        ProbeStatus.FAIL, note = "발화 오류")
                                }
                            })
                            engine.speak(
                                "붕붕일기 한국어 음성 테스트입니다.",
                                TextToSpeech.QUEUE_FLUSH, null, "probe",
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("한국어 TTS 테스트 실행") }
            }
        }

        // ── Pleos SDK ──
        item { SectionHeader("Pleos SDK") }
        if (pleosSdkItems == null) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
                    CircularProgressIndicator(Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("조사 중...", style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            items(pleosSdkItems!!) { ProbeRow(it) }
        }
        items(PLEOS_STATIC_ITEMS) { ProbeRow(it) }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Column(Modifier.padding(top = 8.dp)) {
        HorizontalDivider()
        Text(title, style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 6.dp, bottom = 2.dp))
    }
}

@Composable
private fun ProbeRow(item: ProbeItem) {
    val (bg, badge) = when (item.status) {
        ProbeStatus.OK -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
        ProbeStatus.FAIL -> Color(0xFFFFEBEE) to MaterialTheme.colorScheme.error
        ProbeStatus.SKIP -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.outline
        ProbeStatus.PENDING -> MaterialTheme.colorScheme.surface to MaterialTheme.colorScheme.primary
    }
    val label = when (item.status) {
        ProbeStatus.OK -> "됨"
        ProbeStatus.FAIL -> "안 됨"
        ProbeStatus.SKIP -> "확인 불가"
        ProbeStatus.PENDING -> "조사중"
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = bg),
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(item.name, style = MaterialTheme.typography.bodyMedium)
                if (item.value.isNotEmpty())
                    Text(item.value, style = MaterialTheme.typography.bodySmall, color = badge)
                if (item.note.isNotEmpty())
                    Text(item.note, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(8.dp))
            Surface(color = badge, shape = RoundedCornerShape(4.dp)) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
            }
        }
    }
}
