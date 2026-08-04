<script setup>
import { formatWon } from '@/services/personalMissionFlow';

defineProps({
    difficulty: {
        type: Object,
        required: true,
    },
    selected: {
        type: Boolean,
        default: false,
    },
});

const emit = defineEmits(['select']);
</script>

<template>
    <button
        type="button"
        class="difficulty-option"
        :class="{ 'difficulty-option--selected': selected }"
        :aria-pressed="selected"
        @click="emit('select', difficulty.id)"
    >
        <span class="difficulty-option__radio">
            {{ selected ? '✓' : '' }}
        </span>

        <span class="difficulty-option__content">
            <span class="difficulty-option__heading">
                <strong>{{ difficulty.label }}</strong>
                <small>{{ difficulty.shortLabel }}</small>
            </span>

            <span class="difficulty-option__description">
                {{ difficulty.description }}
            </span>

            <span v-if="selected" class="difficulty-option__target">
                <del>6,800원</del>
                <span>→</span>
                <strong>{{ formatWon(difficulty.targetAmount) }}</strong>
            </span>
        </span>

        <span class="difficulty-option__points"> +{{ difficulty.rewardPoints }}점 </span>
    </button>
</template>

<style scoped>
.difficulty-option {
    display: flex;
    gap: var(--tt-space-3);
    align-items: flex-start;
    width: 100%;
    padding: var(--tt-space-4);
    text-align: left;
    font-family: var(--tt-font-sans);
    color: var(--tt-text);
    background: var(--tt-bg);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-lg);
    cursor: pointer;
}

.difficulty-option--selected {
    border: 2px solid var(--tt-primary);
}

.difficulty-option__radio {
    display: flex;
    flex: 0 0 26px;
    align-items: center;
    justify-content: center;
    width: 26px;
    height: 26px;
    color: var(--tt-text-inverse);
    border: 2px solid var(--tt-border-strong);
    border-radius: var(--tt-radius-full);
}

.difficulty-option--selected .difficulty-option__radio {
    background: var(--tt-primary);
    border-color: var(--tt-primary);
}

.difficulty-option__content {
    display: flex;
    flex: 1;
    flex-direction: column;
}

.difficulty-option__heading {
    display: flex;
    gap: var(--tt-space-2);
    align-items: center;
}

.difficulty-option__heading small {
    padding: 2px var(--tt-space-2);
    color: var(--tt-text-muted);
    background: var(--tt-bg-subtle);
    border-radius: var(--tt-radius-full);
}

.difficulty-option__description {
    margin-top: var(--tt-space-2);
    font-size: var(--tt-fs-caption);
    color: var(--tt-text-muted);
}

.difficulty-option__target {
    display: flex;
    gap: var(--tt-space-3);
    align-items: center;
    margin-top: var(--tt-space-3);
    padding: var(--tt-space-3);
    background: var(--tt-bg-subtle);
    border: 1px dashed var(--tt-border-strong);
    border-radius: var(--tt-radius-md);
}

.difficulty-option__target del {
    color: var(--tt-text-muted);
}

.difficulty-option__points {
    padding: var(--tt-space-2);
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-primary);
    background: var(--tt-primary-subtle);
    border-radius: var(--tt-radius-sm);
}
</style>
