<script setup>
import { ref, computed, watch, onUnmounted } from 'vue';

import mascotHappy from '@/assets/images/emotions/01_happy.png';
import mascotJudging from '@/assets/images/emotions/48_judging.png';
import mascotExcited from '@/assets/images/emotions/05_excited.png';
import lifeAlive from '@/assets/images/challenge_live/gavel-alive.png';
import lifeDepleted from '@/assets/images/challenge_live/gavel-depleted.png';

const props = defineProps({
    modelValue: {
        type: Boolean,
        required: true,
    },
});

const emit = defineEmits(['update:modelValue', 'complete']);

const step = ref(0);
const TOTAL_STEPS = 3;

const steps = [
    {
        key: 'rules',
        overline: 'RULES',
        title: '규칙을 함께 정해요',
        description: '기간(최대 7일)·소비 카테고리·제한 금액을 정하고 친구를 초대해요.',
        ctaLabel: '다음',
        ctaVariant: 'ink',
        mascot: mascotHappy,
        mascotAlt: '탕이 안내',
    },
    {
        key: 'trial',
        overline: 'TRIAL',
        title: '한도를 넘기면 재판이 열려요',
        description: '당사자는 변론을, 그룹원은 익명으로 유·무죄 투표와 한줄 코멘트를 남겨요.',
        ctaLabel: '다음',
        ctaVariant: 'ink',
        mascot: mascotJudging,
        mascotAlt: '탕이 판결',
    },
    {
        key: 'lives',
        overline: 'LIVES · 3 / 5',
        title: '목숨을 지켜 살아남아요',
        description: '유죄면 목숨이 줄어요. 끝까지 남은 사람이 승리! 홈의 ? 아이콘으로 다시 볼 수 있어요.',
        ctaLabel: '재판 시작하기',
        ctaVariant: 'gold',
        mascot: mascotExcited,
        mascotAlt: '탕이 응원',
    },
];

const currentStep = computed(() => steps[step.value]);

function next() {
    if (step.value < TOTAL_STEPS - 1) {
        step.value++;
    } else {
        complete();
    }
}

function skip() {
    complete();
}

function complete() {
    step.value = 0;
    emit('update:modelValue', false);
    emit('complete');
}

/* 스크롤 잠금 */
watch(
    () => props.modelValue,
    (show) => {
        document.body.style.overflow = show ? 'hidden' : '';
    },
);

onUnmounted(() => {
    document.body.style.overflow = '';
});
</script>

