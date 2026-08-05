<script setup>
const ratioOptions = [
    { value: 'portrait', label: '기본 3:4' },
    { value: 'square', label: '정사각형 1:1' },
    { value: 'story', label: '스토리 9:16' },
];

defineProps({
    modelValue: { type: String, required: true },
});

const emit = defineEmits(['update:modelValue']);
</script>

<template>
    <section class="certificate-ratio-selector" aria-labelledby="certificate-ratio-heading">
        <h2 id="certificate-ratio-heading">카드 비율</h2>

        <div class="certificate-ratio-selector__options">
            <button
                v-for="option in ratioOptions"
                :key="option.value"
                type="button"
                :class="{ 'is-selected': option.value === modelValue }"
                :aria-pressed="option.value === modelValue"
                @click="emit('update:modelValue', option.value)"
            >
                {{ option.label }}
            </button>
        </div>
    </section>
</template>

<style scoped src="./PersonalCertificateRatioSelector.css"></style>
