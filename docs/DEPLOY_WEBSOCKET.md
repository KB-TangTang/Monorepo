# 배포 환경 WebSocket(wss) 연결

그룹 채팅(이슈 #174)의 **실시간 송수신**을 배포 환경에서 연결하는 구성이다.
REST(방 정보·이전 대화·읽음 처리)는 그 전부터 정상이었고, 소켓만 길이 없었다.

**현재 상태 — 인프라·프론트 모두 적용 완료(이슈 #268).** 아래는 그 구성의 기록이자 재구축 절차다.

## 요청이 두 길로 갈린다

```
REST   브라우저 → Vercel(rewrite) → EC2:443(nginx) → 127.0.0.1:8080   ← #484 에서 https 로
소켓   브라우저 → EC2:443(nginx)  →                  127.0.0.1:8080   ← #268 에서 뚫은 길
```

**소켓만 EC2 로 직행시킨다.** REST 는 Vercel 을 한 번 거치지만, #484 이후로는 그 다음 구간도
이 nginx 의 443 을 탄다(원래는 `http://<EC2>:8080` 평문이었다).

> ⚠ **REST 를 https 로 옮기는 방법은 두 가지고 조건이 전혀 다르다. 섞어 읽지 말 것.**
>
> | | A. rewrite 대상만 https | B. 프론트가 EC2 직행 |
> |---|---|---|
> | 바꾸는 곳 | `vercel.json` destination | `VITE_API_BASE_URL` |
> | 브라우저가 보는 오리진 | `*.vercel.app` (그대로) | `kb-tangtang.duckdns.org` (갈린다) |
> | CORS · 쿠키 SameSite | **영향 없음** | 둘 다 고쳐야 함 |
> | 상태 | 채택 (2026-08-25) | **기각** — 아래 부록 |
>
> 아래 「부록」은 **B 안**을 기각한 기록이다. **A 안에는 부록의 두 조건이 적용되지 않는다.**
> rewrite 는 브라우저 리다이렉트가 아니라 Vercel 서버가 대신 호출하는 서버 사이드 프록시라
> 오리진이 갈리지 않기 때문이다.

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
| 프록시 대상 | `/ws/` · `/api/notifications/stream` · `/api/` · `/uploads/` → `127.0.0.1:8080` |
| 보안그룹 | 80 · 443 개방. **8080 도 열어 둔다** — #484 이후 정상 경로는 443 이지만, `vercel.json` 을 되돌리는 것이 유일한 롤백 수단이라 그 목적지를 살려 둔다 |

## nginx 설정

**아래는 2026-08-25 기준 EC2 실제 파일 내용이다.** `certbot --nginx` 가 이 파일을 관리하므로
`ssl_certificate` · `include options-ssl-nginx.conf` · `ssl_dhparam` 줄은 손대지 않는다.

> 실제 파일에는 주석이 없다. vi 로 붙여넣을 때 autoindent 가 계단식으로 들여쓰기를 망가뜨려서
> **주석 없는 본문만 `sudo tee ... <<'NGINXCONF'` 로 넣었다.** 설명은 이 문서가 갖는다.

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
    listen 443 ssl;
    http2 on;                 # nginx 1.25+ 는 `listen 443 ssl http2` 가 deprecated 라 별도 지시어다
    server_name kb-tangtang.duckdns.org;

    # ↓ 이 4줄은 certbot 이 관리한다. 갱신 때 덮어써도 되도록 그대로 둔다.
    ssl_certificate     /etc/letsencrypt/live/kb-tangtang.duckdns.org/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/kb-tangtang.duckdns.org/privkey.pem;
    include /etc/letsencrypt/options-ssl-nginx.conf;
    ssl_dhparam /etc/letsencrypt/ssl-dhparams.pem;

    # ── 채팅 WebSocket ──────────────────────────────────
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
        proxy_read_timeout  3600s;
        proxy_send_timeout  3600s;

        # 실시간 프레임이 버퍼에 고이면 지연으로 보인다.
        proxy_buffering off;
    }

    # ── SSE 알림 (2026-08-25 추가) ───────────────────────
    # location 은 「선언 순서」가 아니라 「가장 긴 접두 일치」로 정해진다.
    # 그래서 이 블록이 /api/ 보다 우선한다 — 위치를 옮겨도 동작은 같다.
    location /api/notifications/stream {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header Host              $host;
        proxy_set_header X-Real-IP         $remote_addr;
        proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        proxy_buffering off;   # 없으면 SSE 가 버퍼에 갇혀 프론트가 60초 폴링으로 강등된다
        gzip off;              # 전역 gzip 은 현재 꺼져 있다. 나중에 켜질 때를 위한 보험
        # proxy_read_timeout 은 넣지 않는다 — SseHeartbeat 가 15초마다 ping 을 보낸다
    }

    # ── REST ────────────────────────────────────────────
    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header Host              $host;
        proxy_set_header X-Real-IP         $remote_addr;
        proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # 20m = WebConfig.MAX_REQUEST_SIZE. 변론 증빙이 5MB × 3장 = 15MB 까지 올라온다.
        # 이전 값 12m 은 MAX_FILE_SIZE(10MB) 만 보고 잡은 것이라 3장 업로드가 413 으로 잘렸다.
        client_max_body_size 20m;
    }

    location /uploads/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
    }
}
```

### 변경 이력

| 날짜 | 변경 | 이유 |
|---|---|---|
| 2026-08-19 (#268) | 최초 작성 — `/ws/` wss 종단 | 채팅 실시간 |
| 2026-08-25 | `client_max_body_size` 12m → **20m** | 변론 증빙 3장(15MB) 413 방지 |
| 2026-08-25 | `location /api/notifications/stream` 블록 추가 | REST 를 이 nginx 로 넘길 때 SSE 버퍼링 방지 |

**적용 절차** — 되돌릴 수 있게 백업부터 한다.

```bash
sudo cp /etc/nginx/conf.d/tangtang.conf ~/tangtang.conf.bak   # 1. 백업
sudo vi /etc/nginx/conf.d/tangtang.conf                       # 2. 수정 (sudo 없이 열면 E45 readonly)
sudo nginx -t                                                 # 3. 문법 검사 — 반드시 통과 후에만 다음
sudo systemctl reload nginx                                   # 4. 무중단 반영
```

`nginx -t` 가 실패하면 reload 하지 말고 백업을 되돌린다: `sudo cp ~/tangtang.conf.bak /etc/nginx/conf.d/tangtang.conf`

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

### REST 를 이 nginx 로 넘긴 뒤 추가로 볼 것 (A 안 전환 시)

`/api/` 블록은 #268 당시 `curl -I .../api/health` 로 **GET 200 만** 확인했다.
아래 세 가지는 그 확인에 포함되지 않았으므로 전환 후 반드시 별도로 본다.

| 확인할 것 | 방법 | 기대 |
|---|---|---|
| 대용량 multipart POST | 아래 「업로드 상한 확인」 | 15MB 통과 · 25MB 만 413 |
| SSE 알림 | 아래 「SSE 확인」 | `connected` 즉시 도착 |
| 로그인 유지 | 새로고침 · 15분 후 액세스 토큰 재발급 | 재로그인 요구 없음 (쿠키가 그대로 실리는지) |

**업로드 상한 확인** — 변론 화면으로 사진을 올려 보는 것보다 경계를 직접 때리는 편이 빠르고 정확하다.
인증이 없어도 **nginx 단계**는 판정할 수 있다(401/400 이면 nginx 를 통과한 것, 413 이면 nginx 가 막은 것).

```bash
head -c 15000000 /dev/zero > /tmp/15mb.bin
head -c 25000000 /dev/zero > /tmp/25mb.bin
for f in /tmp/15mb.bin /tmp/25mb.bin; do
  echo -n "$f -> "
  curl -s -o /dev/null -w '%{http_code}\n' -X POST \
    -F "file=@$f" https://kb-tangtang.duckdns.org/api/users/me/profile-image
