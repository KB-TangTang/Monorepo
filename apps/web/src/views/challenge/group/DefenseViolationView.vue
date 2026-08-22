<!--
  01 · 소비 기준 위반 상세 (D 결정형) — GC_07_01
  기소가 발생했을 때 변론 작성 또는 혐의 인정을 선택하는 진입 화면.
  탭바를 숨기고 Court Bar 헤더로 이탈 경로를 제한한다.
-->
<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue';
import { useRouter, useRoute } from 'vue-router';

import DefenseCourtHeader from '@/components/challenge/group/DefenseCourtHeader.vue';
import BaseBottomSheet from '@/components/common/BaseBottomSheet.vue';
import BaseButton from '@/components/common/BaseButton.vue';
import { confessIndictment, fetchTrialDetail } from '@/api/groupChallenge';

import mascotApology from '@/assets/images/emotions/43_apology.png';

const router = useRouter();
const route = useRoute();

const indictment = ref(null);

/*
 * 실패를 조용히 넘기면 화면이 「클릭이 씹힌 것」처럼 보인다 — replace 라 히스토리도 남지 않아
 * 카드를 눌러도 아무 일이 없는 것과 구분되지 않는다 (`GroupChallengeDetailView` 와 같은 이유).
 */
onMounted(async () => {
    try {
        indictment.value = await fetchTrialDetail(route.params.indictmentId);
    } catch (e) {
        console.error('[DefenseViolation] 재판 상세를 불러오지 못해 그룹 상세로 되돌린다.', e);
        router.replace({ name: 'groupChallengeDetail', params: { id: route.params.id } });
        return;
    }
    startTimer();
});

/* ── 마감 타이머 ──
 * 남은 초를 세지 않고 **절대시각과 지금을 매초 다시 비교한다.** 진입 시점의 남은 초를 감소시키면
 * 화면을 열어 둔 채 절전에 들어갔다 돌아왔을 때 값이 그 시점에 굳는다. */
const now = ref(Date.now());
let timerHandle = null;

function startTimer() {
    timerHandle = setInterval(() => {
        now.value = Date.now();
    }, 1000);
}

onBeforeUnmount(() => {
    clearInterval(timerHandle);
});

const remainingLabel = computed(() => {
    const deadline = new Date(indictment.value?.defenseDeadline ?? '').getTime();
    if (Number.isNaN(deadline)) return '';

    const left = Math.max(0, deadline - now.value);
    if (left === 0) return '마감됨';

    const h = Math.floor(left / 3600000);
    const m = Math.floor(left / 60000) % 60;
    if (h > 0) return `${h}시간 ${m}분 남음`;
    return `${m}분 남음`;
});

/* ── 프로그레스 바 계산 ── */
const withinPercent = computed(() => {
    const total = indictment.value.currentAmount;
    if (total === 0) return 0;
    const within = Math.min(indictment.value.limitAmount, total);
    return Math.round((within / total) * 100);
});

const overPercent = computed(() => 100 - withinPercent.value);

/* 기간결산은 「오늘」이 아니다 — 구간 전체를 합쳐 판정하므로 문구를 갈라야 한다. */
const isDaily = computed(() => indictment.value?.evalType === 'DAILY');
const scopeLabel = computed(() => (isDaily.value ? '오늘' : '기간'));
const summaryLabel = computed(() => (isDaily.value ? '오늘 나의 소비' : '기간 나의 소비'));
const limitLabel = computed(() => (isDaily.value ? '하루 기준' : '기간 기준'));

/* ── 혐의 인정 확인 바텀시트 (05a) ── */
const showAdmitSheet = ref(false);
const HISTORY_SETTLE_MS = 100;
const pendingNav = ref(null);
const admitting = ref(false);

function openAdmitSheet() {
    showAdmitSheet.value = true;
}

/*
 * 서버가 성공한 뒤에 시트를 닫는다. 낙관적으로 먼저 넘기면 이미 마감돼
 * `DEFENSE_NOT_ALLOWED` 로 거절된 요청에도 「인정 완료」 화면이 뜬다.
 */
