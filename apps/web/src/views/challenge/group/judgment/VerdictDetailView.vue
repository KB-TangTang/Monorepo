<!--
  03/04 · 판결 상세 — 투표 결과 배너 + 금액 테이블 + 판결 사유.
  outcome 에 따라 색상만 교체되는 통합 화면.
  /group-challenges/:id/trial/:indictmentId/verdict/detail
-->
<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import BaseBackHeader from '@/components/common/BaseBackHeader.vue';
import TrialCommentList from '@/components/challenge/group/TrialCommentList.vue';
import { fetchTrialDetail } from '@/api/groupChallenge';
import { toVerdictScreen } from '@/utils/groupTrial';

const route = useRoute();
const router = useRouter();

const verdict = ref(null);

/* 진입 조건은 최종 판결 화면과 같다 — 확정된 재판만 본다(이슈 #172). */
onMounted(async () => {
    let loaded;
    try {
        loaded = await fetchTrialDetail(route.params.indictmentId);
    } catch {
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
    isInnocent.value ? 'verdict-detail__deco--innocent' : 'verdict-detail__deco--guilty',
);

const statusBadge = computed(() =>
    isInnocent.value
        ? { label: '무죄', cls: 'badge--innocent' }
        : { label: '유죄', cls: 'badge--guilty' },
);

const scoreText = computed(() => {
    if (!verdict.value) return '';
    const label = isInnocent.value ? '무죄' : '유죄';
    return `${label} · ${verdict.value.innocentVotes} : ${verdict.value.guiltyVotes}`;
});

/*
 * 「없음」과 「0원」은 다르다. 변론이 없는 재판은 실제 부담금 자체가 없다 —
 * 변론 마감 배치가 변론 없이 상태를 넘기므로 무변론 확정이 실제로 존재한다.
 */
function formatWon(amount) {
    if (amount === null || amount === undefined) return '-';
    return Number(amount).toLocaleString() + '원';
}

function goBack() {
    router.back();
}

/* 판결 플로우 위에 상세가 다시 쌓이지 않도록 `replace` 로 돌아간다(이슈 #172). */
function goGroupHome() {
    router.replace({
        name: 'groupChallengeDetail',
        params: { id: route.params.id },
    });
}
</script>

<template>
    <div v-if="verdict" class="verdict-detail">
        <!-- 헤더 -->
        <div class="verdict-detail__header">
            <div class="verdict-detail__deco" :class="decoClass"></div>
            <BaseBackHeader title="판결 상세" @back="goBack" />
            <div class="verdict-detail__header-content">
                <div class="verdict-detail__badges">
                    <span class="verdict-detail__badge badge--eval">{{ verdict.evalLabel }}</span>
                    <span class="verdict-detail__badge" :class="statusBadge.cls">{{ statusBadge.label }}</span>
                </div>
                <h2 class="verdict-detail__title">판결 세부 정보</h2>
                <p class="verdict-detail__sub">소비 기준과 변론에 반영된 금액이에요.</p>
            </div>
        </div>

        <!-- 본문 -->
        <div class="verdict-detail__body">
            <!-- 투표 결과 배너 -->
            <div
                class="verdict-detail__banner"
                :class="isInnocent ? 'verdict-detail__banner--innocent' : 'verdict-detail__banner--guilty'"
            >
                <div class="verdict-detail__banner-label">투표 결과</div>
                <div class="verdict-detail__banner-score">{{ scoreText }}</div>
            </div>

            <!-- 금액 테이블 -->
            <div class="verdict-detail__table">
                <div class="verdict-detail__row">
                    <span class="verdict-detail__row-label">판정 기간</span>
                    <span class="verdict-detail__row-value">{{ verdict.detail.judgmentDate }}</span>
                </div>
                <div class="verdict-detail__divider"></div>
                <div class="verdict-detail__row">
                    <span class="verdict-detail__row-label">소비 금액</span>
                    <span class="verdict-detail__row-value">{{ formatWon(verdict.detail.currentAmount) }}</span>
                </div>
                <div class="verdict-detail__divider"></div>
                <div class="verdict-detail__row">
                    <span class="verdict-detail__row-label">초과 기준</span>
                    <span class="verdict-detail__row-value">{{ formatWon(verdict.detail.limitAmount) }}</span>
                </div>
                <div class="verdict-detail__divider verdict-detail__divider--dashed"></div>
                <div class="verdict-detail__row verdict-detail__row--highlight">
                    <span
                        class="verdict-detail__row-label"
                        :class="isInnocent ? 'verdict-detail__row-label--success' : 'verdict-detail__row-label--danger'"
                    >실제 부담금</span>
                    <span
                        class="verdict-detail__row-value verdict-detail__row-value--highlight"
                        :class="isInnocent ? 'verdict-detail__row-value--success' : 'verdict-detail__row-value--danger'"
                    >{{ formatWon(verdict.detail.actualCostAmount) }}</span>
                </div>
                <div class="verdict-detail__row-hint">오늘 개인 소비 기준</div>
            </div>

            <!-- 판결 사유 -->
            <div
                class="verdict-detail__reason"
                :class="isInnocent ? 'verdict-detail__reason--innocent' : 'verdict-detail__reason--guilty'"
            >
                <div class="verdict-detail__reason-label">판결 사유</div>
                <div class="verdict-detail__reason-text">{{ verdict.detail.reason }}</div>
            </div>

            <!-- 배심원 한줄 코멘트 (익명) -->
            <TrialCommentList :comments="verdict.comments" />
        </div>

        <!-- 하단 버튼 -->
        <div class="verdict-detail__footer">
            <button
                type="button"
                class="verdict-detail__btn"
                @click="goGroupHome"
            >
                그룹 화면으로 돌아가기
            </button>
        </div>
    </div>
</template>

<style scoped src="./VerdictDetailView.css"></style>
