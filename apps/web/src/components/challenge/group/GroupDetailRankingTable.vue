<!--
  전체 피고인 현황 — 종료된 그룹 챌린지의 전체 멤버 순위 테이블.
  일일결산: 목숨 아이콘 + 생존/위기/탈락 뱃지
  기간결산: 승소/패소 뱃지
-->
<script setup>
import gavelAlive from '@/assets/images/challenge_live/gavel-alive.png';
import gavelDepleted from '@/assets/images/challenge_live/gavel-depleted.png';
import UserAvatar from '@/components/common/UserAvatar.vue';

const props = defineProps({
    members: { type: Array, required: true },
    evalType: { type: String, default: 'DAILY' },
    maxLives: { type: Number, default: 7 },
});

const isDaily = props.evalType === 'DAILY';

function statusBadge(m) {
    if (m.finalOutcome === 'ELIMINATED') {
        return { label: '탈락', bg: '#FBE9E4', color: '#C24B31' };
    }
    if (isDaily) {
        const lives = m.livesCount ?? 0;
        if (lives >= props.maxLives * 0.5) {
            return { label: `생존 ${lives}`, bg: '#E4F4EC', color: 'var(--tt-green)' };
        }
        return { label: `위기 ${lives}`, bg: 'var(--tt-gold-soft)', color: 'var(--tt-gold-deep)' };
    }
    return m.finalOutcome === 'SURVIVED'
        ? { label: '승소', bg: '#E4F4EC', color: 'var(--tt-green)' }
        : { label: '패소', bg: '#FBE9E4', color: '#C24B31' };
}
</script>

<template>
    <div class="ranking-table">
        <div class="ranking-table__header">
            <span class="ranking-table__title">전체 피고인 현황</span>
            <span class="ranking-table__sub">{{ isDaily ? '목숨· 금액순' : '누적액 기준' }}</span>
        </div>
        <div class="ranking-table__list">
            <div
                v-for="m in members"
                :key="m.userId"
                class="ranking-table__row"
                :class="{
                    'ranking-table__row--me': m.userId === 1,
                    'ranking-table__row--eliminated': m.finalOutcome === 'ELIMINATED',
                }"
            >
                <span class="ranking-table__rank">{{ m.finalRank }}</span>
                <UserAvatar
                    :image-url="m.profileImage"
                    :name="m.nickname"
                    :color="m.avatarColor"
                    :size="28"
                />
                <div class="ranking-table__info">
                    <div class="ranking-table__name-row">
                        <span
                            class="ranking-table__name"
                            :class="{ 'ranking-table__name--dim': m.finalOutcome === 'ELIMINATED' }"
                            >{{ m.nickname }}</span
                        >
                        <span v-if="m.userId === 1" class="ranking-table__me-badge">나</span>
                    </div>
                    <!-- 일일결산: 목숨 아이콘 -->
                    <div v-if="isDaily" class="ranking-table__lives">
                        <img
                            v-for="i in m.livesCount ?? 0"
                            :key="'a' + i"
                            :src="gavelAlive"
                            alt="남은 목숨"
                            class="ranking-table__life-icon"
                        />
                        <img
                            v-for="i in maxLives - (m.livesCount ?? 0)"
                            :key="'d' + i"
                            :src="gavelDepleted"
                            alt="차감된 목숨"
                            class="ranking-table__life-icon"
                        />
                    </div>
                </div>
                <span
                    class="ranking-table__badge"
                    :style="{
                        background: statusBadge(m).bg,
                        color: statusBadge(m).color,
                    }"
                    >{{ statusBadge(m).label }}</span
                >
            </div>
        </div>
    </div>
</template>

<style scoped>
.ranking-table__header {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
}

.ranking-table__title {
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
}

.ranking-table__sub {
    font-size: var(--tt-fs-overline);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-hint);
}

.ranking-table__list {
    margin-top: 10px;
    background: var(--tt-bg);
    border: 1px solid var(--tt-border);
    border-radius: 16px;
    box-shadow: var(--tt-elevation-2);
    overflow: hidden;
}

.ranking-table__row {
    padding: 10px 13px;
    display: flex;
    align-items: center;
    gap: 10px;
}

.ranking-table__row + .ranking-table__row {
    border-top: 1px solid var(--tt-bg-fill);
}

.ranking-table__row--me {
    background: var(--tt-bg-subtle);
}

.ranking-table__row--eliminated {
    background: #fdf4f1;
}

.ranking-table__rank {
    width: 13px;
    text-align: center;
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text-hint);
    flex: none;
}

.ranking-table__info {
    flex: 1;
    min-width: 0;
}

.ranking-table__name-row {
    display: flex;
    align-items: center;
    gap: 5px;
}

.ranking-table__name {
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
}

.ranking-table__name--dim {
    color: var(--tt-text-hint);
}

.ranking-table__me-badge {
    background: var(--tt-gold-soft);
    color: var(--tt-gold-deep);
    font-size: 9.5px;
    font-weight: var(--tt-fw-black);
    padding: 2px 6px;
    border-radius: var(--tt-radius-full);
}

.ranking-table__lives {
    display: flex;
    gap: 2px;
    margin-top: 4px;
}

.ranking-table__life-icon {
    width: 11px;
    height: 11px;
    object-fit: contain;
}

.ranking-table__badge {
    font-size: var(--tt-fs-overline);
    font-weight: var(--tt-fw-black);
    padding: 4px 10px;
    border-radius: var(--tt-radius-full);
    flex: none;
    white-space: nowrap;
}
</style>
