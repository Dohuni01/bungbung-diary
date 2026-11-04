# 설계 — AI 음성 비서 (실시간 상태 관찰 → 음성 안내)

> 작성: 2026-11-04 · 상태: **설계만** (구현은 다음 단계)
> 관련: [구현현황.md](구현현황.md) · [decisions.md](decisions.md) · `domain/SafetyAlert.kt`

## 0. 한 줄 요약

붕붕이가 **실시간 차량 상태를 스스로 관찰**하다가, 위험·권유 상황이 생기면 **먼저 음성으로** 알려준다.
음성은 설정에서 **ON/OFF** 하고, 위급만 들을지도 고를 수 있다.
대표 예시: **타이어 공기압이 빠르게 떨어지면(펑크) 실시간으로 감지해 즉시 음성 경고.**

---

## 1. AI를 어떤 방식으로? → 규칙기반 + LLM 혼합 (권장)

| 영역 | 방식 | 이유 |
|---|---|---|
| **안전 경고·대처 문구** | **규칙기반(템플릿)** | 틀린 정비 안내는 사고로 이어짐. `decisions.md` 결정("경고엔 LLM 안 씀")과 일치. 100% 예측·오프라인 가능 |
| **감지(펑크·졸음 등)** | **규칙기반 + 변화율 계산** | 센서 수치의 패턴은 수학으로 확정. 재현·검증 가능 |
| **일상 말·하루 요약·잡담** | **LLM(선택, 서버 `/api/narrate`)** | "비서다운" 자연스러움은 여기서. 틀려도 안전과 무관 |

> **핵심 원칙**: "진단은 차에 맡기고(텔테일 우선), 안전 멘트는 근거 있는 템플릿으로, 성격·잡담만 AI로."
> LLM이 꺼져 있거나 느리면(3초 초과) 템플릿으로 **폴백** — 안전 기능은 네트워크와 무관하게 항상 동작.

이 문서의 나머지는 이 하이브리드를 전제로 한다. (완전 규칙기반으로 시작해도 되고, LLM은 Phase 4에서 얹을 수 있음.)

---

## 2. 사용자 경험 (예시 흐름 — 타이어 펑크)

```
주행 중, 왼쪽 뒤 타이어에 못이 박힘
        │
        ▼
[관찰] 1초마다 바퀴별 공기압 수집 (36 → 33 → 29 → 24 psi …)
        │
        ▼
[감지] 한 바퀴만 분당 3psi 이상, 10초 지속 하락 → "급감(펑크 의심)"
        │  (TPMS 텔테일이 먼저 뜨면 그걸 우선 = 확정)
        ▼
[판단] 위험도 🔴 CRITICAL · 같은 경고는 1회만 · 음성 ON?
        │
        ▼
[음성] "왼쪽 뒤 타이어 바람이 빠르게 빠지고 있어! 비상등 켜고 천천히 세워줘."
[화면] 주행 중 큰 경고 + '가까운 정비소 보기'
        │
        ▼
정차(기어 P)하면 🔴만 1회 더 안내 → 해제될 때까지 반복 안 함
```

---

## 3. 설정 (음성 ON/OFF)

### UI — `SettingsScreen.kt`에 InfoCard 추가 (기존 "말투" 카드와 동일 패턴)

```
┌ 음성 안내 ─────────────────────────┐
│ 붕붕이가 위험할 때 소리로 알려줘요   │
│ 음성 안내                   [ON/OFF] │  ← Switch
│ 위급한 것만 말하기          [ON/OFF] │  ← Switch (음성 ON일 때만 활성)
└───────────────────────────────────┘
```

### 저장 — `CarProfileEntity`에 컬럼 추가 (Room 마이그레이션 필요)

```kotlin
@ColumnInfo(name = "voice_enabled",      defaultValue = "1") val voiceEnabled: Boolean = true,
@ColumnInfo(name = "voice_critical_only", defaultValue = "0") val voiceCriticalOnly: Boolean = false,
```
→ `AppState`에 `voiceEnabled`, `voiceCriticalOnly` 추가하고, `BungbungApp`의 프로필 로드/저장에 연결(기존 `diaryHour` 저장 흐름과 동일).

