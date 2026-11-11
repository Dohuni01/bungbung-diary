package com.bungbung.diary.ui

import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bungbung.diary.BuildConfig
import com.bungbung.diary.BungbungApplication
import com.bungbung.diary.data.local.CarProfileEntity
import com.bungbung.diary.data.local.CarProfileRepository
import com.bungbung.diary.data.llm.PleosLlmClient
import com.bungbung.diary.data.navi.PleosNaviDataSource
import com.bungbung.diary.data.vehicle.pleos.PleosVehicleDataSource
import com.bungbung.diary.domain.Tone
import com.bungbung.diary.domain.destination.DestinationThoughtService
import com.bungbung.diary.ui.booking.BookingScreen
import com.bungbung.diary.ui.main.MainScreen
import com.bungbung.diary.ui.onboarding.OnboardingScreen
import com.bungbung.diary.ui.probe.ProbeScreen
import com.bungbung.diary.ui.settings.SettingsScreen
import com.bungbung.diary.ui.station.GasStationScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object Routes {
    const val ONBOARDING = "onboarding"
    const val MAIN = "main"
    const val SETTINGS = "settings"
    const val BOOKING = "booking"
    const val STATIONS = "stations"
    const val PROBE = "probe"
}

/**
 * 화면 이동 구조
 *
 * 첫 실행 세팅(onboarding) → 메인(main: 오늘 / 일기장 / 차 건강 탭)
 *                              ├─ 설정(settings)
 *                              ├─ 정비소 예약(booking)
 *                              └─ 주유소 목록(stations)
 */
@Composable
fun BungbungApp(initialNickname: String? = null) {
    val state = remember { AppState() }
    val context = LocalContext.current
    var startDest by remember { mutableStateOf<String?>(null) }
    val nav = rememberNavController()
    val scope = rememberCoroutineScope()

    // 목적지 한마디: 분류(규칙) → 민감하면 고정 문장, 아니면 LLM+템플릿 fallback → 말풍선+표정+음성
    val llmClient = remember {
        PleosLlmClient(context.applicationContext, BuildConfig.PLEOS_CLIENT_ID, BuildConfig.PLEOS_CLIENT_SECRET)
    }
    val thoughtService = remember { DestinationThoughtService(llmClient) }
    val voiceManager = (context.applicationContext as? BungbungApplication)?.voiceManager
    val onDestinationSet: (String) -> Unit = { name ->
        scope.launch {
            val thought = withContext(Dispatchers.IO) {
                thoughtService.thought(name, state.tone, state.nickname)
            }
            state.destinationThought = thought
            voiceManager?.speak(thought.message)
        }
    }

    // Pleos Vehicle SDK → AppState 실시간 연결
    DisposableEffect(Unit) {
        val dataSource = PleosVehicleDataSource(context)
        val dsScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        // initialize()는 fire-and-forget — 성공 여부와 무관하게 아래 수집기는 항상 돈다.
        dsScope.launch {
            try { dataSource.initialize() }
            catch (e: Exception) { Log.w("BungbungApp", "Vehicle SDK init 실패: ${e.message}") }
        }
        // 차량 상태(기어·온도·연료·타이어·문·벨트 등) — StateFlow emit 시마다 반영
        dsScope.launch {
            dataSource.vehicleStateFlow().collect { vs ->
                withContext(Dispatchers.Main) {
                    state.liveState    = vs
                    state.gear         = vs.gear
                    state.outsideTempC = vs.outsideTempC
                    state.fuelPercent  = vs.energy.percent
                    state.fuelLow      = vs.energy.percent in 1..20
                    state.tires        = vs.tires.asList()
                    state.tireLow      = vs.tires.anyLow()
                    state.engineLight  = vs.safetyWarnings.any { it.code == "ENGINE" }
                }
            }
        }
        // DailySummary(와이퍼·급제동·거리 등) — 1초 타이머로 강제 폴링
        // 단, 안전 알림 재현 중에는 시연용으로 세팅한 today(예: 졸음운전 125분)를 덮지 않는다.
        dsScope.launch {
            while (true) {
                kotlinx.coroutines.delay(1_000L)
                val summary = dataSource.getDailySummary()
                withContext(Dispatchers.Main) {
                    // 수동 재현(졸음 등 today를 세팅한 경우)만 덮지 않음. 자동 감지는 실데이터 유지.
                    if (state.manualSafetyAlert == null) state.today = summary
                }
            }
        }
        onDispose {
            dataSource.release()
            dsScope.cancel()
        }
    }

    // Pleos NaviHelper → 목적지 한마디 (에뮬레이터 내비에서 목적지를 정하면 자동 감지)
    DisposableEffect(Unit) {
        llmClient.start()
        val naviSource = PleosNaviDataSource(context.applicationContext)
        naviSource.onDestinationSet = onDestinationSet
        naviSource.onDestinationCleared = {
            voiceManager?.stopSpeaking()
            state.clearDestination()
        }
        naviSource.start()
        onDispose {
            naviSource.release()
            llmClient.release()
        }
    }

    // 앱 시작 시 프로필 로드 — 온보딩 완료 여부 판단
    LaunchedEffect(Unit) {
        val profile = CarProfileRepository.get()
        if (profile?.onboardingCompletedAt != null) {
            state.nickname     = profile.nickname
            state.tone         = runCatching { Tone.valueOf(profile.tone) }.getOrDefault(Tone.FRIEND)
            state.deliveryDate = profile.deliveryDateIso
            state.diaryHour    = profile.diaryHour
            state.diaryMinute  = profile.diaryMinute
            startDest = Routes.MAIN
        } else {
            if (!initialNickname.isNullOrBlank()) state.nickname = initialNickname.trim().take(8)
            startDest = Routes.ONBOARDING
        }
    }

    if (startDest == null) return  // 프로필 로드 중 — 빈 화면 (짧음); nav/scope는 위에서 이미 호출됨

    NavHost(navController = nav, startDestination = startDest!!) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                state = state,
                onFinish = {
                    scope.launch {
                        CarProfileRepository.upsert(
                            CarProfileEntity(
                                nickname = state.nickname,
                                tone = state.tone.name,
                                deliveryDateIso = state.deliveryDate,
                                onboardingCompletedAt = System.currentTimeMillis(),
                            )
                        )
                        nav.navigate(Routes.MAIN) {
                            popUpTo(Routes.ONBOARDING) { inclusive = true }
                        }
                    }
                },
            )
        }
        composable(Routes.MAIN) {
            MainScreen(
                state = state,
                onOpenSettings = { nav.navigate(Routes.SETTINGS) },
                onOpenBooking  = { nav.navigate(Routes.BOOKING) },
                onOpenStations = { nav.navigate(Routes.STATIONS) },
                onOpenProbe    = { nav.navigate(Routes.PROBE) },
                onSetDestination = onDestinationSet,
            )
        }
        composable(Routes.PROBE) {
            ProbeScreen(onBack = { nav.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                state = state,
                onBack = { nav.popBackStack() },
                onRedoOnboarding = {
                    nav.navigate(Routes.ONBOARDING) {
                        popUpTo(Routes.MAIN) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.STATIONS) {
            GasStationScreen(
                state = state,
                onBack = { nav.popBackStack() },
            )
        }
        composable(Routes.BOOKING) {
            BookingScreen(
                state = state,
                onBack = { nav.popBackStack() },
            )
        }
    }
}
