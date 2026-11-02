# Phase 1-B: 온보딩 + CarProfile + VehicleDataSource 구현 플랜

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 닉네임·말투·출고일을 Room에 저장하는 온보딩 7단계를 완성하고, `VehicleDataSource` 인터페이스로 Fake 데이터 레이어를 구조화한다.

**Architecture:** `VehicleDataSource` 인터페이스를 새로 만들고 `FakeVehicleDataSource`가 `ScenarioHolder`를 래핑해 구현한다. `CarProfileEntity`(Room)에 닉네임·말투·출고일·온보딩 완료 시각을 저장한다. `BungbungApp`이 시작 시 프로필을 읽어 온보딩 완료면 MainScreen으로 바로 이동한다.

**Tech Stack:** Kotlin, Jetpack Compose, Room 2.6.1, KSP 2.0.21-1.0.28, JUnit 5 (로컬 단위 테스트), Android Automotive OS

---

## 파일 구조 (신규/변경)

```
car-app/app/src/main/java/com/bungbung/diary/
├── data/
│   ├── vehicle/
│   │   ├── VehicleDataSource.kt              ← NEW  인터페이스
│   │   ├── fake/
│   │   │   ├── FakeVehicleDataSource.kt       ← NEW  ScenarioHolder 래핑
│   │   │   ├── ScenarioHolder.kt              (기존, 수정 없음)
│   │   │   └── Scenarios.kt                  (기존, 수정 없음)
│   │   └── pleos/
│   │       └── PleosVehicleDataSource.kt      ← NEW  스텁만 (Phase 2용)
│   └── local/
│       ├── CarProfileEntity.kt               ← NEW
│       ├── CarProfileDao.kt                  ← NEW
│       ├── CarProfileRepository.kt           ← NEW
│       ├── BungbungDatabase.kt               ← MODIFY  v2 마이그레이션
│       └── DiaryEntryEntity.kt               (기존, 수정 없음)
├── util/
│   └── JosaUtil.kt                           ← NEW  이/가·을/를·은/는·아/야
├── ui/
│   ├── AppState.kt                           ← MODIFY  deliveryDate 추가
│   ├── BungbungApp.kt                        ← MODIFY  시작 시 프로필 로드, 온보딩 완료 저장
│   └── onboarding/
│       └── OnboardingScreen.kt               ← MODIFY  FAMILY 제거, HealthCheckStep·DeliveryDateStep 개선
└── BungbungApplication.kt                   ← MODIFY  CarProfileRepository.init()

car-app/app/src/test/java/com/bungbung/diary/
└── util/
    └── JosaUtilTest.kt                       ← NEW
```

---

## Task 1: VehicleDataSource 인터페이스 + FakeVehicleDataSource + PleosVehicleDataSource 스텁

**Files:**
- Create: `car-app/app/src/main/java/com/bungbung/diary/data/vehicle/VehicleDataSource.kt`
- Create: `car-app/app/src/main/java/com/bungbung/diary/data/vehicle/fake/FakeVehicleDataSource.kt`
- Create: `car-app/app/src/main/java/com/bungbung/diary/data/vehicle/pleos/PleosVehicleDataSource.kt`

**배경:** `VehicleDataSource` 인터페이스를 Pleos SDK 도메인 구조를 고려해 설계한다. Fake와 Pleos 두 구현이 동일한 인터페이스를 공유하므로 Phase 2에서 구현체만 교체하면 나머지 코드는 변경 없이 동작한다.

- [ ] **Step 1: `VehicleDataSource.kt` 작성**

