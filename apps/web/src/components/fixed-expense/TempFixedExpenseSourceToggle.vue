<script setup>
defineProps({
    source: {
        type: String,
        required: true,
        validator: (value) => ['api', 'mock'].includes(value),
    },
    loading: { type: Boolean, default: false },
    elevated: { type: Boolean, default: false },
});

defineEmits(['toggle']);
</script>

<template>
    <!-- TEMP(#202): #204·#205 고정지출 백엔드 연동 완료 후 컴포넌트와 호출부를 함께 삭제한다. -->
    <button
        type="button"
        class="temp-fixed-expense-source"
        :class="[
            `temp-fixed-expense-source--${source}`,
            { 'temp-fixed-expense-source--elevated': elevated },
        ]"
        :disabled="loading"
        :aria-label="`현재 ${source === 'api' ? '백엔드 API' : '목업'} 화면. 데이터 소스 전환`"
        :title="`${source === 'api' ? '백엔드 API' : '목업'} 화면 보는 중`"
        @click="$emit('toggle', source === 'api' ? 'mock' : 'api')"
    >
        <small>TEMP</small>
        <strong>{{ source === 'api' ? 'API' : '목업' }}</strong>
    </button>
</template>

<style scoped>
.temp-fixed-expense-source {
    position: fixed;
    right: max(var(--tt-space-3), calc((100vw - var(--tt-content-max)) / 2 + var(--tt-space-3)));
    bottom: calc(var(--tt-tabbar-height) + env(safe-area-inset-bottom) + var(--tt-space-3));
    z-index: var(--tt-z-sticky);
    display: grid;
    width: 48px;
    height: 48px;
    padding: 5px 3px;
    color: var(--tt-text-inverse);
    background: var(--tt-text);
    border: 2px solid var(--tt-bg);
    border-radius: var(--tt-radius-full);
    box-shadow: var(--tt-elevation-3);
    cursor: pointer;
    place-content: center;
}

.temp-fixed-expense-source--elevated {
    bottom: calc(var(--tt-tabbar-height) + env(safe-area-inset-bottom) + var(--tt-space-3) + 60px);
}

.temp-fixed-expense-source small {
    font-size: 8px;
    font-weight: var(--tt-fw-bold);
    line-height: 1;
    color: var(--tt-border-strong);
}

.temp-fixed-expense-source strong {
    margin-top: 2px;
    font-size: 11px;
    font-weight: var(--tt-fw-black);
    line-height: 1;
}

.temp-fixed-expense-source--mock {
    color: var(--tt-text);
    background: var(--tt-accent);
}

.temp-fixed-expense-source--mock small {
    color: var(--tt-accent-strong);
}

.temp-fixed-expense-source:disabled {
    cursor: wait;
    opacity: 0.55;
}
</style>
