<!--
  그룹 챌린지 상세 — 시작 전(S5/S6) + 진행 중(S1/S2) 통합 뷰.
  status 와 evalType 에 따라 조건부로 다른 섹션을 렌더링한다.
  /group-challenges/:id 로 라우팅된다.
-->
<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { fetchGroupChallengeDetail } from '@/api/groupChallenge';

import ChallengeModeTabBar from '@/components/challenge/ChallengeModeTabBar.vue';
import GroupDetailInkCard from '@/components/challenge/group/GroupDetailInkCard.vue';
import GroupDetailLivesBar from '@/components/challenge/group/GroupDetailLivesBar.vue';
import GroupDetailPeriodBar from '@/components/challenge/group/GroupDetailPeriodBar.vue';
import GroupDetailPromise from '@/components/challenge/group/GroupDetailPromise.vue';
import GroupDetailMemberGrid from '@/components/challenge/group/GroupDetailMemberGrid.vue';
import GroupDetailMemberTable from '@/components/challenge/group/GroupDetailMemberTable.vue';

import mascotHappy from '@/assets/images/emotions/01_happy.png';

const route = useRoute();
const router = useRouter();

const challenge = ref(null);
const loading = ref(true);

onMounted(async () => {
    try {
        challenge.value = await fetchGroupChallengeDetail(route.params.id);
    } catch (e) {
        router.replace({ name: 'groupChallengeList' });
    } finally {
        loading.value = false;
    }
});

const ch = computed(() => challenge.value);
const isRecruiting = computed(() => ch.value?.status === 'RECRUITING');
const isActive = computed(() => ch.value?.status === 'ACTIVE');
const isClosed = computed(() => ch.value?.status === 'CLOSED');
const isDaily = computed(() => ch.value?.evalType === 'DAILY');

/* 헤더 우측 링크 텍스트 */
const headerRight = computed(() => isClosed.value ? '최종 순위' : '재판');

/* 종료 결과 텍스트 */
const outcomeLabel = computed(() => {
    if (!isClosed.value) return '';
    return ch.value.finalOutcome === 'SURVIVED' ? '승소' : '패소';
});

const outcomeStyle = computed(() => {
    if (!isClosed.value) return {};
    if (ch.value.finalOutcome === 'SURVIVED') {
        return { bg: 'var(--tt-green-soft)', color: 'var(--tt-green)' };
    }
    return { bg: 'var(--tt-red-soft)', color: 'var(--tt-red-deep)' };
});

/* 상태 뱃지 */
const statusBadges = computed(() => {
    if (!ch.value) return [];
    if (isRecruiting.value) {
        const dBadge = isDaily.value
            ? { bg: 'var(--tt-gold-soft)', color: 'var(--tt-gold-deep)' }
            : { bg: '#EAF0FF', color: '#3E63D6' };
        return [
            { label: '시작 전', bg: 'var(--tt-bg-fill)', color: 'var(--tt-text-body)' },
            { label: `D-${ch.value.daysUntilStart}`, ...dBadge },
        ];
    }
    if (isClosed.value) {
        return [
            { label: '종료됨', bg: 'var(--tt-bg-fill)', color: 'var(--tt-text-muted)' },
            { label: outcomeLabel.value, ...outcomeStyle.value },
        ];
    }
    return [
        { label: '진행 중', bg: 'var(--tt-green-soft)', color: 'var(--tt-green)' },
        { label: `DAY ${ch.value.currentDay} / ${ch.value.totalDays}`, bg: 'var(--tt-ink)', color: 'var(--tt-gold)' },
    ];
});

/* 기간 설명 */
const periodText = computed(() => {
    if (!ch.value) return '';
    const s = formatDate(ch.value.startDate);
    const e = formatDate(ch.value.endDate);
    if (isRecruiting.value) {
        return `${s} ~ ${e} · ${ch.value.totalDays}일간`;
    }
    if (isClosed.value) {
        return `${s} ~ ${e} · ${ch.value.totalDays}일간 완료`;
    }
    return `${s} ~ ${e} · ${ch.value.settleTime || '매일 23:30 결산'}`;
});

function formatDate(iso) {
    const d = new Date(iso);
    return `${d.getMonth() + 1}월 ${d.getDate()}일`;
}

/* 네비게이션 */
function goBack() {
    router.back();
}

function goToList() {
    router.push({ name: 'groupChallengeList' });
}

function goToRanking() {
    router.push({ name: 'groupChallengeRanking', params: { id: ch.value.id } });
}

function goToInvite() {
    router.push({ name: 'groupChallengeInvite', params: { groupId: ch.value.id } });
}
</script>

