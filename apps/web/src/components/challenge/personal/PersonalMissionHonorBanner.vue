<!--
  용도: 개인 미션 홈에서 월간 명예의 전당 랭킹으로 이동시키는 티켓형 배너
  양옆 노치는 이미지가 아니라 카드 뒤 배경색을 칠한 원으로 표현
-->
<script setup>
defineProps({
    title: { type: String, default: '명예의 전당' },
    description: { type: String, default: '월간 랭킹 순위를 확인해보세요.' },
    buttonLabel: { type: String, default: '확인하기' },
});

defineEmits(['open']);
</script>

<template>
    <article class="honor-banner">
        <div class="honor-banner__content">
            <div class="honor-banner__heading">
                <h2>{{ title }}</h2>
                <span aria-hidden="true"></span>
            </div>
            <p>{{ description }}</p>
        </div>

        <button type="button" class="honor-banner__button" @click="$emit('open')">
            {{ buttonLabel }}
        </button>
    </article>
</template>

<style scoped>
.honor-banner {
    --tt-notch-bg: var(--tt-bg-subtle);

    position: relative;
    display: grid;
    grid-template-columns: minmax(0, 1fr) auto;
    grid-template-rows: auto auto;
    column-gap: var(--tt-space-4);
    row-gap: var(--tt-space-1);
    align-items: center;
    min-height: 100px;
    padding: var(--tt-space-5);
    overflow: hidden;
    color: var(--tt-text-inverse);
    background: var(--tt-info);
    border-radius: var(--tt-radius-lg);
    box-shadow: var(--tt-elevation-3);
}

/* 양옆의 원을 카드 바깥으로 절반 밀어 티켓 모양의 반원 홈을 만든다. */
.honor-banner::before,
.honor-banner::after {
    position: absolute;
    top: 50%;
    width: 24px;
    height: 24px;
    content: '';
    background: var(--tt-notch-bg);
    border-radius: var(--tt-radius-full);
    transform: translateY(-50%);
}

.honor-banner::before {
    left: -12px;
}

.honor-banner::after {
    right: -12px;
}

.honor-banner__content {
    display: contents;
}

.honor-banner__heading {
    grid-row: 1;
    grid-column: 1 / -1;
    display: flex;
    gap: var(--tt-space-3);
    align-items: center;
}

.honor-banner__heading h2 {
    flex: 0 0 auto;
    margin: 0;
    font-size: var(--tt-fs-section);
    font-weight: var(--tt-fw-black);
    line-height: var(--tt-lh-snug);
}

.honor-banner__heading span {
    flex: 1;
    min-width: var(--tt-space-5);
    border-top: 1px dashed var(--tt-info-subtle);
    opacity: 0.72;
}

.honor-banner__content p {
    grid-row: 2;
    grid-column: 1;
    margin: 0;
    font-size: var(--tt-fs-body);
    line-height: var(--tt-lh-normal);
    color: var(--tt-info-subtle);
}

.honor-banner__button {
    grid-row: 2;
    grid-column: 2;
    min-height: 40px;
    padding: 0 var(--tt-space-4);
    font-family: var(--tt-font-sans);
    font-size: var(--tt-fs-button);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
    background: var(--tt-primary-gold);
    border: 0;
    border-radius: var(--tt-radius-sm);
    cursor: pointer;
}

.honor-banner__button:hover {
    filter: brightness(0.96);
}

.honor-banner__button:focus-visible {
    outline: 2px solid var(--tt-text-inverse);
    outline-offset: 2px;
}

@media (max-width: 390px) {
    .honor-banner {
        gap: var(--tt-space-3);
        padding-right: var(--tt-space-4);
        padding-left: var(--tt-space-4);
    }

    .honor-banner__button {
        padding: 0 var(--tt-space-3);
    }
}

@media (max-width: 359px) {
    .honor-banner {
        min-height: 92px;
        padding: var(--tt-space-3);
    }

    .honor-banner__heading h2 {
        font-size: var(--tt-fs-body);
    }

    .honor-banner__button {
        padding: 0 var(--tt-space-2);
        white-space: nowrap;
    }
}
</style>
