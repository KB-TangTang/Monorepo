<script setup>
import { ref, watch, onUnmounted } from 'vue';
import { CursorArrowRippleIcon } from '@heroicons/vue/24/solid';
import TutorialStepIndicator from '@/components/challenge/TutorialStepIndicator.vue';
import TutorialNavBar from '@/components/challenge/TutorialNavBar.vue';

import prosecutorA from '@/assets/images/prosecutor_tangtang/prosecutor_tangtang_A.png';
import magnifyingStare from '@/assets/images/prosecutor_tangtang/magnifying_stare.png';
import raidTangi from '@/assets/images/prosecutor_tangtang/consumer_moment_raid_v2_2d_no_emphasis.png';
import fieldVerification from '@/assets/images/prosecutor_tangtang/field_verification_v2.png';
import happyTangi from '@/assets/images/emotions/01_happy.png';
import gentleSmile from '@/assets/images/emotions/03_gentle_smile.png';
import disappointed from '@/assets/images/emotions/14_disappointed.png';
import celebration from '@/assets/images/emotions/10_celebration.png';

const props = defineProps({
    modelValue: {
        type: Boolean,
        required: true,
    },
});

const emit = defineEmits(['update:modelValue', 'complete']);

const TOTAL_STEPS = 4;
const step = ref(0);

/* FILE 02 — 검사 선택 */
const selectedPros = ref('cold');

/* FILE 03 — 한도 초과 시뮬 */
const isOverLimit = ref(false);

/* FILE 04 — 인정/불인정 토글 */
const verdictType = ref('success');

function next() {
    if (step.value < TOTAL_STEPS - 1) {
        step.value++;
    }
}

function prev() {
    if (step.value > 0) {
        step.value--;
    }
}

function skip() {
    close();
}

function close() {
    step.value = 0;
    isOverLimit.value = false;
    verdictType.value = 'success';
    selectedPros.value = 'cold';
    emit('update:modelValue', false);
    emit('complete');
}

/* 스크롤 잠금 */
watch(
    () => props.modelValue,
    (show) => {
        document.body.style.overflow = show ? 'hidden' : '';
    },
);

onUnmounted(() => {
    document.body.style.overflow = '';
});
</script>