async function confirmAdmit() {
    if (admitting.value) return;
    admitting.value = true;
    try {
        await confessIndictment(route.params.indictmentId);
        pendingNav.value = 'defenseAdmitDone';
        showAdmitSheet.value = false;
    } catch (e) {
        console.error('[DefenseViolation] 혐의 인정에 실패했다.', e);
        window.alert(e.message ?? '혐의 인정에 실패했어요. 잠시 후 다시 시도해주세요.');
    } finally {
        admitting.value = false;
    }
}

function cancelAdmit() {
    showAdmitSheet.value = false;
}

watch(showAdmitSheet, (val) => {
    if (!val && pendingNav.value) {
        const nav = pendingNav.value;
        pendingNav.value = null;
        setTimeout(() => {
            router.replace({
                name: nav,
                params: {
                    id: route.params.id,
                    indictmentId: route.params.indictmentId,
                },
            });
        }, HISTORY_SETTLE_MS);
    }
});

/* ── 변론 작성 진입 ── */
function startDefense() {
    router.replace({
        name: 'defenseActualCost',
        params: {
            id: route.params.id,
            indictmentId: route.params.indictmentId,
        },
    });
}
</script>

<template>
    <div v-if="indictment" class="violation-page">
        <!-- ── Court Bar 헤더 ── -->
        <DefenseCourtHeader
            title="소비 기준 위반"
            :case-number="indictment.caseNumber"
        >
            <div class="violation-page__header-body">
                <div class="violation-page__tags">
                    <span class="violation-page__tag violation-page__tag--eval">
                        {{ indictment.evalLabel }}
                    </span>
                    <span class="violation-page__tag violation-page__tag--charge">기소 발생</span>
                </div>
                <h2 class="violation-page__headline">
                    {{ scopeLabel }} 기준 금액을<br>넘었어요
                </h2>
                <p class="violation-page__sub">
                    {{ indictment.challengeName }} · {{ indictment.settlementLabel }}
                </p>
            </div>
        </DefenseCourtHeader>

        <!-- ── 본문 ── -->
        <div class="violation-page__content">
            <!-- 소비 요약 카드 -->
            <div class="violation-page__summary-card">
                <div class="violation-page__summary-header">
                    <span class="violation-page__summary-label">{{ summaryLabel }}</span>
                    <span class="violation-page__summary-limit">
                        {{ limitLabel }} {{ indictment.limitAmount.toLocaleString() }}원
                    </span>
                </div>

                <div class="violation-page__amount-row">
                    <span class="violation-page__amount">
                        {{ indictment.currentAmount.toLocaleString() }}
                    </span>
                    <span class="violation-page__amount-unit">원</span>
                </div>

                <!-- 프로그레스 바 -->
                <div class="violation-page__progress">
                    <div
                        class="violation-page__bar violation-page__bar--within"
                        :style="{ width: withinPercent + '%' }"
                    ></div>
                    <div
                        class="violation-page__bar violation-page__bar--over"
                        :style="{ width: overPercent + '%' }"
                    ></div>
                </div>

                <div class="violation-page__summary-footer">
                    <span class="violation-page__exceeded-badge">
                        {{ indictment.exceededAmount.toLocaleString() }}원 초과
                    </span>
                    <span class="violation-page__ratio">
                        {{ indictment.currentAmount.toLocaleString() }} / {{ indictment.limitAmount.toLocaleString() }}
                    </span>
                </div>
            </div>

            <!--
              ⚠ 「기소 발생 거래」 카드를 두지 않는다. 한도 초과는 **누적**으로 판정되므로
              기소를 발화시킨 거래가 문제의 거래라는 보장이 없다 — 친구들 몫까지 대납한 큰 거래는
              그 시점에 한도 미달이어서 기소를 만들지 않고, 뒤따르는 평범한 소비가 기소를 만든다.
              한 건만 보여주면 정작 대납한 금액을 신고할 방법이 없다.
              결산 구간 전체 거래는 다음 화면(실제 부담금 입력)이 보여준다.
            -->

            <!-- 변론 마감 안내 -->
            <div class="violation-page__deadline-card">
                <div class="violation-page__deadline-icon">
                    <span class="violation-page__deadline-bang">!</span>
                </div>
                <div class="violation-page__deadline-text">
                    <div class="violation-page__deadline-title">변론 마감</div>
                    <div class="violation-page__deadline-value">
                        {{ indictment.defenseDeadlineLabel }} ·
                        <span class="violation-page__deadline-remain">{{ remainingLabel }}</span>
                    </div>
                </div>
            </div>

            <!--
              마감 배치는 변론이 없어도 유죄로 확정하지 않는다 — `DEFENSE_WAIT` → `VOTING` 으로
              옮기기만 한다(`IndictmentMapper.moveExpiredDefensesToVoting`). 「자동으로 혐의가
              인정된다」고 적으면 실제 동작과 다른 안내가 된다. 목숨 차감 문구도 두지 않는다.
            -->
            <div class="violation-page__notice">
                기한 안에 변론을 제출하지 않으면
                <b>변론 없이 배심원 투표로 넘어가요.</b>
            </div>
        </div>

        <!-- ── 하단 액션 ── -->
        <div class="violation-page__actions">
            <div class="violation-page__btn-row">
                <BaseButton
                    variant="primary"
                    size="lg"
                    block
                    class="violation-page__btn-defense"
                    @click="startDefense"
                >
                    변론 작성하기
                </BaseButton>
                <button
                    type="button"
                    class="violation-page__btn-admit"
                    @click="openAdmitSheet"
                >
                    혐의 인정하기
                </button>
            </div>
        </div>

        <!-- ── 05a · 혐의 인정 확인 바텀시트 ── -->
        <BaseBottomSheet
            v-model="showAdmitSheet"
            close-on-overlay
            close-on-esc
        >
            <div class="admit-sheet">
                <div class="admit-sheet__top">
                    <img
                        :src="mascotApology"
                        alt="탕이"
                        class="admit-sheet__mascot"
                    >
                    <div class="admit-sheet__top-text">
                        <span class="admit-sheet__warn-badge">되돌릴 수 없어요</span>
                        <div class="admit-sheet__headline">
                            혐의를 인정하면<br>바로 유죄로 종결돼요
                        </div>
                    </div>
                </div>

                <!-- 목숨 표시를 두지 않는다 — 혐의 인정은 목숨을 건드리지 않는다(개표·확정 담당). -->
                <div class="admit-sheet__info-card">
                    <div class="admit-sheet__info-header">
                        <span class="admit-sheet__info-label">초과 금액</span>
                        <span class="admit-sheet__info-value">
                            {{ indictment.exceededAmount.toLocaleString() }}원
                        </span>
                    </div>
                    <p class="admit-sheet__info-desc">
                        투표 없이 종결되고
                        <b class="admit-sheet__info-strong">판결 기록에 유죄로 남아요.</b>
                        제출한 변론이나 부담금은 반영되지 않아요.
                    </p>
                </div>
            </div>

            <template #footer>
                <div class="admit-sheet__footer">
                    <BaseButton
                        variant="primary"
                        size="lg"
                        block
                        @click="cancelAdmit"
                    >
                        아니요, 변론할게요
                    </BaseButton>
                    <button
                        type="button"
                        class="admit-sheet__btn-confirm"
                        :disabled="admitting"
                        @click="confirmAdmit"
                    >
                        {{ admitting ? '처리 중…' : '네, 혐의를 인정할게요' }}
                    </button>
                    <p class="admit-sheet__hint">
                        변론은 {{ indictment.defenseDeadlineLabel }}까지 제출할 수 있어요.
                    </p>
                </div>
            </template>
        </BaseBottomSheet>
    </div>
