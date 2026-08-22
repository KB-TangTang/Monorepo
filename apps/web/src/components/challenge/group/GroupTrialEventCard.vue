<script setup>
import { computed } from 'vue';

const props = defineProps({
    /** 'indictment' | 'trial' */
    type: { type: String, required: true },
    /** 요약 데이터 객체 (count 포함) */
    data: { type: Object, required: true },
});

const emit = defineEmits(['action']);

const isIndictment = computed(() => props.type === 'indictment');

const statusLabel = computed(() => {
    const d = props.data;
    if (isIndictment.value) {
        return `INDICTMENT · 기소 ${d.count}건`;
    }
    return `TRIAL · 판결 대기 ${d.count}건`;
});

const badgeText = computed(() => {
    if (isIndictment.value) return '새 알림';
    const d = props.data;
    return `${d.votedCount} / ${d.totalVoters} 투표`;
});

const title = computed(() =>
    isIndictment.value ? '기소가 접수됐어요' : '투표가 열렸어요'
);

const subtitle = computed(() => {
    const d = props.data;
    if (isIndictment.value) {
        return `${d.challengeName} · 청구`;
    }
    return `${d.latestDefendantName}님의 변론 · 마감 ${d.deadlineLabel}`;
});

const chargeAmountFormatted = computed(() => {
    if (!isIndictment.value) return '';
    return `${props.data.chargeAmount?.toLocaleString()}원`;
});

const actionLabel = computed(() =>
    isIndictment.value ? '변론하기' : '투표하기'
);

const deadlineLabel = computed(() => {
    if (!isIndictment.value) return '';
    return `마감 D-${props.data.daysLeft}`;
});

function onAction() {
    emit('action', props.type);
}
</script>

<template>
    <div :class="['gc-event', isIndictment ? 'gc-event--indictment' : 'gc-event--trial']">
        <!-- 상단 상태 행 -->
        <div class="gc-event__header">
            <div class="gc-event__status">
                <span :class="['gc-event__dot', isIndictment ? 'gc-event__dot--red' : 'gc-event__dot--gold']" />
                <span :class="['gc-event__label', isIndictment ? 'gc-event__label--red' : 'gc-event__label--gold']">
                    {{ statusLabel }}
                </span>
            </div>
            <span :class="['gc-event__badge', isIndictment ? 'gc-event__badge--red' : 'gc-event__badge--gold']">
                {{ badgeText }}
            </span>
        </div>

        <!-- 제목 -->
        <div class="gc-event__title">{{ title }}</div>

        <!-- 설명 -->
        <div class="gc-event__subtitle">
            {{ subtitle }}
            <b v-if="isIndictment" class="gc-event__charge">{{ chargeAmountFormatted }}</b>
        </div>

        <!-- 액션 행 -->
        <div class="gc-event__actions">
            <button
                :class="['gc-event__action', isIndictment ? 'gc-event__action--red' : 'gc-event__action--gold']"
                @click="onAction"
            >
                {{ actionLabel }}
            </button>
            <!-- 기소: 마감 라벨 / 투표: 투표 현황 아바타 -->
            <span v-if="isIndictment" class="gc-event__deadline">{{ deadlineLabel }}</span>
            <div v-else class="gc-event__voters">
                <span
                    v-for="(vote, i) in data.votes"
                    :key="i"
                    :class="[
                        'gc-event__vote-chip',
                        vote.verdict === 'GUILTY' ? 'gc-event__vote-chip--guilty' : 'gc-event__vote-chip--not-guilty',
                    ]"
                >
                    {{ vote.verdict === 'GUILTY' ? '유' : '무' }}
                </span>
                <span
                    v-if="data.totalVoters - data.votedCount > 0"
                    class="gc-event__vote-chip gc-event__vote-chip--remaining"
                >
                    +{{ data.totalVoters - data.votedCount }}
                </span>
            </div>
        </div>
    </div>
</template>

<style scoped>
.gc-event {
    background: var(--tt-bg);
    border-radius: var(--tt-radius-xl);
    box-shadow: var(--tt-elevation-3);
    padding: 15px 17px;
}

.gc-event--indictment { border: 1px solid #F3D3C9; }
.gc-event--trial { border: 1px solid #F3E3B8; }

/* ── 상단 상태 행 ───────────────────────── */
.gc-event__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
}

.gc-event__status {
    display: flex;
    align-items: center;
    gap: var(--tt-space-2);
}

.gc-event__dot {
    width: 10px;
    height: 10px;
    border-radius: 50%;
    flex: none;
}
.gc-event__dot--red { background: var(--tt-red); }
.gc-event__dot--gold { background: var(--tt-gold); }

.gc-event__label {
    font-size: var(--tt-fs-overline);
    font-weight: var(--tt-fw-black);
    letter-spacing: 0.08em;
}
.gc-event__label--red { color: var(--tt-red-deep); }
.gc-event__label--gold { color: var(--tt-gold-deep); }

.gc-event__badge {
    font-size: var(--tt-fs-overline);
    font-weight: var(--tt-fw-black);
    padding: 4px 10px;
    border-radius: var(--tt-radius-full);
}
.gc-event__badge--red {
    background: var(--tt-red-soft);
    color: var(--tt-red-deep);
}
.gc-event__badge--gold {
    background: var(--tt-gold-soft);
    color: var(--tt-gold-deep);
}

/* ── 제목 + 설명 ────────────────────────── */
.gc-event__title {
    font-size: 16px;
    font-weight: var(--tt-fw-black);
    margin-top: 10px;
    color: var(--tt-text);
}

.gc-event__subtitle {
    font-size: var(--tt-fs-caption);
    color: var(--tt-text-muted);
    margin-top: 4px;
}

.gc-event__charge {
    color: var(--tt-red-deep);
}

/* ── 액션 행 ────────────────────────────── */
.gc-event__actions {
    margin-top: var(--tt-space-3);
    display: flex;
    align-items: center;
    gap: 12%;
}

.gc-event__action {
    flex: 1;
    font-weight: var(--tt-fw-black);
    font-size: var(--tt-fs-button);
    font-family: inherit;
    padding: 11px;
    border-radius: var(--tt-radius-sm);
    text-align: center;
    border: none;
    cursor: pointer;
    transition: filter 0.15s ease;
}
.gc-event__action:active { filter: brightness(0.93); }

.gc-event__action--red {
    background: var(--tt-red);
    color: var(--tt-white);
}
.gc-event__action--gold {
    background: var(--tt-gold);
    color: var(--tt-text);
}

.gc-event__deadline {
    width: 75px;
    flex: none;
    text-align: right;
    font-size: var(--tt-fs-overline);
    color: var(--tt-text-muted);
    font-weight: var(--tt-fw-bold);
    white-space: nowrap;
}

/* ── 투표 현황 아바타 ───────────────────── */
.gc-event__voters {
    width: 75px;
    flex: none;
    display: flex;
    justify-content: flex-end;
}

.gc-event__vote-chip {
    width: 24px;
    height: 24px;
    border-radius: 50%;
    border: 2px solid var(--tt-white);
    font-size: 10px;
    font-weight: var(--tt-fw-black);
    display: flex;
    align-items: center;
    justify-content: center;
    color: var(--tt-white);
}

.gc-event__vote-chip + .gc-event__vote-chip {
    margin-left: -7px;
}

.gc-event__vote-chip--guilty { background: var(--tt-green); }
.gc-event__vote-chip--not-guilty { background: var(--tt-blue); }
.gc-event__vote-chip--remaining {
    background: var(--tt-bg-fill);
    color: var(--tt-text-muted);
}
</style>
