<script setup>
import { ref, computed } from 'vue';
import BaseModal from '@/components/common/BaseModal.vue';
import { formatWon } from '@/services/personalMissionFlow';

const props = defineProps({
    modelValue: { type: Boolean, required: true },
    verdict: { type: Object, default: null },
    isAcknowledging: { type: Boolean, default: false },
    errorMessage: { type: String, default: '' },
});

const emit = defineEmits(['update:modelValue', 'acknowledge']);

const isEvidenceOpen = ref(false);

const isSuccess = computed(() => props.verdict?.type === 'SUCCESS');
const evidenceTitle = computed(() => {
    const parts = ['판정 근거 보기'];
    if (props.verdict?.categoryName) parts.push(props.verdict.categoryName);
    parts.push(`거래 ${props.verdict?.transactions?.length ?? 0}건`);
    return parts.join(' · ');
});
const gaugeWidth = computed(() => {
    const currentAmount = Number(props.verdict?.currentAmount) || 0;
    const limitAmount = Number(props.verdict?.limitAmount) || 0;
    if (limitAmount <= 0) return 100;
    return Math.min((currentAmount / limitAmount) * 100, 100);
});

function toggleEvidence() {
    isEvidenceOpen.value = !isEvidenceOpen.value;
}

function acknowledge() {
    emit('acknowledge');
}
</script>

<template>
    <BaseModal
        :model-value="modelValue"
        :show-close="false"
        :close-on-overlay="false"
        :close-on-esc="false"
        @update:model-value="$emit('update:modelValue', $event)"
    >
        <template #header><span></span></template>

        <div v-if="verdict" class="verdict">
            <!-- 판정 뱃지 -->
            <div class="verdict__badge-row">
                <span
                    class="verdict__type-badge"
                    :class="
                        isSuccess ? 'verdict__type-badge--success' : 'verdict__type-badge--fail'
                    "
                >
                    {{ isSuccess ? '알리바이 인정' : '알리바이 불인정' }}
                </span>
                <span class="verdict__date">{{ verdict.date }} 판정</span>
            </div>

            <!-- 탕이 + 말풍선 -->
            <div class="verdict__tangi-row">
                <img :src="verdict.tangiImage" alt="" class="verdict__tangi-img" />
                <div class="verdict__speech">
                    <div class="verdict__speech-arrow"></div>
                    <div class="verdict__speech-text">{{ verdict.tangiQuote }}</div>
                </div>
            </div>

            <!-- 게이지 -->
            <div class="verdict__gauge-box">
                <div class="verdict__gauge-amounts">
                    <span class="verdict__gauge-current">
                        {{ formatWon(verdict.currentAmount) }} /
                        {{ formatWon(verdict.limitAmount) }}
                    </span>
                    <span
                        class="verdict__gauge-diff"
                        :class="isSuccess ? 'verdict__gauge-diff--ok' : 'verdict__gauge-diff--over'"
                    >
                        {{
                            isSuccess
                                ? `여유 ${formatWon(verdict.remainAmount)}`
                                : `+${formatWon(verdict.overAmount)} 초과`
                        }}
                    </span>
                </div>
                <div class="verdict__gauge-track">
                    <div
                        class="verdict__gauge-fill"
                        :class="isSuccess ? 'verdict__gauge-fill--ok' : 'verdict__gauge-fill--over'"
                        :style="{ width: `${gaugeWidth}%` }"
                    ></div>
                </div>
            </div>

            <!-- 판정 근거 -->
            <div class="verdict__evidence-card">
                <button
                    type="button"
                    class="verdict__evidence-toggle"
                    :aria-expanded="isEvidenceOpen"
                    @click="toggleEvidence"
                >
                    <span class="verdict__evidence-title">{{ evidenceTitle }}</span>
                    <svg
                        width="10"
                        height="6"
                        viewBox="0 0 10 6"
                        fill="none"
                        class="verdict__evidence-chevron"
                        :class="{ 'verdict__evidence-chevron--open': isEvidenceOpen }"
                    >
                        <path
                            d="M1 1l4 4 4-4"
                            stroke="currentColor"
                            stroke-width="1.8"
                            stroke-linecap="round"
                            stroke-linejoin="round"
                        />
                    </svg>
                </button>

                <div v-if="isEvidenceOpen" class="verdict__evidence-list">
                    <div
                        v-for="tx in verdict.transactions"
                        :key="tx.id ?? tx.name"
                        class="verdict__tx-row"
                    >
                        <span class="verdict__tx-name">{{ tx.name }}</span>
                        <span v-if="tx.manuallyEdited" class="verdict__tx-edited">직접 수정</span>
                        <span class="verdict__tx-spacer"></span>
                        <span class="verdict__tx-amount">{{ formatWon(tx.amount) }}</span>
                    </div>
                    <p class="verdict__evidence-note">
                        판정은 자정에 확정되며 이후 카테고리를 수정해도 결과는 바뀌지 않아요. 수정
                        내용은 다음 사건부터 반영됩니다.
                    </p>
                </div>
            </div>

            <!-- 점수 뱃지 -->
            <div class="verdict__score-badges">
                <template v-if="isSuccess">
                    <span class="verdict__score-badge verdict__score-badge--gold">
                        +{{ verdict.points }}점
                    </span>
                    <span
                        v-if="verdict.bonusPoints"
                        class="verdict__score-badge verdict__score-badge--gold"
                    >
                        보너스 +{{ verdict.bonusPoints }}점
                    </span>
                    <span class="verdict__score-badge verdict__score-badge--green">
                        연속 인정 {{ verdict.streakDays }}일
                    </span>
                </template>
                <template v-else>
                    <span class="verdict__score-badge verdict__score-badge--muted"> 획득 0점 </span>
                    <span class="verdict__score-badge verdict__score-badge--red">
                        연속 인정 리셋
                    </span>
                </template>
            </div>

            <!-- 명예회복 메시지 (실패 시) -->
            <div v-if="!isSuccess" class="verdict__recovery">
                벌점은 없어요. 점수를 못 받았을 뿐.<br />오늘 사건으로 명예 회복 기회를 드리죠.
            </div>

            <!-- 확인 CTA -->
            <p v-if="errorMessage" class="verdict__error" role="alert">{{ errorMessage }}</p>
            <button
                type="button"
                class="verdict__confirm"
                :disabled="isAcknowledging"
                @click="acknowledge"
            >
                {{ isAcknowledging ? '저장 중...' : '확인했습니다' }}
            </button>
        </div>
    </BaseModal>
