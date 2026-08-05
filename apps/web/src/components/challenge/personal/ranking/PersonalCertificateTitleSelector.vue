<script setup>
defineProps({
    titles: { type: Array, required: true },
    modelValue: { type: String, required: true },
});

const emit = defineEmits(['update:modelValue']);

function selectTitle(title) {
    emit('update:modelValue', title);
}
</script>

<template>
    <section class="certificate-title-selector" aria-labelledby="certificate-title-heading">
        <div class="certificate-title-selector__heading">
            <div>
                <h2 id="certificate-title-heading">AI 명예 타이틀 추천</h2>
                <p>인증서에 사용할 제목을 선택해 주세요.</p>
            </div>
            <span>탭해서 교체</span>
        </div>

        <div class="certificate-title-selector__options">
            <button
                v-for="title in titles"
                :key="title"
                type="button"
                :class="{ 'is-selected': title === modelValue }"
                :aria-pressed="title === modelValue"
                @click="selectTitle(title)"
            >
                {{ title }}
            </button>
        </div>
    </section>
</template>

<style scoped src="./PersonalCertificateTitleSelector.css"></style>
