# car-app (붕붕일기 차 앱)

기본 화면만 있는 뼈대예요. 차 데이터, 음성, LLM, 서버 연동은 아직 없고 화면에 보이는 값은 전부 `FakeData`의 예시 값이에요.

## 실행

1. Android Studio에서 **File > Open**으로 이 `car-app` 폴더를 열어요 (저장소 최상위 폴더 말고).
2. Gradle Sync가 끝나길 기다려요. SDK 34가 없으면 설치하라는 안내가 떠요.
3. 가로 화면 에뮬레이터(Automotive AVD 또는 태블릿)를 고르고 ▶ Run 'app'.

## 화면

| 화면 | 파일 |
| --- | --- |
| 첫 실행 세팅 8단계 | `ui/onboarding/OnboardingScreen.kt` |
| 메인 (상태바 + 탭 + 시뮬레이터) | `ui/main/MainScreen.kt` |
| 오늘 탭 (캐릭터, 오늘 기록, 메시지 카드, 경고 3칸) | `ui/today/TodayScreen.kt` |
| 일기장 탭 | `ui/diary/DiaryScreen.kt` |
| 차 건강 탭 | `ui/health/HealthScreen.kt` |
| 정비소 예약 | `ui/booking/BookingScreen.kt` |
| 설정 | `ui/settings/SettingsScreen.kt` |
| 시뮬레이터 (Debug 패널) | `ui/debug/DebugPanel.kt` |

상태는 `ui/AppState.kt`에 메모리로만 들고 있어서 앱을 다시 켜면 처음부터 시작해요.
