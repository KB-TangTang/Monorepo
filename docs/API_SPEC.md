# API 규격

모든 응답은 공통 래퍼 `com.kb.tangtang.common.dto.ApiResponse` 로 감싼다.

```
성공  { "success": true,  "data": { ... } }
실패  { "success": false, "code": "NOT_FOUND", "message": "..." }
```

인증이 필요한 요청은 `Authorization: Bearer <accessToken>` 헤더를 보낸다.
리프레시 토큰은 httpOnly 쿠키(`refresh_token`, `Path=/api/auth`)로만 오간다.

## 공통

| 메서드 | 경로 | 인증 | 응답 |
|---|---|---|---|
| GET | `/api/health` | 불필요 | `{ status, service }` |

## 인증 (이슈 #9)

| 메서드 | 경로 | 인증 | 응답 |
|---|---|---|---|
| GET | `/api/auth/google` | 불필요 | 302 → 구글 동의 화면. `oauth_state` 쿠키 발급 |
| GET | `/api/auth/google/callback` | 불필요 | 302 → 프론트. 성공 시 `/auth/callback` + `refresh_token` 쿠키, 실패 시 `/login?error=...` |
| POST | `/api/auth/refresh` | 쿠키 | `{ accessToken, user: 사용자정보, needsConsent, needsFinancialConsent, needsAccountLink }` |
| POST | `/api/auth/logout` | 쿠키 | `{"success":true,"data":null}` + 쿠키 만료 |
| GET | `/api/users/me` | Bearer | 사용자정보 |
| PATCH | `/api/users/me/name` | Bearer | 요청 `{ name }` → 갱신된 사용자정보 |
| PATCH | `/api/users/me/nickname` | Bearer | 요청 `{ nickname }` → 갱신된 사용자정보 |

**사용자정보** = `{ id, nickname, socialName, displayName, name, email, socialProvider, tutorialSeenAt, groupTutorialSeenAt }`

**이 모양은 `GET /api/users/me` · `POST /api/auth/refresh` 의 `user` · 사용자 정보를 바꾸는 모든
`PATCH` 응답이 똑같이 쓴다.** 서버는 `UserMeDto.from(UserDto)` 한 곳에서만 만든다 — 경로마다
따로 조립하면 필드를 추가할 때 한 곳을 빠뜨려 **그 경로에서만 값이 비는** 버그가 난다.

- `name` 은 **실명(본인확인용)** 이고 `nickname` 은 표시명이다. 서로 다른 컬럼·다른 엔드포인트다.
- `PATCH /api/users/me/name` 은 **간편인증 화면이 인증 요청 직전에** 부른다. 같은 화면에서 받는
  생년월일·통신사·휴대폰은 여기로 오지 않는다 — 저장하지 않는 값이기 때문이다.
  (`DECISIONS.md` 2026-08-11 (4))
- 검증 규칙: 앞뒤 공백 제거 후 **2~50자**, **한글·영문·공백만**. 어기면 `INVALID_NAME`.

### 온보딩 게이트 — 순서가 정해져 있다

신규 사용자는 **`동의 → 금융동의 → 계좌연동 → 닉네임 → 홈`** 을 순서대로 통과한다.
프론트 라우터 가드는 **부팅 시 `POST /api/auth/refresh` 응답 하나만 보고** 다음 단계를 정한다 —
화면 진입마다 조회하지 않는다.

| 플래그 | true 이면 보낼 화면 | 판정 |
|---|---|---|
| `needsConsent` | 서비스 동의 | `SIGNUP` 필수 동의(약관·개인정보·금융정보) 미완료 |
| `needsFinancialConsent` | 금융(제3자 제공) 동의 | `THIRD_PARTY` 동의 미완료 |
| `needsAccountLink` | 계좌 연동 | 활성 연결 계좌가 0개 |
| — | 닉네임 설정 | `user.nickname` 이 `null` (별도 플래그 없음) |

- ⚠ **`THIRD_PARTY` 를 `needsConsent` 에 합치면 안 된다.** 그러면 계좌를 아직 연동하지 않은
  사용자가 가입 동의 화면을 영원히 벗어나지 못한다. 그래서 단계를 나눈다.
