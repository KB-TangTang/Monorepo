<script setup>
import { ref, watch, onUnmounted } from 'vue';
import TutorialStepIndicator from '@/components/challenge/TutorialStepIndicator.vue';
import TutorialNavBar from '@/components/challenge/TutorialNavBar.vue';

import cheeringTangi from '@/assets/images/emotions/09_cheering.png';
import shockedTangi from '@/assets/images/emotions/19_shocked.png';
import verdictTangi from '@/assets/images/emotions/49_verdict.png';
import proudTangi from '@/assets/images/emotions/04_proud.png';
import happyTangi from '@/assets/images/emotions/01_happy.png';
import thinkingTangi from '@/assets/images/emotions/47_thinking.png';
import excitedTangi from '@/assets/images/emotions/05_excited.png';
import lifeAlive from '@/assets/images/challenge_live/gavel-alive.png';
import lifeDepleted from '@/assets/images/challenge_live/gavel-depleted.png';

const props = defineProps({
    modelValue: {
        type: Boolean,
        required: true,
    },
});

const emit = defineEmits(['update:modelValue', 'complete']);

const TOTAL_STEPS = 4;
const step = ref(0);

/* FILE 03 — 봉투 상태 */
const envState = ref('closed');

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
    envState.value = 'closed';
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
            <div v-if="modelValue" class="gt-overlay">
                <div class="gt-overlay__dim" />
                <div class="gt-overlay__frame">
                    <!-- 스텝 화면 -->
                    <template v-if="step >= 0">
                        <Transition name="step-fade" mode="out-in">
                            <div :key="step" class="gt-screen">
                                <!-- 상단: 인디케이터 + 건너뛰기 -->
                                <div class="gt-topbar">
                                    <TutorialStepIndicator
                                        :current-step="step"
                                        :total-steps="TOTAL_STEPS"
                                    />
                                    <button
                                        type="button"
                                        class="gt-topbar__skip"
                                        @click="skip"
                                    >
                                        건너뛰기
                                    </button>
                                </div>

                                <!-- ===== FILE 01 — 지방법원 ===== -->
                                <div v-if="step === 0" class="gt-body">
                                    <div class="gt-body__center">
                                        <img :src="cheeringTangi" alt="응원하는 탕이" class="gt-mascot" />
                                    </div>
                                    <div class="gt-file-label">FILE 01 · 지방법원</div>
                                    <h3 class="gt-title">친구를 모아<br />법정을 열어요</h3>
                                    <p class="gt-desc">초대코드로 2~6명이 모이면,<br />기간·카테고리·1인 한도를 함께 정해요.</p>

                                    <!-- 소환장 카드 -->
                                    <div class="gt-summons">
                                        <div class="gt-summons__inner">
                                            <div class="gt-summons__top-label">탕탕 법정</div>
                                            <div class="gt-summons__title">소환장</div>
                                            <div class="gt-summons__divider">
                                                <div class="gt-summons__line" />
                                                <div class="gt-summons__diamond" />
                                                <div class="gt-summons__line" />
                                            </div>
                                            <div class="gt-summons__code-box">
                                                <div class="gt-summons__code-label">초대 코드</div>
                                                <div class="gt-summons__code">TANG!</div>
                                            </div>
                                            <div class="gt-summons__stamp">
                                                <span>소환</span>
                                            </div>
                                        </div>
                                    </div>

                                    <!-- 배심원단 + 설정 -->
                                    <div class="gt-jury-card">
                                        <div class="gt-jury-card__header">
                                            <span class="gt-jury-card__label">배심원단</span>
                                            <span class="gt-jury-card__count">4 / 6명</span>
                                        </div>
                                        <div class="gt-jury-card__avatars">
                                            <span class="gt-avatar gt-avatar--filled">
                                                <img :src="happyTangi" alt="배심원" class="gt-avatar__img" />
                                            </span>
                                            <span class="gt-avatar gt-avatar--filled">
                                                <img :src="thinkingTangi" alt="배심원" class="gt-avatar__img" />
                                            </span>
                                            <span class="gt-avatar gt-avatar--filled">
                                                <img :src="excitedTangi" alt="배심원" class="gt-avatar__img" />
                                            </span>
                                            <span class="gt-avatar gt-avatar--me">
                                                <img :src="cheeringTangi" alt="나" class="gt-avatar__img" />
                                            </span>
                                            <span class="gt-avatar gt-avatar--empty">
                                                <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" aria-hidden="true">
                                                    <path d="M12 5v14M5 12h14" />
                                                </svg>
                                            </span>
                                        </div>
                                        <div class="gt-jury-card__stats">
                                            <div class="gt-stat">
                                                <div class="gt-stat__label">기간</div>
                                                <div class="gt-stat__value">7일</div>
                                            </div>
                                            <div class="gt-stat__sep" />
                                            <div class="gt-stat">
                                                <div class="gt-stat__label">카테고리</div>
                                                <div class="gt-stat__value">배달비</div>
                                            </div>
                                            <div class="gt-stat__sep" />
                                            <div class="gt-stat">
                                                <div class="gt-stat__label">1인 한도</div>
                                                <div class="gt-stat__value gt-mono">1만원</div>
                                            </div>
                                        </div>
                                    </div>
                                </div>

                                <!-- ===== FILE 02 — 기소 ===== -->
                                <div v-else-if="step === 1" class="gt-body gt-body--left">
                                    <div class="gt-file-label">FILE 02 · 기소</div>
                                    <h3 class="gt-title">한도를 넘기면<br />자동으로 기소돼요</h3>
                                    <p class="gt-desc">계좌 내역에서 자동 감지돼요.<br />봐주기도, 숨기기도 없어요!</p>

                                    <div class="gt-indict-card">
                                        <span class="gt-indict-card__timer">마감 02:14:03</span>
                                        <img :src="shockedTangi" alt="기소당해 놀란 탕이" class="gt-indict-card__tangi" />
                                        <div class="gt-indict-card__title">앗! 한도 금액을 넘겨 기소됐어요</div>
                                        <div class="gt-indict-card__detail">
                                            야식 배달 12,400원 · 1인 한도 10,000원<br />
                                            <b class="gt-text-danger">2,400원 초과</b>
                                        </div>
                                        <div class="gt-indict-card__actions">
                                            <div class="gt-indict-btn gt-indict-btn--dark">변론 작성하기</div>
                                            <div class="gt-indict-btn gt-indict-btn--outline">혐의 인정하기</div>
                                        </div>
                                    </div>

                                    <div class="gt-hint">
                                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" class="gt-hint__icon">
                                            <path d="M3 6l9 6 9-6M3 6v12l9 6 9-6V6" />
                                        </svg>
                                        <span>기소되면 본인에게 <b>변론 기회</b>가 먼저 주어져요. 혐의를 인정하면 바로 유죄로 끝나요.</span>
                                    </div>
                                </div>

                                <!-- ===== FILE 03 — 배심원 ===== -->
                                <div v-else-if="step === 2" class="gt-body gt-body--left">
                                    <div class="gt-file-label">FILE 03 · 배심원</div>
                                    <h3 class="gt-title">변론을 읽고<br />다 같이 판단해요</h3>

                                    <!-- closed: 봉투 -->
                                    <template v-if="envState === 'closed'">
                                        <p class="gt-desc">변론서는 봉투로 도착해요. 직접 열어보세요.</p>
                                        <div class="gt-envelope" @click="envState = 'reading'">
                                            <div class="gt-envelope__visual">
                                                <!-- 봉투 일러스트 (CSS) -->
                                                <div class="gt-envelope__body">
                                                    <div class="gt-envelope__flap" />
                                                    <div class="gt-envelope__line" />
                                                    <div class="gt-envelope__line" />
                                                    <div class="gt-envelope__line gt-envelope__line--short" />
                                                    <div class="gt-envelope__label">변론서 재중</div>
                                                    <div class="gt-envelope__seal">재판<br />문서</div>
                                                </div>
                                                <!-- 변론 요지서 미리보기 -->
                                                <div class="gt-envelope__preview">
                                                    <div class="gt-envelope__preview-title">변론 요지서</div>
                                                    <div class="gt-envelope__preview-divider" />
                                                    <div class="gt-envelope__preview-meta">피고인 : 홍길동</div>
                                                    <div class="gt-envelope__preview-line" />
                                                    <div class="gt-envelope__preview-line" />
                                                    <div class="gt-envelope__preview-line gt-envelope__preview-line--short" />
                                                    <div class="gt-envelope__preview-line" style="margin-top: 9px" />
                                                    <div class="gt-envelope__preview-line gt-envelope__preview-line--half" />
                                                    <div class="gt-envelope__preview-sign">
                                                        홍길동<span class="gt-envelope__preview-seal">홍</span>
                                                    </div>
                                                </div>
                                            </div>
                                            <div class="gt-envelope__text">
                                                <div class="gt-envelope__text-main">홍길동님의 변론서가 도착했어요</div>
                                                <div class="gt-envelope__text-sub">봉투를 눌러 열어보세요</div>
                                            </div>
                                        </div>
                                        <div class="gt-hint">
                                            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" class="gt-hint__icon">
                                                <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                                                <line x1="1" y1="1" x2="23" y2="23" />
                                            </svg>
                                            <span>투표는 <b>익명</b>이에요. 피고인은 자기 재판에 투표할 수 없어요.</span>
                                        </div>
                                    </template>

                                    <!-- reading: 변론 요지서 -->
                                    <template v-else-if="envState === 'reading'">
                                        <div class="gt-brief" style="animation: tt-pop 0.28s ease-out">
                                            <div class="gt-brief__heading">변 론 요 지 서</div>
                                            <div class="gt-brief__meta">
                                                <div>사&nbsp;&nbsp;건&nbsp;:&nbsp;배달 소비 줄이기 · 일일결산</div>
                                                <div>피고인&nbsp;:&nbsp;홍길동&nbsp;&nbsp;<span class="gt-brief__meta-hint">(초과액 2,400원)</span></div>
                                            </div>
                                            <div class="gt-brief__content">"탕이와 야식을 나눠 먹은 것으로, 제 실제 부담금은 6,200원입니다. 송금 내역을 첨부합니다."</div>
                                            <div class="gt-brief__sign">피고인&nbsp;&nbsp;홍&nbsp;길&nbsp;동<span class="gt-brief__seal">홍</span></div>
                                        </div>
                                        <div class="gt-vote-section">
                                            <div class="gt-vote-section__label">배심원 투표 · 익명</div>
                                            <div class="gt-vote-btns">
                                                <div class="gt-vote-btn gt-vote-btn--innocent" @click="envState = 'voted'">
                                                    <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><path d="M2 20h2c.55 0 1-.45 1-1v-9c0-.55-.45-1-1-1H2v11zm19.83-7.12c.11-.25.17-.52.17-.8V11c0-1.1-.9-2-2-2h-5.5l.92-4.65c.05-.22.02-.46-.08-.66-.23-.45-.52-.86-.88-1.22L14 2 7.59 8.41C7.21 8.79 7 9.3 7 9.83v7.84C7 18.95 8.05 20 9.34 20h8.11c.7 0 1.36-.37 1.72-.97l2.66-6.15z" /></svg>
                                                    <span>무죄</span>
                                                </div>
                                                <div class="gt-vote-btn gt-vote-btn--guilty" @click="envState = 'voted'">
                                                    <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><path d="M22 4h-2c-.55 0-1 .45-1 1v9c0 .55.45 1 1 1h2V4zM2.17 11.12c-.11.25-.17.52-.17.8V13c0 1.1.9 2 2 2h5.5l-.92 4.65c-.05.22-.02.46.08.66.23.45.52.86.88 1.22L10 22l6.41-6.41c.38-.38.59-.89.59-1.42V6.34C17 5.05 15.95 4 14.66 4H6.56c-.71 0-1.37.37-1.73.97L2.17 11.12z" /></svg>
                                                    <span>유죄</span>
                                                </div>
                                            </div>
                                            <div class="gt-vote-section__note">한 줄 코멘트도 익명으로 남길 수 있어요 · 제출 후 번복 불가</div>
                                        </div>
                                    </template>

                                    <!-- voted: 투표 완료 -->
                                    <template v-else>
                                        <div class="gt-voted-card" style="animation: tt-pop 0.28s ease-out">
                                            <svg width="40" height="40" viewBox="0 0 24 24" fill="var(--tt-accent)" aria-hidden="true"><path d="M18 13h-.68l-2 2h1.91L19 17H5l1.78-2h2.05l-2-2H6l-3 3v4c0 1.1.89 2 1.99 2H19c1.1 0 2-.89 2-2v-4l-3-3zm-1-5.05l-4.95 4.95-3.54-3.54 4.95-4.95 3.54 3.54zm-4.24-5.66L6.39 8.66a.996.996 0 000 1.41l4.95 4.95c.39.39 1.02.39 1.41 0l6.36-6.36a.996.996 0 000-1.41l-4.95-4.95a.996.996 0 00-1.41 0z" /></svg>
                                            <div class="gt-voted-card__title">투표를 제출했어요</div>
                                            <div class="gt-voted-card__sub">누가 어떻게 투표했는지는 공개되지 않아요.</div>
                                            <div class="gt-voted-card__bar-wrap">
                                                <div class="gt-voted-card__bar-labels">
                                                    <span class="gt-text-success">무죄 1</span>
                                                    <span class="gt-text-hint">마감 후 공개</span>
                                                    <span class="gt-text-danger">유죄 2</span>
                                                </div>
                                                <div class="gt-voted-card__bar">
                                                    <span class="gt-voted-card__bar-fill--innocent" style="width: 33%" />
                                                    <span class="gt-voted-card__bar-fill--guilty" style="width: 67%" />
                                                </div>
                                            </div>
                                        </div>
                                        <div class="gt-lives-warn">
                                            <img :src="verdictTangi" alt="판결하는 탕이" class="gt-lives-warn__img" />
                                            <span>과반이 유죄면 <b class="gt-text-danger">유죄 판결</b> — 홍길동님의 목숨이 하나 줄어요.</span>
                                        </div>
                                        <button
                                            type="button"
                                            class="gt-retry-link"
                                            @click="envState = 'closed'"
                                        >
                                            다시 해보기
                                        </button>
                                    </template>
                                </div>

                                <!-- ===== FILE 04 — 생존 ===== -->
                                <div v-else-if="step === 3" class="gt-body gt-body--left">
                                    <div class="gt-file-label">FILE 04 · 생존</div>
                                    <h3 class="gt-title">일일결산은 목숨제,<br />끝까지 살아남으면 승리</h3>

                                    <!-- 목숨 카드 -->
                                    <div class="gt-lives-card">
                                        <div class="gt-lives-card__label">일일결산 · 7일 챌린지 = 목숨 7개</div>
                                        <div class="gt-lives-card__icons">
                                            <img
                                                v-for="i in 5"
                                                :key="`alive-${i}`"
                                                :src="lifeAlive"
                                                alt="남은 목숨"
                                                class="gt-life-icon"
                                                :style="{ animationDelay: (i * 0.2) + 's' }"
                                            />
                                            <img
                                                v-for="i in 2"
                                                :key="`dep-${i}`"
                                                :src="lifeDepleted"
                                                alt="잃은 목숨"
                                                class="gt-life-icon gt-life-icon--depleted"
                                            />
                                        </div>
                                        <div class="gt-lives-card__count">5 <span class="gt-text-hint">/ 7</span></div>
                                    </div>

                                    <!-- 규칙 리스트 -->
                                    <div class="gt-rules-list">
                                        <div class="gt-rule-item gt-rule-item--gold">
                                            <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><path d="M20 3h-1V1h-2v2H7V1H5v2H4c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm0 18H4V10h16v11z" /></svg>
                                            <span>일일결산 · 목숨 = 챌린지 일수 <span class="gt-text-hint">(최대 7개)</span></span>
                                        </div>
                                        <div class="gt-rule-item__divider" />
                                        <div class="gt-rule-item gt-rule-item--danger">
                                            <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><path d="M14.5 3.5l6 6M4 20l6.5-6.5M2 22l2-2M14 4l-9.5 9.5c-.4.4-.4 1 0 1.4l4.1 4.1c.4.4 1 .4 1.4 0L19.5 9.5M9.5 8.5l5 5" /></svg>
                                            <span>유죄 1건 = 목숨 −1 · 0개가 되면 탈락</span>
                                        </div>
                                        <div class="gt-rule-item__divider" />
                                        <div class="gt-rule-item gt-rule-item--muted">
                                            <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><path d="M19 3h-1V1h-2v2H8V1H6v2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-2 10H7v-2h10v2z" /></svg>
                                            <span>기간결산 · 목숨 없이 마지막 날 일괄 결산</span>
                                        </div>
                                        <div class="gt-rule-item__divider" />
                                        <div class="gt-rule-item gt-rule-item--gold">
                                            <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z" /></svg>
                                            <span>마지막까지 남은 사람이 승리</span>
                                        </div>
                                    </div>

                                    <!-- 명예 법정 안내 -->
                                    <div class="gt-honor-card">
                                        <img :src="proudTangi" alt="자랑스러운 탕이" class="gt-honor-card__img" />
                                        <div class="gt-honor-card__body">
                                            <div class="gt-honor-card__label">HALL OF HONOR</div>
                                            <div class="gt-honor-card__text">챌린지가 끝나면 <b>명예 법정</b>에 생존 기록과 최종 순위가 남아요.</div>
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
.gt-overlay {
    position: fixed;
    inset: 0;
    z-index: var(--tt-z-modal);
    display: flex;
    align-items: center;
    justify-content: center;
}

