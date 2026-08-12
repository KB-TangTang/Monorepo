<script setup>
import { computed } from 'vue';
import UserAvatar from '@/components/common/UserAvatar.vue';

const props = defineProps({
    challenge: { type: Object, required: true },
});

const isWin = computed(() => props.challenge.finalOutcome === 'SURVIVED');

const rankBadgeBg = computed(() => (isWin.value ? 'var(--tt-green-soft)' : 'var(--tt-red-soft)'));
const rankBadgeColor = computed(() => (isWin.value ? 'var(--tt-green)' : 'var(--tt-red-deep)'));

/** 날짜 범위: 07.22 ~ 07.28 */
const dateRange = computed(() => {
    const fmt = (iso) => {
        if (!iso) return '';
        const [, m, d] = iso.split('-');
        return `${m}.${d}`;
    };
    return `${fmt(props.challenge.startDate)} ~ ${fmt(props.challenge.endDate)}`;
});

/** 하단 우측: 절약액 또는 벌금 */
const outcomeText = computed(() => {
    const ch = props.challenge;
    if (isWin.value && ch.savingsAmount > 0) {
        return `절약 ${ch.savingsAmount.toLocaleString()}원`;
    }
    if (!isWin.value && ch.finalChargeAmount > 0) {
        return `초과 ${ch.finalChargeAmount.toLocaleString()}원`;
    }
    return null;
});

const evalDesc = computed(() => (props.challenge.evalType === 'DAILY' ? '일일결산' : '기간평가'));

const AVATAR_COLORS = [
    'var(--tt-gold)',
    'var(--tt-blue)',
    'var(--tt-green)',
    'var(--tt-red)',
    'var(--tt-ink)',
    'var(--tt-gold-deep)',
];
</script>

<template>
    <div class="gec-card">
        <!-- 상단: 이름 + 순위 뱃지 -->
        <div class="gec-card__top">
            <span class="gec-card__name">{{ challenge.groupName }}</span>
            <span
                class="gec-card__badge"
                :style="{ background: rankBadgeBg, color: rankBadgeColor }"
            >
                {{ challenge.finalRank }}위
            </span>
        </div>

        <!-- 중간: 평가타입 · 날짜 범위 -->
        <p class="gec-card__desc">{{ evalDesc }} · {{ dateRange }}</p>

        <!-- 하단: 멤버 아바타 + 결과 금액 -->
        <div class="gec-card__bottom">
            <div class="gec-card__members">
                <div class="gec-card__avatars">
                    <span
                        v-for="(m, i) in challenge.members"
                        :key="m.userId"
                        class="gec-card__avatar-wrap"
                        :style="{
                            marginLeft: i > 0 ? '-7px' : '0',
                            zIndex: challenge.members.length - i,
                        }"
                    >
                        <UserAvatar
                            :image-url="m.profileImage || null"
                            :name="m.nickname"
                            :color="AVATAR_COLORS[i % AVATAR_COLORS.length]"
                            :size="24"
                        />
                    </span>
                </div>
                <span class="gec-card__member-count">{{ challenge.memberCount }}</span>
            </div>
            <span
                v-if="outcomeText"
                class="gec-card__outcome"
                :class="isWin ? 'gec-card__outcome--win' : 'gec-card__outcome--lose'"
            >
                {{ outcomeText }}
            </span>
            <span v-else class="gec-card__link">판결기록 ›</span>
        </div>
    </div>
</template>

<style scoped>
.gec-card {
    background: var(--tt-bg);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-lg);
    box-shadow: var(--tt-elevation-2);
    padding: 14px 16px;
}

.gec-card__top {
    display: flex;
    align-items: center;
    justify-content: space-between;
}

.gec-card__name {
    font-size: var(--tt-fs-button);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
}

.gec-card__badge {
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-black);
    padding: 4px 10px;
    border-radius: var(--tt-radius-full);
    flex: none;
}

.gec-card__desc {
    margin: 8px 0 0;
    font-size: var(--tt-fs-caption);
    color: var(--tt-text-muted);
    line-height: var(--tt-lh-normal);
}

/* ── 하단: 멤버 + 결과 ────────── */
.gec-card__bottom {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-top: 12px;
}

.gec-card__members {
    display: flex;
    align-items: center;
    gap: 8px;
    flex: none;
}

.gec-card__avatars {
    display: flex;
    align-items: center;
}

.gec-card__avatar-wrap {
    display: inline-flex;
    position: relative;
}

/* 겹침 경계 링 — UserAvatar 자체엔 테두리가 없어 여기서 그린다 */
.gec-card__avatar-wrap :deep(.user-avatar) {
    border: 2px solid var(--tt-bg);
}

.gec-card__member-count {
    font-size: var(--tt-fs-overline);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-muted);
}

.gec-card__outcome {
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
    white-space: nowrap;
}

.gec-card__outcome--win {
    color: var(--tt-green);
}

.gec-card__outcome--lose {
    color: var(--tt-red-deep);
}

.gec-card__link {
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-blue);
    white-space: nowrap;
}
</style>
