# FakeCar 시나리오 설계

> 작성일: 2026-11-02  
> 목적: 실제 차 없이 개발·테스트할 수 있도록 차량 상태를 Kotlin 오브젝트로 미리 정의한다.

---

## 1. 목적과 범위

- **대상**: Phase 1 ~ Phase 2 개발 중 Pleos 에뮬레이터 없이 기능을 확인해야 할 때
- **사용 방법**: Debug 패널(릴리스 빌드 숨김)에서 시나리오를 선택 → `FakeVehicleDataSource`가 해당 시나리오의 값을 반환
- **범위 밖**: 실시간 값 변경, 시간 경과 시뮬레이션 — 시나리오는 "이 순간 차 상태"를 찍은 스냅샷

---

## 2. 데이터 구조

### 2-1. CarScenario (최상위)

```kotlin
data class CarScenario(
    val name: String,           // Debug 패널에 표시되는 이름
    val currentState: VehicleState,
    val todaySummary: DailySummary,
    val carInfo: CarInfo
)
```

### 2-2. VehicleState — 현재 차 상태

```kotlin
data class VehicleState(
    val gear: Gear,                         // P / R / N / D
    val speedKmh: Float,
    val odometerKm: Float,
    val outsideTempC: Float,
    val energy: EnergyState,                // 연료 또는 배터리
    val hvac: HvacState,
    val tires: TireState,                   // 4바퀴
    val safetyWarnings: List<SafetyWarning> // 빈 리스트 = 경고 없음
)

data class EnergyState(val percent: Float, val isEv: Boolean)
data class HvacState(val setTempC: Float, val heaterOn: Boolean)

data class TireState(
    val frontLeft: TireData,
    val frontRight: TireData,
    val rearLeft: TireData,
    val rearRight: TireData
)
data class TireData(val psi: Int, val recommendedPsi: Int, val warning: Boolean)

data class SafetyWarning(val code: String, val name: String)
```

### 2-3. DailySummary — 오늘 누적 주행 데이터

PROMPT.md 6절 공통 데이터 계약과 동일한 구조를 사용한다.

```kotlin
data class DailySummary(
    val date: LocalDate,
    val distanceKm: Float,
    val driveMin: Int,
    val trips: Int,
    val wiperMin: Int,
    val hardBrakes: Int,
    val outsideTempC: Float,
    val heaterOn: Boolean,
    val odometerKm: Float
)
```

### 2-4. CarInfo — 차 기본 정보

```kotlin
data class CarInfo(val manufacturer: String, val modelYear: Int)
```

---

## 3. 미리 정의된 시나리오 목록

`data/vehicle/fake/Scenarios.kt`에 싱글턴 오브젝트로 선언한다.

| 상수명 | 상황 | 핵심 값 |
|---|---|---|
| `DiaryDay` | 주행 후 P단 — 일기 트리거 기준 | 기어 P, 37.2km 주행, 와이퍼 41분, 급제동 1회 |
| `TireLow` | 타이어 경고 발생 | 뒷왼쪽 타이어 28psi (권장 36), `warning = true` |
| `EngineWarning` | 엔진 경고등 켜짐 | `safetyWarnings = [SafetyWarning("ENGINE", "엔진 경고등")]` |
| `Driving` | 주행 중 (화면 제한 확인) | 기어 D, 속도 60km/h |
| `Milestone10k` | 1만 km 돌파 | `odometerKm = 10000f` |
| `Birthday` | 출고 1주년 | 차 상태는 `Normal`과 동일. Debug 패널에서 `deliveryDate = 오늘`로 덮어써서 TriggerEngine이 기념일을 감지하게 함 |
| `Normal` | 이벤트 없는 평범한 주차 | 기어 P, 단거리 주행, 경고 없음, 타이어 정상 |

---

## 4. FakeVehicleDataSource 연결 방식

```
Debug 패널 → ScenarioHolder.current = Scenarios.TireLow
                    ↓
FakeVehicleDataSource.getTires() → current.currentState.tires 반환
FakeVehicleDataSource.getSafetyWarnings() → current.currentState.safetyWarnings 반환
...
```

- `ScenarioHolder`는 앱 내 싱글턴(또는 Hilt 싱글턴). `StateFlow<CarScenario>`를 노출해 UI가 변경을 관찰할 수 있게 한다.
- `FakeVehicleDataSource`는 `VehicleDataSource` 인터페이스를 구현한다. 실제 구현(`PleosVehicleDataSource`)과 교체 가능.
- 릴리스 빌드에서 Debug 패널을 숨기면 `ScenarioHolder`는 `DiaryDay`로 고정된다.

---

## 5. 파일 위치

```
car-app/app/src/main/java/com/bungbung/diary/
├── data/vehicle/
│   ├── VehicleDataSource.kt        인터페이스
│   ├── fake/
│   │   ├── Scenarios.kt            7개 시나리오 오브젝트
│   │   ├── ScenarioHolder.kt       현재 시나리오 상태 보유
│   │   └── FakeVehicleDataSource.kt  인터페이스 구현
│   └── pleos/
│       └── PleosVehicleDataSource.kt  Phase 2에서 구현
```

---

## 6. 완료 기준

- `FakeVehicleDataSource`가 `VehicleDataSource` 인터페이스를 구현한다.
- Debug 패널에서 시나리오를 바꾸면 Today 화면의 값이 즉시 바뀐다.
- `PleosVehicleDataSource`로 교체해도 나머지 코드가 변경 없이 동작한다.
