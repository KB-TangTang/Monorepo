# perf — 부하테스트 (k6)

이슈 #188. **로컬 전용**이다. EC2 에 돌리려면 팀과 시간을 먼저 조율한다
(`docker-compose.yml` 기준 EC2 한 대에 DB·목서버·API 가 같이 떠 있어 경합이 섞이고, 시연에 쓰는 유일한 서버다).

## 준비

```powershell
winget install --id GrafanaLabs.k6      # 설치돼 있으면 건너뛴다
k6 version
```

백엔드가 로컬에 떠 있어야 한다. 확인:

```powershell
curl http://localhost:8080/api/health
```

## 1단계 — 대량 데이터 생성

기본 규모는 **사용자 50명 × 거래 1,000건 = 50,000건**이다. 규모는 `seed_loadtest_local.sql` 맨 위의
`@user_count` · `@tx_per_user` 로 조절한다.

```powershell
mysql -u tangtang -p tangtang --default-character-set=utf8mb4 `
  -e "source D:/KB_Final_Project/app/Monorepo/perf/seed_loadtest_local.sql"
```

마지막에 나오는 **`시작_user_id` · `끝_user_id` 를 적어둔다.** 2단계에서 쓴다.
이미 만들어 둔 뒤 값을 잊었다면:

```powershell
mysql -u tangtang -p tangtang -e "SELECT MIN(id), MAX(id), COUNT(*) FROM tbl_user WHERE provider_user_id LIKE 'LOADTEST-%'"
```

> `Get-Content | mysql` 은 PowerShell 기본 인코딩이 UTF-8 이 아니라 한글 주석이 깨진다. `source` 를 쓴다.

### ⚠ 가입일을 과거로 박는 이유

시드가 `created_at` 을 **13개월 전**으로 넣는다. 기본값(오늘)으로 두면 가입 첫 달 정책
(DECISIONS.md 2026-08-12)에 걸려 리포트 3종이 전부 **400 `REPORT_NOT_AVAILABLE`** 이 된다.
월간 리포트는 **완료된 월만** 조회할 수 있고, 진행 중인 이번 달은 `status=CURRENT` 로 막힌다.

같은 이유로 시나리오의 조회 대상 월 기본값은 **지난달**이다. `YEAR_MONTH` 에 이번 달을 넣으면
측정이 전부 400 으로 나온다 — 처음 돌릴 때 여기서 한 번 막혔다.

## 2단계 — 실행

`jwt.secret` 이 필요하다. **값을 직접 타이핑하지 말고 파일에서 읽어 넘긴다** — 화면·기록에 남지 않는다.

```powershell
cd D:\KB_Final_Project\app\Monorepo

$props  = "apps\api\src\main\resources\application-local.properties"
$secret = (Select-String -Path $props -Pattern '^jwt\.secret=(.+)$').Matches.Groups[1].Value

k6 run perf\report-read.js `
  -e JWT_SECRET=$secret `
  -e USER_ID_FROM=<시작_user_id> `
  -e USER_ID_TO=<끝_user_id>
```

조절할 수 있는 값:

| 변수 | 기본 | 뜻 |
|---|---|---|
| `BASE_URL` | `http://localhost:8080` | 대상 서버 |
| `VUS` | `20` | 동시 사용자 수 |
| `RAMP_UP` / `PLATEAU` | `30s` / `1m` | 증가 구간 / 유지 구간 |
| `YEAR_MONTH` | 이번 달 | 조회 대상 월 (`YYYY-MM`) |

동시 사용자를 바꿔가며 여러 번 돌려 표를 만든다:

```powershell
k6 run perf\report-read.js -e JWT_SECRET=$secret -e USER_ID_FROM=.. -e USER_ID_TO=.. -e VUS=50
```

## 3단계 — 결과 읽기

엔드포인트별로 지표를 나눠 뒀다. `http_req_duration` 하나만 보면 무거운 추이 조회가 가벼운 요약에
희석돼 「무엇이 느린가」에 답할 수 없기 때문이다.

| 지표 | 대상 |
|---|---|
| `tt_summary` | `GET /api/reports/monthly/summary` |
| `tt_categories` | `GET /api/reports/monthly/categories` |
| `tt_spending_trend` | `GET /api/reports/monthly/spending-trend` — 6개월 집계, 가장 무겁다 |
| `tt_users_me` | `GET /api/users/me` — 집계 없는 기준선 |

