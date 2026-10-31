# CLAUDE.md

붕붕일기: 차의 마스코트가 1인칭으로 하루를 들려주는 Pleos Connect(Android Automotive) 앱과 Spring Boot 서버. 2026 현대차·기아 차량용 앱 공모전 출품작.

**전체 명세는 `PROMPT.md`.** 작업을 시작하기 전에 해당 Phase 부분을 읽는다.

## 구조와 명령

| 폴더 | 내용 | 명령 (Windows는 `gradlew.bat`) |
| --- | --- | --- |
| `car-app/` | Kotlin + Jetpack Compose, 패키지 `com.bungbung.diary` | 빌드 `./gradlew assembleDebug` · 단위 테스트 `./gradlew testDebugUnitTest` · 설치 `./gradlew installDebug` |
| `server/` | Spring Boot 3, Java 21, PostgreSQL (Phase 6에서 생성) | DB `docker compose up -d` · 실행 `./gradlew bootRun` · 테스트 `./gradlew test` |
| `docs/` | `api-findings.md`(API 조사), `decisions.md`(임계값·결정), `demo-script.md`(시연 순서) | |

- Android Studio에서는 저장소 최상위가 아니라 `car-app` 폴더를 연다.
- 에뮬레이터에 한글 닉네임 넣기: `adb shell am start -S -n com.bungbung.diary/.MainActivity --es nickname 킹카`

## 꼭 지킬 규칙

1. Pleos·카카오 API 이름과 시그니처를 지어내지 않는다. 문서나 SDK 클래스로 확인하고 주석에 출처 URL을 남긴다. 모르면 멈추고 묻는다.
2. 외부 의존성(차 신호, 음성, LLM, 서버, 마스코트 그림)은 인터페이스 뒤에 두고 Fake 구현을 유지한다. 차와 서버 없이도 앱이 끝까지 돌아야 한다.
3. 문장 속 숫자는 데이터에 있는 숫자만. 경고 문장은 LLM 없이 검증된 템플릿만 쓴다.
4. 주행 중(`CarUxRestrictions`)엔 글을 숨기고 음성만. 키보드 입력은 주차 중 닉네임 입력뿐이다.
5. 위치와 경로는 서버와 가족에게 보내지 않는다. 비밀 키는 서버 환경변수에만 둔다.
6. 닉네임·이름 뒤 조사는 `JosaUtil`로 고른다. 문자열에 "가/를" 같은 조사를 직접 붙이지 않는다.
7. 기존 코드를 고쳐서 쓰고, 통째로 다시 쓰지 않는다.

## 작업 방식

- `main`에 직접 푸시하지 않는다. Phase마다 브랜치(`feat/phase1-first-run` 같은 이름)를 만들고 PR을 올린다.
- 끝나면 빌드·테스트를 돌리고 "확인한 것 / 확인 못 한 것"을 나눠 보고한다. 화면 작업은 스크린샷을 남긴다.
- 임계값이나 설계를 바꾸면 `docs/decisions.md`에, API를 확인하면 `docs/api-findings.md`에 적는다.
- 커밋 메시지, 문서, UI 문구는 한국어로 쓴다.
