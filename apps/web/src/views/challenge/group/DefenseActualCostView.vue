<!--
  02a · 실제 부담금 입력 (F 입력형) — GC_07_02
  기소 거래의 실제 개인 부담금을 커스텀 넘패드로 입력한다.
  확인 → 재계산 → 02b(여전히 초과) / 02c(기준 내) 바텀시트.
  02b 에서 혐의 인정 선택 시 05a 확인 시트를 거친다.
-->
<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue';
import { useRouter, useRoute } from 'vue-router';

import DefenseCourtHeader from '@/components/challenge/group/DefenseCourtHeader.vue';
import BaseBottomSheet from '@/components/common/BaseBottomSheet.vue';
import BaseButton from '@/components/common/BaseButton.vue';

import mascotAnxious from '@/assets/images/emotions/17_anxious.png';
import mascotWorried from '@/assets/images/emotions/16_worried.png';
import mascotOkay from '@/assets/images/emotions/41_okay.png';
import mascotApology from '@/assets/images/emotions/43_apology.png';
import lifeAlive from '@/assets/images/challenge_live/gavel-alive.png';
import lifeDepleted from '@/assets/images/challenge_live/gavel-depleted.png';

const router = useRouter();
const route = useRoute();

/* ── mock 데이터 (API 연동 전) ── */
const indictment = ref({
    id: route.params.indictmentId || 'IND-001',
    challengeName: '배달 소비 줄이기',
    evalType: 'DAILY',
    limitAmount: 25000,
    currentAmount: 37400,
    exceededAmount: 12400,
    transaction: {
        merchantName: '배달의민족',
        amount: 24000,
        time: '오늘 18:42',
        category: '음식/배달',
        paymentMethod: '카드 결제',
    },
    defenseDeadlineLabel: '오늘 22:00',
    lives: { current: 5, total: 5 },
});

/* ── 마감 타이머 ── */
const remainingSeconds = ref(8043); // 02:14:03
let timerHandle = null;

const timerLabel = computed(() => {
    const h = String(Math.floor(remainingSeconds.value / 3600)).padStart(2, '0');
    const m = String(Math.floor((remainingSeconds.value % 3600) / 60)).padStart(2, '0');
    const s = String(remainingSeconds.value % 60).padStart(2, '0');
    return `마감 ${h}:${m}:${s}`;
});

function onKeydown(e) {
    if (showOverSheet.value || showSafeSheet.value || showAdmitSheet.value) return;
    if (e.key >= '0' && e.key <= '9') {
        pressKey(e.key);
    } else if (e.key === 'Backspace') {
        pressKey('backspace');
    } else if (e.key === 'Enter' && inputValue.value) {
        onConfirm();
    }
}

onMounted(() => {
    timerHandle = setInterval(() => {
        if (remainingSeconds.value > 0) remainingSeconds.value--;
    }, 1000);
    document.addEventListener('keydown', onKeydown);
});

onBeforeUnmount(() => {
    clearInterval(timerHandle);
    document.removeEventListener('keydown', onKeydown);
});

/* ── 넘패드 입력 ── */
const inputValue = ref('');
const NUMPAD_KEYS = ['1', '2', '3', '4', '5', '6', '7', '8', '9', '000', '0', 'backspace'];

const displayValue = computed(() => {
    if (!inputValue.value) return '';
    return Number(inputValue.value).toLocaleString();
});

const inputAmount = computed(() => {
    return inputValue.value ? Number(inputValue.value) : 0;
});

function pressKey(key) {
    if (key === 'backspace') {
        inputValue.value = inputValue.value.slice(0, -1);
        return;
    }
    if (inputValue.value.length >= 8) return;
    if (!inputValue.value && (key === '0' || key === '000')) return;
    if (inputValue.value.length + key.length > 8) return;
    inputValue.value += key;
}

/* ── 부담금 반영 결과 계산 ── */
const adjustedTotal = computed(() => {
    const base = indictment.value.currentAmount - indictment.value.transaction.amount;
    return base + inputAmount.value;
});

