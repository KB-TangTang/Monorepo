<!--
  재판 캐러셀 — 일일결산 ACTIVE 상세화면에서
  진행 중인 기소(변론 대기·투표 중) 건을 가로 스와이프 카드로 보여준다.
  카드 4종: 변론 필요 / 변론 제출됨 / 투표 필요 / 투표 완료
-->
<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue';

const props = defineProps({
    indictments: { type: Array, required: true },
});

const emit = defineEmits(['defend', 'vote', 'trial']);

const currentSlide = ref(0);
const scrollEl = ref(null);

function onScroll() {
    if (!scrollEl.value || !scrollEl.value.firstElementChild) return;
    const cardW = scrollEl.value.firstElementChild.offsetWidth + 10; // card + gap
    const idx = Math.round(scrollEl.value.scrollLeft / cardW);
    currentSlide.value = Math.min(idx, props.indictments.length - 1);
}

/*
 * 카드 유형 판별
 * defense-needed: 내가 피기소자 + 변론 미제출
 * defense-done:   내가 피기소자 + 변론 제출 (투표 진행 중)
 * vote-needed:    타인 재판 + 내가 미투표
 * vote-done:      타인 재판 + 내가 투표 완료
 */
function cardType(item) {
    if (item.isMine && item.status === 'DEFENSE_WAIT') return 'defense-needed';
    if (item.isMine && item.status === 'VOTING') return 'defense-done';
    if (!item.isMine && !item.myVote) return 'vote-needed';
    return 'vote-done';
}

function isUrgent(item) {
    const t = cardType(item);
    return t === 'defense-needed' || t === 'vote-needed';
}

function cardTitle(item) {
    const t = cardType(item);
    if (t === 'defense-needed') return '내 변론이 필요해요';
    if (t === 'defense-done') return '내 재판';
    return '재판이 열렸어요';
}

function cardDesc(item) {
    const t = cardType(item);
    if (t === 'defense-needed') return `${item.settlementDate} 결산 · 초과`;
    if (t === 'defense-done') return `${item.settlementDate} 결산 · 초과 ${item.exceededAmount.toLocaleString()}원`;
    if (t === 'vote-done') return `${item.settlementDate} 결산 · 초과 ${item.exceededAmount.toLocaleString()}원`;
    return `${item.settlementDate} 결산 · 초과`;
}

function stampLabel(item) {
    const t = cardType(item);
    if (t === 'defense-done') return '변론\n제출';
    if (t === 'vote-done') return '투표\n완료';
    return '';
}

function deadline(item) {
    if (item.status === 'DEFENSE_WAIT') return `마감 ${item.defenseDeadline}`;
    return `마감 ${item.voteDeadline}`;
}
</script>

