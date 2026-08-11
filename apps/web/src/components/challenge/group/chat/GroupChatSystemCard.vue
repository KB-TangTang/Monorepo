<script setup>
import { computed } from 'vue';
import judgeImg from '@/assets/images/judgment/judge_tangtang_default.png';

const props = defineProps({
    message: { type: Object, required: true },
});

const emit = defineEmits(['open-trial']);

const meta = computed(() => props.message.metadata ?? {});
</script>

<template>
    <div class="sys-card">
        <div class="sys-card__inner">
            <img :src="judgeImg" alt="판사 탕이" class="sys-card__avatar" />
            <div class="sys-card__body">
                <p class="sys-card__title">재판이 열렸습니다!</p>
                <p class="sys-card__desc">
                    <strong>{{ meta.defendantName }}</strong>님이
                    <strong>{{ meta.category }}</strong> 카테고리에서
                    <strong>{{ (meta.overAmount ?? 0).toLocaleString() }}원</strong> 초과했습니다.
                </p>
            </div>
        </div>
        <button
            class="sys-card__cta"
            @click="emit('open-trial', meta.indictmentId)"
        >
            변론 확인
        </button>
    </div>
</template>

<style scoped>
.sys-card {
    background: var(--tt-primary);
    border-radius: var(--tt-radius-lg);
    padding: var(--tt-space-4);
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-3);
}

.sys-card__inner {
    display: flex;
    gap: var(--tt-space-3);
    align-items: center;
}

.sys-card__avatar {
    width: 48px;
    height: 48px;
    border-radius: 50%;
    flex: none;
    background: rgba(255, 255, 255, 0.15);
    object-fit: contain;
}

.sys-card__body {
    flex: 1;
    min-width: 0;
}

.sys-card__title {
    font-size: var(--tt-fs-subtitle);
    font-weight: var(--tt-fw-black);
    color: #fff;
    margin: 0;
}

.sys-card__desc {
    font-size: var(--tt-fs-caption);
    color: rgba(255, 255, 255, 0.8);
    margin: 4px 0 0;
    line-height: 1.4;
}

.sys-card__cta {
    width: 100%;
    padding: 10px;
    border: none;
    border-radius: var(--tt-radius-md);
    background: var(--tt-accent);
    color: var(--tt-primary);
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-black);
    cursor: pointer;
}
</style>
