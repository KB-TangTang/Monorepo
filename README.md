# 탕탕 · 지갑재판소 (TangTang)

새는 돈을 법정에 세우는 자산관리 서비스.
KB IT's Your Life 7기 종합실무 프로젝트 (28반 3팀).

## 구성

| 경로 | 내용 |
|---|---|
| `apps/api/` | **Java 17** + **Spring Legacy 5.3.x (Spring MVC)** + MyBatis, Gradle, war 배포 |
| `apps/web/` | Vue3 + Vite + Pinia |
| `db/` | 스키마 DDL·쿼리 (**실행 기준 원본**) |
| `docs/` | API 규격·도메인 용어집 등 개발 참고 문서 |

Gradle 멀티프로젝트다. 루트의 `settings.gradle`·`gradlew`·`gradle/` 는 **루트에 있어야 정상**이다
(래퍼를 공유해 팀 전원이 동일한 Gradle 버전으로 빌드한다).

> ⚠️ **Spring Boot / JPA·Hibernate / React(웹) 사용 금지** — 평가 감점 항목이다.

> 🤖 AI 코딩 도구(Claude Code · Codex · Gemini)를 쓴다면 **`AGENTS.md` 가 규칙 원본**이다.
> 각 파일이 뭘 하는지는 `docs/AI_TOOLING_GUIDE.md` 참고.

## 로컬 세팅

### 0. 사전 요구사항 (팀 전원 동일하게 맞출 것)

| 도구 | 버전 | 확인 |
|---|---|---|
| JDK | **17** (필수 — toolchain 강제) | `java -version` |
| Node | **24.14.1** (최소 24.12) | `node -v` |
| MySQL | 8.x | `mysql --version` |
| Tomcat | 9.0.x (javax.servlet 기준) | |

Node 는 `apps/web/.nvmrc` 에 버전이 고정돼 있다. nvm 사용 시:
```bash
cd apps/web && nvm install 24.14.1 && nvm use 24.14.1
```


### 1. DB
관리자 계정으로 초기화 스크립트를 1회 실행한다. `tangtang` DB 와 `tangtang` 전용 계정이 생성된다.
```bash
mysql -u root -p < db/00_init_local_db.sql
mysql -u tangtang -p tangtang < db/schema.sql   # 스키마가 준비된 뒤
```

### 2. Backend
```bash
# 접속 정보 설정 (커밋되지 않는 파일). 기본값은 위 스크립트로 만든 계정과 일치한다.
cp apps/api/src/main/resources/application-local.properties.example \
   apps/api/src/main/resources/application-local.properties

./gradlew :apps:api:build      # → apps/api/build/libs/*.war
```
Tomcat 9.x 에 war 배포하거나 IntelliJ 의 Tomcat 서버로 실행한다. (기본 포트 8080)

연결 확인: `GET http://localhost:8080/api/health` → `{"success":true,"data":{"status":"UP",...}}`

### 3. Frontend
```bash
cd apps/web
npm install
npm run dev        # http://localhost:5173, /api 요청은 8080 으로 프록시
```

## 규칙

- 브랜치: `main`(배포·직접 push 금지) / `dev`(통합) / `feature/{이슈번호}-{도메인}-{상세}` · `fix/{이슈번호}-{상세}`
- 커밋: `type: 한국어 요약` — 전체 타입 목록은 `AGENTS.md` 참고
- PR 은 `dev` 기준, 제목은 `[타입] 제목`, 리뷰어 1명 이상 승인 후 merge
- API 응답은 **공통 래퍼 `ApiResponse`** 로 통일한다
- 매퍼 인터페이스에는 반드시 `@Mapper` 를 붙인다. 매퍼 XML 은 `src/main/resources/mapper/<모듈>/` 에 둔다

## ☑ 팀 확정 필요

- [ ] Tomcat 버전 고정 (9.0.x 권장 — javax.servlet 기준)
- [ ] MySQL 버전 확정 후 `db/schema.sql` 실행 검증
- [ ] 프론트 UI 라이브러리 사용 여부
- [ ] `dev` 브랜치 생성 및 `main` 브랜치 보호 설정
- [ ] `apps/web` Prettier · ESLint 설정 도입 (들여쓰기 4칸·싱글 쿼트)