const isStillOver = computed(() => adjustedTotal.value > indictment.value.limitAmount);
const adjustedExceeded = computed(() => adjustedTotal.value - indictment.value.limitAmount);
const adjustedMargin = computed(() => indictment.value.limitAmount - adjustedTotal.value);

/* ── 바텀시트 상태 ── */
const showOverSheet = ref(false);
const showSafeSheet = ref(false);
const showAdmitSheet = ref(false);
const overSheetRef = ref(null);
const safeSheetRef = ref(null);
const admitSheetRef = ref(null);

function onConfirm() {
    if (!inputValue.value) return;
    if (isStillOver.value) {
        showOverSheet.value = true;
    } else {
        showSafeSheet.value = true;
    }
}

function goToDefenseWrite() {
    /* useOverlay 가 쌓은 히스토리 소유권을 넘긴 뒤 replace 로 이동해야
       history.back() 과 경쟁하지 않는다. */
    overSheetRef.value?.releaseHistory();
    safeSheetRef.value?.releaseHistory();
    showOverSheet.value = false;
    showSafeSheet.value = false;
    router.replace({
        name: 'defenseWrite',
        params: {
            id: route.params.id,
            indictmentId: route.params.indictmentId,
        },
    });
}

function openAdmitFromOver() {
    overSheetRef.value?.releaseHistory();
    showOverSheet.value = false;
    showAdmitSheet.value = true;
}

const livesAfterAdmit = computed(() => indictment.value.lives.current - 1);

function confirmAdmit() {
    admitSheetRef.value?.releaseHistory();
    showAdmitSheet.value = false;
    router.replace({
        name: 'defenseAdmitDone',
        params: {
            id: route.params.id,
            indictmentId: route.params.indictmentId,
        },
    });
}

function cancelAdmit() {
    showAdmitSheet.value = false;
}

const merchantInitial = computed(() => indictment.value.transaction.merchantName.charAt(0));
</script>

