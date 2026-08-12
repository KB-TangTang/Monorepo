<script setup>
import { computed } from 'vue';
import UserAvatar from '@/components/common/UserAvatar.vue';

const props = defineProps({
    challenge: { type: Object, required: true },
});

/*
 * 뱃지 상태 — "나"의 행동 기준으로 결정한다.
 *   isDefendant        → 변론필요 (내가 피고인)
 *   myVoteStatus='PENDING' → 투표중 (아직 투표 안 함)
 *   myVoteStatus='DONE'    → 투표완료
 *   그 외               → 순항중
 */
const BADGE_MAP = {
    defendant: { label: '변론필요', bg: 'var(--tt-red-soft)', color: 'var(--tt-red-deep)' },
    voting: { label: '투표중', bg: 'var(--tt-gold-soft)', color: 'var(--tt-gold-deep)' },
    voted: { label: '투표완료', bg: 'var(--tt-green-soft)', color: 'var(--tt-green)' },
    cruising: { label: '순항중', bg: 'var(--tt-bg-fill)', color: 'var(--tt-text-body)' },
};

const badgeKey = computed(() => {
    const ch = props.challenge;
    if (ch.isDefendant) return 'defendant';
    if (ch.myVoteStatus === 'PENDING') return 'voting';
    if (ch.myVoteStatus === 'DONE') return 'voted';
    return 'cruising';
});

const badgeStyle = computed(() => BADGE_MAP[badgeKey.value]);

const limitDesc = computed(() => {
    const ch = props.challenge;
    const type = ch.evalType === 'DAILY' ? '일일결산' : '기간평가';
    if (ch.limitAmount > 0) {
        const prefix = ch.evalType === 'DAILY' ? '일' : '총';
        return `${type} · ${prefix} ${ch.limitAmount.toLocaleString()}원`;
    }
    return type;
});

const AVATAR_COLORS = [
    'var(--tt-gold)',
    'var(--tt-blue)',
    'var(--tt-green)',
    'var(--tt-red)',
    'var(--tt-ink)',
    'var(--tt-gold-deep)',
];

/** 채팅 안읽음 표시 텍스트 */
const chatLabel = computed(() => {
    const count = props.challenge.unreadChatCount;
    if (count > 99) return '99+';
    if (count > 0) return String(count);
    return null;
});
</script>

<template>
    <div class="gac-card">
        <!-- 상단: 이름 + 상태 뱃지 -->
        <div class="gac-card__top">
            <span class="gac-card__name">{{ challenge.groupName }}</span>
            <span
                class="gac-card__badge"
                :style="{ background: badgeStyle.bg, color: badgeStyle.color }"
            >
                {{ badgeStyle.label }}
            </span>
        </div>

        <!-- 중간: 설명 (평가타입 · 금액 · 진행일) -->
        <p class="gac-card__desc">
            {{ limitDesc }} · {{ challenge.currentDay }}/{{ challenge.totalDays }}일차
        </p>

        <!-- 하단: 멤버 아바타 + 채팅 -->
        <div class="gac-card__bottom">
            <div class="gac-card__members">
                <div class="gac-card__avatars">
                    <span
                        v-for="(m, i) in challenge.members"
                        :key="m.userId"
                        class="gac-card__avatar-wrap"
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
                <span class="gac-card__member-count">{{ challenge.memberCount }}</span>
            </div>
            <div class="gac-card__chat">
                <div class="gac-card__chat-top">
                    <span class="gac-card__chat-time">
                        {{ challenge.lastChatTime || '' }}
                    </span>
                    <span v-if="chatLabel" class="gac-card__chat-badge">
                        {{ chatLabel }}
                    </span>
                </div>
                <span class="gac-card__chat-text">
                    {{ challenge.lastChatMessage || '대화 없음' }}
                </span>
            </div>
        </div>
    </div>
</template>

<style scoped>
.gac-card {
    background: var(--tt-bg);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-lg);
    box-shadow: var(--tt-elevation-2);
    padding: 14px 16px;
}

.gac-card__top {
    display: flex;
    align-items: center;
    justify-content: space-between;
}

.gac-card__name {
    font-size: var(--tt-fs-button);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
}

.gac-card__badge {
    font-size: var(--tt-fs-overline);
    font-weight: var(--tt-fw-black);
    padding: 3px 9px;
    border-radius: var(--tt-radius-full);
    flex: none;
}

.gac-card__desc {
    margin: 8px 0 0;
    font-size: var(--tt-fs-caption);
    color: var(--tt-text-muted);
    line-height: var(--tt-lh-normal);
}

/* ── 하단: 멤버 + 채팅 ────────── */
.gac-card__bottom {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-top: 12px;
}

.gac-card__members {
    display: flex;
    align-items: center;
    gap: 8px;
    flex: none;
}

.gac-card__avatars {
    display: flex;
    align-items: center;
}

.gac-card__avatar-wrap {
    display: inline-flex;
    position: relative;
}

/* 겹침 경계 링 — UserAvatar 자체엔 테두리가 없어 여기서 그린다 */
.gac-card__avatar-wrap :deep(.user-avatar) {
    border: 2px solid var(--tt-bg);
}

.gac-card__member-count {
    font-size: var(--tt-fs-overline);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-muted);
}

/* ── 채팅 미리보기 (2줄) ────────── */
.gac-card__chat {
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    gap: 2px;
    min-width: 0;
    flex: 1;
}

.gac-card__chat-top {
    display: flex;
    align-items: center;
    gap: 5px;
}

.gac-card__chat-time {
    font-size: var(--tt-fs-overline);
    color: var(--tt-text-hint);
}

.gac-card__chat-badge {
    flex: none;
    min-width: 18px;
    height: 18px;
    padding: 0 5px;
    border-radius: var(--tt-radius-full);
    background: var(--tt-danger);
    color: var(--tt-white);
    font-size: 10px;
    font-weight: var(--tt-fw-black);
    display: flex;
    align-items: center;
    justify-content: center;
    line-height: 1;
}

.gac-card__chat-text {
    font-size: var(--tt-fs-overline);
    color: var(--tt-text-hint);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    max-width: 160px;
}
</style>
