<script setup>
import BaseBottomSheet from '@/components/common/BaseBottomSheet.vue';
import BaseButton from '@/components/common/BaseButton.vue';

defineProps({
    modelValue: {
        type: Boolean,
        required: true,
    },
});

const emit = defineEmits(['update:modelValue', 'agree', 'later']);

function agree() {
    emit('agree');
    emit('update:modelValue', false);
}

function later() {
    emit('later');
    emit('update:modelValue', false);
}
</script>

<template>
    <BaseBottomSheet
        :model-value="modelValue"
        :close-on-overlay="false"
        :close-on-esc="false"
        @update:model-value="emit('update:modelValue', $event)"
    >
        <div class="consent">
            <div class="consent__judge">
                <img
                    src="@/assets/images/emotions/48_judging.png"
                    alt="개인 미션 챌린지를 안내하는 탕탕이"
                />
            </div>

            <h2>개인 미션 챌린지,<br />시작할까요?</h2>

            <p class="consent__description">
                매일 자정, 어제의 소비를 분석해요.<br />
                탕탕이가 오늘의 개인 미션을 배정해 드려요.
            </p>

            <section class="consent__benefits">
                <div>
                    <span class="consent__icon consent__icon--mission">
                        <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
                            <path class="consent__gavel-head" d="m13 4 4 4M5 12l4 4" />
                            <path d="m9 8 10 10" />
                        </svg>
                    </span>

                    <p>
                        <strong>매일 개인 미션 1건 배정</strong>
                        <small>내 소비 습관에 맞춰 자동 출제</small>
                    </p>
                </div>

                <div>
                    <span class="consent__icon consent__icon--honor">
                        <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
                            <path
                                d="m12 3 2.7 5.5 6.1.9-4.4 4.3 1 6.1-5.4-2.9-5.4 2.9 1-6.1-4.4-4.3 6.1-.9z"
                            />
                        </svg>
                    </span>

                    <p>
                        <strong>성공하면 명예 점수 적립</strong>
                        <small>연속 성공에는 보너스 점수</small>
                    </p>
                </div>

                <div>
                    <span class="consent__icon consent__icon--ranking">
                        <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
                            <path d="M8 3h3l1 5-3 2-3-7h2M16 3h-3l-1 5 3 2 3-7z" />
                            <circle cx="12" cy="15" r="6" />
                            <path
                                d="m12 11 1.2 2.4 2.7.4-2 1.9.5 2.7-2.4-1.3-2.4 1.3.5-2.7-2-1.9 2.7-.4z"
                            />
                        </svg>
                    </span>

                    <p>
                        <strong>월간 개인 챌린지 랭킹</strong>
                        <small>전체 참여자 중 내 백분위 확인</small>
                    </p>
                </div>
            </section>

            <label class="consent__agreement">
                <input type="checkbox" checked disabled />

                <span>
                    <strong>개인 챌린지·랭킹 참여에 동의합니다.</strong>
                    <small> 미동의 시에도 자산·장부 기능은 그대로 이용할 수 있어요. </small>
                </span>
            </label>

            <BaseButton size="lg" block @click="agree"> 동의하고 챌린지 시작 </BaseButton>

            <button class="consent__later" type="button" @click="later">다음에 할게요</button>
        </div>
    </BaseBottomSheet>
</template>

<style scoped>
.consent {
    padding-bottom: var(--tt-space-2);
    text-align: center;
}

.consent__judge {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 104px;
    height: 104px;
    margin: 0 auto var(--tt-space-4);
    overflow: hidden;
    background: var(--tt-primary-subtle);
    border-radius: var(--tt-radius-full);
}

.consent__judge img {
    width: 82px;
    height: 82px;
    object-fit: contain;
}

.consent h2 {
    margin: 0;
    font-size: var(--tt-fs-title);
    font-weight: var(--tt-fw-black);
    line-height: var(--tt-lh-tight);
}

.consent__description {
    margin: var(--tt-space-3) 0 var(--tt-space-5);
    line-height: var(--tt-lh-normal);
    color: var(--tt-text-muted);
}

.consent__benefits {
    margin-bottom: var(--tt-space-4);
    overflow: hidden;
    text-align: left;
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-lg);
}

.consent__benefits > div {
    display: flex;
    gap: var(--tt-space-3);
    align-items: center;
    padding: var(--tt-space-3) var(--tt-space-4);
}

.consent__benefits > div + div {
    border-top: 1px solid var(--tt-border);
}

.consent__icon {
    display: flex;
    flex: 0 0 40px;
    align-items: center;
    justify-content: center;
    width: 40px;
    height: 40px;
    border-radius: var(--tt-radius-md);
}

.consent__icon svg {
    width: var(--tt-space-6);
    height: var(--tt-space-6);
    stroke: currentColor;
    stroke-width: 1.8;
    stroke-linecap: round;
    stroke-linejoin: round;
}

.consent__icon--mission {
    color: var(--tt-info);
    background: var(--tt-info-subtle);
}

.consent__icon--mission svg {
    stroke-width: 3.4;
}

.consent__icon--mission .consent__gavel-head {
    stroke-width: 5;
}

.consent__icon--honor {
    color: var(--tt-accent-deep);
    background: var(--tt-accent-subtle);
}

.consent__icon--ranking {
    color: var(--tt-success-deep);
    background: var(--tt-success-subtle);
}

.consent__benefits p {
    display: flex;
    flex-direction: column;
    margin: 0;
}

.consent__benefits small {
    margin-top: var(--tt-space-1);
    color: var(--tt-text-muted);
}

.consent__agreement {
    display: flex;
    gap: var(--tt-space-3);
    margin-bottom: var(--tt-space-4);
    padding: var(--tt-space-3);
    text-align: left;
    background: var(--tt-bg-subtle);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-md);
}

.consent__agreement input {
    flex: 0 0 20px;
    width: 20px;
    height: 20px;
    accent-color: var(--tt-primary);
}

.consent__agreement span {
    display: flex;
    min-width: 0;
    flex-direction: column;
}

.consent__agreement strong,
.consent__agreement small {
    overflow-wrap: anywhere;
}

.consent__agreement small {
    margin-top: var(--tt-space-1);
    color: var(--tt-text-muted);
}

.consent__later {
    margin-top: var(--tt-space-3);
    padding: var(--tt-space-2);
    font: inherit;
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-muted);
    background: transparent;
    border: 0;
    cursor: pointer;
}

@media (max-width: 390px) {
    :global(.tt-sheet__panel:has(.consent)) {
        padding-right: var(--tt-space-4);
        padding-left: var(--tt-space-4);
    }

    .consent__agreement {
        gap: var(--tt-space-2);
        padding: var(--tt-space-3);
    }
}

@media (max-width: 359px) {
    :global(.tt-sheet__panel:has(.consent)) {
        padding-right: var(--tt-space-3);
        padding-left: var(--tt-space-3);
    }

    .consent__judge {
        width: 88px;
        height: 88px;
    }

    .consent__judge img {
        width: 70px;
        height: 70px;
    }

    .consent__benefits > div {
        gap: var(--tt-space-2);
        padding: var(--tt-space-3);
    }

    .consent__agreement {
        padding: var(--tt-space-2);
    }
}
</style>