<template>
    <Teleport to="body">
        <Transition name="tutorial-fade">
            <div v-if="modelValue" class="tutorial-overlay">
                <div class="tutorial-dim" />
                <div class="tutorial-content">
                    <div class="tutorial-card">
                        <Transition name="step-fade" mode="out-in">
                            <div :key="step">
                                <!-- ===== 헤더 ===== -->
                                <div
                                    class="card-header"
                                    :class="`card-header--${currentStep.key}`"
                                >
                                    <div class="header-top">
                                        <span class="header-overline">
                                            {{ currentStep.overline }}
                                        </span>
                                        <span class="header-step">
                                            {{ step + 1 }} / {{ TOTAL_STEPS }}
                                        </span>
                                    </div>

                                    <!-- Step 1: 규칙 칩 -->
                                    <div v-if="step === 0" class="header-illustration">
                                        <div class="rule-chips">
                                            <span class="rule-chip">
                                                <svg width="13" height="13" viewBox="0 0 14 14" aria-hidden="true">
                                                    <rect x="1" y="2" width="12" height="2" rx="1" fill="var(--tt-accent)" />
                                                    <rect x="1" y="6" width="12" height="2" rx="1" fill="var(--tt-accent)" />
                                                    <rect x="1" y="10" width="8" height="2" rx="1" fill="var(--tt-accent)" />
                                                </svg>
                                                기간 최대 7일
                                            </span>
                                            <div class="rule-chips-row">
                                                <span class="rule-chip">
                                                    <svg width="13" height="13" viewBox="0 0 14 14" aria-hidden="true">
                                                        <rect x="1" y="2" width="12" height="2" rx="1" fill="var(--tt-accent)" />
                                                        <rect x="1" y="6" width="12" height="2" rx="1" fill="var(--tt-accent)" />
                                                        <rect x="1" y="10" width="8" height="2" rx="1" fill="var(--tt-accent)" />
                                                    </svg>
                                                    카테고리
                                                </span>
                                                <span class="rule-chip">
                                                    <svg width="13" height="13" viewBox="0 0 14 14" aria-hidden="true">
                                                        <rect x="1" y="2" width="12" height="2" rx="1" fill="var(--tt-accent)" />
                                                        <rect x="1" y="6" width="12" height="2" rx="1" fill="var(--tt-accent)" />
                                                        <rect x="1" y="10" width="8" height="2" rx="1" fill="var(--tt-accent)" />
                                                    </svg>
                                                    제한 금액
                                                </span>
                                            </div>
                                        </div>
                                    </div>

                                    <!-- Step 2: 투표 데모 -->
                                    <div v-else-if="step === 1" class="header-illustration">
                                        <div class="vote-demo">
                                            <div class="verdict-buttons">
                                                <span class="verdict-btn verdict-btn--innocent">무죄</span>
                                                <span class="verdict-btn verdict-btn--guilty">유죄</span>
                                            </div>
                                            <div class="vote-bar">
                                                <div class="vote-bar-guilty" />
                                                <div class="vote-bar-innocent" />
                                            </div>
                                            <div class="vote-labels">
                                                <span class="vote-label vote-label--guilty">유죄 2</span>
                                                <span class="vote-label vote-label--anon">익명 투표</span>
                                                <span class="vote-label vote-label--innocent">무죄 2</span>
                                            </div>
                                        </div>
                                    </div>

                                    <!-- Step 3: 목숨 토큰 -->
                                    <div v-else class="header-illustration header-illustration--lives">
                                        <div class="lives-demo">
                                            <img
                                                v-for="i in 3"
                                                :key="`alive-${i}`"
                                                :src="lifeAlive"
                                                alt="남은 목숨"
                                                class="life-token"
                                            >
                                            <img
                                                v-for="i in 2"
                                                :key="`depleted-${i}`"
                                                :src="lifeDepleted"
                                                alt="차감된 목숨"
                                                class="life-token"
                                            >
                                        </div>
                                    </div>

                                    <!-- 마스코트 -->
                                    <div class="mascot-wrapper">
                                        <div class="mascot-glow" />
                                        <img
                                            :src="currentStep.mascot"
                                            :alt="currentStep.mascotAlt"
                                            class="mascot-img"
                                        >
                                    </div>
                                </div>

                                <!-- ===== 바디 ===== -->
                                <div class="card-body">
                                    <h3 class="card-title">{{ currentStep.title }}</h3>
                                    <p class="card-desc">{{ currentStep.description }}</p>
                                    <div class="card-actions">
                                        <button
                                            type="button"
                                            class="btn-skip"
                                            @click="skip"
                                        >
                                            건너뛰기
                                        </button>
                                        <button
                                            type="button"
                                            class="btn-cta"
                                            :class="`btn-cta--${currentStep.ctaVariant}`"
                                            @click="next"
                                        >
                                            {{ currentStep.ctaLabel }}
                                        </button>
                                    </div>
                                </div>
                            </div>
                        </Transition>
                    </div>

                    <!-- ===== 스텝 인디케이터 ===== -->
                    <div class="step-dots">
                        <span
                            v-for="i in TOTAL_STEPS"
                            :key="i"
                            class="dot"
                            :class="{ 'dot--active': step === i - 1 }"
                        />
                    </div>
                </div>
            </div>
        </Transition>
    </Teleport>
</template>

<style scoped>
/* ── 튜토리얼 전용 장식 색상 ──────────── */
.tutorial-card {
    --_vote-guilty-light: #FF9E86;
    --_vote-innocent-light: #7FD4A8;
}

