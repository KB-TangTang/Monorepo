# 탕탕 · 지갑재판소 — 디자인 시스템 v2

팀 디자인시스템에서 추출한 값을 코드 기준으로 고정한 문서다.
**원본 정의는 `apps/web/src/assets/tokens.css` 하나뿐이고, 이 문서는 그 값을 사람이 읽기 위한 표다.**
값을 바꿀 때는 두 곳을 같이 고친다.

## 절대 규칙

1. **색상 HEX 하드코딩 금지.** `#232842` 처럼 직접 쓰지 말고 `var(--tt-primary)` 를 쓴다.
2. **컴포넌트는 「의미 토큰」만 참조한다.** 원시 팔레트(`--tt-ink`, `--tt-red` …)는
   `tokens.css` 안의 의미 토큰 정의에서만 쓴다.
   - ⭕ `color: var(--tt-danger);`
   - ❌ `color: var(--tt-red);` (컴포넌트에서)
   - ❌ `color: #E0664B;`
3. 폰트 크기·굵기·간격·라운드·그림자도 토큰을 쓴다.
4. 갈색 계열(`--tt-wood`, `--tt-kraft`)은 **판사봉·인장·종이 질감에만** 쓴다.

---

## 1. 색상

### Brand — Ink + Gold 이원 체계

| 토큰 | 값 | 용도 |
|---|---|---|
| `--tt-ink` | `#232842` | 헤더 · 본문 텍스트 · 스탯 카드 |
| `--tt-ink-raised` | `#2E3556` | Ink 위에 겹치는 블록 |
| `--tt-gold` | `#F5B921` | 주요 CTA · 진행바 · 목숨 |
| `--tt-gold-soft` | `#FFF3D1` | 뱃지 배경 · 후광 |
| `--tt-gold-deep` | `#B67D06` | Gold soft 위 텍스트 |

> Ink는 구조, Gold는 강조. Gold는 화면당 1–2곳만.

### 중립 · 종이

| 토큰 | 값 | 용도 |
|---|---|---|
| `--tt-neutral-text` | `#232842` | 최고 대비 텍스트 |
| `--tt-neutral-body` | `#5A6076` | 본문 |
| `--tt-neutral-muted` | `#8A8FA3` | 보조 설명 |
| `--tt-neutral-hint` | `#A6A9B6` | 힌트 · placeholder |
| `--tt-neutral-surface` | `#FFFFFF` | 카드 배경 |
| `--tt-neutral-paper` | `#FAFAFB` | 페이지 · 섹션 배경 |
| `--tt-neutral-page` | `#F2F4F6` | 페이지 배경 — 선·그림자 없는 카드가 뜨는 바탕 |
| `--tt-neutral-fill` | `#EFF1F5` | 입력 필드 · 비활성 |
| `--tt-neutral-border` | `#E5E8EF` | 구분선 · 카드 테두리 |
| `--tt-neutral-track` | `#E9ECF2` | 진행바 트랙 |

### 상태 — 4색 체계 (진한값 / 옅은값 / deep)

| 상태 | 진한 | 옅은 | deep | soft-border | 도메인 매핑 |
|---|---|---|---|---|---|
| **Green** | `#2E9E6B` | `#E4F4EC` | `#1F6B48` | `#C9E6D6` | 성공 · 안전 · 무죄 |
| **Gold** | `#F5B921` | `#FFF3D1` | `#B67D06` | `#F0E0B8` | 주의 · 진행 중 · 기소 |
| **Red** | `#E0664B` | `#FBE9E4` | `#C24B31` | `#F3D3C9` | 초과 · 탈락 · 유죄 |
| **Blue** | `#3E63D6` | `#EAF0FF` | `#3E5299` | `#C9D6F5` | 정보 · 링크 · 기간 |

### 종이 · 목재 (절제 사용)

| 토큰 | 값 | 용도 |
|---|---|---|
| `--tt-wood` | `#9C7B54` | 판사봉 |
| `--tt-kraft` | `#EFE7D8` | 인장 · 종이 질감 |