- **서버도 검사한다.** 계좌 연결 요청(`simple-auth` · `connections`)은 `THIRD_PARTY` 동의가 없으면
  **`CONSENT_REQUIRED`(400)** 로 거부한다. 계좌 연동 진입점이 셋이라 화면 순서만으로는 우회된다 —
  **2026-08-11 까지 실제로 아무도 제3자 제공 동의를 하지 않은 채 연동해 왔다.**
- `POST /api/consents` 와 철회 응답도 `{ needsConsent, needsFinancialConsent }` 를 함께 준다.
  저장 직후 게이트를 갱신하지 않으면 동의를 마쳐도 계속 동의 화면으로 되돌아간다.
  (`DECISIONS.md` 2026-08-11 (7))

### 닉네임 — 이름 3종을 헷갈리지 말 것 (이슈 #110)

| 필드 | 컬럼 | 누가 채우나 | 뜻 |
|---|---|---|---|
| `nickname` | `tbl_user.nickname` | 사용자 (온보딩·마이페이지) | 표시명. **`null` 이면 온보딩 미완료** |
| `socialName` | `tbl_user.social_name` | 가입 시 자동 (구글 `name`) | 입력창 prefill · 표시명 fallback |
| `name` | `tbl_user.name` | 사용자 (간편인증 화면) | **실명(본인확인용).** 표시명으로 쓰지 않는다 |

- **표시명 = `nickname ?? socialName`** 이고 **서버가 `displayName` 으로 계산해 내려준다.**
  화면마다 계산하면 어디선 닉네임, 어디선 구글 이름이 나온다.
- `PATCH /api/users/me/nickname` 은 **온보딩(`AU_03_01`)과 마이페이지 수정(`MY_01_03`)이 함께 쓴다.**
- 검증: 앞뒤 공백 제거 후 **1~50자**(빈 값·공백만 불가). 어기면 `INVALID_REQUEST`.
  실명보다 느슨하다 — 표시명이라 한 글자도, 숫자·기호도 막을 이유가 없다.
- **중복 검사를 하지 않는다.** 닉네임 중복 허용이 팀 결정이라 `UNIQUE` 도 걸지 않았다
  (`DECISIONS.md` 2026-08-11 닉네임 온보딩).
- **온보딩 완료 여부는 `nickname` 이 `null` 인지로만 판별한다. 별도 판별 API 를 두지 않는다.**
- 가입 시 `nickname` 은 **비워 둔다.** 구글 이름은 `social_name` 에 넣는다 —
  미리 채우면 "설정함"과 "안 함"을 구분할 별도 플래그가 필요해진다.

### 튜토리얼 완료 플래그 (이슈 #128)

| 메서드 | 경로 | 인증 | 뜻 |
|---|---|---|---|
| PATCH | `/api/main-challenge/tutorial/complete` | Bearer | 메인(개인·대법원) 튜토리얼 `MC_01_05` 완료 |
| DELETE | `/api/main-challenge/tutorial/complete` | Bearer | 다시 보기 — 완료 시각을 지운다 |
| PATCH | `/api/group-challenge/tutorial/complete` | Bearer | 그룹(지방법원) 튜토리얼 `GC_01_01` 완료 |
| DELETE | `/api/group-challenge/tutorial/complete` | Bearer | 다시 보기 |

- 네 개 모두 응답은 **갱신된 사용자정보**다. 프론트는 이 응답으로 스토어를 갱신한다.
- 저장 위치는 `tbl_user.tutorial_seen_at`(메인) · `tbl_user.group_tutorial_seen_at`(그룹).
  **`null` 이면 아직 안 본 것**이고, 프론트는 이 값만 보고 노출을 정한다.
  로그인 시 `POST /api/auth/refresh` 응답에 이미 실려 오므로 **화면 진입마다 조회하지 않는다.**
- `localStorage` 를 쓰지 않는다 — 기기를 바꾸면 튜토리얼이 다시 뜬다
  (`DECISIONS.md` 2026-08-11 RV-108 에서 기각된 대안이다).
