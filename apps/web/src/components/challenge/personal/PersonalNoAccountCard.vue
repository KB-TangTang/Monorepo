<script setup>
import { tangiMagnify } from '@/fixtures/personalChallenge';

defineEmits(['link-account']);
</script>

<template>
    <div class="no-account">
        <div class="no-account__cta-card">
            <img :src="tangiMagnify" alt="" class="no-account__tangi" />
            <div class="no-account__quote">"단서가 하나도 없군요"</div>
            <p class="no-account__desc">
                계좌를 연결하면 소비 현장 검증이 시작되고,<br />매일 나만의 사건이 배정돼요.
            </p>
            <button type="button" class="no-account__btn" @click="$emit('link-account')">
                계좌 연결하고 수사 개시
            </button>
            <p class="no-account__safe">연결해도 돈은 절대 빠져나가지 않아요</p>
        </div>

        <div class="no-account__steps-card">
            <div class="no-account__steps-title">연결하면 이렇게 진행돼요</div>
            <div class="no-account__steps">
                <div v-for="(step, i) in steps" :key="i" class="no-account__step">
                    <span class="no-account__step-num">{{ i + 1 }}</span>
                    <span class="no-account__step-text">{{ step }}</span>
                </div>
            </div>
        </div>
    </div>
</template>

<script>
const steps = [
    '탕이가 최근 28일 소비를 수사',
    '이번 주 요주의 대상 3곳 선정',
    '매일 자정, 수사 브리핑(미션)이 도착',
];
</script>

<style scoped>
.no-account {
    display: flex;
    flex-direction: column;
    gap: var(--tt-card-gap);
}

.no-account__cta-card {
    background: var(--tt-bg);
    border-radius: 20px;
    padding: var(--tt-space-5);
    text-align: center;
}

.no-account__tangi {
    width: 84px;
    height: 84px;
    object-fit: contain;
}

.no-account__quote {
    font-size: var(--tt-fs-subtitle);
    font-weight: var(--tt-fw-black);
    margin-top: var(--tt-space-3);
    letter-spacing: -0.01em;
}

.no-account__desc {
    font-size: var(--tt-fs-caption);
    color: var(--tt-text-muted);
    font-weight: var(--tt-fw-semibold);
    margin-top: 7px;
    line-height: var(--tt-lh-normal);
}

.no-account__btn {
    margin-top: var(--tt-space-4);
    width: 100%;
    background: var(--tt-surface-inverse);
    color: var(--tt-text-inverse);
    font-weight: var(--tt-fw-black);
    font-size: var(--tt-fs-label);
    padding: 15px;
    border-radius: var(--tt-radius-md);
    border: none;
    cursor: pointer;
    box-shadow: var(--tt-elevation-4);
    font-family: var(--tt-font-sans);
}

.no-account__safe {
    margin-top: 11px;
    font-size: var(--tt-fs-badge);
    color: var(--tt-text-hint);
    font-weight: var(--tt-fw-semibold);
}

.no-account__steps-card {
    background: var(--tt-bg);
    border-radius: var(--tt-radius-lg);
    padding: 14px var(--tt-space-4);
}

.no-account__steps-title {
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-black);
}

.no-account__steps {
    margin-top: 11px;
    display: flex;
    flex-direction: column;
    gap: 9px;
}

.no-account__step {
    display: flex;
    align-items: center;
    gap: var(--tt-space-3);
}

.no-account__step-num {
    width: 22px;
    height: 22px;
    border-radius: 50%;
    background: var(--tt-bg-fill);
    display: flex;
    align-items: center;
    justify-content: center;
    flex: none;
    font-size: var(--tt-fs-overline);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text-body);
    font-family: var(--tt-font-mono);
}

.no-account__step-text {
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-body);
}
</style>
