# 배포 환경 WebSocket(wss) 연결 — EC2 담당자용

그룹 채팅(이슈 #174)의 **실시간 송수신만** 아직 배포 환경에서 동작하지 않는다.
REST(방 정보·이전 대화·읽음 처리)는 이미 정상이다. 이 문서는 그 마지막 한 칸을 채우는 절차다.

## 왜 지금은 안 되나

프론트는 Vercel(https), 백엔드는 EC2(http:8080) 다.

1. https 페이지에서 `ws://` 로 붙으면 브라우저가 **mixed content 로 차단**한다. `wss://` 여야 한다.
2. `wss://` 를 받으려면 EC2 앞단에 **TLS 종단**이 있어야 한다. 지금은 없다.
3. `apps/web/vercel.json` 의 rewrite 는 `/api` · `/uploads` 만 EC2 로 넘기고 나머지는 `index.html` 로
   떨어뜨린다. **rewrite 는 WebSocket 업그레이드를 프록시하지 못한다** — Vercel 을 경유하는 길은 없다.

그래서 **환경변수만으로는 해결되지 않는다.** `VITE_API_BASE_URL` 에 `http://<EC2>:8080` 을 넣는 것도 답이
아니다. 그러면 소켓은 물론이고 지금 잘 도는 REST 까지 mixed content 로 함께 막힌다.

## 필요한 것

EC2 에 도메인과 인증서를 붙이고, nginx 가 443 에서 TLS 를 끊은 뒤 8080 으로 넘긴다.

- 도메인 1개 (예: `api.tangtang.example`) → EC2 공인 IP 로 A 레코드
- Let's Encrypt 인증서 (`certbot --nginx`)
- 아래 server 블록

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
    server_name api.tangtang.example;      # ← 실제 도메인으로 바꾼다
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl http2;
    server_name api.tangtang.example;      # ← 실제 도메인으로 바꾼다

    ssl_certificate     /etc/letsencrypt/live/api.tangtang.example/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/api.tangtang.example/privkey.pem;

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

nginx 가 올라간 뒤 Vercel 프로젝트 환경변수에 아래를 넣고 재배포한다.

```
VITE_API_BASE_URL=https://api.tangtang.example/api
```

- 반드시 **`https://`** 다. `http://` 를 넣으면 mixed content 로 REST 까지 막힌다.
- 소켓 주소는 이 값에서 유도된다(`apps/web/src/api/chatSocketUrl.js`) — 따로 설정할 값이 없다.
- 이 값을 넣으면 REST 도 `vercel.json` rewrite 를 거치지 않고 EC2 로 직접 간다. 그때부터
  프론트와 API 의 오리진이 갈리므로 **리프레시 쿠키 설정을 함께 확인해야 한다**
  (`auth.cookie.same-site` · `auth.cookie.secure` — 오리진이 갈리면 `None` + `secure=true` 가 필요하다).

## 확인 절차

1. 인증서: `curl -I https://api.tangtang.example/api/health` → `200`
2. 업그레이드 성립 여부 (핵심):
   ```bash
   curl -i -N \
     -H "Connection: Upgrade" -H "Upgrade: websocket" \
     -H "Sec-WebSocket-Version: 13" -H "Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==" \
     -H "Origin: https://<프론트 도메인>" \
     https://api.tangtang.example/ws/chat
   ```
   → **`HTTP/1.1 101 Switching Protocols`** 가 나와야 한다. 200·400 이면 업그레이드가 안 넘어간 것이다.
3. 브라우저에서 채팅방 입장 → DevTools → Network → **WS** 필터 → `chat` 항목이 잡히고 프레임이 오간다.

## 서버 쪽에서 이미 준비된 것

- 허용 Origin 은 `WebSocketConfig#registerStompEndpoints` 에 있다:
  `https://*.vercel.app` · `http://localhost:5173`. **프론트 도메인이 vercel.app 이 아니면 여기에 추가해야 한다.**
- 인증은 CONNECT/STOMP 프레임의 `Authorization: Bearer` 헤더다. nginx 는 헤더를 그대로 넘기기만 하면 된다.
- Redis 는 `127.0.0.1:6379` 로만 바인딩돼 있다(이슈 #225 재발 방지). 외부에 열지 말 것.
