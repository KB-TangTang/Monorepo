<script setup>
import { nextTick, onBeforeUnmount, ref } from 'vue';
import { useRouter } from 'vue-router';
import ChallengePageHeader from '@/components/challenge/ChallengePageHeader.vue';
import BaseButton from '@/components/common/BaseButton.vue';
import {
    calculateScratchProgress,
    SCRATCH_COMPLETION_RATIO,
} from '@/views/challenge/group/groupScratchProgress';

const router = useRouter();
const currentScreen = ref('balance');
const scratchComplete = ref(false);
const scratchProgress = ref(0);
const ticketRef = ref(null);
const canvasRef = ref(null);

let canvasContext = null;
let resizeObserver = null;
let isScratching = false;
let lastPoint = null;
let progressCheckCount = 0;

function getTokenValue(tokenName) {
    return getComputedStyle(document.documentElement).getPropertyValue(tokenName).trim();
}

function drawRoundedRect(context, x, y, width, height, radius) {
    const cornerRadius = Math.min(radius, width / 2, height / 2);
    context.beginPath();
    context.moveTo(x + cornerRadius, y);
    context.arcTo(x + width, y, x + width, y + height, cornerRadius);
    context.arcTo(x + width, y + height, x, y + height, cornerRadius);
    context.arcTo(x, y + height, x, y, cornerRadius);
    context.arcTo(x, y, x + width, y, cornerRadius);
    context.closePath();
}

function drawScratchCover() {
    const canvas = canvasRef.value;
    const ticket = ticketRef.value;

    if (!canvas || !ticket || scratchComplete.value) {
        return;
    }

    const ticketBounds = ticket.getBoundingClientRect();
    const pixelRatio = window.devicePixelRatio || 1;
    canvas.width = Math.round(ticketBounds.width * pixelRatio);
    canvas.height = Math.round(ticketBounds.height * pixelRatio);
    canvas.style.width = `${ticketBounds.width}px`;
    canvas.style.height = `${ticketBounds.height}px`;

    canvasContext = canvas.getContext('2d', { willReadFrequently: true });
    canvasContext.setTransform(pixelRatio, 0, 0, pixelRatio, 0, 0);
    canvasContext.globalCompositeOperation = 'source-over';
    canvasContext.fillStyle = getTokenValue('--tt-surface-strong');
    drawRoundedRect(canvasContext, 0, 0, ticketBounds.width, ticketBounds.height, 20);
    canvasContext.fill();

    canvasContext.fillStyle = getTokenValue('--tt-accent');
    canvasContext.textAlign = 'center';
    canvasContext.font = `800 12px ${getTokenValue('--tt-font-sans')}`;
    canvasContext.fillText('FINAL VERDICT', ticketBounds.width / 2, 38);

    canvasContext.fillStyle = getTokenValue('--tt-border-strong');
    drawRoundedRect(canvasContext, 24, 57, ticketBounds.width - 48, ticketBounds.height - 82, 14);
    canvasContext.fill();

    canvasContext.fillStyle = getTokenValue('--tt-text-muted');
    canvasContext.font = `700 15px ${getTokenValue('--tt-font-sans')}`;
    canvasContext.fillText(
        '여기를 긁어보세요',
        ticketBounds.width / 2,
        ticketBounds.height / 2 + 3,
    );
    canvasContext.fillStyle = getTokenValue('--tt-text-soft');
    canvasContext.font = `500 11px ${getTokenValue('--tt-font-sans')}`;
    canvasContext.fillText(
        '손가락으로 문질러 판결을 확인해요',
        ticketBounds.width / 2,
        ticketBounds.height / 2 + 23,
    );
}

function getPointerPosition(event) {
    const canvasBounds = canvasRef.value.getBoundingClientRect();
    return {
        x: event.clientX - canvasBounds.left,
        y: event.clientY - canvasBounds.top,
    };
}