```kotlin
package com.bungbung.diary.data.vehicle

import com.bungbung.diary.domain.CarInfo
import com.bungbung.diary.domain.DailySummary
import com.bungbung.diary.domain.VehicleState

/**
 * 차량 데이터 소스 인터페이스.
 * Fake 구현 → Phase 1-B (FakeVehicleDataSource)
 * Pleos 구현 → Phase 2 (PleosVehicleDataSource)
 */
interface VehicleDataSource {
    /** 현재 차량 상태 (기어·속도·외기온도·연료·공조·타이어·경고) */
    fun getVehicleState(): VehicleState

    /** 오늘 누적 주행 요약 (주행거리·주행시간·와이퍼 등) */
    fun getDailySummary(): DailySummary

    /** 차량 기본 정보 (제조사·연식) */
    fun getCarInfo(): CarInfo
}
```

- [ ] **Step 2: `FakeVehicleDataSource.kt` 작성**

```kotlin
package com.bungbung.diary.data.vehicle.fake

import com.bungbung.diary.data.vehicle.VehicleDataSource
import com.bungbung.diary.domain.CarInfo
import com.bungbung.diary.domain.DailySummary
import com.bungbung.diary.domain.VehicleState

/**
 * ScenarioHolder.current 를 읽어 VehicleDataSource 계약을 이행한다.
 * Debug 패널에서 시나리오를 바꾸면 이 클래스가 반환하는 값도 즉시 바뀐다.
 */
class FakeVehicleDataSource : VehicleDataSource {
    override fun getVehicleState(): VehicleState = ScenarioHolder.current.currentState
    override fun getDailySummary(): DailySummary = ScenarioHolder.current.todaySummary
    override fun getCarInfo(): CarInfo = ScenarioHolder.current.carInfo
}
```

- [ ] **Step 3: `PleosVehicleDataSource.kt` 스텁 작성 (Phase 2 자리 확보)**

```kotlin
package com.bungbung.diary.data.vehicle.pleos

import com.bungbung.diary.data.vehicle.VehicleDataSource
import com.bungbung.diary.domain.CarInfo
import com.bungbung.diary.domain.DailySummary
import com.bungbung.diary.domain.VehicleState

/**
 * Phase 2에서 실제 Pleos SDK로 구현한다.
 * 지금은 빌드를 통과시키기 위한 스텁만 제공한다.
 *
 * Phase 2 구현 계획:
 *  - getVehicleState(): AAOS CarPropertyManager(기어·속도·외기온도) + Pleos #4(연료/EV) + Pleos #7(타이어·경고)
 *  - getDailySummary(): Pleos #2(주행거리·브레이크), Pleos #11(와이퍼)
 *  - getCarInfo(): Pleos #1(제조사·연식)
 */
class PleosVehicleDataSource : VehicleDataSource {
    override fun getVehicleState(): VehicleState = throw NotImplementedError("Phase 2에서 구현")
    override fun getDailySummary(): DailySummary = throw NotImplementedError("Phase 2에서 구현")
    override fun getCarInfo(): CarInfo = throw NotImplementedError("Phase 2에서 구현")
}
```

- [ ] **Step 4: 빌드 확인**

