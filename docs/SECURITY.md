# 보안 점검 결과 (2026-08-14 점검 · 2026-08-19 갱신)

배포 환경(EC2 `<EC2>` · Vercel 프론트)을 대상으로 점검했다.
**애플리케이션 계층은 문제가 없었고, 확인된 3건은 전부 네트워크·배포 계층이다.**

관련 이슈: [#225](https://github.com/KB-TangTang/Monorepo/issues/225)

> **2026-08-19 갱신** — 최초 점검 이후 배포 구조가 바뀌었다.
> - 3307 은 SSH 터널 전환이 끝나 **차단 완료**(아래 1번)
> - EC2 앞단에 호스트 nginx 가 서고 443 에서 **wss 만** 종단한다(이슈 #268).
>   REST 는 Vercel rewrite → 8080 평문이었다. 구조는 `docs/DEPLOY_WEBSOCKET.md` 가 원본
> - 이 점검에서 준비했던 `deploy/` TLS 구성은 위 구조와 충돌해 **폐기했다**(아래 2번)
>
> **2026-08-25 갱신** — 이슈 [#484](https://github.com/KB-TangTang/Monorepo/issues/484) 에서
> **REST·업로드 구간도 https 로 전환**했다(PR 리뷰 대기).
> - 보류 사유였던 「4가지 동반 조건」이 **서로 다른 두 전환 방법의 조건을 섞은 것**임을 재검증했다.
>   채택안에 실제로 필요한 건 ③④ 둘뿐이고, EC2 nginx 에 적용을 마쳤다 (아래 2번)
> - **Swagger 접근 제어는 이미 적용돼 동작 중**임을 실측으로 확인했다 (아래 3번)
> - **보안 헤더 3종을 Vercel 에 추가**했다. 함께 적혀 있던 HSTS 는 **이미 적용돼 있었고**,
>   `nosniff` 담당도 EC2 nginx 가 아니라 Vercel 이었다 (아래 4번)

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

**현재 상태 — 이슈 [#484](https://github.com/KB-TangTang/Monorepo/issues/484) 에서 전환. PR 리뷰 대기(2026-08-25).**

이슈 #268(그룹 채팅 wss)에서 EC2 앞단에 TLS 종단이 생겼다. 다만 당시에는 **소켓 전용**이었다.

```
[#484 이전]
REST   브라우저 → Vercel(rewrite) → EC2:8080                  평문 HTTP  ← 이 항목
소켓   브라우저 → EC2:443(호스트 nginx) → 127.0.0.1:8080       wss       ← #268 해결

[#484 이후]
REST   브라우저 → Vercel(rewrite) → EC2:443(nginx) → :8080    https     ← 전환
소켓   브라우저 → EC2:443(호스트 nginx) → 127.0.0.1:8080       wss
```

**세 구간이 모두 TLS 가 되어 종단 간 암호화가 완성된다.** 브라우저가 보는 오리진은
`*.vercel.app` 그대로이므로(rewrite 는 서버 사이드 프록시다) 애플리케이션 코드 변경은 없다.

호스트에 직접 설치한 nginx 가 `kb-tangtang.duckdns.org` 인증서로 443 을 종단하고 `/ws/` 만
업그레이드 프록시한다. 설정 원본과 적용 절차는 **`docs/DEPLOY_WEBSOCKET.md`** 다.

> 이 점검(#225)에서는 `deploy/` 에 컨테이너 nginx + certbot standalone 구성을 준비했었다.
> 위 구조가 들어선 뒤 **적용 불가·유해**로 판명돼 삭제했다.
> - 템플릿에 `map $http_upgrade $connection_upgrade` 와 `location /ws/` 가 없어
>   적용하면 **그룹 채팅이 죽는다**
> - 호스트 nginx 가 80/443 을 점유 중이라 컨테이너가 바인드하지 못하고,
>   `certbot --standalone -p 80:80` 도 실패한다
> - 인증서 경로도 어긋난다(호스트 `/etc/letsencrypt` vs 도커 볼륨)

#### 전환 방법과 동반 조건 (2026-08-25 재검증)

> 이 문단은 원래 **「4가지를 반드시 함께」** 라고만 적혀 있었다. 재검증 결과 그 목록은
> **서로 다른 두 전환 방법의 조건을 섞어 놓은 것**이었다. 방법에 따라 필요한 조건이 다르다.

| 방법 | 바꾸는 것 | 동반 조건 |
|---|---|---|
| **A. rewrite 대상만 https 로** (채택) | `vercel.json` 의 destination 2개를 `https://kb-tangtang.duckdns.org` 로 | **③ ④** |
| B. 프론트가 EC2 로 직행 | `VITE_API_BASE_URL` 을 EC2 도메인으로 (기각안. `DEPLOY_WEBSOCKET.md` 부록) | ① ② ③ ④ |

1. `ServletConfig` 의 CORS `allowedOrigins`(정확 문자열 2개) → `allowedOriginPatterns`
   — **A 안에는 불필요.** Vercel rewrite 는 브라우저 리다이렉트가 아니라 **Vercel 서버가 대신
   호출하는 서버 사이드 프록시**다. 브라우저가 보는 오리진은 `*.vercel.app` 그대로라
   CORS 심사 자체를 타지 않는다. B 안에서만 필요하다
2. `auth.cookie.same-site` 를 `application-docker.properties` 에서 오버라이드
   — **A 안에는 불필요.** 같은 이유로 오리진이 갈리지 않아 리프레시 쿠키가 그대로 실린다.
   B 안에서만 필요하다
3. nginx `client_max_body_size` 를 12m → **20m** — **A·B 모두 필수.**
   `WebConfig.MAX_REQUEST_SIZE` 가 20MB 이고, 그룹 챌린지 변론 증빙이
   `DefenseService.MAX_IMAGES = 3` × `ImageProcessor.MAX_BYTES = 5MB` = **한 요청 최대 15MB** 다.
   클라이언트 압축도 없다. 12m 이면 사진 3장 변론이 nginx 에서 **413** 으로 잘린다
   (기존 12m 은 `MAX_FILE_SIZE` 10MB 만 보고 잡은 값이었다)
   → **실측(2026-08-25)**: 15MB 본문 = **400**(nginx 통과, 앱까지 도달) · 25MB 본문 = **413**(nginx 차단).
   경계가 의도대로 20m 에 있다
4. `/api/notifications/stream` 에 `proxy_buffering off` — **A·B 모두 필수.**
   없으면 SSE 알림이 버퍼에 갇힌다. 다만 프론트가 60초 폴링(`notification.js` `POLL_INTERVAL_MS`)
   으로 강등되므로 **실시간이 죽을 뿐 완전 장애는 아니다**
   → **실측(2026-08-25)**: 브라우저에서 `https://kb-tangtang.duckdns.org/api/notifications/stream`
   직접 구독 → `connected` 이벤트가 **+0.3초에 즉시** 도착하고 이후 `:ping` 이 **15초 간격**으로 들어온다.
   버퍼링이 켜져 있었다면 4096바이트 버퍼가 찰 때까지(`:ping\n\n` 7바이트 × 585회 ≈ **2.4시간**)
   아무것도 오지 않는다. 즉 「즉시 도착」 자체가 `proxy_buffering off` 의 증거다

> **`proxy_read_timeout` 은 넣지 않는다.** nginx 기본 60초에 SSE 가 끊길 것처럼 보이지만
> `SseHeartbeat.java:26` 이 15초마다 주석 프레임을 보내고, `RootConfig` 의 전용 스케줄러
> 풀(`poolSize 24`)이 그 하트비트가 굶지 않도록 보장한다. 근거 없는 지시어를 늘리지 않는다.
>
> **전역 gzip 도 꺼져 있다** — EC2 에서 `nginx -T | grep -i gzip` 실측, `gzip on;` 없음(2026-08-25).
> SSE 블록의 `gzip off;` 는 나중에 누가 전역 gzip 을 켜도 안전하도록 남겨 둔 보험이다.

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

**조치 — 완료(적용 확인 2026-08-25)**: `SwaggerAccessInterceptor` 로 배포 환경에서만
HTTP Basic 인증을 요구한다. 로컬(`app.env=local`)은 그대로 열린다.
**비밀번호를 설정하지 않으면 404 로 숨긴다** — 설정 누락 시 열린 채 남는 것보다 안 보이는 편이
안전하기 때문이다.

> **실측(2026-08-25)** — EC2 8080 에 직접 요청해 `/swagger-ui.html` · `/swagger-resources` 가
> 모두 **401** 을 돌려주는 것을 확인했다. 이 문서에는 오래 「미적용」으로 남아 있었으나
> **이미 배포돼 동작 중**이다.

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
- [x] **REST(8080) https 전환**(위 표의 **A 안**) — 이슈 #484, **PR 리뷰 대기**.
      동반 조건 ③④ 는 2026-08-25 EC2 nginx 에 적용·실측 완료.
      `apps/web/vercel.json` 의 destination 2개를 `https://kb-tangtang.duckdns.org` 로 바꿨다.
      **머지 직후 프로덕션에서 로그인 → 홈 · 새로고침 로그인 유지 2가지를 확인한다**
      (Vercel 프리뷰는 구글 OAuth `redirect_uri` 가 프로덕션 URL 로 고정돼 있어 로그인 검증이 불가능하다)
- [x] Swagger 접근 제어 — **이미 적용돼 동작 중**(401 실측, 2026-08-25). `SWAGGER_ACCESS_PASSWORD` 는
      EC2 env 에 설정돼 있다
- [x] **보안 헤더 3종 추가**(2026-08-25, 이슈 #484 에 포함). 원래 이 줄은 **5개를 전부 nginx 에
      넣는 것**으로 적혀 있었으나, **헤더는 그 응답을 브라우저에 최종적으로 내보내는 쪽에 붙어야
      효과가 있다.** 실측 결과 그 지점은 EC2 가 아니라 **Vercel** 이었다.

      | 헤더 | 조치 | 근거 (2026-08-25 `curl -sI` 실측) |
      |---|---|---|
      | `X-Frame-Options: DENY` | **Vercel 에 추가** | 없었다. `apps/web/src` 에 `iframe` · `window.parent` · `window.top` 0건이라 `DENY` 로 둔다 |
      | `X-Content-Type-Options: nosniff` | **Vercel 에 추가** | 양쪽 다 없었다. `/uploads/**` 도 rewrite 를 타므로 EC2 가 아닌 Vercel 몫이다(아래) |
      | `Referrer-Policy: strict-origin-when-cross-origin` | **Vercel 에 추가** | 없었다 |
      | **HSTS** | **할 일 없음 — 이미 적용돼 있다** | Vercel 이 `.vercel.app` 에 기본 적용한다: `max-age=63072000; includeSubDomains; preload`. `vercel.app` 자체가 HSTS preload 목록에 있어 `vercel.json` 에 무엇을 적든 바뀌지 않는다 |
      | `server_tokens off` | **남겨둠**(우선순위 낮음) | `kb-tangtang.duckdns.org` 직접 호출에서만 `nginx/1.30.3` 이 보인다. rewrite 를 타면 Vercel 이 `server: vercel` 로 갈아끼운다. 실제 직접 경로는 wss 핸드셰이크와 외부 스캐너뿐 |

      > **왜 `nosniff` 가 EC2 가 아니라 Vercel 인가.** `/uploads/**` 요청은 Vercel rewrite →
      > EC2 nginx → Tomcat 을 거치는데, **Vercel 이 응답 헤더를 자기 것으로 갈아끼운다.**
      > `https://<vercel>/api/health` 응답의 `server` 가 `nginx/1.30.3` 이 아니라 `vercel` 인 것으로
      > 확인했다. EC2 nginx 에만 넣었으면 **「했는데 안 걸리는」** 상태가 됐을 것이다.
      >
      > **HSTS 를 "롤백이 어려우니 짧게 시작한다"고 적어두었던 것도 무의미했다.**
      > 이미 2년짜리가 preload 와 함께 걸려 있다.

      적용 위치: `apps/web/vercel.json` 의 `headers` 블록.
      **머지 후 확인 대상** — `nosniff` 는 Content-Type 이 틀린 응답을 브라우저가 추측으로 구제하지
      못하게 만든다. `/uploads/**` 프로필 이미지가 실제로 렌더되는지 한 번 본다.

> 점검 방법은 팀 소유 서버를 대상으로 **연결 가능 여부와 배너 확인**까지만 했다.
> 인증 시도·취약점 스캐너는 돌리지 않았다.
