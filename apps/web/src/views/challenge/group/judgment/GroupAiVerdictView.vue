<script setup>
import { onMounted, onUnmounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import BaseButton from '@/components/common/BaseButton.vue';
import DefenseCourtHeader from '@/components/challenge/group/DefenseCourtHeader.vue';
import GroupAiVerdictAnimation from '@/components/challenge/group/GroupAiVerdictAnimation.vue';
import { getAiVerdict, toggleDevelopmentAiVerdict } from '@/fixtures/groupChallengeAiVerdict';

const route = useRoute();
const router = useRouter();
const verdict = ref(getAiVerdict());
const stage = ref('intro');
const isReady = ref(false);
const isDevelopment = import.meta.env.DEV;
const timers = [];

function schedule(callback, delay) {
    timers.push(window.setTimeout(callback, delay));
}

function runVerdictAnimation() {
    stage.value = 'intro';
    isReady.value = false;
    schedule(() => {
        stage.value = 'review';
    }, 250);
    schedule(() => {
        stage.value = 'raised';
    }, 1500);
    schedule(() => {
        stage.value = 'strike';
    }, 2000);
    schedule(() => {
        stage.value = 'done';
        isReady.value = true;
    }, 2250);
}

function openResult() {
    if (!isReady.value) return;
    router.push({ name: 'groupAiVerdictResult', params: route.params });
}

function toggleDevelopmentVerdict() {
    verdict.value = toggleDevelopmentAiVerdict();
    timers.forEach((timer) => window.clearTimeout(timer));
    timers.length = 0;
    runVerdictAnimation();
}

onMounted(runVerdictAnimation);
onUnmounted(() => timers.forEach((timer) => window.clearTimeout(timer)));
</script>

<template>
    <main class="ai-page">
        <DefenseCourtHeader>
            <template #nav-right>
                <div class="ai-page__nav-actions">
                    <span class="ai-page__case">{{ verdict.caseNumber }}</span>
                    <button
                        v-if="isDevelopment"
                        type="button"
                        class="ai-page__development-button"
                        :class="`ai-page__development-button--${verdict.outcome}`"
                        @click="toggleDevelopmentVerdict"
                    >
                        목업 · {{ verdict.label }}
                    </button>
                </div>
            </template>
            <span class="ai-page__tag">TANGTANG'S COURT</span>
            <h1 class="ai-page__title">잠시만요,<br />기록을 살펴볼게요</h1>
        </DefenseCourtHeader>

        <section class="ai-page__body">
            <GroupAiVerdictAnimation :stage="stage" :verdict="verdict" />
        </section>

        <footer class="ai-page__footer">
            <BaseButton variant="dark" size="lg" block :disabled="!isReady" @click="openResult"
                >판결 확인하기</BaseButton
            >
        </footer>
    </main>
</template>

<style scoped>
.ai-page {
    min-height: 100dvh;
    display: flex;
    flex-direction: column;
    background: var(--tt-bg-subtle);
}
.ai-page__case {
    color: var(--tt-gray-400);
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-mono-chip);
}
.ai-page__tag {
    display: inline-flex;
    padding: 5px var(--tt-space-3);
    color: var(--tt-accent-700);
    background: var(--tt-accent-subtle);
    border-radius: var(--tt-radius-full);
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
}
.ai-page__title {
    margin-top: var(--tt-space-3);
    color: var(--tt-text-inverse);
    font-size: var(--tt-fs-title);
    font-weight: var(--tt-fw-black);
    line-height: var(--tt-lh-tight);
    letter-spacing: -0.01em;
}
.ai-page__body {
    display: flex;
    flex: 1;
    flex-direction: column;
    padding: var(--tt-space-4) var(--tt-space-5);
}
.ai-page__development-button {
    position: absolute;
    top: 30px;
    right: 0;
    z-index: 3;
    white-space: nowrap;
    padding: 5px var(--tt-space-2);
    border: 1px solid currentColor;
    border-radius: var(--tt-radius-full);
    font-size: var(--tt-fs-mono-chip);
    font-weight: var(--tt-fw-bold);
    cursor: pointer;
}
.ai-page__nav-actions {
    position: relative;
}
.ai-page__development-button--GUILTY {
    color: var(--tt-danger);
    background: var(--tt-danger-subtle);
}
.ai-page__development-button--INNOCENT {
    color: var(--tt-success);
    background: var(--tt-success-subtle);
}
.ai-page__footer {
    padding: var(--tt-space-3) var(--tt-space-5) var(--tt-space-6);
}
</style>
