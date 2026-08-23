<!--
  02a · 실제 부담금 입력 (F 입력형) — GC_07_02
  결산 구간 전체 거래를 날짜별로 보여주고 **건별로** 실제 개인 부담금을 고친다.
  확인 → 합계 재계산 → 02b(여전히 초과) / 02c(기준 내) 바텀시트.
  02b 에서 혐의 인정 선택 시 05a 확인 시트를 거친다.

  ⚠ 「기소 발생 거래 1건」 모델을 쓰지 않는다. 한도 초과는 **누적**으로 판정되므로 기소를
  발화시킨 거래가 문제의 거래라는 보장이 없다 — 친구들 몫까지 대납한 큰 거래는 그 시점에
  한도 미달이라 기소를 만들지 않고, 뒤따르는 평범한 소비가 기소를 만든다. 한 건만 고칠 수
  있으면 정작 대납한 금액을 신고할 방법이 없다.
-->
<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue';
import { useRouter, useRoute } from 'vue-router';

import DefenseCourtHeader from '@/components/challenge/group/DefenseCourtHeader.vue';
import BaseBottomSheet from '@/components/common/BaseBottomSheet.vue';
import BaseButton from '@/components/common/BaseButton.vue';
import {
    confessIndictment,
    fetchTrialDetail,
    fetchTrialTransactions,
} from '@/api/groupChallenge';

import mascotAnxious from '@/assets/images/emotions/17_anxious.png';
import mascotWorried from '@/assets/images/emotions/16_worried.png';
import mascotOkay from '@/assets/images/emotions/41_okay.png';
import mascotApology from '@/assets/images/emotions/43_apology.png';

const router = useRouter();
const route = useRoute();

const indictment = ref(null);
const settlement = ref(null);

/**
 * 건별 실제 부담금 — `{ [transactionId]: number }`.
 *
 * 기본값은 **결제 금액 그대로**다. 0 으로 두면 아무것도 고치지 않은 사람이 전액을 정산받았다고
 * 신고한 셈이 되어 합계가 0 원이 된다.
 */
const burdens = ref({});

onMounted(async () => {
    try {
        /* 상세와 거래 목록은 서로를 기다릴 이유가 없다. 순차로 부르면 진입이 두 배로 느려진다. */
        const [detail, transactions] = await Promise.all([
            fetchTrialDetail(route.params.indictmentId),
            fetchTrialTransactions(route.params.indictmentId),
        ]);
        indictment.value = detail;
        settlement.value = transactions;
        burdens.value = Object.fromEntries(
            allTransactions().map((tx) => [tx.transactionId, tx.amount]),
        );
    } catch (e) {
        console.error('[DefenseActualCost] 결산 구간 거래를 불러오지 못해 기소 안내로 되돌린다.', e);
        router.replace({
            name: 'defenseViolation',
            params: { id: route.params.id, indictmentId: route.params.indictmentId },
        });
        return;
    }
    startTimer();
    document.addEventListener('keydown', onKeydown);
});

/** 날짜 묶음을 평평하게 — 합계·초기화가 날짜를 신경 쓸 이유가 없다. */
function allTransactions() {
    return (settlement.value?.days ?? []).flatMap((day) => day.transactions ?? []);
}

/* ── 마감 타이머 ──
 * 남은 초를 감소시키지 않고 **절대시각과 지금을 매초 다시 비교한다.** 진입 시점의 남은 초를
 * 줄여 나가면 화면을 열어 둔 채 절전에 들어갔다 돌아왔을 때 값이 그 시점에 굳는다. */
const now = ref(Date.now());
let timerHandle = null;

function startTimer() {
    timerHandle = setInterval(() => {
        now.value = Date.now();
    }, 1000);
}

onBeforeUnmount(() => {
    clearInterval(timerHandle);
    document.removeEventListener('keydown', onKeydown);
});

const timerLabel = computed(() => {
    const deadline = new Date(indictment.value?.defenseDeadline ?? '').getTime();
    if (Number.isNaN(deadline)) return '';

    const left = Math.max(0, deadline - now.value);
    const pad = (n) => String(n).padStart(2, '0');
    return `마감 ${pad(Math.floor(left / 3600000))}:${pad(Math.floor(left / 60000) % 60)}:${pad(Math.floor(left / 1000) % 60)}`;
});

