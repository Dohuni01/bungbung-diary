# Pleos Connect SDK 정리본 (AI 프롬프트용 컨텍스트)

> 출처: Pleos Playground 개발자 문서 (document.pleos.ai › 앱 개발·연동 가이드 › SDK 사용 가이드), 2026-11-02 기준 정리
> 용도: 차량용(Pleos Connect, Android Automotive OS 기반) 3rd party 앱을 만들 때 AI에게 "이 플랫폼에서 쓸 수 있는 기능"을 알려주는 컨텍스트

---

## 0. AI에게 주는 지시 (이 블록을 프롬프트 맨 앞에 두세요)

```
너는 Pleos Connect(현대차그룹 차량 인포테인먼트, Android Automotive OS 기반) 3rd party 앱 개발을 돕는 Android 개발자다.
아래 문서에 정리된 6개 SDK(Vehicle, NaviHelper, FusedLocation, SpeechToText, TextToSpeech, LLM)의 API만 사용해 기능을 설계/구현해라.
- 문서에 없는 API·필드·상수를 지어내지 마라. 확실하지 않으면 "API Reference 확인 필요"라고 표시해라.
- 언어는 Kotlin 우선(Java도 가능). 모든 SDK는 "객체 생성 → initialize() → 리스너 등록 → API 호출 → 리스너 해제 → release()" 생명주기를 따른다.
- 대부분의 결과는 비동기 콜백으로 온다(IPC). 반환값을 바로 쓰는 코드를 쓰지 마라.
- 사용하는 API에 맞는 pleos.car.permission.* 퍼미션을 AndroidManifest.xml에 선언하라.
- 개발/검증은 Pleos Connect Emulator(Android Studio AVD) + Virtual Console에서 한다.
```

---

## 1. 공통 사항

### 1-1. SDK 목록 한눈에 보기

| SDK | 하는 일 | 문서 기준 버전 | Gradle 의존성 | 진입점 클래스 |
|---|---|---|---|---|
| **Vehicle** | 차량 상태 조회/제어 (도어, 공조, 시트, 창문, 조명, 배터리, 연비, 경고등 등 19개 도메인) | 2.3.2 (Vehicle Service 2.3.21) | `ai.pleos.playground:Vehicle:2.3.2` | `ai.pleos.playground.vehicle.Vehicle` |
| **NaviHelper** | 내비 목적지·경유지·경로 제어, 주행/경로 정보 조회, 내비 화면 띄우기 | 3.0.5 | `ai.pleos.playground:NaviHelper:3.0.5` | `ai.pleos.playground.navi.helper.NaviHelper` |
| **FusedLocation** | 차량 위치(위경도·고도·방향) 조회/구독, 지오펜스(영역 진입/이탈) 알림 | 1.1.3 | `ai.pleos.playground:FusedLocation:1.1.3` | `ai.pleos.playground.location.fused.FusedLocation` |
| **SpeechToText** | 음성 → 텍스트 (차량 마이크 또는 앱 PCM 오디오) | 3.3.1 | `ai.pleos.playground:SpeechToText:3.3.1` | `ai.pleos.playground.stt.SpeechToText` |
| **TextToSpeech** | 텍스트 → 음성 합성 후 차량 스피커로 재생 | 3.3.1 | `ai.pleos.playground:TextToSpeech:3.3.1` | `ai.pleos.playground.tts.TextToSpeech` |
| **LLM** | 생성형 AI 텍스트 생성 / 멀티턴 채팅 (서버 또는 온디바이스) | (가이드 페이지 없음, API Reference만 존재) | 좌표 미기재 → 확인 필요 | `ai.pleos.playground.llm.LLM` |

> 참고: SpeechToText는 "음악 틀어줘" 같은 발화를 **텍스트로만** 바꿔준다. 음성 명령으로 앱 기능을 실행하려면 별도의 **Gleo Interface(앱 연동 가이드 – 스키마 전달/액션 콜백)** 를 사용해야 한다.

