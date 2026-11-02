# Pleos Playground API 가용 현황 (붕붕일기 기준)

> 확인 일자: 2026-11-02  
> 확인 방법: Pleos Playground 직접 확인  
> **이 문서가 코드 작성의 기준입니다. 여기 "사용 가능"으로 표시된 것만 구현하세요.**

---

## 1. Pleos Vehicle SDK — 사용 가능 도메인

아래 13개 도메인은 Playground에서 사용 가능이 확인되었습니다.  
실제 메서드 시그니처는 **SDK 문서/클래스 파일에서 직접 확인** 후 코드 주석에 출처 URL을 남기세요.

| # | 도메인 (playground 표시명) | 우리 프로젝트에서 쓰는 데이터 | Phase |
|---|---|---|---|
| 1 | 차량 제원 정보 조회 | 제조사, 생산 연도 (CarInfo) | 1-B |
| 2 | 차량 주행 정보 조회 | 주행 거리(Odometer), 드라이브 모드, 브레이크 페달, 크루즈 컨트롤 | 2 |
| 3 | 도어/트렁크/프렁크 상태 조회 | (현재 기획에서 미사용) | — |
| 4 | EV 및 연료 정보 조회 | 배터리 충전량, 연료 잔량, 전비·연비 | 1-B, 2 |
| 5 | 내부 온도 및 공조 상태 조회 | 설정 온도, 히터 ON/OFF, 에어컨 상태 | 2 |
| 6 | 내부/외부 라이트 상태 조회 | (현재 기획에서 미사용) | — |
| 7 | 차량 안전 상태 조회 | 타이어 공기압(4바퀴), 경고등 목록 | 1-B, 2 |
| 8 | 좌석 설정 상태 조회 | (현재 기획에서 미사용) | — |
| 9 | 사이드미러 상태 조회 | (현재 기획에서 미사용) | — |
| 10 | 차량 통행료 시스템 정보 조회 | (현재 기획에서 미사용) | — |
| 11 | 창문 상태 조회 | **와이퍼 모드** (OFF/LOW/HIGH 등) | 2 |
| 12 | 경로 정보 조회 | (현재 기획에서 미사용, 위치정보 수집 안 함) | — |
| 13 | 경로 탐색 설정 | (현재 기획에서 미사용) | — |

> **주의**: 도메인 이름만 확인된 상태입니다. `vehicle.getXxx()`로 얻는 도메인 객체의 정확한 메서드명과 Capability ID는 SDK 클래스에서 확인하세요.

---

## 2. Pleos SDK — 사용 불가 (대안으로 구현)

| 기능 | 상태 | 대안 |
|---|---|---|
| TextToSpeech SDK (Pleos TTS) | **사용 불가** | Android 시스템 TTS (`android.speech.tts.TextToSpeech`) |
| LLM SDK (Gleo AI) | **사용 불가** | 서버 `POST /api/narrate` 프록시 |
| SpeechToText SDK | **사용 불가** | 버튼 UI로 대체 (닉네임 음성 입력 기능 제외) |

---

## 3. AAOS 표준 API (Pleos와 별개)

기어·속도·외기온도는 Pleos SDK가 아닌 Android 표준 `CarPropertyManager`를 씁니다.  
(Pleos 정책: 표준으로 얻을 수 있는 데이터는 표준 API 우선)

| 데이터 | API | 프로퍼티 ID | 권한 | 에뮬레이터 동작 |
|---|---|---|---|---|
| 기어 | `CarPropertyManager.getIntProperty` | `289408000` | `CAR_DRIVE` | Pleos 에뮬레이터에서 확인 필요 |
| 속도 | `CarPropertyManager.getFloatProperty` | `291504647` | `CAR_SPEED` | Pleos 에뮬레이터에서 확인 필요 |
| 외기온도 | `CarPropertyManager.getFloatProperty` | `291505923` | `CAR_CLIMATE` | Pleos 에뮬레이터에서 확인 필요 |
| 운전 중 제한 | `CarUxRestrictionsManager.registerListener()` | — | 없음 | Pleos 에뮬레이터에서 확인 필요 |

> AAOS API가 안 되면 Pleos Vehicle SDK의 "차량 주행 정보 조회"에서 드라이브 모드·브레이크로 기어·주행 상태를 대체합니다.

---

## 4. 프로젝트 기능별 구현 경로 (Phase 2 기준)

| 필요한 데이터 | 구현 경로 | Fake 대안 |
|---|---|---|
| 기어 (P/R/N/D) | AAOS `CarPropertyManager` | `FakeVehicleDataSource.gear` |
| 속도 | AAOS `CarPropertyManager` | `FakeVehicleDataSource.speed` |
| 외기온도 | AAOS `CarPropertyManager` | `FakeVehicleDataSource.outsideTempC` |
| 누적 주행거리 | Pleos **#2** 차량 주행 정보 → 주행 거리 | `FakeVehicleDataSource.odometerKm` |
| 와이퍼 상태 | Pleos **#11** 창문 상태 → 와이퍼 모드 | `FakeVehicleDataSource.wiperState` |
| 타이어 공기압 | Pleos **#7** 차량 안전 → 타이어 공기압 | `FakeVehicleDataSource.tireData` |
| 차량 경고등 | Pleos **#7** 차량 안전 → 경고등 목록 | `FakeVehicleDataSource.safetyWarnings` |
| 차 정보 (CarInfo) | Pleos **#1** 차량 제원 정보 | `FakeVehicleDataSource.carInfo` |
| 연료 / 배터리 | Pleos **#4** EV 및 연료 정보 | `FakeVehicleDataSource.fuelOrBattery` |
| 히터 / 공조 설정 온도 | Pleos **#5** 내부 온도 및 공조 | `FakeVehicleDataSource.hvac` |
| 음성 출력 (일기) | Android 시스템 TTS, `HYBRID` 흉내 | `FakeSpeaker` |
| 음성 출력 (경고) | Android 시스템 TTS, Locale.KOREAN | `FakeSpeaker` |
| LLM 문장 생성 | 서버 `POST /api/narrate` | `TemplateTextGenerator` |
| 운전 중 화면 제한 | `CarUxRestrictionsManager` | `FakeUxRestrictions` (gear == D) |

---

## 5. Playground 환경 정보

Pleos 에뮬레이터(devbox) 연결에 필요한 정보입니다.

| 항목 | 값 |
|---|---|
| CRN | `did:cac:42dot:system:devbox::2da17f7e-7397-4374-9ac9-65b34db304dc` |

**devbox 초기화 명령어** (Android Studio 터미널에서 실행):

```bash
adb root on
adb shell su 0 "echo 'propId: 554696961 areaId: 0 values: did:cac:42dot:system:devbox::2da17f7e-7397-4374-9ac9-65b34db304dc' > /data/vendor/vsomeip/vhal_fifo"
adb reboot
```

> 이 명령어는 CRN을 devbox에 등록합니다. 에뮬레이터를 처음 설정할 때 한 번 실행하면 됩니다.

---

## 6. 카카오톡 (Phase 5-B 시작 전에 채우기)

| 기능 | 필요한 설정·동의항목 | 권한 신청 필요 여부 | 쿼터 | 출처 |
|---|---|---|---|---|
| 카카오 로그인 | | | | |
| 나에게 보내기 | | | | |
| 카카오톡 공유 (JavaScript) | | | | |