<template>
    <div class="cost-page">
        <!-- ── Court Bar 헤더 ── -->
        <DefenseCourtHeader>
            <template #nav-right>
                <span class="cost-page__timer">{{ timerLabel }}</span>
            </template>

            <div class="cost-page__header-body">
                <h2 class="cost-page__headline">
                    실제로 부담한<br>금액을 알려주세요
                </h2>
                <img
                    :src="mascotAnxious"
                    alt="탕이"
                    class="cost-page__header-mascot"
                >
            </div>
        </DefenseCourtHeader>

        <!-- ── 본문 카드 영역 ── -->
        <div class="cost-page__content">
            <!-- 초과 요약 카드 -->
            <div class="cost-page__exceeded-card">
                <div class="cost-page__exceeded-label">일일결산 기준 초과</div>
                <div class="cost-page__exceeded-row">
                    <div class="cost-page__exceeded-amount">
                        <span class="cost-page__exceeded-num">
                            {{ indictment.exceededAmount.toLocaleString() }}
                        </span>
                        <span class="cost-page__exceeded-unit">원 초과</span>
                    </div>
                    <span class="cost-page__exceeded-ratio">
                        {{ indictment.currentAmount.toLocaleString() }}
                        / {{ indictment.limitAmount.toLocaleString() }}
                    </span>
                </div>
            </div>

            <!-- 기소 발생 거래 카드 -->
            <div class="cost-page__tx-card">
                <div class="cost-page__tx-header">
                    <span class="cost-page__tx-label">기소 발생 거래</span>
                    <span class="cost-page__tx-time">{{ indictment.transaction.time }}</span>
                </div>
                <div class="cost-page__tx-detail">
                    <div class="cost-page__tx-icon">{{ merchantInitial }}</div>
                    <div class="cost-page__tx-info">
                        <div class="cost-page__tx-name">
                            {{ indictment.transaction.merchantName }}
                        </div>
                        <div class="cost-page__tx-meta">
                            {{ indictment.transaction.paymentMethod }} · {{ indictment.transaction.category }}
                        </div>
                    </div>
                    <div class="cost-page__tx-amount">
                        {{ indictment.transaction.amount.toLocaleString() }}원
                    </div>
                </div>
            </div>

            <!-- 실제 부담금 입력 -->
            <div class="cost-page__input-section">
                <div class="cost-page__input-header">
                    <span class="cost-page__input-label">실제 개인 부담금</span>
                    <span class="cost-page__input-hint">무죄 판결 시 반영돼요</span>
                </div>
                <div class="cost-page__input-field">
                    <div class="cost-page__input-value-wrap">
                        <span v-if="displayValue" class="cost-page__input-num">
                            {{ displayValue }}
                        </span>
                        <span class="cost-page__cursor"></span>
                    </div>
                    <span class="cost-page__input-unit">원</span>
                </div>
                <p class="cost-page__input-helper">
                    회식비 일괄 결제·이체 정산이라면
                    <b>받을 금액을 뺀 내 몫</b>만 적어주세요.
                </p>
            </div>
        </div>

        <!-- ── 커스텀 넘패드 + 확인 ── -->
        <div class="cost-page__bottom">
            <div class="cost-page__numpad">
                <button
                    v-for="key in NUMPAD_KEYS"
                    :key="key"
                    type="button"
                    :class="[
                        'cost-page__key',
                        { 'cost-page__key--small': key === '000' },
                        { 'cost-page__key--back': key === 'backspace' },
                    ]"
                    @click="pressKey(key)"
                >
                    <template v-if="key === 'backspace'">
                        <svg
                            width="20"
                            height="14"
                            viewBox="0 0 20 14"
                            fill="none"
                            aria-label="지우기"
                        >
                            <path
                                d="M6.5 1h11a1.5 1.5 0 011.5 1.5v9A1.5 1.5 0 0117.5 13h-11L1 7l5.5-6z"
                                stroke="#5A6076"
                                stroke-width="1.4"
                                stroke-linejoin="round"
                            />
                            <path
                                d="M9.5 5l5 4M14.5 5l-5 4"
                                stroke="#5A6076"
                                stroke-width="1.4"
                                stroke-linecap="round"
                            />
                        </svg>
                    </template>
                    <template v-else>{{ key }}</template>
                </button>
            </div>

            <div class="cost-page__confirm-wrap">
                <BaseButton
                    variant="dark"
                    size="lg"
                    block
                    :disabled="!inputValue"
                    @click="onConfirm"
                >
                    확인
                </BaseButton>
            </div>
        </div>

        <!-- ══════ 02b · 부담금 반영 후에도 초과 ══════ -->
        <BaseBottomSheet ref="overSheetRef" v-model="showOverSheet" close-on-overlay close-on-esc>
            <div class="over-sheet">
                <div class="over-sheet__top">
                    <img :src="mascotWorried" alt="탕이" class="over-sheet__mascot">
                    <div>
                        <span class="over-sheet__badge">아직 초과</span>
                        <div class="over-sheet__headline">
                            실제 부담금으로도<br>기준을 넘었어요
                        </div>
                    </div>
                </div>
                <div class="over-sheet__info-card">
                    <div class="over-sheet__info-row">
                        <div class="over-sheet__info-amount">
                            <span class="over-sheet__info-num">
                                {{ adjustedExceeded.toLocaleString() }}
                            </span>
                            <span class="over-sheet__info-unit">원 초과</span>
                        </div>
                        <span class="over-sheet__info-ratio">
                            {{ adjustedTotal.toLocaleString() }}
                            / {{ indictment.limitAmount.toLocaleString() }}
                        </span>
                    </div>
                    <p class="over-sheet__info-desc">
                        부담금 {{ inputAmount.toLocaleString() }}원을 반영해도
                        하루 기준보다 많아요. 사정이 있다면 변론으로 설명할 수 있어요.
                    </p>
                </div>
            </div>

            <template #footer>
                <div class="over-sheet__footer">
                    <BaseButton variant="dark" size="lg" block @click="goToDefenseWrite">
                        그래도 변론 작성하기
                    </BaseButton>
                    <button
                        type="button"
                        class="over-sheet__btn-admit"
                        @click="openAdmitFromOver"
                    >
                        혐의 인정하기
                    </button>
                    <p class="over-sheet__notice">
                        혐의를 인정하면 목숨 1개가 바로 차감돼요.
                    </p>
                </div>
            </template>
        </BaseBottomSheet>

        <!-- ══════ 02c · 부담금 반영 시 기준 내 ══════ -->
        <BaseBottomSheet ref="safeSheetRef" v-model="showSafeSheet" close-on-overlay close-on-esc>
            <div class="safe-sheet">
                <div class="safe-sheet__mascot-wrap">
                    <div class="safe-sheet__glow"></div>
                    <img :src="mascotOkay" alt="탕이" class="safe-sheet__mascot">
                </div>
                <span class="safe-sheet__badge">기준 내 소비</span>
                <div class="safe-sheet__text">
                    <div class="safe-sheet__headline">
                        실제 부담금이면<br>초과가 아니에요
                    </div>
                    <p class="safe-sheet__sub">
                        이제 변론과 증빙으로 그룹원에게 설명하면 돼요.
                    </p>
                </div>
                <div class="safe-sheet__info-card">
                    <div class="safe-sheet__info-amount">
                        <span class="safe-sheet__info-num">
                            {{ adjustedMargin.toLocaleString() }}
                        </span>
                        <span class="safe-sheet__info-unit">원 여유</span>
                    </div>
                    <span class="safe-sheet__info-ratio">
                        {{ adjustedTotal.toLocaleString() }}
                        / {{ indictment.limitAmount.toLocaleString() }}
                    </span>
                </div>
            </div>

            <template #footer>
                <div class="safe-sheet__footer">
                    <BaseButton variant="dark" size="lg" block @click="goToDefenseWrite">
                        다음으로
                    </BaseButton>
                    <p class="safe-sheet__notice">
                        최종 판단은 그룹원 투표로 결정돼요.
                    </p>
                </div>
            </template>
        </BaseBottomSheet>

        <!-- ══════ 05a · 혐의 인정 확인 ══════ -->
        <BaseBottomSheet ref="admitSheetRef" v-model="showAdmitSheet" close-on-overlay close-on-esc>
            <div class="admit-sheet">
                <div class="admit-sheet__top">
                    <img :src="mascotApology" alt="탕이" class="admit-sheet__mascot">
                    <div class="admit-sheet__top-text">
                        <span class="admit-sheet__warn-badge">되돌릴 수 없어요</span>
                        <div class="admit-sheet__headline">
                            혐의를 인정하면<br>바로 유죄로 종결돼요
                        </div>
                    </div>
                </div>
                <div class="admit-sheet__info-card">
                    <div class="admit-sheet__info-header">
                        <span class="admit-sheet__info-label">차감 후 남은 목숨</span>
                        <span class="admit-sheet__info-value">
                            {{ indictment.lives.current }} → {{ livesAfterAdmit }}
                        </span>
                    </div>
                    <div class="admit-sheet__lives">
                        <img
                            v-for="n in livesAfterAdmit"
                            :key="'alive-' + n"
                            :src="lifeAlive"
                            alt="남은 목숨"
                            class="admit-sheet__life"
                        >
                        <img
                            :src="lifeDepleted"
                            alt="차감된 목숨"
                            class="admit-sheet__life admit-sheet__life--depleted"
                        >
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
                    <BaseButton variant="primary" size="lg" block @click="cancelAdmit">
                        아니요, 변론할게요
                    </BaseButton>
                    <button
                        type="button"
                        class="admit-sheet__btn-confirm"
                        @click="confirmAdmit"
                    >
                        네, 혐의를 인정할게요
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
.cost-page {
    display: flex;
    flex-direction: column;
    min-height: 100vh;
    min-height: 100dvh;
    background: var(--tt-bg-subtle);
}