<template>
    <div v-if="loading" class="gc-detail gc-detail--loading">
        <div class="gc-detail__spinner"></div>
    </div>

    <div v-else-if="ch" class="gc-detail">
        <!-- ── 히어로 영역 (장식 원 + 헤더 + 상태) ── -->
        <div class="gc-detail__hero">
            <div class="gc-detail__deco gc-detail__deco--lg"></div>
            <div
                v-if="isRecruiting"
                class="gc-detail__deco gc-detail__deco--sm"
            ></div>

            <!-- 내비게이션 헤더 -->
            <div class="gc-detail__nav">
                <div class="gc-detail__nav-left" @click="goBack">
                    <svg
                        class="gc-detail__back-icon"
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
                    <span class="gc-detail__nav-title">그룹 챌린지</span>
                </div>
                <span
                    class="gc-detail__nav-right"
                    @click="goToRanking"
                >
                    {{ headerRight }}
                </span>
            </div>

            <!-- 상태 뱃지 + 챌린지 이름 -->
            <div class="gc-detail__status">
                <div class="gc-detail__badges">
                    <span
                        v-for="(b, i) in statusBadges"
                        :key="i"
                        class="gc-detail__badge"
                        :style="{ background: b.bg, color: b.color }"
                    >
                        {{ b.label }}
                    </span>
                </div>
                <h2 class="gc-detail__name">{{ ch.groupName }}</h2>
                <p class="gc-detail__period">{{ periodText }}</p>
            </div>

            <!-- 탕이 마스코트 (시작 전에만 히어로에 표시) -->
            <img
                v-if="isRecruiting"
                :src="mascotHappy"
                alt="탕이 환영"
                class="gc-detail__mascot"
            >
        </div>

        <!-- ── 콘텐츠 영역 ── -->
        <div class="gc-detail__content">
            <!-- 종료: 최종 결과 카드 -->
            <template v-if="isClosed">
                <div class="gc-detail__result-card">
                    <div class="gc-detail__result-header">
                        <span class="gc-detail__result-title">최종 결과</span>
                        <span
                            class="gc-detail__result-badge"
                            :style="{
                                background: outcomeStyle.bg,
                                color: outcomeStyle.color,
                            }"
                        >
                            {{ outcomeLabel }}
                        </span>
                    </div>

                    <div class="gc-detail__result-stats">
                        <div class="gc-detail__result-stat">
                            <span class="gc-detail__result-label">최종 순위</span>
                            <span class="gc-detail__result-value">
                                {{ ch.finalRank }}위
                                <span class="gc-detail__result-total">/ {{ ch.memberCount }}명</span>
                            </span>
                        </div>
                        <div v-if="ch.savingsAmount" class="gc-detail__result-stat">
                            <span class="gc-detail__result-label">절약 금액</span>
                            <span class="gc-detail__result-value gc-detail__result-value--green">
                                {{ ch.savingsAmount.toLocaleString() }}원
                            </span>
                        </div>
                        <div v-if="ch.finalChargeAmount > 0" class="gc-detail__result-stat">
                            <span class="gc-detail__result-label">누적 혐의액</span>
                            <span class="gc-detail__result-value gc-detail__result-value--red">
                                {{ ch.finalChargeAmount.toLocaleString() }}원
                            </span>
                        </div>
                    </div>

                    <!-- 최종 멤버 순위 -->
                    <div v-if="ch.finalMembers" class="gc-detail__final-list">
                        <div
                            v-for="m in ch.finalMembers"
                            :key="m.userId"
                            class="gc-detail__final-row"
                            :class="{
                                'gc-detail__final-row--me': m.userId === 1,
                                'gc-detail__final-row--eliminated': m.finalOutcome === 'ELIMINATED',
                            }"
                        >
                            <span class="gc-detail__final-rank">{{ m.finalRank }}</span>
                            <div
                                class="gc-detail__final-avatar"
                                :style="{ background: m.avatarColor }"
                            >
                                {{ m.initial }}
                            </div>
                            <span
                                class="gc-detail__final-name"
                                :class="{ 'gc-detail__final-name--dim': m.finalOutcome === 'ELIMINATED' }"
                            >
                                {{ m.nickname }}
                            </span>
                            <span v-if="m.userId === 1" class="gc-detail__final-me">나</span>
                            <span
                                class="gc-detail__final-outcome"
                                :style="{
                                    background: m.finalOutcome === 'SURVIVED'
                                        ? 'var(--tt-green-soft)' : 'var(--tt-red-soft)',
                                    color: m.finalOutcome === 'SURVIVED'
                                        ? 'var(--tt-green)' : 'var(--tt-red-deep)',
                                }"
                            >
                                {{ m.finalOutcome === 'SURVIVED' ? '승소' : '패소' }}
                            </span>
                        </div>
                    </div>
                </div>
            </template>

            <!-- 시작 전 / 진행 중 -->
            <template v-else>
                <!-- Ink 스탯 카드 -->
                <GroupDetailInkCard
                    :eval-type="ch.evalType"
                    :status="ch.status"
                    :limit-amount="ch.limitAmount"
                    :category-name="ch.categoryName"
                    :current-amount="ch.myDailyAmount"
                    :usage-percent="ch.myUsagePercent"
                    :remaining-amount="ch.myRemainingAmount"
                />

                <!-- 목숨 바 (일일결산) / 기간 카드 (기간결산) -->
                <GroupDetailLivesBar
                    v-if="isDaily"
                    :lives-count="ch.livesCount || ch.maxLives"
                    :max-lives="ch.maxLives"
                    :mode="isRecruiting ? 'upcoming' : 'active'"
                    :size="isRecruiting ? 21 : 19"
                />
                <GroupDetailPeriodBar
                    v-else
                    :start-date="ch.startDate"
                    :end-date="ch.endDate"
                    :total-days="ch.totalDays"
                    :mode="isRecruiting ? 'upcoming' : 'active'"
                    :current-day="ch.currentDay || 0"
                />

                <!-- 약속 아코디언 -->
                <GroupDetailPromise
                    :memo="ch.memo"
                    :memo-author="ch.memoAuthor"
                    :memo-date="ch.memoDate"
                    :show-subtitle="isRecruiting"
                />

                <!-- 시작 전: 참여 멤버 그리드 -->
                <GroupDetailMemberGrid
                    v-if="isRecruiting"
                    :members="ch.members"
                    :member-count="ch.memberCount"
                    :max-members="ch.maxMembers"
                    @invite="goToInvite"
                />

                <!-- 진행 중: 멤버 소비 상태 테이블 -->
                <GroupDetailMemberTable
                    v-if="isActive && ch.dailyMembers"
                    :members="ch.dailyMembers"
                    :eval-type="ch.evalType"
                    :limit-amount="ch.limitAmount"
                />
            </template>
        </div>

        <!-- ── 하단 CTA ── -->
        <div class="gc-detail__footer">
            <div class="gc-detail__actions">
                <button
                    v-if="isActive"
                    class="gc-detail__btn gc-detail__btn--secondary"
                    @click="goToInvite"
                >
                    소환
                </button>
                <button
                    class="gc-detail__btn"
                    :class="{
                        'gc-detail__btn--primary': isActive,
                        'gc-detail__btn--outline': isRecruiting || isClosed,
                    }"
                    @click="goToList"
                >
                    그룹 목록으로
                </button>
            </div>

            <ChallengeModeTabBar active-mode="group" />
        </div>
    </div>
