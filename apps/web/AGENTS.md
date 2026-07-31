# apps/web — 프론트엔드 규칙

루트 `AGENTS.md` 의 규칙을 전제로 한다. 여기에는 **이 폴더에서만 다른 것**만 적는다.

## Vue 작성 방식 (고정)

- **Composition API + `<script setup>` 만 사용한다.** Options API 혼용 금지.
  6명이 섞어 쓰면 리뷰 비용이 급증한다.
- 컴포넌트 파일명은 PascalCase (`FixedExpenseCard.vue`). 페이지는 `views/`, 조각은 `components/`.
- `@/` 별칭이 `src/` 를 가리킨다. 상대경로 `../../` 대신 별칭을 쓴다.

## 폴더

```
src/api/         API 호출 모듈. 도메인별 파일 (health.js, user.js …)
src/views/       라우트에 연결되는 페이지
src/components/  재사용 조각
src/stores/      Pinia 스토어
src/router/      라우팅
src/assets/      tokens.css(디자인 토큰) · 전역 스타일
```

## API 호출

- **컴포넌트에서 `axios` 를 직접 import 하지 않는다.** 반드시 `src/api/http.js` 인스턴스를 쓴다.
- 도메인별 함수는 `src/api/<도메인>.js` 에 모으고, 컴포넌트는 그 함수만 호출한다.
- `http.js` 인터셉터가 백엔드 공통 래퍼(`{success, data}`)를 **이미 벗겨서** 반환한다.
  호출부는 `data` 안의 실제 payload 를 바로 받는다. `res.data.data` 같은 코드를 쓰지 말 것.
- 실패는 `ApiError(code, message, status)` 로 정규화돼 reject 된다. `err.code` 로 분기한다.
- 개발 중 `/api` 요청은 `vite.config.js` 프록시가 `localhost:8080` 으로 넘긴다. CORS 설정 불필요.

```js
import { fetchHealth } from '@/api/health'
const data = await fetchHealth()   // { status: 'UP', service: 'tangtang-api' }
```

## 디자인 토큰

`src/assets/tokens.css` 의 CSS 변수만 사용한다. **색상 HEX 하드코딩 금지.**

| 토큰 | 값 | 용도 |
|---|---|---|
| `--tt-trust-blue` | `#2F5AD0` | 주색 · 신뢰 |
| `--tt-gavel-yellow` | `#FFC338` | 강조 · 판사봉 |
| `--tt-verdict-red` | `#E5484D` | 경고 · 유죄 |
| `--tt-acquit-mint` | `#12A594` | 긍정 · 절감 성공 |

의미 토큰(`--tt-primary`, `--tt-danger`, `--tt-success`, `--tt-accent`)을 우선 쓰고,
브랜드 토큰은 그 정의에서만 참조한다.

## 상태 관리

- Pinia 스토어는 **도메인 단위 1개**. 화면마다 만들지 않는다.
- 서버에서 받은 원본 데이터는 스토어에, 화면 전용 파생값은 `computed` 로 만든다.
- 토큰·로그인 상태는 인증 스토어 한 곳에서만 관리한다. `http.js` 요청 인터셉터가 여기서 토큰을 읽는다.

## 라우팅

- 라우트 이름은 kebab-case 경로 + camelCase name.
- 페이지 컴포넌트는 lazy import (`component: () => import('@/views/...')`).
- 하단 5탭 구조: 재판 · 자산 · 홈 · 장부 · 마이.