/* ── 헤더 타이머 뱃지 ── */
.cost-page__timer {
    background: rgba(224, 102, 75, 0.2);
    color: #FF9E86;
    font-size: 11.5px;
    font-weight: var(--tt-fw-black);
    padding: 5px 12px;
    border-radius: var(--tt-radius-full);
    font-family: var(--tt-font-mono);
}

/* ── 헤더 본문 ── */
.cost-page__header-body {
    display: flex;
    align-items: flex-end;
    justify-content: space-between;
    gap: 6px;
}

.cost-page__headline {
    font-size: 21px;
    font-weight: var(--tt-fw-black);
    color: var(--tt-white);
    letter-spacing: -0.01em;
    line-height: 1.3;
}

.cost-page__header-mascot {
    width: 68px;
    height: 68px;
    object-fit: contain;
    margin-bottom: -6px;
    flex: none;
}

/* ── 본문 카드 영역 ── */
.cost-page__content {
    flex: 1;
    min-height: 0;
    overflow: hidden;
    padding: 13px var(--tt-screen-padding) 0;
    display: flex;
    flex-direction: column;
    gap: 10px;
}

/* ── 초과 요약 카드 ── */
.cost-page__exceeded-card {
    flex: none;
    background: var(--tt-bg);
    border: 1px solid #F3D3C9;
    border-radius: var(--tt-radius-xl);
    padding: 12px 15px;
    box-shadow: 0 8px 22px rgba(224, 102, 75, 0.07);
}

