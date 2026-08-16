<script setup>
/*
 * 시스템 카드 위에 붙는 눈썹 라벨. 참여자 대화와 재판 기록을 눈으로 가른다.
 * 카드 두 종(기록/판결)이 같은 라벨을 쓰므로 여기 한 곳에만 둔다.
 */
</script>

<template>
    <div class="sys-label">
        <svg
            class="sys-label__icon"
            width="13"
            height="13"
            viewBox="0 0 16 16"
            fill="none"
            aria-hidden="true"
        >
            <path
                d="M8 2v12M4.5 14h7M3 5.5h10M3 5.5 1.5 9.5h3L3 5.5Zm10 0-1.5 4h3L13 5.5Z"
                stroke="currentColor"
                stroke-width="1.2"
                stroke-linecap="round"
                stroke-linejoin="round"
            />
        </svg>
        <span class="sys-label__text">재판 시스템</span>
    </div>
</template>

<style scoped>
.sys-label {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 6px;
    color: var(--tt-text-hint);
    margin-top: var(--tt-space-2);
}

.sys-label__icon {
    flex: none;
}

.sys-label__text {
    font-size: var(--tt-fs-overline);
    font-weight: var(--tt-fw-bold);
    letter-spacing: 0.16em;
}
</style>
