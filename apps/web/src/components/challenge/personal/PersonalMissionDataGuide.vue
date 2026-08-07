<script setup>
import { computed } from 'vue';
import BaseCard from '@/components/common/BaseCard.vue';

const props = defineProps({
    profile: {
        type: Object,
        required: true,
    },
});

const emit = defineEmits(['link-account']);

const transactionProgress = computed(() => {
    return Math.min(
        Math.round((props.profile.transactionCount / props.profile.requiredTransactionCount) * 100),
        100,
    );
});
</script>

<template>
    <div class="data-guide">
        <section class="data-guide__message">
            <img src="@/assets/images/emotions/47_thinking.png" alt="" />

            <div>
                <h2>아직 소비 기록이 적어<br />공통 미션만 드리고 있어요</h2>
                <p>기록이 쌓이면 내 소비에 딱 맞춘 맞춤 미션이 열려요.</p>
            </div>
        </section>

        <BaseCard padding="lg">
            <section class="data-guide__condition">
                <h2>맞춤 미션이 열리는 조건</h2>

                <div>
                    <span>
                        <svg viewBox="0 0 24 24" aria-hidden="true">
                            <path d="m5 12 4 4 10-10" />
                        </svg>
                    </span>
                    <strong>계좌 연동</strong>
                    <em>완료</em>
                </div>

                <div>
                    <span>
                        <svg viewBox="0 0 24 24" aria-hidden="true">
                            <circle cx="12" cy="12" r="8" />
                            <path d="M12 7v5l3 2" />
                        </svg>
                    </span>
                    <strong>최근 소비 데이터</strong>
                    <em>
                        {{ profile.availableTransactionDays }} /
                        {{ profile.requiredTransactionDays }}일
                    </em>
                </div>

                <div>
                    <span>
                        <svg viewBox="0 0 24 24" aria-hidden="true">
                            <circle cx="12" cy="12" r="8" />
                            <circle class="data-guide__dot" cx="8.5" cy="12" r="1.2" />
                            <circle class="data-guide__dot" cx="12" cy="12" r="1.2" />
                            <circle class="data-guide__dot" cx="15.5" cy="12" r="1.2" />
                        </svg>
                    </span>
                    <strong>전체 소비 내역 확보</strong>
                    <em>
                        {{ profile.transactionCount }} /
                        {{ profile.requiredTransactionCount }}
                    </em>
                </div>

                <div class="data-guide__progress">
                    <span :style="{ width: `${transactionProgress}%` }"></span>
                </div>

                <button type="button" @click="emit('link-account')">계좌 더 연동하기 ›</button>
            </section>
        </BaseCard>
    </div>
</template>

<style scoped>
.data-guide {
    --personalization: #725a98;
    --personalization-deep: #533d75;
    --personalization-subtle: #f1ecf8;
    --personalization-border: #ded2ed;

    display: flex;
    flex-direction: column;
    gap: var(--tt-space-4);
}

.data-guide__message {
    display: flex;
    gap: var(--tt-space-4);
    align-items: center;
    padding: var(--tt-space-5);
    color: var(--personalization-deep);
    background: var(--personalization-subtle);
    border: 1px solid var(--personalization-border);
    border-radius: var(--tt-radius-lg);
}

.data-guide__message img {
    width: 76px;
    height: 76px;
    object-fit: contain;
}

.data-guide__message h2 {
    margin: 0;
    font-size: var(--tt-fs-section);
}

.data-guide__message p {
    margin: var(--tt-space-2) 0 0;
    font-size: var(--tt-fs-caption);
    color: var(--personalization);
}

.data-guide__condition h2 {
    margin: 0 0 var(--tt-space-4);
    font-size: var(--tt-fs-section);
}

.data-guide__condition > div:not(.data-guide__progress) {
    display: flex;
    gap: var(--tt-space-3);
    align-items: center;
    margin-top: var(--tt-space-3);
}

.data-guide__condition em {
    margin-left: auto;
    font-style: normal;
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-muted);
}

.data-guide__condition > div:nth-of-type(1) span {
    color: var(--tt-success-deep);
    background: var(--tt-success-subtle);
}

.data-guide__condition > div:nth-of-type(1) em {
    color: var(--tt-success-deep);
}

.data-guide__condition > div:nth-of-type(2) span {
    color: var(--tt-accent-deep);
    background: var(--tt-accent-subtle);
}

.data-guide__condition > div:nth-of-type(2) em {
    color: var(--tt-accent-deep);
}

.data-guide__condition > div:nth-of-type(3) span {
    color: var(--tt-info-deep);
    background: var(--tt-info-subtle);
}

.data-guide__condition > div:not(.data-guide__progress) span {
    display: inline-flex;
    flex: 0 0 36px;
    align-items: center;
    justify-content: center;
    width: 36px;
    height: 36px;
    border-radius: var(--tt-radius-md);
}

.data-guide__condition svg {
    width: 20px;
    height: 20px;
    fill: none;
    stroke: currentColor;
    stroke-width: 2.4;
    stroke-linecap: round;
    stroke-linejoin: round;
}

.data-guide__condition svg .data-guide__dot {
    fill: currentColor;
    stroke: none;
}

.data-guide__progress {
    height: 8px;
    margin-top: var(--tt-space-4);
    overflow: hidden;
    background: var(--tt-border-track);
    border-radius: var(--tt-radius-full);
}

.data-guide__progress span {
    display: block;
    height: 100%;
    background: var(--tt-info);
}

.data-guide__condition button {
    display: block;
    margin: var(--tt-space-4) auto 0;
    font: inherit;
    font-weight: var(--tt-fw-bold);
    color: var(--tt-info);
    background: transparent;
    border: 0;
    cursor: pointer;
}
</style>
