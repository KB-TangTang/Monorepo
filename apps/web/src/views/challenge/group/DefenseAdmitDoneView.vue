<!--
  05b · 혐의 인정 완료 (E 결과형) — GC_07_06
  혐의 인정 후 결과 화면. 마스코트 + 유죄 종결 배지 + 판결 기록.

  ⚠ **목숨을 보여주지 않는다.** 목숨 차감은 판결 확정 후처리(이슈 #172)가 담당하고 혐의 인정
  API(`POST .../confession`)는 상태만 `GUILTY` 로 바꾼다. 여기서 「5 → 4」 를 그리면 아직
  일어나지 않은 차감을 단정하게 되고, #172 가 들어온 뒤엔 값이 두 벌로 갈린다.
-->
<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';

import BaseBackHeader from '@/components/common/BaseBackHeader.vue';
import BaseButton from '@/components/common/BaseButton.vue';
import { fetchTrialDetail } from '@/api/groupChallenge';

import mascotApology from '@/assets/images/emotions/43_apology.png';

const router = useRouter();
const route = useRoute();

const indictment = ref(null);

onMounted(async () => {
    try {
        indictment.value = await fetchTrialDetail(route.params.indictmentId);
    } catch (e) {
        console.error('[DefenseAdmitDone] 재판 상세를 불러오지 못해 그룹 상세로 되돌린다.', e);
        goToGroupDetail();
    }
});

/** `verdict_method` 를 사람이 읽는 문구로. 혐의 인정은 `CONFESSION` 이다. */
const VERDICT_METHOD_LABEL = {
    CONFESSION: '본인 인정 · 투표 없음',
    VOTE: '그룹원 투표',
    NO_VOTE: '투표 미달 · 자동 처리',
    SCRATCH_LOTTERY: '동표 · 복권 추첨',
};

const verdictLabel = computed(
    () => VERDICT_METHOD_LABEL[indictment.value?.verdictMethod] ?? '판결 완료',
);

/* ── 네비게이션 ── */
function goToGroupDetail() {
    /* replace 로 들어온 화면이라 `router.back()` 의 직전 항목이 그룹 상세라는 보장이 없다. */
    router.replace({ name: 'groupChallengeDetail', params: { id: route.params.id } });
}
</script>

<template>
    <div v-if="indictment" class="page">
        <!-- ── 상단 헤더 ── -->
        <BaseBackHeader title="혐의 인정">
            <template #right>
                <span class="page__case-number">{{ indictment.caseNumber }}</span>
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
                투표 없이 재판이 종결됐어요.<br>판결 기록에 유죄로 남아요.
            </p>

            <!-- 판결 기록 카드 -->
            <div class="info-card">
                <span class="info-card__label">판결 기록</span>
                <div class="record-list">
                    <div class="record-list__row">
                        <span class="record-list__key">{{ indictment.evalLabel }}</span>
                        <span class="record-list__value">
                            {{ indictment.settlementLabel }}
                        </span>
                    </div>
                    <div class="record-list__row">
                        <span class="record-list__key">
                            {{ indictment.categoryName ?? '총 소비' }}
                        </span>
                        <span class="record-list__value">
                            {{ indictment.currentAmount.toLocaleString() }}
                            / {{ indictment.limitAmount.toLocaleString() }}원
                        </span>
                    </div>
                    <div class="record-list__row">
                        <span class="record-list__key">기준 초과</span>
                        <span class="record-list__value record-list__value--danger">
                            {{ indictment.exceededAmount.toLocaleString() }}원
                        </span>
                    </div>
                    <div class="record-list__row">
                        <span class="record-list__key">판결</span>
                        <span class="record-list__value">{{ verdictLabel }}</span>
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

.info-card__label {
    font-size: 12.5px;
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
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