### 1-2. Maven 저장소 (모든 SDK 공통)

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://nexus-playground.pleos.ai/repository/maven-releases/")
    }
}
```
```kotlin
// app/build.gradle.kts
dependencies {
    implementation("ai.pleos.playground:Vehicle:2.3.2")
    implementation("ai.pleos.playground:NaviHelper:3.0.5")
    implementation("ai.pleos.playground:FusedLocation:1.1.3")
    implementation("ai.pleos.playground:SpeechToText:3.3.1")
    implementation("ai.pleos.playground:TextToSpeech:3.3.1")
}
```

### 1-3. 퍼미션 요약

| SDK | 퍼미션 (`<uses-permission android:name="..."/>`) | 대상 |
|---|---|---|
| FusedLocation | `pleos.car.permission.FUSED_LOCATION` (dangerous) | 위치 조회/구독/근접 알림 |
| NaviHelper | `pleos.car.permission.NAVI_ROUTE` | get*Info(), requestTrip() |
| | `pleos.car.permission.NAVI_ROUTE_SEARCH` | requestRoute, requestRouteWithWaypoints, cancelRoute, requestReRoute, add/removeWaypoint, changeRouteOption |
| | `pleos.car.permission.NAVI_CUSTOM_MAP` | requestNavigationScreen() |
| SpeechToText | `pleos.car.permission.STT_SERVICE` | 전체 |
| TextToSpeech | `pleos.car.permission.TTS_SERVICE` | 전체 |
| LLM | `pleos.car.permission.LLM_SERVICE` | 전체 |
| Vehicle | `pleos.car.permission.CAR_*` (조회) / `CONTROL_CAR_*` (제어) | 도메인별, 아래 3장 참고 |

### 1-4. 개발 환경
Pleos Connect Emulator 설치 → AVD 생성/실행 → Pleos Connect Virtual Console 설정(차량 상태 시뮬레이션) → 마이크 접근 설정 → Cold boot 설정.
서버 기반 STT/TTS/LLM은 Pleos Playground **My Project**에서 발급받은 **Client ID / Client Secret**으로 `registerApp()` 해야 한다.

---

## 2. 공통 생명주기 패턴

```kotlin
class MyApplication : Application() {
    lateinit var vehicle: Vehicle
    override fun onCreate() {
        super.onCreate()
        vehicle = Vehicle(this)
        vehicle.initialize()          // 서비스 연결
    }
}
// 화면/컴포넌트 종료 시: 리스너 unregister/remove → release()
// release() 후 같은 인스턴스 재사용 금지. 다시 쓰려면 새로 생성 + initialize()
// 비동기 콜백이 끝나기 전에 release() 호출 금지
```

---

## 3. Vehicle SDK — 차량 정보 조회 및 제어

### 3-1. 구조
- `Vehicle(context)` → `initialize()` → `vehicle.getDoor()`, `getHvac()`, `getSeat()` … 처럼 **getXxx()로 도메인 객체**를 얻는다(직접 생성 불가) → 사용 후 `release()`.
- 도메인 객체마다 4가지 API 타입이 있다.

| 타입 | 이름 패턴 | 결과 전달 |
|---|---|---|
| Capability | `checkXxxCapabilities(onComplete = { config, areaMap -> }, onFailed)` | `CapabilityConfig.accessType`(NONE/READ/WRITE/READ_WRITE) + `Map<Area, AreaBounds>`(minValue, maxValue) |
| Get | `getCurrentXxx(area?, onComplete = { value -> }, onFailed)` | onComplete(값) / onFailed(Exception) |
| Set | `setXxx(area?, value, onFailed)` | 실패 시 onFailed |
| Callback | `registerXxx(listener)` / `unregisterXxx(listener)` | `listener.onXxxUpdated(value)` / `onFailed(e)` |

```kotlin
val door = vehicle.getDoor()
// 1) 지원 여부/범위 확인
door.checkDoorPositionCapabilities(
    onComplete = { config, areaMap ->
        if (config.accessType == AccessType.READ || config.accessType == AccessType.READ_WRITE) {
            val bounds = areaMap[DoorArea(DoorValue.ROW_1_LEFT)] // bounds?.minValue / maxValue
        } else { /* UI 비활성 */ }
    },
    onFailed = { e -> if (e is VehicleException) Log.e("V", "${e.errorCode}") }
)
// 2) 조회
door.getCurrentDoorPosition(area = DoorArea(DoorValue.ROW_1_LEFT),
    onComplete = { pos: Int? -> }, onFailed = { e -> })
