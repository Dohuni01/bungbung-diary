package com.bungbung.diary.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bungbung.diary.ui.booking.BookingScreen
import com.bungbung.diary.ui.main.MainScreen
import com.bungbung.diary.ui.onboarding.OnboardingScreen
import com.bungbung.diary.ui.settings.SettingsScreen
import com.bungbung.diary.ui.station.GasStationScreen

object Routes {
    const val ONBOARDING = "onboarding"
    const val MAIN = "main"
    const val SETTINGS = "settings"
    const val BOOKING = "booking"
    const val STATIONS = "stations"
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
    val state = remember {
        AppState().apply {
            if (!initialNickname.isNullOrBlank()) nickname = initialNickname.trim().take(8)
        }
    }
    val nav = rememberNavController()

    NavHost(navController = nav, startDestination = Routes.ONBOARDING) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                state = state,
                onFinish = {
                    nav.navigate(Routes.MAIN) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.MAIN) {
            MainScreen(
                state = state,
                onOpenSettings = { nav.navigate(Routes.SETTINGS) },
                onOpenBooking = { nav.navigate(Routes.BOOKING) },
                onOpenStations = { nav.navigate(Routes.STATIONS) },
            )
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
