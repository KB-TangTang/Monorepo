# TLS 적용 절차 (이슈 #225)

Vercel 이 `/api/*` 를 EC2 로 넘길 때 **평문 HTTP** 를 쓰고 있다.
브라우저↔Vercel 구간만 암호화돼 있고, Vercel↔EC2 구간에는 액세스 토큰·리프레시 쿠키·
거래내역이 그대로 흐른다. 이 문서는 그 구간을 HTTPS 로 바꾸는 절차다.

**이 작업은 EC2 접근 권한이 있는 팀원이 수행한다.** 저장소에는 설정만 준비돼 있다.

---

## 0. 먼저 알아야 할 것

| | |
|---|---|
| 왜 지금 못 하나 | Let's Encrypt 는 **공인 IP 에 인증서를 발급하지 않는다.** 도메인이 필요하다 |
| 준비물 | 무료 도메인 하나 (DuckDNS 등) · EC2 보안그룹에 **80 · 443 열기** |
| 되돌리기 | nginx 를 안 띄우고 기존 `docker compose up -d` 로 돌아가면 즉시 원상 복구 |
| ⚠ 순서 | **인증서 발급 → nginx 기동 → vercel.json 전환** 이다. 순서를 바꾸면 서비스가 멈춘다 |

---

## 1. 도메인 준비 (DuckDNS 기준, 5분)

1. <https://www.duckdns.org> 에서 구글 로그인 → 원하는 이름으로 도메인 생성
   (예: `tangtang-api` → `tangtang-api.duckdns.org`)
2. `current ip` 칸에 EC2 공인 IP 를 넣고 update
3. 확인: `nslookup tangtang-api.duckdns.org` 가 EC2 IP 를 돌려주면 된다

> `nip.io` · `sslip.io` 같은 와일드카드 DNS 는 쓰지 않는다. Let's Encrypt 의 발급 한도를
> 도메인 단위로 공유해 이미 소진돼 있는 경우가 많고, 실패해도 원인이 드러나지 않는다.

## 2. 보안그룹 (AWS 콘솔)

인바운드 규칙에 **80(TCP) · 443(TCP)** 을 `0.0.0.0/0` 으로 추가한다.
80 은 인증서 발급·갱신 확인에 필요하므로 닫으면 안 된다.

## 3. `.env` 에 도메인 추가

EC2 의 `.env` 에 두 줄 넣는다.

```
API_DOMAIN=tangtang-api.duckdns.org
API_BIND=127.0.0.1
```

`API_BIND` 는 본서버 8080 을 **호스트 안쪽에만** 열어 인터넷 직결을 끊는다.
nginx 는 compose 네트워크로 `api:8080` 에 붙으므로 영향받지 않는다.

> ⚠ **이 줄은 5번(nginx 기동)과 같은 타이밍에 넣는다.** 먼저 넣고 nginx 없이 다시 띄우면
> Vercel 이 API 에 닿지 못해 서비스가 멈춘다.

## 4. 인증서 최초 발급

nginx 를 띄우기 **전에** 받는다. 아직 80 을 쓰는 것이 없으므로 standalone 방식이 가장 간단하다.

```bash
cd ~/Monorepo          # 저장소 위치에 맞게

docker run --rm -p 80:80 \
  -v tangtang_certbot_certs:/etc/letsencrypt \
  -v tangtang_certbot_webroot:/var/www/certbot \
  certbot/certbot certonly --standalone \
  -d "$API_DOMAIN" --agree-tos -m <팀_이메일> --no-eff-email
```

> 볼륨 이름 앞의 `tangtang_` 은 compose 프로젝트명 접두사다. 실제 이름은
> `docker volume ls | grep certbot` 로 확인해 맞춘다. 이름이 다르면 nginx 가
> 인증서를 못 찾아 기동에 실패한다.

`Successfully received certificate` 가 보이면 성공이다.

## 5. nginx 를 포함해 다시 띄운다

```bash
docker compose -f docker-compose.yml -f deploy/docker-compose.tls.yml up -d
```

이 시점에 **8080 은 더 이상 외부에 열리지 않는다.** 인터넷을 마주하는 것은 nginx 뿐이다.

확인:
```bash
curl https://$API_DOMAIN/api/health          # {"success":true,...}
curl -I http://$API_DOMAIN/api/health        # 301 → https
```

## 6. `vercel.json` 전환 (저장소 커밋)

**5번이 성공한 뒤에** 바꾼다. 먼저 바꾸면 프론트가 죽는다.

```json
{ "source": "/api/:path*",     "destination": "https://tangtang-api.duckdns.org/api/:path*" },
{ "source": "/uploads/:path*", "destination": "https://tangtang-api.duckdns.org/uploads/:path*" }
```

Vercel 이 재배포되면 브라우저부터 EC2 까지 전 구간이 암호화된다.

## 7. 갱신 확인

`certbot` 컨테이너가 12시간마다 갱신을 시도한다. 갱신돼도 nginx 는 옛 인증서를 물고 있으므로
주기적으로 reload 가 필요하다. EC2 의 crontab 에 한 줄 넣는다.

```
0 4 * * 1 cd ~/Monorepo && docker compose -f docker-compose.yml -f deploy/docker-compose.tls.yml exec -T nginx nginx -s reload
```

갱신 리허설: `docker compose ... run --rm certbot renew --dry-run`

---

## 문제가 생기면

| 증상 | 원인 |
|---|---|
| nginx 가 기동 직후 죽는다 | 인증서 경로가 없다. 4번 볼륨 이름을 확인한다 |
| 8080 이 아직 밖에서 열린다 | `.env` 에 `API_BIND=127.0.0.1` 이 없다. `docker compose ... up -d` 로 다시 적용 |
| 알림(SSE)이 안 온다 | `/api/notifications/stream` 의 `proxy_buffering off` 가 빠졌다 |
| 프로필 사진 업로드가 413 | `client_max_body_size` 가 20m 인지 확인 |
| 로그인이 유지되지 않는다 | `.env` 의 `AUTH_COOKIE_SECURE=true` 확인 |

**급하면 되돌린다.** `docker compose -f docker-compose.yml up -d` 로 다시 띄우면
nginx 없이 예전 상태(8080 직결)로 돌아간다. `vercel.json` 도 http 로 되돌린다.
