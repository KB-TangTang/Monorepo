# Google OAuth 로그인 — 설계서

- **이슈**: #9 `[FEAT] 로그인 구현 및 UI 개발`
- **브랜치**: `feature/9-auth-google-login`
- **작성일**: 2026-08-03
- **범위**: 로그인 풀스택 (프론트 화면 + 백엔드 인증 API + DB 초기화)

---

## 1. 배경과 범위

`doc/개발산출물/화면설계/figma_tangtang/home` 폴더는 이름과 달리 온보딩 전체 플로우를 담고 있다.
점검 결과 9개 화면이 4개 단계로 나뉜다.

| 파일 | 화면 | 담당 이슈 |
|---|---|---|
| `download 8.png` | 로그인 — "Google로 판결 시작하기" | **#9 (이 문서)** |
| `div-2.png` · `div-1.png` · `div-5.png` | 계좌 선택 · 1원 인증 · 즉시 조회 | 후속 이슈 |
| `div.png` · `Frame.png` | 서비스 동의 · 금융데이터 수집 동의 | 후속 이슈 |
| `1 · 홈.png` · `div-3.png` · `div-4.png` · `div-6.png` | 홈 3상태 · 알림 목록 | 후속 이슈 |

### 이 문서가 다루는 것
- Google OAuth 2.0 Authorization Code 로그인 (백엔드 리다이렉트 방식)
- JWT 액세스 토큰 + 리프레시 토큰 회전
- 로그인 화면 · 콜백 화면 · 라우트 가드
- 로컬 DB 스키마 초기화 (선행 조건)

### 이 문서가 다루지 않는 것
- 약관 동의 화면 (후속 이슈). 단, 로그인 응답에 `needsConsent` 플래그를 미리 포함해
  후속 이슈에서 라우팅만 바꿔 끼울 수 있게 한다.
- 계좌 연동 · 홈 대시보드 · 알림
- 로그아웃 UI (마이페이지 소관). API만 제공한다.
- 운영 도메인 통합 (7절 참고)

### 사용자 동선과 개발 순서의 차이
실제 동선은 **로그인 → 동의 → 계좌연동**이지만, 개발은 **로그인 → 계좌연동 → 동의** 순으로 진행한다.
따라서 계좌연동 이슈 시점에는 그 앞단의 동의 게이트가 비어 있다. 이는 의도된 상태이며,
동의 이슈에서 `needsConsent` 플래그를 읽어 게이트를 끼운다.

---

## 2. 확정된 기술 결정

| 항목 | 결정 | 이유 |
|---|---|---|
| OAuth 방식 | 백엔드 리다이렉트 (Authorization Code) | `client_secret`이 서버에만 존재. Figma의 커스텀 남색 버튼을 100% 재현 가능 |
| 토큰 보관 | Access = Pinia 메모리 / Refresh = httpOnly 쿠키 | URL·localStorage에 토큰이 남지 않음. XSS로 리프레시 토큰 탈취 불가 |
| 인증 강제 | `HandlerInterceptor` + `@LoginUser` ArgumentResolver | Spring Security는 Boot 없이 설정 비용이 과다. 단일 사용자 롤이라 불필요 |
| JWT 라이브러리 | `io.jsonwebtoken:jjwt` 0.12.6 | `spring-boot-*`가 아니므로 금지 기술 아님 |
| 로그인 가드 | 전 라우트 보호. 예외는 `/login`·`/auth/callback`·`/dev/ui` | 5탭 전부 개인 금융 데이터. 개발용 bypass 플래그는 만들지 않음 |

> **금지 기술 준수**: Spring Legacy(MVC) · MyBatis · Vue3 유지. Spring Boot / JPA / React 미사용.

---

## 3. 선행 조건 — DB 초기화