### 음성 발화 범위 (결정: **위급 + 권유**)

| 설정 | 자동 음성 대상 |
|---|---|
| 음성 OFF | 없음 (화면만) |
| 음성 ON + 위급만 | 🔴 CRITICAL |
| **음성 ON (기본)** | 🔴 + 🟡 + 🟢 **안전 알림 전부** (일상 잡담은 화면만) |

> 일상 대사는 자동 발화하지 않음. 사용자가 '들려줘'를 누르면 그때 읽음(기존 동작 유지).

---

## 4. 아키텍처

```
PleosVehicleDataSource (SDK 리스너)
        │ vehicleStateFlow(): Flow<VehicleState>   ← 이미 있음
        ▼
┌─────────────────────────────────────────────┐
│  VoiceAssistant (신규, 핵심 오케스트레이터)    │
│                                               │
│  1) StateHistory   — 수치 이력 버퍼(타임스탬프)│
│  2) ChangeDetector — 변화율 감지(펑크 등)      │
│  3) AlertEngine    — 위험도·우선순위 결정       │
│       = 기존 AppState.autoSafetyAlert 확장      │
│  4) SpeakPolicy    — 디바운스/중복/쿨다운/설정  │
│  5) Narrator       — 문구 생성                  │
│        ├ 안전: SafetyAlert 템플릿(규칙)         │
│        └ 일상: LLM /api/narrate (선택)          │
└─────────────────────────────────────────────┘
        │ speak(text)
        ▼
PleosVoiceManager.speak()   ← 이미 있음 (TTS)
```

- **위치**: `data/voice/VoiceAssistant.kt`(신규). `BungbungApp`의 `vehicleStateFlow().collect{}` 안에서 구동(이미 코루틴 수집 중).
- 기존 `CarTalkGenerator`/`SafetyAlert`/`autoSafetyAlert`를 재사용 — 화면 문구와 음성 문구를 **한 소스**로.

---

## 5. 핵심 설계 — 타이어 공기압 급감(펑크) 실시간 감지

### 입력
- 바퀴별 공기압(psi)이 `TirePressureListener`로 들어옴 → `VehicleState.tires` (이미 동작, 권한 3단계로 수신됨).

### 이력 버퍼 `StateHistory`
- 바퀴별로 `(timestamp, psi)` 샘플을 **슬라이딩 윈도우**(예: 최근 120초)로 저장.
- 메모리만 사용(영속화 불필요). 느린 누수(며칠)는 별도로 **일 1회 스냅샷**을 DB에 저장해 비교.

### 판정 규칙 (초기값 — `decisions.md` 임계값 표에 등록해 팀이 조정)

| 구분 | 조건 | 결과 |
|---|---|---|
| **펑크(급감)** | 한 바퀴 하락률 **≥ 3 psi/분**이 **≥ 10초** 지속 | 🔴 `TIRE_BLOWOUT` |
| **느린 누수** | 하루 스냅샷 비교 시 **≥ 2 psi/일** 하락이 2~3일 지속 | 🟡 `TIRE_SLOW_LEAK` |
| **차가 띄운 경고** | `TPMS` 텔테일 ON | 🔴 즉시 확정(우선) |

### 오탐 방지
- **추운 날/전체 하락**: 네 바퀴가 비슷하게 서서히 내려가면 **기온 영향** → 경고 안 함. **한 바퀴만** 두드러지게 급감할 때만 펑크로 본다.
- **노이즈**: 단일 샘플 급변은 무시, **지속성(연속 N초)** 확인.
- **출처 우선순위**: 차가 띄운 `TPMS` 텔테일이 있으면 그대로 사용("확정"), 없고 우리가 추정한 것이면 어투를 **"~같아"**(추정)로.

### 확장
- 같은 "변화율 감지" 틀을 **연료 급감(누유), 배터리 급락** 등에도 재사용 가능하게 일반화(`ChangeDetector<T>`).