<template>
    <div class="trial-carousel">
        <div
            ref="scrollEl"
            class="trial-carousel__track"
            @scroll.passive="onScroll"
        >
            <div
                v-for="item in indictments"
                :key="item.id"
                class="trial-carousel__card"
                :class="{
                    'trial-carousel__card--urgent': isUrgent(item),
                    'trial-carousel__card--muted': !isUrgent(item),
                }"
            >
                <!-- 상단: 사이렌 + 제목 + 마감 -->
                <div class="trial-carousel__header">
                    <template v-if="isUrgent(item)">
                        <svg class="trial-carousel__siren" viewBox="0 0 24 24" fill="none">
                            <line x1="5" y1="10" x2="2.5" y2="8" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" opacity="0.35" />
                            <line x1="19" y1="10" x2="21.5" y2="8" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" opacity="0.35" />
                            <path d="M8 14c0-4 2-7 4-7s4 3 4 7" fill="currentColor" />
                            <rect x="6" y="14" width="12" height="3" rx="1" fill="currentColor" />
                            <rect x="4.5" y="17" width="15" height="2.5" rx="1" fill="var(--tt-text-hint)" />
                        </svg>
                        <span class="trial-carousel__title trial-carousel__title--urgent">
                            {{ cardTitle(item) }}
                        </span>
                        <span class="trial-carousel__deadline">{{ deadline(item) }}</span>
                    </template>
                    <template v-else>
                        <!-- 변론 제출됨 / 투표 완료: 스탬프 + 제목 -->
                        <div class="trial-carousel__done-row">
                            <div class="trial-carousel__done-info">
                                <div class="trial-carousel__title trial-carousel__title--muted">
                                    {{ cardType(item) === 'defense-done' ? '내 재판' : `${item.nickname}님 재판` }}
                                </div>
                                <div class="trial-carousel__sub">
                                    {{ cardDesc(item) }}
                                </div>
                            </div>
                            <div
                                class="trial-carousel__stamp"
                                :class="{ 'trial-carousel__stamp--blue': cardType(item) === 'vote-done' }"
                            >
                                <span
                                    v-for="(line, li) in stampLabel(item).split('\n')"
                                    :key="li"
                                >{{ line }}</span>
                            </div>
                        </div>
                    </template>
                </div>

                <!-- 본문: urgent 카드 -->
                <template v-if="isUrgent(item)">
                    <div class="trial-carousel__body">
                        <div
                            v-if="cardType(item) === 'defense-needed'"
                            class="trial-carousel__person"
                        >
                            <div class="trial-carousel__avatar-wrap">
                                <div
                                    class="trial-carousel__avatar"
                                    :style="{ background: item.avatarColor || '#232842' }"
                                >{{ item.initial }}</div>
                                <span class="trial-carousel__me-tag">나</span>
                            </div>
                            <div class="trial-carousel__person-text">
                                <div class="trial-carousel__person-title">내가 한도를 초과했어요</div>
                                <div class="trial-carousel__person-sub">
                                    {{ item.settlementDate }} 결산 · 초과
                                    <b class="trial-carousel__exceeded">{{ item.exceededAmount.toLocaleString() }}원</b>
                                </div>
                            </div>
                        </div>
                        <div
                            v-else
                            class="trial-carousel__person"
                        >
                            <div
                                class="trial-carousel__avatar"
                                :style="{ background: item.avatarColor }"
                            >{{ item.initial }}</div>
                            <div class="trial-carousel__person-text">
                                <div class="trial-carousel__person-title">
                                    {{ item.nickname }}님이 한도를 초과했어요
                                </div>
                                <div class="trial-carousel__person-sub">
                                    {{ item.settlementDate }} 결산 · 초과
                                    <b class="trial-carousel__exceeded">{{ item.exceededAmount.toLocaleString() }}원</b>
                                </div>
                            </div>
                        </div>

                        <!-- 투표 필요 카드: 투표 현황 -->
                        <div
                            v-if="cardType(item) === 'vote-needed'"
                            class="trial-carousel__vote-status"
                        >
                            <span class="trial-carousel__vote-label">투표 현황</span>
                            <span class="trial-carousel__vote-dots">
                                <span
                                    v-for="n in item.totalVoters"
                                    :key="n"
                                    class="trial-carousel__vote-dot"
                                    :class="{ 'trial-carousel__vote-dot--filled': n <= item.voteCount }"
                                ></span>
                            </span>
                            <span class="trial-carousel__vote-count">
                                {{ item.voteCount }} / {{ item.totalVoters }}명
                            </span>
                        </div>
                    </div>

                    <!-- CTA 버튼 -->
                    <button
                        class="trial-carousel__cta"
                        :class="{
                            'trial-carousel__cta--defense': cardType(item) === 'defense-needed',
                            'trial-carousel__cta--vote': cardType(item) === 'vote-needed',
                        }"
                        @click="cardType(item) === 'defense-needed' ? emit('defend', item) : emit('vote', item)"
                    >
                        {{ cardType(item) === 'defense-needed' ? '변론 작성하기' : '변론 확인하고 투표하기' }}
                    </button>
                </template>

                <!-- 본문: done 카드 — 투표 현황 + CTA -->
                <template v-else>
                    <div class="trial-carousel__vote-status">
                        <span class="trial-carousel__vote-label">투표 현황</span>
                        <span class="trial-carousel__vote-dots">
                            <span
                                v-for="n in item.totalVoters"
                                :key="n"
                                class="trial-carousel__vote-dot"
                                :class="{ 'trial-carousel__vote-dot--filled': n <= item.voteCount }"
                            ></span>
                        </span>
                        <span class="trial-carousel__vote-count trial-carousel__vote-count--muted">
                            {{ item.voteCount }} / {{ item.totalVoters }}명
                        </span>
                    </div>
                    <button
                        class="trial-carousel__cta"
                        :class="{
                            'trial-carousel__cta--trial': cardType(item) === 'defense-done',
                            'trial-carousel__cta--trial-outline': cardType(item) === 'vote-done',
                        }"
                        @click="emit('trial', item)"
                    >
                        재판 현황 보기
                    </button>
                </template>
            </div>
        </div>

        <!-- 페이지 인디케이터 -->
        <div v-if="indictments.length > 1" class="trial-carousel__dots">
            <span
                v-for="(_, i) in indictments"
                :key="i"
                class="trial-carousel__dot"
                :class="{ 'trial-carousel__dot--active': i === currentSlide }"
            ></span>
        </div>
    </div>