`db/AGENTS.md`는 `schema.sql`·`seed.sql`을 실행 기준 원본으로 선언하지만 **두 파일이 존재하지 않았다.**
검증된 정본 DDL(`doc/개발산출물/ERD/탕탕_구현용_v2_0803.sql`, 33테이블, MySQL 8.0.46 실측 검증)을
`db/` 로 옮겨 배치한다.

### 3.1 생성 파일
- `db/schema.sql` — 33테이블 전체 DDL + `USE tangtang;` 헤더
- `db/seed.sql` — `tbl_mission_difficulty` 3행(EASY/NORMAL/HARD) + 고정지출 기본 룰 1행

`tbl_user.difficulty_id`가 `tbl_mission_difficulty`를 **NOT NULL FK**로 참조하므로,
시드 3행이 없으면 회원가입 INSERT 자체가 실패한다.

로그인에 필요한 3테이블만 넣지 않고 전체를 배치하는 이유는, 후속 이슈마다 스키마 파편이
생겨 `migration/` 이력이 어지러워지는 것을 막기 위해서다.

### 3.2 docker-compose 연동
`docker-compose.yml`이 `./db`를 `/docker-entrypoint-initdb.d`로 마운트한다.
최초 기동 시 파일명 알파벳순으로 자동 실행되며, `00_init_local_db.sql` → `schema.sql` → `seed.sql`
순서가 되어 의존 관계와 일치한다. 별도 파일명 접두는 불필요하다.

### 3.3 시크릿 정리 (완료)
DB 비밀번호가 3개 파일에 하드코딩돼 git에 추적되고 있었다. 동일 플레이스홀더
`CHANGE_ME_DB_PASSWORD`로 통일했다.

| 파일 | 변경 |
|---|---|
| `.env.example` | `CHANGE_ME_ROOT_PASSWORD` / `CHANGE_ME_DB_PASSWORD` |
| `db/00_init_local_db.sql` | `IDENTIFIED BY 'CHANGE_ME_DB_PASSWORD'` + 커밋 금지 경고 |
| `apps/api/.../application-local.properties.example` | `jdbc.password=CHANGE_ME_DB_PASSWORD` |
| `db/AGENTS.md` | "로컬 개발 계정은 예외" 문구 삭제 → 예외 없음 |
| `AGENTS.md` | 최초 세팅 절차를 3단계로 재작성 |

`.gitignore`는 이미 `.env`·`.env.*`·`**/application-local.properties`를 차단하고
`.example`만 예외 처리하고 있어 변경하지 않았다.

> **팀 공지 필요**: `db/AGENTS.md`의 "로컬 계정은 예외" 규칙을 뒤집는 변경이다. PR 설명에 명시한다.

---

## 4. 인증 플로우

```
[1] /login — "Google로 판결 시작하기" 클릭
      window.location.href = '/api/auth/google'   (SPA 라우팅 아닌 전체 이동)

[2] GET /api/auth/google
      state 32byte 랜덤 생성 → httpOnly 쿠키 oauth_state (5분 만료)
      302 → accounts.google.com/o/oauth2/v2/auth
              ?client_id=… &redirect_uri=… &response_type=code
              &scope=openid%20email%20profile &state=…

[3] 구글 계정 선택 · 동의

[4] GET /api/auth/google/callback?code=…&state=…
      ① state 쿠키 대조 → 불일치 시 OAUTH_STATE_MISMATCH, 쿠키 즉시 삭제
      ② code → 구글 token endpoint POST (RestTemplate) → id_token 획득
      ③ id_token 파싱 → sub / email / name / picture
      ④ tbl_user 조회 (social_provider='GOOGLE', provider_user_id=sub)
           없으면 INSERT — status='ACTIVE', difficulty_id=1(EASY), nickname=구글 name
           status != 'ACTIVE' 이면 USER_WITHDRAWN
      ⑤ Access JWT(15분) + Refresh 토큰(랜덤 UUID, 14일) 발급
           refresh 는 SHA-256 해시만 tbl_refresh_token 에 저장 (원문 미저장)
      ⑥ Set-Cookie: refresh_token=… HttpOnly; Secure; SameSite=…; Path=/api/auth
      302 → 프론트 /auth/callback        ← URL 에 토큰 일절 없음

[5] 브라우저가 SPA 를 새로 로드한다 → main.js 부팅 시퀀스가 이미
      POST /api/auth/refresh 를 1회 호출해 세션을 복원한다 (4.2 와 동일 경로)
      → { accessToken, user, needsConsent } → authStore 저장 (accessToken 은 메모리에만)
      → /auth/callback 라우트는 로그인 여부만 보고 router.replace('/')
```