- 클래스는 `user` 모듈(`user/controller/TutorialController`)에 있다. 경로는 챌린지 도메인이지만
  값이 사는 테이블이 `tbl_user` 라서다.

### 인증 에러 코드

| 코드 | HTTP | 의미 |
|---|---|---|
| `UNAUTHORIZED` | 401 | Authorization 헤더 없음/형식 오류 |
| `TOKEN_EXPIRED` | 401 | 액세스 토큰 만료 — 프론트가 자동 재발급한다 |
| `INVALID_TOKEN` | 401·400 | 서명 위조·형식 오류·리프레시 토큰 없음/만료 |
| `REFRESH_TOKEN_REUSED` | 400 | 폐기된 리프레시 토큰 재사용 — 전체 토큰 폐기됨 |
| `USER_WITHDRAWN` | 400 | 탈퇴·차단 계정 |
| `OAUTH_TOKEN_EXCHANGE_FAILED` | 400 | 구글 code↔token 교환 실패 |
| `NOT_FOUND` | 400 | `/api/users/me` 조회 시 사용자를 찾을 수 없음 (실명 갱신 대상이 탈퇴·차단 상태일 때도 이 코드다) |
| `INVALID_NAME` | 400 | `/api/users/me/name` 의 이름이 형식에 맞지 않음 (2~50자·한글/영문/공백) |
| `INVALID_REQUEST` | 400 | `/api/users/me/nickname` 의 닉네임이 비었거나 50자를 넘음 |

### 콜백 리다이렉트 error 쿼리 (`/api/auth/google/callback` 이 붙이는 값)

`cancelled`(사용자 취소) · `invalid`(state 불일치) · `failed`(교환 실패) · `withdrawn`(이용 불가 계정)

### 프론트 재발급 실패 시 error 쿼리 (`http.js`/`main.js` 가 붙이는 값)

콜백이 아니라 액세스 토큰 재발급(`/api/auth/refresh`) 실패 시 프론트가 붙인다.

`expired`(단순 세션 만료) · `security`(`REFRESH_TOKEN_REUSED` 감지 — 탈취 의심으로 전체 토큰 폐기됨)

## 동의 (이슈 #13)

| 메서드 | 경로 | 인증 | 응답 |
|---|---|---|---|
| GET | `/api/consents/catalog?scope=SIGNUP\|FINANCIAL` | Bearer | `{ scope, termsVersion, items:[{type,required,label,termsUrl}] }` |
| POST | `/api/consents` | Bearer | `{ needsConsent }` — 본문 `{ scope, agreements:[{type,agreed}] }` |
| GET | `/api/consents/me` | Bearer | `{ items:[{type,scope,required,label,termsUrl,agreed,withdrawable,termsVersion,expiresAt}] }` |
| POST | `/api/consents/{type}/withdraw` | Bearer | `{ needsConsent }` |

동의 그룹은 2종이다.

| scope | 항목 |
|---|---|
| `SIGNUP` | 필수 `TERMS`·`PRIVACY`·`FINANCIAL_DATA` / 선택 `AI_USAGE`·`MARKETING` |
| `FINANCIAL` | 필수 `THIRD_PARTY` |

약관 본문은 서버가 갖지 않는다. `termsUrl` 은 노션 공개 페이지이고 새 탭으로 연다.
`terms_version` 은 요청값을 무시하고 서버 카탈로그 값을 저장한다.
`needsConsent` 는 `SIGNUP` 필수 3종 기준으로만 판정한다.

동의를 기록하려면 `agreed: true` 를 명시적으로 보내야 한다. `agreed` 를 생략하거나 `null` 로 보내면 원시형 기본값 `false` 로 바인딩되어, 해당 항목을 요청에 넣지 않은 것과 동일하게 미동의로 저장된다.

`FINANCIAL_DATA` 철회는 부수효과가 있다. `THIRD_PARTY` 를 **함께 철회**하고 `ConsentWithdrawnEvent` 를 발행해
**연결된 계좌를 모두 해제**한다. 또 `SIGNUP` 필수 항목이라 철회 즉시 `needsConsent=true` 가 되어
라우터 가드가 이후 모든 이동을 동의 화면으로 되돌린다 — 화면은 철회 전에 이 사실을 알려야 한다.

