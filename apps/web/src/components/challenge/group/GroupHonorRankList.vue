<!--
  전체 피고인 현황 — 명예 법정의 전체 순위 리스트.
  순위 · 아바타 · 닉네임 · 목숨 아이콘 · 상태 뱃지를 보여준다.
  탈락자는 이름이 회색으로 표시된다.
-->
<script setup>
import gavelAlive from '@/assets/images/challenge_live/gavel-alive.png';
import gavelDepleted from '@/assets/images/challenge_live/gavel-depleted.png';

defineProps({
    rankings: { type: Array, required: true },
});

const STATUS_STYLE = {
    alive: { bg: 'var(--tt-green-soft)', color: 'var(--tt-green)' },
    danger: { bg: 'var(--tt-gold-soft)', color: 'var(--tt-gold-deep)' },
    eliminated: { bg: 'var(--tt-red-soft)', color: 'var(--tt-red-deep)' },
};

function getStatusStyle(type) {
    return STATUS_STYLE[type] || STATUS_STYLE.alive;
}
</script>

<template>
    <div class="rank-section">
        <div class="rank-section__header">
            <span class="rank-section__title">전체 피고인 현황</span>
            <span class="rank-section__sub">목숨 · 금액순</span>
        </div>

        <div class="rank-list">
            <div
                v-for="(r, idx) in rankings"
                :key="r.userId"
                class="rank-list__row"
                :class="{
                    'rank-list__row--me': r.isMe,
                    'rank-list__row--eliminated': r.isEliminated,
                    'rank-list__row--bordered': idx > 0,
                }"
            >
                <span class="rank-list__rank">{{ r.rank }}</span>

                <div
                    class="rank-list__avatar"
                    :style="{ background: r.avatarColor }"
                >
                    {{ r.initial }}
                </div>

                <div class="rank-list__info">
                    <div class="rank-list__name-row">
                        <span
                            class="rank-list__name"
                            :class="{ 'rank-list__name--dim': r.isEliminated }"
                        >
                            {{ r.nickname }}
                        </span>
                        <span v-if="r.isMe" class="rank-list__me-badge">나</span>
                    </div>
                    <div class="rank-list__lives">
                        <img
                            v-for="i in r.livesCount"
                            :key="'a' + i"
                            :src="gavelAlive"
                            alt="남은 목숨"
                            class="rank-list__life-icon"
                        >
                        <img
                            v-for="i in (r.maxLives - r.livesCount)"
                            :key="'d' + i"
                            :src="gavelDepleted"
                            alt="차감된 목숨"
                            class="rank-list__life-icon"
                        >
                    </div>
                </div>

                <span
                    class="rank-list__status"
                    :style="{
                        background: getStatusStyle(r.statusType).bg,
                        color: getStatusStyle(r.statusType).color,
                    }"
                >
                    {{ r.statusLabel }}
                </span>
            </div>
        </div>
    </div>
</template>

<style scoped>
.rank-section__header {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
}

.rank-section__title {
    font-size: var(--tt-fs-button);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
}

.rank-section__sub {
    font-size: var(--tt-fs-overline);
    color: var(--tt-text-muted);
    font-weight: var(--tt-fw-bold);
}

/* ── 리스트 ── */
.rank-list {
    margin-top: 10px;
    background: var(--tt-bg);
    border: 1px solid var(--tt-border);
    border-radius: 16px;
    box-shadow: var(--tt-elevation-2);
    overflow: hidden;
}

.rank-list__row {
    padding: 10px 13px;
    display: flex;
    align-items: center;
    gap: 10px;
}

.rank-list__row--me {
    background: var(--tt-bg-subtle);
}

.rank-list__row--eliminated {
    background: #FDF4F1;
}

.rank-list__row--bordered {
    border-top: 1px solid var(--tt-bg-fill);
}

.rank-list__rank {
    width: 13px;
    text-align: center;
    font-size: 12.5px;
    font-weight: var(--tt-fw-black);
    color: var(--tt-text-hint);
}

.rank-list__avatar {
    width: 28px;
    height: 28px;
    border-radius: 50%;
    color: var(--tt-white);
    font-size: var(--tt-fs-badge);
    font-weight: var(--tt-fw-black);
    display: flex;
    align-items: center;
    justify-content: center;
    flex: none;
}

.rank-list__info {
    flex: 1;
    min-width: 0;
}

.rank-list__name-row {
    display: flex;
    align-items: center;
    gap: 5px;
}

.rank-list__name {
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
}

.rank-list__name--dim {
    color: var(--tt-text-muted);
}

.rank-list__me-badge {
    background: var(--tt-gold-soft);
    color: var(--tt-gold-deep);
    font-size: 9.5px;
    font-weight: var(--tt-fw-black);
    padding: 2px 6px;
    border-radius: var(--tt-radius-full);
}

.rank-list__lives {
    display: flex;
    gap: 2px;
    margin-top: 4px;
}

.rank-list__life-icon {
    width: 11px;
    height: 11px;
    object-fit: contain;
}

.rank-list__status {
    font-size: var(--tt-fs-overline);
    font-weight: var(--tt-fw-black);
    padding: 4px 10px;
    border-radius: var(--tt-radius-full);
    flex: none;
}
</style>