</template>

<style scoped>
.gc-detail {
    min-height: 100vh;
    background: var(--tt-bg-subtle);
    display: flex;
    flex-direction: column;
}

.gc-detail--loading {
    align-items: center;
    justify-content: center;
}

.gc-detail__spinner {
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

/* ── 히어로 영역 ── */
.gc-detail__hero {
    position: relative;
    flex: none;
    overflow: hidden;
    padding-bottom: 6px;
}

.gc-detail__deco {
    position: absolute;
    border-radius: 50%;
}

.gc-detail__deco--lg {
    top: -34px;
    right: -26px;
    width: 132px;
    height: 132px;
    background: #E9EEFB;
}

.gc-detail__deco--sm {
    top: 44px;
    right: 40px;
    width: 56px;
    height: 56px;
    background: #F4EBD4;
}

/* ── 네비게이션 ── */
.gc-detail__nav {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 12px 22px 0;
    position: relative;
    z-index: 2;
}

.gc-detail__nav-left {
    display: flex;
    align-items: center;
    gap: 9px;
    cursor: pointer;
}

.gc-detail__back-icon {
    width: 9px;
    height: 16px;
    color: var(--tt-text);
}

.gc-detail__nav-title {
    font-size: var(--tt-fs-label);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
}

.gc-detail__nav-right {
    font-size: 12.5px;
    font-weight: var(--tt-fw-black);
    color: var(--tt-blue);
    cursor: pointer;
}

/* ── 상태 + 제목 ── */
.gc-detail__status {
    padding: 12px 22px 0;
    position: relative;
    z-index: 2;
}

.gc-detail__badges {
    display: flex;
    gap: 6px;
}

.gc-detail__badge {
    font-size: var(--tt-fs-overline);
    font-weight: var(--tt-fw-black);
    padding: 3px 9px;
    border-radius: var(--tt-radius-full);
}

.gc-detail__name {
    font-size: var(--tt-fs-title);
    font-weight: var(--tt-fw-black);
    letter-spacing: -0.01em;
    margin-top: 9px;
    color: var(--tt-text);
}

.gc-detail__period {
    font-size: 12.5px;
    color: var(--tt-text-muted);
    margin-top: 4px;
}

/* ── 마스코트 (시작 전) ── */
.gc-detail__mascot {
    position: absolute;
    right: 18px;
    top: 56px;
    width: 74px;
    height: 74px;
    object-fit: contain;
    z-index: 2;
    filter: drop-shadow(0 6px 12px rgba(35, 40, 66, 0.16));
    animation: tt-mascot-float 4s ease-in-out infinite;
}

@keyframes tt-mascot-float {
    0%, 100% { transform: translateY(0); }
    50% { transform: translateY(-5px); }
}

/* ── 콘텐츠 ── */
.gc-detail__content {
    flex: 1;
    overflow-y: auto;
    padding: 6px var(--tt-screen-padding) 0;
    display: flex;
    flex-direction: column;
    gap: 7px;
}

/* ── 하단 ── */
.gc-detail__footer {
    flex: none;
    padding: 10px var(--tt-screen-padding) 0;
    background: var(--tt-bg-subtle);
}

.gc-detail__actions {
    display: flex;
    gap: 10px;
}

.gc-detail__btn {
    padding: 14px;
    border-radius: var(--tt-radius-md);
    font-size: var(--tt-fs-label);
    font-weight: var(--tt-fw-black);
    text-align: center;
    border: none;
    cursor: pointer;
}

.gc-detail__btn--primary {
    flex: 1;
    background: var(--tt-ink);
    color: var(--tt-white);
}

.gc-detail__btn--outline {
    flex: 1;
    background: transparent;
    border: 1.5px solid var(--tt-ink);
    color: var(--tt-ink);
}

.gc-detail__btn--secondary {
    flex: none;
    width: 64px;
    background: var(--tt-bg);
    border: 1.5px solid var(--tt-border);
    color: var(--tt-text-body);
    font-size: var(--tt-fs-body);
}

/* ── 종료 결과 카드 ── */
.gc-detail__result-card {
    background: var(--tt-bg);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-xl);
    box-shadow: var(--tt-elevation-2);
    padding: 18px 16px;
}

