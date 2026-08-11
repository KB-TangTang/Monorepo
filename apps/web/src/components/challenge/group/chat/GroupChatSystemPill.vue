<script setup>
import { computed } from 'vue';
import judgeImg from '@/assets/images/judgment/judge_tangtang_default.png';
import verdictImg from '@/assets/images/emotions/49_verdict.png';

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

const avatarSrc = computed(() => {
    if (isVerdict.value) return verdictImg;
    return judgeImg;
});

/* 투표 마감 시각 → "마감 HH:MM" */
const deadlineLabel = computed(() => {
    if (!meta.value.deadline) return '';
    const d = new Date(meta.value.deadline);
    const h = d.getHours();
    const m = String(d.getMinutes()).padStart(2, '0');
    return `마감 ${h}:${m}`;
});

/* 판결 투표 결과 → "투표 N:M" */
const voteResultLabel = computed(() => {
    const vr = meta.value.voteResult;
    if (!vr) return '';
    return `투표 ${vr.guilty}:${vr.innocent}`;
});

/* 유무죄 라벨 */
const verdictLabel = computed(() => {
    return meta.value.verdict === 'GUILTY' ? '유죄 확정' : '무죄 확정';
});
</script>

<template>
    <div class="sys-pill-wrap">
        <div class="sys-pill" :class="pillClass">
            <img :src="avatarSrc" alt="판사 탕이" class="sys-pill__avatar" />

            <!-- 투표 개시 -->
            <template v-if="isVote">
                <span class="sys-pill__text">
                    <strong>배심 투표 시작</strong>
                    <template v-if="deadlineLabel"> · {{ deadlineLabel }}</template>
                </span>
                <button class="sys-pill__btn" @click="emit('vote', meta.indictmentId)">
                    투표하기
                </button>
            </template>

            <!-- 판결 확정 -->
            <template v-else-if="isVerdict">
                <span class="sys-pill__text">
                    <strong>{{ verdictLabel }}</strong>
                    <template v-if="voteResultLabel"> · {{ voteResultLabel }}</template>
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
    </div>
</template>

<style scoped>
.sys-pill-wrap {
    display: flex;
    justify-content: center;
    padding: 14px 0;
    margin: 6px 0;
    border-top: 1px solid var(--tt-border);
    border-bottom: 1px solid var(--tt-border);
}

.sys-pill {
    display: flex;
    align-items: center;
    gap: 8px;
    border-radius: var(--tt-radius-full);
    font-size: 11.5px;
    max-width: 280px;
}

.sys-pill--gold {
    background: var(--tt-accent-subtle);
    border: 1px solid #F2E2B8;
    color: #8A6510;
    padding: 5px 7px 5px 6px;
}

.sys-pill--red {
    background: var(--tt-danger-subtle);
    border: 1px solid #F3D3C9;
    color: #8A4A38;
    padding: 5px 11px 5px 6px;
}

.sys-pill__avatar {
    width: 22px;
    height: 22px;
    border-radius: 50%;
    background: #fff;
    object-fit: contain;
    flex: none;
}

.sys-pill__text {
    flex: 1;
    min-width: 0;
    font-weight: var(--tt-fw-semibold);
    line-height: 1.4;
}

.sys-pill__text strong {
    font-weight: var(--tt-fw-black);
}

.sys-pill--gold .sys-pill__text strong {
    color: #6E4F06;
}

.sys-pill--red .sys-pill__text strong {
    color: #C24B31;
}

.sys-pill__btn {
    border: none;
    background: #232842;
    color: var(--tt-accent);
    font-size: 11px;
    font-weight: var(--tt-fw-black);
    padding: 5px 11px;
    border-radius: var(--tt-radius-full);
    cursor: pointer;
    font-family: inherit;
    flex: none;
    white-space: nowrap;
}

.sys-pill__link {
    border: none;
    background: none;
    color: #C24B31;
    font-size: 11.5px;
    font-weight: var(--tt-fw-black);
    cursor: pointer;
    font-family: inherit;
    flex: none;
    white-space: nowrap;
    padding: 0;
}
</style>
