<script setup>
import { ref } from 'vue';
import { ChevronDownIcon } from '@heroicons/vue/24/solid';
import { tangiFieldVerify } from '@/fixtures/personalChallenge';
import {
    formatWatchlistMissionRound,
    formatWatchlistRotationStatus,
} from '@/services/personalMissionFlow';

/*
 * 분석 기간('7/20 ~ 8/16')을 그대로 보여주던 자리다.
 * 날짜 범위를 읽고 나서야 무엇을 근거로 뽑혔는지 알 수 있어 계산을 강요했다.
 * 「지난 주」라고 말로 적는다 — 로테이션이 주 단위라 이 말이 곧 그 기간이다.
 */
defineProps({
    items: { type: Array, required: true },
});

const isOpen = ref(false);

function toggle() {
    isOpen.value = !isOpen.value;
}

function statusBadgeClass(status) {
    switch (status) {
        case 'SUCCESS':
            return 'watchlist__status--cleared';
        case 'PENDING':
            return 'watchlist__status--today';
        case 'FAIL':
            return 'watchlist__status--failed';
        default:
            return 'watchlist__status--pending';
    }
}
</script>

<template>
    <div class="watchlist">
        <div class="watchlist__trigger" @click="toggle">
            <img :src="tangiFieldVerify" alt="" class="watchlist__tangi" />
            <div class="watchlist__header-text">
                <div class="watchlist__title">새로 선정된 요주의 대상</div>
                <div class="watchlist__meta">지난 주 · 소비 상위 {{ items.length }}개</div>
            </div>
            <ChevronDownIcon
                class="watchlist__chevron"
                :class="{ 'watchlist__chevron--open': isOpen }"
            />
        </div>

        <Transition name="watchlist-slide">
            <div v-if="isOpen" class="watchlist__body">
                <div class="watchlist__items">
                    <div
                        v-for="item in items"
                        :key="item.name"
                        class="watchlist__item"
                        :class="{ 'watchlist__item--muted': item.rotationResult === 'WAITING' }"
                    >
                        <div class="watchlist__item-header">
                            <span class="watchlist__item-name">
                                {{ item.name }}
                                <b class="watchlist__item-ratio">{{ item.ratio }}%</b>
                            </span>
                            <span class="watchlist__status-group">
                                <span class="watchlist__status watchlist__status--round">
                                    {{ formatWatchlistMissionRound(item) }}
                                </span>
                                <span
                                    class="watchlist__status"
                                    :class="statusBadgeClass(item.rotationResult)"
                                >
                                    {{ formatWatchlistRotationStatus(item) }}
                                </span>
                            </span>
                        </div>
                        <div class="watchlist__bar">
                            <div
                                class="watchlist__bar-fill"
                                :style="{ width: item.ratio + '%' }"
                            ></div>
                        </div>
                    </div>
                </div>
            </div>
        </Transition>
    </div>
</template>

<style scoped>
.watchlist {
    background: var(--tt-bg);
    border-radius: var(--tt-radius-lg);
    overflow: hidden;
}

.watchlist__trigger {
    padding: 14px var(--tt-space-4);
    display: flex;
    align-items: center;
    gap: 9px;
    cursor: pointer;
}

.watchlist__tangi {
    width: 34px;
    height: 34px;
    object-fit: contain;
    flex: none;
}

.watchlist__header-text {
    flex: 1;
    min-width: 0;
}

/*
 * 카드 제목은 앱의 다른 카드 제목과 같은 15px(label)이다.
 * 여기만 본문 크기(13.5px)여서 접힌 상태에서 무슨 카드인지 읽히지 않았다.
 */
.watchlist__title {
    font-size: var(--tt-fs-label);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
}

/* 숫자가 사라져 등폭이 필요 없다 — 본문 폰트로 돌린다 */
.watchlist__meta {
    margin-top: 1px;
    font-size: var(--tt-fs-caption);
    color: var(--tt-text-muted);
    font-weight: var(--tt-fw-semibold);
}

.watchlist__chevron {
    width: 20px;
    height: 20px;
    color: var(--tt-text-hint);
    flex: none;
    transition: transform 0.25s ease;
}

.watchlist__chevron--open {
    transform: rotate(180deg);
}

.watchlist__body {
    padding: 0 var(--tt-space-4) 14px;
}

.watchlist__items {
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-3);
}

.watchlist__item--muted {
    opacity: 0.62;
}

.watchlist__item-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: var(--tt-space-2);
    font-size: var(--tt-fs-body);
}

.watchlist__item-name {
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
}

.watchlist__item--muted .watchlist__item-name {
    color: var(--tt-text-body);
}

.watchlist__item-ratio {
    color: var(--tt-accent-deep);
    font-family: var(--tt-font-mono);
}

.watchlist__item--muted .watchlist__item-ratio {
    color: var(--tt-text-muted);
}

.watchlist__status {
    font-size: var(--tt-fs-badge);
    font-weight: var(--tt-fw-black);
    padding: 3px var(--tt-space-2);
    border-radius: var(--tt-radius-full);
    white-space: nowrap;
}

.watchlist__status-group {
    display: flex;
    align-items: center;
    justify-content: flex-end;
    gap: 4px;
}

.watchlist__status--round {
    background: var(--tt-bg-fill);
    color: var(--tt-text-body);
}

.watchlist__status--cleared {
    background: var(--tt-success-subtle);
    color: var(--tt-success);
}

.watchlist__status--today {
    background: var(--tt-surface-inverse);
    color: var(--tt-accent);
}

.watchlist__status--failed {
    background: var(--tt-danger-subtle);
    color: var(--tt-danger);
}

.watchlist__status--pending {
    background: var(--tt-bg-fill);
    color: var(--tt-text-muted);
}

.watchlist__bar {
    margin-top: 5px;
    height: 7px;
    border-radius: var(--tt-radius-full);
    background: var(--tt-border-track);
    overflow: hidden;
}

.watchlist__bar-fill {
    height: 100%;
    border-radius: var(--tt-radius-full);
    transition: width 0.4s ease;
}

.watchlist__item:nth-child(3n + 1) .watchlist__bar-fill {
    background: var(--tt-primary);
}

.watchlist__item:nth-child(3n + 2) .watchlist__bar-fill {
    background: var(--tt-success);
}

.watchlist__item:nth-child(3n) .watchlist__bar-fill {
    background: var(--tt-danger);
}

.watchlist__bar-fill--muted {
    background: var(--tt-tab-inactive);
}

.watchlist__comment {
    margin-top: var(--tt-space-3);
    background: var(--tt-bg-fill);
    border-radius: var(--tt-radius-sm);
    padding: 9px var(--tt-space-3);
    font-size: var(--tt-fs-badge);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-body);
    line-height: 1.5;
    word-break: keep-all;
}

/* ── 슬라이드 트랜지션 ── */
.watchlist-slide-enter-active,
.watchlist-slide-leave-active {
    transition: all 0.25s ease;
    max-height: 500px;
    overflow: hidden;
}

.watchlist-slide-enter-from,
.watchlist-slide-leave-to {
    max-height: 0;
    opacity: 0;
}
</style>
