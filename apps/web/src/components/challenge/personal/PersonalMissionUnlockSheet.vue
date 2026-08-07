<script setup>
import BaseBottomSheet from '@/components/common/BaseBottomSheet.vue';
import BaseButton from '@/components/common/BaseButton.vue';
import { formatWon } from '@/services/personalMissionFlow';

const props = defineProps({
    modelValue: { type: Boolean, required: true },
    difficulty: { type: Object, required: true },
});

const emit = defineEmits(['update:modelValue', 'set-difficulty', 'view-mission']);

function chooseDifficulty() {
    emit('set-difficulty');
    emit('update:modelValue', false);
}

function viewMission() {
    emit('view-mission');
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
        <div class="mission-unlock">
            <div class="mission-unlock__judge">
                <img
                    src="@/assets/images/emotions/07_heart_eyes.png"
                    alt="맞춤 미션 개시를 축하하는 탕이"
                />
            </div>

            <span class="mission-unlock__badge">NEW · 맞춤 미션 개시</span>

            <h2>내 소비 기록으로 만든<br />맞춤 미션이 열렸어요</h2>
            <p class="mission-unlock__description">
                이제 공통 미션 대신, 나에게 딱 맞는<br />목표로 심판받아요.
            </p>

            <section class="mission-unlock__guide">
                <header>
                    <svg
                        viewBox="0 0 24 24"
                        fill="none"
                        stroke="currentColor"
                        stroke-width="1.8"
                        stroke-linecap="round"
                        stroke-linejoin="round"
                        aria-hidden="true"
                    >
                        <circle cx="10.5" cy="10.5" r="6.5" />
                        <path d="m15.5 15.5 4.5 4.5" />
                    </svg>
                    <strong>목표는 이렇게 정해져요</strong>
                </header>

                <p>
                    최근 28일간 카페 이용일의 지출 분포를 분석해, 선택한 난이도만큼 낮춘 금액을
                    목표로 삼아요.
                </p>

                <div class="mission-unlock__target">
                    <del>평소 6,800원</del>
                    <span aria-hidden="true">→</span>
                    <strong>목표 {{ formatWon(props.difficulty.targetAmount) }}</strong>
                </div>
            </section>

            <BaseButton size="lg" block @click="chooseDifficulty">난이도 설정하기</BaseButton>
            <button class="mission-unlock__later" type="button" @click="viewMission">
                지금 미션 보기
            </button>
        </div>
    </BaseBottomSheet>
</template>

<style scoped>
.mission-unlock {
    padding-bottom: var(--tt-space-2);
    text-align: center;
}

.mission-unlock__judge {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 112px;
    height: 112px;
    margin: 0 auto var(--tt-space-3);
    overflow: hidden;
    background: var(--tt-accent-subtle);
    border-radius: var(--tt-radius-full);
}

.mission-unlock__judge img {
    width: 92px;
    height: 92px;
    object-fit: contain;
}

.mission-unlock__badge {
    display: inline-flex;
    padding: var(--tt-space-2) var(--tt-space-3);
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-black);
    letter-spacing: 0.08em;
    color: var(--tt-info);
    background: var(--tt-info-subtle);
    border-radius: var(--tt-radius-full);
}

.mission-unlock h2 {
    margin: var(--tt-space-4) 0 0;
    font-size: var(--tt-fs-title);
    font-weight: var(--tt-fw-black);
    line-height: var(--tt-lh-tight);
}

.mission-unlock__description {
    margin: var(--tt-space-3) 0 var(--tt-space-5);
    color: var(--tt-text-muted);
    line-height: var(--tt-lh-normal);
}

.mission-unlock__guide {
    margin-bottom: var(--tt-space-4);
    padding: var(--tt-space-4);
    text-align: left;
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-lg);
}

.mission-unlock__guide header {
    display: flex;
    gap: var(--tt-space-2);
    align-items: center;
}

.mission-unlock__guide svg {
    flex: none;
    width: var(--tt-space-6);
    height: var(--tt-space-6);
    color: var(--tt-info);
}

.mission-unlock__guide p {
    margin: var(--tt-space-4) 0;
    color: var(--tt-text-muted);
    line-height: var(--tt-lh-normal);
}

.mission-unlock__target {
    display: flex;
    gap: var(--tt-space-3);
    align-items: center;
    justify-content: center;
    padding: var(--tt-space-4) var(--tt-space-3);
    font-family: var(--tt-font-mono);
    background: var(--tt-bg-subtle);
    border: 1px dashed var(--tt-border-strong);
    border-radius: var(--tt-radius-md);
}

.mission-unlock__target del,
.mission-unlock__target span {
    color: var(--tt-text-hint);
}

.mission-unlock__target strong {
    font-size: var(--tt-fs-section);
}

.mission-unlock__later {
    margin-top: var(--tt-space-3);
    padding: var(--tt-space-2);
    font: inherit;
    font-weight: var(--tt-fw-bold);
    color: var(--tt-info);
    background: transparent;
    border: 0;
    cursor: pointer;
}

@media (max-width: 359px) {
    :global(.tt-sheet__panel:has(.mission-unlock)) {
        padding-right: var(--tt-space-3);
        padding-left: var(--tt-space-3);
    }

    .mission-unlock__judge {
        width: 88px;
        height: 88px;
    }

    .mission-unlock__judge img {
        width: 72px;
        height: 72px;
    }

    .mission-unlock h2 {
        margin-top: var(--tt-space-3);
        font-size: var(--tt-fs-section);
    }

    .mission-unlock__description {
        margin-bottom: var(--tt-space-3);
    }

    .mission-unlock__guide {
        margin-bottom: var(--tt-space-3);
        padding: var(--tt-space-3);
    }

    .mission-unlock__guide p {
        margin: var(--tt-space-3) 0;
    }

    .mission-unlock__target {
        gap: var(--tt-space-2);
        padding: var(--tt-space-3) var(--tt-space-2);
    }
}
</style>
