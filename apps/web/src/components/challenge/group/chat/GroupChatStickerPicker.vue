<script setup>
import { ref, computed } from 'vue';
import { STICKER_CATEGORIES, getStickersByCategory } from '@/constants/stickerCatalog';

const emit = defineEmits(['select', 'close']);

const activeTab = ref(STICKER_CATEGORIES[0].key);

const stickers = computed(() => getStickersByCategory(activeTab.value));
</script>

<template>
    <div class="sticker-picker">
        <!-- 탭 -->
        <div class="sticker-picker__tabs">
            <button
                v-for="cat in STICKER_CATEGORIES"
                :key="cat.key"
                class="sticker-picker__tab"
                :class="{ 'sticker-picker__tab--active': activeTab === cat.key }"
                @click="activeTab = cat.key"
            >
                {{ cat.label }}
            </button>
            <button class="sticker-picker__close" @click="emit('close')">
                <svg width="18" height="18" viewBox="0 0 18 18" fill="none">
                    <path d="M4 4l10 10M14 4L4 14" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
                </svg>
            </button>
        </div>

        <!-- 그리드 -->
        <div class="sticker-picker__grid">
            <button
                v-for="s in stickers"
                :key="s.id"
                class="sticker-picker__item"
                @click="emit('select', s.id)"
            >
                <img :src="s.src" :alt="s.label" />
            </button>
        </div>
    </div>
</template>

<style scoped>
.sticker-picker {
    flex: none;
    background: var(--tt-bg);
    border-top: 1px solid var(--tt-border);
    animation: slideUp 0.2s ease-out;
}

@keyframes slideUp {
    from { transform: translateY(100%); }
    to { transform: translateY(0); }
}

.sticker-picker__tabs {
    display: flex;
    align-items: center;
    border-bottom: 1px solid var(--tt-border);
    padding: 0 var(--tt-screen-padding);
}

.sticker-picker__tab {
    flex: 1;
    border: none;
    background: none;
    padding: 10px 0;
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-semibold);
    color: var(--tt-text-muted);
    cursor: pointer;
    border-bottom: 2px solid transparent;
}

.sticker-picker__tab--active {
    color: var(--tt-primary);
    border-bottom-color: var(--tt-primary);
    font-weight: var(--tt-fw-black);
}

.sticker-picker__close {
    border: none;
    background: none;
    color: var(--tt-text-hint);
    cursor: pointer;
    padding: 8px;
    flex: none;
}

.sticker-picker__grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: var(--tt-space-2);
    padding: var(--tt-space-3) var(--tt-screen-padding);
    max-height: 240px;
    overflow-y: auto;
    scrollbar-width: none;
}

.sticker-picker__grid::-webkit-scrollbar {
    display: none;
}

.sticker-picker__item {
    border: none;
    background: none;
    cursor: pointer;
    padding: 6px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: var(--tt-radius-md);
}

.sticker-picker__item:active {
    background: var(--tt-bg-fill);
}

.sticker-picker__item img {
    width: 56px;
    height: 56px;
    object-fit: contain;
}
</style>
