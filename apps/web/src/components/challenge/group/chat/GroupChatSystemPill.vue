<script setup>
/*
 * systemType 이 없는 시스템 메시지의 폴백.
 *
 * 지금 서버는 systemType 을 함께 보내므로 새 메시지는 전부 카드(GroupChatRecordCard ·
 * GroupChatVerdictCard)로 그려진다. 이 pill 은 그 필드가 생기기 전에 저장된 메시지를 위한 자리다 —
 * Redis 에 남아 있는 동안(종료일 + 2일)은 계속 흘러나온다.
 */
defineProps({
    message: { type: Object, required: true },
});
</script>

<template>
    <div class="sys-pill-wrap">
        <span class="sys-pill">{{ message.content }}</span>
    </div>
</template>

<style scoped>
.sys-pill-wrap {
    display: flex;
    justify-content: center;
    padding: var(--tt-space-1) 0;
}

.sys-pill {
    max-width: 300px;
    background: var(--tt-bg-fill);
    color: var(--tt-text-muted);
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-semibold);
    line-height: 1.45;
    padding: 7px 14px;
    border-radius: var(--tt-radius-lg);
    text-align: center;
    word-break: break-word;
}
</style>