<template>
    <Teleport to="body">
        <Transition name="tutorial-fade">
            <div v-if="modelValue" class="pt-overlay">
                <div class="pt-overlay__dim" />
                <div class="pt-overlay__frame">
                    <!-- 스텝 화면 -->
                    <template v-if="step >= 0">
                        <Transition name="step-fade" mode="out-in">
                            <div :key="step" class="pt-screen">
                                <!-- 상단: 인디케이터 + 건너뛰기 -->
                                <div class="pt-topbar">
                                    <TutorialStepIndicator
                                        :current-step="step"
                                        :total-steps="TOTAL_STEPS"
                                    />
                                    <button
                                        type="button"
                                        class="pt-topbar__skip"
                                        @click="skip"
                                    >
                                        건너뛰기
                                    </button>
                                </div>

                                <!-- ===== FILE 01 — 매일 재판 ===== -->
                                <div v-if="step === 0" class="pt-body">
                                    <div class="pt-body__center">
                                        <img
                                            :src="prosecutorA"
                                            alt="검사 탕이"
                                            class="pt-mascot pt-mascot--lg"
                                        />
                                        <div class="pt-speech">
                                            <div class="pt-speech__arrow" />
                                            "매일 자정, 당신의 소비를 재판합니다"
                                        </div>
                                    </div>

                                    <div class="pt-file-label" style="margin-top: 20px">FILE 01 · 대법원</div>
                                    <h3 class="pt-title">매일 소비 재판이<br />열려요</h3>
                                    <p class="pt-desc">검사 탕이가 지켜보는 가운데,<br />하루 한 건의 미션이 배정됩니다.</p>

                                    <div class="pt-rules-card">
                                        <div class="pt-rule">
                                            <span class="pt-rule__num">1</span>
                                            <span class="pt-rule__text">담당 검사 고르기 <b>= 난이도</b></span>
                                        </div>
                                        <div class="pt-rule__divider" />
                                        <div class="pt-rule">
                                            <span class="pt-rule__num">2</span>
                                            <span class="pt-rule__text">매일 1개 <b>미션 배정</b></span>
                                        </div>
                                        <div class="pt-rule__divider" />
                                        <div class="pt-rule">
                                            <span class="pt-rule__num">3</span>
                                            <span class="pt-rule__text">자정 판결 · <b>점수와 랭킹</b></span>
                                        </div>
                                    </div>
                                </div>

                                <!-- ===== FILE 02 — 담당 검사 ===== -->
                                <div v-else-if="step === 1" class="pt-body pt-body--left">
                                    <div class="pt-file-label">FILE 02 · 담당 검사</div>
                                    <h3 class="pt-title">검사가 깐깐할수록<br />점수가 커져요</h3>
                                    <p class="pt-desc">담당 검사가 목표 절감률을 정해요.</p>

                                    <div class="pt-pros-list">
                                        <div
                                            class="pt-pros-card"
                                            :class="{ 'pt-pros-card--active': selectedPros === 'strict' }"
                                            @click="selectedPros = 'strict'"
                                        >
                                            <img :src="magnifyingStare" alt="깐깐한 탕이" class="pt-pros-card__img" />
                                            <div class="pt-pros-card__info">
                                                <div class="pt-pros-card__name">깐깐한 탕이</div>
                                                <span class="pt-pros-card__diff pt-pros-card__diff--hard">난이도 ●●●</span>
                                                <div class="pt-pros-card__sub">"1원도 넘기면 안 됩니다." · 절감 40~50%</div>
                                            </div>
                                            <span class="pt-pros-card__badge pt-pros-card__badge--hard">+35점</span>
                                        </div>
                                        <div
                                            class="pt-pros-card"
                                            :class="{ 'pt-pros-card--active': selectedPros === 'cold' }"
                                            @click="selectedPros = 'cold'"
                                        >
                                            <img :src="prosecutorA" alt="냉정한 탕이" class="pt-pros-card__img" />
                                            <div class="pt-pros-card__info">
                                                <div class="pt-pros-card__name">냉정한 탕이</div>
                                                <span class="pt-pros-card__diff pt-pros-card__diff--mid">난이도 ●●○</span>
                                                <div class="pt-pros-card__sub">"넘지 않으면 문제 삼지 않죠." · 절감 20~40%</div>
                                            </div>
                                            <span class="pt-pros-card__badge pt-pros-card__badge--mid">+20점</span>
                                        </div>
                                        <div
                                            class="pt-pros-card"
                                            :class="{ 'pt-pros-card--active': selectedPros === 'mild' }"
                                            @click="selectedPros = 'mild'"
                                        >
                                            <img :src="happyTangi" alt="너그러운 탕이" class="pt-pros-card__img" />
                                            <div class="pt-pros-card__info">
                                                <div class="pt-pros-card__name">너그러운 탕이</div>
                                                <span class="pt-pros-card__diff pt-pros-card__diff--easy">난이도 ●○○</span>
                                                <div class="pt-pros-card__sub">"이 정도는 눈감아 드리죠." · 절감 10~20%</div>
                                            </div>
                                            <span class="pt-pros-card__badge pt-pros-card__badge--easy">+10점</span>
                                        </div>
                                    </div>

                                    <div class="pt-hint">
                                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" class="pt-hint__icon">
                                            <path d="M14.5 3.5l6 6M4 20l6.5-6.5M2 22l2-2M14 4l-9.5 9.5c-.4.4-.4 1 0 1.4l4.1 4.1c.4.4 1 .4 1.4 0L19.5 9.5" />
                                            <path d="M9.5 8.5l5 5" />
                                        </svg>
                                        <span>담당 검사는 언제든 바꿀 수 있어요. <b>다음 날 미션부터</b> 반영됩니다.</span>
                                    </div>
                                </div>

                                <!-- ===== FILE 03 — 오늘의 사건 ===== -->
                                <div v-else-if="step === 2" class="pt-body pt-body--left">
                                    <div class="pt-file-label">FILE 03 · 오늘의 사건</div>
                                    <h3 class="pt-title">매일 아침, 오늘의<br />한도가 배정돼요</h3>
                                    <p class="pt-desc">내 소비 패턴에서 뽑은 맞춤 미션이에요. 한 번 결제해 보면서 감을 잡아보세요.</p>

                                    <div class="pt-mission-card">
                                        <div class="pt-mission-card__top">
                                            <span class="pt-mission-card__label">TODAY'S MISSION</span>
                                            <span class="pt-mission-card__badge">수사 중</span>
                                        </div>
                                        <div class="pt-mission-card__title">카페/간식, 오늘 <span class="pt-mono">6,000원</span> 이하</div>

                                        <!-- 정상 상태 -->
                                        <div v-if="!isOverLimit" class="pt-mission-state" style="animation: tt-pop 0.25s ease-out">
                                            <div class="pt-progress-track">
                                                <div class="pt-progress-fill" style="width: 55%" />
                                            </div>
                                            <div class="pt-mission-amounts">
                                                <span class="pt-mono pt-text-hint">3,300 / 6,000원</span>
                                                <span class="pt-text-gold">한도까지 2,700원</span>
                                            </div>
                                            <div class="pt-tangi-bubble">
                                                <img :src="gentleSmile" alt="여유로운 탕이" class="pt-tangi-bubble__img" />
                                                <span>"아직은 평화롭군요."</span>
                                            </div>
                                            <button
                                                type="button"
                                                class="pt-sim-btn pt-sim-btn--gold"
                                                @click="isOverLimit = true"
                                            >
                                                커피 한 잔 결제해 보기 <CursorArrowRippleIcon class="pt-sim-btn__icon" />
                                            </button>
                                        </div>

                                        <!-- 초과 상태 -->
                                        <div v-else class="pt-mission-state" style="animation: tt-pop 0.25s ease-out">
                                            <div class="pt-progress-track">
                                                <div class="pt-progress-fill pt-progress-fill--over" style="width: 100%" />
                                            </div>
                                            <div class="pt-mission-amounts">
                                                <span class="pt-mono pt-text-hint">7,800 / 6,000원</span>
                                                <span class="pt-text-danger">1,800원 초과!</span>
                                            </div>
                                            <div class="pt-tangi-bubble pt-tangi-bubble--danger">
                                                <img :src="fieldVerification" alt="출동한 검사 탕이" class="pt-tangi-bubble__img" />
                                                <span class="pt-tangi-bubble__danger-text">"한도 초과, 현행범입니다!"</span>
                                            </div>
                                            <button
                                                type="button"
                                                class="pt-sim-btn pt-sim-btn--outline"
                                                @click="isOverLimit = false"
                                            >
                                                되돌리기
                                            </button>
                                        </div>
                                    </div>

                                    <div class="pt-hint">
                                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" class="pt-hint__icon">
                                            <path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z" />
                                        </svg>
                                        <span>결제 내역은 <b>계좌에서 자동 수집</b>돼요. 따로 기록할 필요가 없어요.</span>
                                    </div>
                                </div>

                                <!-- ===== FILE 04 — 자정 판결 ===== -->
                                <div v-else-if="step === 3" class="pt-body pt-body--left">
                                    <div class="pt-file-label">FILE 04 · 자정 판결</div>
                                    <h3 class="pt-title">자정, 판결문에<br />도장이 찍혀요</h3>

                                    <div class="pt-verdict-toggle">
                                        <div
                                            class="pt-verdict-chip"
                                            :class="{ 'pt-verdict-chip--active': verdictType === 'success' }"
                                            @click="verdictType = 'success'"
                                        >
                                            알리바이 인정
                                        </div>
                                        <div
                                            class="pt-verdict-chip"
                                            :class="{ 'pt-verdict-chip--active': verdictType === 'fail' }"
                                            @click="verdictType = 'fail'"
                                        >
                                            불인정
                                        </div>
                                    </div>

                                    <!-- 인정 판결문 -->
                                    <div v-if="verdictType === 'success'" class="pt-verdict-card" style="animation: tt-pop 0.25s ease-out">
                                        <div class="pt-stamp pt-stamp--success">인 정</div>
                                        <div class="pt-verdict-card__overline">VERDICT · 00:00</div>
                                        <div class="pt-verdict-card__case">판결문 · 카페/간식 사건</div>
                                        <div class="pt-verdict-card__row">
                                            <img :src="disappointed" alt="아쉬워하는 검사 탕이" class="pt-verdict-card__tangi" />
                                            <div class="pt-verdict-card__bubble">"알리바이, 인정하겠습니다"</div>
                                        </div>
                                        <div class="pt-verdict-card__badges">
                                            <span class="pt-badge pt-badge--gold">+20점</span>
                                            <span class="pt-badge pt-badge--gold">연속 보너스 +5점</span>
                                            <span class="pt-badge pt-badge--success">연속 6일</span>
                                        </div>
                                        <p class="pt-verdict-card__bonus-hint">연속 성공 시 매일 보너스 점수가 추가돼요</p>
                                    </div>

                                    <!-- 불인정 판결문 -->
                                    <div v-else class="pt-verdict-card" style="animation: tt-pop 0.25s ease-out">
                                        <div class="pt-stamp pt-stamp--fail">불인정</div>
                                        <div class="pt-verdict-card__overline">VERDICT · 00:00</div>
                                        <div class="pt-verdict-card__case">판결문 · 카페/간식 사건</div>
                                        <div class="pt-verdict-card__row">
                                            <img :src="fieldVerification" alt="현장 검증하는 검사 탕이" class="pt-verdict-card__tangi" />
                                            <div class="pt-verdict-card__bubble">"이번 알리바이는 인정하기 어렵겠군요"</div>
                                        </div>
                                        <div class="pt-verdict-card__badges">
                                            <span class="pt-badge pt-badge--muted">획득 0점</span>
                                            <span class="pt-badge pt-badge--danger">연속일 리셋</span>
                                        </div>
                                    </div>

                                    <!-- 명예의 전당 안내 -->
                                    <div class="pt-honor-card">
                                        <img :src="celebration" alt="축하하는 탕이" class="pt-honor-card__img" />
                                        <div class="pt-honor-card__body">
                                            <div class="pt-honor-card__label">HALL OF HONOR</div>
                                            <div class="pt-honor-card__text">점수는 월간 누적돼 랭킹이 되고, 상위권은 <b>명예의 전당</b>에 올라요.</div>
                                        </div>
                                    </div>
                                </div>

                                <!-- 하단 네비게이션 -->
                                <TutorialNavBar
                                    :step="step"
                                    :total-steps="TOTAL_STEPS"
                                    @prev="prev"
                                    @next="next"
                                    @skip="skip"
                                />
                            </div>
                        </Transition>
                    </template>
                </div>
            </div>
        </Transition>
    </Teleport>
