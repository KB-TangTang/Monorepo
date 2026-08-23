<script setup>
import { computed } from 'vue';
import UserAvatar from '@/components/common/UserAvatar.vue';
import { clockLabel } from '@/utils/groupChat';

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
    /*
     * 같은 사람이 같은 분에 연달아 보내면 시간은 묶음의 마지막 줄에만 남긴다(카카오톡 방식).
     * 판정은 다음 메시지를 봐야 해서 목록을 만드는 GroupChatView 가 한다 —
     * utils/groupChat.js 의 shouldShowTime 이다. 기본값 true 는 단독 사용 시 기존 동작 유지용.
     */
    showTime: { type: Boolean, default: true },
    /*
     * 발신자 프로필 이미지. 채팅 메시지에는 이 값이 없다 — 메시지가 Redis 에 저장돼
     * 발송 시점 값이 굳기 때문에 일부러 싣지 않는다(프로필을 바꿔도 과거 메시지가 안 바뀐다).
     * 그릴 때마다 멤버 목록에서 찾아 넘긴다. 없으면 UserAvatar 가 이니셜 원으로 그린다.
     */
    avatarUrl: { type: String, default: null },
    /*
     * 발신자의 **지금** 닉네임. 메시지에 실려 온 senderName 은 Redis 에 저장된 발송 시점
     * 값이라 닉네임을 바꿔도 과거 메시지가 옛 이름으로 남는다(이슈 #414).
     * 멤버 목록에 없는 사람(나간 참여자 등)이면 null 이 오고, 그때만 메시지 값으로 되돌아간다.
     */
    senderName: { type: String, default: null },
});

/* 재판 알림 필도 같은 자리에 같은 모양으로 시각을 찍는다 — 사본을 두지 않는다 */
const timeLabel = computed(() => clockLabel(props.message.sentAt));

/* 닉네임 온보딩 전(서버가 senderNickname: null)이면 빈 문자열이 온다 */
const displayName = computed(() => props.senderName || props.message.senderName || '익명');
</script>

<template>
    <div class="bubble-row" :class="{ 'bubble-row--mine': isMine, 'bubble-row--grouped': grouped }">
        <div v-if="!isMine" class="bubble-row__avatar-slot">
            <UserAvatar v-if="!grouped" :image-url="avatarUrl" :name="displayName" :size="34" />
        </div>

        <div class="bubble-row__body">
            <span v-if="!isMine && !grouped" class="bubble-row__name">{{ displayName }}</span>

            <div class="bubble-row__content-row">
                <span v-if="isMine && showTime" class="bubble-row__time">{{ timeLabel }}</span>

                <div
                    class="bubble-row__bubble"
                    :class="isMine ? 'bubble-row__bubble--mine' : 'bubble-row__bubble--other'"
                >
                    {{ message.content }}
                </div>

                <span v-if="!isMine && showTime" class="bubble-row__time">{{ timeLabel }}</span>
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
