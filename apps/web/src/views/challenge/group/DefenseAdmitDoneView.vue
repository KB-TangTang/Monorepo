<!--
  05b · 혐의 인정 완료 (E 결과형) — GC_07_06
  혐의 인정 후 결과 화면. 마스코트 + 유죄 종결 배지 + 남은 목숨 + 판결 기록.
  "그룹 화면으로 돌아가기" → router.back() 으로 그룹 챌린지 상세 복귀.
-->
<script setup>
import { ref } from 'vue';
import { useRouter, useRoute } from 'vue-router';

import BaseBackHeader from '@/components/common/BaseBackHeader.vue';
import BaseButton from '@/components/common/BaseButton.vue';

import mascotApology from '@/assets/images/emotions/43_apology.png';
import gavelAlive from '@/assets/images/challenge_live/gavel-alive.png';
import gavelDepleted from '@/assets/images/challenge_live/gavel-depleted.png';

const router = useRouter();
const route = useRoute();

/* ── mock 데이터 (API 연동 전) ── */
const caseNumber = ref('2026-재판-0729');

const TOTAL_LIVES = 5;

const result = ref({
    livesRemaining: 4,
    transaction: {
        merchantName: '배달의민족',
        amount: 24000,
    },
    exceededAmount: 12400,
    verdict: '본인 인정 · 투표 없음',
});

/* ── 네비게이션 ── */
function goToGroupDetail() {
    router.back();
}


</script>

<template>
    <div class="page">
        <!-- ── 상단 헤더 ── -->
        <BaseBackHeader title="혐의 인정">
            <template #right>
                <span class="page__case-number">{{ caseNumber }}</span>
            </template>
        </BaseBackHeader>

        <!-- ── 본문 (중앙 정렬) ── -->
        <div class="page__body">
            <!-- 마스코트 + 글로우 -->
            <div class="result__mascot-wrap">
                <div class="result__glow"></div>
                <img :src="mascotApology" alt="탕이" class="result__mascot">
            </div>

            <!-- 유죄 종결 배지 -->
            <span class="result__badge">유죄 종결</span>

            <!-- 제목·부제 -->
            <h2 class="result__title">혐의를 인정했어요</h2>
            <p class="result__desc">
                투표 없이 재판이 종결됐어요.<br>목숨 1개가 차감됐습니다.
            </p>

            <!-- 남은 목숨 카드 -->
            <div class="info-card">
                <div class="info-card__header">
                    <span class="info-card__label">남은 목숨</span>
                    <span class="lives-count">
                        {{ result.livesRemaining }} / {{ TOTAL_LIVES }}
                    </span>
                </div>
                <div class="lives-row">
                    <img
                        v-for="i in TOTAL_LIVES"
                        :key="i"
                        :src="i <= result.livesRemaining ? gavelAlive : gavelDepleted"
                        :alt="i <= result.livesRemaining ? '남은 목숨' : '차감된 목숨'"
                        class="lives-row__icon"
                    >
                </div>
            </div>

            <!-- 판결 기록 카드 -->
            <div class="info-card">
                <span class="info-card__label">판결 기록</span>
                <div class="record-list">
                    <div class="record-list__row">
                        <span class="record-list__key">기소 거래</span>
                        <span class="record-list__value">
                            {{ result.transaction.merchantName }}
                            {{ result.transaction.amount.toLocaleString() }}원
                        </span>
                    </div>
                    <div class="record-list__row">
                        <span class="record-list__key">기준 초과</span>
                        <span class="record-list__value record-list__value--danger">
                            {{ result.exceededAmount.toLocaleString() }}원
                        </span>
                    </div>
                    <div class="record-list__row">
                        <span class="record-list__key">판결</span>
                        <span class="record-list__value">{{ result.verdict }}</span>
                    </div>
                </div>
            </div>
        </div>

        <!-- ── 하단 버튼 ── -->
        <div class="page__footer">
            <BaseButton variant="dark" size="lg" block @click="goToGroupDetail">
                그룹 화면으로 돌아가기
            </BaseButton>
        </div>
    </div>
</template>

<style scoped>
.page {
    min-height: 100vh;
    display: flex;
    flex-direction: column;
    background: var(--tt-bg-subtle);
}

.page__case-number {
    font-size: var(--tt-fs-overline);
    color: var(--tt-text-muted);
    font-weight: var(--tt-fw-semibold);
    font-family: var(--tt-font-mono);
}

/* ── 본문 ── */
.page__body {
    flex: 1;
    min-height: 0;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 0 var(--tt-screen-padding);
    gap: 12px;
}

/* ── 마스코트 ── */
.result__mascot-wrap {
    position: relative;
    width: 108px;
    height: 108px;
    display: flex;
    align-items: center;
    justify-content: center;
}

.result__glow {
    position: absolute;
    inset: 0;
    border-radius: 50%;
    background: radial-gradient(
        circle,
        rgba(224, 102, 75, 0.2) 0%,
        rgba(224, 102, 75, 0) 70%
    );
}

.result__mascot {
    position: relative;
    width: 86px;
    height: 86px;
    object-fit: contain;
    filter: drop-shadow(0 8px 14px rgba(35, 40, 66, 0.16));
}

/* ── 배지 ── */
.result__badge {
    background: #FBE9E4;
    color: var(--tt-danger);
    font-size: 11.5px;
    font-weight: var(--tt-fw-black);
    padding: 5px 13px;
    border-radius: 999px;
}

/* ── 제목·부제 ── */
.result__title {
    font-size: 24px;
    font-weight: var(--tt-fw-black);
    letter-spacing: -0.01em;
    line-height: 1.3;
    text-align: center;
    color: var(--tt-text);
    margin: 0;
}

.result__desc {
    font-size: var(--tt-fs-caption);
    color: var(--tt-text-muted);
    font-weight: var(--tt-fw-medium);
    line-height: 1.55;
    text-align: center;
    margin: 0;
}

/* ── 정보 카드 (공통) ── */
.info-card {
    width: 100%;
    background: var(--tt-white);
    border: 1px solid var(--tt-border);
    border-radius: 18px;
    padding: 14px 16px;
    box-shadow: 0 8px 22px rgba(35, 40, 66, 0.05);
}

.info-card__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
}

.info-card__label {
    font-size: 12.5px;
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
}

/* ── 남은 목숨 ── */
.lives-count {
    font-size: 11.5px;
    font-weight: var(--tt-fw-black);
    color: var(--tt-danger);
    font-family: var(--tt-font-mono);
}

.lives-row {
    margin-top: 11px;
    display: flex;
    gap: 6px;
}

.lives-row__icon {
    width: 26px;
    height: 26px;
    object-fit: contain;
}

/* ── 판결 기록 ── */
.record-list {
    margin-top: 11px;
    display: flex;
    flex-direction: column;
    gap: 8px;
}

.record-list__row {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
}

.record-list__key {
    font-size: 11.5px;
    color: var(--tt-text-muted);
    font-weight: var(--tt-fw-bold);
}

.record-list__value {
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
}

.record-list__value--danger {
    color: var(--tt-danger);
}

/* ── 하단 ── */
.page__footer {
    flex: none;
    padding: 12px var(--tt-screen-padding) 18px;
    background: var(--tt-bg-subtle);
}

</style>