### 철회한 동의를 다시 켜기 (재동의)

전용 엔드포인트가 없다. **기존 `POST /api/consents` 를 그대로 재사용**한다.

- `POST /api/consents` 는 **scope 단위 전량 저장**이다(`ConsentService.submit`).
  다시 켤 항목 하나만 보내면 **같은 scope 의 나머지가 전부 꺼진다.**
  현재 상태를 함께 실어 보내야 한다 (프론트 `utils/consent.js` 의 `buildAgreeAgainPayload`).
- 그래서 `GET /api/consents/me` 가 항목마다 **`scope`** 를 내려준다.
  프론트가 타입→scope 매핑을 들고 있으면 서버의 `ConsentScope` 가 바뀔 때 어긋난다.
- `TERMS`·`PRIVACY` 는 `withdrawable=false` 라 철회 자체가 없다.

### 동의 에러 코드

| 코드 | HTTP | 의미 |
|---|---|---|
| `CONSENT_REQUIRED_MISSING` | 400 | 필수 항목에 동의하지 않고 저장 시도 |
| `CONSENT_TYPE_INVALID` | 400 | 알 수 없는 동의 항목·그룹이거나 해당 scope 밖의 항목 |
| `CONSENT_NOT_WITHDRAWABLE` | 400 | `TERMS`·`PRIVACY` 철회 시도 (탈퇴에 해당) |
| `NOT_FOUND` | 400 | 철회할 동의 내역 없음 |

## 계좌 연동 (이슈 #12)

기준 파일은 `AccountLinkController` 이고, 프론트 `apps/web/src/api/account.js` 의 함수 13종과 1:1 이다.
사용자 식별은 전부 `@LoginUser` 로만 한다 — 요청 본문·쿼리로 `userId` 를 받는 엔드포인트는 없다.

**어느 금융 데이터 공급자(목서버 / 실 CODEF)가 붙었는지 프론트는 알지 못한다.**
`financial.client` 프로퍼티가 정하고, 화면은 `GET /api/accounts/link/auth-methods` 응답만 보고 인증 화면을 고른다.
(DECISIONS.md 2026-08-05)

### 1단계 — 기관 선택

| 메서드 | 경로 | 인증 | 응답 |
|---|---|---|---|
| GET | `/api/accounts/institutions` | Bearer | `{ banks:[], cards:[], securities:[] }` — 각 항목 `{code,name,shortLabel,connected}` |

- `connected: true` 는 **이미 연결된 기관**이라 다시 고를 수 없다.
- 공급자가 다루지 못하는 기관은 목록에서 아예 빠진다(`supportedOrganizations`. 빈 집합이면 "제한 없음").
- **보험은 연동 범위에서 제외됐다(2026-08-06).** 되살리려면 `InstitutionCatalog` 부터 손봐야 한다.

### 2단계 — 인증

| 메서드 | 경로 | 인증 | 요청 본문 | 응답 |
|---|---|---|---|---|
| GET | `/api/accounts/link/auth-methods` | Bearer | — | `{ methods:[{type,providers:[{code,name}],requiresIdentity}] }` |
| POST | `/api/accounts/connections/simple-auth` | Bearer | `{provider, organizations:[code], userName?, birthDate?, carrier?, phoneNo?}` | 연결 응답 |
| POST | `/api/accounts/connections` | Bearer | `{credentials:[{organization,loginType,id,password}]}` | 연결 응답 |
| GET | `/api/accounts/connections/{connectionId}/auth-status` | Bearer | — | `{ status }` |
| POST | `/api/accounts/connections/{connectionId}/extra-auth` | Bearer | `{authCode}` | 연결 응답 |

연결 응답 = `{ connectionId, status, expiresInSeconds, needsExtraAuth, extraAuthType }`
(간편인증·기관 로그인이 **같은 모양**을 돌려준다. 이후 단계는 어느 경로로 왔는지 모른다.)