**기준선과 비교해서 읽는다.** `tt_users_me` 가 30ms 인데 `tt_spending_trend` 가 600ms 라면
그 차이가 집계 비용이다. 넷이 다 같이 느려지면 집계가 아니라 커넥션풀·톰캣 스레드 쪽을 본다.

`http_req_failed` 가 0 이 아니면 **응답시간 숫자는 읽지 않는다.** 실패한 요청은 대개 빨라서
p95 를 실제보다 좋게 만든다. 실패 원인부터 잡는다.

## 1차 측정 결과 (2026-08-13 · 로컬)

거래 **50,000건** (사용자 50명 × 1,000건), 로컬 톰캣 + 로컬 MySQL 8.4.
전 구간 **실패 0%**. 숫자는 p95.

| 동시 사용자 | 처리량 | `summary` | `spending-trend` | `categories` | `users/me` (기준선) |
|---:|---:|---:|---:|---:|---:|
| 20 | 121 req/s | 21.8ms | 19.6ms | 26.9ms | 13.0ms |
| 50 | 156 req/s | 16.8ms | 12.1ms | 20.2ms | 6.2ms |
| 100 | 298 req/s | 69.6ms | 56.6ms | 69.9ms | 49.2ms |
| 200 | 361 req/s | **429ms** | **406ms** | **423ms** | **394ms** |

### 읽는 법

**50 VU 가 20 VU 보다 빠르다.** 이상한 게 아니라 JIT·커넥션풀 워밍업이다.
첫 회 측정은 버리고 두 번째부터 읽는 편이 낫다.

**200 VU 에서 기준선까지 같이 느려진 것이 핵심이다.**
집계가 없는 `users/me` 가 394ms 인데 6개월치를 훑는 `spending-trend` 가 406ms 다 — 차이가 12ms 뿐이다.
느린 쿼리가 원인이면 무거운 것만 느려져야 한다. 넷이 나란히 같은 값으로 수렴한다는 것은
**쿼리를 기다리는 게 아니라 순서를 기다리고 있다**는 뜻이다.

처리량도 100 VU 298 req/s → 200 VU 361 req/s 로, 부하를 2배 걸었는데 1.2배밖에 안 늘었다.
**약 360 req/s 에서 천장에 닿았다.**

### 병목 후보 — HikariCP 기본 풀 크기

`RootConfig` 에 `maximumPoolSize` 를 지정한 곳이 없다 → **HikariCP 기본값 10** 이 그대로 쓰인다.
동시 200명이 커넥션 10개를 나눠 쓰면 위 패턴(전 엔드포인트 동일 수렴 + 처리량 포화)이 그대로 나온다.

**아직 고치지 않았다.** 풀 크기를 올리면 이번엔 MySQL 쪽이 병목이 되므로,
바꾸기 전에 `SHOW STATUS LIKE 'Threads_connected'` 와 톰캣 `maxThreads` 를 같이 봐야 한다.
지표 확보가 1차 목표였으므로 여기까지가 이번 범위다.

> 이 숫자는 **로컬 환경 기준**이다. 발표 자료에 쓸 때 반드시 명시한다.
> EC2 는 DB·목서버·API 가 한 대에 같이 떠 있어 더 낮게 나올 것이다.

## 정리

```powershell
mysql -u tangtang -p tangtang --default-character-set=utf8mb4 `
  -e "source D:/KB_Final_Project/app/Monorepo/perf/cleanup_loadtest_local.sql"
```

생성물은 전부 `provider_user_id LIKE 'LOADTEST-%'` · `codef_tr_key LIKE 'LOADTEST-%'` 로 식별된다.

## 부하 대상에서 제외한 것

| 제외 | 이유 |
|---|---|
| `POST /api/reports/monthly/ai-analysis` | **OpenAI 를 실제로 호출한다 — 과금된다** |
| 계좌 연동 계열 | 목서버를 실제로 호출한다 |
| `/api/dev/batches/*` | 배치 트리거 |

## 인증을 어떻게 통과하는가

구글 OAuth 는 자동화할 수 없지만 우회할 필요도 없다. `JwtProvider` 가 만드는 액세스 토큰은
클레임이 `sub`·`iat`·`exp` 뿐인 HS256 대칭키 서명이라, 서명키만 있으면 `lib/jwt.js` 가 같은 토큰을 만든다.
키 길이에 따라 jjwt 가 HS256/384/512 를 자동 선택하므로 그 규칙까지 맞춰 두었다.