.cost-page__exceeded-label {
    font-size: 11.5px;
    color: var(--tt-text-muted);
    font-weight: var(--tt-fw-bold);
}

.cost-page__exceeded-row {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
    margin-top: 5px;
}

.cost-page__exceeded-amount {
    display: flex;
    align-items: baseline;
    gap: 3px;
}

.cost-page__exceeded-num {
    font-size: 22px;
    font-weight: var(--tt-fw-black);
    color: var(--tt-danger-deep);
    letter-spacing: -0.01em;
}

.cost-page__exceeded-unit {
    font-size: 14px;
    font-weight: var(--tt-fw-black);
    color: var(--tt-danger-deep);
}

.cost-page__exceeded-ratio {
    font-size: var(--tt-fs-overline);
    color: var(--tt-text-muted);
    font-weight: var(--tt-fw-bold);
    font-family: var(--tt-font-mono);
}

/* ── 기소 발생 거래 카드 ── */
.cost-page__tx-card {
    flex: none;
    background: var(--tt-bg);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-xl);
    padding: 12px 15px;
    box-shadow: var(--tt-elevation-2);
}

.cost-page__tx-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
}

.cost-page__tx-label {
    font-size: 11.5px;
    color: var(--tt-text-muted);
    font-weight: var(--tt-fw-bold);
}

.cost-page__tx-time {
    font-size: var(--tt-fs-overline);
    color: var(--tt-text-hint);
    font-weight: var(--tt-fw-bold);
}

.cost-page__tx-detail {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-top: 8px;
}

.cost-page__tx-icon {
    width: 34px;
    height: 34px;
    border-radius: 10px;
    background: var(--tt-bg-fill);
    border: 1px solid var(--tt-border);
    flex: none;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: var(--tt-fs-overline);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text-muted);
}

.cost-page__tx-info {
    flex: 1;
    min-width: 0;
}

.cost-page__tx-name {
    font-size: 14px;
    font-weight: var(--tt-fw-black);
}

.cost-page__tx-meta {
    font-size: var(--tt-fs-overline);
    color: var(--tt-text-hint);
    font-weight: var(--tt-fw-bold);
    margin-top: 2px;
}

.cost-page__tx-amount {
    font-size: 15px;
    font-weight: var(--tt-fw-black);
    flex: none;
}

/* ── 실제 부담금 입력 ── */
.cost-page__input-section {
    flex: none;
}

.cost-page__input-header {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
    margin-bottom: 7px;
}

.cost-page__input-label {
    font-size: 13px;
    font-weight: var(--tt-fw-black);
}

.cost-page__input-hint {
    font-size: var(--tt-fs-overline);
    color: var(--tt-text-muted);
    font-weight: var(--tt-fw-bold);
}

