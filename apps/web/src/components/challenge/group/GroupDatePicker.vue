<script setup>
import { ref, computed, watch } from 'vue';
import BaseBottomSheet from '@/components/common/BaseBottomSheet.vue';

const props = defineProps({
    modelValue: { type: String, default: '' },
    label: { type: String, default: '날짜 선택' },
    min: { type: String, default: '' },
    max: { type: String, default: '' },
});

const emit = defineEmits(['update:modelValue']);

const isOpen = ref(false);

/* ── 캘린더가 보여줄 연·월 ────────────────── */
const viewYear = ref(new Date().getFullYear());
const viewMonth = ref(new Date().getMonth()); // 0-based

watch(isOpen, (open) => {
    if (!open) return;
    // 열릴 때: 선택된 날짜가 있으면 그 달을, 없으면 min 날짜의 달을
    if (props.modelValue) {
        const d = new Date(props.modelValue);
        viewYear.value = d.getFullYear();
        viewMonth.value = d.getMonth();
    } else if (props.min) {
        const d = new Date(props.min);
        viewYear.value = d.getFullYear();
        viewMonth.value = d.getMonth();
    } else {
        const now = new Date();
        viewYear.value = now.getFullYear();
        viewMonth.value = now.getMonth();
    }
});

const WEEKDAYS = ['일', '월', '화', '수', '목', '금', '토'];

const viewLabel = computed(() => `${viewYear.value}년 ${viewMonth.value + 1}월`);

/* ── 이전/다음 월 이동 가능 여부 ─────────── */
const canPrev = computed(() => {
    if (!props.min) return true;
    const minD = new Date(props.min);
    return viewYear.value > minD.getFullYear()
        || (viewYear.value === minD.getFullYear() && viewMonth.value > minD.getMonth());
});

const canNext = computed(() => {
    if (!props.max) return true;
    const maxD = new Date(props.max);
    return viewYear.value < maxD.getFullYear()
        || (viewYear.value === maxD.getFullYear() && viewMonth.value < maxD.getMonth());
});

function prevMonth() {
    if (!canPrev.value) return;
    if (viewMonth.value === 0) {
        viewMonth.value = 11;
        viewYear.value--;
    } else {
        viewMonth.value--;
    }
}

function nextMonth() {
    if (!canNext.value) return;
    if (viewMonth.value === 11) {
        viewMonth.value = 0;
        viewYear.value++;
    } else {
        viewMonth.value++;
    }
}

/* ── 달력 그리드 생성 ────────────────────── */
const calendarDays = computed(() => {
    const year = viewYear.value;
    const month = viewMonth.value;
    const firstDay = new Date(year, month, 1).getDay(); // 0=일
    const daysInMonth = new Date(year, month + 1, 0).getDate();

    const cells = [];

    // 앞쪽 빈칸
    for (let i = 0; i < firstDay; i++) {
        cells.push({ day: null, dateStr: '', disabled: true });
    }

    const todayStr = toDateStr(new Date());

    for (let d = 1; d <= daysInMonth; d++) {
        const dateStr = toDateStr(new Date(year, month, d));
        const disabled = (props.min && dateStr < props.min) || (props.max && dateStr > props.max);
        const isToday = dateStr === todayStr;
        const isSelected = dateStr === props.modelValue;

        cells.push({ day: d, dateStr, disabled, isToday, isSelected });
    }

    return cells;
});

function toDateStr(date) {
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, '0');
    const d = String(date.getDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
}

function selectDate(cell) {
    if (cell.disabled || !cell.day) return;
    emit('update:modelValue', cell.dateStr);
    isOpen.value = false;
}

function open() {
    isOpen.value = true;
}
</script>