.gc-detail__result-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
}

.gc-detail__result-title {
    font-size: var(--tt-fs-button);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
}

.gc-detail__result-badge {
    font-size: var(--tt-fs-overline);
    font-weight: var(--tt-fw-black);
    padding: 4px 10px;
    border-radius: var(--tt-radius-full);
}

.gc-detail__result-stats {
    margin-top: 14px;
    display: flex;
    flex-direction: column;
    gap: 10px;
}

.gc-detail__result-stat {
    display: flex;
    align-items: center;
    justify-content: space-between;
}

.gc-detail__result-label {
    font-size: var(--tt-fs-body);
    color: var(--tt-text-muted);
}

.gc-detail__result-value {
    font-size: var(--tt-fs-button);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
}

.gc-detail__result-total {
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-medium);
    color: var(--tt-text-muted);
}

.gc-detail__result-value--green {
    color: var(--tt-green);
}

.gc-detail__result-value--red {
    color: var(--tt-red-deep);
}

/* ── 최종 멤버 순위 ── */
.gc-detail__final-list {
    margin-top: 16px;
    border-top: 1px solid var(--tt-border);
    padding-top: 12px;
}

.gc-detail__final-row {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 8px 0;
}

.gc-detail__final-row + .gc-detail__final-row {
    border-top: 1px solid var(--tt-bg-fill);
}

.gc-detail__final-row--me {
    background: var(--tt-bg-subtle);
    border-radius: var(--tt-radius-sm);
    padding: 8px 6px;
    margin: 0 -6px;
}

.gc-detail__final-rank {
    width: 16px;
    text-align: center;
    font-size: 12.5px;
    font-weight: var(--tt-fw-black);
    color: var(--tt-text-hint);
}

.gc-detail__final-avatar {
    width: 26px;
    height: 26px;
    border-radius: 50%;
    color: var(--tt-white);
    font-size: var(--tt-fs-badge);
    font-weight: var(--tt-fw-black);
    display: flex;
    align-items: center;
    justify-content: center;
    flex: none;
}

.gc-detail__final-name {
    flex: 1;
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text);
}

.gc-detail__final-name--dim {
    color: var(--tt-text-muted);
}

.gc-detail__final-me {
    background: var(--tt-gold-soft);
    color: var(--tt-gold-deep);
    font-size: 9.5px;
    font-weight: var(--tt-fw-black);
    padding: 2px 6px;
    border-radius: var(--tt-radius-full);
}

.gc-detail__final-outcome {
    font-size: var(--tt-fs-overline);
    font-weight: var(--tt-fw-black);
    padding: 4px 10px;
    border-radius: var(--tt-radius-full);
    flex: none;
}
</style>
