<script setup>
import BaseBottomSheet from '@/components/common/BaseBottomSheet.vue';
import tangiJudge from '@/assets/images/emotions/48_judging.png';

defineProps({
    modelValue: { type: Boolean, required: true },
    isPreparing: { type: Boolean, default: false },
    message: { type: String, default: '' },
    certificateTitle: { type: String, required: true },
});

const emit = defineEmits(['update:modelValue', 'share', 'copy-link']);
</script>

<template>
    <BaseBottomSheet
        :model-value="modelValue"
        title="공유하기"
        @update:model-value="emit('update:modelValue', $event)"
    >
        <div class="certificate-share-sheet">
            <p class="certificate-share-sheet__description">
                성과 카드를 친구에게 보내거나 저장할 수 있어요.
            </p>

            <article class="certificate-share-sheet__preview">
                <img :src="tangiJudge" alt="판결봉을 든 탕이" />
                <div>
                    <small>카카오 메시지 템플릿</small>
                    <strong>{{ certificateTitle }} 😎</strong>
                    <p>
                        <span>참여하기</span>
                        <code>인증서 링크</code>
                    </p>
                </div>
            </article>

            <div class="certificate-share-sheet__actions">
                <button
                    type="button"
                    class="certificate-share-sheet__action--kakao"
                    :disabled="isPreparing"
                    @click="emit('share')"
                >
                    <span aria-hidden="true">
                        <svg viewBox="0 0 24 24"><path d="M4 5.5h16v11H9l-4 3v-3H4z" /></svg>
                    </span>
                    <strong>카카오톡</strong>
                </button>
                <button
                    type="button"
                    class="certificate-share-sheet__action--story"
                    :disabled="isPreparing"
                    @click="emit('share')"
                >
                    <span aria-hidden="true">
                        <svg viewBox="0 0 24 24">
                            <rect x="4" y="4" width="16" height="16" rx="5" />
                            <circle cx="12" cy="12" r="3.5" />
                            <circle cx="17" cy="7" r="1" class="is-filled" />
                        </svg>
                    </span>
                    <strong>스토리</strong>
                </button>
                <button type="button" @click="emit('copy-link')">
                    <span aria-hidden="true">
                        <svg viewBox="0 0 24 24">
                            <path d="m9 15 6-6" />
                            <path d="M7.5 17.5 5 20a3.5 3.5 0 0 1-5-5l3-3a3.5 3.5 0 0 1 5 0" />
                            <path d="m16.5 6.5 2.5-2.5a3.5 3.5 0 0 1 5 5l-3 3a3.5 3.5 0 0 1-5 0" />
                        </svg>
                    </span>
                    <strong>링크</strong>
                </button>
                <button type="button" :disabled="isPreparing" @click="emit('share')">
                    <span aria-hidden="true" class="certificate-share-sheet__more">•••</span>
                    <strong>더보기</strong>
                </button>
            </div>

            <p class="certificate-share-sheet__message" aria-live="polite">
                <span aria-hidden="true">ⓘ</span>
                {{ message }}
            </p>
        </div>
    </BaseBottomSheet>
</template>

<style scoped src="./PersonalCertificateShareSheet.css"></style>