</template>

<style scoped>
/* ── 오버레이 레이아웃 ────────────────── */
.pt-overlay {
    position: fixed;
    inset: 0;
    z-index: var(--tt-z-modal);
    display: flex;
    align-items: center;
    justify-content: center;
}

.pt-overlay__dim {
    position: absolute;
    inset: 0;
    background: var(--tt-overlay-dim);
}

.pt-overlay__frame {
    position: relative;
    width: 100%;
    max-width: var(--tt-content-max);
    height: 100vh;
    height: 100dvh;
    /* 실제 홈 화면을 흉내 내는 틀이라 그 화면의 배경을 그대로 따라간다 */
    background: var(--tt-bg-page);
    overflow: hidden;
}

/* ── 스텝 래퍼 ────────────────────────── */
.pt-screen {
    position: absolute;
    inset: 0;
    display: flex;
    flex-direction: column;
}

/* ── 상단바 ───────────────────────────── */
.pt-topbar {
    flex: none;
    padding: 12px 22px 0;
    display: flex;
    align-items: center;
    justify-content: space-between;
}

.pt-topbar__skip {
    font-size: 13px;
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-hint);
    background: none;
    border: none;
    cursor: pointer;
    font-family: inherit;
}

.pt-topbar__skip:active {
    opacity: 0.6;
}

