<!--
  02d · 변론 작성 (F 입력형) — GC_07_02c
  기소 거래 요약 아코디언 + 변론 텍스트(150자) + 증빙 이미지(최대 3장).
  제출 시 defenseDone 으로 이동한다.
-->
<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue';
import { useRouter, useRoute } from 'vue-router';

import DefenseCourtHeader from '@/components/challenge/group/DefenseCourtHeader.vue';
import BaseButton from '@/components/common/BaseButton.vue';

import mascotPlease from '@/assets/images/emotions/44_please.png';

const router = useRouter();
const route = useRoute();

const MAX_TEXT = 150;
const MAX_IMAGES = 3;

/* ── mock 데이터 (API 연동 전) ── */
const indictment = ref({
    id: route.params.indictmentId || 'IND-001',
    challengeName: '배달 소비 줄이기',
    evalType: 'DAILY',
    limitAmount: 25000,
    currentAmount: 37400,
    exceededAmount: 12400,
    transaction: {
        merchantName: '배달의민족',
        amount: 24000,
        time: '오늘 18:42',
        category: '음식/배달',
        paymentMethod: '카드 결제',
    },
    actualCost: 8000,
    defenseDeadlineLabel: '오늘 22:00',
});

/* ── 부담금 반영 결과 (02a 에서 넘어온 값, mock) ── */
const adjustedTotal = computed(() => {
    const base = indictment.value.currentAmount - indictment.value.transaction.amount;
    return base + indictment.value.actualCost;
});

const isSafe = computed(() => adjustedTotal.value <= indictment.value.limitAmount);

const marginOrExceeded = computed(() => {
    const diff = indictment.value.limitAmount - adjustedTotal.value;
    if (diff >= 0) return { safe: true, amount: diff };
    return { safe: false, amount: Math.abs(diff) };
});

/* ── 마감 타이머 ── */
const remainingSeconds = ref(8043);
let timerHandle = null;

const timerLabel = computed(() => {
    const h = String(Math.floor(remainingSeconds.value / 3600)).padStart(2, '0');
    const m = String(Math.floor((remainingSeconds.value % 3600) / 60)).padStart(2, '0');
    const s = String(remainingSeconds.value % 60).padStart(2, '0');
    return `마감 ${h}:${m}:${s}`;
});

onMounted(() => {
    timerHandle = setInterval(() => {
        if (remainingSeconds.value > 0) remainingSeconds.value--;
    }, 1000);
});

onBeforeUnmount(() => {
    clearInterval(timerHandle);
});

/* ── 기소 거래 아코디언 ── */
const citeOpen = ref(false);

function toggleCite() {
    citeOpen.value = !citeOpen.value;
}

/* ── 변론 텍스트 ── */
const defenseText = ref('');

const charCount = computed(() => defenseText.value.length);

function onInput(e) {
    if (e.target.value.length > MAX_TEXT) {
        e.target.value = e.target.value.slice(0, MAX_TEXT);
    }
    defenseText.value = e.target.value;
}

/* ── 증빙 이미지 ── */
const images = ref([]);
const fileInput = ref(null);

function triggerUpload() {
    if (images.value.length >= MAX_IMAGES) return;
    fileInput.value?.click();
}

function onFileChange(e) {
    const files = e.target.files;
    if (!files?.length) return;
    for (const file of files) {
        if (images.value.length >= MAX_IMAGES) break;
        if (!file.type.startsWith('image/')) continue;
        images.value.push({
            file,
            name: file.name,
            url: URL.createObjectURL(file),
        });
    }
    e.target.value = '';
}

function removeImage(index) {
    const removed = images.value.splice(index, 1);
    if (removed[0]?.url) URL.revokeObjectURL(removed[0].url);
}

/* ── 제출 ── */
const canSubmit = computed(() => defenseText.value.trim().length > 0);

function submitDefense() {
    if (!canSubmit.value) return;
    router.push({
        name: 'defenseDone',
        params: {
            id: route.params.id,
            indictmentId: route.params.indictmentId,
        },
    });
}
</script>

