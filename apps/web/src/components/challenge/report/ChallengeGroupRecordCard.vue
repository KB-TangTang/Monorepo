<script setup>
import { computed } from 'vue';

const props = defineProps({
    state: {
        type: String,
        default: 'EMPTY',
        validator: (value) => ['JUDGING', 'READY', 'EMPTY'].includes(value),
    },
    groupRecord: { type: Object, default: null },
});

defineEmits(['open-group-history']);

const isReady = computed(() => props.state === 'READY' && props.groupRecord);
const alternativeCopy = computed(() =>
    props.state === 'JUDGING'
        ? {
              label: '판결 대기',
              messageLines: ['챌린지 리포트 내 아직 재판 중인 법정이 있어요!'],
          }
        : {
              label: '참여 기록 없음',
              messageLines: ['참여한 법정 기록이 없어요!', '친구와 함께 법정을 열어봐요!'],
          },
);
</script>

<template>
    <article v-if="isReady" class="group-record-card">
        <div class="group-record-card__stats">
            <div>
                <strong>{{ groupRecord.participatingGroups }}개</strong><span>참여 그룹</span>
            </div>
            <div>
                <strong>{{ groupRecord.survivedCount }}승</strong><span>생존</span>
            </div>
            <div>
                <strong>{{ groupRecord.eliminatedCount }}패</strong><span>탈락</span>
            </div>
        </div>
        <footer>
            <div>
                <span class="group-record-card__stamp group-record-card__stamp--success"
                    >무죄 {{ groupRecord.acquittedCount }}</span
                >
                <span class="group-record-card__stamp group-record-card__stamp--danger"
                    >유죄 {{ groupRecord.convictedCount }}</span
                >
                <span>· 피기소 {{ groupRecord.indictedCount }}회</span>
            </div>
            <button type="button" @click="$emit('open-group-history')">이력 ›</button>
        </footer>
    </article>

    <article
        v-else
        class="group-record-card group-record-card--alternative"
        :class="`group-record-card--${state.toLowerCase()}`"
        role="status"
    >
        <span class="group-record-card__status-mark" aria-hidden="true">
            {{ state === 'JUDGING' ? '⌛' : '⚖' }}
        </span>
        <div>
            <small>{{ alternativeCopy.label }}</small>
            <p>
                <span v-for="line in alternativeCopy.messageLines" :key="line">{{ line }}</span>
            </p>
        </div>
    </article>
</template>

<style scoped>
.group-record-card {
    padding: var(--tt-space-5);
    background: var(--tt-primary-subtle);
    border: 1px solid color-mix(in srgb, var(--tt-primary) 14%, var(--tt-primary-subtle));
    border-radius: var(--tt-radius-lg);
}

.group-record-card__stats {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    text-align: center;
}

.group-record-card__stats > div + div {
    border-left: 1px solid color-mix(in srgb, var(--tt-primary) 20%, var(--tt-primary-subtle));
}

.group-record-card__stats strong,
.group-record-card__stats span {
    display: block;
}

.group-record-card__stats strong {
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-title);
}

.group-record-card__stats > div:nth-child(2) strong {
    color: var(--tt-success);
}

.group-record-card__stats > div:nth-child(3) strong {
    color: var(--tt-danger);
}

.group-record-card__stats span,
.group-record-card footer span {
    font-size: var(--tt-fs-caption);
    color: var(--tt-text-muted);
}

.group-record-card footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: var(--tt-space-3);
    margin-top: var(--tt-space-5);
    padding-top: var(--tt-space-4);
    border-top: 1px solid var(--tt-border);
}

.group-record-card footer > div {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: var(--tt-space-2);
}

.group-record-card footer button {
    flex-shrink: 0;
    font-weight: var(--tt-fw-bold);
    color: var(--tt-primary);
    background: transparent;
    border: 0;
    cursor: pointer;
}

.group-record-card__stamp {
    padding: var(--tt-space-1) var(--tt-space-2);
    font-weight: var(--tt-fw-bold);
    background: var(--tt-bg-subtle);
    border: 1px solid currentColor;
    border-radius: var(--tt-radius-sm);
    transform: rotate(-3deg);
}

.group-record-card__stamp--success {
    color: var(--tt-success) !important;
}

.group-record-card__stamp--danger {
    color: var(--tt-danger) !important;
    transform: rotate(3deg);
}

.group-record-card--alternative {
    display: flex;
    align-items: center;
    gap: var(--tt-space-4);
    min-height: 116px;
}

.group-record-card--judging {
    background: var(--tt-accent-subtle);
    border-color: var(--tt-accent-subtle-border);
}

.group-record-card--empty {
    background: var(--tt-bg);
    border-color: var(--tt-border);
}

.group-record-card__status-mark {
    display: grid;
    flex-shrink: 0;
    width: 48px;
    height: 48px;
    font-size: var(--tt-fs-title);
    border-radius: var(--tt-radius-full);
    place-items: center;
}

.group-record-card--judging .group-record-card__status-mark {
    background: color-mix(in srgb, var(--tt-accent) 35%, var(--tt-accent-subtle));
}

.group-record-card--empty .group-record-card__status-mark {
    color: var(--tt-primary);
    background: var(--tt-primary-subtle);
}

.group-record-card--alternative small {
    display: block;
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text-muted);
}

.group-record-card--alternative p {
    margin-top: var(--tt-space-1);
    font-size: var(--tt-fs-label);
    font-weight: var(--tt-fw-black);
    line-height: var(--tt-lh-snug);
    color: var(--tt-text);
}

.group-record-card--alternative p span {
    display: block;
}

@media (max-width: 360px) {
    .group-record-card {
        padding: var(--tt-space-4);
    }

    .group-record-card footer {
        align-items: flex-start;
        flex-direction: column;
    }
}
</style>
