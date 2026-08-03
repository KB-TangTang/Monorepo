# 탕탕 · 지갑재판소 — 디자인 시스템

팀 디자인시스템(Figma)에서 추출한 값을 코드 기준으로 고정한 문서다.
**원본 정의는 `apps/web/src/assets/tokens.css` 하나뿐이고, 이 문서는 그 값을 사람이 읽기 위한 표다.**
값을 바꿀 때는 두 곳을 같이 고친다. Figma 접근 권한이 없어도 여기서 값을 찾을 수 있어야 한다.

## 절대 규칙

1. **색상 HEX 하드코딩 금지.** `#2f5ad0` 처럼 직접 쓰지 말고 `var(--tt-primary)` 를 쓴다.
2. **컴포넌트는 「의미 토큰」만 참조한다.** 원시 팔레트(`--tt-brand-700`, `--tt-gray-800` …)는
   `tokens.css` 안의 의미 토큰 정의에서만 쓴다. 그래야 나중에 팔레트를 통째로 바꿔도 화면이 안 깨진다.
   - ⭕ `color: var(--tt-danger);`
   - ❌ `color: var(--tt-guilty-700);` (컴포넌트에서)
   - ❌ `color: #c7515a;`
3. 폰트 크기·굵기·간격·라운드·그림자도 토큰을 쓴다 (`--tt-fs-*`, `--tt-fw-*`, `--tt-space-*`, `--tt-radius-*`, `--tt-elevation-*`).
4. 갈색 계열(`--tt-wood`, `--tt-kraft`)은 **판사봉·인장·종이 질감에만** 쓴다. 일반 배경·텍스트에 쓰지 않는다.

---

## 1. 색상

### Brand (Trust Blue) — 주색

| 토큰 | 값 | 용도 |
|---|---|---|
| `--tt-brand-900` | `#1E3E9C` | hover · pressed |
| `--tt-brand-700` | `#2F5AD0` | **Primary** — 메인 액션 |
| `--tt-brand-500` | `#5B7BC4` | 보조 |
| `--tt-brand-200` | `#BFD2F6` | border |
| `--tt-brand-100` | `#DAE7FB` | **Secondary** — 선택 상태 |
| `--tt-brand-50` | `#EAF0FB` | bg |

### Accent (Gold) — 배지 · 판사봉

| 토큰 | 값 | 용도 |
|---|---|---|
| `--tt-accent-700` | `#8A6410` | 텍스트용 진한 골드 (골드 배경 위 대비 확보) |
| `--tt-accent-500` | `#FFC338` | **Accent** |
| `--tt-accent-50` | `#FFF6E0` | bg |

### Cool Gray 10단계

| 토큰 | 값 | | 토큰 | 값 |
|---|---|---|---|---|
| `--tt-gray-50` | `#F8FAFD` | | `--tt-gray-500` | `#9DA8BD` |
| `--tt-gray-100` | `#F1F4F9` | | `--tt-gray-600` | `#8792AE` |
| `--tt-gray-200` | `#E5EAF2` | | `--tt-gray-700` | `#68728F` |
| `--tt-gray-300` | `#D3DAE7` | | `--tt-gray-800` | `#444C68` |
| `--tt-gray-400` | `#B8C2D3` | | `--tt-gray-900` | `#252B42` (Navy — 수치카드·CTA) |
| | | | `--tt-ink` | `#1B2138` (최고 대비 텍스트) |

### 판정 (시맨틱)

| 토큰 | 값 | 용도 |
|---|---|---|
| `--tt-guilty-700` | `#C7515A` | 유죄 · 초과 · 위험 |
| `--tt-guilty-300` | `#F0D4D7` | 유죄 border |
| `--tt-guilty-50` | `#FBEDEE` | 유죄 bg |
| `--tt-innocent-800` | `#2F7A62` | 무죄 강조 |
| `--tt-innocent-700` | `#3E9B7E` | 무죄 · 절약 · 성공 |
| `--tt-innocent-300` | `#CDE5D9` | 무죄 border |
| `--tt-innocent-50` | `#E7F2ED` | 무죄 bg |

### 종이 · 목재 (절제 사용)

| 토큰 | 값 | 용도 |
|---|---|---|
| `--tt-wood` | `#9C7B54` | 판사봉 |
| `--tt-kraft` | `#EFE7D8` | 인장 · 종이 질감 |

### 의미 토큰 — **컴포넌트는 이것만 쓴다**

| 토큰 | 참조 | 쓰는 곳 |
|---|---|---|
| `--tt-primary` | `--tt-brand-700` | 기본 버튼 · 링크 · 활성 탭 |
| `--tt-primary-hover` | `--tt-brand-900` | hover · pressed |
| `--tt-primary-subtle` | `--tt-brand-100` | 선택된 칩 · 연한 강조 배경 |
| `--tt-danger` | `--tt-guilty-700` | 유죄 · 초과 · 삭제 |
| `--tt-danger-subtle` | `--tt-guilty-50` | 유죄 배경 |
| `--tt-success` | `--tt-innocent-700` | 무죄 · 절감 성공 |
| `--tt-success-subtle` | `--tt-innocent-50` | 성공 배경 |
| `--tt-accent` | `--tt-accent-500` | 배지 · 판사봉 포인트 |
| `--tt-accent-subtle` | `--tt-accent-50` | 배지 배경 |
| `--tt-text` | `--tt-ink` | 본문 |
| `--tt-text-muted` | `--tt-gray-700` | 보조 설명 · 캡션 |
| `--tt-text-inverse` | `--tt-white` | 어두운 배경 위 텍스트 |
| `--tt-bg` | `--tt-white` | 기본 배경 |
| `--tt-bg-subtle` | `--tt-gray-50` | 섹션 배경 |
| `--tt-border` | `--tt-gray-200` | 기본 선 |
| `--tt-border-strong` | `--tt-gray-400` | 강조 선 · 입력 포커스 전 |
| `--tt-overlay-dim` | `rgba(27,33,56,.48)` | 모달 · 바텀시트 뒤 딤 |
| `--tt-notch-bg` | `--tt-bg-subtle` | 카드 노치(영수증 절취선 · 소환장 펀치홀) |