.gt-overlay__dim {
    position: absolute;
    inset: 0;
    background: var(--tt-overlay-dim);
}

.gt-overlay__frame {
    position: relative;
    width: 100%;
    max-width: var(--tt-content-max);
    height: 100vh;
    height: 100dvh;
    background: var(--tt-bg-subtle);
    overflow: hidden;
}

/* ── 스텝 래퍼 ────────────────────────── */
.gt-screen {
    position: absolute;
    inset: 0;
    display: flex;
    flex-direction: column;
}

/* ── 상단바 ───────────────────────────── */
.gt-topbar {
    flex: none;
    padding: 12px 22px 0;
    display: flex;
    align-items: center;
    justify-content: space-between;
}

.gt-topbar__skip {
    font-size: 13px;
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-hint);
    background: none;
    border: none;
    cursor: pointer;
    font-family: inherit;
}

.gt-topbar__skip:active {
    opacity: 0.6;
}

/* ── 본문 영역 ────────────────────────── */
.gt-body {
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

.gt-body::-webkit-scrollbar {
    display: none;
}

.gt-body--left {
    text-align: left;
}

.gt-body__center {
    display: flex;
    flex-direction: column;
    align-items: center;
}

/* ── 공통 텍스트 ──────────────────────── */
.gt-file-label {
    font-size: 10.5px;
    font-weight: var(--tt-fw-black);
    letter-spacing: 0.18em;
    color: var(--tt-text-hint);
    font-family: var(--tt-font-mono);
    text-align: center;
    margin-top: 10px;
}

.gt-body--left .gt-file-label {
    text-align: left;
    margin-top: 0;
}

.gt-title {
    font-size: 23px;
    font-weight: var(--tt-fw-black);
    letter-spacing: -0.02em;
    line-height: 1.32;
    margin-top: 6px;
    text-align: center;
    word-break: keep-all;
}

.gt-body--left .gt-title {
    text-align: left;
}

.gt-desc {
    font-size: 13px;
    color: var(--tt-text-hint);
    margin-top: 8px;
    line-height: 1.6;
    font-weight: var(--tt-fw-semibold);
    text-align: center;
    word-break: keep-all;
}

.gt-body--left .gt-desc {
    text-align: left;
}

.gt-mascot {
    width: 128px;
    height: 128px;
    object-fit: contain;
    animation: tt-float 3.4s ease-in-out infinite;
}

.gt-mono {
    font-family: var(--tt-font-mono);
}

.gt-text-hint { color: var(--tt-text-hint); }
.gt-text-success { color: var(--tt-success-deep); font-weight: var(--tt-fw-black); font-size: 11px; }
.gt-text-danger { color: var(--tt-danger-deep); }

/* ── FILE 01 소환장 ───────────────────── */
.gt-summons {
    margin-top: 16px;
    background: #FDFBF3;
    border: 2px solid var(--tt-surface-inverse);
    border-radius: 6px;
    padding: 5px;
    box-shadow: 0 14px 30px -16px rgba(35, 40, 66, 0.45);
    transform: rotate(-1.2deg);
}

.gt-summons__inner {
    border: 1px solid #9AA0B8;
    border-radius: 3px;
    padding: 12px 14px 13px;
    display: flex;
    flex-direction: column;
    align-items: center;
    text-align: center;
    position: relative;
}

.gt-summons__top-label {
    font-size: 9px;
    font-weight: var(--tt-fw-bold);
    letter-spacing: 0.34em;
    color: var(--tt-text-hint);
}

.gt-summons__title {
    font-family: 'Nanum Myeongjo', serif;
    font-size: 19px;
    font-weight: 800;
    color: var(--tt-text);
    letter-spacing: 0.42em;
    text-indent: 0.42em;
    margin-top: 4px;
}

.gt-summons__divider {
    display: flex;
    align-items: center;
    gap: 8px;
    width: 100%;
    margin: 9px 0;
}

.gt-summons__line {
    flex: 1;
    border-top: 1px solid #D8D3C4;
}

.gt-summons__diamond {
    width: 4px;
    height: 4px;
    background: var(--tt-text);
    transform: rotate(45deg);
}

.gt-summons__code-box {
    width: 100%;
    border: 1.5px dashed #C5BFA9;
    border-radius: 8px;
    padding: 8px 10px 9px;
    background: #FBF7EA;
}

.gt-summons__code-label {
    font-size: 9.5px;
    font-weight: var(--tt-fw-black);
    color: #8A8265;
    letter-spacing: 0.12em;
}

.gt-summons__code {
    font-family: var(--tt-font-mono);
    font-size: 21px;
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
    letter-spacing: 0.16em;
    text-indent: 0.16em;
    margin-top: 3px;
}

.gt-summons__stamp {
    position: absolute;
    right: 8px;
    top: 8px;
    width: 40px;
    height: 40px;
    border: 2px solid rgba(194, 75, 49, 0.85);
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    opacity: 0.9;
    transform: rotate(8deg);
}

.gt-summons__stamp span {
    font-family: 'Nanum Myeongjo', serif;
    font-size: 12px;
    font-weight: 800;
    color: var(--tt-danger-deep);
    letter-spacing: 0.1em;
    text-indent: 0.1em;
}

/* ── FILE 01 배심원 카드 ──────────────── */
.gt-jury-card {
    margin-top: 11px;
    background: var(--tt-bg);
    border: 1.5px solid var(--tt-border);
    border-radius: 18px;
    padding: 14px 15px;
}

.gt-jury-card__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
}

