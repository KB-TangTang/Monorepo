<script setup>
defineProps({
    title: { type: String, required: true },
    backLabel: { type: String, default: '이전 화면으로 돌아가기' },
});

defineEmits(['back']);
</script>

<template>
    <header class="challenge-page-header">
        <div class="challenge-page-header__title-row">
            <button type="button" :aria-label="backLabel" @click="$emit('back')">
                <svg
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="2"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    aria-hidden="true"
                >
                    <path d="m15 4-8 8 8 8" />
                </svg>
            </button>
            <h1>{{ title }}</h1>
        </div>

        <div v-if="$slots.action" class="challenge-page-header__action">
            <slot name="action" />
        </div>
    </header>
</template>

<style scoped src="./ChallengePageHeader.css"></style>
