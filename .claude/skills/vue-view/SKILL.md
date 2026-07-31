---
name: vue-view
description: Vue3 화면(view)을 새로 추가하는 절차. view→router→store→api 순서와 디자인 토큰·API 호출 규칙을 강제한다. "화면 만들어줘", "페이지 추가", "컴포넌트 만들어줘" 요청 시 사용.
---

# Vue 화면 추가 절차

## 0. 시작 전
- `apps/web/AGENTS.md` 와 `docs/DOMAIN_GLOSSARY.md` 를 확인한다.
- 유사 화면이 이미 있는지 `src/views/` 를 훑는다.

## 1. API 모듈 — `src/api/<도메인>.js`
```js
import http from './http'
export function fetchFixedExpenses(params) {
  return http.get('/fixed-expenses', { params })
}
```
- **컴포넌트에서 axios 를 직접 쓰지 않는다.** 반드시 `http` 인스턴스 경유.
- 인터셉터가 공통 래퍼를 벗기므로 **payload 가 바로 반환된다.** `res.data.data` 같은 코드 금지.

## 2. 스토어 — `src/stores/<도메인>.js` (상태 공유가 필요할 때만)
- 도메인 단위 1개. 화면마다 만들지 않는다.
- 서버 원본은 state 에, 화면 전용 파생값은 `computed` 로.

## 3. 화면 — `src/views/XxxView.vue`
```vue
<script setup>
import { ref, onMounted } from 'vue'
import { fetchFixedExpenses } from '@/api/fixedExpense'

const items = ref([])
const error = ref(null)

onMounted(async () => {
  try {
    items.value = await fetchFixedExpenses()
  } catch (e) {
    error.value = e.message      // ApiError(code, message, status)
  }
})
</script>
```
- **Composition API + `<script setup>` 만.** Options API 금지.
- 로딩·에러 상태를 반드시 처리한다.

## 4. 라우터 등록 — `src/router/index.js`
```js
{ path: '/fixed-expenses', name: 'fixedExpenses',
  component: () => import('@/views/FixedExpenseView.vue') }
```
- 페이지는 lazy import.

## 5. 스타일
- 색상은 `src/assets/tokens.css` 의 CSS 변수만 사용. **HEX 하드코딩 금지.**
  `color: var(--tt-primary)` / `background: var(--tt-bg-subtle)`
- 의미 토큰(`--tt-primary` `--tt-danger` `--tt-success` `--tt-accent`)을 우선 쓴다.

## 6. 확인
`npm run dev` 로 띄워 실제 동작을 확인한다. 백엔드가 떠 있어야 `/api` 프록시가 통한다.
