<script setup>
import { computed } from 'vue';
import analyzingTangi from '@/assets/images/prosecutor_tangtang/magnifying_stare.png';
import { formatPeriod } from '@/utils/challengeReport';
import BaseButton from '@/components/common/BaseButton.vue';

const props = defineProps({
    state: { type: String, required: true },
    period: { type: String, default: '' },
    title: { type: String, required: true },
    description: { type: String, required: true },
});

defineEmits(['start-challenge']);

const isPreparing = computed(() => props.state === 'preparing');
const statusLabel = computed(() => {
    if (props.state === 'not-agreed') {
        return '챌린지 참여 준비 중';
    }
    if (isPreparing.value) {
        return '첫 리포트 분석 중';
    }
    return '첫 챌린지 시작 전';
});

const publishCopy = computed(() =>
    isPreparing.value
        ? '한 달의 기록이 모이면 첫 재판 보고서가 열려요'
        : '첫 챌린지를 마치면 재판 보고서가 열려요',
);
</script>

<template>
    <section class="challenge-onboarding" aria-labelledby="challenge-onboarding-title">
        <p v-if="period" class="challenge-onboarding__period">{{ formatPeriod(period) }}</p>
        <div class="challenge-onboarding__visual" aria-hidden="true">
            <span class="challenge-onboarding__orbit"></span>
            <span class="challenge-onboarding__paper challenge-onboarding__paper--left"></span>
            <span class="challenge-onboarding__paper challenge-onboarding__paper--right"></span>
            <img :src="analyzingTangi" alt="" />
        </div>
        <span class="challenge-onboarding__status">{{ statusLabel }}</span>
        <h2 id="challenge-onboarding-title">{{ title }}</h2>
        <p class="challenge-onboarding__copy">{{ description }}</p>

        <div class="challenge-onboarding__guide">
            <p class="challenge-onboarding__guide-title">첫 재판 보고서에서 확인할 수 있어요</p>
            <ul>
                <li>미션<br />성공률</li>
                <li>카테고리별<br />절감액</li>
                <li>난이도별<br />성과</li>
            </ul>
            <p class="challenge-onboarding__publish">{{ publishCopy }}</p>
        </div>

        <BaseButton
            v-if="!isPreparing"
            block
            size="lg"
            class="challenge-onboarding__cta"
            @click="$emit('start-challenge')"
        >
            챌린지 하러가기
        </BaseButton>
    </section>
</template>

<style scoped>
.challenge-onboarding {
    display: flex;
    flex: 1;
    flex-direction: column;
    align-items: center;
    min-height: 66vh;
    padding-top: clamp(var(--tt-space-5), 4.5vh, 42px);
    text-align: center;
}

.challenge-onboarding__period {
    align-self: flex-start;
    margin-bottom: calc(var(--tt-space-4) * -1);
    padding: var(--tt-space-2) var(--tt-space-4);
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-black);
    color: var(--tt-primary);
    background: var(--tt-primary-subtle);
    border-radius: var(--tt-radius-full);
}

.challenge-onboarding__visual {
    position: relative;
    display: grid;
    width: 166px;
    height: 166px;
    margin-bottom: var(--tt-space-5);
    place-items: center;
}

.challenge-onboarding__visual::before {
    position: absolute;
    inset: 9px;
    content: '';
    background: linear-gradient(145deg, var(--tt-bg), var(--tt-accent-subtle));
    border: 1px solid var(--tt-accent-subtle-border);
    border-radius: var(--tt-radius-full);
}

.challenge-onboarding__orbit {
    position: absolute;
    inset: 0;
    border: 1px dashed color-mix(in srgb, var(--tt-accent-strong) 26%, transparent);
    border-radius: var(--tt-radius-full);
    animation: challenge-onboarding-orbit 24s linear infinite;
}

.challenge-onboarding__paper {
    position: absolute;
    z-index: 1;
    display: grid;
    width: 34px;
    height: 38px;
    background: var(--tt-bg);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-md);
    box-shadow: var(--tt-elevation-2);
    place-items: center;
}

.challenge-onboarding__paper::before {
    width: 18px;
    height: 2px;
    content: '';
    background: var(--tt-border-strong);
    border-radius: var(--tt-radius-full);
    box-shadow: 0 6px 0 var(--tt-border);
}