done
```

> **실측(2026-08-25)**: `15mb.bin → 400` · `25mb.bin → 413`. 경계가 20m 에 정확히 있다.
> **15MB 가 413 이면 `client_max_body_size` 가 반영되지 않은 것**이다.

**SSE 확인** — ⚠ **DevTools 로는 하트비트가 안 보인다.** 두 가지 이유가 겹친다.

- 하트비트는 `:ping` 이라는 **주석 프레임**이고, `sseStream.js:28-30` 이 `:` 로 시작하는 줄을 버린다
- Chrome 의 EventStream 탭도 주석은 표시하지 않는다

그래서 **판정 근거는 「15초 프레임」이 아니라 `connected` 이벤트의 도착 시각**이다.
`NotificationController.java:70` 이 연결 직후 이걸 한 번 쏘는데, 버퍼링이 켜져 있으면
4096바이트 버퍼가 찰 때까지(`:ping\n\n` 7바이트 × 585회 ≈ **2.4시간**) 아무것도 오지 않는다.

프로덕션 페이지에서 **로그인을 마친 뒤** 콘솔에 붙여넣는다. 액세스 토큰은 메모리(Pinia)에만 있어
꺼낼 방법이 없으므로, axios 가 쓰는 XHR 의 헤더 설정을 한 번만 가로채 받아온다.

```js
(() => {
    const orig = XMLHttpRequest.prototype.setRequestHeader;
    XMLHttpRequest.prototype.setRequestHeader = function (k, v) {
        if (k.toLowerCase() === 'authorization' && !window.__done) {
            window.__done = true;
            XMLHttpRequest.prototype.setRequestHeader = orig; // 즉시 원복
            start(v);
        }
        return orig.apply(this, arguments);
    };
    function start(tok) {
        fetch('https://kb-tangtang.duckdns.org/api/notifications/stream', {
            headers: { Authorization: tok },
        }).then(async (res) => {
            console.log('[nginx] 응답 ' + res.status);
            if (!res.ok) return console.log('인증 실패');
            const rd = res.body.getReader(), dec = new TextDecoder(), t0 = Date.now();
            for (;;) {
                const { value, done } = await rd.read();
                const s = ((Date.now() - t0) / 1000).toFixed(1);
                if (done) return console.log('[nginx] +' + s + 's 끊김');
                console.log('[nginx] +' + s + 's', JSON.stringify(dec.decode(value)));
            }
        });
    }
    console.log('설치 완료. 하단 탭을 아무거나 눌러 API 를 한 번 호출하세요.');
})();
```

> **실측(2026-08-25)**: `+0.3s "event:connected\ndata:ok\n\n"` 이후 `+15.3s`·`+30.3s` … 로
> `":ping\n\n"` 이 15초 간격으로 들어왔다.
>
> ⚠ **로그인 「전」에 붙여넣으면 안 된다.** 구글 OAuth 는 전체 페이지 이동이라 패치가 날아간다.

> **되돌리기**: `vercel.json` 의 destination 을 `http://3.35.24.153:8080` 로 되돌리고 재배포하면 끝이다.
> 그래서 **보안그룹의 8080 을 닫지 않는다** — 8080 이 롤백 경로다.

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
