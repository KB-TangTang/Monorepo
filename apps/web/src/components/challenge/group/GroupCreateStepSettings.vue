<script setup>
import { computed } from 'vue';
import BaseButton from '@/components/common/BaseButton.vue';
import GroupDatePicker from '@/components/challenge/group/GroupDatePicker.vue';

const props = defineProps({
    evalType: { type: String, required: true },
    startDate: { type: String, required: true },
    endDate: { type: String, required: true },
    limitAmount: { type: [Number, String], default: null },
});

const emit = defineEmits(['update:start-date', 'update:end-date', 'update:limit-amount', 'next']);

/* ── date helpers ────────────────────────── */
const minStartDate = computed(() => {
    const d = new Date();
    d.setDate(d.getDate() + 1); // 시작일: 내일부터
    return toDateStr(d);
});

const minEndDate = computed(() => {
    if (!props.startDate) return '';
    const d = new Date(props.startDate);
    d.setDate(d.getDate() + 1);
    return toDateStr(d);
});

const maxEndDate = computed(() => {
    if (!props.startDate) return '';
    const d = new Date(props.startDate);
    d.setDate(d.getDate() + 6); // 최대 7일 (시작일 포함)
    return toDateStr(d);
});

function toDateStr(date) {
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, '0');
    const d = String(date.getDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
}

const totalDays = computed(() => {
    if (!props.startDate || !props.endDate) return 0;
    const s = new Date(props.startDate);
    const e = new Date(props.endDate);
    return Math.round((e - s) / (1000 * 60 * 60 * 24)) + 1;
});

function onStartDateChange(dateStr) {
    emit('update:start-date', dateStr);
    // 종료일이 범위를 벗어나면 리셋
    if (props.endDate) {
        const maxEnd = new Date(dateStr);
        maxEnd.setDate(maxEnd.getDate() + 6);
        if (new Date(props.endDate) > maxEnd || new Date(props.endDate) <= new Date(dateStr)) {
            emit('update:end-date', '');
        }
    }
}

/* ── amount helpers ──────────────────────── */
function formatAmount(val) {
    if (!val && val !== 0) return '';
    return Number(val).toLocaleString('ko-KR');
}

function onAmountInput(e) {
    const raw = e.target.value.replace(/[^0-9]/g, '');
    /* 0 을 falsy 로 보면 「0」을 치는 순간 칸이 도로 비워져 무지출 챌린지를 만들 수 없다. */
    const num = raw === '' ? null : Number(raw);
    emit('update:limit-amount', num);
    e.target.value = num === null ? '' : num.toLocaleString('ko-KR');
}

/*
 * **0 원은 「미입력」이 아니라 무지출 챌린지의 정상 입력값이다.** 한 푼이라도 쓰면 재판이 열린다.
 * 서버도 음수만 막고(ChallengeGroupService.validateCreate), 목록·상세 카드도 0 이면 「무지출」로
 * 표기한다 — 만들기 화면만 truthy 검사로 0 을 떨어뜨려 그 컨셉을 만들 수 없었다.
 * 그래서 「비어 있는지」와 「0 인지」를 갈라 둔다.
 */
const isEmpty = computed(() => props.limitAmount === null || props.limitAmount === '');
const isNoSpend = computed(() => !isEmpty.value && Number(props.limitAmount) === 0);

const isValid = computed(() => {
    if (!props.startDate || !props.endDate) {
        return false;
    }
    return !isEmpty.value && Number(props.limitAmount) >= 0;
});
</script>