</template>

<style scoped>
.verdict {
    text-align: center;
}

.verdict__badge-row {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: var(--tt-space-2);
}

.verdict__type-badge {
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-black);
    padding: 5px var(--tt-space-3);
    border-radius: var(--tt-radius-full);
}

.verdict__type-badge--success {
    background: var(--tt-success-subtle);
    color: var(--tt-success);
}

.verdict__type-badge--fail {
    background: var(--tt-danger-subtle);
    color: var(--tt-danger-deep);
}

.verdict__date {
    font-size: 10.5px;
    color: var(--tt-text-hint);
    font-weight: var(--tt-fw-bold);
    font-family: var(--tt-font-mono);
}

.verdict__tangi-row {
    margin-top: var(--tt-space-3);
    display: flex;
    align-items: center;
    gap: 2%;
    text-align: left;
}

.verdict__tangi-img {
    width: 27%;
    aspect-ratio: 1;
    object-fit: contain;
    flex: none;
    animation: tt-verdict-float 3.4s ease-in-out infinite;
}

@keyframes tt-verdict-float {
    0%,
    100% {
        transform: translateY(0);
    }
    50% {
        transform: translateY(-5px);
    }
}

.verdict__speech {
    flex: 1;
    min-width: 0;
    background: var(--tt-bg);
    border-radius: var(--tt-radius-lg);
    padding: var(--tt-space-3) 14px;
    box-shadow: 0 10px 22px -12px rgba(35, 40, 66, 0.35);
    position: relative;
}

.verdict__speech-arrow {
    position: absolute;
    left: -10px;
    top: 50%;
    transform: translateY(-50%);
    width: 0;
    height: 0;
    border-top: 8px solid transparent;
    border-bottom: 8px solid transparent;
    border-right: 11px solid var(--tt-bg);
}

.verdict__speech-text {
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-black);
    line-height: 1.5;
    letter-spacing: -0.01em;
    word-break: keep-all;
}

.verdict__gauge-box {
    margin-top: 11px;
    background: var(--tt-bg);
    border: 1px dashed var(--tt-border-divider);
    border-radius: var(--tt-radius-md);
    padding: 10px 13px;
}

.verdict__gauge-amounts {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
    margin-top: 4px;
}

.verdict__gauge-current {
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-body);
    font-family: var(--tt-font-mono);
    white-space: nowrap;
}