> 콜백 착지 화면이 refresh 를 따로 부르지 않는 이유: OAuth 리다이렉트는 전체 페이지 이동이라
> 앱이 새로 부팅된다. 부팅 시퀀스(4.2)가 이미 refresh 를 수행하므로 여기서 또 부르면
> **회전 방식 때문에 두 번째 호출이 재사용으로 감지돼 전체 토큰이 폐기된다.**

> **`needsConsent`의 이번 범위**: 응답에 포함하고 `authStore`에 보관만 한다.
> 이번 이슈에서는 값과 무관하게 항상 `/`로 보낸다(동의 화면이 아직 없음).
> 동의 이슈에서 이 필드를 읽어 `/consent`로 분기하도록 라우팅만 바꾼다.

### 4.1 리프레시 토큰 회전
`tbl_refresh_token.is_revoked` 컬럼 주석이 *"탈취 의심/재사용 감지 시 조기 폐기 (AU_02_01)"* 로
회전 방식을 전제하고 있어 그대로 구현한다.

- `/refresh` 호출마다 기존 토큰을 `is_revoked=1`로 바꾸고 새 토큰을 발급한다.
- **이미 revoke된 토큰이 다시 들어오면** 해당 사용자의 전체 토큰을 일괄 폐기한다(탈취 신호).

### 4.2 앱 부팅 시 자동 로그인
`main.js`에서 `app.mount()` 전에 `/api/auth/refresh`를 1회 시도한다.
성공하면 로그인 상태로 복원, 실패하면 조용히 비로그인으로 진행한다.
새로고침 후 로그인이 유지되는 경로는 이것뿐이다(액세스 토큰이 메모리에만 있으므로).

---

## 5. 백엔드 설계 (`com.kb.tangtang.user`)

`apps/api/AGENTS.md`가 인증을 `user` 모듈 소관으로 지정하므로 새 모듈을 만들지 않는다.

```
user/
├─ controller/  AuthController          로그인 진입 · 콜백 · refresh · logout
│               UserController          GET /api/users/me
├─ service/     AuthService             로그인 오케스트레이션 (@Transactional 경계)
│               GoogleOAuthClient       구글 token 엔드포인트 호출 (RestTemplate)
│               RefreshTokenService     발급 · 회전 · 재사용 감지
├─ mapper/      UserMapper              findBySocialId · insert
│               RefreshTokenMapper      insert · findByHash · revoke · revokeAllByUser
└─ dto/         LoginResultDto · UserMeDto · GoogleTokenDto · GoogleProfileDto

common/auth/    JwtProvider                 생성 · 검증
                JwtAuthInterceptor          Bearer 검증 → 401
                LoginUserArgumentResolver   @LoginUser → userId 주입
                @LoginUser
```

`common/auth`에 두는 이유: 이후 모든 모듈의 컨트롤러가 `@LoginUser`를 사용한다.
`user` 모듈에 두면 다른 모듈이 `user`를 직접 의존하게 되어 모듈 경계 원칙에 어긋난다.

### 5.1 엔드포인트

