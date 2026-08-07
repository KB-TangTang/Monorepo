<!--
  용도: 계좌 연결 플로우 4단계가 공유하는 상단 헤더. 뒤로가기 · 타이틀 · 진행 표시를 담는다.
  언제 쓰는지: /accounts/link/* 단계 화면.
  쓰면 안 되는 경우: 탭바가 있는 화면(연결 계좌 관리 등). 그쪽은 자체 헤더를 쓴다.
-->
<script setup>
import { computed } from 'vue';

const props = defineProps({
    /** 네비게이션 줄에 들어가는 짧은 제목 (예: `금융기관 연결`). */
    title: { type: String, required: true },
    /**
     * 화면의 큰 헤드라인. Figma 확정본은 네비 제목과 헤드라인을 분리한다 —
     * 네비는 "지금 어느 플로우인지", 헤드라인은 "이 화면에서 뭘 하는지"를 말한다.
     * `|` 로 줄을 나눈다(템플릿 속성에 개행을 직접 넣으면 Vue 파서가 끊는다).
     * 생략하면 네비 제목만 그린다.
     */
    headline: { type: String, default: '' },
    description: { type: String, default: '' },
    /* 진행 표시 `2 / 4`. 생략하면 표시하지 않는다. */
    step: { type: Number, default: 0 },
    totalSteps: { type: Number, default: 0 },
    showBack: { type: Boolean, default: true },
});

defineEmits(['back']);

/* 줄바꿈은 script 에서 나눈다 — 템플릿 속성 안의 개행 이스케이프는 Vue 파서가 끊는다. */
const headlineLines = computed(() => (props.headline ? props.headline.split('|') : []));
</script>

<template>
    <header class="link-header">
        <div class="link-header__row">
            <button
                v-if="showBack"
                class="link-header__back"
                type="button"
                aria-label="이전 단계로"
                @click="$emit('back')"
            >
                ‹
            </button>
            <p class="link-header__nav">{{ title }}</p>
            <span v-if="step && totalSteps" class="link-header__step">
                {{ step }} / {{ totalSteps }}
            </span>
        </div>
        <h1 v-if="headlineLines.length" class="link-header__headline">
            <template v-for="(line, index) in headlineLines" :key="index">
                {{ line }}<br v-if="index < headlineLines.length - 1" />
            </template>
        </h1>

        <p v-if="description" class="link-header__description">{{ description }}</p>
    </header>
</template>

<style scoped>
.link-header {
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-2);
}

.link-header__row {
    display: flex;
    align-items: center;
    gap: var(--tt-space-3);
}

.link-header__back {
    border: 0;
    background: none;
    padding: 0;
    line-height: 1;
    font-size: var(--tt-fs-title);
    color: var(--tt-text);
    cursor: pointer;
}

/* 네비 줄. 헤드라인보다 작고 조용하다. */
.link-header__nav {
    flex: 1;
    margin: 0;
    font-size: var(--tt-fs-section);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text);
}

/* 화면의 주제문. Figma 확정본은 여기가 26px black 2줄이다. */
.link-header__headline {
    margin: var(--tt-space-3) 0 0;
    font-size: var(--tt-fs-title);
    font-weight: var(--tt-fw-black);
    line-height: var(--tt-lh-tight);
    color: var(--tt-text);
}

.link-header__step {
    flex-shrink: 0;
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-caption);
    color: var(--tt-text-muted);
}

.link-header__description {
    margin: 0;
    font-size: var(--tt-fs-caption);
    line-height: var(--tt-lh-normal);
    color: var(--tt-text-muted);
}
</style>