### 재판탭 법원 헤더 (건물 일러스트 풀블리드)

개인챌린지(대법원) · 그룹챌린지(지방법원) 홈 상단 전용. 값은 건물 일러스트의 실제 픽셀 색에서 뽑았다.
이미지 가장자리와 패널이 이어져 보이게 하는 게 목적이라 **임의로 바꾸면 이음매가 드러난다.**

| 토큰 | 값 | 용도 |
|---|---|---|
| `--tt-court-navy` | `#1E2F4D` | 헤더 바탕 · 그라데이션 끝 |
| `--tt-court-top-supreme` | `#0D1E38` | 대법원 이미지 상단 색 |
| `--tt-court-top-district` | `#0C233F` | 지방법원 이미지 상단 색 |
| `--tt-court-fade-supreme` | `#152B47` | 대법원 이미지 하단 색 |
| `--tt-court-fade-district` | `#172C48` | 지방법원 이미지 하단 색 |
| `--tt-court-caption` | `#C3CFE4` | 헤더 부제 |
| `--tt-court-bell-ink` | `#1C2B4A` | 알림 벨 아이콘 |
| `--tt-court-bell-surface` | `rgba(255,255,255,.95)` | 알림 벨 원 |

> `--tt-court-top-*` 는 `src/utils/themeColor.js` 의 설치형 상태바 색과 **같은 값이어야** 한다.
> 한쪽만 고치면 상태바만 다른 색 띠로 뜬다 (테스트가 잡는다).

### 의미 토큰 — **컴포넌트는 이것만 쓴다**

| 토큰 | 참조 | 쓰는 곳 |
|---|---|---|
| `--tt-primary` | `--tt-ink` | Ink 버튼 · 헤더 · 활성 상태 |
| `--tt-primary-hover` | `--tt-ink-raised` | hover · pressed |
| `--tt-primary-gold` | `--tt-gold` | 게임 시작 · 다음 라운드 CTA |
| `--tt-primary-subtle` | `--tt-neutral-fill` | 선택된 칩 · 연한 강조 배경 |
| `--tt-danger` | `--tt-red` | 유죄 · 초과 · 삭제 |
| `--tt-danger-deep` | `--tt-red-deep` | Red soft 위 텍스트 |
| `--tt-danger-subtle` | `--tt-red-soft` | 유죄 배경 |
| `--tt-success` | `--tt-green` | 무죄 · 절감 성공 |
| `--tt-success-deep` | `--tt-green-deep` | Green soft 위 텍스트 |
| `--tt-success-subtle` | `--tt-green-soft` | 성공 배경 |
| `--tt-info` | `--tt-blue` | 정보 · 링크 · 기간 |
| `--tt-info-deep` | `--tt-blue-deep` | Blue soft 위 텍스트 |
| `--tt-info-subtle` | `--tt-blue-soft` | 정보 배경 |
| `--tt-accent` | `--tt-gold` | 배지 · 판사봉 포인트 |
| `--tt-accent-deep` | `--tt-gold-deep` | Gold soft 위 텍스트 |
| `--tt-accent-subtle` | `--tt-gold-soft` | 배지 배경 |
| `--tt-success-subtle-border` | `--tt-green-soft-border` | 무죄 카드 테두리 |
| `--tt-danger-subtle-border` | `--tt-red-soft-border` | 유죄 카드 테두리 |
| `--tt-text` | `--tt-neutral-text` | 제목 · 강조 텍스트 |
| `--tt-text-body` | `--tt-neutral-body` | 본문 |
| `--tt-text-muted` | `--tt-neutral-muted` | 보조 설명 · 캡션 |
| `--tt-text-hint` | `--tt-neutral-hint` | 힌트 · placeholder |
| `--tt-text-inverse` | `--tt-white` | 어두운 배경 위 텍스트 |
| `--tt-surface-inverse` | `--tt-ink` | Ink 스탯 카드 · 역전 배경 |
| `--tt-surface-raised` | `--tt-ink-raised` | Ink 위에 겹치는 블록 |
| `--tt-bg` | `--tt-neutral-surface` | 카드 배경 |
| `--tt-bg-subtle` | `--tt-neutral-paper` | 페이지 · 섹션 배경 |
| `--tt-bg-page` | `--tt-neutral-page` | 페이지 배경 — **선·그림자 없는 카드**가 뜨는 바탕 |
| `--tt-bg-fill` | `--tt-neutral-fill` | 입력 필드 · 비활성 배경 |
| `--tt-border` | `--tt-neutral-border` | 기본 선 |
| `--tt-border-track` | `--tt-neutral-track` | 진행바 트랙 |
| `--tt-court-header-bg` | `--tt-court-navy` | 법원 헤더 바탕 |
| `--tt-court-header-top-supreme` | `--tt-court-top-supreme` | 개인챌린지 헤더 세이프에어리어 |
| `--tt-court-header-top-district` | `--tt-court-top-district` | 그룹챌린지 헤더 세이프에어리어 |
| `--tt-court-header-fade-supreme` | `--tt-court-fade-supreme` | 개인챌린지 헤더 그라데이션 시작 |
| `--tt-court-header-fade-district` | `--tt-court-fade-district` | 그룹챌린지 헤더 그라데이션 시작 |
| `--tt-court-header-caption` | `--tt-court-caption` | 법원 헤더 부제 |
| `--tt-court-bell-bg` | `--tt-court-bell-surface` | 법원 헤더 알림 벨 원 |
| `--tt-court-bell-fg` | `--tt-court-bell-ink` | 법원 헤더 알림 벨 아이콘 |
| `--tt-overlay-dim` | `rgba(35,40,66,.48)` | 모달 · 바텀시트 뒤 딤 |
| `--tt-notch-bg` | `--tt-bg-subtle` | 카드 노치(영수증 절취선 · 소환장 펀치홀) |