/* ── 오버레이 레이아웃 ────────────────── */
.tutorial-overlay {
    position: fixed;
    inset: 0;
    z-index: var(--tt-z-modal);
    display: flex;
    align-items: center;
    justify-content: center;
}

.tutorial-dim {
    position: absolute;
    inset: 0;
    background: var(--tt-overlay-dim);
}

.tutorial-content {
    position: relative;
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 0 var(--tt-screen-padding);
    width: 100%;
    max-width: var(--tt-content-max);
}

/* ── 카드 ─────────────────────────────── */
.tutorial-card {
    width: 100%;
    background: var(--tt-bg);
    border-radius: var(--tt-radius-xl);
    box-shadow: 0 24px 60px rgba(35, 40, 66, 0.4);
    overflow: hidden;
}

/* ── 카드 헤더 ────────────────────────── */
.card-header {
    position: relative;
    padding: var(--tt-space-4) 18px var(--tt-space-5);
    overflow: hidden;
}

.card-header--rules {
    background: var(--tt-bg-fill);
}

.card-header--trial {
    background: var(--tt-surface-inverse);
}

.card-header--lives {
    background: var(--tt-success-subtle);
}

.header-top {
    display: flex;
    align-items: center;
    justify-content: space-between;
}

.header-overline {
    font-size: var(--tt-fs-overline);
    font-weight: var(--tt-fw-black);
    letter-spacing: 0.18em;
}

.header-step {
    font-size: var(--tt-fs-badge);
    font-weight: var(--tt-fw-black);
}

/* 헤더별 오버라인·스텝 색상 */
.card-header--rules .header-overline { color: var(--tt-text-hint); }
.card-header--rules .header-step { color: var(--tt-text-muted); }

.card-header--trial .header-overline { color: var(--tt-accent); }
.card-header--trial .header-step { color: var(--tt-tab-inactive); }

.card-header--lives .header-overline { color: var(--tt-success); }
.card-header--lives .header-step { color: var(--tt-text-muted); }

/* ── 일러스트: 규칙 칩 (Step 1) ────────── */
.header-illustration {
    margin-top: var(--tt-space-3);
    max-width: 196px;
}

.header-illustration--lives {
    max-width: none;
}

.rule-chips {
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-2);
    align-items: flex-start;
}

.rule-chips-row {
    display: flex;
    gap: var(--tt-space-2);
}

.rule-chip {
    display: inline-flex;
    align-items: center;
    gap: 7px;
    white-space: nowrap;
    background: var(--tt-bg);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-full);
    padding: 6px var(--tt-space-3);
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-black);
    box-shadow: 0 2px 6px rgba(35, 40, 66, 0.04);
}

/* ── 일러스트: 투표 데모 (Step 2) ──────── */
.vote-demo {
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-3);
    max-width: 184px;
}

.verdict-buttons {
    display: flex;
    gap: 9px;
}

.verdict-btn {
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-black);
    padding: 7px var(--tt-space-4);
    border-radius: var(--tt-radius-full);
    background: var(--tt-bg);
}

.verdict-btn--innocent {
    color: var(--tt-success);
    border: 1.5px solid var(--tt-success);
}

.verdict-btn--guilty {
    color: var(--tt-danger);
    border: 1.5px solid var(--tt-danger);
}

.vote-bar {
    height: 8px;
    border-radius: var(--tt-radius-full);
    overflow: hidden;
    display: flex;
}

.vote-bar-guilty {
    width: 50%;
    background: var(--tt-danger);
}

.vote-bar-innocent {
    width: 50%;
    background: var(--tt-success);
}

.vote-labels {
    display: flex;
    align-items: center;
    justify-content: space-between;
}

.vote-label {
    font-size: var(--tt-fs-overline);
    font-weight: var(--tt-fw-black);
}

.vote-label--guilty { color: var(--_vote-guilty-light); }
.vote-label--innocent { color: var(--_vote-innocent-light); }
.vote-label--anon {
    font-size: 10px;
    font-weight: var(--tt-fw-bold);
    color: var(--tt-tab-inactive);
    letter-spacing: 0.04em;
}

