<script setup>
import { ref } from 'vue';
import { ChevronDownIcon } from '@heroicons/vue/24/solid';
import { tangiFieldVerify } from '@/fixtures/personalChallenge';
import {
    formatWatchlistMissionRound,
    formatWatchlistRotationStatus,
} from '@/services/personalMissionFlow';

defineProps({
    items: { type: Array, required: true },
    analysisPeriod: { type: String, default: '' },
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
                <div class="watchlist__meta">
                    {{ analysisPeriod }} · 전체 소비 대비 상위 {{ items.length }}개
                </div>
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
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-lg);
    box-shadow: var(--tt-elevation-2);
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

.watchlist__title {
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
}

.watchlist__meta {
    font-size: var(--tt-fs-overline);
    color: var(--tt-text-hint);
    font-weight: var(--tt-fw-semibold);
    font-family: var(--tt-font-mono);
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
    font-size: var(--tt-fs-caption);
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
    font-size: var(--tt-fs-overline);
    font-weight: var(--tt-fw-black);
    padding: 2px var(--tt-space-2);
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
