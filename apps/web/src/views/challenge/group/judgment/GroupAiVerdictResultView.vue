<script setup>
import { onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import BaseButton from '@/components/common/BaseButton.vue';
import DefenseCourtHeader from '@/components/challenge/group/DefenseCourtHeader.vue';
import GroupAiVerdictResultCard from '@/components/challenge/group/GroupAiVerdictResultCard.vue';
import GroupTieScoreCard from '@/components/challenge/group/GroupTieScoreCard.vue';
import { fetchTrialDetail } from '@/api/groupChallenge';
import { isAiVerdict, toVerdictScreen, verdictRouteName } from '@/utils/groupTrial';

const route = useRoute();
const router = useRouter();
const verdict = ref(null);

/* 판결은 서버에서 다시 읽는다. 앞 화면이 쿼리로 넘겨주면 URL 조작으로 결과가 바뀐다(이슈 #172). */
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

/*
 * 판결 플로우 위에 상세가 다시 쌓이지 않도록 `replace` 로 돌아간다(이슈 #172).
 * 헤더 X 와 하단 「그룹 화면으로 돌아가기」가 같은 곳으로 간다 — 이 화면은 재판의 종착점이라
 * 뒤로 갈 곳이 판사봉 애니메이션(`groupAiVerdict`)뿐이다.
 */
function goGroupHome() {
    router.replace({ name: 'groupChallengeDetail', params: { id: route.params.id } });
}

function goDetail() {
    router.push({ name: 'groupAiVerdictDetail', params: route.params });
}
</script>

<template>
    <main v-if="verdict" class="result-page">
        <DefenseCourtHeader nav-action="close" @close="goGroupHome">
            <template #nav-right
                ><span class="result-page__case">{{ verdict.caseNumber }}</span></template
            >
            <h1 class="result-page__title">최종 판결</h1>
            <p class="result-page__subtitle">
                {{ verdict.challengeName }} · {{ verdict.evaluationLabel }}
            </p>
        </DefenseCourtHeader>

        <section class="result-page__body">
            <GroupAiVerdictResultCard :verdict="verdict" />
            <GroupTieScoreCard
                :guilty-votes="verdict.guiltyVotes"
                :innocent-votes="verdict.innocentVotes"
                :outcome="verdict.outcome"
                class="result-page__score"
            />
        </section>

        <footer class="result-page__footer">
            <BaseButton variant="dark" size="lg" block @click="goGroupHome"
                >그룹 화면으로 돌아가기</BaseButton
            >
            <button type="button" class="result-page__detail" @click="goDetail">
                세부 정보 보기
            </button>
        </footer>
    </main>
</template>

<style scoped>
.result-page {
    min-height: 100dvh;
    display: flex;
    flex-direction: column;
    background: var(--tt-bg-subtle);
}
.result-page__case {
    color: var(--tt-gray-400);
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-mono-chip);
}
.result-page__title {
    margin-top: var(--tt-space-2);
    color: var(--tt-text-inverse);
    font-size: var(--tt-fs-title);
    font-weight: var(--tt-fw-black);
    letter-spacing: -0.01em;
}
.result-page__subtitle {
    margin-top: var(--tt-space-2);
    color: var(--tt-gray-400);
    font-size: var(--tt-fs-caption);
}
.result-page__body {
    display: flex;
    flex: 1;
    flex-direction: column;
    gap: var(--tt-space-4);
    padding: var(--tt-space-4) var(--tt-space-5);
}
.result-page__score {
    margin-top: auto;
}
.result-page__footer {
    padding: var(--tt-space-3) var(--tt-space-5) var(--tt-space-6);
}
.result-page__detail {
    display: block;
    width: 100%;
    margin-top: var(--tt-space-3);
    padding: var(--tt-space-2);
    color: var(--tt-primary);
    background: transparent;
    border: 0;
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-bold);
    cursor: pointer;
}
</style>