<template>
    <div class="write-page">
        <!-- ── Court Bar 헤더 ── -->
        <DefenseCourtHeader>
            <template #nav-right>
                <span class="write-page__timer">{{ timerLabel }}</span>
            </template>

            <div class="write-page__header-body">
                <h2 class="write-page__headline">
                    지민님의 변론을<br>작성해주세요
                </h2>
                <img
                    :src="mascotPlease"
                    alt="탕이"
                    class="write-page__header-mascot"
                >
            </div>
        </DefenseCourtHeader>

        <!-- ── 본문 ── -->
        <div class="write-page__content">
            <!-- 기소 거래 아코디언 -->
            <div class="write-page__cite-card">
                <div class="write-page__cite-header" @click="toggleCite">
                    <span class="write-page__cite-badge">기소 거래</span>
                    <span class="write-page__cite-summary">
                        {{ indictment.transaction.merchantName }}
                        {{ indictment.transaction.amount.toLocaleString() }}원
                    </span>
                    <svg
                        class="write-page__cite-chevron"
                        :class="{ 'write-page__cite-chevron--open': citeOpen }"
                        width="12"
                        height="8"
                        viewBox="0 0 12 8"
                        fill="none"
                    >
                        <path
                            d="M1 1.5L6 6.5l5-5"
                            stroke="#8A8FA3"
                            stroke-width="2"
                            stroke-linecap="round"
                            stroke-linejoin="round"
                        />
                    </svg>
                </div>

                <Transition name="accordion">
                    <div v-if="citeOpen" class="write-page__cite-body">
                        <div class="write-page__cite-divider"></div>
                        <div class="write-page__cite-rows">
                            <div class="write-page__cite-row">
                                <span class="write-page__cite-label">결제 시각</span>
                                <span class="write-page__cite-value">
                                    {{ indictment.transaction.time }}
                                </span>
                            </div>
                            <div class="write-page__cite-row">
                                <span class="write-page__cite-label">결제 금액</span>
                                <span class="write-page__cite-value write-page__cite-value--bold">
                                    {{ indictment.transaction.amount.toLocaleString() }}원
                                </span>
                            </div>
                            <div class="write-page__cite-row">
                                <span class="write-page__cite-label write-page__cite-label--dark">
                                    실제 개인 부담금
                                </span>
                                <span class="write-page__cite-value write-page__cite-value--blue">
                                    {{ indictment.actualCost.toLocaleString() }}원
                                </span>
                            </div>
                        </div>
                        <div
                            v-if="isSafe"
                            class="write-page__cite-safe-badge"
                        >
                            반영 시 기준 내 · {{ marginOrExceeded.amount.toLocaleString() }}원 여유
                        </div>
                    </div>
                </Transition>
            </div>

            <!-- 나의 변론 -->
            <div class="write-page__text-section">
                <div class="write-page__text-label">나의 변론</div>
                <div class="write-page__textarea-wrap">
                    <textarea
                        class="write-page__textarea"
                        :value="defenseText"
                        :maxlength="MAX_TEXT"
                        placeholder="사정을 설명해주세요..."
                        @input="onInput"
                    ></textarea>
                    <span class="write-page__char-count">
                        {{ charCount }} / {{ MAX_TEXT }}
                    </span>
                </div>
            </div>

            <!-- 증빙 자료 -->
            <div class="write-page__evidence-section">
                <div class="write-page__evidence-header">
                    <span class="write-page__evidence-label">증빙 자료</span>
                    <span class="write-page__evidence-hint">
                        최대 {{ MAX_IMAGES }}장 · 이체 내역도 가능
                    </span>
                </div>
                <div class="write-page__evidence-grid">
                    <!-- 업로드된 이미지 -->
                    <div
                        v-for="(img, idx) in images"
                        :key="img.name + idx"
                        class="write-page__evidence-thumb"
                    >
                        <img
                            :src="img.url"
                            :alt="img.name"
                            class="write-page__evidence-img"
                        >
                        <button
                            type="button"
                            class="write-page__evidence-remove"
                            aria-label="삭제"
                            @click="removeImage(idx)"
                        >
                            &times;
                        </button>
                        <span class="write-page__evidence-name">{{ img.name }}</span>
                    </div>

                    <!-- 추가 슬롯 -->
                    <button
                        v-for="n in (MAX_IMAGES - images.length)"
                        :key="'add-' + n"
                        type="button"
                        class="write-page__evidence-add"
                        @click="triggerUpload"
                    >
                        <span class="write-page__evidence-plus">+</span>
                        <span class="write-page__evidence-add-label">추가</span>
                    </button>
                </div>
                <input
                    ref="fileInput"
                    type="file"
                    accept="image/*"
                    hidden
                    @change="onFileChange"
                >
            </div>

            <!-- 수정 불가 경고 -->
            <div class="write-page__warn-card">
                <div class="write-page__warn-icon">
                    <span class="write-page__warn-bang">!</span>
                </div>
                <div class="write-page__warn-text">
                    <b class="write-page__warn-strong">
                        한 번 제출한 변론은 수정할 수 없어요.
                    </b>
                    부담금과 증빙까지 확인한 뒤 제출해주세요.
                </div>
            </div>
        </div>

        <!-- ── 하단 제출 버튼 ── -->
        <div class="write-page__footer">
            <BaseButton
                variant="dark"
                size="lg"
                block
                :disabled="!canSubmit"
                @click="submitDefense"
            >
                변론 제출하기
            </BaseButton>
        </div>
    </div>