/* ── 본문 영역 ────────────────────────── */
.pt-body {
    flex: 1;
    min-height: 0;
    padding: 10px 24px 0;
    display: flex;
    flex-direction: column;
    justify-content: center;
    overflow-y: auto;
    -ms-overflow-style: none;
    scrollbar-width: none;
}

.pt-body::-webkit-scrollbar {
    display: none;
}

.pt-body--left {
    text-align: left;
}

.pt-body__center {
    display: flex;
    flex-direction: column;
    align-items: center;
}

/* ── 파일 라벨 ────────────────────────── */
.pt-file-label {
    font-size: 10.5px;
    font-weight: var(--tt-fw-black);
    letter-spacing: 0.18em;
    color: var(--tt-text-hint);
    font-family: var(--tt-font-mono);
    text-align: center;
}

.pt-body--left .pt-file-label {
    text-align: left;
}

/* ── 제목·설명 ────────────────────────── */
.pt-title {
    font-size: 23px;
    font-weight: var(--tt-fw-black);
    letter-spacing: -0.02em;
    line-height: 1.32;
    margin-top: 6px;
    text-align: center;
    word-break: keep-all;
}

.pt-body--left .pt-title {
    text-align: left;
}

.pt-desc {
    font-size: 13px;
    color: var(--tt-text-hint);
    margin-top: 8px;
    line-height: 1.6;
    font-weight: var(--tt-fw-semibold);
    text-align: center;
    word-break: keep-all;
}