.gt-jury-card__label {
    font-size: 11px;
    font-weight: var(--tt-fw-black);
    color: var(--tt-text-hint);
    letter-spacing: 0.08em;
}

.gt-jury-card__count {
    font-size: 11px;
    font-weight: var(--tt-fw-black);
    color: var(--tt-accent-deep);
}

.gt-jury-card__avatars {
    margin-top: 10px;
    display: flex;
    align-items: center;
    gap: 7px;
}

.gt-avatar {
    width: 34px;
    height: 34px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 12px;
    font-weight: var(--tt-fw-black);
}

.gt-avatar__img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    border-radius: 50%;
}

.gt-avatar--filled {
    background: var(--tt-bg-fill);
    box-shadow: 0 2px 8px rgba(35, 40, 66, 0.15);
}

.gt-avatar--me {
    background: var(--tt-bg-fill);
    color: var(--tt-text-hint);
}

.gt-avatar--empty {
    border: 1.5px dashed #C9CBD4;
    color: #C9CBD4;
}

.gt-jury-card__stats {
    margin-top: 12px;
    border-top: 1px dashed var(--tt-border-strong);
    padding-top: 11px;
    display: flex;
    gap: 8px;
}

.gt-stat {
    flex: 1;
    text-align: center;
}

.gt-stat__label {
    font-size: 10px;
    font-weight: var(--tt-fw-black);
    color: var(--tt-text-hint);
    letter-spacing: 0.06em;
}