</template>

<style scoped>
/* ── 페이지 레이아웃 ── */
.write-page {
    display: flex;
    flex-direction: column;
    min-height: 100vh;
    min-height: 100dvh;
    background: var(--tt-bg-subtle);
}

/* ── 헤더 타이머 뱃지 ── */
.write-page__timer {
    background: rgba(224, 102, 75, 0.2);
    color: #FF9E86;
    font-size: 11.5px;
    font-weight: var(--tt-fw-black);
    padding: 5px 12px;
    border-radius: var(--tt-radius-full);
    font-family: var(--tt-font-mono);
}

/* ── 헤더 본문 ── */
.write-page__header-body {
    display: flex;
    align-items: flex-end;
    justify-content: space-between;
    gap: 6px;
}

.write-page__headline {
    font-size: 21px;
    font-weight: var(--tt-fw-black);
    color: var(--tt-white);
    letter-spacing: -0.01em;
    line-height: 1.3;
}

.write-page__header-mascot {
    width: 68px;
    height: 68px;
    object-fit: contain;
    margin-bottom: -6px;
    flex: none;
}

/* ── 본문 영역 ── */
.write-page__content {
    flex: 1;
    min-height: 0;
    overflow-y: auto;
    padding: 13px var(--tt-screen-padding) 0;
    display: flex;
    flex-direction: column;
    gap: 10px;
}

/* ══════════════════════════════════════
   기소 거래 아코디언
   ══════════════════════════════════════ */
.write-page__cite-card {
    flex: none;
    background: var(--tt-bg);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-xl);
    box-shadow: var(--tt-elevation-2);
    overflow: hidden;
}

.write-page__cite-header {
    display: flex;
    align-items: center;
    gap: 9px;
    padding: 12px 14px;
    cursor: pointer;
}

.write-page__cite-badge {
    flex: none;
    background: var(--tt-danger-subtle);
    color: var(--tt-danger-deep);
    font-size: 10.5px;
    font-weight: var(--tt-fw-black);
    padding: 4px 9px;
    border-radius: var(--tt-radius-full);
}

