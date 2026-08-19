# 보안 점검 결과 (2026-08-14 점검 · 2026-08-19 갱신)

배포 환경(EC2 `<EC2>` · Vercel 프론트)을 대상으로 점검했다.
**애플리케이션 계층은 문제가 없었고, 확인된 3건은 전부 네트워크·배포 계층이다.**

관련 이슈: [#225](https://github.com/KB-TangTang/Monorepo/issues/225)

> **2026-08-19 갱신** — 최초 점검 이후 배포 구조가 바뀌었다.
> - 3307 은 SSH 터널 전환이 끝나 **차단 완료**(아래 1번)
> - EC2 앞단에 호스트 nginx 가 서고 443 에서 **wss 만** 종단한다(이슈 #268).
>   REST 는 여전히 Vercel rewrite → 8080 평문이다. 구조는 `docs/DEPLOY_WEBSOCKET.md` 가 원본
> - 이 점검에서 준비했던 `deploy/` TLS 구성은 위 구조와 충돌해 **폐기했다**(아래 2번)

---

## 1. 확인된 것

### 🔴 MySQL 이 인터넷에 노출돼 있었다

| | |
|---|---|
| 확인 방법 | 외부 네트워크에서 `<EC2>:3307` TCP 연결 |
| 결과 | 연결 성립. MySQL 이 `8.0.46` · `caching_sha2_password` 핸드셰이크를 응답 |
| 대조 확인 | 같은 방식으로 `9999` · `12345` 는 연결 실패 → 위 결과가 오탐이 아님 |
| 영향 | 누구나 `root` · `tangtang` 계정에 인증을 시도할 수 있다. 비밀번호 하나에만 의존하는 상태 |

**조치 — 완료(2026-08-19)**. 팀 전원의 SSH 터널 설정이 끝난 뒤 AWS 보안그룹에서 3307 을 차단했다.
차단 후 REST(8080) 200 · wss 101 을 확인해 서비스 영향이 없음을 검증했다.

> 점검 시점에는 바로 닫지 못했다. 팀이 **MySQL Workbench 로 EC2 DB 에 직접 붙어 작업**하고 있었고
> SSH 터널을 쓸 수 있는 사람이 없었기 때문이다. 접근 수단을 없애는 조치는 보안이 아니라 사고다.
> 그래서 터널 설정을 먼저 돌린 뒤 닫았다.

`docker-compose.yml` 의 바인딩 주소는 `DB_BIND` 변수로 빼 두었다. 보안그룹에 더해
`DB_BIND=127.0.0.1` 로 EC2 내부까지 좁힐 수 있다(현재는 기본값인 전체 공개, 보안그룹이 막는 구조).

### 🔴 Vercel → EC2 구간이 평문 HTTP

| | |
|---|---|
| 확인 방법 | `vercel.json` 의 rewrite 대상 |
| 결과 | `http://<EC2>:8080` — 브라우저↔Vercel 구간만 TLS 다 |
| 영향 | 액세스 토큰(Authorization 헤더) · 리프레시 쿠키 · 계좌·거래내역이 평문으로 인터넷을 지난다 |

**현재 상태 — 미해결. 후속 이슈로 넘긴다.**

이슈 #268(그룹 채팅 wss)에서 EC2 앞단에 TLS 종단이 생겼다. 다만 **소켓 전용**이다.

```
REST   브라우저 → Vercel(rewrite) → EC2:8080            평문 HTTP  ← 이 항목
소켓   브라우저 → EC2:443(호스트 nginx) → 127.0.0.1:8080  wss       ← 해결됨
```

호스트에 직접 설치한 nginx 가 `kb-tangtang.duckdns.org` 인증서로 443 을 종단하고 `/ws/` 만
업그레이드 프록시한다. 설정 원본과 적용 절차는 **`docs/DEPLOY_WEBSOCKET.md`** 다.

> 이 점검(#225)에서는 `deploy/` 에 컨테이너 nginx + certbot standalone 구성을 준비했었다.
> 위 구조가 들어선 뒤 **적용 불가·유해**로 판명돼 삭제했다.
> - 템플릿에 `map $http_upgrade $connection_upgrade` 와 `location /ws/` 가 없어
>   적용하면 **그룹 채팅이 죽는다**
> - 호스트 nginx 가 80/443 을 점유 중이라 컨테이너가 바인드하지 못하고,
>   `certbot --standalone -p 80:80` 도 실패한다
> - 인증서 경로도 어긋난다(호스트 `/etc/letsencrypt` vs 도커 볼륨)

REST 를 https 로 옮기려면 **아래 4가지를 반드시 함께** 해야 한다. 하나라도 빠지면 로그인이 깨진다.

1. `ServletConfig` 의 CORS `allowedOrigins`(정확 문자열 2개) → `allowedOriginPatterns`.
   지금 그대로면 Vercel 프리뷰 배포가 CORS 로 막힌다
2. `auth.cookie.same-site` 를 `application-docker.properties` 에서 오버라이드.
   기본 `Lax` 로는 오리진이 갈리는 순간 리프레시 쿠키가 실려 나가지 않는다
3. nginx `client_max_body_size` 를 12m → **20m**. `WebConfig` 의 `MAX_REQUEST_SIZE` 가 20MB 다
   (현재 값은 `MAX_FILE_SIZE` 10MB 만 보고 잡은 것이다)
4. `/api/notifications/stream` 에 `proxy_buffering off` 유지 — SSE 알림이 버퍼에 갇힌다

### 🟡 Swagger 문서가 무인증으로 공개

| | |
|---|---|
| 확인 방법 | `curl http://<EC2>:8080/swagger-ui.html` · `/swagger-resources` · `/v2/api-docs?group=...` |
| 결과 | 전부 200. **86 paths / 94 operations** 의 경로·파라미터·응답 구조가 그대로 열람 가능 (2026-08-19 재실측) |
| 내역 | 「01. 서비스 API」 80 paths / 88 operations (134,342 B) · **「02. 개발 전용 API」 6 paths / 6 operations (10,343 B)** |
| 원인 | 인증 인터셉터가 `/api/**` 에만 걸려 있고 Swagger 경로는 그 밖이다 |

`/swagger-resources` 가 그룹 목록을 그대로 돌려주기 때문에 **개발 전용 API 그룹의 존재만이 아니라
명세 전문이 다운로드된다.** 실제 호출은 `DevEnvironmentGuard` 가 막지만, 배치 트리거·미션 재배정
같은 내부 운영 엔드포인트의 형태가 노출된다.

**조치**: `SwaggerAccessInterceptor` 로 배포 환경에서만 HTTP Basic 인증을 요구한다.
로컬(`app.env=local`)은 그대로 열린다. **비밀번호를 설정하지 않으면 404 로 숨긴다** —
설정 누락 시 열린 채 남는 것보다 안 보이는 편이 안전하기 때문이다.

### 🟡 관리 포트 접근 범위

보안그룹의 접근 범위를 팀 IP 로 좁히기를 권고한다(EC2 담당 팀원).
**구체적인 포트·현재 상태는 이 저장소가 공개라 적지 않는다.** 팀 채널로 공유했다.

### ✅ 목서버(8081)는 이미 막혀 있었다

기존에 "노출됐을 수 있다" 고 적어둔 항목이다. 실제로는 보안그룹이 8081 을 필터링하고 있어
외부에서 닿지 않는다. **우려는 해소됐다.**

다만 compose 는 `0.0.0.0:8081` 로 열고 있었다. 보안그룹 규칙 하나에만 기대지 않도록
바인딩을 `127.0.0.1:8081` 로 좁혔다. 목서버에는 인증이 없고, 본서버는 compose 네트워크로
`mock:8080` 에 붙으므로 영향이 없다. 로컬 개발자가 쓰는 `http://localhost:8081` 도 그대로 된다.

---

## 2. 문제가 없었던 것 (점검 범위와 근거)

| 항목 | 확인한 내용 | 결과 |
|---|---|---|
| 권한 검증 (IDOR) | `@PathVariable` 을 받는 모든 엔드포인트가 `@LoginUser userId` 를 서비스로 넘기고, 매퍼 SQL 의 `WHERE` 에 `user_id` 가 함께 들어간다 (예: `FixedExpenseCandidateActionMapper` · `NotificationMapper`) | ✅ |
| SQL 인젝션 | 매퍼 XML 전수 검색 — `${}` 사용 **0건**. 전부 `#{}` | ✅ |
| 액세스 토큰 저장 | 프론트가 메모리(Pinia)에만 둔다. `localStorage` 에 넣지 않는다 | ✅ |
| 리프레시 토큰 | `HttpOnly` · `SameSite` · `Secure`(배포) 쿠키. `Path=/api/auth` 로 좁혀 다른 요청에 실려 나가지 않는다. `SameSite=None` + `Secure` 누락 조합은 기동 시 예외로 막는다 | ✅ |
| 오픈 리다이렉트 | 로그인 후 복귀 경로를 `isSafeRedirectPath` 로 `/` 시작 내부 경로만 허용 | ✅ |
| 개발용 API | `DevEnvironmentGuard` 가 `app.env` 로 차단. 배포 환경에서 `/api/dev/**` 는 동작하지 않는다 | ✅ |
| 시크릿 관리 | 저장소에 커밋된 시크릿 없음. `.gitignore` 가 `application-local.properties` · `.env` · `*.key` 를 제외 | ✅ |
| CORS | `/api/**` 에 명시된 오리진 2개만 허용. 와일드카드 + `allowCredentials` 조합 없음 | ✅ |

---

## 3. 남겨둔 것 (판단과 이유)

| 항목 | 판단 | 이유 |
|---|---|---|
| CORS 에 `http://localhost:5173` 이 배포에도 남아 있음 | 그대로 둔다 | 악용하려면 **피해자 PC 의 5173 포트에서** 공격 페이지를 서빙해야 한다. 개발자 본인 외에는 성립하지 않는다. 환경별로 가르려면 설정 구조를 손대야 해서 마감 직전 변경 대비 이득이 작다 |
| `/uploads/**` 무인증 공개 | 그대로 둔다 | 그룹 멤버에게 보여야 하는 프로필 이미지다. 키에 UUID 가 들어 있어 추측으로 열 수 없다 |
| 인증 실패 횟수 제한(brute force) | 범위 밖 | 로그인이 구글 OAuth 라 자체 비밀번호가 없다. 시도할 대상이 없다 |

---

## 4. 남은 작업

- [x] 보안그룹: **3307 차단 완료**(2026-08-19). 팀은 SSH 터널로 접속한다
- [ ] 보안그룹: 관리 포트 접근 범위를 팀 IP 로 제한 (EC2 담당 팀원 · 상세는 팀 채널)
- [ ] **REST(8080) https 전환 → `vercel.json` 을 https 로** — **후속 이슈.**
      위 「Vercel → EC2 구간이 평문 HTTP」의 동반 조건 4가지를 함께 처리해야 한다
- [ ] `.env` 에 `SWAGGER_ACCESS_PASSWORD` 설정 후 재배포. 값은 팀 채널로 공유
      (EC2 env 에는 이미 넣어 뒀다. 이 PR 이 머지돼 새 war 가 올라가야 실제로 적용된다)
- [ ] nginx 에 보안 헤더가 없다 — HSTS · `X-Content-Type-Options` · `X-Frame-Options` ·
      `Referrer-Policy` 전무. `server` 헤더로 버전도 나간다(`server_tokens off` 로 끈다)

> 점검 방법은 팀 소유 서버를 대상으로 **연결 가능 여부와 배너 확인**까지만 했다.
> 인증 시도·취약점 스캐너는 돌리지 않았다.
