<script setup>
import { computed } from 'vue';
import { STICKER_MAP } from '@/constants/stickerCatalog';

const props = defineProps({
    message: { type: Object, required: true },
    isMine: { type: Boolean, default: false },
});

const isSticker = computed(() => props.message.contentType === 'STICKER');
const stickerSrc = computed(() => {
    if (!isSticker.value) return null;
    return STICKER_MAP[props.message.content]?.src ?? null;
});

const timeLabel = computed(() => {
    const d = new Date(props.message.createdAt);
    const h = d.getHours();
    const m = String(d.getMinutes()).padStart(2, '0');
    const period = h < 12 ? '오전' : '오후';
    const hour12 = h % 12 || 12;
    return `${period} ${hour12}:${m}`;
});

const initial = computed(() => {
    const name = props.message.senderName ?? '';
    return name.charAt(0);
});
</script>

<template>
    <div class="bubble-row" :class="{ 'bubble-row--mine': isMine }">
        <!-- 상대 아바타 -->
        <div v-if="!isMine" class="bubble-row__avatar" :style="{ background: message.senderColor }">
            {{ initial }}
        </div>

        <div class="bubble-row__body">
            <!-- 상대 이름 -->
            <span v-if="!isMine" class="bubble-row__name">{{ message.senderName }}</span>

            <div class="bubble-row__content-row">
                <!-- 내 메시지: 시간이 왼쪽 -->
                <span v-if="isMine" class="bubble-row__time">{{ timeLabel }}</span>

                <!-- 스티커 -->
                <img
                    v-if="isSticker && stickerSrc"
                    :src="stickerSrc"
                    :alt="message.content"
                    class="bubble-row__sticker"
                />

                <!-- 스티커 로드 실패 -->
                <div v-else-if="isSticker" class="bubble-row__sticker-fallback">
                    스티커
                </div>

                <!-- 텍스트 -->
                <div
                    v-else
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

.bubble-row--mine .bubble-row__content-row {
    flex-direction: row-reverse;
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
    border-bottom-left-radius: 4px;
}

.bubble-row__sticker {
    width: 100px;
    height: 100px;
    object-fit: contain;
}

.bubble-row__sticker-fallback {
    width: 100px;
    height: 100px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: var(--tt-bg-fill);
    border-radius: var(--tt-radius-md);
    color: var(--tt-text-hint);
    font-size: var(--tt-fs-caption);
}

.bubble-row__time {
    font-size: 10.5px;
    color: var(--tt-text-hint);
    flex: none;
    white-space: nowrap;
}
</style>