</template>

<style scoped>
/* ── 페이지 레이아웃 ── */
.violation-page {
    display: flex;
    flex-direction: column;
    min-height: 100vh;
    min-height: 100dvh;
    background: var(--tt-bg-subtle);
}

/* ── 헤더 본문 ── */
.violation-page__header-body {
    text-align: center;
}

.violation-page__tags {
    display: flex;
    gap: 6px;
    justify-content: center;
}

.violation-page__tag {
    font-size: var(--tt-fs-overline);
    font-weight: var(--tt-fw-black);
    padding: 4px 10px;
    border-radius: var(--tt-radius-full);
}

.violation-page__tag--eval {
    background: rgba(245, 185, 33, 0.16);
    color: var(--tt-gold);
}

.violation-page__tag--charge {
    background: rgba(224, 102, 75, 0.2);
    color: #FF9E86;
}

.violation-page__headline {
    font-size: var(--tt-fs-title);
    font-weight: var(--tt-fw-black);
    color: var(--tt-white);
    letter-spacing: -0.01em;
    line-height: var(--tt-lh-snug);
    margin-top: var(--tt-space-3);
}

.violation-page__sub {
    font-size: var(--tt-fs-caption);
    color: #AEB2CC;
    margin-top: 7px;
    font-weight: var(--tt-fw-semibold);
}