---

## 2. 타이포

본문 **Pretendard Variable**, 수치·코드 **시스템 모노스페이스**.

| 스타일 | 크기 | 굵기 | 토큰 | 용도 |
|---|---|---|---|---|
| hero | 38px | 800 | `--tt-fs-hero` / `--tt-fw-black` | 결과 수치 · 순위 |
| stat | 30px | 800 | `--tt-fs-stat` / `--tt-fw-black` | 스탯 카드 숫자 |
| title | 23px | 800 | `--tt-fs-title` / `--tt-fw-black` | 화면 제목 (H1) |
| subtitle | 19px | 800 | `--tt-fs-subtitle` / `--tt-fw-black` | 카드 제목 (H2) |
| label | 15px | 800 | `--tt-fs-label` / `--tt-fw-black` | 섹션 라벨 · 리스트 이름 |
| body | 13.5px | 500 | `--tt-fs-body` / `--tt-fw-medium` | 본문 (기본값) |
| caption | 12px | 700 | `--tt-fs-caption` / `--tt-fw-bold` | 보조 설명 · 뱃지 |
| badge | 11.5px | 800 | `--tt-fs-badge` / `--tt-fw-black` | 뱃지 텍스트 |
| overline | 11px | 800 | `--tt-fs-overline` / `--tt-fw-black` | 오버라인 · 대문자 |
| button | 14.5px | 800 | `--tt-fs-button` / `--tt-fw-black` | 버튼 라벨 |

행간: `--tt-lh-tight` 1.1 (hero·stat) / `--tt-lh-snug` 1.25 (title·subtitle) / `--tt-lh-normal` 1.6 (body·caption)

제목은 항상 800, `letter-spacing: -.01em`. 굵기는 **800 / 700 / 600 / 500** 네 단계만.

---

## 3. Radius · Spacing · Elevation

