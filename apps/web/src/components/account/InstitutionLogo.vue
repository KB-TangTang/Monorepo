<!--
  용도: 금융기관을 나타내는 정사각 로고. 기관 선택·계좌 목록·완료·즉시 조회에서 같은 얼굴을 쓴다.
  언제 쓰는지: 기관 코드로 기관을 표시해야 하는 모든 자리.
  쓰면 안 되는 경우: 계좌 종류·상태를 나타내는 배지(AccountSyncBadge).

  로고 이미지는 팀 Figma export 기준으로 은행 9곳 + 카드 4곳이 있고, 계열사 3곳은 그룹 심볼을 공유한다.
  대출·페이머니(#344)는 저축은행 3곳 + 페이 5곳 + 현대캐피탈이 자기 파일을 갖고,
  캐피탈 4곳과 KB Pay 는 위와 같은 방식으로 지주사 심볼을 공유한다. **지금은 폴백으로 남은 기관이 없다.**
  **파일이 없는 기관은 브랜드 톤 배경 + 약칭**으로 그린다 — 한 화면에 이미지와 글자가 섞여도
  크기·모양·라운드가 같아 리듬은 유지된다.
  ⚠ 현대캐피탈은 가로로 긴 워드마크라 44px 타일에서 글자가 거의 안 읽힌다.
    정사각 심볼을 구하면 교체한다.
-->
<script setup>
import { computed } from 'vue';
import { resolveInstitutionTone } from '@/utils/account';

/**
 * 로고를 공유하는 계열사.
 *
 * 그룹 **심볼**(글자 없는 마크)을 쓰는 곳만 넣는다 — 신한·KB·NH 의 앱 아이콘은 심볼이라
 * 카드사·증권사에 그대로 써도 틀린 말이 되지 않는다.
 * ⚠ 워드마크(`Samsung Card` 처럼 글자가 든 것)는 여기 넣지 않는다.
 *   삼성증권 자리에 "Samsung Card" 가 뜨면 그건 잘못된 정보다.
 */
const LOGO_ALIASES = {
    '0301': '0088', // 신한카드 → 신한
    '0381': '0004', // KB국민카드 → KB
    '0218': '0004', // KB증권 → KB
    '0247': '0011', // NH투자증권 → NH농협
    /* 대출 업권(#344) — 캐피탈은 지주사 심볼을 그대로 쓴다. */
    CP_KB: '0004', // KB캐피탈 → KB
    CP_SHINHAN: '0088', // 신한캐피탈 → 신한
    CP_HANA: '0081', // 하나캐피탈 → 하나
    CP_WOORI: '0020', // 우리금융캐피탈 → 우리
    /* 페이머니 업권(#344) — KB Pay 는 KB국민카드가 운영한다. 나머지는 자기 로고 파일이 있다. */
    PAY_KB: '0004', // KB Pay → KB
};

/* Vite 가 빌드 시 해시된 URL 로 바꿔준다. 파일명은 기관 코드다. */
const LOGOS = import.meta.glob('@/assets/images/institutions/*.png', {
    eager: true,
    query: '?url',
    import: 'default',
});

const props = defineProps({
    code: { type: String, required: true },
    shortLabel: { type: String, default: '' },
    /** md = 44px(그리드 타일) · sm = 32px(목록 행) */
    size: { type: String, default: 'md' },
});

const src = computed(() => {
    const code = LOGO_ALIASES[props.code] ?? props.code;
    const found = Object.entries(LOGOS).find(([path]) => path.endsWith(`/${code}.png`));
    return found ? found[1] : null;
});
const tone = computed(() => resolveInstitutionTone(props.code));
</script>

<template>
    <img
        v-if="src"
        class="institution-logo"
        :class="`institution-logo--${size}`"
        :src="src"
        alt=""
        aria-hidden="true"
    />
    <span
        v-else
        class="institution-logo institution-logo--fallback"
        :class="[`institution-logo--${size}`, `institution-logo--${tone}`]"
        aria-hidden="true"
    >
        {{ shortLabel }}
    </span>
</template>

<style scoped>
.institution-logo {
    flex-shrink: 0;
    border-radius: var(--tt-radius-md);
    object-fit: contain;
}

.institution-logo--md {
    width: 44px;
    height: 44px;
}

.institution-logo--sm {
    width: 32px;
    height: 32px;
}

.institution-logo--fallback {
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: var(--tt-fs-mono-chip);
    font-weight: var(--tt-fw-bold);
}

.institution-logo--gold {
    background: var(--tt-accent);
    color: var(--tt-text);
}

.institution-logo--blue {
    background: var(--tt-info-subtle);
    color: var(--tt-info);
}

.institution-logo--green {
    background: var(--tt-success-subtle);
    color: var(--tt-success);
}

.institution-logo--rose {
    background: var(--tt-danger-subtle);
    color: var(--tt-danger);
}
</style>