| 메서드 | 경로 | 인증 | 응답 |
|---|---|---|---|
| GET | `/api/auth/google` | 불필요 | 302 → 구글 |
| GET | `/api/auth/google/callback` | 불필요 | 302 → 프론트 `/auth/callback` + `Set-Cookie` |
| POST | `/api/auth/refresh` | 쿠키 | `{ accessToken, user, needsConsent }` |
| POST | `/api/auth/logout` | 쿠키 | `{}` + 쿠키 만료 + DB revoke |
| GET | `/api/users/me` | Bearer | `{ id, nickname, email }` |

리다이렉트 2건을 제외한 모든 응답은 `ApiResponse` 래퍼로 감싼다.

> `tbl_user`에는 프로필 이미지 컬럼이 없다(`id`·`social_provider`·`provider_user_id`·`email`·
> `nickname`·`name`·`phone`·`birth_date`·`gender`·`status`·`withdrawn_at`·`difficulty_id`·타임스탬프).
> 구글 `picture` 클레임은 저장하지 않고 버린다. 필요해지면 컬럼 추가를 `db/migration/`으로 처리한다.
> 구글 `name`은 `nickname`에 넣고 `name`(실명)은 NULL로 둔다 — 본인확인용 실명은 계좌 인증 단계에서 채운다.

### 5.2 인터셉터 적용 범위
`/api/**` 전체에 등록하되 `/api/health`, `/api/auth/**`는 제외한다.

**두 컨텍스트 주의**: 이 프로젝트는 스캔이 나뉘어 있다 — `ServletConfig`는 `@Controller`·`@ControllerAdvice`만,
`RootConfig`는 나머지를 스캔한다. `JwtAuthInterceptor`·`LoginUserArgumentResolver`·`JwtProvider`는
`@Component`로 두면 루트 컨텍스트에 등록되고, 자식인 서블릿 컨텍스트의 `ServletConfig`가
`addInterceptors()`·`addArgumentResolvers()`에서 주입받아 등록한다.
`ServletConfig`에 `@Configuration`을 붙이면 `@EnableWebMvc`가 두 번 적용되므로 붙이지 않는다.

### 5.3 설정 값 분리
`apps/api/AGENTS.md`의 *"공통 파일에 계정을 쓰지 않는다"* 규칙에 따라 시크릿은 전부 local 쪽에 둔다.

| 파일 | 키 |
|---|---|
| `application.properties` (공통, 커밋됨) | `google.oauth.redirect-uri`, `google.oauth.authorization-uri`, `google.oauth.token-uri`, `jwt.access-token-validity`, `jwt.refresh-token-validity`, `auth.cookie.same-site`, `auth.cookie.secure`, `app.front-url` |
| `application-local.properties` (개인, 미커밋) | `google.oauth.client-id`, `google.oauth.client-secret`, `jwt.secret` |

`application-local.properties.example`에도 위 3개 키를 플레이스홀더로 추가한다.

`app.front-url`·`google.oauth.redirect-uri`는 시크릿은 아니지만 환경마다 다르다.
`application.properties`에 로컬 기본값(`http://localhost:5173`, `http://localhost:5173/api/auth/google/callback`)을
둔다. 다른 환경은 같은 키로 "덮어쓰는" 것이 아니라, `APP_ENV` 기반 조건부 로딩으로 환경별 파일
(`application-docker.properties` 등) 자체가 공통 파일 대신 통째로 로드되어 이 값을 대체한다
(`apps/api/AGENTS.md` 참고 — 환경 파일은 한 번에 하나만 로드된다. 여러 환경 파일을 나란히
`@PropertySource`에 나열하지 않는다).
리다이렉트 URI는 프론트 오리진(`:5173`) 기준이어야 한다 — Vite 프록시를 거쳐야
쿠키가 프론트와 same-origin으로 심긴다. 이 값은 Google Cloud Console의
"승인된 리디렉션 URI"에도 **똑같이** 등록돼 있어야 한다.

### 5.4 추가 의존성
```gradle
implementation 'io.jsonwebtoken:jjwt-api:0.12.6'
runtimeOnly   'io.jsonwebtoken:jjwt-impl:0.12.6'
runtimeOnly   'io.jsonwebtoken:jjwt-jackson:0.12.6'
```