.pt-body--left .pt-desc {
    text-align: left;
}

/* ── 마스코트 ─────────────────────────── */
.pt-mascot {
    object-fit: contain;
    animation: tt-float 3.4s ease-in-out infinite;
}

.pt-mascot--lg {
    width: 128px;
    height: 128px;
}

/* ── 말풍선 ───────────────────────────── */
.pt-speech {
    margin-top: 2px;
    background: var(--tt-surface-inverse);
    color: var(--tt-text-inverse);
    border-radius: 14px;
    padding: 9px 16px;
    font-size: 13px;
    font-weight: var(--tt-fw-black);
    position: relative;
    box-shadow: 0 12px 26px -12px rgba(35, 40, 66, 0.6);
}

.pt-speech__arrow {
    position: absolute;
    top: -7px;
    left: 50%;
    transform: translateX(-50%);
    width: 0;
    height: 0;
    border-left: 8px solid transparent;
    border-right: 8px solid transparent;
    border-bottom: 8px solid var(--tt-surface-inverse);
}

/* ── FILE 01 규칙 카드 ────────────────── */
.pt-rules-card {
    margin-top: 16px;
    background: var(--tt-bg);
    border: 1.5px solid var(--tt-border);
    border-radius: 20px;
    padding: 8px 16px;
    box-shadow: 0 10px 26px -18px rgba(35, 40, 66, 0.35);
}

.pt-rule {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 10px 0;
}

.pt-rule__num {
    flex: none;
    width: 24px;
    height: 24px;
    border-radius: 8px;
    background: var(--tt-surface-inverse);
    color: var(--tt-accent);
    font-size: 11px;
    font-weight: var(--tt-fw-black);
    display: flex;
    align-items: center;
    justify-content: center;
    font-family: var(--tt-font-mono);
}

.pt-rule__text {
    flex: 1;
    font-size: 13px;
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-muted);
}

