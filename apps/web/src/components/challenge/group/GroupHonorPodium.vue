<!--
  시상대 — 명예 법정 TOP 3 포디움.
  1위는 가운데(높은 단), 2위는 왼쪽, 3위는 오른쪽 배치.
  인원이 2명이면 1위 왼쪽 · 2위 오른쪽.
  Ink 배경 위에 왕관 + 아바타 + 닉네임 + 단 높이 차이를 표현한다.
-->
<script setup>
import { computed } from 'vue';

const props = defineProps({
    podium: { type: Array, required: true },
});

/* 디자인 순서: 2위(왼) → 1위(중앙) → 3위(오) */
const ordered = computed(() => {
    const p = props.podium;
    if (p.length === 2) return [p[0], p[1]]; // 2명이면 왼·오 배치
    if (p.length >= 3) return [p[1], p[0], p[2]]; // 2위, 1위, 3위
    return p;
});

const PODIUM_HEIGHT = { 1: 26, 2: 15, 3: 9 };
const AVATAR_SIZE = { 1: 54, 2: 38, 3: 38 };

function podiumPadding(rank) {
    return PODIUM_HEIGHT[rank] ?? 9;
}

function avatarSize(rank) {
    return AVATAR_SIZE[rank] ?? 38;
}
</script>

<template>
    <div class="podium">
        <!-- 왕관 -->
        <div class="podium__crown">
            <svg width="24" height="18" viewBox="0 0 24 18">
                <path d="M2 16V5l5.2 4L12 2l4.8 7L22 5v11z" fill="var(--tt-gold)" />
            </svg>
        </div>

        <div class="podium__row">
            <div
                v-for="(p, idx) in ordered"
                :key="p.userId"
                class="podium__col"
            >
                <!-- 아바타 -->
                <div
                    class="podium__avatar"
                    :style="{
                        width: avatarSize(p.rank) + 'px',
                        height: avatarSize(p.rank) + 'px',
                        background: p.avatarColor,
                        boxShadow: p.rank === 1
                            ? '0 0 0 5px rgba(245,185,33,.24)'
                            : '0 0 0 2px rgba(255,255,255,.18)',
                        fontSize: p.rank === 1 ? '18px' : '14px',
                    }"
                >
                    {{ p.initial }}
                </div>

                <!-- 닉네임 + "나" 뱃지 -->
                <div class="podium__name-row">
                    <span
                        class="podium__name"
                        :class="{ 'podium__name--1st': p.rank === 1 }"
                    >
                        {{ p.nickname }}
                    </span>
                    <span v-if="p.isMe" class="podium__me-badge">나</span>
                </div>

                <!-- 단 -->
                <div
                    class="podium__stand"
                    :class="{
                        'podium__stand--1st': p.rank === 1,
                        'podium__stand--other': p.rank !== 1,
                    }"
                    :style="{ padding: podiumPadding(p.rank) + 'px 0' }"
                >
                    <span
                        class="podium__rank-num"
                        :class="{ 'podium__rank-num--1st': p.rank === 1 }"
                    >
                        {{ p.rank }}
                    </span>
                </div>
            </div>
        </div>
    </div>
</template>

<style scoped>
.podium {
    background: var(--tt-ink);
    border-radius: var(--tt-radius-xl);
    padding: 16px 14px 0;
    position: relative;
    overflow: hidden;
    box-shadow: var(--tt-elevation-4);
}

.podium__crown {
    position: absolute;
    left: 50%;
    top: 8px;
    transform: translateX(-50%);
    z-index: 3;
}

.podium__row {
    display: flex;
    align-items: flex-end;
    justify-content: center;
    gap: 8px;
    position: relative;
    z-index: 2;
}

.podium__col {
    flex: 1;
    text-align: center;
}

.podium__avatar {
    border-radius: 50%;
    color: var(--tt-white);
    font-weight: var(--tt-fw-black);
    display: flex;
    align-items: center;
    justify-content: center;
    margin: 0 auto;
    object-fit: cover;
}

.podium__name-row {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 4px;
    margin-top: 6px;
}

.podium__name {
    font-size: var(--tt-fs-badge);
    font-weight: var(--tt-fw-black);
    color: var(--tt-white);
}

.podium__name--1st {
    font-size: var(--tt-fs-body);
}

.podium__me-badge {
    background: rgba(245, 185, 33, 0.2);
    color: var(--tt-gold);
    font-size: 9px;
    font-weight: var(--tt-fw-black);
    padding: 2px 5px;
    border-radius: var(--tt-radius-full);
}

.podium__stand {
    margin-top: 8px;
    border-radius: 10px 10px 0 0;
    text-align: center;
}

.podium__stand--1st {
    background: var(--tt-gold);
}

.podium__stand--other {
    background: rgba(255, 255, 255, 0.1);
}

.podium__rank-num {
    font-size: 17px;
    font-weight: var(--tt-fw-black);
    color: #AEB2CC;
}

.podium__rank-num--1st {
    font-size: 24px;
    color: var(--tt-ink);
}
</style>