.write-page__cite-summary {
    flex: 1;
    min-width: 0;
    font-size: 13px;
    font-weight: var(--tt-fw-black);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

.write-page__cite-chevron {
    flex: none;
    transition: transform 0.22s ease;
}

.write-page__cite-chevron--open {
    transform: rotate(180deg);
}

/* ── 아코디언 본문 ── */
.write-page__cite-body {
    padding: 0 14px 12px;
}

.write-page__cite-divider {
    height: 1px;
    background: #F0EDE6;
    margin-bottom: 10px;
}

.write-page__cite-rows {
    display: flex;
    flex-direction: column;
    gap: 7px;
}

.write-page__cite-row {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
}

.write-page__cite-label {
    font-size: 11.5px;
    color: var(--tt-text-muted);
    font-weight: var(--tt-fw-bold);
}

.write-page__cite-label--dark {
    color: var(--tt-text-body);
}

.write-page__cite-value {
    font-size: 12px;
    color: var(--tt-text-body);
    font-weight: var(--tt-fw-bold);
}

.write-page__cite-value--bold {
    font-size: 12.5px;
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
}

.write-page__cite-value--blue {
    font-size: 13.5px;
    font-weight: var(--tt-fw-black);
    color: var(--tt-info);
}

.write-page__cite-safe-badge {
    margin-top: 10px;
    background: #E4F4EC;
    border-radius: 10px;
    padding: 7px 10px;
    font-size: var(--tt-fs-overline);
    font-weight: var(--tt-fw-black);
    color: #2E9E6B;
}

/* 아코디언 전환 */
.accordion-enter-active {
    animation: tt-acc 0.24s cubic-bezier(0.22, 1, 0.36, 1) both;
}

.accordion-leave-active {
    animation: tt-acc 0.18s cubic-bezier(0.22, 1, 0.36, 1) reverse both;
}

@keyframes tt-acc {
    from {
        opacity: 0;
        max-height: 0;
        transform: translateY(-4px);
    }
    to {
        opacity: 1;
        max-height: 200px;
        transform: translateY(0);
    }
}

/* ══════════════════════════════════════
   나의 변론
   ══════════════════════════════════════ */
.write-page__text-section {
    flex: none;
}

.write-page__text-label {
    font-size: 13px;
    font-weight: var(--tt-fw-black);
    margin-bottom: 7px;
}

.write-page__textarea-wrap {
    background: var(--tt-bg);
    border: 1.5px solid var(--tt-surface-inverse);
    border-radius: var(--tt-radius-md);
    padding: 12px 14px;
}

.write-page__textarea {
    width: 100%;
    min-height: 72px;
    border: none;
    outline: none;
    resize: none;
    font-family: var(--tt-font-sans);
    font-size: 12.5px;
    color: var(--tt-surface-inverse);
    line-height: 1.6;
    background: transparent;
}

.write-page__textarea::placeholder {
    color: var(--tt-text-hint);
}

.write-page__char-count {
    display: block;
    text-align: right;
    font-size: var(--tt-fs-overline);
    color: var(--tt-text-hint);
    font-weight: var(--tt-fw-bold);
    margin-top: 8px;
    font-family: var(--tt-font-mono);
}

/* ══════════════════════════════════════
   증빙 자료
   ══════════════════════════════════════ */
.write-page__evidence-section {
    flex: none;
}

.write-page__evidence-header {
    display: flex;
    align-items: baseline;
    gap: 6px;
    margin-bottom: 7px;
}

.write-page__evidence-label {
    font-size: 13px;
    font-weight: var(--tt-fw-black);
}

.write-page__evidence-hint {
    font-size: 11.5px;
    color: var(--tt-text-muted);
    font-weight: var(--tt-fw-bold);
}

.write-page__evidence-grid {
    display: flex;
    gap: 9px;
}

/* 업로드된 이미지 썸네일 */
.write-page__evidence-thumb {
    flex: 1;
    height: 70px;
    border-radius: var(--tt-radius-md);
    background: var(--tt-bg-fill);
    border: 1px solid var(--tt-border);
    overflow: hidden;
    position: relative;
    display: flex;
    align-items: flex-end;
    padding: 6px;
}

.write-page__evidence-img {
    position: absolute;
    inset: 0;
    width: 100%;
    height: 100%;
    object-fit: cover;
}

.write-page__evidence-remove {
    position: absolute;
    top: 4px;
    right: 4px;
    width: 20px;
    height: 20px;
    border-radius: 50%;
    background: rgba(35, 40, 66, 0.6);
    color: var(--tt-white);
    border: none;
    font-size: 13px;
    font-weight: var(--tt-fw-bold);
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    line-height: 1;
    z-index: 2;
}

.write-page__evidence-name {
    position: relative;
    z-index: 1;
    background: var(--tt-bg);
    border: 1px solid var(--tt-border);
    border-radius: 6px;
    font-size: 9.5px;
    color: var(--tt-text-body);
    font-weight: var(--tt-fw-bold);
    padding: 2px 5px;
    font-family: var(--tt-font-mono);
    max-width: 100%;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

/* 추가 슬롯 */
.write-page__evidence-add {
    flex: 1;
    height: 70px;
    border-radius: var(--tt-radius-md);
    border: 1.5px dashed #D8D3C8;
    background: transparent;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 3px;
    cursor: pointer;
    -webkit-tap-highlight-color: transparent;
    transition: background 0.15s ease;
}

.write-page__evidence-add:active {
    background: var(--tt-bg-fill);
}

.write-page__evidence-plus {
    font-size: 17px;
    color: var(--tt-text-hint);
    font-weight: var(--tt-fw-semibold);
    line-height: 1;
}

.write-page__evidence-add-label {
    font-size: 10.5px;
    color: var(--tt-text-hint);
    font-weight: var(--tt-fw-bold);
}

/* ══════════════════════════════════════
   수정 불가 경고 카드
   ══════════════════════════════════════ */
.write-page__warn-card {
    flex: none;
    background: var(--tt-danger-subtle);
    border: 1px solid #F3D3C9;
    border-radius: var(--tt-radius-xl);
    padding: 12px 15px;
    display: flex;
    gap: 10px;
    align-items: flex-start;
}

.write-page__warn-icon {
    width: 22px;
    height: 22px;
    border-radius: 50%;
    background: var(--tt-bg);
    border: 1.5px solid var(--tt-danger);
    display: flex;
    align-items: center;
    justify-content: center;
    flex: none;
    margin-top: 1px;
}

.write-page__warn-bang {
    font-size: 12px;
    font-weight: 900;
    color: var(--tt-danger);
}

.write-page__warn-text {
    font-size: 12px;
    color: var(--tt-text-body);
    line-height: 1.55;
}

.write-page__warn-strong {
    color: var(--tt-danger-deep);
}

/* ── 하단 제출 ── */
.write-page__footer {
    flex: none;
    padding: 12px var(--tt-screen-padding) 18px;
    background: var(--tt-bg-subtle);
}
</style>
