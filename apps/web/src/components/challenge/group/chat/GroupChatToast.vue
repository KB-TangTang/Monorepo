<script setup>
defineProps({
    message: { type: String, default: '' },
});
</script>

<template>
    <Transition name="toast">
        <div v-if="message" class="chat-toast">
            {{ message }}
        </div>
    </Transition>
</template>

<style scoped>
.chat-toast {
    position: absolute;
    bottom: 80px;
    left: 50%;
    transform: translateX(-50%);
    background: rgba(27, 33, 56, 0.85);
    color: #fff;
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-semibold);
    padding: 8px 20px;
    border-radius: var(--tt-radius-full);
    white-space: nowrap;
    z-index: var(--tt-z-toast, 100);
}

.toast-enter-active {
    animation: toastIn 0.25s ease-out;
}

.toast-leave-active {
    animation: toastIn 0.2s ease-in reverse;
}

@keyframes toastIn {
    from {
        opacity: 0;
        transform: translateX(-50%) translateY(8px);
    }
    to {
        opacity: 1;
        transform: translateX(-50%) translateY(0);
    }
}
</style>