---

## 6. 프론트 설계 (`apps/web`)

### 6.1 신규 파일
```
views/auth/LoginView.vue                로그인 (download 8.png)
views/auth/AuthCallbackView.vue         콜백 착지 → refresh 1회 → 홈으로 replace
components/auth/GoogleSignInButton.vue  구글 로고 + 남색 커스텀 버튼
stores/auth.js                          accessToken(메모리) · user · needsConsent
api/auth.js                             refresh · logout · fetchMe
assets/images/google-logo.svg
```

`src/stores/`는 아직 존재하지 않는다. `stores/auth.js`가 이 프로젝트의 첫 Pinia 스토어다.

### 6.2 수정 파일 — 4개가 팀 공유 파일

| 파일 | 변경 | 영향 범위 |
|---|---|---|
| `router/index.js` | `/login`·`/auth/callback` 라우트 + `beforeEach` 가드 | 기존 5탭 전부 로그인 필수가 됨 |
| `App.vue` | `to.meta.hideTabBar`면 `TheTabBar` 렌더 생략 | 공유 레이아웃 |
| `api/http.js` | 요청에 Bearer 주입 + 401 시 refresh 1회 재시도 | 모든 API 호출이 경유 |
| `main.js` | `mount()` 전 silent refresh 1회 | 앱 부팅 경로 |

> `AGENTS.md` 규칙 5(담당 모듈 밖 파일 수정 시 사전 공지)에 따라 PR 전에 팀에 알린다.

### 6.3 라우트 가드
```js
router.beforeEach((to) => {
    const auth = useAuthStore();
    if (to.meta.public) return true;
    if (!auth.isLoggedIn) return { name: 'login', query: { redirect: to.fullPath } };
    return true;
});
```

`meta.public`은 `/login`·`/auth/callback`·`/dev/ui` 세 곳에만 붙인다.
`/dev/ui`는 이미 `import.meta.env.DEV`로 프로덕션 빌드에서 제거되며 사용자 데이터를 다루지 않는다.

개발용 인증 bypass 플래그는 만들지 않는다. 그런 플래그는 인증 버그를 가리고 운영 설정에 새어나가기 쉽다.
팀원은 개발용 OAuth 클라이언트를 공유해 `application-local.properties`에 2줄을 채우면 된다.

### 6.4 401 재시도 중복 방지
동시에 여러 요청이 401을 받으면 refresh가 N번 발생하고, **회전 방식이라 재사용 감지에 걸려
전체 토큰이 폐기된다**. `http.js`에서 진행 중인 refresh Promise를 단일 큐로 묶어
1회만 호출하고 나머지 요청은 그 결과를 기다린 뒤 재시도한다.
재시도는 요청당 1회로 제한한다(refresh 자체가 401이면 즉시 로그아웃).

### 6.5 버튼 컴포넌트 위치
Figma 버튼은 남색(`--tt-ink` 계열) 배경인데 `BaseButton`의 4개 variant(primary/secondary/ghost/danger)에
남색이 없다. variant를 추가하면 공용 컴포넌트 변경이라 다른 담당자 리뷰가 필요해지고,
구글 로고 슬롯까지 넣으면 범용 버튼이 오염된다.
`AGENTS.md`의 **3의 법칙**(세 번째 화면에서 또 필요해지면 그때 `common/`으로)에 따라
`components/auth/` 전용 컴포넌트로 둔다. `views/dev/UiCatalogView.vue` 등록은 불필요하다.

### 6.6 Figma 대비 변경 2건
1. 상단 라벨 `CPR · CASH POCKET RESCUE` → **`탕탕 · 지갑재판소`**.
   구 서비스명이므로 그대로 두지 않는다. Figma 원본 수정은 디자인 담당 몫으로 남긴다.
