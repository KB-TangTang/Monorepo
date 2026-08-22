<script setup>
import BaseCard from '@/components/common/BaseCard.vue';

defineProps({
    days: {
        type: Array,
        required: true,
    },
});
</script>

<template>
    <BaseCard class="personal-mission-streak-card" padding="lg">
        <section class="streak">
            <header>
                <h2>연속 성공</h2>
                <span>🔥 5일</span>
            </header>

            <div class="streak__days">
                <div
                    v-for="day in days"
                    :key="day.date"
                    class="streak__day"
                    :class="`streak__day--${day.status}`"
                >
                    <span>
                        {{
                            day.status === 'success'
                                ? '무죄'
                                : day.status === 'failed'
                                  ? '유죄'
                                  : day.status === 'today'
                                    ? '⚖'
                                    : ''
                        }}
                    </span>

                    <small>
                        {{ day.status === 'today' ? '오늘' : day.date }}
                    </small>
                </div>
            </div>

            <div class="streak__summary">
                <div>
                    <small>이번 달 누적</small>
                    <strong>480점</strong>
                </div>

                <div>
                    <small>최고 연속</small>
                    <strong>9일</strong>
                </div>
            </div>
        </section>
    </BaseCard>
</template>

<style scoped>
.streak header {
    display: flex;
    justify-content: space-between;
    align-items: center;
}

.streak h2 {
    margin: 0;
    font-size: var(--tt-fs-section);
}

.streak header span {
    padding: var(--tt-space-1) var(--tt-space-3);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-accent-deep);
    background: var(--tt-accent-subtle);
    border-radius: var(--tt-radius-full);
}

.streak__days {
    display: grid;
    grid-template-columns: repeat(7, minmax(0, 1fr));
    gap: clamp(2px, 1.5vw, var(--tt-space-2));
    margin-top: var(--tt-space-4);
}

.streak__day {
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-1);
    align-items: center;
    min-width: 0;
}

.streak__day > span {
    display: flex;
    align-items: center;
    justify-content: center;
    width: min(38px, 100%);
    max-width: 38px;
    height: auto;
    aspect-ratio: 1;
    font-size: 10px;
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-full);
}

.streak__day small {
    color: var(--tt-text-muted);
}

.streak__day--success > span {
    color: var(--tt-success-deep);
    background: var(--tt-success-subtle);
    border: 2px solid var(--tt-success);
}

.streak__day--failed > span {
    color: var(--tt-danger-deep);
    background: var(--tt-danger-subtle);
    border: 2px solid var(--tt-danger);
}

.streak__day--today > span {
    color: var(--tt-accent-deep);
    background: var(--tt-accent-subtle);
    border: 2px dashed var(--tt-accent);
}

.streak__day--pending > span {
    color: var(--tt-text-hint);
    background: var(--tt-bg-fill);
    border-color: var(--tt-border);
}

.streak__summary {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: var(--tt-space-3);
    margin-top: var(--tt-space-5);
}

.streak__summary div {
    display: flex;
    flex-direction: column;
    padding: var(--tt-space-3);
    background: var(--tt-bg-fill);
    border-radius: var(--tt-radius-md);
}

.streak__summary small {
    color: var(--tt-text-muted);
}

.streak__summary strong {
    margin-top: var(--tt-space-1);
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-section);
}

@media (max-width: 390px) {
    .streak__days {
        gap: 3px;
    }

    .streak__day > span {
        font-size: 9px;
    }

    .streak__summary {
        gap: var(--tt-space-2);
    }
}

@media (max-width: 359px) {
    .personal-mission-streak-card {
        padding: var(--tt-space-3);
    }
}
</style>
