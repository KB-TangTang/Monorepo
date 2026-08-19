<!--
  명예 법정 (S3 · S4) — 생존 순위 / 누적 순위.
  일일결산은 목숨 기준 순위, 기간결산은 누적 혐의액 기준 순위를 보여준다.
  /group-challenges/:id/ranking 으로 라우팅된다.
-->
<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { fetchGroupChallengeRanking } from '@/api/groupChallenge';

import ChallengeModeTabBar from '@/components/challenge/ChallengeModeTabBar.vue';
import GroupHonorPodium from '@/components/challenge/group/GroupHonorPodium.vue';
import GroupHonorRankList from '@/components/challenge/group/GroupHonorRankList.vue';
import GroupHonorPeriodRankList from '@/components/challenge/group/GroupHonorPeriodRankList.vue';
import GroupDetailPromise from '@/components/challenge/group/GroupDetailPromise.vue';

const route = useRoute();
const router = useRouter();

const ranking = ref(null);
const loading = ref(true);

onMounted(async () => {
    try {
        ranking.value = await fetchGroupChallengeRanking(route.params.id);
    } catch (e) {
        router.replace({ name: 'groupChallengeDetail', params: { id: route.params.id } });
    } finally {
        loading.value = false;
    }
});

const data = computed(() => ranking.value);
const isDaily = computed(() => data.value?.evalType === 'DAILY');
const evalLabel = computed(() => isDaily.value ? '일일 결산' : '기간 결산');
const titleText = computed(() => isDaily.value ? '생존 순위' : '누적 혐의액 순위');
const dateLabel = computed(() => {
    if (!data.value) return '';
    if (isDaily.value) {
        /* 결산 행이 아직 없으면(시작 첫날) 날짜가 빈 문자열로 온다 — 날짜 부분을 생략한다. */
        return data.value.lastSettlementDate
            ? `남은 목숨 기준 · ${data.value.lastSettlementDate} 결산 반영`
            : '남은 목숨 기준';
    }
    return '기간 누적 소비 기준 · 적게 쓴 순';
});

function goBack() {
    router.back();
}
</script>

<template>
    <div v-if="loading" class="honor-court honor-court--loading">
        <div class="honor-court__spinner"></div>
    </div>

    <div v-else-if="data" class="honor-court">
        <!-- ── 히어로 ── -->
        <div class="honor-court__hero">
            <div class="honor-court__deco"></div>

            <!-- 네비게이션 -->
            <div class="honor-court__nav">
                <div class="honor-court__nav-left" @click="goBack">
                    <svg
                        class="honor-court__back-icon"
                        viewBox="0 0 9 16"
                        fill="none"
                    >
                        <path
                            d="M8 1L1 8l7 7"
                            stroke="currentColor"
                            stroke-width="2.2"
                            stroke-linecap="round"
                            stroke-linejoin="round"
                        />
                    </svg>
                    <span class="honor-court__nav-title">명예 법정</span>
                </div>
                <span class="honor-court__nav-right">그룹 재판</span>
            </div>

            <!-- 뱃지 + 제목 -->
            <div class="honor-court__status">
                <span
                    class="honor-court__badge"
                    :class="isDaily ? 'honor-court__badge--daily' : 'honor-court__badge--period'"
                >{{ evalLabel }}</span>
                <h2 class="honor-court__title">{{ titleText }}</h2>
                <p class="honor-court__subtitle">{{ dateLabel }}</p>
            </div>
        </div>

        <!-- ── 콘텐츠 ── -->
        <div class="honor-court__content">
            <!-- 시상대 -->
            <GroupHonorPodium :podium="data.podium" />

            <!-- 약속 아코디언 -->
            <GroupDetailPromise
                :memo="data.memo"
                :memo-author="data.memoAuthor"
                :memo-date="data.memoDate"
                :show-subtitle="true"
            />

            <!-- 전체 순위 리스트 -->
            <GroupHonorRankList v-if="isDaily" :rankings="data.rankings" />
            <GroupHonorPeriodRankList
                v-else
                :rankings="data.rankings"
                :limit-amount="data.limitAmount"
            />
        </div>

        <!-- ── 하단 ── -->
        <div class="honor-court__footer">
            <ChallengeModeTabBar active-mode="group" />
        </div>
    </div>
</template>

<style scoped>
.honor-court {
    min-height: 100vh;
    background: var(--tt-bg-subtle);
    display: flex;
    flex-direction: column;
}

.honor-court--loading {
    align-items: center;
    justify-content: center;
}

.honor-court__spinner {
    width: 32px;
    height: 32px;
    border: 3px solid var(--tt-border);
    border-top-color: var(--tt-ink);
    border-radius: 50%;
    animation: spin 0.8s linear infinite;
}

@keyframes spin {
    to { transform: rotate(360deg); }
}

/* ── 히어로 ── */
.honor-court__hero {
    position: relative;
    flex: none;
    overflow: hidden;
    padding-bottom: 8px;
}

.honor-court__deco {
    position: absolute;
    top: -34px;
    right: -26px;
    width: 132px;
    height: 132px;
    border-radius: 50%;
    background: #F4EBD4;
}

/* ── 네비게이션 ── */
.honor-court__nav {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 12px 22px 0;
    position: relative;
    z-index: 2;
}

.honor-court__nav-left {
    display: flex;
    align-items: center;
    gap: 9px;
    cursor: pointer;
}

.honor-court__back-icon {
    width: 9px;
    height: 16px;
    color: var(--tt-text);
}

.honor-court__nav-title {
    font-size: var(--tt-fs-label);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
}

.honor-court__nav-right {
    font-size: 12.5px;
    font-weight: var(--tt-fw-black);
    color: var(--tt-blue);
}

/* ── 제목 영역 ── */
.honor-court__status {
    padding: 12px 22px 0;
    position: relative;
    z-index: 2;
}

.honor-court__badge {
    font-size: var(--tt-fs-overline);
    font-weight: var(--tt-fw-black);
    padding: 3px 9px;
    border-radius: var(--tt-radius-full);
}

.honor-court__badge--daily {
    background: var(--tt-gold-soft);
    color: var(--tt-gold-deep);
}

.honor-court__badge--period {
    background: #EAF0FF;
    color: #3E63D6;
}

.honor-court__title {
    font-size: var(--tt-fs-title);
    font-weight: var(--tt-fw-black);
    letter-spacing: -0.01em;
    margin-top: 8px;
    color: var(--tt-text);
}

.honor-court__subtitle {
    font-size: 12.5px;
    color: var(--tt-text-muted);
    margin-top: 3px;
}

/* ── 콘텐츠 ── */
.honor-court__content {
    flex: 1;
    overflow-y: auto;
    padding: 6px var(--tt-screen-padding) 0;
    display: flex;
    flex-direction: column;
    gap: 14px;
}

/* ── 하단 ── */
.honor-court__footer {
    flex: none;
    padding: 4px 0 0;
    background: var(--tt-bg-subtle);
}
</style>