- `type` 이 `SIMPLE_AUTH` 면 `providers` 가 채워지고, `INSTITUTION_LOGIN` 이면 빈 배열이다.
- 본인 정보(`userName`·`birthDate`·`carrier`·`phoneNo`)는 **`requiresIdentity: true` 일 때만** 보낸다.
  목 모드에서는 화면이 형식만 검증하고 폐기해 필드가 비어 온다. 서버도 공급자에 전달만 하고 저장·로깅하지 않는다.
  - ⚠ **이름만 예외다.** 화면이 입력받은 이름은 이 요청과 별개로 `PATCH /api/users/me/name` 이
    `tbl_user.name` 에 저장한다. 이 요청의 `userName` 은 여전히 **공급자 전달용**이고 저장되지 않는다.
    (`DECISIONS.md` 2026-08-11 (4) — 목 모드는 `requiresIdentity: false` 라 이 페이로드에만 실으면
    시연 환경에서 실명이 영영 저장되지 않는다)
- **`credentials` 는 요청 처리 중에만 존재한다.** 진행 상태·DB·로그 어디에도 남지 않는다.
- 인증 승인은 프론트가 `auth-status` 를 1초 간격으로 폴링해 확인한다.

### 3단계 — 조회 진행

| 메서드 | 경로 | 인증 | 응답 |
|---|---|---|---|
| GET | `/api/accounts/connections/{connectionId}/progress` | Bearer | `{ percent, done, institutions:[{code,name,status}] }` |

- **진행률은 서버가 계산한다. 프론트 연출이 아니다.**
- 이 엔드포인트는 **폴링될 때마다 기관을 하나씩 실제로 조회**한다(브로커·비동기 인프라가 없는 구조라 의도한 설계).
  한 기관이 실패해도 `FAILED` 로 두고 다음 기관으로 넘어간다.
- 승인(`APPROVED`) 전에는 조회를 시작하지 않고 현재 상태만 돌려준다.
- 진행 상태는 메모리 보관이고 **TTL 10분**이다. 지나면 `CONNECTION_NOT_FOUND` 다.

### 4단계 — 계좌 선택

| 메서드 | 경로 | 인증 | 응답 |
|---|---|---|---|
| GET | `/api/accounts/connections/{connectionId}/accounts` | Bearer | `[{bankCode,bankName,shortLabel,accounts:[…]}]` |

`accounts[]` 항목 = `{accountId, bankCode, bankName, accountType, accountName, accountNoMasked, currency, balance, alreadyLinked}`

- 이 엔드포인트만 `data` 가 **배열**이다(공통 래퍼는 동일).
- `accountId` 는 아직 DB 에 없는 **목록 순번 기반 임시 식별자**다. 5단계 요청에만 쓰고 보관하지 않는다.
- `accountNoMasked` 는 서버가 마스킹한 값이다. 원본 계좌번호는 내려보내지 않는다.
- `alreadyLinked: true` 는 이미 연결된 계좌다(계좌번호 해시로 판정).

### 5단계 — 연결 저장

| 메서드 | 경로 | 인증 | 요청 본문 | 응답 |
|---|---|---|---|---|
| POST | `/api/accounts` | Bearer | `{connectionId, accountIds:[임시 id]}` | `{ linkedCount }` |

- 중복 `accountId`·이미 연결된 계좌는 **조용히 건너뛴다**. `linkedCount` 는 실제로 연결된 수다.
- 해제했던 계좌는 새 행을 만들지 않고 되살린다(UNIQUE KEY 충돌 방지).
- 하나도 연결되지 않으면 `EXTERNAL_API_ERROR` 다.

### 연결 계좌 관리

| 메서드 | 경로 | 인증 | 응답 |
|---|---|---|---|
| GET | `/api/accounts` | Bearer | `{ accounts:[…], nextAutoSyncAt }` |
| POST | `/api/accounts/refresh` | Bearer | `{ lastSyncAt, institutionCount, cooldownSeconds, institutions:[…] }` |
| DELETE | `/api/accounts/{accountId}` | Bearer | `{ accountId, disconnected }` |
| POST | `/api/accounts/{accountId}/resync` | Bearer | `{ accountId, syncStatus, lastSyncAt }` |

`accounts[]` 항목 = `{accountId, bankCode, bankName, shortLabel, accountName, accountNoMasked, accountType, balance, syncStatus, lastSyncAt, syncFailReason, expiresAt}`
`institutions[]` 항목 = `{bankCode, bankName, shortLabel, newTransactionCount, balance, syncStatus}`