/* ── 표시 헬퍼 ── */
const isDaily = computed(() => indictment.value?.evalType === 'DAILY');
const limitScopeLabel = computed(() => (isDaily.value ? '하루 기준' : '기간 기준'));

function initialOf(name) {
    return name ? name.charAt(0) : '?';
}

/** 시각·결제수단·카테고리 중 있는 것만 이어 붙인다. `tr_time` 과 분류는 NULL 일 수 있다. */
function metaOf(tx) {
    return [tx.timeLabel, tx.paymentMethod, tx.categoryName].filter(Boolean).join(' · ');
}

function burdenOf(tx) {
    return burdens.value[tx.transactionId] ?? tx.amount;
}

function isEditedRow(tx) {
    return burdenOf(tx) !== tx.amount;
}

/* ── 부담금 반영 결과 계산 ──
 * 합계는 **건별 입력의 합**이다. 초과액에서 한 건만 빼고 더하는 방식이 아니다. */
const limitAmount = computed(() => indictment.value?.limitAmount ?? 0);
const adjustedTotal = computed(() =>
    allTransactions().reduce((sum, tx) => sum + burdenOf(tx), 0),
);

const isStillOver = computed(() => adjustedTotal.value > limitAmount.value);
const adjustedExceeded = computed(() => adjustedTotal.value - limitAmount.value);
const adjustedMargin = computed(() => limitAmount.value - adjustedTotal.value);

/* ── 건별 넘패드 (바텀시트) ──
 * 오버레이·스크롤 잠금·뒤로가기 처리를 각자 만들지 않기 위해 `BaseBottomSheet` 를 쓴다. */
const showKeypadSheet = ref(false);
const editingId = ref(null);
const inputValue = ref('');
const NUMPAD_KEYS = ['1', '2', '3', '4', '5', '6', '7', '8', '9', '000', '0', 'backspace'];

const editingTx = computed(
    () => allTransactions().find((tx) => tx.transactionId === editingId.value) ?? null,
);

const displayValue = computed(() => {
    if (!inputValue.value) return '';
    return Number(inputValue.value).toLocaleString();
});

const inputAmount = computed(() => (inputValue.value ? Number(inputValue.value) : 0));

/*
 * 결제 금액보다 큰 부담금은 성립하지 않는다 — 청구된 적 없는 돈을 냈다는 뜻이 된다.
 * 여기서 막아 두면 합계도 자연히 `currentAmount` 를 넘지 않아 서버의
 * `INVALID_BURDEN_AMOUNT` 를 보기 전에 걸러진다.
 */
const inputTooLarge = computed(
    () => !!editingTx.value && inputAmount.value > editingTx.value.amount,
);

function openKeypad(tx) {
    /* 환불 행은 고치지 않는다 — 돌려받은 돈에 「내가 실제로 부담한 금액」이 없다. */
    if (tx.isRefund) return;
    editingId.value = tx.transactionId;
    inputValue.value = String(burdenOf(tx));
    showKeypadSheet.value = true;
}

function applyKeypad() {
    if (inputTooLarge.value) return;
    burdens.value = { ...burdens.value, [editingId.value]: inputAmount.value };
    showKeypadSheet.value = false;
}

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

function onKeydown(e) {
    /* 결과 시트가 떠 있는 동안의 키 입력은 삼킨다 — 뒤에 있는 금액이 몰래 바뀌면 안 된다. */
    if (showOverSheet.value || showSafeSheet.value || showAdmitSheet.value) return;
    if (!showKeypadSheet.value) return;

    if (e.key >= '0' && e.key <= '9') {
        pressKey(e.key);
    } else if (e.key === 'Backspace') {
        pressKey('backspace');
    } else if (e.key === 'Enter') {
        applyKeypad();
    }
}

/* ── 바텀시트 상태 ── */
const showOverSheet = ref(false);
const showSafeSheet = ref(false);
const showAdmitSheet = ref(false);
const admitting = ref(false);