```powershell
cd C:\bungbung-diary\bungbung-diary\car-app
.\gradlew assembleDebug
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: 커밋**

```powershell
git add car-app/app/src/main/java/com/bungbung/diary/data/vehicle/
git commit -m "feat: VehicleDataSource 인터페이스 + FakeVehicleDataSource + PleosVehicleDataSource 스텁"
```

---

## Task 2: JosaUtil — 한국어 조사 처리

**Files:**
- Create: `car-app/app/src/main/java/com/bungbung/diary/util/JosaUtil.kt`
- Create: `car-app/app/src/test/java/com/bungbung/diary/util/JosaUtilTest.kt`

**배경:** 닉네임이 받침이 있는지에 따라 "이/가", "을/를", "은/는", "아/야"가 달라진다. 유니코드 한글 음절 블록(AC00–D7A3)에서 (코드포인트 - 0xAC00) % 28 == 0이면 받침 없음.

- [ ] **Step 1: 테스트 먼저 작성 (`JosaUtilTest.kt`)**

```kotlin
package com.bungbung.diary.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class JosaUtilTest {

    @Test fun `받침 없는 이름에 이_가 → 가`() = assertEquals("붕붕이가", JosaUtil.ga("붕붕이"))
    @Test fun `받침 있는 이름에 이_가 → 이`() = assertEquals("킹카이", JosaUtil.ga("킹카"))
    @Test fun `받침 없는 이름에 을_를 → 를`() = assertEquals("붕붕이를", JosaUtil.eul("붕붕이"))
    @Test fun `받침 있는 이름에 을_를 → 을`() = assertEquals("킹을", JosaUtil.eul("킹"))
    @Test fun `받침 없는 이름에 은_는 → 는`() = assertEquals("붕붕이는", JosaUtil.eun("붕붕이"))
    @Test fun `받침 있는 이름에 은_는 → 은`() = assertEquals("킹은", JosaUtil.eun("킹"))
    @Test fun `받침 없는 이름에 아_야 → 야`() = assertEquals("붕붕이야", JosaUtil.ah("붕붕이"))
    @Test fun `받침 있는 이름에 아_야 → 아`() = assertEquals("킹아", JosaUtil.ah("킹"))
    @Test fun `이야_야 받침 없음 → 야`() = assertEquals("붕붕이야", JosaUtil.iya("붕붕이"))
    @Test fun `이야_야 받침 있음 → 이야`() = assertEquals("킹이야", JosaUtil.iya("킹"))
    @Test fun `빈 문자열은 받침 없음으로 처리`() = assertEquals("가", JosaUtil.ga(""))
    @Test fun `영어는 받침 없음으로 처리`() = assertEquals("Carが", JosaUtil.ga("Car"))
}
```

- [ ] **Step 2: 테스트 실패 확인**

```powershell
.\gradlew :app:test --tests "com.bungbung.diary.util.JosaUtilTest" 2>&1 | Select-String -Pattern "FAIL|ERROR|passed|failed"
```

Expected: 컴파일 오류 또는 FAIL (JosaUtil 없음)

- [ ] **Step 3: `JosaUtil.kt` 구현**

```kotlin
package com.bungbung.diary.util

object JosaUtil {
    fun ga(word: String): String  = if (hasFinalConsonant(word)) "${word}이" else "${word}가"
    fun eul(word: String): String = if (hasFinalConsonant(word)) "${word}을" else "${word}를"
    fun eun(word: String): String = if (hasFinalConsonant(word)) "${word}은" else "${word}는"
    fun ah(word: String): String  = if (hasFinalConsonant(word)) "${word}아" else "${word}야"
    fun iya(word: String): String = if (hasFinalConsonant(word)) "${word}이야" else "${word}야"

