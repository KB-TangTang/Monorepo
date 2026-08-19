<script setup>
import { computed } from 'vue';
import BaseButton from '@/components/common/BaseButton.vue';

const props = defineProps({
    form: { type: Object, required: true },
});

const emit = defineEmits(['confirm']);

// API 연동 시 실제 DB 값으로 교체 예정
const CATEGORY_MAP = {
    null: '총 소비',
    1: '식비',
    2: '카페',
    3: '편의점',
    4: '택시',
    5: '쇼핑',
};

function formatDate(dateStr) {
    if (!dateStr) return '';
    const d = new Date(dateStr);
    return `${d.getMonth() + 1}월 ${d.getDate()}일`;
}

function formatAmount(val) {
    if (!val && val !== 0) return '0';
    return Number(val).toLocaleString('ko-KR');
}

const categoryLabel = computed(() => CATEGORY_MAP[props.form.categoryId] ?? '총 소비');
const evalLabel = computed(() => props.form.evalType === 'DAILY' ? '일일결산' : '기간결산');
const periodLabel = computed(() => `${formatDate(props.form.startDate)} ~ ${formatDate(props.form.endDate)}`);
const limitLabel = computed(() => {
    const amt = formatAmount(props.form.limitAmount);
    return props.form.evalType === 'DAILY' ? `하루 ${amt}원` : `총 ${amt}원`;
});

const SUMMARY_ROWS = computed(() => [
    { key: '그룹 이름', value: props.form.groupName },
    { key: '결산 타입', value: evalLabel.value },
    { key: '대상', value: categoryLabel.value },
    { key: '기간', value: periodLabel.value },
    { key: '초과 기준', value: limitLabel.value },
]);
</script>

<template>
    <div class="step-confirm">
        <div class="step-confirm__card step-confirm__card--raised">
            <div
                v-for="(row, i) in SUMMARY_ROWS"
                :key="row.key"
                class="summary-row"
                :class="{ 'summary-row--last': i === SUMMARY_ROWS.length - 1 }"
            >
                <span class="summary-row__key">{{ row.key }}</span>
                <span class="summary-row__value">{{ row.value }}</span>
            </div>
        </div>

        <div class="step-confirm__info">
            <span class="step-confirm__info-icon">i</span>
            <p class="step-confirm__info-text">
                그룹 생성 후 바로 친구들을 초대할 수 있어요.<br>
                <b>첫날 23:59</b>가 지나면 초대가 닫혀요.<br>
                시작일까지 <b>혼자면 그룹이 사라져요.</b>
            </p>
        </div>

        <div class="step-confirm__bottom">
            <BaseButton variant="primary" size="lg" block @click="emit('confirm')">
                그룹 만들고 친구 초대
            </BaseButton>
        </div>
    </div>
</template>

<style scoped>
.step-confirm {
    flex: 1;
    display: flex;
    flex-direction: column;
    overflow-y: auto;
    margin-top: -22px;
    position: relative;
    z-index: 2;
}

.step-confirm__card {
    background: var(--tt-bg);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-xl);
    box-shadow: var(--tt-elevation-3);
    padding: 6px 18px;
    margin: 0 var(--tt-screen-padding);
}

.step-confirm__card--raised {
    box-shadow: var(--tt-elevation-3);
}

/* ── summary rows ────────────────────────── */
.summary-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 13px 0;
    border-bottom: 1px solid #F0EDE6;
}

.summary-row--last {
    border-bottom: none;
}

.summary-row__key {
    font-size: 13px;
    color: var(--tt-text-muted);
    font-weight: var(--tt-fw-semibold);
}

.summary-row__value {
    font-size: 14px;
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
}

/* ── info box (blue) ─────────────────────── */
.step-confirm__info {
    margin: 14px var(--tt-screen-padding) 0;
    background: var(--tt-info-subtle);
    border: 1px solid #C9D6F5;
    border-radius: 16px;
    padding: 13px 15px;
    display: flex;
    gap: 9px;
    align-items: flex-start;
}

.step-confirm__info-icon {
    width: 18px;
    height: 18px;
    border-radius: 50%;
    background: var(--tt-info);
    color: var(--tt-text-inverse);
    font-size: 11px;
    font-weight: var(--tt-fw-black);
    display: flex;
    align-items: center;
    justify-content: center;
    flex: none;
    margin-top: 1px;
}

.step-confirm__info-text {
    font-size: var(--tt-fs-caption);
    color: var(--tt-info-deep);
    line-height: 1.55;
}

.step-confirm__info-text b {
    font-weight: var(--tt-fw-black);
}

/* ── bottom button ────────────────────────── */
.step-confirm__bottom {
    margin-top: auto;
    padding: var(--tt-space-3) var(--tt-screen-padding) var(--tt-space-5);
}
</style>