.challenge-onboarding__paper--left {
    top: 43px;
    left: -1px;
    transform: rotate(-9deg);
}

.challenge-onboarding__paper--right {
    right: -1px;
    bottom: 25px;
    transform: rotate(8deg);
}

.challenge-onboarding__visual img {
    position: relative;
    z-index: 2;
    width: 142px;
    height: 142px;
    object-fit: contain;
    filter: drop-shadow(0 12px 12px color-mix(in srgb, var(--tt-text) 12%, transparent));
    animation: challenge-onboarding-breathe 3.2s ease-in-out infinite;
}

.challenge-onboarding__status {
    display: inline-flex;
    align-items: center;
    gap: var(--tt-space-2);
    min-height: 29px;
    margin-bottom: var(--tt-space-3);
    padding: var(--tt-space-1) var(--tt-space-3);
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-black);
    color: var(--tt-accent-strong);
    background: var(--tt-accent-subtle);
    border-radius: var(--tt-radius-full);
}

.challenge-onboarding__status::before {
    width: 7px;
    height: 7px;
    content: '';
    background: var(--tt-accent);
    border-radius: var(--tt-radius-full);
    animation: challenge-onboarding-pulse 1.8s ease-out infinite;
}

.challenge-onboarding h2 {
    font-size: clamp(20px, 5.1vw, 23px);
    font-weight: var(--tt-fw-black);
    line-height: 1.4;
    letter-spacing: -0.055em;
    word-break: keep-all;
}

.challenge-onboarding__copy {
    max-width: 27em;
    margin-top: var(--tt-space-2);
    color: var(--tt-text-muted);
    font-size: var(--tt-fs-body);
    line-height: 1.65;
    white-space: pre-line;
    word-break: keep-all;
}

.challenge-onboarding__guide {
    width: 100%;
    margin-top: clamp(var(--tt-space-6), 4.5vh, 38px);
    padding: var(--tt-space-4);
    text-align: left;
    background: var(--tt-bg);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-xl);
    box-shadow: var(--tt-elevation-1);
}

.challenge-onboarding__guide-title {
    font-weight: var(--tt-fw-black);
}

.challenge-onboarding__guide-title::before {
    display: inline-grid;
    width: 24px;
    height: 24px;
    margin-right: var(--tt-space-2);
    content: 'i';
    color: var(--tt-accent-strong);
    background: var(--tt-accent-subtle);
    border-radius: var(--tt-radius-full);
    place-items: center;
}

.challenge-onboarding ul {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: var(--tt-space-2);
    margin-top: var(--tt-space-3);
}

.challenge-onboarding li {
    display: grid;
    min-height: 56px;
    padding: var(--tt-space-2);
    color: var(--tt-text-muted);
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
    text-align: center;
    background: var(--tt-bg-subtle);
    border-radius: var(--tt-radius-lg);
    place-items: center;
}

.challenge-onboarding__publish {
    margin-top: var(--tt-space-3);
    color: var(--tt-text-muted);
    font-size: var(--tt-fs-caption);
    text-align: center;
}

.challenge-onboarding__cta {
    margin-top: var(--tt-space-5);
}

@keyframes challenge-onboarding-breathe {
    0%,
    100% {
        transform: translateY(0) rotate(-1deg);
    }

    50% {
        transform: translateY(-6px) rotate(1deg);
    }
}

@keyframes challenge-onboarding-pulse {
    0% {
        box-shadow: 0 0 0 0 color-mix(in srgb, var(--tt-accent) 38%, transparent);
    }

    70%,
    100% {
        box-shadow: 0 0 0 7px transparent;
    }
}

@keyframes challenge-onboarding-orbit {
    to {
        transform: rotate(360deg);
    }
}

@media (prefers-reduced-motion: reduce) {
    .challenge-onboarding__orbit,
    .challenge-onboarding__status::before,
    .challenge-onboarding__visual img {
        animation: none;
    }
}

@media (max-width: 360px) {
    .challenge-onboarding {
        padding-top: var(--tt-space-4);
    }

    .challenge-onboarding__visual {
        width: 150px;
        height: 150px;
        margin-bottom: var(--tt-space-4);
    }

    .challenge-onboarding__visual img {
        width: 128px;
        height: 128px;
    }

    .challenge-onboarding__guide {
        margin-top: var(--tt-space-5);
    }
}
</style>
