<!--
  00 · 재판 진행 현황 — 기소 후 판결 전까지의 4단계 타임라인.
  상태별 분기: INDICTED / DEFENSE_SUBMITTED / VOTING / VERDICT_DONE.
  /group-challenges/:id/trial/:indictmentId
-->
<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import BaseBackHeader from '@/components/common/BaseBackHeader.vue';
import TrialProgressTimeline from '@/components/challenge/group/TrialProgressTimeline.vue';
import thinkingImg from '@/assets/images/emotions/47_thinking.png';
import verdictImg from '@/assets/images/emotions/49_verdict.png';
import { MOCK_TRIAL_PROGRESS } from '@/fixtures/groupChallengeDetail';

const route = useRoute();
const router = useRouter();

const trial = ref(null);

onMounted(() => {
    const id = Number(route.params.indictmentId);
    trial.value = MOCK_TRIAL_PROGRESS[id] ?? null;
    if (!trial.value) {
        router.replace({ name: 'groupChallenge' });
    }
});

const isDone = computed(() => trial.value?.status === 'VERDICT_DONE');
const isVoting = computed(() => trial.value?.status === 'VOTING');
const isDefenseSubmitted = computed(() => trial.value?.status === 'DEFENSE_SUBMITTED');

const heroImage = computed(() => isDone.value ? verdictImg : thinkingImg);
const heroAlt = computed(() => isDone.value ? '탕이 판결 완료' : '탕이 판결 대기');

const heroTitle = computed(() => {
    if (isDone.value) return '최종 판결이\n확정됐어요';
    if (isVoting.value) return '배심원 투표가\n진행 중이에요';
    if (isDefenseSubmitted.value) return '변론이\n제출됐어요';
    return '기소가\n접수됐어요';
});

const heroSub = computed(() => {
    if (isDone.value) return '아래에서 최종 판결을 확인하세요.';
    if (isVoting.value) return `판결은 오늘 ${trial.value?.steps.voteDeadline}에 확정돼요.`;
    if (isDefenseSubmitted.value) return '투표가 시작되면 알려드릴게요.';
    return '변론을 작성해주세요.';
});

const statusBadge = computed(() => {
    if (isDone.value) return { label: '판결 완료', cls: 'badge--done' };
    if (isVoting.value) return { label: '투표 진행 중', cls: 'badge--voting' };
    if (isDefenseSubmitted.value) return { label: '변론 완료', cls: 'badge--defense' };
    return { label: '기소 접수', cls: 'badge--indicted' };
});

const decoClass = computed(() => {
    if (isDone.value) return 'trial-progress__deco--done';
    return 'trial-progress__deco--pending';
});

const showVoteStats = computed(() =>
    isVoting.value && trial.value?.vote,
);

function goVerdict() {
    router.push({
        name: 'verdictResult',
        params: route.params,
    });
}

function goBack() {
    router.push({
        name: 'groupChallengeDetail',
        params: { id: route.params.id },
    });
}
</script>

<template>
    <div v-if="trial" class="trial-progress">
        <!-- 헤더 -->
        <div class="trial-progress__header">
            <div class="trial-progress__deco" :class="decoClass"></div>
            <BaseBackHeader title="재판 진행 현황" @back="goBack" />
            <div class="trial-progress__badges">
                <span class="trial-progress__badge badge--eval">{{ trial.evalLabel }}</span>
                <span class="trial-progress__badge" :class="statusBadge.cls">{{ statusBadge.label }}</span>
            </div>
        </div>

        <!-- 본문 -->
        <div class="trial-progress__body">
            <!-- 히어로 카드 -->
            <div class="trial-progress__hero">
                <div class="trial-progress__hero-mascot-wrap">
                    <div class="trial-progress__hero-glow"></div>
                    <img :src="heroImage" :alt="heroAlt" class="trial-progress__hero-mascot">
                </div>
                <div class="trial-progress__hero-text">
                    <div class="trial-progress__hero-title">{{ heroTitle }}</div>
                    <div class="trial-progress__hero-sub">{{ heroSub }}</div>
                </div>
            </div>

            <!-- 타임라인 -->
            <TrialProgressTimeline
                :status="trial.status"
                :case-number="trial.caseNumber"
                :steps="trial.steps"
                :vote="trial.vote"
            />

            <!-- 투표 참여 현황 (VOTING 일 때만) -->
            <div v-if="showVoteStats" class="trial-progress__vote-stats">
                <div class="trial-progress__vote-label">투표 참여</div>
                <div class="trial-progress__vote-row">
                    <span class="trial-progress__vote-count">
                        {{ trial.vote.votedCount }}<span class="trial-progress__vote-total"> / {{ trial.vote.totalVoters }}명</span>
                    </span>
                    <span class="trial-progress__vote-remain">{{ trial.vote.remainingVoters }}명 남음</span>
                </div>
                <div class="trial-progress__vote-track">
                    <div
                        class="trial-progress__vote-fill"
                        :style="{ width: (trial.vote.votedCount / trial.vote.totalVoters * 100) + '%' }"
                    ></div>
                </div>
            </div>
        </div>

        <!-- 하단 버튼 -->
        <div class="trial-progress__footer">
            <button
                v-if="isDone"
                type="button"
                class="trial-progress__btn trial-progress__btn--primary"
                @click="goVerdict"
            >
                최종 판결 보러가기
            </button>
            <button
                v-else
                type="button"
                class="trial-progress__btn trial-progress__btn--primary"
                @click="goBack"
            >
                그룹 화면으로 돌아가기
            </button>
        </div>
    </div>
</template>

<style scoped src="./TrialProgressView.css"></style>