    private fun hasFinalConsonant(word: String): Boolean {
        if (word.isEmpty()) return false
        val last = word.last()
        if (last.code < 0xAC00 || last.code > 0xD7A3) return false
        return (last.code - 0xAC00) % 28 != 0
    }
}
```

- [ ] **Step 4: 테스트 통과 확인**

```powershell
.\gradlew :app:test --tests "com.bungbung.diary.util.JosaUtilTest"
```

Expected: `12 tests completed, 0 failed`

- [ ] **Step 5: 커밋**

```powershell
git add car-app/app/src/main/java/com/bungbung/diary/util/JosaUtil.kt
git add car-app/app/src/test/java/com/bungbung/diary/util/JosaUtilTest.kt
git commit -m "feat: JosaUtil — 한국어 조사 처리 (이/가, 을/를, 은/는, 아/야)"
```

---

## Task 3: CarProfile Room 레이어

**Files:**
- Create: `car-app/app/src/main/java/com/bungbung/diary/data/local/CarProfileEntity.kt`
- Create: `car-app/app/src/main/java/com/bungbung/diary/data/local/CarProfileDao.kt`
- Create: `car-app/app/src/main/java/com/bungbung/diary/data/local/CarProfileRepository.kt`
- Modify: `car-app/app/src/main/java/com/bungbung/diary/data/local/BungbungDatabase.kt`
- Modify: `car-app/app/src/main/java/com/bungbung/diary/BungbungApplication.kt`

**배경:** CarProfile은 앱에 하나만 존재하는 단일 행 테이블이다. PrimaryKey = 1로 고정해 `upsert`로 덮어쓴다. `onboardingCompletedAt`이 null이면 온보딩 미완료, non-null이면 완료.

- [ ] **Step 1: `CarProfileEntity.kt` 작성**

```kotlin
package com.bungbung.diary.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "car_profile")
data class CarProfileEntity(
    @PrimaryKey val id: Int = 1,
    val nickname: String,
    val tone: String,
    @ColumnInfo(name = "delivery_date_iso") val deliveryDateIso: String?,
    @ColumnInfo(name = "onboarding_completed_at") val onboardingCompletedAt: Long?,
)
```

- [ ] **Step 2: `CarProfileDao.kt` 작성**

```kotlin
package com.bungbung.diary.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CarProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: CarProfileEntity)

    @Query("SELECT * FROM car_profile WHERE id = 1 LIMIT 1")
    suspend fun get(): CarProfileEntity?

    @Query("SELECT * FROM car_profile WHERE id = 1 LIMIT 1")
    fun observe(): Flow<CarProfileEntity?>
}
```

- [ ] **Step 3: `CarProfileRepository.kt` 작성**

```kotlin
package com.bungbung.diary.data.local

import kotlinx.coroutines.flow.Flow

object CarProfileRepository {
    private lateinit var dao: CarProfileDao

    fun init(dao: CarProfileDao) { this.dao = dao }