/* ── 본문 카드 영역 ── */
.violation-page__content {
    flex: 1;
    padding: 14px var(--tt-screen-padding) 0;
    display: flex;
    flex-direction: column;
    gap: var(--tt-card-gap);
    overflow-y: auto;
}

/* ── 소비 요약 카드 ── */
.violation-page__summary-card {
    background: var(--tt-bg);
    border: 1px solid #F3D3C9;
    border-radius: var(--tt-radius-xl);
    padding: var(--tt-card-padding) 18px;
    box-shadow: 0 8px 22px rgba(224, 102, 75, 0.07);
}

.violation-page__summary-header {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
}

.violation-page__summary-label {
    font-size: var(--tt-fs-caption);
    color: var(--tt-text-muted);
    font-weight: var(--tt-fw-bold);
}

.violation-page__summary-limit {
    font-size: var(--tt-fs-badge);
    color: var(--tt-text-muted);
    font-weight: var(--tt-fw-bold);
}

.violation-page__amount-row {
    display: flex;
    align-items: baseline;
    gap: 4px;
    margin-top: var(--tt-space-2);
}

.violation-page__amount {
    font-size: 31px;
    font-weight: var(--tt-fw-black);
    color: var(--tt-danger-deep);
    letter-spacing: -0.02em;
}

.violation-page__amount-unit {
    font-size: 17px;
    font-weight: var(--tt-fw-black);
    color: var(--tt-danger-deep);
}

/* ── 프로그레스 바 ── */
.violation-page__progress {
    margin-top: 13px;
    height: 9px;
    border-radius: var(--tt-radius-full);
    background: var(--tt-border-track);
    overflow: hidden;
    display: flex;
}

.violation-page__bar {
    height: 100%;
    transition: width 0.6s ease;
}

.violation-page__bar--within {
    background: var(--tt-accent);
}

.violation-page__bar--over {
    background: var(--tt-danger);
}

.violation-page__summary-footer {
    margin-top: 11px;
    display: flex;
    align-items: center;
    justify-content: space-between;
}

.violation-page__exceeded-badge {
    background: var(--tt-danger-subtle);
    color: var(--tt-danger-deep);
    font-size: var(--tt-fs-badge);
    font-weight: var(--tt-fw-black);
    padding: 5px 11px;
    border-radius: var(--tt-radius-full);
}

.violation-page__ratio {
    font-size: var(--tt-fs-badge);
    color: var(--tt-text-muted);
    font-weight: var(--tt-fw-bold);
    font-family: var(--tt-font-mono);
}

/* ── 변론 마감 안내 ── */
.violation-page__deadline-card {
    background: var(--tt-danger-subtle);
    border: 1px solid #F3D3C9;
    border-radius: var(--tt-radius-lg);
    padding: 13px var(--tt-card-padding);
    display: flex;
    align-items: center;
    gap: 11px;
}

.violation-page__deadline-icon {
    width: 30px;
    height: 30px;
    border-radius: 50%;
    background: var(--tt-bg);
    border: 1.5px solid var(--tt-danger);
    display: flex;
    align-items: center;
    justify-content: center;
    flex: none;
}

.violation-page__deadline-bang {
    font-size: 14px;
    font-weight: 900;
    color: var(--tt-danger);
}