function scratchAt(point) {
    if (!canvasContext || scratchComplete.value) {
        return;
    }

    const pixelRatio = window.devicePixelRatio || 1;
    canvasContext.save();
    canvasContext.setTransform(pixelRatio, 0, 0, pixelRatio, 0, 0);
    canvasContext.globalCompositeOperation = 'destination-out';
    canvasContext.lineCap = 'round';
    canvasContext.lineJoin = 'round';
    canvasContext.lineWidth = 36;
    canvasContext.beginPath();
    canvasContext.moveTo(lastPoint?.x ?? point.x, lastPoint?.y ?? point.y);
    canvasContext.lineTo(point.x, point.y);
    canvasContext.stroke();
    canvasContext.beginPath();
    canvasContext.arc(point.x, point.y, 18, 0, Math.PI * 2);
    canvasContext.fill();
    canvasContext.restore();

    lastPoint = point;
    progressCheckCount += 1;

    if (progressCheckCount % 5 === 0) {
        updateScratchProgress();
    }
}

function updateScratchProgress() {
    const canvas = canvasRef.value;

    if (!canvas || !canvasContext || scratchComplete.value) {
        return;
    }

    const pixels = canvasContext.getImageData(0, 0, canvas.width, canvas.height).data;
    let transparentSamples = 0;
    let totalSamples = 0;

    for (let alphaIndex = 3; alphaIndex < pixels.length; alphaIndex += 24) {
        totalSamples += 1;
        if (pixels[alphaIndex] < 90) {
            transparentSamples += 1;
        }
    }

    const transparentRatio = transparentSamples / totalSamples;
    scratchProgress.value = calculateScratchProgress(transparentSamples, totalSamples);

    if (transparentRatio >= SCRATCH_COMPLETION_RATIO) {
        revealVerdict();
    }
}

function startScratch(event) {
    if (scratchComplete.value) {
        return;
    }

    isScratching = true;
    lastPoint = getPointerPosition(event);
    canvasRef.value.setPointerCapture?.(event.pointerId);
    scratchAt(lastPoint);
}

function moveScratch(event) {
    if (!isScratching || scratchComplete.value) {
        return;
    }

    scratchAt(getPointerPosition(event));
}

function stopScratch() {
    if (!isScratching) {
        return;
    }

    isScratching = false;
    lastPoint = null;
    updateScratchProgress();
}

function revealVerdict() {
    if (scratchComplete.value) {
        return;
    }

    scratchComplete.value = true;
    scratchProgress.value = 100;
    isScratching = false;
}

function openScratchScreen() {
    scratchComplete.value = false;
    scratchProgress.value = 0;
    currentScreen.value = 'scratch';
}

async function handleScreenEntered() {
    if (currentScreen.value !== 'scratch') {
        return;
    }

    await nextTick();
    drawScratchCover();
    resizeObserver?.disconnect();
    resizeObserver = new ResizeObserver(drawScratchCover);
    resizeObserver.observe(ticketRef.value);
}

function goBack() {
    if (currentScreen.value === 'scratch') {
        resizeObserver?.disconnect();
        currentScreen.value = 'balance';
        return;
    }

    router.back();
}

function finishVerdict() {
    if (!scratchComplete.value) {
        revealVerdict();
        return;
    }

    currentScreen.value = 'balance';
}

onBeforeUnmount(() => {
    resizeObserver?.disconnect();
});
</script>