| Radius | 값 | 용도 | | Spacing (4px 그리드) | 값 |
|---|---|---|---|---|---|
| `--tt-radius-xs` | 8px | 태그 | | `--tt-space-1` | 4px |
| `--tt-radius-sm` | 12px | 내부 요소 | | `--tt-space-2` | 8px |
| `--tt-radius-md` | 14px | 버튼 | | `--tt-space-3` | 12px |
| `--tt-radius-lg` | 18px | 리스트 행 · 배너 | | `--tt-space-4` | 16px |
| `--tt-radius-xl` | 22px | 카드 | | `--tt-space-5` | 20px |
| `--tt-radius-2xl` | 30px | 헤더 하단 곡률 | | `--tt-space-6` | 24px |
| `--tt-radius-full` | 999px | pill · 뱃지 | | `--tt-space-8` ~ `12` | 32~48px |

| 용도별 간격 | 토큰 | 값 |
|---|---|---|
| 화면 좌우 패딩 | `--tt-screen-padding` | 20px |
| 카드 내부 패딩 | `--tt-card-padding` | 16px |
| 카드 사이 간격 | `--tt-card-gap` | 10px |
| 리스트 행 간격 | `--tt-list-gap` | 8px |
| 최소 터치 영역 | `--tt-touch-min` | 44px |

| Elevation | 값 | 용도 |
|---|---|---|
| `--tt-elevation-0` | `none` | 평면 |
| `--tt-elevation-1` | `0 6px 16px rgba(35,40,66,.04)` | 리스트 행 |
| `--tt-elevation-2` | `0 8px 22px rgba(35,40,66,.05)` | 일반 카드 |
| `--tt-elevation-3` | `0 12px 28px rgba(35,40,66,.10)` | 겹치는 카드 · 토글 |
| `--tt-elevation-4` | `0 14px 30px -16px rgba(35,40,66,.55)` | Ink 스탯 카드 |
| `--tt-elevation-btn` | `0 10px 24px -12px rgba(35,40,66,.6)` | Primary 버튼 |
| `--tt-elevation-court-bell` | `0 6px 14px rgba(0,0,0,.25)` | 법원 헤더 알림 벨 |
| `--tt-elevation-court-speech` | `0 12px 14px rgba(0,0,0,.24)` | 법원 헤더 양피지 말풍선 (`filter: drop-shadow()` 전용 — spread 없음) |

---

## 4. z-index · 레이아웃

**컴포넌트에 z-index 숫자를 직접 쓰지 않는다.**

| 토큰 | 값 | 용도 |
|---|---|---|
| `--tt-z-base` | 0 | 기본 |
| `--tt-z-sticky` | 100 | 화면 상단 고정 헤더 |
| `--tt-z-tabbar` | 200 | 하단 탭바 |
| `--tt-z-overlay` | 900 | 모달 · 바텀시트 뒤 딤 |
| `--tt-z-modal` | 1000 | 모달 · 바텀시트 패널 |
| `--tt-z-toast` | 1100 | 토스트 · 스낵바 |

| 레이아웃 토큰 | 값 | 용도 |
|---|---|---|
| `--tt-tabbar-height` | 64px | 탭바 높이 |
| `--tt-app-bottom-inset` | 탭바 + safe-area + 16px | 앱 셸(`<main>`)이 탭바를 피해 잡는 아래 여백 |
| `--tt-float-toggle-inset` | 76px (44+8+8+16) | 하단에 **떠 있는 토글**을 피하는 본문 여백 |
| `--tt-content-max` | 480px | 본문 최대 폭 (모바일 웹 우선) |

- `--tt-app-bottom-inset` 은 **페이지가 직접 쓸 일이 거의 없다.** 그 자리는 페이지 요소 밖이라
  앱 셸의 흰 배경이 띠로 드러나는데, 이 보정은 `base.css` 의 `.tt-app__content > *` 규칙
  (투명 `border-bottom` + 같은 크기 음수 마진)이 **모든 화면에 대해 이미** 처리한다.
  화면에서 `padding-bottom` 으로 다시 더하면 여백이 두 배가 된다.
