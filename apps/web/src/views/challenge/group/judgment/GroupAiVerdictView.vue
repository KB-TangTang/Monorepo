<script setup>
import { onMounted, onUnmounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import BaseButton from '@/components/common/BaseButton.vue';
import DefenseCourtHeader from '@/components/challenge/group/DefenseCourtHeader.vue';
import GroupAiVerdictAnimation from '@/components/challenge/group/GroupAiVerdictAnimation.vue';
import { fetchTrialDetail } from '@/api/groupChallenge';
import { isAiVerdict, toVerdictScreen, verdictRouteName } from '@/utils/groupTrial';

const route = useRoute();
const router = useRouter();
const verdict = ref(null);
const stage = ref('intro');
const isReady = ref(false);
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

/*
 * 결과는 서버가 이미 확정한 값이다 — 다음 화면이 다시 조회한다.
 * 판결을 쿼리스트링으로 넘기지 않는다. URL 을 고쳐 남의 재판을 유죄로 바꿔 볼 수 있다.
 */
function openResult() {
    if (!isReady.value) return;
    router.push({
        name: 'groupAiVerdictResult',
        params: route.params,
    });
}

onMounted(async () => {
    let loaded;
    try {
        loaded = await fetchTrialDetail(route.params.indictmentId);
    } catch {
        router.replace({ name: 'groupChallenge' });
        return;
    }

    /* 동률 재판이 아니면 판사 탕이가 판결하지 않았다 — 애니메이션을 띄울 근거가 없다. */
    if (!isAiVerdict(loaded)) {
        router.replace({
            name: verdictRouteName(loaded) ?? 'trialProgress',
            params: route.params,
        });
        return;
    }

    verdict.value = toVerdictScreen(loaded);
    runVerdictAnimation();
});
onUnmounted(() => timers.forEach((timer) => window.clearTimeout(timer)));
</script>

<template>
    <main v-if="verdict" class="ai-page">
        <DefenseCourtHeader>
            <template #nav-right>
                <span class="ai-page__case">{{ verdict.caseNumber }}</span>
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
.ai-page :deep(.court-header__nav) {
    z-index: 3;
}
.ai-page__footer {
    padding: var(--tt-space-3) var(--tt-space-5) var(--tt-space-6);
}
</style>