<template>
    <div class="step-settings">
        <!-- ── 기간 카드 ──────────────────────── -->
        <div class="step-settings__card step-settings__card--raised">
            <p class="step-settings__desc">최대 7일까지 설정할 수 있어요.</p>

            <div class="step-settings__dates">
                <GroupDatePicker
                    :model-value="startDate"
                    label="시작 날짜"
                    :min="minStartDate"
                    @update:model-value="onStartDateChange"
                />
                <GroupDatePicker
                    :model-value="endDate"
                    label="종료 날짜"
                    :min="minEndDate"
                    :max="maxEndDate"
                    @update:model-value="emit('update:end-date', $event)"
                />
            </div>

            <div v-if="totalDays > 0" class="step-settings__info-box">
                총 {{ totalDays }}일{{ totalDays >= 7 ? ' · 설정 가능한 최대 기간' : '' }}
            </div>
        </div>

        <!-- ── 금액 카드 ──────────────────────── -->
        <div class="step-settings__card">
            <div class="step-settings__section-label">초과 기준 금액</div>
            <p class="step-settings__desc">
                <template v-if="evalType === 'DAILY'">
                    하루 동안 나의 소비가 넘으면 <b class="step-settings__danger">재판</b>이 열려요.
                </template>
                <template v-else>
                    전체 기간 나의 누적 소비가 넘으면 <b class="step-settings__danger">재판</b>이 열려요.
                </template>
            </p>

            <div class="amount-field">
                <input
                    type="text"
                    inputmode="numeric"
                    class="amount-field__input"
                    placeholder="금액 입력"
                    :value="formatAmount(limitAmount)"
                    @input="onAmountInput"
                />
                <span class="amount-field__unit">원</span>
            </div>

            <p v-if="isNoSpend" class="step-settings__no-spend">
                0원은 <b>무지출 챌린지</b>예요. 한 푼이라도 쓰면 재판이 열려요.
            </p>

            <div class="step-settings__info-box">
                <template v-if="evalType === 'DAILY'">
                    <b>판정 방식</b> · 1일 기준 · 매일 00:00에 새로 시작
                </template>
                <template v-else>
                    <b>판정 방식</b> · {{ totalDays || 'N' }}일 전체 기준 · 종료 다음 날 판정
                </template>
            </div>
        </div>

        <div class="step-settings__bottom">
            <BaseButton variant="primary" size="lg" block :disabled="!isValid" @click="emit('next')">
                설정 확인
            </BaseButton>
        </div>
    </div>
</template>

<style scoped>
.step-settings {
    flex: 1;
    display: flex;
    flex-direction: column;
    overflow-y: auto;
    margin-top: -22px;
    position: relative;
    z-index: 2;
}

.step-settings__card {
    background: var(--tt-bg);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-xl);
    box-shadow: var(--tt-elevation-2);
    padding: var(--tt-space-4);
    margin: 0 var(--tt-screen-padding);
}

.step-settings__card--raised {
    box-shadow: var(--tt-elevation-3);
}

.step-settings__card + .step-settings__card {
    margin-top: 14px;
}

.step-settings__section-label {
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text-muted);
}

.step-settings__desc {
    font-size: 11.5px;
    color: var(--tt-text-muted);
    line-height: 1.45;
    margin-top: 4px;
}

.step-settings__card--raised .step-settings__desc {
    margin-top: 0;
}

.step-settings__danger {
    color: var(--tt-danger-deep);
}

/* ── date fields ─────────────────────────── */
.step-settings__dates {
    margin-top: var(--tt-space-3);
    display: flex;
    gap: 10px;
}

/* ── amount field ────────────────────────── */
.amount-field {
    margin-top: 11px;
    border: 1.5px solid var(--tt-border);
    border-radius: 13px;
    padding: 13px 15px;
    display: flex;
    align-items: center;
    justify-content: space-between;
}

.amount-field__input {
    flex: 1;
    font-size: 22px;
    font-weight: var(--tt-fw-black);
    letter-spacing: -0.01em;
    color: var(--tt-text);
    border: none;
    outline: none;
    background: transparent;
    font-family: inherit;
}

.amount-field__input::placeholder {
    color: var(--tt-text-hint);
}

.amount-field__unit {
    font-size: 14px;
    color: var(--tt-text-muted);
    font-weight: var(--tt-fw-bold);
    flex: none;
    margin-left: var(--tt-space-2);
}

/* 0원을 골랐을 때만 뜬다. 금액 칸의 「0」이 미입력으로 오해되지 않게 뜻을 적어 준다. */
.step-settings__no-spend {
    margin-top: 9px;
    font-size: 11.5px;
    line-height: 1.45;
    color: var(--tt-primary);
}

.step-settings__no-spend b {
    font-weight: var(--tt-fw-black);
}

/* ── info box (warm yellow) ──────────────── */
.step-settings__info-box {
    margin-top: var(--tt-space-3);
    background: #FFF6E2;
    border: 1px solid #F0E0B8;
    border-radius: 13px;
    padding: 11px 13px;
    font-size: 11.5px;
    color: #8A6A16;
    line-height: 1.5;
}

.step-settings__info-box b {
    font-weight: var(--tt-fw-black);
}

/* ── bottom button ────────────────────────── */
.step-settings__bottom {
    margin-top: auto;
    padding: var(--tt-space-3) var(--tt-screen-padding) var(--tt-space-5);
}
</style>
