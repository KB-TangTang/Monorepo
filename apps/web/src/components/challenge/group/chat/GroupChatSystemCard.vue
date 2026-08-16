<script setup>
import { computed } from 'vue';
import judgeImg from '@/assets/images/judgment/judge_tangtang_default.png';

const props = defineProps({
    message: { type: Object, required: true },
});

const emit = defineEmits(['open-trial']);

const meta = computed(() => props.message.metadata ?? {});

const overAmountLabel = computed(() => {
    const amount = meta.value.overAmount ?? 0;
    return amount.toLocaleString() + '원';
});
</script>

<template>
    <div class="sys-card-wrap">
        <div class="sys-card">
            <img :src="judgeImg" alt="판사 탕이" class="sys-card__avatar" />
            <div class="sys-card__body">
                <p class="sys-card__title">재판이 게시됐어요</p>
                <p class="sys-card__desc">
                    {{ meta.defendantName }}님 ·
                    <strong class="sys-card__amount">{{ overAmountLabel }}</strong> 초과
                </p>
            </div>
            <button class="sys-card__cta" @click="emit('open-trial', meta.indictmentId)">
                변론 확인
            </button>
        </div>
    </div>
</template>

<style scoped>
.sys-card-wrap {
    display: flex;
    justify-content: center;
    padding: 14px 0;
    margin: 6px 0;
    border-top: 1px solid var(--tt-border);
    border-bottom: 1px solid var(--tt-border);
}

.sys-card {
    display: flex;
    align-items: center;
    gap: 10px;
    background: #232842;
    border-radius: 999px;
    padding: 9px 11px;
    width: 80%;
    box-shadow: 0 6px 14px -6px rgba(35, 40, 66, 0.4);
}

.sys-card__avatar {
    width: 30px;
    height: 30px;
    border-radius: 50%;
    background: var(--tt-accent-subtle);
    object-fit: contain;
    flex: none;
}

.sys-card__body {
    flex: 1;
    min-width: 0;
}

.sys-card__title {
    font-size: 12px;
    font-weight: var(--tt-fw-black);
    color: #fff;
    line-height: 1.3;
    margin: 0;
}

.sys-card__desc {
    font-size: 11px;
    color: #c7cbdb;
    font-weight: var(--tt-fw-semibold);
    line-height: 1.4;
    margin: 2px 0 0;
}

.sys-card__amount {
    color: #ff9e86;
    font-weight: var(--tt-fw-black);
}

.sys-card__cta {
    border: none;
    background: var(--tt-accent);
    color: #232842;
    font-size: 11px;
    font-weight: var(--tt-fw-black);
    padding: 6px 11px;
    border-radius: var(--tt-radius-full);
    cursor: pointer;
    font-family: inherit;
    flex: none;
    white-space: nowrap;
}
</style>