.verdict__gauge-diff {
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-black);
    font-family: var(--tt-font-mono);
    white-space: nowrap;
}

.verdict__gauge-diff--ok {
    color: var(--tt-success);
}

.verdict__gauge-diff--over {
    color: var(--tt-danger-deep);
}

.verdict__gauge-track {
    margin-top: var(--tt-space-2);
    height: 7px;
    border-radius: var(--tt-radius-full);
    background: var(--tt-border-track);
    overflow: hidden;
}

.verdict__gauge-fill {
    height: 100%;
    border-radius: var(--tt-radius-full);
    transition: width 0.4s ease;
}

.verdict__gauge-fill--ok {
    background: var(--tt-success);
}

.verdict__gauge-fill--over {
    background: var(--tt-danger);
}

.verdict__evidence-card {
    margin-top: 9px;
    background: var(--tt-bg);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-md);
    overflow: hidden;
}

.verdict__evidence-toggle {
    width: 100%;
    background: var(--tt-bg);
    border: 0;
    padding: 10px 13px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    cursor: pointer;
    font-family: var(--tt-font-sans);
}

.verdict__evidence-toggle:focus {
    outline: none;
}

.verdict__evidence-toggle:active {
    background: var(--tt-bg-fill);
}

.verdict__evidence-title {
    font-size: 11.5px;
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
}

.verdict__evidence-chevron {
    color: var(--tt-text-muted);
    transition: transform 0.2s ease;
}

.verdict__evidence-chevron--open {
    transform: rotate(180deg);
}

.verdict__evidence-list {
    border-top: 1px solid var(--tt-border);
    padding: 10px 13px;
    text-align: left;
}

.verdict__tx-row {
    display: flex;
    align-items: center;
    gap: var(--tt-space-2);
    padding: 3px 0;
}

.verdict__tx-name {
    font-size: 11.5px;
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-body);
}

.verdict__tx-edited {
    background: var(--tt-info-subtle);
    color: var(--tt-info);
    font-size: 9.5px;
    font-weight: var(--tt-fw-black);
    padding: 2px 6px;
    border-radius: var(--tt-radius-full);
    white-space: nowrap;
}

.verdict__tx-spacer {
    flex: 1;
}

.verdict__tx-amount {
    font-size: 11.5px;
    font-weight: var(--tt-fw-black);
    font-family: var(--tt-font-mono);
}

.verdict__evidence-note {
    margin-top: var(--tt-space-2);
    border-top: 1px dashed var(--tt-border);
    padding-top: 7px;
    font-size: 10px;
    color: var(--tt-text-hint);
    font-weight: var(--tt-fw-semibold);
    line-height: 1.5;
}

.verdict__score-badges {
    margin-top: 9px;
    display: flex;
    justify-content: center;
    gap: 7px;
    flex-wrap: wrap;
}

.verdict__score-badge {
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-black);
    padding: 6px var(--tt-space-3);
    border-radius: var(--tt-radius-full);
    font-family: var(--tt-font-mono);
    white-space: nowrap;
}

.verdict__score-badge--gold {
    background: var(--tt-accent-subtle);
    color: var(--tt-accent-deep);
}

.verdict__score-badge--green {
    background: var(--tt-success-subtle);
    color: var(--tt-success);
}

.verdict__score-badge--muted {
    background: var(--tt-bg-fill);
    color: var(--tt-text-body);
}

.verdict__score-badge--red {
    background: var(--tt-danger-subtle);
    color: var(--tt-danger-deep);
}

.verdict__recovery {
    margin-top: var(--tt-space-3);
    font-size: 11.5px;
    color: var(--tt-accent-warn);
    font-weight: var(--tt-fw-semibold);
    background: var(--tt-accent-subtle);
    border: 1px solid var(--tt-accent-subtle-border);
    border-radius: 11px;
    padding: 9px 11px;
    line-height: 1.5;
}

.verdict__confirm {
    margin-top: var(--tt-space-3);
    width: 100%;
    background: var(--tt-surface-inverse);
    color: var(--tt-text-inverse);
    font-weight: var(--tt-fw-black);
    font-size: var(--tt-fs-button);
    padding: 14px;
    border-radius: 13px;
    border: none;
    cursor: pointer;
    box-shadow: var(--tt-elevation-4);
    font-family: var(--tt-font-sans);
}

.verdict__confirm:disabled {
    cursor: wait;
    opacity: 0.7;
}

.verdict__error {
    margin-top: var(--tt-space-3);
    color: var(--tt-danger);
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
}
</style>