2. 버튼 문구 `Google로 판결 시작하기`는 구글 브랜드 가이드라인의 권장 문구("Sign in with Google" 계열)에서
   벗어나지만, 컨셉 일관성이 평가 요소이므로 Figma 문구를 유지한다.
   로고 자체는 가이드라인대로 흰 배경 원형 안에 배치한다.

### 6.7 디자인 토큰
`assets/tokens.css`의 의미 토큰만 사용한다. 색상 HEX 하드코딩 금지.
구글 로고의 브랜드 컬러는 SVG 자산 내부에 있으므로 CSS 토큰 규칙과 무관하다.

---

## 7. 에러 처리

> **구현 후 정정(2026-08-03)**: `OAUTH_STATE_MISMATCH`·`OAUTH_CANCELLED`는 **API 에러 코드로 존재하지 않는다.**
> `AuthController`의 콜백 핸들러가 이 두 상황에서 예외를 던지지 않고 바로 302 리다이렉트로 처리하기
> 때문이다(동작은 동일하고 더 단순하다). 즉 아래 표의 이 두 행은 `ApiResponse.code` 로는 절대 나타나지
> 않고, 오직 리다이렉트 쿼리(`/login?error=...`)로만 표현된다. 후속 담당자는 이 두 코드를
> `CommonExceptionAdvice`나 `BusinessException`에서 찾지 말 것.

| 코드 | 상황 | 프론트 동작 |
|---|---|---|
| `OAUTH_STATE_MISMATCH`(리다이렉트 쿼리 전용 — API 코드 아님) | state 쿠키 불일치 (CSRF 의심) | `/login?error=invalid` — 재시도 안내 |
| `OAUTH_CANCELLED`(리다이렉트 쿼리 전용 — API 코드 아님) | 구글 동의 화면에서 사용자가 취소 | `/login` 조용히 복귀 |
| `OAUTH_TOKEN_EXCHANGE_FAILED` | code→token 교환 실패 | `/login?error=failed` |
| `TOKEN_EXPIRED` | 액세스 토큰 만료 | refresh 자동 재시도 (6.4) |
| `INVALID_TOKEN` | 서명 위조·형식 오류 | 즉시 로그아웃 → `/login` |
| `REFRESH_TOKEN_REUSED` | 폐기된 리프레시 토큰 재사용 | 전체 로그아웃 → `/login?error=security` |
| `USER_WITHDRAWN` | `tbl_user.status != 'ACTIVE'` | `/login?error=withdrawn` |

업무 규칙 위반은 `BusinessException(code, message)`로 던져 400으로 자동 변환한다.
인증 실패(401)는 `JwtAuthInterceptor`가 `ApiResponse` 실패 포맷으로 직접 응답한다.
전역 예외 처리는 `common/exception/CommonExceptionAdvice` 한 곳에서만 한다.

---

## 8. 보안 설계

| 항목 | 조치 |
|---|---|
| CSRF (OAuth) | `state` 파라미터 32byte 랜덤 + httpOnly 쿠키 대조, 5분 만료, 1회용 |
| 리프레시 토큰 저장 | 원문 미저장. SHA-256 해시만 `tbl_refresh_token.token_hash`에 보관 |
| 리프레시 토큰 탈취 | 회전 + 재사용 감지 시 사용자 전체 토큰 폐기 |
| 액세스 토큰 노출 | 메모리(Pinia)에만 보관. localStorage·URL·쿠키에 쓰지 않음 |
| 토큰 수명 | Access 15분 / Refresh 14일 |
| 쿠키 속성 | `HttpOnly` · `Secure` · `Path=/api/auth` · `SameSite`는 설정값 |
| SQL Injection | MyBatis `#{}` 만 사용. `${}` 금지 (팀 규칙) |