/* ── 일러스트: 목숨 토큰 (Step 3) ──────── */
.lives-demo {
    display: flex;
    gap: 6px;
    align-items: center;
    margin-top: var(--tt-space-4);
}

.life-token {
    width: 26px;
    height: 26px;
    object-fit: contain;
}

/* ── 마스코트 ─────────────────────────── */
.mascot-wrapper {
    position: absolute;
    right: 6px;
    bottom: -6px;
    width: 96px;
    height: 96px;
    display: flex;
    align-items: flex-end;
    justify-content: center;
}

.mascot-glow {
    position: absolute;
    inset: 10px;
    border-radius: 50%;
}

.card-header--rules .mascot-glow,
.card-header--trial .mascot-glow {
    background: radial-gradient(
        circle,
        rgba(245, 185, 33, 0.2) 0%,
        rgba(245, 185, 33, 0) 72%
    );
}

.card-header--lives .mascot-glow {
    background: radial-gradient(
        circle,
        rgba(46, 158, 107, 0.18) 0%,
        rgba(46, 158, 107, 0) 72%
    );
}

.mascot-img {
    position: relative;
    width: 82px;
    height: 82px;
    object-fit: contain;
    filter: drop-shadow(0 6px 12px rgba(35, 40, 66, 0.18));
}

.card-header--trial .mascot-img {
    filter: drop-shadow(0 8px 14px rgba(0, 0, 0, 0.3));
}

/* ── 카드 바디 ────────────────────────── */
.card-body {
    padding: 18px var(--tt-space-5);
}

.card-title {
    font-size: var(--tt-fs-subtitle);
    font-weight: var(--tt-fw-black);
    letter-spacing: -0.01em;
    color: var(--tt-text);
    line-height: var(--tt-lh-snug);
}

.card-desc {
    font-size: var(--tt-fs-body);
    color: var(--tt-text-muted);
    margin-top: var(--tt-space-2);
    line-height: var(--tt-lh-normal);
}

.card-actions {
    margin-top: var(--tt-space-4);
    display: flex;
    align-items: center;
    justify-content: space-between;
}

/* ── 버튼 ─────────────────────────────── */
.btn-skip {
    background: none;
    border: none;
    cursor: pointer;
    font-size: var(--tt-fs-button);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-muted);
    padding: 0;
}

.btn-skip:active {
    opacity: 0.6;
}

.btn-cta {
    border: none;
    cursor: pointer;
    font-weight: var(--tt-fw-black);
    font-size: var(--tt-fs-button);
    padding: var(--tt-space-3) var(--tt-space-6);
    border-radius: var(--tt-radius-md);
    transition: opacity 0.15s ease;
}

.btn-cta:active {
    opacity: 0.8;
}

.btn-cta--ink {
    background: var(--tt-primary);
    color: var(--tt-text-inverse);
}

.btn-cta--gold {
    background: var(--tt-primary-gold);
    color: var(--tt-primary);
}

/* ── 스텝 인디케이터 ──────────────────── */
.step-dots {
    margin-top: 18px;
    display: flex;
    align-items: center;
    gap: 7px;
}

.dot {
    width: 7px;
    height: 7px;
    border-radius: var(--tt-radius-full);
    background: rgba(255, 255, 255, 0.55);
    transition: width 0.3s ease, background 0.3s ease;
}

.dot--active {
    width: 20px;
    background: var(--tt-accent);
}

/* ── 트랜지션: 오버레이 등장/퇴장 ────── */
.tutorial-fade-enter-active,
.tutorial-fade-leave-active {
    transition: opacity 0.3s ease;
}

.tutorial-fade-enter-from,
.tutorial-fade-leave-to {
    opacity: 0;
}

/* ── 트랜지션: 스텝 전환 ──────────────── */
.step-fade-enter-active,
.step-fade-leave-active {
    transition: opacity 0.2s ease;
}

.step-fade-enter-from,
.step-fade-leave-to {
    opacity: 0;
}
</style>
