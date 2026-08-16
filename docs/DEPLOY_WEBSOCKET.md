# 배포 환경 WebSocket(wss) 연결

그룹 채팅(이슈 #174)의 **실시간 송수신**을 배포 환경에서 연결하는 구성이다.
REST(방 정보·이전 대화·읽음 처리)는 그 전부터 정상이었고, 소켓만 길이 없었다.

**현재 상태 — 인프라·프론트 모두 적용 완료(이슈 #268).** 아래는 그 구성의 기록이자 재구축 절차다.

## 요청이 두 길로 갈린다

```
REST   브라우저 → Vercel(rewrite) → EC2:8080          ← 기존 그대로
소켓   브라우저 → EC2:443(nginx)  → 127.0.0.1:8080     ← #268 에서 뚫은 길
```

**소켓만 EC2 로 직행시킨다.** REST 까지 옮기지 않는 이유는 아래 「부록 — REST 까지 옮기면 생기는 일」 참고.

## 왜 소켓이 rewrite 를 못 타나

프론트는 Vercel(https), 백엔드는 EC2(http:8080) 다.

1. https 페이지에서 `ws://` 로 붙으면 브라우저가 **mixed content 로 차단**한다. `wss://` 여야 한다.
2. `wss://` 를 받으려면 EC2 앞단에 **TLS 종단**이 있어야 한다.
3. `apps/web/vercel.json` 의 rewrite 는 `/api` · `/uploads` 만 EC2 로 넘기고 나머지는 `index.html` 로
   떨어뜨린다. **rewrite 는 WebSocket 업그레이드를 프록시하지 못한다** — Vercel 을 경유하는 길이 없다.

## 적용된 값

| 항목 | 값 |
|---|---|
| API 도메인 | `kb-tangtang.duckdns.org` (DuckDNS, A 레코드 → `3.35.24.153`) |
| 인증서 | Let's Encrypt (`certbot --nginx`). `certbot renew` 타이머로 자동 갱신 |
| nginx 설정 | `/etc/nginx/conf.d/tangtang.conf` |
| 프록시 대상 | `/ws/` · `/api/` · `/uploads/` → `127.0.0.1:8080` |
| 보안그룹 | 80 · 443 개방 (**8080 은 그대로 유지** — REST 가 아직 rewrite 로 이 길을 쓴다) |

## nginx 설정

```nginx
# /etc/nginx/conf.d/tangtang.conf

# 업그레이드 헤더 전달용. WebSocket 이 아닌 요청에서는 빈 값이어야 한다 —
# "upgrade" 를 무조건 박으면 일반 REST 응답이 깨진다.
map $http_upgrade $connection_upgrade {
    default upgrade;
    ''      close;
}

server {
    listen 80;
    server_name kb-tangtang.duckdns.org;
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl http2;
    server_name kb-tangtang.duckdns.org;

    ssl_certificate     /etc/letsencrypt/live/kb-tangtang.duckdns.org/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/kb-tangtang.duckdns.org/privkey.pem;

    # ── 채팅 WebSocket ──────────────────────────────────
    # 이 블록이 /api 블록보다 먼저 와야 한다(location 은 접두 일치라 순서가 아니라 구체성으로
    # 정해지지만, 사람이 읽을 때 헷갈리지 않게 위에 둔다).
    location /ws/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;

        # 이 두 줄이 없으면 업그레이드가 성립하지 않아 101 대신 200/400 이 돌아온다.
        proxy_set_header Upgrade    $http_upgrade;
        proxy_set_header Connection $connection_upgrade;

        proxy_set_header Host              $host;
        proxy_set_header X-Real-IP         $remote_addr;
        proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # STOMP 연결은 오래 열려 있다. 기본 60초면 1분마다 끊겨 재연결을 반복한다.
        # 서버가 보내는 하트비트 주기보다 넉넉히 길게 잡는다.
        proxy_read_timeout  3600s;
        proxy_send_timeout  3600s;

        # 실시간 프레임이 버퍼에 고이면 지연으로 보인다.
        proxy_buffering off;
    }

    # ── REST ────────────────────────────────────────────
    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header Host              $host;
        proxy_set_header X-Real-IP         $remote_addr;
        proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        client_max_body_size 12m;          # 프로필 이미지 업로드 상한(10MB)보다 크게
    }

    location /uploads/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
    }
}
```

## 프론트 설정

Vercel 프로젝트 환경변수에 **소켓 전용 값 하나만** 넣고 재배포한다.

```
VITE_WS_BASE_URL=https://kb-tangtang.duckdns.org
```

- **Production · Preview 두 스코프에 모두 넣는다.** Preview 를 빼면 프리뷰 배포에서 소켓이
  안 붙는다 — 프리뷰를 살리는 것이 이 방식을 고른 이유다(부록 참고).
- **`https://` 로 넣는다. 끝에 `/api` 를 붙이지 않는다.**
  `chatSocketUrl.js` 가 `wss://kb-tangtang.duckdns.org/ws/chat` 을 만들어 준다.
- ⚠ **`wss://` 를 넣으면 안 된다.** 절대 URL 판정이 `/^https?:\/\//i` 라 걸리지 않고
  **조용히 Vercel 호스트로 폴백**한다. 증상이 「그냥 안 붙는다」뿐이라 원인을 찾기 어렵다.
- `VITE_API_BASE_URL` 은 **건드리지 않는다.** REST 는 지금처럼 `vercel.json` rewrite 로 간다.
- 되돌리려면 `VITE_WS_BASE_URL` 을 지우고 재배포하면 된다. 코드가 `VITE_API_BASE_URL` 로
  폴백하므로 배포 외 작업이 없다.

## 확인 절차

1. 인증서: `curl -I https://kb-tangtang.duckdns.org/api/health` → `200`
2. 업그레이드 성립 여부 (핵심):
   ```bash
   curl -i -N --http1.1 \
     -H "Connection: Upgrade" -H "Upgrade: websocket" \
     -H "Sec-WebSocket-Version: 13" -H "Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==" \
     -H "Origin: https://monorepo-three-ruby-81.vercel.app" \
     https://kb-tangtang.duckdns.org/ws/chat
   ```
   → **`HTTP/1.1 101 Switching Protocols`** 가 나와야 한다.

   > **`--http1.1` 을 빠뜨리지 말 것.** 없으면 curl 이 HTTP/2 로 붙는데 HTTP/2 에는 업그레이드
   > 개념이 없어 **400** 이 돌아온다. 설정 오류가 아니다. 실제 브라우저는 WebSocket 핸드셰이크를
   > 항상 HTTP/1.1 로 하므로 영향이 없다.
3. 브라우저에서 채팅방 입장 → DevTools → Network → **WS** 필터 → `chat` 항목이 잡히고 프레임이 오간다.

## 부록 — REST 까지 EC2 직행으로 옮기면 생기는 일 (기각한 안)

`VITE_API_BASE_URL` 을 EC2 도메인으로 바꿔 REST 까지 옮기는 방법도 있었다. **기각했다.**
나중에 다시 검토한다면 아래 두 가지를 **함께** 처리해야 한다. 환경변수만 바꾸면 배포가 깨진다.

1. **프리뷰 배포가 전부 막힌다.** `ServletConfig.java` 의 `.allowedOrigins(...)` 는 정확 문자열
   2개만 받는다(와일드카드가 아니다). 지금은 rewrite 덕에 same-origin 이라 CORS 심사 자체를
   타지 않는데, REST 를 직행으로 바꾸면 심사가 시작되고 `monorepo-git-*.vercel.app` 프리뷰가
   전부 차단된다.
2. **로그인 유지가 깨진다.** `application.properties` 의 `auth.cookie.same-site=Lax` 를
   `application-docker.properties` 가 오버라이드하지 않는다(키 자체가 없다). 오리진이 갈리면
   리프레시 쿠키가 안 실려 재로그인이 반복된다. **환경변수로는 못 바꾸고 코드를 고쳐야 한다.**

소켓만 빼내는 방식이 성립하는 근거는 **STOMP 인증이 쿠키가 아니기 때문**이다.
`chatSocket.js` 가 CONNECT 프레임의 `Authorization: Bearer` 헤더로 인증하고 SockJS 도 쓰지 않는다
(`brokerURL` 직결). 소켓 Origin 은 `WebSocketConfig` 가 `https://*.vercel.app` 와일드카드라
프리뷰까지 통과한다. 그래서 REST·쿠키·CORS 를 전혀 건드리지 않는다.

## 서버 쪽에서 이미 준비된 것

- 허용 Origin 은 `WebSocketConfig#registerStompEndpoints` 에 있다:
  `https://*.vercel.app` · `http://localhost:5173`. **프론트 도메인이 vercel.app 이 아니면 여기에 추가해야 한다.**
- 인증은 CONNECT/STOMP 프레임의 `Authorization: Bearer` 헤더다. nginx 는 헤더를 그대로 넘기기만 하면 된다.
- Redis 는 `127.0.0.1:6379` 로만 바인딩돼 있다(이슈 #225 재발 방지). 외부에 열지 말 것.