- `nextAutoSyncAt` — 자동 동기화는 **매일 18시 배치**다. 오늘 시각이 지났으면 내일 18시를 안내한다.
- `expiresAt` — 연결 시점 **+365일**(마이데이터 재동의 주기).
- `cooldownSeconds` 는 **150 고정이고 서버가 판단한다.** 프론트가 직접 계산하지 않는다.
- `POST /refresh` 는 **공급자를 실제로 호출한다.** 기관 단위로 묶어 한 번씩 부르고,
  `institutions[].balance` 는 그 기관 계좌들의 **잔액 합계**다(첫 계좌 값이 아니다).
- `newTransactionCount` 는 거래내역 수집이 붙기 전까지 **항상 0** 이다. 값을 지어내지 않는다.
- 조회 결과 `NEED_RECONNECT` 가 되면 `AccountReconnectRequiredEvent` 가 발행되어
  `ACCOUNT_RECONNECT` 알림이 나간다(→ 「알림」 절).

### 계좌 연동 열거값

| 대상 | 값 |
|---|---|
| 인증 수단 `type` | `SIMPLE_AUTH` · `INSTITUTION_LOGIN` |
| 인증 상태 `status` | `PENDING` · `APPROVED` · `FAILED` · `EXPIRED` |
| 진행 상태 `institutions[].status` | `WAITING` · `FETCHING` · `DONE` · `FAILED` |
| 동기화 상태 `syncStatus` | `NORMAL` · `SYNCING` · `NEED_SYNC` · `FAILED` · `NEED_RECONNECT` |

`syncStatus` 값은 `tbl_connected_account.sync_status` 컬럼과 1:1 이다 (`db/schema.sql` 이 기준).

### 계좌 연동 에러 코드

| 코드 | HTTP | 의미 |
|---|---|---|
| `INVALID_CREDENTIALS` | 400 | 인증 수단·아이디/비밀번호·인증번호·본인 정보 누락 또는 형식 오류 |
| `CONNECTION_NOT_FOUND` | 400 | `connectionId` 가 없거나 TTL(10분) 만료, 또는 남의 연결 |
| `TOKEN_EXPIRED` | 400 | **금융기관** 연결 인증 만료 — 재연동이 필요하다 |
| `AUTH_METHOD_UNSUPPORTED` | 400 | 공급자가 지원하지 않는 인증 수단 |
| `EXTERNAL_API_ERROR` | 400 | 기관·계좌 미선택, 공급자 응답 오류 |
| `EXTERNAL_API_UNAVAILABLE` | 400 | 공급자 장애·무응답 |
| `NOT_FOUND` | 400 | 연결된 계좌 없음 또는 남의 계좌 |

> ⚠ `TOKEN_EXPIRED` 는 인증 절의 액세스 토큰 만료와 **이름만 같다.** 이쪽은 HTTP 400 이라
> 프론트 `http.js` 의 자동 재발급(401 트리거)을 유발하지 않는다.

## 금융 데이터 동기화 (이슈 #147)

| 메서드 | 경로 | 인증 | 응답 |
|---|---|---|---|
| POST | `/api/financial-syncs` | Bearer | `{ status, syncedSources, syncedAt, collectedTransactionCount, ruleCategorizedCount, llmPendingTransactionCount, llmCategorizationStatus }` |

```json
{
  "success": true,
  "data": {
    "status": "COMPLETED",
    "syncedSources": ["BANK", "DEPOSIT", "SECURITIES", "LOAN", "PAY_MONEY", "CARD"],
    "syncedAt": "2026-08-13T10:00:00+09:00",
    "collectedTransactionCount": 42,
    "ruleCategorizedCount": 30,
    "llmPendingTransactionCount": 12,
    "llmCategorizationStatus": "PENDING"
  }
}
```