/*
 * 시트 내부에서 라우트를 이동할 때의 전략:
 *   1. 시트를 정상적으로 닫는다 (useOverlay 가 pushState 를 history.back() 으로 정리)
 *   2. back() 이 완료된 뒤(setTimeout) router.replace 를 호출한다.
 * 이렇게 하면 overlay 가 쌓은 히스토리가 깨끗이 제거되고,
 * replace 가 현재 라우트 항목(DAC)을 교체하므로 스택에 잔여 항목이 남지 않는다.
 */
const HISTORY_SETTLE_MS = 100;
const pendingNav = ref(null);

function onConfirm() {
    if (isStillOver.value) {
        showOverSheet.value = true;
    } else {
        showSafeSheet.value = true;
    }
}

function goToDefenseWrite() {
    pendingNav.value = 'defenseWrite';
    showOverSheet.value = false;
    showSafeSheet.value = false;
}

function openAdmitFromOver() {
    pendingNav.value = 'openAdmit';
    showOverSheet.value = false;
}

async function confirmAdmit() {
    if (admitting.value) return;
    admitting.value = true;
    try {
        await confessIndictment(route.params.indictmentId);
        pendingNav.value = 'defenseAdmitDone';
        showAdmitSheet.value = false;
    } catch (e) {
        console.error('[DefenseActualCost] 혐의 인정에 실패했다.', e);
        window.alert(e.message ?? '혐의 인정에 실패했어요. 잠시 후 다시 시도해주세요.');
    } finally {
        admitting.value = false;
    }
}

function cancelAdmit() {
    showAdmitSheet.value = false;
}

/* 시트가 닫힌 뒤 overlay 의 history.back() 이 처리될 시간을 두고 네비게이션 */
watch([showOverSheet, showSafeSheet, showAdmitSheet], () => {
    if (!pendingNav.value) return;
    if (showOverSheet.value || showSafeSheet.value || showAdmitSheet.value) return;

    const nav = pendingNav.value;
    pendingNav.value = null;

    setTimeout(() => {
        if (nav === 'openAdmit') {
            showAdmitSheet.value = true;
            return;
        }
        router.replace({
            name: nav,
            params: {
                id: route.params.id,
                indictmentId: route.params.indictmentId,
            },
            /*
             * 건별 입력은 순수 UI 이고 서버로는 합계만 간다. 그 합계를 스토어가 아니라
             * **쿼리로** 넘긴다 — 변론 작성 화면에서 새로고침해도 사용자가 건별로 매긴
             * 작업이 사라지지 않는다. 없으면(직접 URL 진입) 작성 화면이 소비액을 그대로 쓴다.
             */
            query: nav === 'defenseWrite' ? { burden: String(adjustedTotal.value) } : undefined,
        });
    }, HISTORY_SETTLE_MS);
});
</script>