---

## 6. 발화 정책 `SpeakPolicy` (오탐·반복 방지)

- **디바운스**: 상태가 **몇 초 이상 지속**될 때만 발화(순간 튐 무시).
- **중복 억제**: 같은 알림은 **해제될 때까지 1회만**. 단 🔴은 **정차 시 1회 더**.
- **쿨다운**: 연속 발화 사이 최소 간격(예: 20초). TTS가 말하는 중이면 큐에 넣거나 건너뜀.
- **우선순위**: 🔴 > 🟡 > 🟢. 동시 발생 시 가장 위험한 것 먼저. (이미 `autoSafetyAlert`가 위험도 순으로 반환)
- **설정 반영**: `voiceEnabled=false`면 전부 무음. `voiceCriticalOnly=true`면 🔴만.
- **조용 조건(선택)**: 주행 시작 직후 수 초, 또는 통화 중 등은 보류(후순위).

---

## 7. 기존 코드 연결점 (구현 시 건드릴 곳)

| 할 일 | 파일 |
|---|---|
| 음성 비서 오케스트레이터 신규 | `data/voice/VoiceAssistant.kt` (신규) |
| 변화율 감지/이력 | `data/voice/ChangeDetector.kt`, `StateHistory.kt` (신규) |
| 상태 수집 루프에 비서 연결 | `ui/BungbungApp.kt` (`vehicleStateFlow().collect`) |
| 음성 설정 상태 | `ui/AppState.kt` (`voiceEnabled`, `voiceCriticalOnly`) |
| 설정 UI | `ui/settings/SettingsScreen.kt` (InfoCard + Switch) |
| 설정 영속화(마이그레이션) | `data/local/CarProfileEntity.kt`, `BungbungDatabase.kt`(version+Migration) |
| 안전 문구 재사용 | `domain/SafetyAlert.kt`, `data/text/CarTalkGenerator.kt` |
| TTS 출력 | `data/voice/PleosVoiceManager.kt` (`speak()` 그대로 사용) |
| 임계값 기록 | `docs/decisions.md` |

---

## 8. 구현 단계 (다음에 이 순서로)

- **Phase 1 — 음성 설정 + 기존 알림 자동 발화**
  설정 ON/OFF·위급만 추가, `autoSafetyAlert`가 바뀔 때 `SpeakPolicy` 거쳐 TTS. (변화율 감지 없이, 텔테일 기반만으로 먼저)
- **Phase 2 — 변화율 감지(펑크)**
  `StateHistory` + `ChangeDetector`로 타이어 급감 감지. 오탐 방지 규칙 적용. 임계값 `decisions.md` 등록.
- **Phase 3 — 발화 품질 고도화**
  디바운스·쿨다운·중복억제·🔴 재알림 완성. 느린 누수(일 스냅샷) 추가.
- **Phase 4 — LLM 일상 말(선택)**
  일상/요약 대사만 `/api/narrate`로 자연스럽게, 실패 시 템플릿 폴백. 안전 경고는 절대 LLM 미경유.

---

## 9. 열린 결정 / 리스크

- **임계값**(psi/분, 지속 초, 쿨다운)은 실주행 데이터로 조정 필요 → `decisions.md` 임계값 표.
- **에뮬레이터 검증**: 타이어 공기압은 권한 3단계로 수신되나, Virtual Console에서 psi를 "빠르게" 바꿀 수 있는지 확인 필요. 안 되면 `TPMS` 텔테일로 데모하고 변화율은 단위테스트로 검증.
- **TTS 가용성**: `PleosVoiceManager`는 초기화 실패 시 조용히 skip. 음성이 안 될 환경이면 화면 경고가 항상 대체.
- **동시 다발 경고**: 우선순위 1건만 말하고 나머지는 화면. 큐 과다 방지.
- **LLM 안전 경계**: 안전 문구는 어떤 경우에도 LLM을 거치지 않는다(오안내 위험). 이 경계는 코드 구조로 강제(Narrator에서 분기).
