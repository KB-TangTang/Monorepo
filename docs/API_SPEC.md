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
| POST | `/api/auth/refresh` | 쿠키 | `{ accessToken, user: { id, nickname, email }, needsConsent }` |
| POST | `/api/auth/logout` | 쿠키 | `{"success":true,"data":null}` + 쿠키 만료 |
| GET | `/api/users/me` | Bearer | `{ id, nickname, email, socialProvider }` |

### 인증 에러 코드

| 코드 | HTTP | 의미 |
|---|---|---|
| `UNAUTHORIZED` | 401 | Authorization 헤더 없음/형식 오류 |
| `TOKEN_EXPIRED` | 401 | 액세스 토큰 만료 — 프론트가 자동 재발급한다 |
| `INVALID_TOKEN` | 401·400 | 서명 위조·형식 오류·리프레시 토큰 없음/만료 |
| `REFRESH_TOKEN_REUSED` | 400 | 폐기된 리프레시 토큰 재사용 — 전체 토큰 폐기됨 |
| `USER_WITHDRAWN` | 400 | 탈퇴·차단 계정 |
| `OAUTH_TOKEN_EXCHANGE_FAILED` | 400 | 구글 code↔token 교환 실패 |
| `NOT_FOUND` | 400 | `/api/users/me` 조회 시 사용자를 찾을 수 없음 |

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
| GET | `/api/consents/me` | Bearer | `{ items:[{type,required,label,termsUrl,agreed,withdrawable,termsVersion,expiresAt}] }` |
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

### 동의 에러 코드

| 코드 | HTTP | 의미 |
|---|---|---|
| `CONSENT_REQUIRED_MISSING` | 400 | 필수 항목에 동의하지 않고 저장 시도 |
| `CONSENT_TYPE_INVALID` | 400 | 알 수 없는 동의 항목·그룹이거나 해당 scope 밖의 항목 |
| `CONSENT_NOT_WITHDRAWABLE` | 400 | `TERMS`·`PRIVACY` 철회 시도 (탈퇴에 해당) |
| `NOT_FOUND` | 400 | 철회할 동의 내역 없음 |

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

### 알림 에러 코드

| 코드 | HTTP | 상황 |
|---|---|---|
| `NOT_FOUND` | 400 | 없는 알림 또는 남의 알림 (구분해 알려주지 않는다) |