<template>
    <div v-if="indictment && settlement" class="cost-page">
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
            <!-- 초과 요약 카드 — 기소 당시의 값(고정) -->
            <div class="cost-page__exceeded-card">
                <div class="cost-page__exceeded-label">
                    {{ indictment.evalLabel }} 기준 초과
                </div>
                <div class="cost-page__exceeded-row">
                    <div class="cost-page__exceeded-amount">
                        <span class="cost-page__exceeded-num">
                            {{ indictment.exceededAmount.toLocaleString() }}
                        </span>
                        <span class="cost-page__exceeded-unit">원 초과</span>
                    </div>
                    <span class="cost-page__exceeded-ratio">
                        {{ indictment.currentAmount.toLocaleString() }}
                        / {{ indictment.limitAmount.toLocaleString() }}원
                    </span>
                </div>
            </div>

            <!-- 실제 부담금 합계 — 아래 목록을 고칠 때마다 즉시 다시 계산된다 -->
            <div class="cost-page__total-card">
                <div class="cost-page__total-header">
                    <span class="cost-page__total-label">실제 부담금 합계</span>
                    <span
                        :class="[
                            'cost-page__total-badge',
                            isStillOver
                                ? 'cost-page__total-badge--over'
                                : 'cost-page__total-badge--safe',
                        ]"
                    >
                        {{ isStillOver
                            ? `${adjustedExceeded.toLocaleString()}원 초과`
                            : `${adjustedMargin.toLocaleString()}원 여유` }}
                    </span>
                </div>
                <div class="cost-page__total-row">
                    <span class="cost-page__total-num">
                        {{ adjustedTotal.toLocaleString() }}
                    </span>
                    <span class="cost-page__total-unit">
                        원 / {{ limitScopeLabel }} {{ limitAmount.toLocaleString() }}원
                    </span>
                </div>
                <p class="cost-page__total-helper">
                    회식비 일괄 결제·이체 정산이라면
                    <b>받을 금액을 뺀 내 몫</b>으로 고쳐주세요.
                </p>
            </div>

            <!-- 결산 구간 거래 목록 (날짜별) -->
            <div
                v-for="day in settlement.days"
                :key="day.date"
                class="cost-page__day"
            >
                <div class="cost-page__day-header">
                    <span class="cost-page__day-date">{{ day.dateLabel }}</span>
                    <span class="cost-page__day-amount">
                        {{ day.dailyAmount.toLocaleString() }}원
                    </span>
                </div>

                <button
                    v-for="tx in day.transactions"
                    :key="tx.transactionId"
                    type="button"
                    class="cost-page__row"
                    :disabled="tx.isRefund"
                    @click="openKeypad(tx)"
                >
                    <span class="cost-page__row-icon">
                        {{ initialOf(tx.merchantName) }}
                    </span>
                    <span class="cost-page__row-info">
                        <span class="cost-page__row-name">{{ tx.merchantName }}</span>
                        <span class="cost-page__row-meta">{{ metaOf(tx) }}</span>
                    </span>
                    <span class="cost-page__row-amounts">
                        <span
                            :class="[
                                'cost-page__row-amount',
                                { 'cost-page__row-amount--struck': isEditedRow(tx) },
                            ]"
                        >
                            {{ tx.amount.toLocaleString() }}원
                        </span>
                        <span v-if="isEditedRow(tx)" class="cost-page__row-burden">
                            내 몫 {{ burdenOf(tx).toLocaleString() }}원
                        </span>
                        <span v-else-if="tx.isRefund" class="cost-page__row-burden">
                            환불
                        </span>
                    </span>
                </button>
            </div>

            <p class="cost-page__footnote">
                건별 입력은 이 화면에서만 쓰이고, 서버로는 <b>합계만</b> 전달돼요.
            </p>
        </div>

        <!-- ── 확인 ── -->
        <div class="cost-page__bottom">
            <div class="cost-page__confirm-wrap">
                <BaseButton variant="dark" size="lg" block @click="onConfirm">
                    확인
                </BaseButton>
            </div>
        </div>

        <!-- ══════ 건별 실제 부담금 넘패드 ══════ -->
        <BaseBottomSheet v-model="showKeypadSheet" close-on-overlay close-on-esc>
            <div v-if="editingTx" class="keypad-sheet">
                <div class="keypad-sheet__header">
                    <span class="keypad-sheet__name">{{ editingTx.merchantName }}</span>
                    <span class="keypad-sheet__charged">
                        결제 {{ editingTx.amount.toLocaleString() }}원
                    </span>
                </div>

                <div class="keypad-sheet__field">
                    <div class="keypad-sheet__value-wrap">
                        <span v-if="displayValue" class="keypad-sheet__num">
                            {{ displayValue }}
                        </span>
                        <span class="keypad-sheet__cursor"></span>
                    </div>
                    <span class="keypad-sheet__unit">원</span>
                </div>

                <p
                    :class="[
                        'keypad-sheet__helper',
                        { 'keypad-sheet__helper--error': inputTooLarge },
                    ]"
                >
                    {{ inputTooLarge
                        ? '결제 금액보다 많이 부담할 수는 없어요.'
                        : '내가 실제로 부담한 금액만 적어주세요.' }}
                </p>

                <div class="keypad-sheet__numpad">
                    <button
                        v-for="key in NUMPAD_KEYS"
                        :key="key"
                        type="button"
                        :class="[
                            'keypad-sheet__key',
                            { 'keypad-sheet__key--small': key === '000' },
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
                                    stroke="currentColor"
                                    stroke-width="1.4"
                                    stroke-linejoin="round"
                                />
                                <path
                                    d="M9.5 5l5 4M14.5 5l-5 4"
                                    stroke="currentColor"
                                    stroke-width="1.4"
                                    stroke-linecap="round"
                                />
                            </svg>
                        </template>
                        <template v-else>{{ key }}</template>
                    </button>
                </div>
            </div>

            <template #footer>
                <BaseButton
                    variant="dark"
                    size="lg"
                    block
                    :disabled="inputTooLarge"
                    @click="applyKeypad"
                >
                    적용
                </BaseButton>
            </template>
        </BaseBottomSheet>

        <!-- ══════ 02b · 부담금 반영 후에도 초과 ══════ -->
        <BaseBottomSheet v-model="showOverSheet" close-on-overlay close-on-esc>
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
                            / {{ indictment.limitAmount.toLocaleString() }}원
                        </span>
                    </div>
                    <p class="over-sheet__info-desc">
                        건별 실제 부담금을 모두 반영해도
                        {{ limitScopeLabel }}보다 많아요. 사정이 있다면 변론으로 설명할 수 있어요.
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
                        혐의를 인정하면 투표 없이 유죄로 종결돼요.
                    </p>
                </div>
            </template>
        </BaseBottomSheet>

        <!-- ══════ 02c · 부담금 반영 시 기준 내 ══════ -->
        <BaseBottomSheet v-model="showSafeSheet" close-on-overlay close-on-esc>
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
                        / {{ indictment.limitAmount.toLocaleString() }}원
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
        <BaseBottomSheet v-model="showAdmitSheet" close-on-overlay close-on-esc>
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
                <!--
                  목숨 표시를 두지 않는다. 목숨 차감은 판결 확정(#172)이 담당하고 #170 은
                  건드리지 않으므로, 여기서 「5 → 4」 를 보여주면 아직 일어나지 않은 일을
                  단정하게 된다.
                -->
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
                        입력한 실제 부담금은 반영되지 않아요.
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

/* ── 본문 카드 영역 ──
 * 거래 목록은 길이를 알 수 없다(기간결산은 수십 건). `overflow: hidden` 이면 뒤쪽 거래를
 * 아예 만질 수 없으므로 스크롤을 연다. */
.cost-page__content {
    flex: 1;
    min-height: 0;
    overflow-y: auto;
    padding: 13px var(--tt-screen-padding) 16px;
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

/* ── 실제 부담금 합계 카드 ── */
.cost-page__total-card {
    flex: none;
    background: var(--tt-bg);
    border: 1.5px solid var(--tt-surface-inverse);
    border-radius: var(--tt-radius-xl);
    padding: 12px 15px;
    box-shadow: var(--tt-elevation-2);
}

.cost-page__total-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
}

.cost-page__total-label {
    font-size: 13px;
    font-weight: var(--tt-fw-black);
}

.cost-page__total-badge {
    font-size: var(--tt-fs-overline);
    font-weight: var(--tt-fw-black);
    padding: 3px 9px;
    border-radius: var(--tt-radius-full);
}

.cost-page__total-badge--over {
    background: var(--tt-danger-subtle);
    color: var(--tt-danger-deep);
}

.cost-page__total-badge--safe {
    background: var(--tt-success-subtle);
    color: var(--tt-success-deep);
}

.cost-page__total-row {
    display: flex;
    align-items: baseline;
    gap: 4px;
    margin-top: 6px;
}

.cost-page__total-num {
    font-size: 22px;
    font-weight: var(--tt-fw-black);
    color: var(--tt-surface-inverse);
    letter-spacing: -0.01em;
}

.cost-page__total-unit {
    font-size: 12px;
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-muted);
}

