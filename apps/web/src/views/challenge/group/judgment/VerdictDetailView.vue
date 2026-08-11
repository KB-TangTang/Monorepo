<!--
  03/04 · 판결 상세 — 투표 결과 배너 + 금액 테이블 + 판결 사유.
  outcome 에 따라 색상만 교체되는 통합 화면.
  /group-challenges/:id/trial/:indictmentId/verdict/detail
-->
<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import BaseBackHeader from '@/components/common/BaseBackHeader.vue';
import { MOCK_VERDICT_RESULT } from '@/fixtures/groupChallengeDetail';

const route = useRoute();
const router = useRouter();

const verdict = ref(null);

onMounted(() => {
    const id = Number(route.params.indictmentId);
    verdict.value = MOCK_VERDICT_RESULT[id] ?? null;
    if (!verdict.value) {
        router.replace({ name: 'groupChallenge' });
    }
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

function formatWon(amount) {
    return amount.toLocaleString() + '원';
}

function goBack() {
    router.back();
}

function goGroupHome() {
    router.push({
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
