# 붕붕일기 개발 프롬프트

> 이 파일을 저장소 루트에 `PROMPT.md`로 넣고, AI 코딩 도구(Claude Code 등)에게 단계별로 시키세요.
> 맨 아래 "단계별 시작 문장"을 한 번에 하나씩 붙여넣으면 돼요.

---

## 0. 사람이 먼저 할 일 (AI가 대신 못 하는 것)

- [ ] Pleos Playground 가입 → **My Project 만들기 + SDK 사용 신청** ([Create a Project and Request SDK Access](https://document.pleos.ai/en/docs/pleos-only/create-my-project))
- [ ] 개발자에게 주는 **에뮬레이터 시스템 이미지 URL**을 받아 Android Studio SDK Update Sites에 등록 ([환경 설정 가이드](https://document.pleos.ai/en/docs/connect/guide/getting-started/application-development/setup-connect-sdk-emulator))
- [ ] Pleos Maven 저장소 주소 확인 (SDK 신청 후 문서에 안내됨)
- [ ] 팀 역할 정하기: 차 앱(Kotlin) / 서버(Spring Boot) / 기획서·PT
- [ ] 일정: 기획안 제출 11/15 → 서류 결과 11/23 → 최종 PT 12/3~4

권장 일정: Phase 0~1은 11/9까지, Phase 2~5는 11/29까지, Phase 6(시연 준비)은 12/2까지.

---

## 1. 너의 역할과 목표

너는 현대자동차그룹 차량용 앱 공모전에 나가는 팀의 개발자야. **붕붕일기**라는 Pleos Connect 앱(Android Automotive OS 기반)과 이를 받쳐주는 Spring Boot 서버를 만든다.

최종 목표는 **12월 3일 PT에서 Pleos Connect 에뮬레이터로 아래 시연을 끝까지 문제없이 보여주는 것**이다.

1. 주행을 마치고 P단에 놓으면, 차가 1인칭으로 오늘 일기를 화면과 음성으로 들려준다.
2. 말투(친구/집사/무뚝뚝)를 바꾸면 같은 데이터가 다른 말투로 나온다.
3. 타이어 공기압 경고가 뜨면 차가 사람 말로 설명하고, 정비소 예약 요청까지 이어진다.
4. D단 주행 중에는 화면에 글을 띄우지 않고, 경고만 음성으로 알려준다.
5. 1만 km 돌파, 출고 1주년을 축하한다.
6. 일요일 밤 주간 일기가 가족에게 공유된다.

## 2. 제품 기능 명세

| 기능 | 동작 | 트리거 | 차 데이터 |
| --- | --- | --- | --- |
| 오늘의 일기 | 오늘 주행을 1인칭 문장으로 화면+음성 출력 | 주행 후 기어가 P가 될 때 | 주행거리, 와이퍼 작동 시간, 급제동, 외기온도, 히터 |
| 말투와 차 이름 | 친구/집사/무뚝뚝 3종, 차 이름 설정(기본 "붕붕이") | 설정 화면 | 없음 |
| 경고 통역 + 정비 예약 | 경고를 말로 설명 + "급한 정도 / 무슨 뜻 / 할 일" 3칸 고정 표시, 정비소 선택 → 예약 요청 | 경고 발생 즉시 | 타이어 공기압, 차량 경고(Safety) |
| 주행 중 음성 모드 | 글 숨김, 경고만 즉시 음성, 나머지는 P단까지 보류 | 기어 D 또는 CarUxRestrictions 활성 | 기어, UX 제한 상태 |
| 기념일 | 축하 메시지 + 캐릭터 축하 표정, 일기장에 기록 | 누적 1만 km 단위 돌파, 출고일 기념 | 누적 주행거리, 출고일(사용자 입력) |
| 가족 주간 일기 | 한 주 요약을 가족 공유 페이지에 게시 | 일요일 21:00 | 주간 집계, 미해결 점검 |
| 일기장 / 차 건강 탭 | 지난 일기 목록, 바퀴별 공기압·경고등·예약 상태 | 항상 | 위와 동일 |

화면 구성: 상단 상태바(시각, 외기온도, 기어 P/R/N/D, 누적 km) / 왼쪽 차 캐릭터 + 오늘 기록 / 오른쪽 메시지 카드 / 하단 탭(오늘, 일기장, 차 건강).
캐릭터 표정 4종: 평소(웃음), 경고(걱정), 기념일(축하), 주행 중(집중).

## 3. 절대 규칙

1. **Pleos API 이름과 시그니처를 지어내지 마.** 쓰기 전에 공식 문서나 SDK 라이브러리(Gradle 캐시의 클래스)에서 확인하고, 코드 주석에 문서 URL을 남겨. 확인이 안 되면 멈추고 나에게 물어봐.
2. **모든 외부 의존성은 우리 인터페이스 뒤에 숨겨.** `VehicleDataSource`, `Speaker`, `TextGenerator` 각각 Fake 구현을 항상 유지해서 차 없이도 앱이 돈다.
3. **숫자는 데이터에서만.** 문장에 나오는 숫자는 요약 JSON에 있는 숫자여야 한다. LLM 출력에 JSON에 없는 숫자가 있으면 버리고 템플릿 문장을 쓴다.
4. **경고 문장은 LLM을 쓰지 않는다.** 검증된 템플릿에서 말투만 고른다. "급한 정도 / 무슨 뜻 / 할 일"은 말투와 무관하게 고정.
5. **운전 중 화면 제한을 지켜.** `CarUxRestrictions`가 distraction optimization을 요구하면 글을 숨기고 음성만 쓴다.
6. **개인정보 최소화.** 위치와 경로는 서버와 가족 공유로 보내지 않는다. 첫 실행 때 동의 화면을 보여준다. (Pleos 심사 기준: 개인정보·위치를 쓰는 앱은 이용약관과 데이터 처리 방침을 제출해야 하고, 사용 전 동의가 필요하다.)
7. **단계마다 빌드와 테스트를 돌리고**, 끝나면 "확인한 것 / 확인 못 한 것"을 나눠서 보고해.

## 4. 확인된 Pleos 사실 (2026-10-31 문서 기준)

출처를 밝힌 것만 적었다. 버전 번호는 바뀔 수 있으니 최신 문서로 다시 확인해.

**프로젝트 설정** ([시작 가이드](https://document.pleos.ai/en/docs/pleos-only/getting-started))
- Android SDK API 34, `minSdk` 28 이상, Car/Automotive 템플릿, Kotlin
- 에뮬레이터: Pleos Connect 시스템 이미지(API 34)로 Automotive AVD 생성, Cold boot, RAM 4GB
- `settings.gradle.kts`에 Pleos Maven 저장소 추가, 의존성 예: `ai.pleos.playground:Vehicle:2.3.1`

**Vehicle SDK** ([소개](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/Vehicle/intro), [도메인 객체](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/Vehicle/obtain-domain-object))
```kotlin
val vehicle = Vehicle(context)
vehicle.initialize()
val door = vehicle.getDoor()   // 도메인 객체는 getXxx()로만 얻는다
// ... Capability / Get / Set / Callback API 호출
vehicle.release()              // 비동기 콜백이 끝나기 전에 release() 금지
```
- 도메인: Brake, CarInfo, Display, Door, DrivingMode, EvBattery, Fuel, HVAC, Light, Odometer, Safety, Seat, SideMirror, Steeringwheel, Tire, Toll, TurnSignal, Window, Wiper
- API 4종: Capability(`checkXXXCapabilities`, 지원 여부와 값 범위), Get, Set, Callback(리스너 등록). 결과는 `onComplete` / `onFailed`로 비동기 도착
- Get 결과가 `null`이나 unavailable이면 "값 없음"이다. 권한 오류와 구분해서 처리
- 우리가 쓸 권한 후보 ([샘플 앱](https://document.pleos.ai/en/docs/pleos-only/tutorial/vehicle-sdk-sample-app), [권한 문서](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle-permission)): `pleos.car.permission.CAR_MILEAGE`, `CAR_TIRES`, `READ_WINDSHIELD_WIPERS`, `CAR_SAFETY`, `CAR_DRIVE`, `CAR_SPEED`, `CAR_CLIMATE`, `CAR_INFO`. 승인 안 된 권한을 쓰면 심사에서 떨어질 수 있으니 **필요한 것만** 선언
- 속도, 기어, 외기온도 같은 표준 데이터는 AAOS Car API(`CarPropertyManager`)를 우선 쓰는 게 Pleos 정책 ([API 정책](https://document.pleos.ai/en/docs/pleos-only/vehicle-app-planning-guide/pleos-connect-sdk-api))

**TextToSpeech SDK** ([소개](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/TextToSpeech/intro))
```kotlin
val textToSpeech = TextToSpeech(applicationContext, Mode.HYBRID) // ai.pleos.playground.tts
// initialize() → addEventListener() → setLocale() → speak()
// 서버 모드에선 OnServerConnectionListener.onConnected() 이후에 speak()
// 끝나면 stop(), removeEventListener(), release()
```
- `HYBRID`(기본): 서버 우선, 네트워크 실패 시 온디바이스. My Project 등록 필요
- `ON_DEVICE`: 네트워크 없이 동작, 등록 불필요 → **경고 음성은 ON_DEVICE로**
- 음성 안내 앞 무음 처리 가이드: [TTS Pre-roll Silence](https://document.pleos.ai/en/docs/pleos-only/vehicle-app-developer-guide/voice-guidance-preroll-silence-guide), 오디오 포커스: [Audio Focus 가이드](https://document.pleos.ai/en/docs/pleos-only/vehicle-app-developer-guide/audio-focus-development-guide/audio-focus-development)

**운전 중 제한** ([Driving Restriction 가이드](https://document.pleos.ai/en/docs/pleos-only/vehicle-app-developer-guide/driving-restriction-development-guide))
- AAOS `CarUxRestrictionsManager.registerListener()` → `onUxRestrictionsChanged()`로 상태 수신
- `isRequiresDistractionOptimization()`과 `getActiveRestrictions()`를 보고 기능별로 제한 적용

**LLM (불확실)**
- 예전 문서엔 Gleo AI SDK에 LLM이 있었지만, 지금 LLM 문서 주소는 서비스 상태 페이지로 넘어가고 "Using SDKs" 목록에도 없다. **우리 프로젝트에서 쓸 수 있는지 Phase 0에서 반드시 확인.** 못 쓰면 우리 서버가 외부 LLM API를 대신 부른다(Phase 4).

## 5. 기술 스택과 저장소 구조

```
bungbung/
├─ car-app/        Android Automotive 앱 (Kotlin, Jetpack Compose, Coroutines/Flow, Room)
├─ server/         Spring Boot 3 (Java 21), PostgreSQL, Flyway, Spring Data JPA, springdoc-openapi
├─ docs/           api-findings.md, decisions.md, demo-script.md
└─ docker-compose.yml   로컬 PostgreSQL
```

차 앱 계층:
```
ui/            Compose 화면 (Today, Diary, Health, Settings, Booking, DebugPanel)
domain/        DailySummary, CarEvent, Tone, CarMessage, 규칙(TriggerEngine)
data/vehicle/  VehicleDataSource (Pleos 구현 + Fake 구현)
data/voice/    Speaker (Pleos TTS 구현 + Fake 구현)
data/text/     TextGenerator (Template 구현 + Llm 구현) + NumberGuard
data/local/    Room (일별 요약, 메시지, 설정)
data/remote/   서버 API 클라이언트
```

## 6. 공통 데이터 계약

```json
// DailySummary: 차 안에서 만들고 문장 생성과 서버 저장에 쓰는 유일한 입력
{
  "date": "2026-10-31",
  "distanceKm": 37.2,
  "driveMin": 74,
  "trips": 2,
  "wiperMin": 41,
  "hardBrakes": 1,
  "outsideTempC": 11,
  "heaterOn": true,
  "odometerKm": 9982
}
```

- `CarEvent`: `DIARY`, `TIRE_LOW`, `SAFETY_WARNING`, `MILESTONE_ODO`, `BIRTHDAY`, `WEEKLY`, `BOOKED`
- `Tone`: `FRIEND`, `BUTLER`, `BLUNT`
- `CarMessage`: `id, event, tone, text, textSource(TEMPLATE|LLM), data(JSON), createdAt, delivery(SCREEN_AND_VOICE|VOICE_ONLY|DEFERRED)`

집계 규칙 (임계값은 전부 상수로 빼고 `docs/decisions.md`에 기록):
- 주행거리 = 오늘 첫 시동 시 누적거리와 현재 누적거리의 차
- 와이퍼 시간 = 와이퍼 상태가 OFF가 아닌 시간의 합
- 급제동 = 초당 감속이 임계값을 넘은 횟수 (초기값 12 km/h/s, 팀이 조정)
- 타이어 경고 = Tire API가 주는 경고 상태 우선, 없으면 권장값 대비 비율 임계값
- 기념일 = 누적거리가 10,000 km 배수를 넘는 순간 / 출고일(설정에서 입력)과 같은 월·일

## 7. 트리거와 출력 규칙

| 상황 | 출력 |
| --- | --- |
| 오늘 1km 이상 주행 후 기어 P | DIARY: 화면 + 음성 |
| 경고 발생, 주차 중 | 경고 카드 + 음성, "가까운 정비소 보기" 버튼 |
| 경고 발생, 주행 중 | 글 없이 ON_DEVICE 음성 즉시, 카드는 P단에서 표시 |
| 기념일·일기, 주행 중 | DEFERRED, P단이 되면 표시 |
| 일요일 21:00 | WEEKLY 생성 → 서버에 게시 |

## 8. 단계별 작업

각 단계 끝에 빌드·테스트 결과와 "확인한 것 / 못 한 것"을 보고하고 멈춰.

### Phase 0. 환경과 API 조사 (기능 코드 금지)
1. `car-app` 빈 프로젝트를 문서 설정대로 만들고 에뮬레이터에서 실행.
2. **Probe 화면**: 각 도메인(Odometer, Tire, Wiper, Safety, Brake)과 AAOS 표준(속도, 기어, 외기온도)에 대해 Capability 확인 → Get 1회 → Callback 등록 결과를 화면과 로그에 출력.
3. TTS `ON_DEVICE`로 한국어 문장 하나 읽기.
4. LLM SDK가 우리 프로젝트에서 쓸 수 있는지 확인.
5. `docs/api-findings.md`에 표로 정리: 우리가 필요한 데이터 / 쓰는 API·프로퍼티 / 권한 / 에뮬레이터에서 동작 여부 / 실제 값 예시 / 출처 URL.

완료 기준: 표의 모든 행이 "됨 / 안 됨 / 확인 불가" 중 하나로 채워짐. 안 되는 항목엔 대안 제안.

### Phase 1. 차 앱 뼈대 + Fake 데이터 + 화면
1. 2절 화면 구성대로 Today / Diary / Health / Settings 화면을 Compose로 만든다. 17인치 가로 화면 기준.
2. `FakeVehicleDataSource`: 부록 B의 데모 값을 내보내고, **Debug 패널**(릴리스 빌드에선 숨김)에서 이벤트를 발생시킨다. 이 패널이 PT 시연 도구다.
3. `TemplateTextGenerator`: 부록 A 문장을 이벤트 × 말투 전부 구현.
4. Room에 일별 요약과 메시지 저장, 일기장 탭에 표시.
5. 단위 테스트: 템플릿 21종 출력, 숫자가 데이터와 일치하는지.

완료 기준: 에뮬레이터에서 Debug 패널만으로 1장의 시연 6단계가 전부 재현됨(음성 제외).

### Phase 2. 실제 차 신호 연결
1. `PleosVehicleDataSource` 구현 (Phase 0 표에서 "됨"인 것만). 앱 생명주기에 맞춰 `initialize()` / `release()`.
2. `DailyAggregator`: 6절 규칙으로 신호를 DailySummary로 집계, Room에 저장.
3. `TriggerEngine`: 7절 규칙. 같은 경고는 해제 전까지 한 번만 알림.
4. `CarUxRestrictions` 연결: 제한 중엔 글 숨김, DEFERRED 처리.
5. 에뮬레이터의 Virtual Console로 기어·타이어 값을 바꿔가며 테스트.

완료 기준: Fake 없이 에뮬레이터 신호만으로 DIARY와 TIRE_LOW가 뜬다.

### Phase 3. 음성
1. `PleosSpeaker`: 경고는 `ON_DEVICE`, 일기는 `HYBRID`(등록 안 됐으면 ON_DEVICE).
2. 말투별 속도·높낮이 설정이 SDK에 있으면 적용, 없으면 생략하고 기록.
3. 오디오 포커스 가이드대로 다른 소리와 겹치지 않게 처리.

완료 기준: 주행 중 경고가 글 없이 음성으로만 나온다.

### Phase 4. 문장 생성 (LLM)
1. `TextGenerator` 인터페이스 뒤에 `LlmTextGenerator` 추가. 쓸 수 있는 LLM 경로는 Phase 0 결과로 결정:
   - Pleos LLM SDK를 쓸 수 있으면 그것을 쓴다.
   - 못 쓰면 서버 `POST /api/narrate`를 부른다(키는 서버에만).
2. 프롬프트는 부록 C를 그대로 쓴다. 입력은 DailySummary JSON과 말투, 출력은 문장만.
3. `NumberGuard`: 출력의 모든 숫자가 입력 JSON 숫자 집합에 있는지 검사. 실패, 3초 초과, 오류면 템플릿으로 대체.
4. 일기는 주행 중에 미리 생성해 두고 P단에서 바로 보여준다.
5. 경고와 예약 문장은 LLM 경로를 타지 않는지 테스트로 보장.

완료 기준: LLM을 끊어도 모든 기능이 템플릿으로 정상 동작.

### Phase 5. 서버 (Spring Boot)
테이블 (Flyway 마이그레이션):
- `car(id, name, tone, delivery_date, device_token_hash, created_at)`
- `daily_summary(id, car_id, date, summary jsonb)` — `(car_id, date)` 유니크
- `diary_entry(id, car_id, event, tone, text, text_source, created_at)`
- `family_share(id, car_id, share_code, created_at)`
- `shop(id, name, description, slots jsonb)` — 시연용 시드 데이터
- `booking(id, car_id, shop_id, slot, reason, status, created_at)`

엔드포인트:
| 메서드 | 경로 | 하는 일 |
| --- | --- | --- |
| POST | `/api/cars` | 차 등록, 기기 토큰 발급 |
| PUT | `/api/cars/{carId}/settings` | 이름, 말투, 출고일 |
| PUT | `/api/cars/{carId}/summaries/{date}` | 일별 요약 저장(멱등) |
| POST | `/api/cars/{carId}/diaries` | 일기·기념일 문장 저장 |
| GET | `/api/cars/{carId}/diaries` | 기간별 조회 |
| POST | `/api/cars/{carId}/weekly` | 주간 요약 생성 후 가족 공유에 게시 |
| GET | `/family/{shareCode}` | 가족용 주간 일기 웹 페이지 (로그인 없음, 위치 정보 없음) |
| GET | `/api/shops` | 시연용 정비소 목록 |
| POST | `/api/bookings` | 예약 요청 (상태 REQUESTED) |
| POST | `/api/narrate` | LLM 프록시 (Phase 4에서 필요할 때만) |

요구사항:
- 인증은 차 등록 때 발급한 기기 토큰(Bearer) 하나로 단순하게.
- Bean Validation, 전역 예외 처리, springdoc으로 API 문서 자동 생성.
- 주간 집계는 서버에서 `daily_summary`로 계산(지난주 대비 급제동 비교 포함).
- 테스트: 서비스 단위 테스트 + Testcontainers PostgreSQL 통합 테스트.
- `/api/narrate`는 요청 본문에 위치·경로가 들어오면 400으로 거절, 차별 분당 호출 제한.

완료 기준: `docker compose up` 후 차 앱이 일기를 저장하고, `/family/{shareCode}`에서 주간 일기가 보인다.

### Phase 6. 시연 준비
1. `docs/demo-script.md`: 1장의 6단계를 누를 순서와 할 말로 정리.
2. Debug 패널에 "처음 상태로" 버튼.
3. 네트워크가 끊겨도 시연이 끝까지 가는지 확인(템플릿 + ON_DEVICE).
4. README: 설치, 실행, 구조도, 확인된 제약.

---

## 부록 A. 템플릿 문장 (이벤트 × 말투)

`{}`는 DailySummary 또는 이벤트 데이터의 값.

**DIARY**
- FRIEND: 오늘은 너랑 {distanceKm}km 달렸어. 아침부터 비가 와서 와이퍼를 {wiperMin}분이나 흔들었더니 팔이 뻐근해. 출근길에 한 번 급하게 멈췄던 거 기억나? 나도 깜짝 놀랐는데 둘 다 괜찮아서 다행이야. 밖이 {outsideTempC}도라 히터 켜줬는데 따뜻했지? 오늘도 수고했어.
- BUTLER: 오늘은 {distanceKm}km를 함께 달렸습니다. 아침 내내 비가 내려 와이퍼가 {wiperMin}분 동안 일했습니다. 출근길에 급제동이 한 번 있었으나 차량에는 이상이 없습니다. 바깥 기온이 {outsideTempC}도여서 히터를 켜 두었습니다. 오늘도 수고 많으셨습니다.
- BLUNT: {distanceKm}km. 비 와서 와이퍼 {wiperMin}분. 급제동 1번, 이상 없음. 밖 {outsideTempC}도, 히터 켰음. 수고.

> 위 DIARY 템플릿은 데모 데이터에 맞춘 문장이다. 실제 구현에서는 문장을 조각으로 나눠 조건부로 조합해: 와이퍼 0분이면 비 문장 생략, 급제동 0회면 "부드럽게 달렸어", 히터 off면 온도 문장만.

**TIRE_LOW** (데이터: position, psi, recommendedPsi)
- FRIEND: 있잖아, {position}발에 바람이 좀 빠졌어. 지금 {psi}psi인데 원래는 {recommendedPsi}psi거든. 당장 위험한 건 아니지만 이번 주 안에 바람 좀 넣어줄래? 이대로 두면 연비도 떨어지고 타이어도 빨리 닳아.
- BUTLER: {position} 바퀴 공기압이 {psi}psi로, 권장치 {recommendedPsi}psi보다 낮습니다. 즉시 위험한 수준은 아니지만 이번 주 안에 점검을 권해 드립니다. 공기압이 낮으면 연비와 타이어 수명이 나빠집니다.
- BLUNT: {position} 타이어 {psi}psi. 권장 {recommendedPsi}. 이번 주 안에 넣어.
- 고정 3칸: 급한 정도 "이번 주 안에" / 무슨 뜻 "타이어 공기압 경고 (TPMS)" / 할 일 "공기 보충, 계속 빠지면 펑크 점검"

**SAFETY_WARNING: 엔진 경고등**
- FRIEND: 나 속이 좀 이상해. 엔진 경고등이 켜졌어. 지금 바로 멈춰야 할 정도는 아니지만, 며칠 안에 정비소에서 봐줬으면 좋겠어. 혹시 불이 깜빡이기 시작하면 그땐 바로 안전한 곳에 세워줘.
- BUTLER: 엔진 경고등이 켜졌습니다. 즉시 정차할 상황은 아니지만, 가급적 빠른 시일 안에 정비소 점검을 권해 드립니다. 경고등이 깜빡이면 안전한 곳에 바로 정차해 주십시오.
- BLUNT: 엔진 경고등 켜짐. 며칠 안에 정비소. 깜빡이면 바로 세워.
- 고정 3칸: "빠른 시일 안에" / "엔진·배기 계통 이상 감지" / "정비소 점검, 깜빡이면 즉시 정차"
- Safety API의 경고 코드 → (템플릿, 3칸) 매핑 표를 `docs/decisions.md`에 만들고, 모르는 코드는 "경고등이 켜졌어요. 차량 설명서를 확인하고 정비소에 문의해 주세요"로 처리. **경고 의미는 팀이 검증한 것만 넣는다.**

**MILESTONE_ODO** (odometerKm, seoulBusanTimes = odometerKm / 400 반올림)
- FRIEND: 우리 오늘로 딱 {odometerKm}km 같이 달렸어! 서울에서 부산까지 {seoulBusanTimes}번 간 거리래. 그동안 무사히 데려와줘서 고마워. 다음 1만 km도 잘 부탁해!
- BUTLER: 오늘로 누적 주행 {odometerKm}km를 달성했습니다. 서울에서 부산까지 {seoulBusanTimes}번을 달린 거리입니다. 그동안 안전하게 운전해 주셔서 감사합니다. 이참에 정기 점검 일정도 한번 확인해 보시길 권해 드립니다.
- BLUNT: {odometerKm}km 찍었다. 서울-부산 {seoulBusanTimes}번. 점검 한번 받자.

**BIRTHDAY** (odometerKm, parkings, longestDayKm)
- FRIEND: 오늘 내 생일이야! 1년 전 오늘 우리 처음 만났잖아. 그동안 {odometerKm}km를 같이 달렸고, 주차는 {parkings}번 했어. 제일 멀리 간 날은 하루에 {longestDayKm}km였는데 그날 진짜 신났어. 앞으로도 잘 부탁해!
- BUTLER: 오늘은 출고 1주년입니다. 지난 1년 동안 {odometerKm}km를 함께 달렸고, {parkings}번 주차했습니다. 하루 최장 주행은 {longestDayKm}km였습니다. 앞으로도 안전한 동행이 되겠습니다.
- BLUNT: 출고 1년. {odometerKm}km, 주차 {parkings}번. 최장 하루 {longestDayKm}km. 내년에도 잘 굴러갈게.

**WEEKLY** (days, distanceKm, rainyDays, hardBrakes, hardBrakesLastWeek, tireState)
- FRIEND: 이번 주엔 {days}일 동안 {distanceKm}km 달렸어. 비 온 날이 {rainyDays}일이라 와이퍼가 고생 좀 했지. 급제동은 {hardBrakes}번, 지난주({hardBrakesLastWeek}번)보다 줄었어. 운전이 점점 부드러워지는 중! + tireState별 꼬리말
- BUTLER: 이번 주는 {days}일 동안 {distanceKm}km를 달렸습니다. 비가 온 날은 {rainyDays}일이었습니다. 급제동은 {hardBrakes}회로, 지난주 {hardBrakesLastWeek}회보다 줄었습니다. + 꼬리말
- BLUNT: 이번 주 {days}일, {distanceKm}km. 비 {rainyDays}일. 급제동 {hardBrakes}번, 지난주보다 줄었음. + 꼬리말
- 꼬리말: 공기압 낮음 / 예약함 / 정상(없음) 3가지 × 말투. 급제동이 늘었으면 "줄었어" 대신 "늘었어" 문장으로 분기.

**BOOKED** (shop, slot)
- FRIEND: 고마워! {slot}에 {shop} 가는 거지? 그때까지 살살 달려줘.
- BUTLER: {shop}에 {slot} 예약 요청을 보냈습니다. 감사합니다.
- BLUNT: {slot} {shop}. 접수.

**주행 중 안내**
- FRIEND: 운전 중엔 말로만 할게. 주차하면 오늘 얘기 들려줄게!
- BUTLER: 주행 중에는 음성으로만 안내드리겠습니다. 주차 후 오늘의 일기를 들려드리겠습니다.
- BLUNT: 운전 중. 말로만 함. 일기는 주차 후.

## 부록 B. Fake 데이터 (시연용)

```json
{
  "today": { "distanceKm": 37.2, "driveMin": 74, "trips": 2, "wiperMin": 41, "hardBrakes": 1, "outsideTempC": 11, "heaterOn": true },
  "odometerKm": 9982,
  "tireLow": { "position": "왼쪽 뒤", "psi": 28, "recommendedPsi": 36 },
  "engine": { "light": "엔진 경고등", "color": "노란색", "state": "계속 켜짐" },
  "week": { "days": 5, "distanceKm": 182.4, "rainyDays": 2, "hardBrakes": 3, "hardBrakesLastWeek": 5 },
  "birthday": { "parkings": 612, "longestDayKm": 412 },
  "shops": [
    { "name": "도움카센터", "meta": "1.2km · 타이어·일반 정비", "slots": ["오늘 18:30", "내일 10:00", "내일 14:00"] },
    { "name": "한빛자동차정비", "meta": "2.8km · 엔진 진단 가능", "slots": ["내일 09:30", "내일 16:00"] },
    { "name": "바른타이어", "meta": "3.5km · 타이어 전문", "slots": ["오늘 19:00", "내일 11:00"] }
  ]
}
```
정비소는 지어낸 이름이다. 화면에 "시연용" 표시를 남겨.

## 부록 C. LLM 프롬프트 (일기·기념일·주간 일기 전용)

```
너는 차량용 앱 '붕붕일기'에서, 차가 1인칭으로 운전자에게 말하는 문장을 쓴다.
차 이름: {carName}
상황: {eventTitle}
말투: {toneLabel} - {toneRule}
규칙:
- 아래 데이터에 있는 숫자와 사실만 쓴다. 새 숫자, 장소, 사건을 지어내지 않는다.
- 2~4문장, 180자 이내.
- 이모지, 따옴표, 제목 없이 문장만 답한다.
데이터(JSON):
{summaryJson}
```
말투 규칙:
- 친구: 반말. 다정하고 장난스럽게, 운전자를 '너'라고 부른다.
- 집사: 존댓말(-습니다체). 정중하고 차분하게.
- 무뚝뚝: 반말. 짧은 단문만 쓰고 감탄사와 꾸밈말은 쓰지 않는다.

---

## 단계별 시작 문장 (AI 코딩 도구에 하나씩 붙여넣기)

1. `PROMPT.md를 전부 읽고 Phase 0만 해. 기능 코드는 쓰지 말고, 끝나면 docs/api-findings.md를 보여주고 멈춰.`
2. `Phase 0 결과를 반영해서 Phase 1을 해. 끝나면 에뮬레이터 스크린샷과 테스트 결과를 보여주고 멈춰.`
3. `Phase 2를 해. Phase 0 표에서 "됨"인 API만 실제로 연결하고, 나머지는 Fake로 남겨.`
4. `Phase 3, 4를 해. LLM 경로는 Phase 0 결과대로 정하고, 끊겨도 템플릿으로 동작하는지 테스트로 보여줘.`
5. `server 폴더에 Phase 5를 해. 테이블과 API 목록은 PROMPT.md 그대로 따르고, Testcontainers 테스트까지 통과시켜.`
6. `Phase 6을 하고, demo-script.md 순서대로 처음부터 끝까지 한 번 돌려본 결과를 보고해.`