### 8.1 SameSite를 설정값으로 분리하는 이유
로컬은 Vite 프록시(`:5173 → :8080`)로 same-origin이라 `Lax`로 충분하다.
그러나 운영에서 프론트(Vercel)와 API 도메인이 갈리면 `Lax`는 크로스사이트 XHR에 쿠키를 싣지 않아
**`/refresh`가 통째로 실패**한다. `auth.cookie.same-site` 프로퍼티로 분리해
로컬 `Lax` / 운영 `None`을 환경별로 선택한다.

### 8.2 미해결 — 운영 도메인 통합 (이번 범위 밖)
현재 Vercel 프론트와 API가 서로 다른 도메인이고 CORS(`allowCredentials=true`)로 연결돼 있다.
이 구조에서 리프레시 쿠키는 **서드파티 쿠키**가 되어 최신 크롬에서 차단될 수 있다.

- **이번 이슈의 완료 기준은 로컬 동작까지**로 한다.
- 운영 대응은 `/api`를 프론트 도메인 아래로 프록시하는 것(Vercel rewrites 또는 nginx)이며,
  별도 이슈로 분리한다. Vercel rewrites는 대상이 HTTPS여야 하므로 백엔드 TLS 여부를 먼저 확인해야 한다.

---

## 9. 테스트

기능 구현에 단위 테스트를 동반한다(팀 규칙). JUnit5 + Spring Test.

| 테스트 | 검증 내용 |
|---|---|
| `JwtProviderTest` | 발급·검증·만료 처리·위조 서명 거부 |
| `RefreshTokenServiceTest` | 회전 시 구토큰 revoke / 재사용 시 전체 폐기 |
| `AuthServiceTest` | 신규가입 INSERT 경로 vs 기존 로그인 경로 (구글 클라이언트·매퍼 목) |
| `AuthControllerTest` | MockMvc로 302 `Location` 헤더 · `Set-Cookie` 속성 검증 |

실제 DB 연결이 필요한 매퍼 테스트는 `@Disabled`로 둔다(활성화한 채 커밋하면 팀원 빌드가 깨진다).

프론트는 현재 테스트 러너가 설정돼 있지 않다. 이번 이슈에서 러너를 도입하지 않고,
`npm run lint:check`와 `npm run format:check` 통과를 완료 기준에 포함한다.

---

## 10. 완료 기준

- [ ] `db/schema.sql`·`db/seed.sql` 실행으로 33테이블 + 시드 생성
- [ ] `/login`에서 구글 계정으로 로그인 → 홈(`/`) 진입
- [ ] 신규 계정 로그인 시 `tbl_user`에 행이 생성되고 `difficulty_id=1`
- [ ] 새로고침 후에도 로그인 유지 (silent refresh)
- [ ] 미로그인 상태로 `/asset` 직접 접근 시 `/login`으로 리다이렉트
- [ ] 로그아웃 API 호출 후 쿠키 만료 + DB `is_revoked=1`
- [ ] 폐기된 리프레시 토큰 재사용 시 전체 토큰 폐기
- [ ] URL·localStorage 어디에도 토큰이 남지 않음 (DevTools 확인)
- [ ] 백엔드 테스트 4종 통과 (`./gradlew :apps:api:test`)
- [ ] `npm run lint:check` · `npm run format:check` 통과
- [ ] `db/00_init_local_db.sql`이 `CHANGE_ME_DB_PASSWORD` 상태로 커밋됨

---

## 11. 후속 작업

| 항목 | 사유 |
|---|---|
| 운영 도메인 통합 (`/api` 프록시) | 8.2 — 서드파티 쿠키 차단 대응 |
| Figma 로그인 화면의 `CPR` 표기 수정 | 6.6 — 구 서비스명. 디자인 담당 |
| 약관 동의 화면 (`needsConsent` 게이트 연결) | 후속 이슈 |
| 계좌 연동 3화면 | 후속 이슈 |
| 마이페이지 로그아웃 UI | API는 이번에 제공, 화면은 마이페이지 소관 |
| 프론트 테스트 러너 도입 | 9절 — 팀 논의 필요 |
