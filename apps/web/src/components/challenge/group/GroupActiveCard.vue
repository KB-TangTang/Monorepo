<script setup>
import { computed } from 'vue';
import UserAvatar from '@/components/common/UserAvatar.vue';

const props = defineProps({
    challenge: { type: Object, required: true },
});

/*
 * 카드 전체 클릭은 상세로 가고, 하단 채팅 영역만 채팅방으로 간다.
 * 목록 → 채팅방은 이 경로가 유일하다 — 상세 화면이 아직 실서버에 붙지 않아
 * 상세의 채팅 FAB 로는 방에 들어갈 수 없다(이슈 #271).
 */
defineEmits(['open-chat']);

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

/*
 * 마지막 대화 시각 — 오늘이면 시:분, 그 전이면 날짜(카카오톡과 같은 방식).
 * 서버는 ISO 문자열만 내려주고 표시 형식은 여기서 정한다.
 * 챌린지 기간이 최대 7일이라 「8/14」 같은 표기도 실제로 나온다.
 */
const chatTimeLabel = computed(() => {
    const raw = props.challenge.lastChatTime;
    if (!raw) return '';
    const d = new Date(raw);
    /* 목데이터는 「오후 2:03」·「어제」 같은 표시용 문자열을 그대로 담고 있다. 그건 손대지 않고 통과시킨다 */
    if (Number.isNaN(d.getTime())) return raw;

    const now = new Date();
    const isToday =
        d.getFullYear() === now.getFullYear() &&
        d.getMonth() === now.getMonth() &&
        d.getDate() === now.getDate();
    if (!isToday) return `${d.getMonth() + 1}/${d.getDate()}`;
    return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
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
            <!-- .stop 이 없으면 카드 전체의 @click(=상세 이동)까지 함께 타서 상세로 새어 나간다 -->
            <div class="gac-card__chat" @click.stop="$emit('open-chat')">
                <div class="gac-card__chat-top">
                    <span class="gac-card__chat-time">
                        {{ chatTimeLabel }}
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
