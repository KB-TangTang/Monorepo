<!--
  01/02 · 최종 판결 — 무죄/유죄 판결문 카드 + 투표 결과.
  outcome 에 따라 색상·표정·도장만 교체되는 통합 화면.
  /group-challenges/:id/trial/:indictmentId/verdict
-->
<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import BaseBackHeader from '@/components/common/BaseBackHeader.vue';
import VerdictDocumentCard from '@/components/challenge/group/VerdictDocumentCard.vue';
import VoteResultBar from '@/components/challenge/group/VoteResultBar.vue';
import { fetchTrialDetail } from '@/api/groupChallenge';
import { toVerdictScreen } from '@/utils/groupTrial';

const route = useRoute();
const router = useRouter();

const verdict = ref(null);

/*
 * 확정된 재판(`GUILTY`/`INNOCENT`)만 이 화면에 들어온다(이슈 #172).
 * 아직 투표 중이면 진행 현황이 4개 상태를 모두 그려준다 — 판결문을 빈 채로 띄우지 않는다.
 */
onMounted(async () => {
    let loaded;
    try {
        loaded = await fetchTrialDetail(route.params.indictmentId);
    } catch {
        /* 참여자가 아니거나 없는 재판이다. 서버가 존재 자체를 알려주지 않는다. */
        router.replace({ name: 'groupChallenge' });
        return;
    }

    const screen = toVerdictScreen(loaded);
    if (!screen) {
        router.replace({ name: 'trialProgress', params: route.params });
        return;
    }
    verdict.value = screen;
});

const isInnocent = computed(() => verdict.value?.outcome === 'INNOCENT');

const decoClass = computed(() =>
    isInnocent.value ? 'verdict-result__deco--innocent' : 'verdict-result__deco--guilty',
);

const statusBadge = computed(() =>
    isInnocent.value
        ? { label: '무죄 판결', cls: 'badge--innocent' }
        : { label: '유죄 판결', cls: 'badge--guilty' },
);

function goDetail() {
    router.push({
        name: 'verdictDetail',
        params: route.params,
    });
}

/*
 * 헤더 뒤로가기와 하단 CTA 는 역할이 다르다(이슈 #172).
 *
 * 이 화면으로 오는 push 진입로는 `TrialProgressView.goVerdict:92` 하나뿐이라
 * (나머지 `verdictRouteName` 사용처는 잘못 들어온 화면을 고치는 `replace` 다)
 * 히스토리는 항상 `상세 → 재판 진행 현황 → 최종 판결` 이다. 화살표는 한 단계만 되돌린다.
 */
function goBack() {
    router.back();
}

/*
 * 하단 CTA 는 플로우 탈출이라 목적지를 명시한다. `push` 면 판결 위에 상세가 다시 얹혀
 * 상세에서 뒤로가기를 눌렀을 때 방금 본 판결문이 나온다.
 */
function goGroupHome() {
    router.replace({
        name: 'groupChallengeDetail',
        params: { id: route.params.id },
    });
}
</script>

<template>
    <div v-if="verdict" class="verdict-result">
        <!-- 헤더 -->
        <div class="verdict-result__header">
            <div class="verdict-result__deco" :class="decoClass"></div>
            <BaseBackHeader title="최종 판결" @back="goBack" />
            <div class="verdict-result__badges">
                <span class="verdict-result__badge badge--eval">{{ verdict.evalLabel }}</span>
                <span class="verdict-result__badge" :class="statusBadge.cls">{{
                    statusBadge.label
                }}</span>
            </div>
        </div>

        <!-- 본문 -->
        <div class="verdict-result__body">
            <VerdictDocumentCard :outcome="verdict.outcome" />
            <VoteResultBar
                :outcome="verdict.outcome"
                :innocent-votes="verdict.innocentVotes"
                :guilty-votes="verdict.guiltyVotes"
            />
        </div>

        <!-- 하단 버튼 -->
        <div class="verdict-result__footer">
            <button
                type="button"
                class="verdict-result__btn verdict-result__btn--outline"
                @click="goDetail"
            >
                <span>판결 세부 정보 보기</span>
                <svg width="7" height="12" viewBox="0 0 7 12" fill="none">
                    <path
                        d="M1 1l5 5-5 5"
                        stroke="currentColor"
                        stroke-width="2"
                        stroke-linecap="round"
                        stroke-linejoin="round"
                    />
                </svg>
            </button>
            <button
                type="button"
                class="verdict-result__btn verdict-result__btn--primary"
                @click="goGroupHome"
            >
                그룹 화면으로 돌아가기
            </button>
        </div>
    </div>
</template>

<style scoped src="./VerdictResultView.css"></style>