.violation-page__deadline-title {
    font-size: var(--tt-fs-badge);
    font-weight: var(--tt-fw-black);
    color: var(--tt-danger-deep);
}

.violation-page__deadline-value {
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
    margin-top: 2px;
}

.violation-page__deadline-remain {
    color: var(--tt-danger-deep);
}

/* ── 자동 인정 안내 ── */
.violation-page__notice {
    background: var(--tt-bg-fill);
    border-radius: var(--tt-radius-lg);
    padding: 13px var(--tt-card-padding);
    font-size: var(--tt-fs-caption);
    color: var(--tt-text-body);
    line-height: var(--tt-lh-normal);
}

.violation-page__notice b {
    color: var(--tt-text);
}

/* ── 하단 액션 ── */
.violation-page__actions {
    flex: none;
    padding: var(--tt-space-3) var(--tt-screen-padding) 18px;
    background: var(--tt-bg-subtle);
}

.violation-page__btn-row {
    display: flex;
    gap: var(--tt-card-gap);
}

.violation-page__btn-defense {
    flex: 1.25;
}

.violation-page__btn-admit {
    flex: 1;
    border: 1.5px solid var(--tt-danger);
    color: var(--tt-danger-deep);
    background: var(--tt-bg);
    font-family: var(--tt-font-sans);
    font-weight: var(--tt-fw-black);
    font-size: 14px;
    padding: 13px 0;
    border-radius: var(--tt-radius-md);
    text-align: center;
    cursor: pointer;
}

/* ── 05a 혐의 인정 확인 바텀시트 ── */
.admit-sheet {
    display: flex;
    flex-direction: column;
    gap: 13px;
}

.admit-sheet__top {
    display: flex;
    align-items: center;
    gap: var(--tt-space-3);
}

.admit-sheet__mascot {
    width: 56px;
    height: 56px;
    object-fit: contain;
    flex: none;
}

.admit-sheet__warn-badge {
    display: inline-block;
    background: var(--tt-danger-subtle);
    color: var(--tt-danger-deep);
    font-size: var(--tt-fs-overline);
    font-weight: var(--tt-fw-black);
    padding: 4px 10px;
    border-radius: var(--tt-radius-full);
}

.admit-sheet__headline {
    font-size: 17px;
    font-weight: var(--tt-fw-black);
    line-height: 1.35;
    margin-top: 6px;
}

/* ── 목숨 차감 정보 카드 ── */
.admit-sheet__info-card {
    background: var(--tt-danger-subtle);
    border: 1px solid #F3D3C9;
    border-radius: var(--tt-radius-lg);
    padding: 13px 15px;
}

.admit-sheet__info-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
}

.admit-sheet__info-label {
    font-size: var(--tt-fs-badge);
    font-weight: var(--tt-fw-black);
    color: var(--tt-danger-deep);
}

.admit-sheet__info-value {
    font-size: var(--tt-fs-badge);
    font-weight: var(--tt-fw-black);
    color: var(--tt-danger-deep);
    font-family: var(--tt-font-mono);
}

.admit-sheet__info-desc {
    font-size: var(--tt-fs-badge);
    color: var(--tt-text-body);
    line-height: 1.55;
    margin-top: 9px;
}

.admit-sheet__info-strong {
    color: var(--tt-danger-deep);
}

/* ── 바텀시트 푸터 ── */
.admit-sheet__footer {
    display: flex;
    flex-direction: column;
    gap: 9px;
    width: 100%;
}

.admit-sheet__btn-confirm {
    width: 100%;
    border: 1.5px solid var(--tt-danger);
    color: var(--tt-danger-deep);
    background: var(--tt-bg);
    font-family: var(--tt-font-sans);
    font-weight: var(--tt-fw-black);
    font-size: 14px;
    padding: 13px 0;
    border-radius: var(--tt-radius-md);
    text-align: center;
    cursor: pointer;
}

.admit-sheet__btn-confirm:disabled {
    opacity: 0.5;
    cursor: default;
}

.admit-sheet__hint {
    text-align: center;
    font-size: var(--tt-fs-overline);
    color: var(--tt-text-hint);
    font-weight: var(--tt-fw-bold);
}
</style>