.pt-rule__text b {
    color: var(--tt-text);
}

.pt-rule__divider {
    height: 1px;
    background: var(--tt-bg-fill);
}

/* ── FILE 02 검사 카드 ────────────────── */
.pt-pros-list {
    margin-top: 16px;
    display: flex;
    flex-direction: column;
    gap: 9px;
}

.pt-pros-card {
    background: #FFFDF7;
    border: 1.5px solid #E7E0CC;
    border-radius: 18px;
    padding: 11px 13px;
    display: flex;
    align-items: center;
    gap: 11px;
    cursor: pointer;
    transition: border-color 0.15s ease, box-shadow 0.15s ease;
}

.pt-pros-card--active {
    border: 2px solid var(--tt-surface-inverse);
    box-shadow: 0 10px 24px -12px rgba(35, 40, 66, 0.3);
}

.pt-pros-card__img {
    width: 52px;
    height: 52px;
    object-fit: contain;
    flex: none;
}

.pt-pros-card__info {
    flex: 1;
    min-width: 0;
}

.pt-pros-card__name {
    font-size: 15px;
    font-weight: var(--tt-fw-black);
}

.pt-pros-card__diff {
    font-size: 11px;
    font-weight: var(--tt-fw-black);
    font-family: var(--tt-font-mono);
    letter-spacing: 0.08em;
}

.pt-pros-card__diff--hard { color: var(--tt-danger-deep); }
.pt-pros-card__diff--mid { color: var(--tt-primary); }
.pt-pros-card__diff--easy { color: var(--tt-accent-deep); }

.pt-pros-card__sub {
    font-size: 11px;
    color: var(--tt-text-hint);
    font-weight: var(--tt-fw-semibold);
    margin-top: 3px;
}

.pt-pros-card__badge {
    flex: none;
    font-size: 11.5px;
    font-weight: var(--tt-fw-black);
    padding: 5px 10px;
    border-radius: var(--tt-radius-full);
    font-family: var(--tt-font-mono);
}

.pt-pros-card__badge--hard { background: var(--tt-danger-subtle); color: var(--tt-danger-deep); }
.pt-pros-card__badge--mid { background: var(--tt-info-subtle); color: var(--tt-primary); }
.pt-pros-card__badge--easy { background: var(--tt-accent-subtle); color: var(--tt-accent-deep); }

/* ── 힌트 박스 ────────────────────────── */
.pt-hint {
    margin-top: 14px;
    background: var(--tt-bg-fill);
    border-radius: 14px;
    padding: 11px 14px;
    display: flex;
    gap: 9px;
    align-items: flex-start;
    font-size: 11.5px;
    color: var(--tt-text-muted);
    line-height: 1.55;
    font-weight: var(--tt-fw-semibold);
    word-break: keep-all;
}

.pt-hint b {
    color: var(--tt-text);
}

.pt-hint__icon {
    flex: none;
    margin-top: 1px;
    color: var(--tt-text-hint);
}

/* ── FILE 03 미션 카드 ────────────────── */
.pt-mission-card {
    margin-top: 16px;
    background: var(--tt-bg);
    border: 1.5px solid var(--tt-border);
    border-radius: 20px;
    padding: 16px;
    box-shadow: 0 10px 26px -18px rgba(35, 40, 66, 0.35);
}

.pt-mission-card__top {
    display: flex;
    align-items: center;
    justify-content: space-between;
}

.pt-mission-card__label {
    font-size: 10.5px;
    font-weight: var(--tt-fw-black);
    letter-spacing: 0.14em;
    color: var(--tt-text-hint);
    font-family: var(--tt-font-mono);
}

.pt-mission-card__badge {
    background: var(--tt-surface-inverse);
    color: var(--tt-accent);
    font-size: 10.5px;
    font-weight: var(--tt-fw-black);
    padding: 3px 10px;
    border-radius: var(--tt-radius-full);
}

