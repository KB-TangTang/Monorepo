<script setup>
import { computed, onUnmounted, ref, watch } from 'vue';
import defaultJudgeImage from '@/assets/images/judgment/judge_tangtang_default.png';
import raisedGavelImage from '@/assets/images/judgment/judge_tangtang_gavel_raised.png';
import strikeGavelImage from '@/assets/images/judgment/judge_tangtang_gavel_strike.png';

const props = defineProps({
    stage: { type: String, required: true },
    verdict: { type: Object, required: true },
});

const typedMessage = ref('');
let typingTimer = null;

function stopTyping() {
    if (typingTimer) window.clearInterval(typingTimer);
    typingTimer = null;
}

function startTyping() {
    stopTyping();
    typedMessage.value = '';
    let index = 0;
    typingTimer = window.setInterval(() => {
        typedMessage.value += props.verdict.reviewMessage[index] ?? '';
        index += 1;
        if (index >= props.verdict.reviewMessage.length) stopTyping();
    }, 26);
}

watch(
    () => [props.stage, props.verdict.reviewMessage],
    ([nextStage]) => {
        if (nextStage === 'intro') {
            stopTyping();
            typedMessage.value = '';
            return;
        }
        if (nextStage === 'review') startTyping();
    },
    { immediate: true },
);

onUnmounted(stopTyping);

const judgeImage = computed(() => {
    if (props.stage === 'raised') return raisedGavelImage;
    if (props.stage === 'strike' || props.stage === 'done') return strikeGavelImage;
    return defaultJudgeImage;
});

const stageLabel = computed(
    () =>
        ({
            intro: '판결 준비',
            review: '판사 탕이의 기록 검토',
            raised: '판결 직전',
            strike: '최종 판결',
            done: '판결 확정',
        })[props.stage],
);
</script>

<template>
    <section
        class="ai-animation"
        :class="[`ai-animation--${verdict.outcome}`, `ai-animation--${stage}`]"
        aria-live="polite"
    >
        <span class="ai-animation__stage">{{ stageLabel }}</span>
        <img :src="judgeImage" alt="판사 탕탕이" class="ai-animation__judge" />
        <div class="ai-animation__speech">
            <span class="ai-animation__speech-label">판사 탕이의 한마디</span>
            <span>{{ typedMessage }}</span
            ><span
                v-if="stage === 'review' && typedMessage.length < verdict.reviewMessage.length"
                class="ai-animation__cursor"
                aria-hidden="true"
            ></span>
        </div>
        <div
            class="ai-animation__verdict"
            :class="{ 'ai-animation__verdict--visible': stage === 'strike' || stage === 'done' }"
        >
            {{ verdict.label }}
        </div>
        <p class="ai-animation__foot">
            {{ stage === 'done' ? '탕이가 판결을 확정했어요.' : '탕이의 판결을 기다리고 있어요.' }}
        </p>
    </section>
</template>

<style scoped>
.ai-animation {
    flex: 1;
    display: grid;
    grid-template-rows: 18px 230px 108px 64px 24px;
    align-content: center;
    justify-items: center;
    gap: var(--tt-space-2);
    min-height: 0;
    text-align: center;
}

.ai-animation__stage {
    color: var(--tt-primary);
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-mono-chip);
    font-weight: var(--tt-fw-bold);
}

.ai-animation__judge {
    width: min(68vw, 250px);
    height: 230px;
    object-fit: contain;
}

.ai-animation--strike .ai-animation__judge {
    animation: judge-impact 0.25s ease-out;
}

.ai-animation__speech {
    position: relative;
    width: 100%;
    padding: var(--tt-space-4);
    color: var(--tt-text);
    background: var(--tt-bg);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-lg);
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-bold);
    line-height: var(--tt-lh-normal);
    text-align: left;
}

.ai-animation__speech::before {
    position: absolute;
    top: -7px;
    left: 50%;
    width: 12px;
    height: 12px;
    background: var(--tt-bg);
    border-top: 1px solid var(--tt-border);
    border-left: 1px solid var(--tt-border);
    content: '';
    transform: translateX(-50%) rotate(45deg);
}

.ai-animation__speech-label {
    display: block;
    margin-bottom: var(--tt-space-1);
    color: var(--tt-primary);
    font-size: var(--tt-fs-mono-chip);
}

.ai-animation__verdict {
    display: grid;
    height: 64px;
    padding: 0;
    box-sizing: border-box;
    place-items: center;
    width: 100%;
    border: 2px solid currentColor;
    border-radius: var(--tt-radius-md);
    font-size: var(--tt-fs-title);
    font-weight: var(--tt-fw-black);
    letter-spacing: -0.01em;
    visibility: hidden;
    opacity: 0;
}

.ai-animation__verdict--visible {
    visibility: visible;
    animation: ai-rise 0.2s ease-out both;
}

.ai-animation--GUILTY .ai-animation__verdict {
    color: var(--tt-danger);
    background: var(--tt-danger-subtle);
}

.ai-animation--INNOCENT .ai-animation__verdict {
    color: var(--tt-success);
    background: var(--tt-success-subtle);
}

.ai-animation__foot {
    align-self: start;
    color: var(--tt-text-muted);
    font-size: var(--tt-fs-caption);
}

.ai-animation__cursor {
    display: inline-block;
    width: 1px;
    height: 1em;
    margin-left: 2px;
    vertical-align: -0.14em;
    background: currentColor;
    animation: ai-blink 0.55s step-end infinite;
}

@keyframes ai-rise {
    from {
        opacity: 0;
        transform: translateY(10px) scale(0.97);
    }
    to {
        opacity: 1;
        transform: none;
    }
}

@keyframes judge-impact {
    50% {
        transform: translateY(3px);
    }
}

@keyframes ai-blink {
    50% {
        opacity: 0;
    }
}

@media (prefers-reduced-motion: reduce) {
    .ai-animation--strike .ai-animation__judge,
    .ai-animation__speech,
    .ai-animation__verdict {
        animation-duration: 0.01ms;
    }
}
</style>