.cost-page__input-field {
    background: var(--tt-bg);
    border: 1.5px solid var(--tt-surface-inverse);
    border-radius: var(--tt-radius-xl);
    padding: 14px 15px;
    box-shadow: var(--tt-elevation-2);
    display: flex;
    align-items: baseline;
    justify-content: space-between;
}

.cost-page__input-value-wrap {
    display: flex;
    align-items: center;
    gap: 2px;
}

.cost-page__input-num {
    font-size: 22px;
    font-weight: var(--tt-fw-black);
    color: var(--tt-surface-inverse);
    letter-spacing: -0.01em;
}

.cost-page__cursor {
    width: 1.5px;
    height: 20px;
    background: var(--tt-surface-inverse);
    margin-left: 2px;
    animation: cursor-blink 1s step-end infinite;
}

@keyframes cursor-blink {
    0%, 100% { opacity: 1; }
    50% { opacity: 0; }
}

.cost-page__input-unit {
    font-size: 13px;
    font-weight: var(--tt-fw-black);
    color: var(--tt-text-muted);
}

.cost-page__input-helper {
    font-size: var(--tt-fs-overline);
    color: var(--tt-text-muted);
    line-height: 1.5;
    margin-top: 7px;
}

.cost-page__input-helper b {
    color: var(--tt-text-body);
}

/* ── 넘패드 + 확인 영역 ── */
.cost-page__bottom {
    flex: none;
    background: var(--tt-bg-fill);
    border-top: 1px solid #E2E4EA;
}

.cost-page__numpad {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 6px;
    padding: 10px 8px 0;
}

.cost-page__key {
    background: var(--tt-bg);
    border: none;
    border-radius: 9px;
    padding: 7px 0;
    text-align: center;
    font-size: 19px;
    font-weight: var(--tt-fw-semibold);
    color: var(--tt-surface-inverse);
    font-family: var(--tt-font-sans);
    box-shadow: 0 1px 0 rgba(35, 40, 66, 0.12);
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    -webkit-tap-highlight-color: transparent;
    transition: background 0.1s ease;
}

.cost-page__key:active {
    background: var(--tt-border);
}

.cost-page__key--small {
    font-size: 14px;
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-body);
}

.cost-page__key--back {
    background: #DDE0E7;
    box-shadow: none;
}

.cost-page__key--back:active {
    background: #CDD1DA;
}

.cost-page__confirm-wrap {
    padding: 8px var(--tt-screen-padding) 12px;
}

/* ══════════════════════════════════════════════
   02b · 부담금 반영 후에도 초과 바텀시트
   ══════════════════════════════════════════════ */
.over-sheet {
    display: flex;
    flex-direction: column;
    gap: 13px;
}

.over-sheet__top {
    display: flex;
    align-items: center;
    gap: 12px;
}

.over-sheet__mascot {
    width: 56px;
    height: 56px;
    object-fit: contain;
    flex: none;
}

.over-sheet__badge {
    display: inline-block;
    background: var(--tt-danger-subtle);
    color: var(--tt-danger-deep);
    font-size: var(--tt-fs-overline);
    font-weight: var(--tt-fw-black);
    padding: 4px 10px;
    border-radius: var(--tt-radius-full);
}

.over-sheet__headline {
    font-size: 17px;
    font-weight: var(--tt-fw-black);
    line-height: 1.35;
    margin-top: 6px;
}

.over-sheet__info-card {
    background: var(--tt-danger-subtle);
    border: 1px solid #F3D3C9;
    border-radius: var(--tt-radius-xl);
    padding: 13px 15px;
}

.over-sheet__info-row {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
}

.over-sheet__info-amount {
    display: flex;
    align-items: baseline;
    gap: 3px;
}

.over-sheet__info-num {
    font-size: 21px;
    font-weight: var(--tt-fw-black);
    color: var(--tt-danger-deep);
}

.over-sheet__info-unit {
    font-size: 13px;
    font-weight: var(--tt-fw-black);
    color: var(--tt-danger-deep);
}