> `--tt-overlay-dim` 은 `--tt-ink` 에 투명도를 준 값이다. CSS 변수로는 색상 채널만 따로 꺼내
> 쓸 수 없어 값을 직접 적었다. 딤 농도를 바꿀 일이 생기면 이 한 곳만 고친다.
>
> `--tt-notch-bg` 는 **카드 뒤에 보이는 배경색**이어야 한다. 노치는 진짜 구멍이 아니라 그 색을
> 칠한 원이기 때문이다. 흰 배경 화면에서 `ReceiptCard` · `SummonsCard` 를 쓸 때는
> 그 화면에서 `--tt-notch-bg: var(--tt-bg)` 로 덮어쓴다.

---

## 2. 타이포

본문 **Pretendard**, 수치·코드 **Roboto Mono**. 둘 다 `apps/web/index.html` 에서 CDN 로드하고
`--tt-font-sans` / `--tt-font-mono` 로 참조한다.

| 스타일 | 크기 | 굵기 | 토큰 | 용도 |
|---|---|---|---|---|
| display | 44px | 800 | `--tt-fs-display` / `--tt-fw-black` | 랜딩·판결 헤드라인 |
| title | 26px | 800 | `--tt-fs-title` / `--tt-fw-black` | 화면 제목 |
| numeric | 34px | 800 | `--tt-fs-numeric` / `--tt-fw-black` | 금액·수치 카드 |
| section | 18px | 700 | `--tt-fs-section` / `--tt-fw-bold` | 섹션 헤더 |
| body | 15px | 400 | `--tt-fs-body` / `--tt-fw-regular` | 본문 (기본값) |
| caption | 13px | 500 | `--tt-fs-caption` / `--tt-fw-medium` | 보조 설명 |
| mono-chip | 12px | 400~700 | `--tt-fs-mono-chip` + `--tt-font-mono` | 사건번호·참여코드 |

행간: `--tt-lh-tight` 1.2 (display·title·numeric) / `--tt-lh-snug` 1.4 (section) / `--tt-lh-normal` 1.6 (body·caption)

```css
.amount {
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-numeric);
    font-weight: var(--tt-fw-black);
    line-height: var(--tt-lh-tight);
}
```

---

## 3. Radius · Spacing · Elevation

| Radius | 값 | | Spacing (4px 그리드) | 값 |
|---|---|---|---|---|
| `--tt-radius-xs` | 4px | | `--tt-space-1` | 4px |
| `--tt-radius-sm` | 6px | | `--tt-space-2` | 8px |
| `--tt-radius-md` | 12px | | `--tt-space-3` | 12px |
| `--tt-radius-lg` | 20px | | `--tt-space-4` | 16px |
| `--tt-radius-xl` | 28px | | `--tt-space-5` | 20px |
| `--tt-radius-full` | 999px (pill) | | `--tt-space-6` | 24px |
| | | | `--tt-space-8` | 32px |
| | | | `--tt-space-10` | 40px |
| | | | `--tt-space-12` | 48px |

| Elevation | 값 | 용도 |
|---|---|---|
| `--tt-elevation-0` | `none` | 평면 |
| `--tt-elevation-1` | `0 1px 2px rgba(37,43,66,.06)` | 카드 |
| `--tt-elevation-2` | `0 2px 8px rgba(37,43,66,.08)` | 떠 있는 카드 · 탭바 |
| `--tt-elevation-3` | `0 8px 24px rgba(37,43,66,.12)` | 모달 · 바텀시트 |

---

## 4. z-index · 레이아웃

**컴포넌트에 z-index 숫자를 직접 쓰지 않는다.** 각자 숫자를 고르면 화면마다 겹침 순서가 달라진다.

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
| `--tt-tabbar-height` | 60px | 탭바 높이. 콘텐츠 하단 여백 계산에 쓴다 |
| `--tt-content-max` | 480px | 본문 최대 폭 (모바일 웹 우선) |

---

## 5. 변경 이력

| 날짜 | 내용 |
|---|---|
| 2026-07-31 | 초기 세팅 팔레트(Verdict Red `#E5484D` / Acquit Mint `#12A594` / 중립 그레이)를 팀 디자인시스템 값으로 교체. Cool Gray 10단계 · 타이포 스케일 · spacing · elevation 토큰 신설 |
| 2026-08-01 | 공통 컴포넌트 작업(P2~P4)에 맞춰 `--tt-z-*` 6단계 · `--tt-tabbar-height` · `--tt-content-max` 신설 |
| 2026-08-01 | 컴포넌트에 남아 있던 색상 하드코딩을 걷어내며 `--tt-overlay-dim` · `--tt-notch-bg` 신설 |