<template>
    <section class="group-verdict" aria-label="그룹 챌린지 동률 판결">
        <Transition name="group-verdict-screen" mode="out-in" @after-enter="handleScreenEntered">
            <div v-if="currentScreen === 'balance'" key="balance" class="group-verdict__screen">
                <ChallengePageHeader
                    title="동률 발생"
                    back-label="그룹 챌린지로 돌아가기"
                    @back="goBack"
                />

                <main class="group-verdict__content group-verdict__balance-content">
                    <span class="group-verdict__badge">최종 동률</span>
                    <h2>판결이 팽팽하게<br />갈렸어요</h2>
                    <p class="group-verdict__description">
                        마지막 표결은 스크래치 복권으로 결정해요.
                    </p>

                    <div class="group-verdict__character" aria-hidden="true">
                        <img src="@/assets/images/emotions/47_thinking.png" alt="" />
                    </div>

                    <article class="group-verdict__score-card" aria-label="무죄 3표, 유죄 3표">
                        <div class="group-verdict__scores">
                            <div>
                                <span class="group-verdict__innocent">무죄</span>
                                <strong>3</strong>
                            </div>
                            <b aria-hidden="true">:</b>
                            <div>
                                <span class="group-verdict__guilty">유죄</span>
                                <strong>3</strong>
                            </div>
                        </div>
                        <div class="group-verdict__score-line" aria-hidden="true">
                            <span></span>
                            <span></span>
                        </div>
                        <small>총 6표</small>
                    </article>

                    <aside class="group-verdict__guide">
                        <strong>동률 해결법</strong>
                        <p>
                            피고가 직접 복권을 긁어 최종 판결을 확인해요.<br />결과는 오직 행운에게
                            맡겨야 공평하니까요!
                        </p>
                    </aside>
                </main>

                <footer class="group-verdict__action">
                    <BaseButton
                        class="group-verdict__accent-button"
                        block
                        size="lg"
                        @click="openScratchScreen"
                    >
                        스크래치 복권 긁기
                    </BaseButton>
                </footer>
            </div>

            <div v-else key="scratch" class="group-verdict__screen">
                <ChallengePageHeader
                    title="스크래치 복권"
                    back-label="동률 결과로 돌아가기"
                    @back="goBack"
                />

                <main class="group-verdict__content group-verdict__scratch-content">
                    <span class="group-verdict__badge">스크래치 찬스</span>
                    <h2>가려진 판결을<br />확인해보세요</h2>
                    <p class="group-verdict__description">복권을 긁은 순간 최종 판결이 공개돼요.</p>

                    <section
                        ref="ticketRef"
                        class="group-verdict__ticket"
                        :class="{ 'group-verdict__ticket--revealed': scratchComplete }"
                    >
                        <div class="group-verdict__ticket-result" :aria-hidden="!scratchComplete">
                            <span>FINAL VERDICT</span>
                            <strong>유죄</strong>
                            <p>스크래치 재판으로 결정</p>
                        </div>

                        <canvas
                            v-if="!scratchComplete"
                            ref="canvasRef"
                            class="group-verdict__scratch-canvas"
                            role="img"
                            aria-label="손가락이나 마우스로 긁어서 판결 확인하기"
                            @pointerdown.prevent="startScratch"
                            @pointermove.prevent="moveScratch"
                            @pointerup="stopScratch"
                            @pointercancel="stopScratch"
                            @pointerleave="stopScratch"
                        ></canvas>

                        <button
                            v-if="!scratchComplete"
                            class="group-verdict__skip"
                            type="button"
                            @click="revealVerdict"
                        >
                            결과 바로 보기
                        </button>
                    </section>

                    <article
                        class="group-verdict__progress-card"
                        :class="{ 'group-verdict__progress-card--complete': scratchComplete }"
                        aria-live="polite"
                    >
                        <div class="group-verdict__progress-heading">
                            <div>
                                <span aria-hidden="true">✦</span>
                                <strong>{{
                                    scratchComplete ? '스크래치 완료' : '스크래치 진행'
                                }}</strong>
                            </div>
                            <b>{{ scratchProgress }}%</b>
                        </div>
                        <div
                            class="group-verdict__progress-track"
                            role="progressbar"
                            aria-label="스크래치 진행률"
                            aria-valuemin="0"
                            aria-valuemax="100"
                            :aria-valuenow="scratchProgress"
                        >
                            <span :style="{ width: `${scratchProgress}%` }"></span>
                        </div>
                        <p>
                            {{
                                scratchComplete
                                    ? '최종 판결이 공개됐어요.'
                                    : '손가락으로 판결 영역을 긁어주세요.'
                            }}
                        </p>
                    </article>
                </main>

                <footer class="group-verdict__action">
                    <BaseButton
                        :variant="scratchComplete ? 'danger' : 'primary'"
                        class="group-verdict__verdict-button"
                        block
                        size="lg"
                        @click="finishVerdict"
                    >
                        {{ scratchComplete ? '판결 확인 완료' : '최종 판결 확인하기' }}
                    </BaseButton>
                </footer>
            </div>
        </Transition>
    </section>
</template>

<style scoped src="./GroupChallengeVerdictView.css"></style>
