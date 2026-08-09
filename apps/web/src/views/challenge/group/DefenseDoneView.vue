<!--
  03 · 변론 완료 (E 결과형) — GC_07_03
  변론 제출 후 결과 화면. 마스코트 + 제출 완료 배지 + 제출 내용 요약 카드.
  "그룹 화면으로 돌아가기" → 그룹 챌린지 상세, "재판 진행 보기" → 추후 연결.
-->
<script setup>
import { ref, computed } from 'vue';
import { useRouter, useRoute } from 'vue-router';

import BaseBackHeader from '@/components/common/BaseBackHeader.vue';
import BaseButton from '@/components/common/BaseButton.vue';

import mascotPlease from '@/assets/images/emotions/44_please.png';

const router = useRouter();
const route = useRoute();

/* ── mock 데이터 (API 연동 전) ── */
const caseNumber = ref('2026-재판-0729');

const defense = ref({
    text: '친구들과 함께 주문했고, 제가 실제로 부담한 금액은 8,000원이에요. 다음부터는 미리 한도를 정할게요.',
    submittedAt: '18:47',
    images: [
        { name: '영수증.jpg' },
    ],
});

/* ── 네비게이션 ── */
function goToGroupDetail() {
    router.back();
}

function goToTrial() {
    // 추후 재판 진행 화면 연결
    goToGroupDetail();
}
</script>

<template>
    <div class="page">
        <!-- ── 상단 헤더 ── -->
        <BaseBackHeader title="변론 완료">
            <template #right>
                <span class="page__case-number">{{ caseNumber }}</span>
            </template>
        </BaseBackHeader>

        <!-- ── 본문 (중앙 정렬) ── -->
        <div class="page__body">
            <!-- 마스코트 + 글로우 -->
            <div class="result__mascot-wrap">
                <div class="result__glow"></div>
                <img :src="mascotPlease" alt="탕이" class="result__mascot">
            </div>

            <!-- 제출 완료 배지 -->
            <span class="result__badge">제출 완료</span>

            <!-- 제목·부제 -->
            <h2 class="result__title">변론을 제출했어요!</h2>
            <p class="result__desc">
                이제 그룹원들이 투표해요.<br>결과는 마감 후 공개돼요.
            </p>

            <!-- 제출한 변론 요약 카드 -->
            <div class="summary-card">
                <div class="summary-card__header">
                    <span class="summary-card__label">제출한 변론</span>
                    <span class="summary-card__time">{{ defense.submittedAt }} 제출</span>
                </div>
                <p class="summary-card__text">{{ defense.text }}</p>

                <div
                    v-for="(img, idx) in defense.images"
                    :key="idx"
                    class="summary-card__attachment"
                >
                    <div class="summary-card__thumb"></div>
                    <span class="summary-card__filename">{{ img.name }}</span>
                </div>
            </div>
        </div>

        <!-- ── 하단 버튼 ── -->
        <div class="page__footer">
            <BaseButton variant="dark" size="lg" block @click="goToGroupDetail">
                그룹 화면으로 돌아가기
            </BaseButton>
            <button type="button" class="page__link" @click="goToTrial">
                재판 진행 보기
            </button>
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
        rgba(245, 185, 33, 0.22) 0%,
        rgba(245, 185, 33, 0) 70%
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
    background: #E4F4EC;
    color: var(--tt-success);
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

/* ── 제출 변론 요약 카드 ── */
.summary-card {
    width: 100%;
    background: var(--tt-white);
    border: 1px solid var(--tt-border);
    border-radius: 18px;
    padding: 14px 16px;
    box-shadow: 0 8px 22px rgba(35, 40, 66, 0.05);
    margin-top: var(--tt-space-1);
}

.summary-card__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
}

.summary-card__label {
    font-size: 12.5px;
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
}

.summary-card__time {
    font-size: 11px;
    color: var(--tt-text-muted);
    font-weight: var(--tt-fw-bold);
    font-family: var(--tt-font-mono);
}

.summary-card__text {
    font-size: 12.5px;
    color: var(--tt-text-sub);
    line-height: 1.6;
    margin: 8px 0 0;
}

.summary-card__attachment {
    margin-top: 11px;
    display: flex;
    align-items: center;
    gap: 8px;
}

.summary-card__thumb {
    width: 32px;
    height: 32px;
    border-radius: 9px;
    background: var(--tt-bg-subtle);
    border: 1px solid var(--tt-border);
    flex: none;
}

.summary-card__filename {
    font-size: 11px;
    color: var(--tt-text-muted);
    font-weight: var(--tt-fw-bold);
    font-family: var(--tt-font-mono);
}

/* ── 하단 ── */
.page__footer {
    flex: none;
    padding: 12px var(--tt-screen-padding) 18px;
    background: var(--tt-bg-subtle);
}

.page__link {
    display: block;
    width: 100%;
    margin-top: 13px;
    padding: 0;
    border: 0;
    background: transparent;
    text-align: center;
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-black);
    color: var(--tt-primary);
    cursor: pointer;
}
</style>