</template>

<style scoped>
.trial-carousel {
    flex: none;
    margin: 0 calc(-1 * var(--tt-screen-padding));
}

.trial-carousel__track {
    display: flex;
    gap: 10px;
    overflow-x: auto;
    scroll-snap-type: x mandatory;
    scrollbar-width: none;
    -ms-overflow-style: none;
    padding: 2px calc(var(--tt-screen-padding) + 10px) 8px;
}

.trial-carousel__track::-webkit-scrollbar {
    display: none;
}

/* ── 카드 공통 ── */
.trial-carousel__card {
    flex: none;
    width: calc(100vw - 2 * var(--tt-screen-padding) - 20px);
    scroll-snap-align: center;
    border-radius: var(--tt-radius-lg);
    display: flex;
    flex-direction: column;
    justify-content: space-between;
}

.trial-carousel__card--urgent {
    background: #FDF4F1;
    border: 1px solid #F0D2C9;
    box-shadow: 0 10px 26px rgba(224, 102, 75, 0.16);
    padding: 11px 14px 13px;
    aspect-ratio: 1.75 / 1;
}

.trial-carousel__card--muted {
    background: var(--tt-bg-subtle);
    border: 1px solid var(--tt-border);
    box-shadow: var(--tt-elevation-2);
    padding: 13px 14px;
    aspect-ratio: 1.75 / 1;
    cursor: pointer;
}

/* ── 상단 헤더 ── */
.trial-carousel__header {
    display: flex;
    align-items: center;
    gap: 7px;
}

.trial-carousel__siren {
    width: 18px;
    height: 18px;
    color: var(--tt-red);
    flex: none;
    animation: tt-siren 1.1s ease-in-out infinite;
}

@keyframes tt-siren {
    0%, 100% { opacity: 1; transform: scale(1); }
    50% { opacity: 0.3; transform: scale(0.88); }
}

.trial-carousel__title--urgent {
    font-size: var(--tt-fs-button);
    font-weight: var(--tt-fw-black);
    color: var(--tt-red-deep);
}

.trial-carousel__title--muted {
    font-size: var(--tt-fs-label);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text-body);
}

.trial-carousel__deadline {
    margin-left: auto;
    background: #FBE9E4;
    color: var(--tt-red-deep);
    font-size: var(--tt-fs-badge);
    font-weight: var(--tt-fw-black);
    padding: 3px 9px;
    border-radius: var(--tt-radius-full);
}

/* ── done 카드 상단 ── */
.trial-carousel__done-row {
    display: flex;
    align-items: center;
    gap: 11px;
    width: 100%;
}

.trial-carousel__done-info {
    flex: 1;
    min-width: 0;
}

.trial-carousel__sub {
    margin-top: 3px;
    font-size: var(--tt-fs-caption);
    color: var(--tt-text-hint);
    font-weight: var(--tt-fw-bold);
}