.gt-stat__value {
    font-size: 13px;
    font-weight: var(--tt-fw-black);
    margin-top: 3px;
}

.gt-stat__sep {
    width: 1px;
    background: var(--tt-bg-fill);
}

/* ── FILE 02 기소장 카드 ──────────────── */
.gt-indict-card {
    margin-top: 16px;
    background: var(--tt-bg);
    border: 1.5px solid var(--tt-border);
    border-radius: 20px;
    padding: 18px 16px 16px;
    position: relative;
    text-align: center;
    box-shadow: 0 12px 28px -18px rgba(35, 40, 66, 0.35);
}

.gt-indict-card__timer {
    position: absolute;
    right: 14px;
    top: 14px;
    background: var(--tt-danger-subtle);
    color: var(--tt-danger-deep);
    font-size: 11.5px;
    font-weight: var(--tt-fw-black);
    padding: 5px 12px;
    border-radius: var(--tt-radius-full);
    font-family: var(--tt-font-mono);
}

.gt-indict-card__tangi {
    display: block;
    margin: 8px auto 0;
    width: 104px;
    height: 104px;
    object-fit: contain;
    animation: tt-float 3.2s ease-in-out infinite;
}

.gt-indict-card__title {
    font-size: 15.5px;
    font-weight: var(--tt-fw-black);
    word-break: keep-all;
}