.cost-page__total-helper {
    font-size: var(--tt-fs-overline);
    color: var(--tt-text-muted);
    line-height: 1.5;
    margin-top: 7px;
}

.cost-page__total-helper b {
    color: var(--tt-text-body);
}

/* ── 날짜별 거래 목록 ── */
.cost-page__day {
    flex: none;
    background: var(--tt-bg);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-xl);
    padding: 10px 6px 4px;
    box-shadow: var(--tt-elevation-2);
}

.cost-page__day-header {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
    padding: 0 9px 6px;
}

.cost-page__day-date {
    font-size: 12.5px;
    font-weight: var(--tt-fw-black);
}

.cost-page__day-amount {
    font-size: var(--tt-fs-overline);
    color: var(--tt-text-muted);
    font-weight: var(--tt-fw-bold);
    font-family: var(--tt-font-mono);
}

.cost-page__row {
    width: 100%;
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 8px 9px;
    background: none;
    border: none;
    border-radius: var(--tt-radius-lg);
    text-align: left;
    font-family: var(--tt-font-sans);
    cursor: pointer;
    -webkit-tap-highlight-color: transparent;
}

.cost-page__row:active {
    background: var(--tt-bg-fill);
}

.cost-page__row:disabled {
    cursor: default;
    opacity: 0.6;
}

