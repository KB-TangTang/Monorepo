# apps/web — 탕탕 프론트엔드

Vue3 + Vite. 작업 규칙은 `AGENTS.md` 에 있다.

## 실행

```sh
npm install
npm run dev        # http://localhost:5173  (/api 요청은 8080 으로 프록시)
npm run build      # 프로덕션 빌드 → dist/
npm run format     # Prettier 적용 (들여쓰기 4칸 · 싱글 쿼트 · 세미콜론)
npm run lint:check # ESLint
```

Node 는 **24.14.1** 을 쓴다 (`.nvmrc`).

## ⚠️ 화면 작업 전에 `/dev/ui` 를 먼저 확인한다

개발 서버를 띄우고 <http://localhost:5173/dev/ui> 를 연다.
공통 컴포넌트(카드·버튼·입력·모달·바텀시트·배지·상태 3종·탭바)와 컬러 토큰·타이포 스케일이
용도 설명 + 복사용 코드와 함께 한 화면에 정리돼 있다.

- **새 공통 컴포넌트를 만들기 전에 여기부터 본다.** 확인하지 않으면 같은 걸 여러 명이 각자 만든다.
- 화면 전용 조각은 `src/components/<도메인>/` 에 만들고, **세 번째 화면에서 또 필요해지면**
  그때 `src/components/common/` 으로 올린다.
- `common/` 에 추가했다면 **`src/views/dev/UiCatalogView.vue` 에도 등록**한다.
- 모달·바텀시트는 반드시 `BaseModal` / `BaseBottomSheet` 를 쓴다. 각자 만들면 z-index·스크롤 처리가 어긋난다.
- 색상·간격·폰트는 `src/assets/tokens.css` 변수만 쓴다. **HEX 하드코딩 금지** (값 표: `docs/DESIGN_SYSTEM.md`).

카탈로그 라우트는 `import.meta.env.DEV` 가드로 막혀 있어 프로덕션 빌드에는 들어가지 않는다.

## 권장 IDE

[VS Code](https://code.visualstudio.com/) + [Vue (Official)](https://marketplace.visualstudio.com/items?itemName=Vue.volar) (Vetur 는 비활성화).
브라우저에는 [Vue.js devtools](https://chromewebstore.google.com/detail/vuejs-devtools/nhdogjmejiglipccpnnnanhbledajbpd) 를 설치하면 편하다.
