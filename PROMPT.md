# 붕붕일기 개발 명세 (PROMPT.md)

> AI 코딩 도구(Claude Code 등)에게 주는 단계별 개발 명세예요. 맨 아래 "단계별 시작 문장"을 하나씩 붙여넣으면 돼요.
> 항상 지켜야 할 규칙은 `CLAUDE.md`에도 요약돼 있어요.

---

## 0. 사람이 먼저 할 일 (AI가 대신 못 하는 것)

- [ ] **PR #3(차 앱 기본 화면) 리뷰 후 `main`에 합치기.** 이 명세의 Phase 1은 그 코드에서 시작해요.
- [ ] Pleos Playground 가입 → **My Project 만들기 + SDK 사용 신청** ([Create a Project and Request SDK Access](https://document.pleos.ai/en/docs/pleos-only/create-my-project))
- [ ] SDK 신청 후 받는 **에뮬레이터 시스템 이미지 URL**을 Android Studio SDK Update Sites에 등록 ([환경 설정 가이드](https://document.pleos.ai/en/docs/connect/guide/getting-started/application-development/setup-connect-sdk-emulator))
- [ ] Pleos Maven 저장소 주소 확인 (SDK 신청 후 안내됨)
- [ ] [Kakao Developers](https://developers.kakao.com)에 앱 등록 → REST API 키·JavaScript 키 발급, 카카오 로그인 켜기, 웹 도메인 등록, 팀원을 앱 팀 멤버로 추가 (메시지 권한을 받기 전에는 팀 멤버에게만 메시지가 가요)
- [ ] 가족 폰이 QR로 접속할 **서버 공개 주소** 준비 (클라우드 배포, 또는 시연 때 터널링)
- [ ] 마스코트 3종 그림 담당 정하기 (없으면 AI가 단순한 벡터 그림으로 먼저 만들어요)

일정: 기획안 제출 11/15 → 서류 결과 11/23 → 최종 PT 12/3~4
권장: Phase 0~1은 11/9까지(기획서에 첫 실행 화면 캡처를 넣기 위해), Phase 2~7은 11/29까지, Phase 8은 12/2까지.

---

## 1. 너의 역할, 목표, 현재 상태

너는 현대자동차그룹 차량용 앱 공모전에 나가는 팀의 개발자야. **붕붕일기**라는 Pleos Connect 앱(Android Automotive OS 기반)과 이를 받쳐주는 Spring Boot 서버를 만든다.

**최종 목표**: 12월 3일 PT에서 Pleos Connect 에뮬레이터로 아래 시연을 끝까지 문제없이 보여주는 것.

1. 처음 켜면 마스코트 3종 중 하나를 고르고, 닉네임(예: "킹카")을 정하고, 카카오톡 가족 연결은 되면 하고 안 되면 나중으로 미룬다.
2. 설정이 끝나면 차의 현재 상태를 불러오고, 마스코트가 그 결과로 첫인사를 한다. 앱 안 제목은 "킹카 일기"가 된다.
3. 주행을 마치고 P단에 놓으면, 마스코트가 1인칭으로 오늘 일기를 화면과 음성으로 들려준다.
4. 타이어·엔진·연료·엔진오일 경고를 사람 말로 설명하고, 정비소 예약이나 주유소 경유지 추가로 이어준다.
5. D단 주행 중에는 화면에 글을 띄우지 않고, 경고만 짧은 음성 질문으로 알려준다.
6. 1만 km 돌파, 출고 1주년을 축하한다.
7. 일요일 밤 주간 일기가 가족의 카카오톡으로 간다.

**현재 상태 (PR #3 기준)**
- `car-app/`에 Kotlin + Jetpack Compose 기본 화면이 있다. compileSdk/targetSdk 34, minSdk 28, 패키지 `com.bungbung.diary`.
- 화면: 첫 실행 세팅(예전 8단계), 메인(상태바 + 오늘/일기장/차 건강 탭), 정비소 예약, 주유소 목록, 설정, 시뮬레이터(Debug 패널).
- 모든 값은 `data/fake/FakeData.kt`의 고정 값이고, 상태는 `ui/AppState.kt`에 메모리로만 있어서 앱을 다시 켜면 처음부터 시작한다.
- 캐릭터 표정은 이모지로 대신하고 있다.
- 차 신호, 음성, LLM, 서버는 아직 없다.

## 2. 기능 명세

| 기능 | 동작 | 트리거 | 차 데이터 |
| --- | --- | --- | --- |
| 첫 실행 세팅 | 마스코트 → 닉네임 → 동의 → 가족 연결(선택) → 설정 완료 (3절) | 앱 첫 실행, 주차 중에만 | 없음 |
| 차 상태 불러오기 | 설정이 끝나면 현재 차 상태를 점검하고 그 결과로 첫인사 (3절) | 설정 완료 직후, 설정에서 다시 실행 가능 | 차 정보, 누적거리, 타이어, 경고, 연료·배터리 |
| 마스코트와 닉네임 | 마스코트 3종(생김새 + 성격 + 기본 말투). 닉네임을 정하면 앱 안 제목이 "{nickname} 일기" | 첫 실행 세팅, 설정 | 없음 |
| 오늘의 일기 | 오늘 주행을 1인칭 문장으로 화면+음성 출력 | 오늘 1km 이상 주행 후 기어 P | 주행거리, 와이퍼 작동 시간, 급제동, 외기온도, 히터 |
| 경고 통역 | 말로 설명 + "급한 정도 / 무슨 뜻 / 할 일" 3칸 고정 표시 | 경고 발생 즉시 | 타이어, 차량 경고(Safety), 연료, 엔진오일 |
| 정비소 예약 | 정비소와 시간을 골라 예약 요청 | 타이어·엔진·엔진오일 경고에서 | 경고 종류 |
| 주유소 경유지 | 가까운 주유소를 골라 내비 경유지로 추가 | 연료 부족 경고에서 | 연료 잔량, 주행 가능 거리 |
| 주행 중 음성 모드 | 글 숨김. 경고는 짧은 질문 + "응/아니" 음성 답(큰 버튼 2개는 대체 수단) | 기어 D 또는 CarUxRestrictions 활성 | 기어, UX 제한 상태 |
| 기념일 | 축하 메시지 + 축하 표정, 일기장에 기록 | 누적 1만 km 단위 돌파, 출고일 | 누적 주행거리, 출고일(설정에서 입력) |
| 가족 주간 일기 | 연결된 가족의 카카오톡과 가족 웹 페이지로 전달 | 일요일 21:00 | 주간 집계, 미해결 점검 |
| 일기장 / 차 건강 | 지난 일기와 기록, 바퀴별 공기압·경고·예약 상태 | 항상 | 위와 동일 |

**화면 구성**: 상단 상태바(제목 "{nickname} 일기", 시각, 외기온도, 기어 P/R/N/D, 누적 km) / 왼쪽 마스코트 + 오늘 기록 / 오른쪽 메시지 카드 / 하단 탭(오늘, 일기장, 차 건강).

**마스코트 표정 4종**: 평소, 걱정(경고), 축하(기념일), 집중(주행 중).

**설정 화면**: 마스코트 바꾸기, 닉네임, 말투(마스코트 기본값과 다르게 바꿀 수 있음), 가족 연결, 출고일, 엔진오일 마지막 교체 시점(차에서 못 읽을 때만), 차 상태 다시 불러오기, 모든 기록 지우기, 첫 실행 세팅 다시 하기.

## 3. 첫 실행 흐름 (지금 가장 먼저 만들 프로토타입)

```
[설정]  환영 → 마스코트 고르기 → 닉네임 → 동의 → 가족 연결(가능하면) → 설정 완료
[시작]  차 상태 불러오기 → 첫인사 → 홈(오늘 탭) 시작 화면
```

**전제 조건**
- 주차 중(P단)에만 진행한다. 주행 중이거나 `CarUxRestrictions`가 키보드 입력을 막으면 "주차한 뒤에 이어서 할게요" 화면을 띄우고 멈춘다.
- 진행 단계를 저장해서 중간에 앱을 꺼도 그 단계부터 이어서 한다. 세팅을 끝낸 뒤에는 앱을 켜면 바로 홈으로 간다.
- 화면 하나에 할 일 하나. 큰 버튼. 건너뛸 수 있는 단계엔 "나중에" 버튼.

| 순서 | 화면 | 하는 일 | 저장 값 |
| --- | --- | --- | --- |
| 1 | 환영 | 마스코트 3종이 나란히 인사하고 "나랑 같이 다닐 친구를 골라줘"를 보여준다 | 없음 |
| 2 | 마스코트 고르기 | 카드 3장(그림, 이름, 성격 한 줄, 자기소개 문장, "들어보기" 버튼). 하나를 고르면 기본 말투도 정해진다 | `mascot`, `tone` |
| 3 | 닉네임 | 고른 마스코트 그림 + 입력칸 + 그 마스코트의 추천 닉네임 칩. 입력하는 동안 제목 미리보기 "킹카 일기"가 바로 바뀐다 | `nickname` |
| 4 | 동의 | 모으는 것(차 상태 데이터, 닉네임), 보내지 않는 것(위치, 경로), 가족에게 가는 것(주간 숫자와 마스코트의 말). 필수 동의가 없으면 다음으로 못 감 | `consentVersion`, `consentAt` |
| 5 | 가족 연결 | **연결할 수 있으면** QR과 6자리 코드를 보여주고, 연결된 가족 이름을 띄운다. **연결할 수 없으면** "지금은 연결할 수 없어요. 설정 > 가족 연결에서 나중에 할 수 있어요"를 보여주고 "다음"만 남긴다. 어느 쪽이든 "나중에" 가능 | 가족 목록(서버), `familyLinkDeferred` |
| 6 | 설정 완료 | "준비 끝! 이제 {nickname:이/가} 지금 몸 상태를 살펴볼게"와 함께 차 상태 불러오기로 넘어간다 | `setupCompletedAt` |
| 7 | 차 상태 불러오기 | 아래 "건강검진" 항목을 하나씩 확인하는 애니메이션 | `baselineOdometerKm`, `firstMetAt`, 첫 점검 결과 |
| 8 | 첫인사 | 마스코트가 점검 결과로 첫인사(부록 A의 FIRST_MEET)를 화면과 음성으로 한다 | `onboardingCompletedAt` |
| 9 | 홈 시작 화면 | 아래 "홈 시작 화면" 참고 | 없음 |

**가족 연결 "가능" 판단** (`FamilyLinkAvailability`)
- 서버에 연결되고(`GET /api/config` 성공) 서버가 `kakaoEnabled: true`를 주면 가능.
- 네트워크 없음, 서버 오류, 카카오 설정 꺼짐이면 불가. 판단은 3초 안에 끝내고, 넘으면 불가로 본다.
- 나중으로 미뤘으면 홈에 "가족 연결하기" 카드를 한 번 보여주고, 닫으면 다시 띄우지 않는다.
- Phase 1에는 서버가 없으니 항상 불가로 동작하고, Debug 패널에 "가족 연결 가능으로 바꾸기" 스위치를 둔다(Fake QR과 이름 목록 표시).

**차 상태 불러오기 (건강검진)**

항목마다 Capability 확인 → Get 1회. 지원하지 않으면 "이 차에선 확인할 수 없어요"로 표시하고 넘어간다. 한 항목이 실패해도 전체가 멈추지 않는다. 단위는 Display API의 사용자 단위 설정을 따른다(psi/kPa, km/mi).

| 항목 | 데이터 | 표시 예 |
| --- | --- | --- |
| 차 정보 | CarInfo (제조사, 생산 연도) | 2026년식 |
| 누적 주행거리 | Odometer | 9,982 km |
| 타이어 | Tire (바퀴 4개 공기압) | 왼쪽 뒤 28 psi · 주의 |
| 경고등 | Safety | 켜진 경고 없음 |
| 연료 또는 배터리 | Fuel / EvBattery | 62% |

- 결과는 "첫 점검" 기록으로 저장하고 차 건강 탭에서 다시 볼 수 있다.
- 누적거리를 `baselineOdometerKm`로 저장해서 "우리가 함께 달린 거리"를 계산한다.
- 주의 항목이 있으면 첫인사에 반영하고, 홈에서 경고 카드로 먼저 보여준다.

**홈 시작 화면** (세팅 직후, 아직 오늘 주행이 없을 때)
- 왼쪽: 마스코트(평소 표정, 주의 항목이 있으면 걱정 표정) + "지금 내 상태" 요약(누적거리, 타이어, 경고등, 연료·배터리)
- 오른쪽: 주의 항목이 있으면 경고 카드, 없으면 START_EMPTY 메시지("첫 주행이 끝나면 오늘 얘기 들려줄게!")
- 일기장 첫 기록: "처음 만난 날"과 첫인사 문장

**마스코트 3종** (원본 캐릭터. 기존 캐릭터나 브랜드를 닮게 만들지 않는다)

| id | 이름 | 생김새 | 성격 / 기본 말투 | 추천 닉네임 | 자기소개 (카드 문장) |
| --- | --- | --- | --- | --- | --- |
| `CAR` | 꼬마차 | 동글동글한 민트색 꼬마 자동차, 큰 눈 | 다정하고 장난스러움 / 친구 | 붕붕이, 킹카, 씽씽이 | 안녕! 나랑 같이 달리면 매일 재밌는 얘기 해줄게. |
| `OWL` | 부엉이 | 동그란 안경과 나비넥타이를 한 부엉이 | 정중하고 차분함 / 집사 | 부엉 집사, 밤톨이 | 안녕하십니까. 매일의 주행을 차분히 정리해 드리겠습니다. |
| `BEAR` | 곰 | 정비 모자를 쓴 무표정한 곰 | 짧고 담백함 / 무뚝뚝 | 뚝심이, 곰탱이 | 안녕. 필요한 말만 한다. |

- 마스코트마다 표정 4종(평소, 걱정, 축하, 집중)이 필요하다. 총 12장.
- 그림은 `MascotArt` 인터페이스 뒤에 두고, 먼저 Compose Canvas나 VectorDrawable로 단순하게 그린다. 디자이너 그림이 나오면 이 구현만 바꾼다. 이모지는 쓰지 않는다.
- 120dp 크기에서도 표정이 구분돼야 하고, 어두운 차 화면 배경에서 잘 보여야 한다.

**닉네임 규칙**
- 1~8자, 한글·영문·숫자만. 앞뒤 공백 제거. 비속어 목록에 걸리면 "다른 이름은 어때요?"를 보여준다.
- 앱 안 제목, 일기장, 가족 메시지에 `"{nickname} 일기"`를 쓴다. 런처(앱 목록)에 보이는 이름은 "붕붕일기"로 고정한다. Android 앱 이름은 실행 중에 자유롭게 바꿀 수 없다.
- **조사 처리**: 닉네임, 가족 이름, 장소 이름 뒤 조사는 마지막 글자 받침에 따라 고른다. `JosaUtil`을 만들고 테스트한다.
  - 킹카**가** / 붕붕이**가**, 킹카**는** / 뚝심**은**, 킹카**야** / 뚝심**이야**, 킹카**와** / 뚝심**과**, 새싹주유소**를** / 푸른에너지**를** / 한빛정비**를**
  - 숫자로 끝나면 읽는 소리로 판단(0 영, 1 일, 3 삼, 6 육, 7 칠, 8 팔은 받침 있음). 영문은 L·M·N·R로 끝나면 받침 있음으로 본다.
- 선택 기능: SpeechToText SDK를 쓸 수 있으면 "말로 이름 짓기" 버튼(Phase 0에서 확인).

## 4. 절대 규칙

1. **Pleos·카카오 API 이름과 시그니처를 지어내지 마.** 쓰기 전에 공식 문서나 SDK 라이브러리(Gradle 캐시의 클래스)에서 확인하고, 코드 주석에 문서 URL을 남겨. 확인이 안 되면 멈추고 물어봐.
2. **모든 외부 의존성은 우리 인터페이스 뒤에 숨겨.** `VehicleDataSource`, `Speaker`, `VoiceAnswer`, `TextGenerator`, `FamilyLinkClient`, `MascotArt` 각각 Fake 구현을 유지해서 차와 서버 없이도 앱이 끝까지 돈다.
3. **숫자는 데이터에서만.** 문장에 나오는 숫자는 요약 데이터에 있는 숫자여야 한다. LLM 출력에 데이터에 없는 숫자가 있으면 버리고 템플릿 문장을 쓴다.
4. **경고 문장은 LLM을 쓰지 않는다.** 검증된 템플릿에서 말투만 고른다. "급한 정도 / 무슨 뜻 / 할 일"은 말투와 무관하게 고정. 경고의 의미는 팀이 검증한 것만 쓴다.
5. **운전 중 화면 제한을 지켜.** `CarUxRestrictions`가 distraction optimization을 요구하면 글을 숨기고 음성만 쓴다. 키보드 입력은 주차 중 닉네임 입력 하나뿐이다.
6. **개인정보 최소화.** 위치와 경로는 서버와 가족에게 보내지 않는다(주유소 검색에 쓰는 위치는 차 안에서만 쓴다). Pleos 심사 기준상 개인정보·위치를 쓰는 앱은 이용약관과 데이터 처리 방침을 제출해야 하고, 사용 전 동의가 필요하다.
7. **기존 코드를 존중해.** PR #3 코드를 고쳐서 쓰고, 통째로 다시 쓰지 마. 화면 파일 위치와 패키지 구조를 유지하고, `FakeData`는 진짜 구현으로 바뀔 때까지 남긴다.
8. **단계마다 브랜치를 만들고 PR로 올려.** `main`에 직접 푸시하지 않는다. 브랜치 이름 예: `feat/phase1-first-run`.
9. **단계마다 빌드와 테스트를 돌리고**, 끝나면 "확인한 것 / 확인 못 한 것"을 나눠서 보고하고 멈춰.

## 5. 확인된 사실 (2026-10-31 문서 기준)

출처를 밝힌 것만 적었다. 버전 번호와 정책은 바뀔 수 있으니 최신 문서로 다시 확인해.

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
- 권한 후보 ([샘플 앱](https://document.pleos.ai/en/docs/pleos-only/tutorial/vehicle-sdk-sample-app), [권한 문서](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle-permission)): `pleos.car.permission.CAR_MILEAGE`, `CAR_TIRES`, `READ_WINDSHIELD_WIPERS`, `CAR_SAFETY`, `CAR_DRIVE`, `CAR_SPEED`, `CAR_CLIMATE`, `CAR_INFO`, `CAR_ENERGY`, `READ_CAR_DISPLAY_UNITS`. 승인 안 된 권한을 쓰면 심사에서 떨어질 수 있으니 **필요한 것만** 선언
- 속도, 기어, 외기온도 같은 표준 데이터는 AAOS Car API(`CarPropertyManager`)를 우선 쓰는 게 Pleos 정책 ([API 정책](https://document.pleos.ai/en/docs/pleos-only/vehicle-app-planning-guide/pleos-connect-sdk-api))
- 엔진오일 수명은 Vehicle SDK에서 읽을 수 있는지 아직 모른다. 못 읽으면 설정에서 입력한 "마지막 교체 시점 누적거리"로 계산한다.

**TextToSpeech SDK** ([소개](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/TextToSpeech/intro))
```kotlin
val textToSpeech = TextToSpeech(applicationContext, Mode.HYBRID) // ai.pleos.playground.tts
// initialize() → addEventListener() → setLocale() → speak()
// 서버 모드에선 OnServerConnectionListener.onConnected() 이후에 speak()
// 끝나면 stop(), removeEventListener(), release()
```
- `HYBRID`(기본): 서버 우선, 네트워크 실패 시 온디바이스. My Project 등록 필요
- `ON_DEVICE`: 네트워크 없이 동작, 등록 불필요 → **경고 음성은 ON_DEVICE로**
- [TTS Pre-roll Silence 가이드](https://document.pleos.ai/en/docs/pleos-only/vehicle-app-developer-guide/voice-guidance-preroll-silence-guide), [Audio Focus 가이드](https://document.pleos.ai/en/docs/pleos-only/vehicle-app-developer-guide/audio-focus-development-guide/audio-focus-development)
- SpeechToText SDK도 있다([SDK 목록](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/)). "응/아니" 답과 "말로 이름 짓기"에 쓸 수 있는지 Phase 0에서 확인.

**NaviHelper SDK** ([Pleos 개발 소개](https://pleos.ai/playground/develop))
- 목적지 설정, 경로(경유지) 추가·삭제, 주행 경로 조회를 지원한다고 소개돼 있다. 주유소 경유지 추가에 쓴다.
- 주변 주유소 검색 API는 아직 확인하지 못했다. 후보: NaviHelper, 한국석유공사 오피넷 Open API. Phase 0에서 확인.
- 차 위치를 주는 Fused Location SDK는 준비 중이었다. Phase 0에서 상태 확인.

**운전 중 제한** ([Driving Restriction 가이드](https://document.pleos.ai/en/docs/pleos-only/vehicle-app-developer-guide/driving-restriction-development-guide))
- AAOS `CarUxRestrictionsManager.registerListener()` → `onUxRestrictionsChanged()`로 상태 수신
- `isRequiresDistractionOptimization()`과 `getActiveRestrictions()`를 보고 기능별로 제한 적용

**LLM (불확실)**
- 예전 문서엔 Gleo AI SDK에 LLM이 있었지만, 지금 LLM 문서 주소는 서비스 상태 페이지로 넘어가고 "Using SDKs" 목록에도 없다. **Phase 0에서 반드시 확인.** 못 쓰면 우리 서버가 외부 LLM API를 대신 부른다(Phase 5).

**카카오톡** (Kakao Developers)
- 카카오톡 공유: 사용자가 카카오톡에서 받을 친구나 채팅방을 직접 고르고 직접 보낸다. JavaScript 키와 JavaScript SDK 도메인 등록이 필요하다. ([카카오톡 공유 JavaScript](https://developers.kakao.com/docs/latest/ko/kakaotalk-share/js-link))
- 카카오톡 메시지 API: "나에게 보내기"와 "서비스 사용자 간 메시지 전송(친구에게 보내기)"이 있다. ([메시지 문서](https://developers.kakao.com/docs/latest/message))
- 친구에게 보내기는 권한 신청 전엔 앱에 등록된 팀 멤버끼리만 되고, 전체 사용자 대상은 검수를 거친 권한이 필요하다. ([카카오 데브톡 답변](https://devtalk.kakao.com/t/api-api/110820), 2020년 글이라 현재 정책 재확인 필요)
- 나에게 보내기의 동의항목, 조건, 쿼터는 **Phase 7 시작 전에 공식 문서로 확인**하고 `docs/api-findings.md`에 적어.
- 단톡방에 자동으로 메시지를 올리는 일반 API는 확인하지 못했다. 단톡방은 카카오톡 공유로 처리한다.
- REST API 키와 Client Secret은 서버 환경변수에만 둔다. 차 앱과 저장소에는 넣지 않는다.

## 6. 기술 스택과 저장소 구조

```
bungbung-diary/
├─ car-app/            Android Automotive 앱 (Kotlin, Jetpack Compose)
├─ server/             Spring Boot 3 (Java 21), PostgreSQL, Flyway, Spring Data JPA, springdoc-openapi
├─ docs/               api-findings.md, decisions.md, demo-script.md
├─ docker-compose.yml  로컬 PostgreSQL
├─ CLAUDE.md           AI 코딩 도구가 항상 지킬 규칙 요약
└─ PROMPT.md           이 명세
```

차 앱 (`car-app/app/src/main/java/com/bungbung/diary/`). 지금 있는 것 + 만들 것:
```
domain/          Models.kt (Tone, CarEvent, Mood, DailySummary …)  + Mascot, CarProfile, TriggerEngine, JosaUtil
data/fake/       FakeData.kt (유지)                                 + Fake 구현들
data/vehicle/    (만들 것) VehicleDataSource: Pleos 구현 + Fake 구현
data/voice/      (만들 것) Speaker, VoiceAnswer: Pleos 구현 + Fake 구현
data/text/       (만들 것) TextGenerator: Template 구현 + Llm 구현 + NumberGuard
data/local/      (만들 것) DataStore(프로필, 세팅 진행 단계) + Room(일별 요약, 메시지, 점검 기록)
data/remote/     (만들 것) 서버 API 클라이언트, FamilyLinkClient
ui/              onboarding, main, today, diary, health, booking, station, settings, debug, components, theme
ui/mascot/       (만들 것) MascotArt, 마스코트 3종 × 표정 4종
```
상태 관리: `AppState`(메모리) → 화면별 ViewModel + 저장소(DataStore/Room)로 옮긴다. Coroutines/Flow 사용.

## 7. 데이터 계약

```json
// CarProfile: 첫 실행 세팅에서 정하는 값 (DataStore, 서버에도 동기화)
{
  "mascot": "CAR",
  "nickname": "킹카",
  "tone": "FRIEND",
  "consentVersion": "2026-11-01",
  "familyLinkDeferred": true,
  "setupCompletedAt": "2026-11-01T14:02:10+09:00",
  "firstMetAt": "2026-11-01",
  "baselineOdometerKm": 9982,
  "deliveryDate": null,
  "lastOilChangeOdometerKm": null,
  "onboardingCompletedAt": "2026-11-01T14:03:00+09:00"
}
```

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

- `Mascot`: `CAR`, `OWL`, `BEAR`
- `Tone`: `FRIEND`, `BUTLER`, `BLUNT`
- `CarEvent`: `FIRST_MEET`, `FAMILY_JOINED`, `DIARY`, `TIRE_LOW`, `SAFETY_WARNING`, `LOW_FUEL`, `ENGINE_OIL`, `WAYPOINT_ADDED`, `MILESTONE_ODO`, `BIRTHDAY`, `WEEKLY`, `BOOKED`
- `CarMessage`: `id, event, tone, mascot, text, textSource(TEMPLATE|LLM), data(JSON), createdAt, delivery(SCREEN_AND_VOICE|VOICE_ONLY|DEFERRED)`

집계 규칙 (임계값은 전부 상수로 빼고 `docs/decisions.md`에 기록):
- 날짜는 차의 현지 시각 기준. 자정을 넘긴 주행은 시작한 날에 넣는다.
- 주행거리 = 그날 첫 시동 때 누적거리와 현재 누적거리의 차
- 와이퍼 시간 = 와이퍼 상태가 OFF가 아닌 시간의 합
- 급제동 = 초당 감속이 임계값을 넘은 횟수 (초기값 12 km/h/s)
- 타이어 경고 = Tire API 경고 상태 우선, 없으면 권장값 대비 임계값
- 연료 부족 = 잔량 10% 이하 또는 주행 가능 거리 50km 이하 (초기값)
- 기념일 = 누적거리가 10,000 km 배수를 넘는 순간 / 출고일과 같은 월·일

## 8. 트리거와 출력 규칙

| 상황 | 출력 |
| --- | --- |
| 오늘 1km 이상 주행 후 기어 P | DIARY: 화면 + 음성 |
| 경고 발생, 주차 중 | 경고 카드 + 음성, 정비소 보기 또는 주유소 보기 버튼 |
| 경고 발생, 주행 중 | 글 없이 짧은 음성 질문(부록 A의 VOICE_PROMPT) → "응"이면 주유소 경유지 추가 또는 주차 후 정비소 표시, "아니"거나 10초 동안 답이 없으면 넘어간다. 같은 경고는 해제될 때까지 한 번만 묻는다 |
| 기념일·일기, 주행 중 | DEFERRED, P단이 되면 표시 |
| 일요일 21:00 | WEEKLY 생성 → 서버에 게시 |

차 앱은 서버 없이도 모든 기능이 돌아야 한다. 서버로 보낼 것은 쌓아뒀다가 네트워크가 되면 보낸다(WorkManager).

## 9. 단계별 작업

각 단계는 새 브랜치에서 하고, 끝나면 빌드·테스트 결과와 "확인한 것 / 못 한 것"을 보고하고 PR을 올린 뒤 멈춰.

### Phase 0. 환경과 API 조사 (기능 코드 금지)
1. `car-app`을 문서 설정대로 Pleos 에뮬레이터에서 실행한다.
2. **Probe 화면**(Debug 메뉴 안): CarInfo, Odometer, Tire, Wiper, Safety, Brake, Fuel, EvBattery, Display(단위)와 AAOS 표준(속도, 기어, 외기온도)에 대해 Capability 확인 → Get 1회 → Callback 등록 결과를 화면과 로그에 출력한다.
3. TTS `ON_DEVICE`로 한국어 문장 하나 읽기, SpeechToText로 "응/아니" 인식 시도.
4. LLM SDK, NaviHelper 경유지 추가, 주변 주유소 검색, 엔진오일 정보, Fused Location 상태를 확인한다.
5. `docs/api-findings.md` 표를 채운다: API·프로퍼티 / 권한 / 에뮬레이터 동작 / 값 예시 / 출처 URL / 대안.

완료 기준: 표의 모든 행이 "됨 / 안 됨 / 확인 불가" 중 하나로 채워지고, 안 되는 항목엔 대안이 있다.

### Phase 1. 첫 실행 프로토타입 ⭐ (지금 우선순위)
PR #3 코드에서 시작한다. 차 신호는 아직 Fake.
1. `domain/`에 `Mascot`(3종, 기본 말투, 추천 닉네임, 자기소개)과 `CarProfile`을 추가한다.
2. `ui/mascot/`에 `MascotArt`와 3종 × 표정 4종 그림을 만들고, 지금 이모지(`Mood.emoji`)를 쓰는 곳을 전부 바꾼다.
3. `OnboardingScreen`을 3절 흐름으로 고친다: 환영 → 마스코트 → 닉네임 → 동의 → 가족 연결 → 설정 완료 → 차 상태 불러오기 → 첫인사. 지금 있는 "말투" 단계는 마스코트 선택으로 대체하고, "출고일" 단계는 설정 화면으로 옮긴다.
4. 프로필과 세팅 진행 단계를 DataStore에 저장한다. 앱을 켰을 때 세팅을 마쳤으면 바로 홈, 중간이면 그 단계부터.
5. 가족 연결 단계: `FamilyLinkAvailability`가 불가면 안내 문구와 "다음"만, 가능(Debug 스위치)이면 Fake QR·코드·이름 목록.
6. 차 상태 불러오기: 항목이 하나씩 나타나는 화면. `FakeVehicleDataSource`로 동작하고, 일부 항목을 "확인할 수 없어요"로 만드는 Fake 설정도 둔다.
7. 홈 시작 화면(3절)과 일기장 첫 기록 "처음 만난 날".
8. 주행 중(Debug 패널의 D) 세팅이 멈추고 "주차한 뒤에 이어서 할게요"가 뜨는지.
9. `JosaUtil`과 단위 테스트, 닉네임 검증 테스트.
10. "건너뛰기 (개발용)" 버튼은 debug 빌드에서만 보이게 한다.

완료 기준: 앱 데이터를 지우고 켜면 세팅이 처음부터 나오고, 부엉이를 골라 "킹카"로 정하면 제목·일기장·첫인사가 전부 "킹카"와 집사 말투로 바뀐다. 앱을 다시 켜면 세팅 없이 홈으로 간다. 스크린샷을 남긴다.

### Phase 2. 핵심 기능 정리 (Fake 데이터)
1. `TemplateTextGenerator`: 부록 A 문장을 이벤트 × 말투 전부 구현하고 `FakeData.message()`를 대체한다. 조사가 붙는 곳은 `JosaUtil`을 쓴다.
2. Room에 일별 요약, 메시지, 점검 기록 저장. 일기장 탭이 저장된 기록을 보여준다.
3. 경고 → 정비소 예약, 연료 부족 → 주유소 경유지, 기념일, 주간 일기 흐름을 시뮬레이터로 끝까지 확인한다.
4. 설정 화면(2절 목록)을 완성한다.
5. 단위 테스트: 템플릿 전부, 숫자가 데이터와 일치하는지, 조사.

완료 기준: Debug 패널만으로 1절 시연 1~7단계가 재현된다(음성 제외).

### Phase 3. 실제 차 신호 연결
1. `PleosVehicleDataSource` 구현 (Phase 0 표에서 "됨"인 것만). 앱 생명주기에 맞춰 `initialize()` / `release()`.
2. `DailyAggregator`: 7절 규칙으로 신호를 DailySummary로 집계해 저장.
3. `TriggerEngine`: 8절 규칙. 같은 경고는 해제 전까지 한 번만.
4. `CarUxRestrictions` 연결: 제한 중엔 글 숨김, DEFERRED 처리, 세팅 중단.
5. 차 상태 불러오기를 실제 API로 연결한다.
6. 에뮬레이터 Virtual Console로 기어·타이어·연료 값을 바꿔가며 테스트한다.

완료 기준: Fake 없이 에뮬레이터 신호만으로 차 상태 불러오기, DIARY, TIRE_LOW가 동작한다.

### Phase 4. 음성
1. `PleosSpeaker`: 경고는 `ON_DEVICE`, 일기는 `HYBRID`(등록 안 됐으면 ON_DEVICE). 마스코트 카드의 "들어보기"도 연결한다.
2. `VoiceAnswer`: SpeechToText로 "응/아니"를 받는다. 못 쓰면 큰 버튼 2개만 남긴다.
3. 마스코트별 목소리 차이(속도·높낮이)는 SDK가 지원할 때만 적용하고, 안 되면 기록만 남긴다.
4. 오디오 포커스 가이드대로 다른 소리와 겹치지 않게 한다.

완료 기준: 주행 중 경고가 글 없이 음성 질문으로 나오고, "응"이라고 답하면 다음 동작으로 이어진다.

### Phase 5. 문장 생성 (LLM)
1. `TextGenerator` 뒤에 `LlmTextGenerator` 추가. 경로는 Phase 0 결과로 정한다: Pleos LLM SDK를 쓸 수 있으면 그것, 아니면 서버 `POST /api/narrate`(키는 서버에만).
2. 프롬프트는 부록 C를 그대로 쓴다.
3. `NumberGuard`: 출력의 숫자가 전부 입력 데이터에 있는지 검사. 실패, 3초 초과, 오류면 템플릿으로 대체.
4. 일기는 주행 중에 미리 만들어 두고 P단에서 바로 보여준다.
5. 경고·예약·경유지 문장이 LLM 경로를 타지 않는다는 걸 테스트로 보장한다.

완료 기준: LLM을 끊어도 모든 기능이 템플릿으로 정상 동작한다.

### Phase 6. 서버 (Spring Boot)
테이블 (Flyway 마이그레이션):
- `car(id, mascot, nickname, tone, delivery_date, first_met_at, baseline_odometer_km, consent_version, consent_at, onboarding_completed_at, device_token_hash, created_at)`
- `daily_summary(id, car_id, date, summary jsonb)` — `(car_id, date)` 유니크
- `diary_entry(id, car_id, event, tone, mascot, text, text_source, created_at)`
- `family_share(id, car_id, share_code, created_at)`
- `pairing_code(code, car_id, expires_at, created_at)` — 6자리, 10분 만료
- `family_member(id, car_id, display_name, kakao_user_id, message_opt_in, kakao_token_enc, joined_at)` — 토큰은 암호화 저장, 연결 해제하면 삭제
- `shop(id, name, description, slots jsonb)` — 시연용 시드 데이터
- `booking(id, car_id, shop_id, slot, reason, status, created_at)`

엔드포인트:

| 메서드 | 경로 | 하는 일 |
| --- | --- | --- |
| GET | `/api/config` | `kakaoEnabled` 등 차 앱이 쓸 기능 스위치 |
| POST | `/api/cars` | 차 등록, 기기 토큰 발급 |
| PUT | `/api/cars/{carId}/profile` | 마스코트, 닉네임, 말투, 출고일, 동의 버전, 첫 점검 기준값 |
| PUT | `/api/cars/{carId}/summaries/{date}` | 일별 요약 저장(멱등) |
| POST | `/api/cars/{carId}/diaries` | 일기·기념일 문장 저장 |
| GET | `/api/cars/{carId}/diaries` | 기간별 조회 |
| POST | `/api/cars/{carId}/weekly` | 주간 요약 생성, 가족에게 전달 |
| POST | `/api/cars/{carId}/pairing-codes` | 가족 연결 코드 발급 (6자리 + QR 주소, 10분) |
| GET | `/api/cars/{carId}/family` | 연결된 가족 목록 (세팅 화면에서 3초마다 조회) |
| DELETE | `/api/cars/{carId}/family/{memberId}` | 가족 연결 해제, 토큰 삭제 |
| GET | `/family/{shareCode}` | 가족용 주간 일기 웹 페이지 (로그인 없음, 위치 없음) |
| GET | `/api/shops` | 시연용 정비소 목록 |
| POST | `/api/bookings` | 예약 요청 (상태 REQUESTED) |
| POST | `/api/narrate` | LLM 프록시 (Phase 5에서 필요할 때만) |

요구사항:
- 인증은 차 등록 때 발급한 기기 토큰(Bearer) 하나로 단순하게.
- Bean Validation, 전역 예외 처리, springdoc으로 API 문서 자동 생성.
- 주간 집계는 서버에서 `daily_summary`로 계산(지난주 대비 급제동 비교 포함).
- 테스트: 서비스 단위 테스트 + Testcontainers PostgreSQL 통합 테스트.
- `/api/narrate`는 요청에 위치·경로가 있으면 400으로 거절하고, 차별 분당 호출을 제한한다.
- GitHub Actions: PR마다 서버 빌드와 테스트를 돌린다.

완료 기준: `docker compose up` 후 차 앱이 프로필과 일기를 저장하고, `/family/{shareCode}`에서 주간 일기가 보인다.

### Phase 7. 가족 연결과 카카오톡
1. 5절 카카오톡 사실을 공식 문서로 다시 확인하고 `docs/api-findings.md`에 적는다(동의항목, 권한, 쿼터).
2. 서버 `GET /join/{code}`: "{nickname}의 가족이 되어 주세요"(조사 처리), 받는 정보와 보내는 정보 안내, 카카오 로그인 버튼. 폰 화면 기준.
3. `GET /oauth/kakao/callback`: 가족 등록, 메시지 수신에 동의한 경우에만 토큰을 암호화해 저장.
4. 주간 일기 발송 잡: 동의한 가족에게 "나에게 보내기". 한 명이 실패해도 다음 사람은 계속. 토큰 만료 시 갱신하고, 실패하면 "다시 연결해 주세요" 상태로 표시.
5. `/family/{shareCode}`에 "카카오톡으로 공유" 버튼(JavaScript SDK).
6. 차 앱 `FamilyLinkClient`를 실제 서버로 연결해, 첫 실행 세팅과 설정의 가족 연결이 실제로 동작하게 한다.
7. 카카오 API는 인터페이스 뒤에 두고 Fake로 통합 테스트. 실제 발송은 팀 멤버 계정으로 수동 확인.

완료 기준: 첫 실행 세팅에서 폰으로 QR을 찍어 카카오 로그인하면 차 화면에 이름이 뜨고(FAMILY_JOINED), 주간 일기가 그 사람 카카오톡 '나와의 채팅'에 도착한다. 서버를 끄면 세팅이 "나중에"로 자연스럽게 넘어간다.

### Phase 8. 시연 준비
1. `docs/demo-script.md`를 실제 버튼 순서와 할 말로 고친다. 첫 실행 세팅은 발표 시작에 보여준다.
2. Debug 패널에 "처음 상태로"와 "첫 실행 세팅 다시 하기".
3. 네트워크가 끊겨도 시연이 끝까지 가는지 확인(템플릿 + ON_DEVICE 음성 + 가족 연결 "나중에").
4. README: 설치, 실행, 스크린샷, 구조도, 확인된 제약.

---

## 부록 A. 템플릿 문장 (이벤트 × 말투)

`{}`는 데이터 값. 조사가 붙는 자리는 `{nickname:이야/야}`처럼 적고 `JosaUtil`로 고른다.

**환영** (닉네임 정하기 전, 공통): 안녕! 나랑 같이 다닐 친구를 골라줘.

**마스코트 자기소개**: 3절 마스코트 표의 "자기소개" 열.

**설정 완료** (공통): 준비 끝! 이제 {nickname:이/가} 지금 몸 상태를 살펴볼게.

**FIRST_MEET** (데이터: nickname, odometerKm, healthLine)
- FRIEND: 반가워! 나는 {nickname:이야/야}. 지금까지 {odometerKm}km 달려왔어. 방금 몸 상태를 살펴봤는데 {healthLine}. 앞으로 매일 오늘 얘기 들려줄게!
- BUTLER: 처음 뵙겠습니다. 저는 {nickname}입니다. 지금까지 {odometerKm}km를 달려왔습니다. 방금 점검해 보니 {healthLine}. 앞으로 매일 하루를 정리해 드리겠습니다.
- BLUNT: {nickname}. 누적 {odometerKm}km. {healthLine}. 잘 부탁.

| healthLine | 친구 | 집사 | 무뚝뚝 |
| --- | --- | --- | --- |
| 모두 정상 | 다 괜찮아 | 이상 없습니다 | 이상 없음 |
| 타이어 주의 | {position} 타이어 바람이 좀 빠졌어 | {position} 타이어 공기압이 낮습니다 | {position} 타이어 공기압 낮음 |
| 경고등 켜짐 | 경고등이 {warningCount}개 켜져 있어서 신경 쓰여 | 켜진 경고등이 {warningCount}개 있습니다 | 경고등 {warningCount}개 |
| 연료 부족 | 배가 좀 고파, 기름이 {fuelPercent}% 남았어 | 연료가 {fuelPercent}% 남았습니다 | 기름 {fuelPercent}% |
| 확인 못 한 항목만 있음 | 몇 가지는 내가 확인할 수 없었어 | 일부 항목은 확인할 수 없었습니다 | 일부 확인 불가 |

**START_EMPTY** (홈 시작 화면, 오늘 주행 전)
- FRIEND: 첫 주행이 끝나면 오늘 얘기 들려줄게!
- BUTLER: 첫 주행을 마치시면 오늘의 기록을 정리해 드리겠습니다.
- BLUNT: 달리고 오면 말해줌.

**FAMILY_JOINED** (데이터: memberName)
- FRIEND: {memberName:이/가} 우리 가족이 됐어! 일요일마다 일기 보내줄게.
- BUTLER: {memberName} 님이 가족으로 연결되었습니다. 매주 일요일에 주간 일기를 보내 드리겠습니다.
- BLUNT: {memberName} 연결됨. 일요일마다 보냄.

**DIARY**
- FRIEND: 오늘은 너랑 {distanceKm}km 달렸어. 아침부터 비가 와서 와이퍼를 {wiperMin}분이나 흔들었더니 팔이 뻐근해. 출근길에 한 번 급하게 멈췄던 거 기억나? 나도 깜짝 놀랐는데 둘 다 괜찮아서 다행이야. 밖이 {outsideTempC}도라 히터 켜줬는데 따뜻했지? 오늘도 수고했어.
- BUTLER: 오늘은 {distanceKm}km를 함께 달렸습니다. 아침 내내 비가 내려 와이퍼가 {wiperMin}분 동안 일했습니다. 출근길에 급제동이 한 번 있었으나 차량에는 이상이 없습니다. 바깥 기온이 {outsideTempC}도여서 히터를 켜 두었습니다. 오늘도 수고 많으셨습니다.
- BLUNT: {distanceKm}km. 비 와서 와이퍼 {wiperMin}분. 급제동 1번, 이상 없음. 밖 {outsideTempC}도, 히터 켰음. 수고.

> 위 DIARY 문장은 데모 데이터에 맞춘 것이다. 실제 구현에서는 조각으로 나눠 조건부로 조합한다: 와이퍼 0분이면 비 문장 생략, 급제동 0회면 "부드럽게 달렸어", 히터 off면 온도 문장만. "출근길", "아침" 같은 시간 표현은 데이터(주행 시각)가 있을 때만 쓴다.

**TIRE_LOW** (position, psi, recommendedPsi)
- FRIEND: 있잖아, {position}발에 바람이 좀 빠졌어. 지금 {psi}psi인데 원래는 {recommendedPsi}psi거든. 당장 위험한 건 아니지만 이번 주 안에 바람 좀 넣어줄래? 이대로 두면 연비도 떨어지고 타이어도 빨리 닳아.
- BUTLER: {position} 바퀴 공기압이 {psi}psi로, 권장치 {recommendedPsi}psi보다 낮습니다. 즉시 위험한 수준은 아니지만 이번 주 안에 점검을 권해 드립니다. 공기압이 낮으면 연비와 타이어 수명이 나빠집니다.
- BLUNT: {position} 타이어 {psi}psi. 권장 {recommendedPsi}. 이번 주 안에 넣어.
- 3칸: "이번 주 안에" / "타이어 공기압 경고 (TPMS)" / "공기 보충, 계속 빠지면 펑크 점검"

**SAFETY_WARNING: 엔진 경고등**
- FRIEND: 나 속이 좀 이상해. 엔진 경고등이 켜졌어. 지금 바로 멈춰야 할 정도는 아니지만, 며칠 안에 정비소에서 봐줬으면 좋겠어. 혹시 불이 깜빡이기 시작하면 그땐 바로 안전한 곳에 세워줘.
- BUTLER: 엔진 경고등이 켜졌습니다. 즉시 정차할 상황은 아니지만, 가급적 빠른 시일 안에 정비소 점검을 권해 드립니다. 경고등이 깜빡이면 안전한 곳에 바로 정차해 주십시오.
- BLUNT: 엔진 경고등 켜짐. 며칠 안에 정비소. 깜빡이면 바로 세워.
- 3칸: "빠른 시일 안에" / "엔진·배기 계통 이상 감지" / "정비소 점검, 깜빡이면 즉시 정차"
- Safety API 경고 코드 → (템플릿, 3칸) 매핑은 `docs/decisions.md`에. 모르는 코드는 "경고등이 켜졌어요. 차량 설명서를 확인하고 정비소에 문의해 주세요"로 처리.

**LOW_FUEL** (fuelPercent, rangeKm)
- FRIEND: 배고파… 기름이 {fuelPercent}%밖에 안 남았어. 이대로면 {rangeKm}km쯤 더 갈 수 있어. 가까운 주유소 들렀다 갈래?
- BUTLER: 연료가 {fuelPercent}% 남았습니다. 약 {rangeKm}km 주행할 수 있습니다. 가까운 주유소를 경유지로 추가해 드릴까요?
- BLUNT: 기름 {fuelPercent}%. {rangeKm}km 남음. 주유소 들르자.
- 3칸: "오늘 안에" / "연료 {fuelPercent}% · 약 {rangeKm}km 주행 가능" / "가까운 주유소에서 주유"

**ENGINE_OIL** (kmSinceOilChange)
- FRIEND: 엔진오일 갈 때가 됐어. 지난번에 갈고 {kmSinceOilChange}km나 달렸거든. 이번 달 안에 정비소 한번 같이 가자!
- BUTLER: 엔진오일 교체 시기가 되었습니다. 지난 교체 후 {kmSinceOilChange}km를 주행했습니다. 이번 달 안에 교체를 권해 드립니다.
- BLUNT: 엔진오일 교체 시기. {kmSinceOilChange}km 탔음. 이번 달 안에.
- 3칸: "이번 달 안에" / "엔진오일 교체 시기 (지난 교체 후 {kmSinceOilChange}km)" / "정비소에서 엔진오일·필터 교체"
- 교체 기준 거리는 차종 설명서 기준으로 팀이 검증해서 `docs/decisions.md`에 적는다.

**VOICE_PROMPT** (주행 중 질문, 짧게)
- 연료 부족: 친구 "기름이 {fuelPercent}% 남았어. 가까운 주유소 경유지로 넣을까?" / 집사 "연료가 {fuelPercent}% 남았습니다. 가까운 주유소를 경유지로 추가할까요?" / 무뚝뚝 "기름 {fuelPercent}%. 주유소 넣을까?"
- 정비 경고: 친구 "{eventTitle}! 주차하면 가까운 정비소 보여줄까?" / 집사 "{eventTitle} 안내입니다. 주차 후 가까운 정비소를 보여 드릴까요?" / 무뚝뚝 "{eventTitle}. 주차하면 정비소 볼래?"

**WAYPOINT_ADDED** (place)
- FRIEND: {place:을/를} 경유지로 넣었어. 안내 따라가면 돼!
- BUTLER: {place:을/를} 경유지로 추가했습니다. 안내를 따라 주십시오.
- BLUNT: {place} 경유지 추가. 따라가.

**MILESTONE_ODO** (odometerKm, seoulBusanTimes = odometerKm / 400 반올림)
- FRIEND: 우리 오늘로 딱 {odometerKm}km 같이 달렸어! 서울에서 부산까지 {seoulBusanTimes}번 간 거리래. 그동안 무사히 데려와줘서 고마워. 다음 1만 km도 잘 부탁해!
- BUTLER: 오늘로 누적 주행 {odometerKm}km를 달성했습니다. 서울에서 부산까지 {seoulBusanTimes}번을 달린 거리입니다. 그동안 안전하게 운전해 주셔서 감사합니다. 이참에 정기 점검 일정도 한번 확인해 보시길 권해 드립니다.
- BLUNT: {odometerKm}km 찍었다. 서울-부산 {seoulBusanTimes}번. 점검 한번 받자.

**BIRTHDAY** (odometerKm, parkings, longestDayKm)
- FRIEND: 오늘 내 생일이야! 1년 전 오늘 우리 처음 만났잖아. 그동안 {odometerKm}km를 같이 달렸고, 주차는 {parkings}번 했어. 제일 멀리 간 날은 하루에 {longestDayKm}km였는데 그날 진짜 신났어. 앞으로도 잘 부탁해!
- BUTLER: 오늘은 출고 1주년입니다. 지난 1년 동안 {odometerKm}km를 함께 달렸고, {parkings}번 주차했습니다. 하루 최장 주행은 {longestDayKm}km였습니다. 앞으로도 안전한 동행이 되겠습니다.
- BLUNT: 출고 1년. {odometerKm}km, 주차 {parkings}번. 최장 하루 {longestDayKm}km. 내년에도 잘 굴러갈게.

**WEEKLY** (days, distanceKm, rainyDays, hardBrakes, hardBrakesLastWeek, tireState)
- FRIEND: 이번 주엔 {days}일 동안 {distanceKm}km 달렸어. 비 온 날이 {rainyDays}일이라 와이퍼가 고생 좀 했지. 급제동은 {hardBrakes}번, 지난주({hardBrakesLastWeek}번)보다 줄었어. 운전이 점점 부드러워지는 중! + 꼬리말
- BUTLER: 이번 주는 {days}일 동안 {distanceKm}km를 달렸습니다. 비가 온 날은 {rainyDays}일이었습니다. 급제동은 {hardBrakes}회로, 지난주 {hardBrakesLastWeek}회보다 줄었습니다. + 꼬리말
- BLUNT: 이번 주 {days}일, {distanceKm}km. 비 {rainyDays}일. 급제동 {hardBrakes}번, 지난주보다 줄었음. + 꼬리말
- 꼬리말: 공기압 낮음 / 예약함 / 정상(없음) × 말투. 급제동이 늘었으면 "늘었어" 문장으로 분기.

**BOOKED** (shop, slot)
- FRIEND: 고마워! {slot}에 {shop} 가는 거지? 그때까지 살살 달려줘.
- BUTLER: {shop}에 {slot} 예약 요청을 보냈습니다. 감사합니다.
- BLUNT: {slot} {shop}. 접수.

**주행 중 안내**
- FRIEND: 운전 중엔 말로만 할게. 주차하면 오늘 얘기 들려줄게!
- BUTLER: 주행 중에는 음성으로만 안내드리겠습니다. 주차 후 오늘의 일기를 들려드리겠습니다.
- BLUNT: 운전 중. 말로만 함. 일기는 주차 후.

## 부록 B. Fake 데이터 (시연용)

PR #3의 `FakeData.kt` 값을 그대로 쓴다. 요약:

```json
{
  "today": { "distanceKm": 37.2, "driveMin": 74, "trips": 2, "wiperMin": 41, "hardBrakes": 1, "outsideTempC": 11, "heaterOn": true },
  "odometerKm": 9982,
  "tireLow": { "position": "왼쪽 뒤", "psi": 28, "recommendedPsi": 36 },
  "fuelLow": { "fuelPercent": 10, "rangeKm": 45 },
  "engineOil": { "kmSinceOilChange": 9800 },
  "week": { "days": 5, "distanceKm": 182.4, "rainyDays": 2, "hardBrakes": 3, "hardBrakesLastWeek": 5 },
  "birthday": { "parkings": 612, "longestDayKm": 412 },
  "shops": ["도움카센터", "한빛자동차정비", "바른타이어"],
  "gasStations": ["새싹주유소", "한길셀프주유소", "푸른에너지"]
}
```
정비소와 주유소는 지어낸 이름이다. 화면에 "시연용" 표시를 남긴다.

## 부록 C. LLM 프롬프트 (일기·기념일·주간 일기 전용)

```
너는 차량용 앱 '붕붕일기'의 마스코트로, 차가 1인칭으로 운전자에게 말하는 문장을 쓴다.
마스코트: {mascotName} - {mascotPersona}
닉네임: {nickname}
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

1. `PROMPT.md와 CLAUDE.md를 전부 읽고 Phase 0만 해. 기능 코드는 쓰지 말고, 끝나면 docs/api-findings.md를 보여주고 멈춰.`
2. `Phase 1을 해. PR #3 코드를 고쳐서 첫 실행 흐름을 3절대로 바꾸고, 부엉이 + "킹카"로 끝까지 진행한 스크린샷을 보여줘.`
3. `Phase 2를 해. FakeData.message()를 템플릿 엔진으로 바꾸고, 시뮬레이터로 시연 1~7단계가 되는지 보여줘.`
4. `Phase 3을 해. Phase 0 표에서 "됨"인 API만 실제로 연결하고, 나머지는 Fake로 남겨.`
5. `Phase 4, 5를 해. LLM 경로는 Phase 0 결과대로 정하고, 끊겨도 템플릿으로 동작하는지 테스트로 보여줘.`
6. `server 폴더에 Phase 6을 해. 테이블과 API 목록은 PROMPT.md 그대로 따르고, Testcontainers 테스트까지 통과시켜.`
7. `Phase 7을 해. 카카오 조건부터 공식 문서로 확인해서 docs/api-findings.md에 적고, 그다음 구현해.`
8. `Phase 8을 하고, demo-script.md 순서대로 처음부터 끝까지 한 번 돌려본 결과를 보고해.`