- `collectedTransactionCount`·`ruleCategorizedCount`·`llmPendingTransactionCount` 는 전부 **이번 호출 한 번에서 upsert 된 거래 기준**이다(사용자의 누적 미분류 거래 수가 아니다).
- 저장이 끝난 뒤 규칙 1~4단계(사용자 가맹점 매핑 → 공용 가맹점 매핑 → MCC/업종명 → 키워드)로 **동기** 카테고리화한다. `ruleCategorizedCount` 는 이 응답 안에서 확정된 값이다.
- 규칙으로도 분류하지 못한 소비 거래는 이 응답 이후 **비동기**로 LLM 분류 작업(`tbl_llm_categorization_job`, 사용자별 transaction_date 오름차순 최대 20건 배치)에 등록된다. 실제 LLM 호출은 이번 범위에 포함되지 않는다 — 작업 등록까지만 한다.
- `llmCategorizationStatus`: `PENDING`(LLM 대상 거래가 있음) · `NOT_REQUIRED`(전부 규칙으로 분류됐거나 대상 자체가 없음).
- 사용자가 직접 지정한 카테고리(`category_source='USER'`)는 재동기화로 자동 분류가 절대 덮어쓰지 않는다(DB 레벨 가드).
- 환불 거래(`is_refund=1`)는 일반 거래와 동일하게 규칙 1~4단계를 적용하되, 전부 미스하면 LLM 대상에서는 제외한다. 원거래 카테고리를 그대로 물려받는(계승) 처리는 **후속 작업**이다.
- `tbl_merchant_category_map`(공용 캐시)에 대한 write-back(3·4단계 판정 결과를 다시 채워 넣는 것)은 이번 범위가 아니다 — 비어 있으면 2단계는 항상 미스로 3단계로 넘어간다.
- 정기 동기화 스케줄러, 연결된 전체 사용자 대상 자동 동기화, 증분 동기화 조회 범위 변경도 이번 범위 밖이다.

### LLM 분류 작업 처리 (후속 구현)

`tbl_llm_categorization_job`에 `PENDING`으로 등록된 작업은 별도 API 호출 없이 서버 내부 스케줄러(`LlmCategorizationScheduler`)가 주기적으로 집어 OpenAI Chat Completions API로 실제 분류를 수행한다.

- 주기: `llm.categorization.poll.fixed-delay-ms`(기본 1분). 한 번에 최대 `llm.categorization.poll.max-jobs-per-tick`(기본 20)개 작업을 처리한다.
- 분류 결과는 `category_source='LLM'`으로 `tbl_transaction.category_id`에 반영된다. 사용자 지정(`USER`) 카테고리는 기존 DB 가드로 보호된다.
- LLM이 제공된 카테고리 목록에 없는 id를 응답하면 그 거래는 반영하지 않고 건너뛴다(환각 방지).
- 작업 상태는 `PENDING → PROCESSING → COMPLETED`(정상 처리, 개별 거래가 분류 안 됐어도 정상 종료) 또는 `PROCESSING → FAILED`(API 호출 자체가 실패)로 전이한다. **`FAILED` 작업은 이번 범위에서 자동 재시도하지 않는다** — 후속 작업.
- `openai.api.key`는 `application-local.properties`에서만 설정한다(팀 공용 키는 팀 채널에서 배포).

## 마이페이지 (이슈 #57)

전용 엔드포인트를 새로 만들지 않았다. 아래 기존 API 를 조합해 그린다.

| 화면 | 쓰는 API |
|---|---|
| 프로필 카드 (`/my`) | `GET /api/users/me` — `{id, nickname, name, email, socialProvider}` |
| 동의 관리 (`/my/consents`) | `GET /api/consents/me` · `POST /api/consents/{type}/withdraw` · `POST /api/consents` (재동의) |
| 로그아웃 | `POST /api/auth/logout` |

- `socialProvider` 는 마이페이지가 "google · 이메일" 형식으로 표시하려고 추가한 필드다.
- 등급은 화면에 표시하지 않는다 (DECISIONS.md 2026-08-06).
- 재동의 흐름은 「동의」 절의 *철회한 동의를 다시 켜기* 를 따른다.

## 메인 챌린지 카테고리 분석 (이슈 #119)

| 메서드 | 경로 | 인증 | 응답 |
|---|---|---|---|
| GET | `/api/missions/categoryAnalysis` | Bearer | 최근 28일 상대형 미션 대상 소비 상위 3개 |

