package com.bungbung.diary.ui

import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bungbung.diary.data.local.CarProfileEntity
import com.bungbung.diary.data.local.CarProfileRepository
import com.bungbung.diary.data.vehicle.pleos.PleosVehicleDataSource
import com.bungbung.diary.domain.Tone
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

    // Pleos Vehicle SDK → AppState 실시간 연결
    DisposableEffect(Unit) {
        val dataSource = PleosVehicleDataSource(context)
        val dsScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        dsScope.launch {
            try {
                dataSource.initialize()
                // 차량 상태(기어·온도·연료·타이어) — StateFlow emit 시마다 반영
                launch {
                    dataSource.vehicleStateFlow().collect { vs ->
                        withContext(Dispatchers.Main) {
                            state.gear        = vs.gear
                            state.outsideTempC = vs.outsideTempC
                            state.fuelPercent  = vs.energy.percent
                            state.fuelLow      = vs.energy.percent <= 15
                            state.tires        = vs.tires.asList()
                            state.tireLow      = vs.tires.anyLow()
                            state.engineLight  = vs.safetyWarnings.any { it.code == "ENGINE" }
                        }
                    }
                }
                // DailySummary(와이퍼·급제동 등) — 1초 타이머로 강제 폴링
                launch {
                    while (true) {
                        kotlinx.coroutines.delay(1_000L)
                        val summary = dataSource.getDailySummary()
                        withContext(Dispatchers.Main) { state.today = summary }
                    }
                }
            } catch (e: Exception) {
                Log.w("BungbungApp", "Vehicle SDK 연결 실패 (Fake 유지): ${e.message}")
            }
        }
        onDispose {
            dataSource.release()
            dsScope.cancel()
        }
    }

    // 앱 시작 시 프로필 로드 — 온보딩 완료 여부 판단
    LaunchedEffect(Unit) {
        val profile = CarProfileRepository.get()
        if (profile?.onboardingCompletedAt != null) {
            state.nickname     = profile.nickname
            state.tone         = runCatching { Tone.valueOf(profile.tone) }.getOrDefault(Tone.FRIEND)
            state.deliveryDate = profile.deliveryDateIso
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
