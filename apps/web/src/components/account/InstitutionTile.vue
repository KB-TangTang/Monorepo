<!--
  용도: 금융기관 하나를 고르는 카드 (AC_01_01).
  언제 쓰는지: 기관 선택 · 추가 연결 화면의 3열 그리드.
  쓰면 안 되는 경우: 이미 연결된 계좌 목록의 행(ConnectedAccountRow).

  Figma 확정본(`금융기관 선택`) 구조 — 흰 카드에 로고를 위, 기관명을 아래.
  선택하면 파란 테두리 + 우상단 체크 배지. 로고는 InstitutionLogo 가 그린다.

  이미 연결된 기관은 connected=true 로 넘기면 `연결됨` 라벨이 붙는다. **탭은 막지 않는다** —
  connected 는 서버가 기관 단위로 판정하는 값이라(그 기관에 활성 계좌가 하나라도 있으면 true),
  막아버리면 같은 은행의 두 번째 계좌 추가 연결도, 재연동도 영원히 불가능해진다
  (2026-08-05 리뷰 지적). 중복 연결은 다음 단계(계좌 선택)의 alreadyLinked 가 막는다.
-->
<script setup>
import { computed } from 'vue';
import InstitutionLogo from '@/components/account/InstitutionLogo.vue';

const props = defineProps({
    institution: { type: Object, required: true },
    selected: { type: Boolean, default: false },
});

const emit = defineEmits(['toggle']);

/** 이미 연결된 기관인지. 표시만 바꾸고 선택은 막지 않는다. */
const connected = computed(() => props.institution.connected);

/**
 * 우하단 라벨.
 * 연결된 기관을 고르면 **무슨 일이 일어나는지** 말한다 — 남은 계좌를 추가하는 것이지
 * 이미 연결한 계좌를 다시 붙이는 게 아니다. "연결됨" 인데 눌리는 모순을 여기서 푼다.
 */
const flag = computed(() => {
    if (!connected.value) {
        return '';
    }
    return props.selected ? '계좌 추가' : '연결됨';
});

function onClick() {
    emit('toggle', props.institution.code);
}
</script>

<template>
    <button
        class="institution-tile"
        :class="{
            'institution-tile--on': selected,
        }"
        type="button"
        :aria-pressed="selected"
        @click="onClick"
    >
        <InstitutionLogo :code="institution.code" :short-label="institution.shortLabel" />
        <span class="institution-tile__name">{{ institution.name }}</span>
        <span v-if="flag" class="institution-tile__flag">{{ flag }}</span>
        <span v-if="selected" class="institution-tile__check" aria-hidden="true">✓</span>
    </button>
</template>

<style scoped>
.institution-tile {
    position: relative;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: var(--tt-space-2);
    padding: var(--tt-space-4) var(--tt-space-2);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-lg);
    background: var(--tt-bg);
    cursor: pointer;
}

.institution-tile--on {
    border-color: var(--tt-primary);
    border-width: 2px;
    /* 테두리가 두꺼워진 만큼 안쪽을 줄여 카드 크기가 흔들리지 않게 한다. */
    padding: calc(var(--tt-space-4) - 1px) calc(var(--tt-space-2) - 1px);
}

/*
 * 기관별 색조. 브랜드 색을 그대로 쓰면 HEX 하드코딩이 되므로
 * 디자인시스템의 의미 토큰 4계열에 배정했다 (DESIGN_SYSTEM.md 절대 규칙 1).
 */
.institution-tile__name {
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-medium);
    line-height: var(--tt-lh-snug);
    color: var(--tt-text);
    text-align: center;
}

.institution-tile__flag {
    font-size: var(--tt-fs-mono-chip);
    color: var(--tt-text-soft);
}

/* 선택 표시는 카드 모서리에 얹는다 — 이름 자리를 뺏지 않는다. */
.institution-tile__check {
    position: absolute;
    top: var(--tt-space-2);
    right: var(--tt-space-2);
    display: flex;
    align-items: center;
    justify-content: center;
    width: 20px;
    height: 20px;
    border-radius: var(--tt-radius-full);
    background: var(--tt-primary);
    color: var(--tt-text-inverse);
    font-size: var(--tt-fs-mono-chip);
}
</style>
