<!--
  탕이 판결 세부 정보 — 판결 사유 원문 + 결산 금액.
  /group-challenges/:id/vote/:indictmentId/ai-verdict/detail
-->
<script setup>
import { onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import BaseButton from '@/components/common/BaseButton.vue';
import BaseCard from '@/components/common/BaseCard.vue';
import DefenseCourtHeader from '@/components/challenge/group/DefenseCourtHeader.vue';
import { fetchTrialDetail } from '@/api/groupChallenge';
import { isAiVerdict, toVerdictScreen, verdictRouteName } from '@/utils/groupTrial';

const route = useRoute();
const router = useRouter();
const verdict = ref(null);

onMounted(async () => {
    let loaded;
    try {
        loaded = await fetchTrialDetail(route.params.indictmentId);
    } catch {
        router.replace({ name: 'groupChallenge' });
        return;
    }

    if (!isAiVerdict(loaded)) {
        router.replace({
            name: verdictRouteName(loaded) ?? 'trialProgress',
            params: route.params,
        });
        return;
    }
    verdict.value = toVerdictScreen(loaded);
});

/* 「없음」과 「0원」은 다르다. 무변론 확정 재판은 실제 부담금 자체가 없다. */
function formatWon(amount) {
    if (amount === null || amount === undefined) return '-';
    return Number(amount).toLocaleString() + '원';
}
</script>

<template>
    <main v-if="verdict" class="ai-detail">
        <DefenseCourtHeader>
            <template #nav-right
                ><span class="ai-detail__case">{{ verdict.caseNumber }}</span></template
            >
            <span class="ai-detail__tag">TANGTANG'S COURT</span>
            <h1 class="ai-detail__title">판사 탕이의<br />판결 사유</h1>
            <p class="ai-detail__subtitle">
                {{ verdict.challengeName }} · {{ verdict.evaluationLabel }}
            </p>
        </DefenseCourtHeader>

        <section class="ai-detail__body">
            <!--
              사유 원문을 그대로 띄운다. 요약하지 않는다 — 사용자가 납득할 근거는 이 문장뿐이다.
              LLM 호출이 실패한 재판에도 서버가 「무죄 추정으로 확정했다」는 문장을 채워 보낸다.
            -->
            <BaseCard padding="lg" class="ai-detail__reason">
                <span class="ai-detail__reason-label">판결 사유</span>
                <p class="ai-detail__reason-text">{{ verdict.detail.reason }}</p>
            </BaseCard>

            <BaseCard padding="lg" class="ai-detail__facts">
                <div class="ai-detail__row">
                    <span class="ai-detail__row-label">판정 기간</span>
                    <span class="ai-detail__row-value">{{ verdict.detail.judgmentDate }}</span>
                </div>
                <div class="ai-detail__row">
                    <span class="ai-detail__row-label">소비 금액</span>
                    <span class="ai-detail__row-value">{{
                        formatWon(verdict.detail.currentAmount)
                    }}</span>
                </div>
                <div class="ai-detail__row">
                    <span class="ai-detail__row-label">소비 기준</span>
                    <span class="ai-detail__row-value">{{
                        formatWon(verdict.detail.limitAmount)
                    }}</span>
                </div>
                <div class="ai-detail__row">
                    <span class="ai-detail__row-label">실제 부담금</span>
                    <span class="ai-detail__row-value">{{
                        formatWon(verdict.detail.actualCostAmount)
                    }}</span>
                </div>
                <div class="ai-detail__row">
                    <span class="ai-detail__row-label">최종 투표</span>
                    <span class="ai-detail__row-value"
                        >{{ verdict.guiltyVotes }} : {{ verdict.innocentVotes }}</span
                    >
                </div>
            </BaseCard>
        </section>

        <footer class="ai-detail__footer">
            <BaseButton variant="dark" size="lg" block @click="router.back()"
                >판결 결과로 돌아가기</BaseButton
            >
        </footer>
    </main>
</template>

<style scoped>
.ai-detail {
    min-height: 100dvh;
    display: flex;
    flex-direction: column;
    background: var(--tt-bg-subtle);
}
.ai-detail__case {
    color: var(--tt-gray-400);
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-mono-chip);
}
.ai-detail__tag {
    display: inline-flex;
    padding: 5px var(--tt-space-3);
    color: var(--tt-accent-700);
    background: var(--tt-accent-subtle);
    border-radius: var(--tt-radius-full);
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
}
.ai-detail__title {
    margin-top: var(--tt-space-3);
    color: var(--tt-text-inverse);
    font-size: var(--tt-fs-title);
    font-weight: var(--tt-fw-black);
    line-height: var(--tt-lh-tight);
    letter-spacing: -0.01em;
}
.ai-detail__subtitle {
    margin-top: var(--tt-space-2);
    color: var(--tt-gray-400);
    font-size: var(--tt-fs-caption);
}
.ai-detail__body {
    display: flex;
    flex: 1;
    flex-direction: column;
    gap: var(--tt-space-4);
    padding: var(--tt-space-4) var(--tt-space-5);
}
.ai-detail__reason-label {
    color: var(--tt-accent-700);
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
}
.ai-detail__reason-text {
    margin-top: var(--tt-space-2);
    color: var(--tt-text);
    font-size: var(--tt-fs-body);
    line-height: var(--tt-lh-normal);
    white-space: pre-wrap;
}
.ai-detail__row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: var(--tt-space-2) 0;
}
.ai-detail__row + .ai-detail__row {
    border-top: 1px solid var(--tt-border);
}
.ai-detail__row-label {
    color: var(--tt-text-muted);
    font-size: var(--tt-fs-caption);
}
.ai-detail__row-value {
    color: var(--tt-text);
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-bold);
}
.ai-detail__footer {
    padding: var(--tt-space-3) var(--tt-space-5) var(--tt-space-6);
}
</style>