.gt-indict-card__detail {
    font-size: 11.5px;
    color: var(--tt-text-hint);
    font-weight: var(--tt-fw-semibold);
    margin-top: 6px;
    line-height: 1.55;
}

.gt-indict-card__detail b {
    color: var(--tt-danger-deep);
}

.gt-indict-card__actions {
    margin-top: 12px;
    display: flex;
    gap: 10px;
}

.gt-indict-btn {
    flex: 1;
    font-weight: var(--tt-fw-black);
    padding: 14px 0;
    border-radius: 14px;
    text-align: center;
    font-size: 14px;
}

.gt-indict-btn--dark {
    flex: 1.25;
    background: var(--tt-surface-inverse);
    color: var(--tt-text-inverse);
    box-shadow: 0 10px 24px -12px rgba(35, 40, 66, 0.6);
}

.gt-indict-btn--outline {
    border: 1.5px solid var(--tt-danger);
    color: var(--tt-danger-deep);
    background: var(--tt-bg);
}

/* ── 힌트 ─────────────────────────────── */
.gt-hint {
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

.gt-hint b { color: var(--tt-text); }

.gt-hint__icon {
    flex: none;
    margin-top: 1px;
    color: var(--tt-text-hint);
}

/* ── FILE 03 봉투 ─────────────────────── */
.gt-envelope {
    margin-top: 16px;
    background: var(--tt-bg);
    border: 1.5px solid var(--tt-border);
    border-radius: 20px;
    padding: 18px 14px 16px;
    cursor: pointer;
    box-shadow: 0 12px 28px -18px rgba(35, 40, 66, 0.35);
}

.gt-envelope__visual {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 16px;
}

.gt-envelope__body {
    width: 112px;
    height: 128px;
    background: linear-gradient(180deg, #EFBE68, #E4AA4C);
    border-radius: 8px;
    position: relative;
    box-shadow: 0 16px 30px -14px rgba(140, 96, 20, 0.55);
    overflow: hidden;
}

.gt-envelope__flap {
    position: absolute;
    left: 0;
    right: 0;
    top: 0;
    height: 46px;
    background: linear-gradient(180deg, #E3A94D, #D89C3F);
    clip-path: polygon(0 0, 100% 0, 100% 52%, 54% 100%, 46% 100%, 0 52%);
    filter: drop-shadow(0 3px 3px rgba(120, 76, 14, 0.25));
}

.gt-envelope__line {
    position: absolute;
    left: 25px;
    right: 25px;
    height: 5px;
    background: rgba(160, 105, 25, 0.32);
    border-radius: 3px;
}

.gt-envelope__line:nth-child(2) { top: 70px; }
.gt-envelope__line:nth-child(3) { top: 82px; }
.gt-envelope__line--short { left: 40px; right: 40px; top: 94px !important; }

.gt-envelope__label {
    position: absolute;
    left: 0;
    right: 0;
    top: 106px;
    text-align: center;
    font-size: 8px;
    font-weight: var(--tt-fw-black);
    letter-spacing: 0.3em;
    color: rgba(120, 76, 14, 0.55);
}

.gt-envelope__seal {
    position: absolute;
    left: 8px;
    top: 54px;
    transform: rotate(-7deg);
    border: 1.5px solid rgba(194, 75, 49, 0.75);
    color: rgba(194, 75, 49, 0.85);
    font-size: 7px;
    font-weight: var(--tt-fw-black);
    letter-spacing: 0.16em;
    padding: 2px 5px;
    border-radius: 2px;
}

.gt-envelope__preview {
    width: 116px;
    flex: none;
    background: #FCFBF6;
    border: 1px solid #E7E1D2;
    border-radius: 5px;
    box-shadow: 0 8px 18px -8px rgba(35, 40, 66, 0.25);
    padding: 10px 9px 9px;
    transform: rotate(2deg);
}

.gt-envelope__preview-title {
    font-family: 'Nanum Myeongjo', serif;
    font-size: 9px;
    font-weight: 700;
    letter-spacing: 0.28em;
    text-indent: 0.28em;
    text-align: center;
    color: #33333B;
}

.gt-envelope__preview-divider {
    height: 1px;
    background: #33333B;
    margin-top: 5px;
}

.gt-envelope__preview-meta {
    margin-top: 7px;
    font-family: 'Nanum Myeongjo', serif;
    font-size: 8.5px;
    line-height: 1.7;
    color: #5A554A;
}

.gt-envelope__preview-line {
    height: 3.5px;
    background: #E7EAF0;
    border-radius: 3px;
    margin-top: 5px;
}

.gt-envelope__preview-line--short {
    width: 70%;
}

.gt-envelope__preview-line--half {
    width: 55%;
}

.gt-envelope__preview-sign {
    margin-top: 9px;
    display: flex;
    justify-content: flex-end;
    align-items: center;
    gap: 4px;
    font-family: 'Nanum Myeongjo', serif;
    font-size: 8px;
    color: #5A554A;
}

.gt-envelope__preview-seal {
    width: 16px;
    height: 16px;
    border: 1px solid rgba(194, 75, 49, 0.8);
    border-radius: 50%;
    color: rgba(194, 75, 49, 0.85);
    font-size: 6.5px;
    font-weight: 700;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    transform: rotate(-10deg);
    flex: none;
}

.gt-envelope__text {
    margin-top: 12px;
    text-align: center;
}

.gt-envelope__text-main {
    font-size: 14.5px;
    font-weight: var(--tt-fw-black);
}

.gt-envelope__text-sub {
    font-size: 12px;
    color: var(--tt-text-hint);
    font-weight: var(--tt-fw-bold);
    margin-top: 5px;
}

/* ── FILE 03 변론 요지서 ──────────────── */
.gt-brief {
    margin-top: 14px;
    background: #FCFBF6;
    background-image:
        linear-gradient(rgba(62, 99, 214, 0.05) 1px, transparent 1px),
        linear-gradient(90deg, rgba(62, 99, 214, 0.05) 1px, transparent 1px);
    background-size: 15px 15px;
    border: 1px solid #E7E1D2;
    border-radius: 12px;
    box-shadow: 0 8px 26px -8px rgba(35, 40, 66, 0.16);
    padding: 14px 15px 12px;
    font-family: 'Nanum Myeongjo', serif;
    color: #33333B;
}

.gt-brief__heading {
    text-align: center;
    font-size: 13.5px;
    font-weight: 800;
    letter-spacing: 0.3em;
    text-indent: 0.3em;
    padding-bottom: 7px;
    border-bottom: 1.5px solid #33333B;
}

.gt-brief__meta {
    margin-top: 8px;
    font-size: 11px;
    line-height: 1.75;
}

.gt-brief__meta-hint {
    color: #8A8378;
}

.gt-brief__content {
    margin-top: 7px;
    font-size: 11px;
    line-height: 1.75;
    font-weight: 700;
    color: #1F1F26;
}

.gt-brief__sign {
    margin-top: 8px;
    display: flex;
    justify-content: flex-end;
    align-items: center;
    gap: 7px;
    font-size: 11px;
}

.gt-brief__seal {
    width: 22px;
    height: 22px;
    border: 1.5px solid rgba(194, 75, 49, 0.8);
    border-radius: 50%;
    color: rgba(194, 75, 49, 0.85);
    font-size: 8px;
    font-weight: 700;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    transform: rotate(-10deg);
    flex: none;
}

/* ── FILE 03 투표 버튼 ────────────────── */
.gt-vote-section {
    margin-top: 13px;
}

.gt-vote-section__label {
    font-size: 11px;
    font-weight: var(--tt-fw-black);
    color: var(--tt-text-hint);
    letter-spacing: 0.08em;
    text-align: center;
}

.gt-vote-btns {
    margin-top: 9px;
    display: flex;
    gap: 9px;
}

.gt-vote-btn {
    flex: 1;
    border-radius: 16px;
    padding: 14px 0;
    text-align: center;
    cursor: pointer;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 4px;
    font-size: 14px;
    font-weight: var(--tt-fw-black);
}

.gt-vote-btn--innocent {
    background: var(--tt-success-subtle);
    border: 1.5px solid var(--tt-success);
    color: var(--tt-success-deep);
}

.gt-vote-btn--guilty {
    background: var(--tt-danger-subtle);
    border: 1.5px solid var(--tt-danger);
    color: var(--tt-danger-deep);
}

.gt-vote-btn--innocent svg { color: var(--tt-success); }
.gt-vote-btn--guilty svg { color: var(--tt-danger); }

.gt-vote-section__note {
    font-size: 10.5px;
    color: var(--tt-text-hint);
    font-weight: var(--tt-fw-semibold);
    margin-top: 9px;
    text-align: center;
    line-height: 1.55;
}

/* ── FILE 03 투표 완료 ────────────────── */
.gt-voted-card {
    margin-top: 16px;
    background: var(--tt-bg);
    border: 1.5px solid var(--tt-border);
    border-radius: 20px;
    padding: 18px 16px;
    text-align: center;
    box-shadow: 0 12px 28px -18px rgba(35, 40, 66, 0.4);
}

.gt-voted-card__title {
    font-size: 16px;
    font-weight: var(--tt-fw-black);
    margin-top: 9px;
}

.gt-voted-card__sub {
    font-size: 11.5px;
    color: var(--tt-text-hint);
    font-weight: var(--tt-fw-semibold);
    margin-top: 6px;
    line-height: 1.6;
}

.gt-voted-card__bar-wrap {
    margin-top: 14px;
    background: var(--tt-bg-fill);
    border-radius: 14px;
    padding: 13px;
}

.gt-voted-card__bar-labels {
    display: flex;
    align-items: center;
    justify-content: space-between;
    font-size: 11px;
    font-weight: var(--tt-fw-black);
    color: var(--tt-text-muted);
}

.gt-voted-card__bar {
    margin-top: 8px;
    display: flex;
    height: 9px;
    border-radius: var(--tt-radius-full);
    overflow: hidden;
}

.gt-voted-card__bar-fill--innocent {
    background: var(--tt-success);
}

.gt-voted-card__bar-fill--guilty {
    background: var(--tt-danger);
}

.gt-lives-warn {
    margin-top: 12px;
    display: flex;
    gap: 10px;
    align-items: center;
    background: var(--tt-danger-subtle);
    border: 1px solid #F3D3C9;
    border-radius: 16px;
    padding: 12px 13px;
    font-size: 11.5px;
    color: #8A5449;
    font-weight: var(--tt-fw-semibold);
    line-height: 1.6;
    word-break: keep-all;
}

.gt-lives-warn__img {
    width: 38px;
    height: 38px;
    object-fit: contain;
    flex: none;
}

.gt-lives-warn b {
    color: var(--tt-danger-deep);
}

.gt-retry-link {
    margin-top: 10px;
    text-align: center;
    font-size: 12px;
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-hint);
    background: none;
    border: none;
    cursor: pointer;
    font-family: inherit;
}

/* ── FILE 04 목숨 카드 ────────────────── */
.gt-lives-card {
    margin-top: 16px;
    background: var(--tt-bg);
    border: 1.5px solid var(--tt-border);
    border-radius: 20px;
    padding: 16px 12px 14px;
    text-align: center;
    box-shadow: 0 10px 26px -18px rgba(35, 40, 66, 0.35);
}

.gt-lives-card__label {
    font-size: 10.5px;
    font-weight: var(--tt-fw-black);
    color: var(--tt-text-hint);
    letter-spacing: 0.1em;
}

.gt-lives-card__icons {
    margin-top: 11px;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 5px;
}

.gt-life-icon {
    width: 30px;
    height: 30px;
    object-fit: contain;
    animation: tt-float 3.4s ease-in-out infinite;
}

.gt-life-icon--depleted {
    opacity: 0.4;
    animation: none;
}

.gt-lives-card__count {
    margin-top: 9px;
    font-size: 13px;
    font-weight: var(--tt-fw-black);
    font-family: var(--tt-font-mono);
}

/* ── FILE 04 규칙 리스트 ──────────────── */
.gt-rules-list {
    margin-top: 12px;
    background: var(--tt-bg);
    border: 1.5px solid var(--tt-border);
    border-radius: 18px;
    padding: 5px 16px;
}

.gt-rule-item {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 9px 0;
    font-size: 12px;
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-muted);
}

.gt-rule-item svg {
    flex: none;
}

.gt-rule-item--gold { color: var(--tt-accent-deep); }
.gt-rule-item--danger { color: var(--tt-danger); }
.gt-rule-item--muted { color: var(--tt-text-muted); }

.gt-rule-item__divider {
    height: 1px;
    background: var(--tt-bg-fill);
}

/* ── FILE 04 명예 법정 ────────────────── */
.gt-honor-card {
    margin-top: 12px;
    background: var(--tt-bg);
    border: 1.5px solid var(--tt-border);
    border-radius: 18px;
    padding: 13px 15px;
    display: flex;
    align-items: center;
    gap: 12px;
}

.gt-honor-card__img {
    width: 44px;
    height: 44px;
    object-fit: contain;
    flex: none;
    animation: tt-float 3.6s ease-in-out infinite;
}

.gt-honor-card__body {
    flex: 1;
    min-width: 0;
}

.gt-honor-card__label {
    font-size: 10px;
    font-weight: var(--tt-fw-black);
    color: var(--tt-accent-deep);
    letter-spacing: 0.14em;
    font-family: var(--tt-font-mono);
}

.gt-honor-card__text {
    font-size: 12px;
    color: var(--tt-text-muted);
    font-weight: var(--tt-fw-semibold);
    margin-top: 4px;
    line-height: 1.5;
    word-break: keep-all;
}

.gt-honor-card__text b {
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
