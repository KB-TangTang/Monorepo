<script setup>
import { computed } from 'vue';
import UserAvatar from '@/components/common/UserAvatar.vue';

/*
 * message 는 api/groupChatAdapter.js 가 정규화한 모양이다
 * ({ messageId, type, isSystem, senderId, senderName, content, sentAt: Date|null }).
 *
 * 아바타 색은 공용 UserAvatar 가 이름 해시로 정한다. 화면마다 각자 이니셜 원을 그리지 않는다.
 */
const props = defineProps({
    message: { type: Object, required: true },
    isMine: { type: Boolean, default: false },
    /* 같은 사람이 연달아 보낸 메시지면 이름·아바타를 반복하지 않는다 */
    grouped: { type: Boolean, default: false },
});

const timeLabel = computed(() => {
    const d = props.message.sentAt;
    if (!d) return '';
    return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
});

/* 닉네임 온보딩 전(서버가 senderNickname: null)이면 빈 문자열이 온다 */
const displayName = computed(() => props.message.senderName || '익명');
</script>

<template>
    <div class="bubble-row" :class="{ 'bubble-row--mine': isMine, 'bubble-row--grouped': grouped }">
        <div v-if="!isMine" class="bubble-row__avatar-slot">
            <UserAvatar v-if="!grouped" :name="displayName" :size="34" />
        </div>

        <div class="bubble-row__body">
            <span v-if="!isMine && !grouped" class="bubble-row__name">{{ displayName }}</span>

            <div class="bubble-row__content-row">
                <span v-if="isMine" class="bubble-row__time">{{ timeLabel }}</span>

                <div
                    class="bubble-row__bubble"
                    :class="isMine ? 'bubble-row__bubble--mine' : 'bubble-row__bubble--other'"
                >
                    {{ message.content }}
                </div>

                <span v-if="!isMine" class="bubble-row__time">{{ timeLabel }}</span>
            </div>
        </div>
    </div>
</template>

<style scoped>
.bubble-row {
    display: flex;
    gap: var(--tt-space-2);
    align-items: flex-start;
}

.bubble-row--mine {
    flex-direction: row-reverse;
}

/* 연속 메시지는 아바타 자리만 비워 말풍선 왼쪽 선을 맞춘다 */
.bubble-row__avatar-slot {
    width: 34px;
    flex: none;
}

.bubble-row--grouped {
    margin-top: -6px;
}

.bubble-row__body {
    display: flex;
    flex-direction: column;
    gap: 4px;
    max-width: 74%;
}

.bubble-row__name {
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-muted);
    padding-left: 2px;
}

.bubble-row__content-row {
    display: flex;
    align-items: flex-end;
    gap: 6px;
}

.bubble-row__bubble {
    padding: 10px 14px;
    border-radius: var(--tt-radius-lg);
    font-size: var(--tt-fs-body);
    line-height: 1.5;
    word-break: break-word;
}

.bubble-row__bubble--mine {
    background: var(--tt-info);
    color: var(--tt-text-inverse);
    border-bottom-right-radius: 6px;
}

.bubble-row__bubble--other {
    background: var(--tt-bg);
    color: var(--tt-text);
    border-top-left-radius: 6px;
    box-shadow: var(--tt-elevation-1);
}

.bubble-row__time {
    font-size: var(--tt-fs-overline);
    color: var(--tt-text-hint);
    flex: none;
    white-space: nowrap;
    padding-bottom: 2px;
}
</style>
