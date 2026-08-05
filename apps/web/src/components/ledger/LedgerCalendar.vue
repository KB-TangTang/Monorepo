<!--
  용도: 조회 월의 달력 그리드. 날짜를 눌러 아래 거래내역 목록을 그 날짜로 좁힌다.
  언제 쓰는지: LedgerView 한 곳에서만 렌더한다. 거래가 있는 날은 점으로 표시한다.
-->
<script setup>
import { computed } from 'vue';
import { buildCalendarWeeks } from '@/utils/ledger';

const props = defineProps({
    period: { type: String, required: true },
    transactionsByDate: { type: Map, required: true },
    selectedDate: { type: String, default: null },
});

defineEmits(['select-date']);

const weeks = computed(() =>
    buildCalendarWeeks(props.period, new Set(props.transactionsByDate.keys())),
);

function weekdayTone(index) {
    if (index === 0) return 'ledger-calendar__cell--sun';
    if (index === 6) return 'ledger-calendar__cell--sat';
    return '';
}
</script>

<template>
    <section class="ledger-calendar" aria-label="날짜별 거래내역 달력">
        <div class="ledger-calendar__weekdays">
            <span
                v-for="(label, index) in ['일', '월', '화', '수', '목', '금', '토']"
                :key="label"
                :class="weekdayTone(index)"
            >
                {{ label }}
            </span>
        </div>

        <div v-for="(week, weekIndex) in weeks" :key="weekIndex" class="ledger-calendar__week">
            <button
                v-for="(cell, dayIndex) in week"
                :key="`${weekIndex}-${dayIndex}`"
                type="button"
                class="ledger-calendar__day"
                :class="[
                    weekdayTone(dayIndex),
                    {
                        'ledger-calendar__day--outside': !cell.inCurrentMonth,
                        'ledger-calendar__day--selected':
                            cell.dateStr !== null && cell.dateStr === selectedDate,
                    },
                ]"
                :disabled="!cell.inCurrentMonth"
                @click="$emit('select-date', cell.dateStr)"
            >
                <span class="ledger-calendar__day-badge">{{ cell.day }}</span>
                <i v-if="cell.hasTransactions" aria-hidden="true"></i>
            </button>
        </div>
    </section>
</template>

<style scoped>
.ledger-calendar {
    padding: var(--tt-space-4);
    background: var(--tt-bg);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-lg);
}

.ledger-calendar__weekdays,
.ledger-calendar__week {
    display: grid;
    grid-template-columns: repeat(7, 1fr);
}

.ledger-calendar__weekdays {
    margin-bottom: var(--tt-space-2);
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-muted);
    text-align: center;
}

.ledger-calendar__day {
    position: relative;
    display: grid;
    height: 48px;
    font-size: var(--tt-fs-body);
    color: var(--tt-text);
    background: transparent;
    border: 0;
    cursor: pointer;
    place-items: center;
}

.ledger-calendar__day-badge {
    display: grid;
    width: 34px;
    height: 34px;
    border-radius: var(--tt-radius-full);
    place-items: center;
}

.ledger-calendar__day--outside {
    color: var(--tt-border-strong);
    cursor: default;
}

.ledger-calendar__cell--sun {
    color: var(--tt-danger);
}

.ledger-calendar__cell--sat {
    color: var(--tt-primary);
}

.ledger-calendar__day--outside.ledger-calendar__cell--sun,
.ledger-calendar__day--outside.ledger-calendar__cell--sat {
    color: var(--tt-border-strong);
}

.ledger-calendar__day--selected .ledger-calendar__day-badge {
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
    background: var(--tt-accent);
}

.ledger-calendar__day i {
    position: absolute;
    bottom: 1px;
    left: 50%;
    width: 4px;
    height: 4px;
    background: currentColor;
    border-radius: var(--tt-radius-full);
    opacity: 0.6;
    transform: translateX(-50%);
}

.ledger-calendar__day--selected i {
    background: var(--tt-text);
    opacity: 1;
}
</style>