.pt-mission-card__title {
    margin-top: 10px;
    font-size: 17px;
    font-weight: var(--tt-fw-black);
    word-break: keep-all;
}

.pt-mono {
    font-family: var(--tt-font-mono);
}

.pt-mission-state {
    margin-top: 14px;
}

/* 진행바 */
.pt-progress-track {
    height: 10px;
    border-radius: var(--tt-radius-full);
    background: var(--tt-bg-fill);
    overflow: hidden;
}

.pt-progress-fill {
    height: 100%;
    border-radius: var(--tt-radius-full);
    background: var(--tt-accent);
}

.pt-progress-fill--over {
    background: var(--tt-danger);
}

.pt-mission-amounts {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-top: 7px;
    font-size: 12px;
    font-weight: var(--tt-fw-bold);
}

.pt-text-hint { color: var(--tt-text-hint); }
.pt-text-gold { color: var(--tt-accent-deep); font-weight: var(--tt-fw-black); }
.pt-text-danger { color: var(--tt-danger-deep); font-weight: var(--tt-fw-black); }

/* 탕이 말풍선 */
.pt-tangi-bubble {
    margin-top: 13px;
    display: flex;
    align-items: center;
    gap: 10px;
    background: var(--tt-bg-fill);
    border-radius: 14px;
    padding: 10px 12px;
    font-size: 12px;
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-muted);
    word-break: keep-all;
}

.pt-tangi-bubble--danger {
    background: var(--tt-danger-subtle);
}

.pt-tangi-bubble__img {
    width: 36px;
    height: 36px;
    object-fit: contain;
    flex: none;
}

.pt-tangi-bubble__danger-text {
    font-weight: var(--tt-fw-black);
    color: var(--tt-danger-deep);
}

/* 시뮬레이션 버튼 */
.pt-sim-btn {
    margin-top: 12px;
    width: 100%;
    font-weight: var(--tt-fw-black);
    font-size: 13.5px;
    padding: 12px;
    border-radius: 13px;
    cursor: pointer;
    border: none;
    font-family: inherit;
    display: flex;
    align-items: center;
    justify-content: center;
}

.pt-sim-btn:active {
    opacity: 0.85;
}

.pt-sim-btn__icon {
    width: 18px;
    height: 18px;
    display: inline-block;
    vertical-align: middle;
    margin-left: 4px;
}

.pt-sim-btn--gold {
    background: var(--tt-accent);
    color: var(--tt-surface-inverse);
    box-shadow: 0 10px 22px -12px rgba(245, 185, 33, 0.9);
}

.pt-sim-btn--outline {
    background: var(--tt-bg);
    border: 1.5px solid var(--tt-border);
    color: var(--tt-text-hint);
}

/* ── FILE 04 판결 ─────────────────────── */
.pt-verdict-toggle {
    margin-top: 12px;
    display: flex;
    gap: 7px;
    background: #E9E2D0;
    border-radius: var(--tt-radius-full);
    padding: 4px;
    align-self: flex-start;
}

.pt-verdict-chip {
    padding: 8px 16px;
    border-radius: var(--tt-radius-full);
    font-size: 12.5px;
    font-weight: var(--tt-fw-black);
    cursor: pointer;
    color: #8B8470;
}

.pt-verdict-chip--active {
    background: var(--tt-surface-inverse);
    color: var(--tt-text-inverse);
}

.pt-verdict-card {
    margin-top: 14px;
    background: var(--tt-bg);
    border: 1.5px solid var(--tt-border);
    border-radius: 20px;
    padding: 18px 16px;
    position: relative;
    box-shadow: 0 14px 32px -20px rgba(35, 40, 66, 0.45);
}

.pt-stamp {
    position: absolute;
    right: 14px;
    top: 14px;
    font-size: 14px;
    font-weight: var(--tt-fw-black);
    padding: 6px 12px;
    border-radius: 8px;
    letter-spacing: 0.14em;
    animation: tt-stamp 0.3s ease-out both;
}

