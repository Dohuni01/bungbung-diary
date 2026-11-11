# 목적지 한마디 설계 (붕붕일기)

> 작성일: 2026-11-11
> 상태: 설계 확정 (사용자 승인), 구현 진행
> 출처: `C:\hyundai`(향하다)의 목적지 분류·브리핑 설계를 붕붕일기에 맞게 응용

## 1. 한 줄 설명

네비에 목적지를 정하면 붕붕이가 "어디로, 왜 가는지"를 이해하고,
그 이동의 의미에 맞는 한마디(철학적·공감)와 표정으로 반응한다.

> 예: 목적지 "서울아산병원 장례식장" → 붕붕이(차분한 표정):
> "오늘은 조용히 갈게. 누군가를 배웅하는 길은 말보다 마음이 먼저니까."

## 2. 확정된 결정 (2026-11-11 사용자 선택)

| # | 결정 | 내용 |
| --- | --- | --- |
| 1 | 목적지 입력 | **Pleos NaviHelper 실제 연동** + 디버그 패널 입력(시연 안전망) |
| 2 | 문장 생성 | **Pleos LLM 생성 + 템플릿 fallback.** 단, 민감 상황(장례식장·응급실·병원)은 LLM 금지, 사람이 쓴 고정 문장만 |
| 3 | 연출 범위 | **말 + 표정(Mood)만.** 도착 전 단계 안내(ETA 기반)는 범위 밖 |

## 3. 동작 흐름

```
네비 목적지 설정
  → NaviHelper onRoutingCompleted / onDestinationChanged → getDestinationInfo()
  → onDestinationInfo(info) → info.name (이름만 사용. 좌표·주소는 쓰지 않음)
  → DestinationClassifier (규칙 기반, hyundai 포팅)
      FUNERAL / HOSPITAL_ER / HOSPITAL / AIRPORT / WEDDING / EXAM_INTERVIEW / NONE
  → DestinationThoughtService
      민감(FUNERAL·HOSPITAL_ER·HOSPITAL) → 템플릿 고정 문장 (LLM 호출 자체가 불가능한 구조)
      비민감(AIRPORT·WEDDING·EXAM_INTERVIEW·NONE) → LLM 생성(2.5초 타임아웃) → 검증 실패 시 템플릿
  → 화면 말풍선 + 표정(Mood) + TTS 1회
목적지 취소(onRouteCancelled) → 한마디 카드 제거, 평소 표정 복귀
```

## 4. 구성 요소

| 파일 | 역할 |
| --- | --- |
| `domain/destination/DestinationCategory.kt` | 카테고리 enum. `sensitive`, `mood` 속성 |
| `domain/destination/DestinationClassifier.kt` | 정규화 + 포함/제외 키워드 + 우선순위 (hyundai `RuleClassifier` 포팅) |
| `domain/destination/DestinationThoughtService.kt` | 한마디 결정. 민감=템플릿만, 비민감=LLM+검증+fallback |
| `data/text/DestinationTalkGenerator.kt` | 말투 3종 × 카테고리 템플릿 (fallback 겸용) |
| `data/llm/LlmClient.kt` | `suspend fun generate(prompt): String?` 인터페이스 |
| `data/llm/PleosLlmClient.kt` | Pleos LLM SDK 래핑 (SERVER 모드, registerApp, 실패 시 null) |
| `data/navi/PleosNaviDataSource.kt` | NaviHelper 래핑. 목적지 이름 콜백. 초기화 실패 시 조용히 skip |
| UI | `CarEvent.DESTINATION_SET`, `AppState.destination*`, TodayScreen 카드, DebugPanel 목적지 섹션 |

## 5. 안전 원칙 (테스트로 강제)

1. 민감 카테고리에서는 `LlmClient`가 호출되지 않는다 (테스트: 호출 기록 Fake로 보장).
2. LLM 출력 검증: 빈 문자열 / 80자 초과 / 목적지 이름에 없는 숫자 포함 → 버리고 템플릿.
3. LLM 타임아웃 2.5초. 네트워크 없어도 템플릿으로 100% 동작.
4. LLM에 보내는 것: 목적지 이름, 카테고리, 말투, 닉네임뿐. 좌표·주소·시각 미전송.
5. Client ID/Secret은 `local.properties`(`PLEOS_CLIENT_ID`, `PLEOS_CLIENT_SECRET`) → BuildConfig. 커밋 금지.

## 6. 확인된 Pleos API (실제 AAR에서 javap으로 확인, 2026-11-11)

- `ai.pleos.playground:NaviHelper:3.0.5` (+ 전이 의존성 `navi-data:3.0.4`)
  - `NaviHelper(context)`, `initialize(onConnected: () -> Unit, onFailed: (Throwable) -> Unit)`
  - `addListener/removeListener(NaviHelperEventListener)`, `getDestinationInfo()`, `cancelRoute()`, `release()`
  - 콜백(default 메서드): `onRoutingCompleted()`, `onDestinationChanged()`, `onDestinationInfo(DestinationInfo)`, `onRouteCancelled()`, `onError(NaviErrorCode)`
  - `DestinationInfo.name: String` (그 외 address, 좌표, remainDistance/Time 등)
  - 권한: `pleos.car.permission.NAVI_ROUTE`
- `ai.pleos.playground:LLM:2.1.3.2`
  - `LLM(context, Mode.SERVER|ON_DEVICE)`, `initialize()`, `registerApp(id, secret, OnServerConnectionListener)`
  - `generateContent(prompt, ResultListener)` — `onResponse(String, Boolean)`, `onError(String)`
  - 권한: `pleos.car.permission.LLM_SERVICE`

## 7. 분류 키워드 (hyundai PROMPT 8절 포팅)

우선순위: FUNERAL > HOSPITAL_ER > AIRPORT > WEDDING > EXAM_INTERVIEW > HOSPITAL.
제외어: 동물병원·의원·치과(병원), 공항동·역·리무진(공항), 드레스·스튜디오(웨딩) 등.
애매하면 NONE — 일반 목적지용 잔잔한 한마디로 처리.

## 8. 범위 밖 (YAGNI)

- 도착 전 ETA 단계 안내, 확인 질문(ASKING 상태 기계), 차량 제어(음악·공조) — 향하다 고유 기능
- FUNERAL_RETURN(직전 여정 기억) — 여정 기록이 필요해 제외
- 장소 기억(place_correction), LLM 분류 — 규칙 분류만 사용

## 9. 코드 밖 남은 작업 (사람이 할 일)

- [ ] My Project에서 NaviHelper(`NAVI_ROUTE`)·LLM(`LLM_SERVICE`) SDK 사용 신청·승인
- [ ] `local.properties`에 `PLEOS_CLIENT_ID`, `PLEOS_CLIENT_SECRET` 입력 (LLM SERVER 모드용)
- [ ] 에뮬레이터에서 내비 목적지 설정 시 이벤트 수신 실동작 확인
- [ ] 민감 상황 고정 문장 팀 검증 (장례식장·병원 표현)
