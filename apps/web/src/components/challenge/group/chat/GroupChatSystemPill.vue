<script setup>
import { computed } from 'vue';

const props = defineProps({
    message: { type: Object, required: true },
});

const emit = defineEmits(['vote', 'open-verdict']);

const meta = computed(() => props.message.metadata ?? {});
const subType = computed(() => props.message.systemSubType);

const isVote = computed(() => subType.value === 'VOTE_OPENED');
const isVerdict = computed(() => subType.value === 'VERDICT_CONFIRMED');
const isConfession = computed(() => subType.value === 'CONFESSION');

const pillClass = computed(() => {
    if (isVerdict.value || isConfession.value) return 'sys-pill--red';
    return 'sys-pill--gold';
});
</script>

<template>
    <div class="sys-pill" :class="pillClass">
        <!-- 투표 개시 -->
        <template v-if="isVote">
            <span class="sys-pill__text">
                <strong>{{ meta.defendantName }}</strong>님에 대한 투표가 시작되었습니다
            </span>
            <button class="sys-pill__btn" @click="emit('vote', meta.indictmentId)">
                투표하기
            </button>
        </template>

        <!-- 판결 확정 -->
        <template v-else-if="isVerdict">
            <span class="sys-pill__text">
                <strong>{{ meta.defendantName }}</strong>님 판결:
                {{ meta.verdict === 'GUILTY' ? '유죄' : '무죄' }}
                <template v-if="meta.penalty"> · {{ meta.penalty }}</template>
            </span>
            <button class="sys-pill__link" @click="emit('open-verdict', meta.indictmentId)">
                판결문 ›
            </button>
        </template>

        <!-- 혐의 인정 -->
        <template v-else-if="isConfession">
            <span class="sys-pill__text">
                <strong>{{ meta.defendantName }}</strong>님이 혐의를 인정했습니다
            </span>
        </template>
    </div>
</template>

<style scoped>
.sys-pill {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: var(--tt-space-2);
    padding: 10px 14px;
    border-radius: var(--tt-radius-full);
    font-size: var(--tt-fs-caption);
}

.sys-pill--gold {
    background: var(--tt-accent-subtle);
    color: var(--tt-accent-deep);
}

.sys-pill--red {
    background: var(--tt-danger-subtle);
    color: var(--tt-danger-deep);
}

.sys-pill__text {
    flex: 1;
    min-width: 0;
    font-weight: var(--tt-fw-semibold);
    line-height: 1.3;
}

.sys-pill__btn {
    border: none;
    background: var(--tt-accent);
    color: var(--tt-primary);
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-black);
    padding: 5px 14px;
    border-radius: var(--tt-radius-full);
    cursor: pointer;
    flex: none;
    white-space: nowrap;
}

.sys-pill__link {
    border: none;
    background: none;
    color: inherit;
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-black);
    cursor: pointer;
    flex: none;
    white-space: nowrap;
    text-decoration: underline;
    padding: 0;
}
</style>
