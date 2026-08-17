<!--
  03 · 변론 완료 (E 결과형) — GC_07_03
  변론 제출 후 결과 화면. 마스코트 + 제출 완료 배지 + 제출 내용 요약 카드.
  "그룹 화면으로 돌아가기" → 그룹 챌린지 상세, "재판 진행 보기" → 추후 연결.
-->
<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';

import BaseBackHeader from '@/components/common/BaseBackHeader.vue';
import BaseButton from '@/components/common/BaseButton.vue';
import { fetchTrialDetail } from '@/api/groupChallenge';

import mascotPlease from '@/assets/images/emotions/44_please.png';

const router = useRouter();
const route = useRoute();

const indictment = ref(null);

onMounted(async () => {
    try {
        indictment.value = await fetchTrialDetail(route.params.indictmentId);
    } catch (e) {
        console.error('[DefenseDone] 재판 상세를 불러오지 못해 그룹 상세로 되돌린다.', e);
        goToGroupDetail();
    }
});

const defense = computed(() => indictment.value?.defense ?? null);

/** `2026-08-18T18:47:12` → `18:47`. 초는 버린다. */
const submittedAtLabel = computed(() => {
    const at = new Date(defense.value?.createdAt ?? '');
    if (Number.isNaN(at.getTime())) return '';
    return `${String(at.getHours()).padStart(2, '0')}:${String(at.getMinutes()).padStart(2, '0')}`;
});

/* ── 네비게이션 ── */
function goToGroupDetail() {
    /*
     * `router.back()` 을 쓰지 않는다 — 변론 작성 화면이 replace 로 이 화면을 밀어 넣었으므로
     * 직전 항목이 그룹 상세라는 보장이 없다(기소 안내에서 바로 들어온 경로도 있다).
     */
    router.replace({ name: 'groupChallengeDetail', params: { id: route.params.id } });
}

function goToTrial() {
    // 재판 진행(투표) 화면은 이슈 #171 담당이다. 그전까지는 그룹 상세로 보낸다.
    goToGroupDetail();
}
</script>

<template>
    <div v-if="indictment" class="page">
        <!-- ── 상단 헤더 ── -->
        <BaseBackHeader title="변론 완료">
            <template #right>
                <span class="page__case-number">{{ indictment.caseNumber }}</span>
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
            <div v-if="defense" class="summary-card">
                <div class="summary-card__header">
                    <span class="summary-card__label">제출한 변론</span>
                    <span class="summary-card__time">{{ submittedAtLabel }} 제출</span>
                </div>
                <p class="summary-card__text">{{ defense.content }}</p>

                <div class="summary-card__row">
                    <span class="summary-card__row-label">실제 개인 부담금</span>
                    <span class="summary-card__row-value">
                        {{ defense.actualBurdenAmount.toLocaleString() }}원
                    </span>
                </div>

                <!-- 증빙 — 서버가 키를 URL 로 변환해 내려주므로 그대로 렌더한다. -->
                <div v-if="defense.images.length" class="summary-card__thumbs">
                    <img
                        v-for="(url, idx) in defense.images"
                        :key="url"
                        :src="url"
                        :alt="`증빙 ${idx + 1}`"
                        class="summary-card__thumb"
                    >
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

.summary-card__row {
    margin-top: 10px;
    padding-top: 9px;
    border-top: 1px solid var(--tt-border);
    display: flex;
    align-items: baseline;
    justify-content: space-between;
}

.summary-card__row-label {
    font-size: 11.5px;
    color: var(--tt-text-muted);
    font-weight: var(--tt-fw-bold);
}

.summary-card__row-value {
    font-size: 13px;
    font-weight: var(--tt-fw-black);
    color: var(--tt-primary);
}

.summary-card__thumbs {
    margin-top: 10px;
    display: flex;
    gap: 6px;
}

.summary-card__thumb {
    width: 46px;
    height: 46px;
    border-radius: 9px;
    background: var(--tt-bg-subtle);
    border: 1px solid var(--tt-border);
    object-fit: cover;
    flex: none;
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