// 3) 제어
door.setDoorPosition(area = DoorArea(DoorValue.ROW_1_LEFT), value = 1, onFailed = { e -> })
// 4) 구독
val l = object : DoorPositionListener {
    override fun onPositionUpdated(pos: Int?) {}
    override fun onFailed(e: Exception) {}
}
door.registerDoorPosition(doorPositionListener = l)
door.unregisterDoorPosition(doorPositionListener = l)
```

### 3-2. 도메인별 사용 가능 기능 (API Reference 기준)

표기: **조회** = get/check/register, **제어** = set/move/select. 대부분의 항목은 `checkXxxCapabilities`, `getXxx`, `registerXxx/unregisterXxx` 세트를 가진다.

| 도메인 (getXxx) | 조회 가능 | 제어 가능 | 조회 퍼미션 |
|---|---|---|---|
| **Brake** | 브레이크 페달 상태 | – | CAR_DRIVE |
| **CarInfo** | VIN 정보, CRN 정보 (외부 크기·제조사·연식 등 기본 정보) | – | CAR_INFO |
| **Display** | 온도 표시 단위(TemperatureUnit), 속도 표시 단위 | – | CAR_INFO |
| **Door** | 도어 열림 위치, 도어 잠금 상태, 글로브박스 잠금 상태 | setDoorPosition, setDoorLock, setGloveBoxLock | CAR_DOORS |
| **DrivingMode** | 드라이브 모드(ECO/NORMAL/SPORT), 크루즈 컨트롤 상태, 크루즈 목표 속도 | – | CAR_DRIVE |
| **EvBattery** | 배터리 잔량(SoC), 충전 스위치 상태, 충전 전류 제한, 전비(순간/현재/누적/충전 이후) | setEvChargeSwitch, setEvChargePercentLimit, setEvChargeCurrentDrawLimit, setEvChargePortOpen | CAR_ENERGY |
| **Fuel** | 연료 잔량(%), 연비(순간/현재/누적/주유 이후) | – | CAR_ENERGY |
| **HVAC** | 공조 전원, AC, MAX AC, 오토, 듀얼, 내기순환/자동 내기순환, 디프로스터/MAX 디프로스트, 공기청정, 풍량, 풍향, 설정 온도, 현재 실내 온도 | 위 항목 전부 set 가능 (setHvacPower, setHvacAc, setHvacTemperature, setHvacFanSpeed, setHvacFanDirection, setHvacRecirculation, setHvacDefroster, setHvacAirCleaner …) | CAR_CLIMATE |
| **Light** | 상향등/하향등/안개등(전·후)/테일램프/실내등/독서등 상태·스위치, 앰비언트 라이트 상태·밝기·색상, 드라이브모드 연동 앰비언트 자동색 | 각 라이트 스위치 set, 앰비언트 라이트 스위치/밝기/색상/자동색 set | CAR_LIGHTS |
| **Odometer** | 총 주행거리, 트립 주행거리·시간(현재/누적/순간/주유 이후) | (setRangeRemainingDistance – deprecated) | CAR_DRIVE |
| **Safety** | 경고등(Telltales: ABS, 에어백, 엔진, TPMS, 안전벨트, 저연료 등 100여 종), 브레이크액/엔진오일/변속기오일 레벨, 워셔액 부족 경고, 파워다운 경고, 에어백 활성/전개 여부, 안전벨트 미착용 경고 | – | CAR_SAFETY |
| **Seat** | 시트 위치(전후·높이·틸트·깊이·등받이 각도·헤드레스트·요추·벨트 높이), 착좌 여부, 벨트 체결, 시트 열선 온도, 통풍 | moveSeatXxx(전후/높이/등받이/헤드레스트/요추 등, MoveControl FORWARD/BACKWARD/STOP), selectSeatMemory, setHvacSeatTemperature, setHvacSeatVentilation | CAR_SEATS |
| **SideMirror** | 사이드미러 열선 | setHvacSideMirrorHeat | CAR_MIRRORS |
| **SteeringWheel** | 조향각, 핸들 열선 | setHvacSteeringWheelHeat | – (API별 확인) |
| **Tire** | 타이어 저압 경고 (바퀴별) | – | CAR_SAFETY |
| **Toll** | 하이패스(ETCS) 음성 안내 음량 | setEtcsVoiceGuidanceVolume | CAR_TOLL |
| **TurnSignal** | 방향지시등 상태 (LEFT/RIGHT/OFF) | – | CAR_LIGHTS |
| **Window** | 창문 위치, 창문 잠금, 블라인드 위치, 스마트 글라스 상태/레벨/구역 | setWindowPosition, setWindowLock, moveWindowBlind, setSmartGlassState/Level/ZoneState | CAR_WINDOWS |
| **Wiper** | 와이퍼 상태, 와이퍼 스위치 | setWiperSwitch (OFF/AUTO/MIST/간헐 1~5/연속 1~5) | CAR_WINDOWS |

- 제어(set)에는 `CONTROL_CAR_*` 퍼미션이 필요하다(예: `pleos.car.permission.CONTROL_CAR_DOORS`, `CONTROL_CAR_CLIMATE`). 그 외 `PRIVATE_CAR_*`도 상수로 존재. 세부 API별 요구 퍼미션은 `@RequiresPermission` 확인.

### 3-3. Area(영역) 상수
- `DoorArea(DoorValue.ROW_1_LEFT | ROW_1_RIGHT | ROW_2_LEFT | ROW_2_RIGHT | ROW_3_LEFT | ROW_3_RIGHT | HOOD | REAR)`
- `SeatArea(SeatValue.ROW_1_LEFT … ROW_3_RIGHT, *_CENTER)`
- `WindowArea(WindowValue.ROW_1_LEFT … ROW_2_RIGHT, FRONT_WINDSHIELD, REAR_WINDSHIELD, ROOF_TOP_1, ROOF_TOP_2)`
- `MirrorArea(MirrorValue.DRIVER_LEFT | DRIVER_RIGHT | DRIVER_CENTER)`
- `WheelArea(WheelValue.LEFT_FRONT | RIGHT_FRONT | LEFT_REAR | RIGHT_REAR)`
- `TurnSignalArea(TurnSignalValue.LEFT | RIGHT | OFF)`

### 3-4. 주요 enum
DrivingModeType(ECO, NORMAL, SPORT) · CruiseControl(ENABLED, ACTIVATED, SUSPENDED, …) · EvChargeState(CHARGING, FULLY_CHARGED, NOT_CHARGING, ERROR) · FanDirectionType(FACE, FLOOR, DEFROST, 조합) · LightState(ON, OFF, DAYTIME_RUNNING) · LightSwitch(+AUTOMATIC) · OilLevelState(NORMAL, LOW, CRITICALLY_LOW, HIGH, ERROR) · TemperatureUnit(CELSIUS, FAHRENHEIT) · DistanceDisplayUnit(KILOMETER, MILE) · SpeedDisplayUnit · TelltaleType/Color/State · TripStatisticType(CURRENT, CUMULATIVE, INSTANTANEOUS, SINCE_REFILL)

### 3-5. 에러 처리
`onFailed(e: Exception)` → `e is VehicleException`이면 `e.errorCode`:

| ErrorCode | 의미 | 권장 처리 |
|---|---|---|
| ERROR_NOT_SUPPORTED_PROPERTY | 기능 미지원 | UI 비활성 |
| ERROR_NOT_SUPPORTED_AREA | 영역 미지원 | 영역 변경 |
| ERROR_NOT_SUPPORTED_VALUE_TYPE | 값 타입 미지원 | 파라미터 확인 |
| ERROR_SECURITY_PERMISSION | 퍼미션 부족 | 매니페스트 확인 |
| ERROR_SERVICE_CONNECTION_FAILURE | 서비스 연결 실패 | initialize()/에뮬레이터 확인 |
| ERROR_INTERNAL_SERVICE | 내부 오류 | 재시도 |
| ERROR_DRIVING_RESTRICTED | **주행 중 기능 제한** | 주행 제한 안내 |
| ERROR_EMPTY_RESULT | 결과 없음 | 빈 상태 UI |
| ERROR_UNDEFINED | 기타 | 로그·재시도 |

---

## 4. NaviHelper SDK — 내비게이션 제어 및 정보 조회

요청은 Pleos Connect 내비 앱으로 전달되고, **결과는 전부 `NaviHelperEventListener` 콜백**으로 온다(get*Info()도 값을 반환하지 않음).

### 4-1. 생명주기
```kotlin
val naviHelper = NaviHelper(applicationContext)
naviHelper.initialize(
    onConnected = { naviHelper.addListener(naviEventListener) /* 이후 API 호출 */ },
    onFailed = { throwable -> }
)
// 종료: naviHelper.removeListener(naviEventListener); naviHelper.release()
```

### 4-2. 사용 가능한 API

| 기능 | API | 결과 콜백 |
|---|---|---|
| 목적지 설정(재설정) | `requestRoute(RouteInfo)` | onRouteStarted, onRoutingCompleted, onDestinationChanged, onError |
| 경유지 포함 경로 요청 | `requestRouteWithWaypoints(RouteWithWaypointsRequest(destination, waypoints))` | 위와 동일 |
| 목적지 취소 | `cancelRoute()` | onRouteCancelled |
| 재탐색 | `requestReRoute()` (활성 경로 있을 때) | onReRouteEnded |
| 경로 옵션 변경 | `changeRouteOption(RouteOption)` | onRouteOptionChanged |
| 경유지 추가/삭제 | `addWaypoint(RequestWaypointInfo)` / `removeWaypoint(WaypointIndex.FIRST/SECOND/ALL)` (최대 2개) | onWaypointChanged |
| 경유지 조회 | `getWaypointInfo()` | onWayPointInfo(List<WaypointInfo>) |
| 목적지 조회 | `getDestinationInfo()` | onDestinationInfo(이름, 주소, 좌표, 남은 거리/시간, 통행료) |
| 즐겨찾기 조회 | `getBookmarkInfo()` | onBookmarkInfo (HOME/OFFICE/EV_CHARGING_STATION/DEFAULT) |
| 최근 목적지 | `getRecentDestinationInfo()` | onRecentDestinationInfo |
| 현재 위치 | `getCurrentLocationInfo()` | onCurrentLocationInfo(주소, 위경도, heading) |
| 경로 상태 | `getRouteStateInfo()` | onRouteStateInfo(isRouting, naviMode, routeOption, 교통상황, 도착 시 예상 배터리) |
| TBT(턴바이턴) | `getTBTInfo()` | onTBTInfo(회전 타입, 거리, 설명) |
| 충전 사업자 | `getChargerOperatorInfo()` | onChargerOperatorInfo |
| 경로 미리 계산(Trip) | `requestTrip(TripRequest)` | onTripInfo(거리, ETA, 에너지 소비, 도착 예상 SoC, 경로 요약/교통) |
| 내비 화면 띄우기 | `requestNavigationScreen(NavigationScreenRequest)` | – |

**그 외 이벤트(자동 수신)**: onDestinationArrived, onWayPointArrived, onNavigationStatus(INACTIVE/INITIALIZING/FREE_DRIVE/ROUTE_DRIVE), onDrivingInfo, onCurrentRoadSpeedLimit(현재 도로 제한속도), onCameraAlert(과속카메라: 종류·남은 거리·제한속도·과속 여부), onCautionPointInfo(어린이보호구역, 과속방지턱, 낙석, 안개 등 주의 구간), onBookMarkChanged

**NavigationScreenRequest 종류**: Foreground, FavoriteMain, AddHomeFavorite, AddWorkFavorite, NearbyChargingStations, Settings, MoveToCurrentLocationOnMap, OverviewRoute(info), SearchPlace(keyword), POIDetailPopup(poiId, poiProvider), RecentDestinations, RouteOptionList, EditRoute

**RouteOption**: RECOMMENDED, SHORT_DISTANCE, SHORT_TIME, FREE(무료도로), HIGHWAY, COMFORTABLE, SCHOOL_ZONE

```kotlin
naviHelper.requestRoute(RouteInfo(
    latitude = 37.48544722, longitude = 127.03636666,
    displayLatitude = null, displayLongitude = null,
    poiName = "42dot", poiId = "36khp37hh8gqcm5nqhe5", poiSubId = "0",
    address = "서울시 강남구 남부순환로 2621"   // routeOption 지정 가능
))
```

**NaviErrorCode**: CAUSE_LOW_BATTERY, CAUSE_SEARCH_ROUTE_OVER_DISTANCE, CAUSE_SEARCH_ROUTE_TOO_SHORT_DISTANCE, CAUSE_SEARCH_ROUTE, CAUSE_SEARCH_TRIP

---

## 5. FusedLocation SDK — 차량 위치

### 5-1. 기능
| 기능 | API |
|---|---|
| 지원 환경 확인 | `isSupported()` (반드시 먼저) |
| 초기화/해제 | `initialize()` / `release()` |
| 마지막 위치 1회 조회 | `getLastKnownLocation(...)` – 새로 계산하지 않음, 없으면 null |
| 위치 업데이트 구독/중지 | `registerFusedLocationCallback(FusedLocationListener)` / `unregisterFusedLocationCallback(listener)` |
| 지오펜스(영역 진입/이탈 알림) | `addProximityAlert(ProximityAlertRequest, ProximityAlertListener)` / `removeProximityAlert(...)` |
| 위치 접근 차단 안내 팝업 | `showLocationAccessBlockedPopup()` (권한을 허용해주진 않음) |

- `LocationData`: latitude, longitude, altitude, heading, confidence, timestampMillis
- `ProximityAlertRequest(latitude, longitude, radiusMeters: Float(>0), transitionTypes: Set<ProximityTransition>)` – ProximityTransition: ENTER, EXIT
- `ProximityAlertListener`: onRegistered(alertId), onTransition(ProximityAlertEvent(alertId, transition)), onError
- `FusedLocationListener`: onChanged(LocationData?) – **null 체크 필수**, onError(Exception)

```kotlin
val fused = FusedLocation(this)
if (fused.isSupported()) fused.initialize()
val listener = object : FusedLocationListener {
    override fun onChanged(location: LocationData?) { location ?: return }
    override fun onError(exception: Exception) {}
}
fused.registerFusedLocationCallback(listener)
// 종료: fused.unregisterFusedLocationCallback(listener); fused.release()
```

---

## 6. SpeechToText SDK — 음성 인식

### 6-1. 모드 (`SpeechToText(context, Mode.HYBRID)`, 기본 HYBRID)
- **HYBRID**: 서버 우선, 네트워크 실패 시 온디바이스 자동 전환 (서버 사용 시 registerApp 필요)
- **SERVER**: 서버만 (네트워크 + registerApp 필요)
- **ON_DEVICE**: 차량 온디바이스 엔진, 네트워크/등록 불필요

### 6-2. 흐름 & API
`initialize(OnInitializeListener)` → `addListener(ResultListener)` → `setLocale(Locale)` → (서버 모드) `registerApp(clientID, clientSecret, OnServerConnectionListener)` → onConnected 이후 `request()` (차량 마이크로 인식 시작) → `stop()` → `removeListener()` → `release()`

- **앱 보유 PCM 오디오 인식**: `sendAudio(ByteArray)` 반복 → `completeAudioSend()` (ON_DEVICE 모드 전용, 예: 16kHz/16bit/mono, 100ms=3,200byte 청크)
- **지원 Locale**: ko-KR, en-US, fr-CA, es-US, es-MX (미설정 시 이전 설정값 → 시스템 locale)
- **ResultListener**: onReady, onStartedRecognition, `onUpdated(stt: String, completed: Boolean)` (중간/최종 결과), onUpdatedEpdData(on, off), onUpdatedRms(rms), onEndedRecognition, onError(ErrorType)

```kotlin
val stt = SpeechToText(applicationContext, Mode.HYBRID)
stt.initialize(object : OnInitializeListener {
    override fun onSuccess() { stt.addListener(resultListener); stt.setLocale(Locale.KOREA); stt.request() }
    override fun onFailure(errorType: ErrorType) {}
})
```

---

## 7. TextToSpeech SDK — 음성 합성/재생

### 7-1. 모드: HYBRID(기본) / SERVER / ON_DEVICE (STT와 동일 개념, streaming TTS)

### 7-2. API
| 기능 | API |
|---|---|
| 초기화/해제 | `initialize(OnInitializeListener)` / `release()` |
| 이벤트 리스너 | `addEventListener(EventListener)` / `removeEventListener()` |
| 서버 등록 | `registerApp(clientID, clientSecret, OnServerConnectionListener)` |
| 말하기 | `speak(text, isFinal = true)` – 여러 조각은 마지막만 isFinal=true (LLM 스트리밍 응답을 바로 읽어줄 때 유용) |
| 중지 | `stop()` → onStop |
| 언어 | `setLocale(Locale)` (ko-KR, en-US, fr-CA, es-US, es-MX) |
| 목소리 | `setSpeakerType(String)` – 한국어: seoyoon, eunwoo, haeun(기본), sanghyeok / 영어: lyra, finley / 스페인어: iris, arlo / 프랑스어: sabine, noah |
| 출력 구역 | `setAudioZoneId(zoneId: Int)` |
| 오디오 속성 | `setAudioAttributes(AudioAttributes)` |

- **EventListener**: onReady, onStart, onUpdatedRms(rms), onDone, onStop, onError(ErrorType)

```kotlin
val tts = TextToSpeech(applicationContext, Mode.HYBRID)
tts.initialize(object : OnInitializeListener {
    override fun onSuccess() { tts.addEventListener(eventListener); tts.setSpeakerType("seoyoon"); tts.speak("안녕하세요. 무엇을 도와드릴까요?") }
    override fun onFailure(errorType: ErrorType) {}
})
```

### 7-3. STT/TTS 공통 ErrorType
AUDIO_ERROR, INVALID_REQUEST_ERROR, MIC_ERROR, NETWORK_ERROR, NOT_SUPPORTED_API, ON_DEVICE_ENGINE_ERROR, PERMISSION_DENIED, SERVER_INTERNAL_ERROR, TIMEOUT, UNAUTHENTICATED(자격 증명 확인), UNAVAILABLE_LANGUAGE, UNKNOWN → `when`에 else 분기 필수

---

## 8. LLM SDK — 생성형 AI (API Reference 기반)

- 생성자: `LLM(context, mode: Mode = Mode.SERVER)` — Mode: **SERVER**(네트워크 실패 시 에러) / **ON_DEVICE**
- 퍼미션: `pleos.car.permission.LLM_SERVICE`

| 기능 | API |
|---|---|
| 초기화/해제 | `initialize()` / `release()` |
| 서버 등록 (SERVER 모드) | `registerApp(clientID, clientSecret, OnServerConnectionListener)` → onConnected / onFailed |
| 단발성 텍스트 생성 | `generateContent(prompt: String, listener: ResultListener)` |
| 멀티턴 채팅 시작 | `startChat(history: List<LLMContent> = listOf(), onSuccess: () -> Unit, onFailed: (reason: String) -> Unit)` |
| 채팅 메시지 전송 | `sendMessage(inputContent: LLMContent, listener: ResultListener)` |
| 모델 파라미터 (SERVER 전용) | `setModelParameter(ModelParameter.TEMPERATURE / MAX_TOKENS / MODEL_NAME, value: String, listener)` |
| 온디바이스 프롬프트 조회 (ON_DEVICE 전용) | `getOnDeviceModelPromptsContents(): String` |
| 서버 타입 | `setServerType(type: Int)` (설명 없음) |

- `ResultListener`: `onResponse(response: String, completed: Boolean)` (스트리밍, 완료 시 completed=true), `onError(reason: String)`
- `LLMContent(role: String = "user", parts: MutableList<Part>)`, `TextPart(text: String) : Part`

```kotlin
val llm = LLM(applicationContext, Mode.SERVER)
llm.initialize()
llm.registerApp(clientId, clientSecret, object : OnServerConnectionListener {
    override fun onConnected() {
        llm.startChat(onSuccess = {
            llm.sendMessage(LLMContent(parts = mutableListOf(TextPart("근처 충전소 추천해줘"))), object : ResultListener {
                override fun onResponse(response: String, completed: Boolean) { /* TTS.speak(response, isFinal = completed) 연계 가능 */ }
                override fun onError(reason: String) {}
            })
        }, onFailed = { reason -> })
    }
    override fun onFailed(errorType: ErrorType) {}   // 타입은 Reference 확인 필요
})
```

---

## 9. 조합 아이디어 (기능 설계 시 참고)

- **음성 대화형 앱**: STT(request → onUpdated 최종) → LLM(sendMessage) → TTS(speak 스트리밍)
- **차량 상태 기반 앱**: Vehicle 구독(EvBattery SoC, Fuel, Odometer, Safety Telltales, DrivingMode) → 상황에 맞는 안내/기록
- **위치·경로 기반 앱**: FusedLocation(위치/지오펜스) + NaviHelper(목적지 설정, ETA·남은 거리, 도착 이벤트, 충전소 화면 열기)
- **편의 제어 앱**: Vehicle HVAC/Seat/Light(앰비언트)/Window 제어 — 반드시 capability 확인 후 UI 노출, 주행 중 제한(ERROR_DRIVING_RESTRICTED) 처리

## 10. 주의사항 체크리스트
1. 사용 전 지원 여부 확인 (FusedLocation `isSupported()`, Vehicle `checkXxxCapabilities`)
2. 리스너는 요청 **전에** 등록, 해제 시 **같은 인스턴스**로 제거
3. 결과는 비동기 콜백 — 반환값 기대 금지 (NaviHelper get*Info 포함)
4. nullable 값(위치, Vehicle get 결과) null 처리
5. 서버 모드 STT/TTS/LLM은 registerApp → onConnected 이후 요청
6. release() 후 재사용 금지, 비동기 콜백 끝나기 전 release() 금지
7. 실제 전달되는 내비 이벤트는 내비 앱 상태/구현에 따라 다를 수 있음