.cost-page__row-icon {
    width: 32px;
    height: 32px;
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

.cost-page__row-info {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
}

.cost-page__row-name {
    font-size: 13.5px;
    font-weight: var(--tt-fw-black);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.cost-page__row-meta {
    font-size: var(--tt-fs-overline);
    color: var(--tt-text-hint);
    font-weight: var(--tt-fw-bold);
    margin-top: 2px;
}

.cost-page__row-amounts {
    flex: none;
    display: flex;
    flex-direction: column;
    align-items: flex-end;
}

.cost-page__row-amount {
    font-size: 14px;
    font-weight: var(--tt-fw-black);
}

/* 고친 행은 결제 금액에 취소선을 그어 「원래 금액 → 내 몫」 관계를 보이게 한다. */
.cost-page__row-amount--struck {
    font-size: 11.5px;
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-hint);
    text-decoration: line-through;
}

.cost-page__row-burden {
    font-size: 13px;
    font-weight: var(--tt-fw-black);
    color: var(--tt-primary);
    margin-top: 1px;
}

.cost-page__footnote {
    flex: none;
    font-size: var(--tt-fs-overline);
    color: var(--tt-text-hint);
    line-height: 1.5;
    padding: 0 4px;
}

.cost-page__footnote b {
    color: var(--tt-text-muted);
}

/* ── 확인 영역 ── */
.cost-page__bottom {
    flex: none;
    background: var(--tt-bg-fill);
    border-top: 1px solid var(--tt-border);
}

.cost-page__confirm-wrap {
    padding: 10px var(--tt-screen-padding) 12px;
}

/* ══════════════════════════════════════════════
   건별 실제 부담금 넘패드 시트
   ══════════════════════════════════════════════ */
.keypad-sheet__header {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
    gap: 8px;
}

.keypad-sheet__name {
    font-size: 15px;
    font-weight: var(--tt-fw-black);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.keypad-sheet__charged {
    flex: none;
    font-size: var(--tt-fs-overline);
    color: var(--tt-text-muted);
    font-weight: var(--tt-fw-bold);
    font-family: var(--tt-font-mono);
}

.keypad-sheet__field {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
    background: var(--tt-bg-fill);
    border: 1.5px solid var(--tt-surface-inverse);
    border-radius: var(--tt-radius-xl);
    padding: 13px 15px;
    margin-top: 10px;
}

.keypad-sheet__value-wrap {
    display: flex;
    align-items: center;
    gap: 2px;
}

.keypad-sheet__num {
    font-size: 22px;
    font-weight: var(--tt-fw-black);
    color: var(--tt-surface-inverse);
    letter-spacing: -0.01em;
}

.keypad-sheet__cursor {
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

.keypad-sheet__unit {
    font-size: 13px;
    font-weight: var(--tt-fw-black);
    color: var(--tt-text-muted);
}

.keypad-sheet__helper {
    font-size: var(--tt-fs-overline);
    color: var(--tt-text-muted);
    line-height: 1.5;
    margin-top: 7px;
}

.keypad-sheet__helper--error {
    color: var(--tt-danger);
    font-weight: var(--tt-fw-bold);
}

.keypad-sheet__numpad {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 6px;
    margin-top: 12px;
}

.keypad-sheet__key {
    background: var(--tt-bg-fill);
    border: 1px solid var(--tt-border);
    border-radius: 9px;
    padding: 9px 0;
    font-size: 19px;
    font-weight: var(--tt-fw-semibold);
    color: var(--tt-surface-inverse);
    font-family: var(--tt-font-sans);
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    -webkit-tap-highlight-color: transparent;
    transition: background 0.1s ease;
}

.keypad-sheet__key:active {
    background: var(--tt-border);
}

.keypad-sheet__key--small {
    font-size: 14px;
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-body);
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
