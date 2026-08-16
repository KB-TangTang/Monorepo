<script setup>
import { computed } from 'vue';

/*
 * message 는 api/groupChatAdapter.js 가 정규화한 모양이다
 * ({ messageId, type, isSystem, senderId, senderName, content, sentAt: Date|null }).
 * 스티커는 서버 계약에 없는 목업 잔재라 이 컴포넌트에서 다루지 않는다.
 */
const props = defineProps({
    message: { type: Object, required: true },
    isMine: { type: Boolean, default: false },
});

const timeLabel = computed(() => {
    const d = props.message.sentAt;
    if (!d) return '';
    const h = d.getHours();
    const m = String(d.getMinutes()).padStart(2, '0');
    const period = h < 12 ? '오전' : '오후';
    const hour12 = h % 12 || 12;
    return `${period} ${hour12}:${m}`;
});

/* 닉네임 온보딩 전(서버가 senderNickname: null)이면 빈 문자열이 온다 */
const displayName = computed(() => props.message.senderName || '익명');
const initial = computed(() => displayName.value.charAt(0));
</script>

<template>
    <div class="bubble-row" :class="{ 'bubble-row--mine': isMine }">
        <!-- 상대 아바타 -->
        <div v-if="!isMine" class="bubble-row__avatar">
            {{ initial }}
        </div>

        <div class="bubble-row__body">
            <!-- 상대 이름 -->
            <span v-if="!isMine" class="bubble-row__name">{{ displayName }}</span>

            <div class="bubble-row__content-row">
                <!-- 내 메시지: 시간이 왼쪽 -->
                <span v-if="isMine" class="bubble-row__time">{{ timeLabel }}</span>

                <!-- 텍스트 -->
                <div
                    class="bubble-row__bubble"
                    :class="isMine ? 'bubble-row__bubble--mine' : 'bubble-row__bubble--other'"
                >
                    {{ message.content }}
                </div>

                <!-- 상대 메시지: 시간이 오른쪽 -->
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

.bubble-row__avatar {
    width: 36px;
    height: 36px;
    border-radius: 50%;
    /* 서버가 사용자별 색을 주지 않는다. 지어내지 않고 주색 하나로 통일한다 */
    background: var(--tt-primary);
    display: flex;
    align-items: center;
    justify-content: center;
    color: #fff;
    font-size: 13px;
    font-weight: var(--tt-fw-bold);
    flex: none;
}

.bubble-row__body {
    display: flex;
    flex-direction: column;
    gap: 4px;
    max-width: 70%;
}

.bubble-row__name {
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-semibold);
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
    border-radius: 16px;
    font-size: var(--tt-fs-body);
    line-height: 1.45;
    word-break: break-word;
}

.bubble-row__bubble--mine {
    background: var(--tt-info);
    color: #fff;
    border-bottom-right-radius: 4px;
}

.bubble-row__bubble--other {
    background: var(--tt-bg);
    color: var(--tt-text);
    border: 1px solid var(--tt-border);
    border-top-left-radius: 4px;
}

.bubble-row__time {
    font-size: 10.5px;
    color: var(--tt-text-hint);
    flex: none;
    white-space: nowrap;
}
</style>