.pt-stamp--success {
    border: 3px solid rgba(46, 158, 107, 0.6);
    color: var(--tt-success);
}

.pt-stamp--fail {
    border: 3px solid rgba(194, 75, 49, 0.6);
    color: var(--tt-danger-deep);
}

.pt-verdict-card__overline {
    font-size: 10.5px;
    font-weight: var(--tt-fw-black);
    letter-spacing: 0.14em;
    color: var(--tt-text-hint);
    font-family: var(--tt-font-mono);
}

.pt-verdict-card__case {
    margin-top: 8px;
    font-size: 16px;
    font-weight: var(--tt-fw-black);
    word-break: keep-all;
}

.pt-verdict-card__row {
    margin-top: 12px;
    display: flex;
    align-items: center;
    gap: 10px;
}

.pt-verdict-card__tangi {
    width: 56px;
    height: 56px;
    object-fit: contain;
    flex: none;
    animation: tt-float 3.4s ease-in-out infinite;
}

.pt-verdict-card__bubble {
    flex: 1;
    min-width: 0;
    background: var(--tt-bg-fill);
    border-radius: 13px;
    padding: 10px 12px;
    font-size: 12.5px;
    font-weight: var(--tt-fw-black);
    line-height: 1.5;
    word-break: keep-all;
}

.pt-verdict-card__badges {
    margin-top: 12px;
    border-top: 1px dashed var(--tt-border-strong);
    padding-top: 11px;
    display: flex;
    justify-content: center;
    gap: 7px;
    flex-wrap: wrap;
}

/* 뱃지 */
.pt-badge {
    font-size: 11.5px;
    font-weight: var(--tt-fw-black);
    padding: 5px 11px;
    border-radius: var(--tt-radius-full);
    font-family: var(--tt-font-mono);
}

.pt-badge--gold { background: var(--tt-accent-subtle); color: var(--tt-accent-deep); }
.pt-badge--success { background: var(--tt-success-subtle); color: var(--tt-success); }
.pt-badge--muted { background: var(--tt-bg-fill); color: var(--tt-text-hint); }
.pt-badge--danger { background: var(--tt-danger-subtle); color: var(--tt-danger-deep); }

.pt-verdict-card__bonus-hint {
    margin-top: 8px;
    font-size: 11px;
    font-weight: var(--tt-fw-semibold);
    color: var(--tt-text-hint);
    text-align: center;
}

/* 명예의 전당 안내 */
.pt-honor-card {
    margin-top: 14px;
    background: var(--tt-bg);
    border: 1.5px solid var(--tt-border);
    border-radius: 18px;
    padding: 13px 15px;
    display: flex;
    align-items: center;
    gap: 12px;
}

.pt-honor-card__img {
    width: 44px;
    height: 44px;
    object-fit: contain;
    flex: none;
    animation: tt-float 3.6s ease-in-out infinite;
}

.pt-honor-card__body {
    flex: 1;
    min-width: 0;
}

.pt-honor-card__label {
    font-size: 10px;
    font-weight: var(--tt-fw-black);
    color: var(--tt-accent-deep);
    letter-spacing: 0.14em;
    font-family: var(--tt-font-mono);
}

.pt-honor-card__text {
    font-size: 12px;
    color: var(--tt-text-muted);
    font-weight: var(--tt-fw-semibold);
    margin-top: 4px;
    line-height: 1.5;
    word-break: keep-all;
}

.pt-honor-card__text b {
    color: var(--tt-text);
}

/* ── 트랜지션 ─────────────────────────── */
.tutorial-fade-enter-active,
.tutorial-fade-leave-active {
    transition: opacity 0.3s ease;
}

.tutorial-fade-enter-from,
.tutorial-fade-leave-to {
    opacity: 0;
}

.step-fade-enter-active,
.step-fade-leave-active {
    transition: opacity 0.2s ease;
}

.step-fade-enter-from,
.step-fade-leave-to {
    opacity: 0;
}
</style>
