# 탕탕 · 지갑재판소 — AI 에이전트 작업 규칙

> **이 파일이 규칙 원본이다.** `CLAUDE.md` · `GEMINI.md` 는 이 파일을 임포트하는 포인터일 뿐이다.
> 규칙을 바꿀 일이 있으면 **반드시 이 파일을 고친다.**

## 프로젝트

새는 돈을 법정에 세우는 자산관리 서비스. 계좌·카드 거래내역에서 고정지출을 자동 탐지(기소)하고,
절약 시뮬레이션 → 챌린지·미션(집행) → 실제 절감액 검증(판결문=월간 리포트)까지 폐루프로 잇는다.
KB IT's Your Life 7기 종합실무 프로젝트. 6인 팀이 동시에 개발한다.

## ❌ 절대 금지 기술 (사용 시 평가 감점 — 예외 없음)

| 금지 | 반드시 사용 |
|------|------------|
| Spring Boot (`spring-boot-*`, `@SpringBootApplication`) | **Spring Legacy (Spring MVC)** — JavaConfig 수동 설정, war 배포 |
| JPA / Hibernate (`@Entity`, `JpaRepository`) | **MyBatis** (Mapper 인터페이스 + XML) |
| React (웹) | **Vue3** |

제안·코드·의존성 어디에도 위 항목이 등장하면 즉시 멈추고 오른쪽 대체 기술로 바꾼다.
`build.gradle` 에 `spring-boot-starter-*` 가 들어가면 그 자체로 감점이다.

## 기술 스택 (고정 — 임의 변경 금지)

- **Backend**: Java 17 · Spring Framework 5.3.39 (Spring MVC) · MyBatis 3.5.16 / mybatis-spring 2.1.2 · MySQL 8 · HikariCP · **Gradle** · Tomcat 9.x (war)
- **Frontend**: Vue3 + Vite · Pinia · Vue Router · axios
- **모듈 간 비동기**: Spring Event (`ApplicationEventPublisher` + `@EventListener`/`@Async`) — 메시지 브로커 없음
- **실시간 알림**: SSE (`SseEmitter`). `WebConfig` 에 `setAsyncSupported(true)` 적용됨
- **알림 실패 대비**: `tbl_notification_dlq` + 스케줄 재시도 배치
- 빌드에 **JDK 17 필수** (toolchain 강제. JDK 21만 설치된 환경은 빌드 실패)

## 폴더 구조

```
Monorepo/                 Gradle 멀티프로젝트 (루트의 settings.gradle·gradlew·gradle/ 는 정상 위치, 옮기지 말 것)
├─ apps/api/              백엔드. Spring Legacy, war
├─ apps/web/              프론트. Vite + Vue3
├─ db/                    스키마 SQL — 실행 기준 원본
├─ docs/                  API 규격 · 도메인 용어집
└─ .claude/skills/        반복 작업 절차 (팀 공유)
```

폴더별 상세 규칙은 각 폴더의 `AGENTS.md` 를 읽는다.

## 실행 명령어

```bash
./gradlew :apps:api:build      # 백엔드 빌드 → apps/api/build/libs/*.war
./gradlew :apps:api:test       # 백엔드 테스트
cd apps/web && npm install     # 프론트 의존성
cd apps/web && npm run dev     # 프론트 개발 서버 :5173, /api 는 :8080 으로 프록시
```

최초 세팅 (신규 합류자):
```bash
mysql -u root -p < db/00_init_local_db.sql     # tangtang DB + 전용 계정 생성
cp apps/api/src/main/resources/application-local.properties.example \
   apps/api/src/main/resources/application-local.properties
```
연결 확인: `GET http://localhost:8080/api/health` → `{"success":true,"data":{"status":"UP",...}}`

## 작업 프로토콜

1. 코드를 쓰기 전에 **관련 기존 코드·문서를 먼저 읽는다.** 유사 기능이 이미 있는지 확인한다.
2. 계획을 짧게 요약해 보여주고 시작한다. **추측으로 구조를 바꾸지 않는다.**
3. 기능 구현에는 **단위 테스트를 동반**한다.
4. 확실하지 않은 값(포트·경로·버전·컬럼명)은 지어내지 말고 실제 파일을 확인하거나 질문한다.
5. 6명이 병렬로 작업한다. 담당 모듈 밖의 파일을 고칠 때는 먼저 알린다.

## API 응답 규칙

모든 REST 응답은 공통 래퍼 `com.kb.tangtang.common.dto.ApiResponse` 로 감싼다. **raw 객체 반환 금지.**

```json
성공  { "success": true,  "data": { ... } }
실패  { "success": false, "code": "NOT_FOUND", "message": "..." }
```

업무 규칙 위반은 `BusinessException(code, message)` 를 던진다 → 400 으로 자동 변환된다.
전역 예외 처리는 `common/exception/CommonExceptionAdvice` 한 곳에서만 한다.

## Git 규칙

- 브랜치: `main`(배포) / `develop`(통합) / `feat-<모듈>-<기능>` · `fix-<이슈>`
- 커밋: `타입: 한국어 요약` — `feat` `fix` `docs` `refactor` `test` `chore`
  - 예) `feat: 고정지출 탐지 룰 기반 매칭 구현`
- `main`·`develop` 직접 push 금지. PR 로 병합하며 리뷰어 1명 이상
- 문서·주석·커밋 메시지·PR 은 **한국어**

### 커밋 금지 목록
- 시크릿: CODEF 키, DB 비밀번호, `application-local.properties`, `*.key`, `.env`
- 빌드 산출물: `build/`, `.gradle/`, `node_modules/`, `dist/`
- 개인 설정: `.idea/`, `CLAUDE.local.md`, `AGENTS.override.md`, `.claude/settings.local.json`

## 참조 문서 (내용을 이 파일로 복사하지 말 것)

| 문서 | 내용 |
|---|---|
| `apps/api/AGENTS.md` | 백엔드 패키지 구조·MyBatis·모듈 경계 |
| `apps/web/AGENTS.md` | Vue3 규칙·디자인 토큰·API 호출 |
| `db/AGENTS.md` | 스키마 원본 규칙·네이밍 |
| `docs/DOMAIN_GLOSSARY.md` | 한글 용어 ↔ 영문 코드명 ↔ 테이블명 |
| `docs/AI_TOOLING_GUIDE.md` | 이 규칙 파일들이 각각 무엇이고 어떻게 쓰이는지 (신규 합류자용) |
| `docs/API_SPEC.md` | 엔드포인트 목록 |