.trial-carousel__stamp {
    flex: none;
    width: 58px;
    height: 58px;
    border-radius: 50%;
    border: 2.5px solid var(--tt-red-deep);
    box-shadow: inset 0 0 0 1px rgba(194, 75, 49, 0.3);
    color: var(--tt-red-deep);
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    transform: rotate(-11deg);
    opacity: 0.72;
    font-size: var(--tt-fs-button);
    font-weight: var(--tt-fw-black);
    line-height: 1.15;
    letter-spacing: -0.02em;
}

/* ── 본문 ── */
.trial-carousel__body {
    display: flex;
    flex-direction: column;
    gap: 11px;
}

.trial-carousel__person {
    display: flex;
    align-items: center;
    gap: 9px;
}

.trial-carousel__avatar-wrap {
    position: relative;
    flex: none;
}

.trial-carousel__avatar {
    width: 28px;
    height: 28px;
    border-radius: 50%;
    color: var(--tt-white);
    font-size: 11px;
    font-weight: var(--tt-fw-black);
    display: flex;
    align-items: center;
    justify-content: center;
    flex: none;
}

.trial-carousel__me-tag {
    position: absolute;
    right: -3px;
    bottom: -3px;
    background: var(--tt-ink);
    color: var(--tt-white);
    font-size: 8px;
    font-weight: var(--tt-fw-black);
    padding: 1px 4px;
    border-radius: var(--tt-radius-full);
}

.trial-carousel__person-text {
    flex: 1;
    min-width: 0;
}

.trial-carousel__person-title {
    font-size: var(--tt-fs-label);
    font-weight: var(--tt-fw-black);
}

.trial-carousel__person-sub {
    margin-top: 3px;
    font-size: var(--tt-fs-caption);
    color: var(--tt-text-muted);
    font-weight: var(--tt-fw-bold);
}

.trial-carousel__exceeded {
    color: var(--tt-red-deep);
}

/* ── 투표 현황 바 ── */
.trial-carousel__vote-status {
    display: flex;
    align-items: center;
    gap: 7px;
}

.trial-carousel__vote-label {
    font-size: var(--tt-fs-badge);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text-muted);
}

.trial-carousel__vote-dots {
    display: flex;
    gap: 3px;
    flex: 1;
}

.trial-carousel__vote-dot {
    flex: 1;
    height: 5px;
    border-radius: var(--tt-radius-full);
    background: #E1E4EC;
}

.trial-carousel__vote-dot--filled {
    background: var(--tt-blue);
}

.trial-carousel__vote-count {
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-black);
    color: var(--tt-blue);
}

.trial-carousel__vote-count--muted {
    color: var(--tt-text-body);
}

/* ── CTA 버튼 ── */
.trial-carousel__cta {
    width: 100%;
    padding: 12px;
    border-radius: var(--tt-radius-sm);
    font-size: var(--tt-fs-button);
    font-weight: var(--tt-fw-black);
    text-align: center;
    border: none;
    cursor: pointer;
    color: var(--tt-white);
}

.trial-carousel__cta--defense {
    background: var(--tt-red-deep);
}

.trial-carousel__cta--vote {
    background: var(--tt-blue);
}

.trial-carousel__cta--trial {
    background: var(--tt-blue);
}

.trial-carousel__cta--trial-outline {
    background: transparent;
    border: 1.5px solid var(--tt-blue);
    color: var(--tt-blue);
}

/* ── 스탬프 파란색 변형 (투표 완료) ── */
.trial-carousel__stamp--blue {
    border-color: var(--tt-blue);
    box-shadow: inset 0 0 0 1px rgba(47, 90, 208, 0.3);
    color: var(--tt-blue);
}

/* ── 페이지 인디케이터 ── */
.trial-carousel__dots {
    display: flex;
    justify-content: center;
    gap: 5px;
    padding-bottom: 2px;
}

.trial-carousel__dot {
    width: 5px;
    height: 5px;
    border-radius: var(--tt-radius-full);
    background: #DCDFE7;
    transition: all 0.2s;
}

.trial-carousel__dot--active {
    width: 15px;
    background: var(--tt-red);
}
</style>