.over-sheet__info-ratio {
    font-size: var(--tt-fs-overline);
    color: var(--tt-text-muted);
    font-weight: var(--tt-fw-bold);
    font-family: var(--tt-font-mono);
}

.over-sheet__info-desc {
    font-size: 11.5px;
    color: var(--tt-text-body);
    line-height: 1.55;
    margin-top: 8px;
}

.over-sheet__footer {
    display: flex;
    flex-direction: column;
    gap: 9px;
    width: 100%;
}

.over-sheet__btn-admit {
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

.over-sheet__notice {
    text-align: center;
    font-size: var(--tt-fs-overline);
    color: var(--tt-text-hint);
    font-weight: var(--tt-fw-bold);
}

/* ══════════════════════════════════════════════
   02c · 부담금 반영 시 기준 내 바텀시트
   ══════════════════════════════════════════════ */
.safe-sheet {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 13px;
}

.safe-sheet__mascot-wrap {
    position: relative;
    width: 96px;
    height: 96px;
    display: flex;
    align-items: center;
    justify-content: center;
}

.safe-sheet__glow {
    position: absolute;
    inset: 0;
    border-radius: 50%;
    background: radial-gradient(circle, rgba(46, 158, 107, 0.2) 0%, rgba(46, 158, 107, 0) 70%);
}

.safe-sheet__mascot {
    position: relative;
    width: 80px;
    height: 80px;
    object-fit: contain;
    filter: drop-shadow(0 8px 14px rgba(35, 40, 66, 0.16));
}

.safe-sheet__badge {
    background: #E4F4EC;
    color: #2E9E6B;
    font-size: var(--tt-fs-overline);
    font-weight: var(--tt-fw-black);
    padding: 5px 12px;
    border-radius: var(--tt-radius-full);
}

.safe-sheet__text {
    text-align: center;
}

.safe-sheet__headline {
    font-size: 19px;
    font-weight: var(--tt-fw-black);
    line-height: 1.35;
}

.safe-sheet__sub {
    font-size: 12.5px;
    color: var(--tt-text-muted);
    font-weight: var(--tt-fw-semibold);
    line-height: 1.55;
    margin-top: 8px;
}

.safe-sheet__info-card {
    width: 100%;
    background: #E4F4EC;
    border: 1px solid #C3E5D4;
    border-radius: var(--tt-radius-xl);
    padding: 13px 15px;
    display: flex;
    align-items: baseline;
    justify-content: space-between;
}

.safe-sheet__info-amount {
    display: flex;
    align-items: baseline;
    gap: 3px;
}

.safe-sheet__info-num {
    font-size: 20px;
    font-weight: var(--tt-fw-black);
    color: #2E9E6B;
}

.safe-sheet__info-unit {
    font-size: 13px;
    font-weight: var(--tt-fw-black);
    color: #2E9E6B;
}

.safe-sheet__info-ratio {
    font-size: var(--tt-fs-overline);
    color: #5A8A73;
    font-weight: var(--tt-fw-bold);
    font-family: var(--tt-font-mono);
}

.safe-sheet__footer {
    display: flex;
    flex-direction: column;
    gap: 9px;
    width: 100%;
}

.safe-sheet__notice {
    text-align: center;
    font-size: var(--tt-fs-overline);
    color: var(--tt-text-hint);
    font-weight: var(--tt-fw-bold);
}

/* ══════════════════════════════════════════════
   05a · 혐의 인정 확인 바텀시트
   ══════════════════════════════════════════════ */
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

.admit-sheet__lives {
    display: flex;
    gap: 6px;
    margin-top: 10px;
}

.admit-sheet__life {
    width: 24px;
    height: 24px;
    object-fit: contain;
}

.admit-sheet__life--depleted {
    opacity: 0.9;
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

.admit-sheet__hint {
    text-align: center;
    font-size: var(--tt-fs-overline);
    color: var(--tt-text-hint);
    font-weight: var(--tt-fw-bold);
}
</style>