- `--tt-float-toggle-inset` 은 `ChallengeModeTabBar` · `AssetLedgerToggle` · `ChallengeReportToggle`
  처럼 **탭바 위에 떠 있는** 토글을 렌더링하는 화면만 본문 아래에 준다.
  탭바 몫은 위 규칙이 맡으므로 **여기에 `--tt-tabbar-height` 를 다시 더하지 않는다.**

---

## 5. 버튼 체계

| 버튼 | 배경 | 텍스트 | 용도 |
|---|---|---|---|
| Primary Ink | `--tt-primary` | `--tt-text-inverse` | 기본 이동 · 확정 |
| Primary Gold | `--tt-primary-gold` | `--tt-primary` | 게임 시작 · 다음 라운드 |
| Secondary | `--tt-bg` + border `--tt-primary` | `--tt-primary` | 외곽선 |
| Tertiary | `--tt-bg` + border `--tt-border` | `--tt-text-body` | 보조 (좁은 폭 2개 병치) |
| 무죄 | `--tt-bg` + border `--tt-success` | `--tt-success` | 재판 전용 |
| 유죄 | `--tt-bg` + border `--tt-danger` | `--tt-danger` | 재판 전용 |

한 화면에 Primary는 **1개**. Gold Primary는 게임을 시작·재개하는 순간에만.

---

## 6. 색상 사용 비율 (한 화면 기준)

| 영역 | 비율 |
|---|---|
| Paper · Surface | 56% |
| Ink | 24% |
| Gold | 12% |
| 상태색 | 8% |

> Gold가 12%를 넘으면 강조가 사라진다. 상태색은 뱃지·바에만.

---

## 7. 변경 이력

| 날짜 | 내용 |
|---|---|
| 2026-07-31 | 초기 팔레트 교체. Cool Gray · 타이포 · spacing · elevation 토큰 신설 |
| 2026-08-01 | `--tt-z-*` 6단계 · `--tt-tabbar-height` · `--tt-content-max` 신설 |
| 2026-08-01 | `--tt-overlay-dim` · `--tt-notch-bg` 신설 |
| 2026-08-03 | 고정지출 화면 의미 토큰 추가 |
| 2026-08-04 | `--tt-surface-inverse` 신설 |
| 2026-08-05 | **v2 전면 교체** — Ink/Gold 이원 체계, 4색 상태, 타이포 재조정, 7단 라운드, 4단 그림자 |
| 2026-08-21 | 재판탭 법원 헤더 토큰(`--tt-court-*`) · `--tt-elevation-court-bell` · `--tt-elevation-court-speech` 신설 |
| 2026-08-21 | 법원 헤더 말풍선을 양피지 PNG 로 되돌리며 `--tt-court-speech-*` 3개 삭제, `--tt-elevation-court-speech` 를 drop-shadow 용으로 교체, `--tt-elevation-court-nameplate` 추가 |
| 2026-08-21 | 목재 명패를 없애고 화자 이름을 말풍선 머리로 옮기며 `--tt-elevation-court-nameplate` 삭제 (신규 토큰 없음 — 헤더 하단 곡면은 기존 `--tt-radius-2xl` 사용) |
| 2026-08-21 | 페이지 배경 `--tt-neutral-paper` `#F7F8FA` → `#FAFAFB` (회색기 완화). `index.html` · `manifest.webmanifest` · `utils/themeColor.js` 의 종이색도 같은 값으로 맞춤 |
| 2026-08-22 | 주간 판정 「인정」 원을 `--tt-success` 채움으로 (파랑 그라데이션 시안 폐기 — `--tt-blue-bright` · `--tt-blue-strong` · `--tt-info-gradient` 삭제) |
| 2026-08-22 | `--tt-neutral-page` `#F2F4F6` → `--tt-bg-page` 신설. 선·그림자 없이 흰 카드만 세우는 화면의 배경 (재판탭 홈 2개에 시범 적용) |
| 2026-08-22 | `--tt-app-bottom-inset` · `--tt-float-toggle-inset` 신설. 탭바 아래 흰 띠 제거(전역 `base.css` 규칙)와 떠 있는 토글 회피 여백을 토큰화 |