응답은 `{ analysisStartDate, analysisEndDate, transactionCount, relativeEligible, topCategories }` 다.
`topCategories[]` 항목은 `{ rank, categoryId, parentCategoryName, categoryName, totalAmount, transactionCount, spendingRatio }` 형태다.

- 분석 기간은 오늘을 제외한 최근 28일이다.
- 거래 데이터가 28일 이상이고 최근 28일 정상 소비가 50건 이상일 때만 상위 카테고리를 집계한다.
- 미션 대상은 `tbl_mission_pool`에 `RELATIVE` 행이 존재하는 소분류다. 현재 정책은 15개다.
- 환불은 거래 건수에서 제외하고 순소비금액에서 차감한다.
- 대상 카테고리의 양수 순소비금액이 없으면 `relativeEligible=false`, `topCategories=[]`를 반환한다.
- 소비금액, 거래 건수, 카테고리 ID 순으로 정렬해 최대 3개를 반환한다.
- `spendingRatio`의 분모는 최근 28일 전체 분류 소비의 순소비금액이다.

## 알림 (이슈 #58)

| 메서드 | 경로 | 인증 | 응답 |
|---|---|---|---|
| GET | `/api/notifications?cursor=&size=` | Bearer | `{ items:[{id,type,title,content,deepLinkUrl,isRead,createdAt}], nextCursor, unreadCount }` |
| GET | `/api/notifications/unread-count` | Bearer | `{ unreadCount }` |
| POST | `/api/notifications/{id}/read` | Bearer | `{ unreadCount }` |
| POST | `/api/notifications/read-all` | Bearer | `{ unreadCount }` |
| GET | `/api/notifications/stream` | Bearer | SSE. `connected` 1회 → 이후 `notification` 이벤트 |

- `cursor` 없으면 최신부터. 있으면 `id < cursor` 부터 `id DESC`
- `nextCursor` 는 마지막 항목의 `id`. 더 없으면 `null`
- `size` 기본 20 · 최대 50 (초과는 50 으로 자른다)
- SSE 는 스트림이라 `ApiResponse` 로 감싸지 않는다. 15초마다 `: ping` 주석 프레임을 보낸다
- `notification` 이벤트의 `data` 는 **목록 항목과 완전히 같은 모양**이다
  (`{id,type,title,content,deepLinkUrl,isRead,createdAt}`) — 변환은 `NotificationDto.from` 한 곳뿐
- `isRead` 는 boolean. Lombok getter 를 Jackson 이 `read` 로 바꾸지 않도록 `@JsonProperty("isRead")` 로 고정돼 있다
- `createdAt` 은 `yyyy-MM-ddTHH:mm:ss` (ISO-8601, 초 단위). 푸시된 알림도 서버가 값을 채워 내려보낸다

### 알림 종류 (`type`)

`tbl_notification.type` 컬럼(VARCHAR(30))에 그대로 들어가는 값이다.

| 값 | 기본 제목 | 발행 주체 |
|---|---|---|
| `ACCOUNT_RECONNECT` | 계좌 재연동이 필요해요 | `AccountReconnectRequiredEvent` (**현재 유일하게 실제 발행됨**) |
| `GROUP_JUDGMENT` | 판결이 확정됐어요 | challenge — 백엔드 미구현 |
| `GROUP_TRIAL_OPENED` | 재판이 열렸어요 | challenge — 백엔드 미구현 |
| `MISSION_DEADLINE` | 오늘 미션 마감 임박 | mission — 백엔드 미구현 |
| `MONTHLY_REPORT` | 판결문이 도착했어요 | report — 백엔드 미구현 |
| `PAYMENT_DUE` | 결제 예정 알림 | fixedexpense — 백엔드 미구현 |

미구현 종류는 `db/seed_notification_demo.sql` 로만 들어간다 (DECISIONS.md 2026-08-06 (3)).

### 알림 에러 코드

| 코드 | HTTP | 상황 |
|---|---|---|
| `NOT_FOUND` | 400 | 없는 알림 또는 남의 알림 (구분해 알려주지 않는다) |