    suspend fun upsert(profile: CarProfileEntity) = dao.upsert(profile)
    suspend fun get(): CarProfileEntity? = dao.get()
    fun observe(): Flow<CarProfileEntity?> = dao.observe()
}
```

- [ ] **Step 4: `BungbungDatabase.kt` 수정 — version 2 마이그레이션 추가**

현재 파일 전체를 아래로 교체한다:

```kotlin
package com.bungbung.diary.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [DiaryEntryEntity::class, CarProfileEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class BungbungDatabase : RoomDatabase() {
    abstract fun diaryEntryDao(): DiaryEntryDao
    abstract fun carProfileDao(): CarProfileDao

    companion object {
        @Volatile private var instance: BungbungDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS car_profile (
                        id INTEGER NOT NULL PRIMARY KEY,
                        nickname TEXT NOT NULL,
                        tone TEXT NOT NULL,
                        delivery_date_iso TEXT,
                        onboarding_completed_at INTEGER
                    )"""
                )
            }
        }

        fun getInstance(context: Context): BungbungDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    BungbungDatabase::class.java,
                    "bungbung.db",
                )
                .addMigrations(MIGRATION_1_2)
                .build().also { instance = it }
            }
    }
}
```

- [ ] **Step 5: `BungbungApplication.kt` 수정 — CarProfileRepository.init() 추가**

```kotlin
package com.bungbung.diary

import android.app.Application
import com.bungbung.diary.data.local.BungbungDatabase
import com.bungbung.diary.data.local.CarProfileRepository
import com.bungbung.diary.data.local.DiaryRepository

class BungbungApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val db = BungbungDatabase.getInstance(this)
        DiaryRepository.init(db.diaryEntryDao())
        CarProfileRepository.init(db.carProfileDao())
    }
}
```

- [ ] **Step 6: 빌드 확인**

```powershell
.\gradlew assembleDebug
```

Expected: `BUILD SUCCESSFUL` (KSP가 CarProfileDao 구현 클래스를 자동 생성)

- [ ] **Step 7: 커밋**

```powershell
git add car-app/app/src/main/java/com/bungbung/diary/data/local/
git add car-app/app/src/main/java/com/bungbung/diary/BungbungApplication.kt
git commit -m "feat: CarProfile Room 레이어 — entity/dao/repository + DB 마이그레이션 v1→v2"
```

---

## Task 4: AppState — deliveryDate 필드 추가

**Files:**
- Modify: `car-app/app/src/main/java/com/bungbung/diary/ui/AppState.kt`

**배경:** 출고일을 온보딩에서 입력받아 `AppState`에 임시 보관했다가 완료 시 Room에 저장한다. 형식은 ISO 날짜 문자열("YYYY-MM-DD"), null = 미설정.

- [ ] **Step 1: `AppState.kt`에 `deliveryDate` 필드 추가**

`var tone by mutableStateOf(Tone.FRIEND)` 줄 바로 뒤에 추가:

```kotlin
var deliveryDate by mutableStateOf<String?>(null)
```

`resetDemo()` 함수 안에도 초기화 추가:

```kotlin
fun resetDemo() {
    loadScenario(Scenarios.DiaryDay)
    oilDue       = false
    nickname     = "붕붕이"
    tone         = Tone.FRIEND
    deliveryDate = null
}
```

- [ ] **Step 2: 빌드 확인**

```powershell
.\gradlew assembleDebug
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: 커밋**

```powershell
git add car-app/app/src/main/java/com/bungbung/diary/ui/AppState.kt
git commit -m "feat: AppState에 deliveryDate 필드 추가"
```

---

## Task 5: OnboardingScreen 개선 — FAMILY 제거·DeliveryDateStep·HealthCheckStep

**Files:**
- Modify: `car-app/app/src/main/java/com/bungbung/diary/ui/onboarding/OnboardingScreen.kt`

**배경:**
1. `Step.FAMILY` 제거 → 7단계로 축소
2. `DeliveryDateStep`: "나중에" 버튼은 그대로 두고, 날짜 입력 필드(OutlinedTextField, YYYY-MM-DD)를 실제 동작하게 수정
3. `HealthCheckStep`: `FakeData.healthCheck` 대신 `FakeVehicleDataSource`에서 현재 차량 상태를 읽어와 동적으로 구성

- [ ] **Step 1: Step enum에서 FAMILY 제거**

현재:
```kotlin
private enum class Step(val title: String, val skippable: Boolean = false) {
    WELCOME("환영"),
    NICKNAME("닉네임"),
    TONE("말투"),
    CONSENT("동의"),
    HEALTH_CHECK("차 상태 불러오기"),
    DELIVERY_DATE("출고일", skippable = true),
    FAMILY("가족 연결", skippable = true),
    FIRST_MEET("첫인사"),
}
```

변경 후:
```kotlin
private enum class Step(val title: String, val skippable: Boolean = false) {
    WELCOME("환영"),
    NICKNAME("닉네임"),
    TONE("말투"),
    CONSENT("동의"),
    HEALTH_CHECK("차 상태 불러오기"),
    DELIVERY_DATE("출고일", skippable = true),
    FIRST_MEET("첫인사"),
}
```

- [ ] **Step 2: `when(step)` 블록에서 `Step.FAMILY ->` 케이스 제거**

현재 when 블록 안에서:
```kotlin
Step.FAMILY -> FamilyStep()
```
이 줄을 삭제한다.

- [ ] **Step 3: `FamilyStep` composable 함수 전체 삭제**

파일에서 아래 함수를 찾아 전체 삭제:
```kotlin
@Composable
private fun FamilyStep() {
    // ... (전체 함수 본문)
}
```

- [ ] **Step 4: `DeliveryDateStep` 실제 입력 동작 구현**

현재 `DeliveryDateStep()` 함수를 아래로 교체:

```kotlin
@Composable
private fun DeliveryDateStep(state: AppState) {
    Speech("우리 처음 만난 날은 언제야?")
    OutlinedTextField(
        value = state.deliveryDate ?: "",
        onValueChange = { input ->
            // YYYY-MM-DD 형식만 허용 (숫자와 하이픈, 최대 10자)
            val filtered = input.filter { it.isDigit() || it == '-' }.take(10)
            state.deliveryDate = filtered.ifBlank { null }
        },
        singleLine = true,
        label = { Text("출고일 (YYYY-MM-DD)") },
        placeholder = { Text("예: 2025-03-15") },
    )
    Text(
        "모르면 \"나중에\"를 누르세요. 오늘을 처음 만난 날로 쓸게요.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
```

- [ ] **Step 5: `when(step)` 블록에서 `DeliveryDateStep()` 호출에 state 인자 추가**

현재:
```kotlin
Step.DELIVERY_DATE -> DeliveryDateStep()
```

변경 후:
```kotlin
Step.DELIVERY_DATE -> DeliveryDateStep(state)
```

- [ ] **Step 6: `HealthCheckStep` 동적 데이터로 교체**

현재 `HealthCheckStep()` 함수를 아래로 교체:

```kotlin
@Composable
private fun HealthCheckStep() {
    val ds = remember { com.bungbung.diary.data.vehicle.fake.FakeVehicleDataSource() }
    val vehicleState = remember { ds.getVehicleState() }
    val summary = remember { ds.getDailySummary() }
    val carInfo = remember { ds.getCarInfo() }

    val items = remember(vehicleState) {
        buildList {
            add(HealthCheckItem("차 정보", "${carInfo.modelYear}년식 ${carInfo.manufacturer}", HealthCheckItem.Level.OK))
            add(HealthCheckItem("누적 주행거리", "${summary.odometerKm} km", HealthCheckItem.Level.OK))

            val lowTire = vehicleState.tires.asList().firstOrNull { it.isLow }
            if (lowTire != null) {
                add(HealthCheckItem("타이어", "${lowTire.position} ${lowTire.psi} psi (권장 ${lowTire.recommendedPsi}) · 주의", HealthCheckItem.Level.CAUTION))
            } else {
                add(HealthCheckItem("타이어", "4바퀴 정상", HealthCheckItem.Level.OK))
            }

            if (vehicleState.safetyWarnings.isEmpty()) {
                add(HealthCheckItem("경고등", "켜진 경고 없음", HealthCheckItem.Level.OK))
            } else {
                add(HealthCheckItem("경고등", vehicleState.safetyWarnings.joinToString { it.name }, HealthCheckItem.Level.CAUTION))
            }

            add(HealthCheckItem(
                if (vehicleState.energy.isEv) "배터리" else "연료",
                "${vehicleState.energy.percent}%",
                if (vehicleState.energy.percent <= 15) HealthCheckItem.Level.CAUTION else HealthCheckItem.Level.OK,
            ))
        }
    }

    Speech("내 몸 상태를 살펴볼게")
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.forEach { item ->
            val color = when (item.level) {
                HealthCheckItem.Level.OK      -> Ok
                HealthCheckItem.Level.CAUTION -> Warning
                HealthCheckItem.Level.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            StatRow(item.name, item.result, valueColor = color)
        }
    }
}
```

- [ ] **Step 7: 불필요한 import 정리 — `FakeData` import 제거**

`OnboardingScreen.kt` 상단에서 아래 import를 제거:
```kotlin
import com.bungbung.diary.data.fake.FakeData
```
단, `FakeData.nicknameSuggestions`와 `FakeData.message`는 `NicknameStep`, `ToneStep`, `FirstMeetStep`에서 여전히 사용 중이므로 주의. `FakeData.healthCheck` 만 제거된 것이므로 import를 제거하면 안 된다. 이 단계는 건너뛴다.

> **주의:** FakeData는 NicknameStep의 제안 이름 목록, ToneStep의 예시 문장, FirstMeetStep의 첫인사 문장에서 계속 사용된다. healthCheck 필드만 더 이상 사용되지 않는다.

- [ ] **Step 8: 빌드 확인**

```powershell
.\gradlew assembleDebug
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 9: 커밋**

```powershell
git add car-app/app/src/main/java/com/bungbung/diary/ui/onboarding/OnboardingScreen.kt
git commit -m "feat: 온보딩 — FAMILY 제거, DeliveryDateStep 입력 구현, HealthCheckStep 동적 데이터"
```

---

## Task 6: BungbungApp — 온보딩 완료 저장 + 앱 시작 시 프로필 로드

**Files:**
- Modify: `car-app/app/src/main/java/com/bungbung/diary/ui/BungbungApp.kt`

**배경:**
- 온보딩이 끝나면 CarProfileRepository에 저장하고 MainScreen으로 이동
- 앱 재시작 시 `LaunchedEffect`로 프로필을 읽어 온보딩 완료 여부에 따라 시작 화면 결정
- 프로필이 있으면 AppState에 nickname·tone·deliveryDate를 복원

- [ ] **Step 1: `BungbungApp.kt` 전체 교체**

```kotlin
package com.bungbung.diary.ui

import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bungbung.diary.data.local.CarProfileEntity
import com.bungbung.diary.data.local.CarProfileRepository
import com.bungbung.diary.domain.Tone
import com.bungbung.diary.ui.booking.BookingScreen
import com.bungbung.diary.ui.main.MainScreen
import com.bungbung.diary.ui.onboarding.OnboardingScreen
import com.bungbung.diary.ui.probe.ProbeScreen
import com.bungbung.diary.ui.settings.SettingsScreen
import com.bungbung.diary.ui.station.GasStationScreen
import kotlinx.coroutines.launch

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
    var startDest by remember { mutableStateOf<String?>(null) }

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

    if (startDest == null) return  // 프로필 로드 중 — 빈 화면 (짧음)

    val nav = rememberNavController()
    val scope = rememberCoroutineScope()

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
```

- [ ] **Step 2: 빌드 확인**

```powershell
.\gradlew assembleDebug
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: 모든 단위 테스트 통과 확인**

```powershell
.\gradlew :app:test
```

Expected: `BUILD SUCCESSFUL` (TemplateTextGeneratorTest + JosaUtilTest 모두 통과)

- [ ] **Step 4: 커밋**

```powershell
git add car-app/app/src/main/java/com/bungbung/diary/ui/BungbungApp.kt
git commit -m "feat: 앱 시작 시 CarProfile 로드 + 온보딩 완료 시 Room에 저장"
```

---

## 완료 기준 체크리스트

- [ ] `VehicleDataSource` 인터페이스가 `FakeVehicleDataSource`와 `PleosVehicleDataSource`(스텁)으로 구현된다
- [ ] `FakeVehicleDataSource`를 `PleosVehicleDataSource`로 교체해도 나머지 코드가 변경 없이 컴파일된다
- [ ] JosaUtil이 이/가·을/를·은/는·아/야 12개 테스트 케이스를 통과한다
- [ ] `car_profile` 테이블이 DB 버전 2에 생성되고 기존 `diary_entry` 데이터가 유지된다
- [ ] 온보딩 7단계(환영·닉네임·말투·동의·차 상태 불러오기·출고일·첫인사)가 순서대로 진행된다
- [ ] `HealthCheckStep`이 `ScenarioHolder.current`의 실제 차량 상태를 표시한다
- [ ] `DeliveryDateStep`에서 날짜를 입력하면 `AppState.deliveryDate`에 저장된다
- [ ] 온보딩 완료 후 재실행 시 MainScreen으로 바로 이동한다
- [ ] `resetDemo()` 후 재실행하면 온보딩부터 시작된다 (Room 값은 그대로이므로 실제로는 MAIN으로 감 — 에뮬레이터에서 앱 데이터 지우고 테스트)