<template>
    <button type="button" class="gdp-trigger" @click="open">
        <span class="gdp-trigger__label">{{ label }}</span>
        <span class="gdp-trigger__value" :class="{ 'gdp-trigger__value--empty': !modelValue }">
            <template v-if="modelValue">
                {{ new Date(modelValue).getMonth() + 1 }}월 {{ new Date(modelValue).getDate() }}일
            </template>
            <template v-else>선택</template>
        </span>
    </button>

    <BaseBottomSheet v-model="isOpen" :title="label">
        <div class="gdp-calendar">
            <!-- ── 월 이동 ──────────────────── -->
            <div class="gdp-nav">
                <button
                    type="button"
                    class="gdp-nav__btn"
                    :disabled="!canPrev"
                    aria-label="이전 달"
                    @click="prevMonth"
                >
                    <svg width="7" height="12" viewBox="0 0 7 12" fill="none">
                        <path d="M6 1L1 6l5 5" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
                    </svg>
                </button>
                <span class="gdp-nav__label">{{ viewLabel }}</span>
                <button
                    type="button"
                    class="gdp-nav__btn"
                    :disabled="!canNext"
                    aria-label="다음 달"
                    @click="nextMonth"
                >
                    <svg width="7" height="12" viewBox="0 0 7 12" fill="none">
                        <path d="M1 1l5 5-5 5" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
                    </svg>
                </button>
            </div>

            <!-- ── 요일 헤더 ────────────────── -->
            <div class="gdp-weekdays">
                <span
                    v-for="w in WEEKDAYS"
                    :key="w"
                    class="gdp-weekday"
                    :class="{ 'gdp-weekday--sun': w === '일', 'gdp-weekday--sat': w === '토' }"
                >
                    {{ w }}
                </span>
            </div>

            <!-- ── 날짜 그리드 ──────────────── -->
            <div class="gdp-grid">
                <button
                    v-for="(cell, i) in calendarDays"
                    :key="i"
                    type="button"
                    class="gdp-day"
                    :class="{
                        'gdp-day--empty': !cell.day,
                        'gdp-day--disabled': cell.disabled,
                        'gdp-day--today': cell.isToday,
                        'gdp-day--selected': cell.isSelected,
                    }"
                    :disabled="cell.disabled || !cell.day"
                    @click="selectDate(cell)"
                >
                    <span v-if="cell.day">{{ cell.day }}</span>
                </button>
            </div>
        </div>
    </BaseBottomSheet>
</template>

<style scoped>
/* ── trigger button (날짜 필드) ───────────── */
.gdp-trigger {
    flex: 1;
    position: relative;
    border: 1.5px solid var(--tt-border);
    border-radius: 13px;
    padding: 11px 13px;
    cursor: pointer;
    background: var(--tt-bg);
    text-align: left;
    transition: border-color 0.15s ease;
}

.gdp-trigger:active {
    border-color: var(--tt-primary);
}

.gdp-trigger__label {
    display: block;
    font-size: var(--tt-fs-overline);
    color: var(--tt-text-muted);
    font-weight: var(--tt-fw-bold);
}

.gdp-trigger__value {
    display: block;
    font-size: 16px;
    font-weight: var(--tt-fw-black);
    margin-top: 3px;
    color: var(--tt-text);
}

.gdp-trigger__value--empty {
    color: var(--tt-text-hint);
    font-weight: var(--tt-fw-bold);
}

/* ── calendar ────────────────────────────── */
.gdp-calendar {
    padding-bottom: var(--tt-space-2);
}

/* ── month navigation ────────────────────── */
.gdp-nav {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 var(--tt-space-1);
    margin-bottom: var(--tt-space-4);
}

.gdp-nav__label {
    font-size: var(--tt-fs-label);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
}

.gdp-nav__btn {
    width: 36px;
    height: 36px;
    display: flex;
    align-items: center;
    justify-content: center;
    border: none;
    border-radius: 50%;
    background: var(--tt-bg-fill);
    color: var(--tt-text-body);
    cursor: pointer;
    transition: background-color 0.15s ease;
}

.gdp-nav__btn:active:not(:disabled) {
    background: var(--tt-border);
}

.gdp-nav__btn:disabled {
    opacity: 0.3;
    cursor: not-allowed;
}

/* ── weekday headers ─────────────────────── */
.gdp-weekdays {
    display: grid;
    grid-template-columns: repeat(7, 1fr);
    margin-bottom: var(--tt-space-1);
}

.gdp-weekday {
    text-align: center;
    font-size: var(--tt-fs-overline);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-muted);
    padding: var(--tt-space-1) 0;
}

.gdp-weekday--sun {
    color: var(--tt-danger);
}

.gdp-weekday--sat {
    color: var(--tt-info);
}

/* ── day grid ────────────────────────────── */
.gdp-grid {
    display: grid;
    grid-template-columns: repeat(7, 1fr);
    gap: 2px;
}

.gdp-day {
    position: relative;
    display: flex;
    align-items: center;
    justify-content: center;
    height: 42px;
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-semibold);
    color: var(--tt-text);
    border: none;
    border-radius: 50%;
    background: transparent;
    cursor: pointer;
    transition: background-color 0.12s ease;
}

.gdp-day:active:not(:disabled):not(.gdp-day--empty) {
    background: var(--tt-bg-fill);
}

.gdp-day--empty {
    cursor: default;
}

.gdp-day--disabled {
    color: var(--tt-text-hint);
    opacity: 0.4;
    cursor: not-allowed;
}

.gdp-day--today:not(.gdp-day--selected) {
    color: var(--tt-accent);
    font-weight: var(--tt-fw-black);
}

.gdp-day--today:not(.gdp-day--selected)::after {
    content: '';
    position: absolute;
    bottom: 4px;
    left: 50%;
    transform: translateX(-50%);
    width: 4px;
    height: 4px;
    border-radius: 50%;
    background: var(--tt-accent);
}

.gdp-day--selected {
    background: var(--tt-primary);
    color: var(--tt-text-inverse);
    font-weight: var(--tt-fw-black);
}
</style>
