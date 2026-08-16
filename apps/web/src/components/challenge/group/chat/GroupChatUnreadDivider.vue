<script setup>
/*
 * "여기부터 새 메시지 N" 구분선.
 *
 * 방에 들어온 시점의 안 읽은 수만큼 뒤에서 세어 그 자리에 한 번 그린다. 읽음 처리는 입장과
 * 동시에 서버에서 끝나므로, 이 선은 그 순간을 화면에 붙잡아 두는 유일한 표시다.
 */
defineProps({
    count: { type: Number, required: true },
});
</script>

<template>
    <div class="unread-divider" role="separator">
        <span class="unread-divider__line" aria-hidden="true"></span>
        <span class="unread-divider__label">여기부터 새 메시지 {{ count }}</span>
        <span class="unread-divider__line" aria-hidden="true"></span>
    </div>
</template>

<style scoped>
.unread-divider {
    display: flex;
    align-items: center;
    gap: var(--tt-space-2);
    margin: var(--tt-space-2) 0;
}

.unread-divider__line {
    flex: 1;
    height: 1px;
    background: var(--tt-danger-subtle-border);
}

.unread-divider__label {
    flex: none;
    font-size: var(--tt-fs-overline);
    font-weight: var(--tt-fw-black);
    color: var(--tt-danger);
    letter-spacing: 0.02em;
}
</style>
