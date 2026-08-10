<script setup>
import { BellIcon } from '@heroicons/vue/24/outline';

defineProps({
    courtImage: { type: String, required: true },
    date: { type: String, required: true },
    prosecutorImage: { type: String, default: '' },
    prosecutorName: { type: String, default: '' },
    quote: { type: String, default: '' },
    hasNotification: { type: Boolean, default: false },
    compact: { type: Boolean, default: false },
    compactTitle: { type: String, default: '' },
});

defineEmits(['notification-click']);
</script>

<template>
    <header class="court-header" :class="{ 'court-header--compact': compact }">
        <div class="court-header__gradient"></div>
        <div class="court-header__glow"></div>

        <!-- 기본 모드: 중앙 현판 + 날짜 + 검사 -->
        <div v-if="!compact" class="court-header__inner">
            <div class="court-header__sign-area">
                <div class="court-header__sign-wrap">
                    <img :src="courtImage" alt="" class="court-header__sign-img" />
                    <div class="court-header__date-badge">{{ date }}</div>
                </div>
                <button
                    type="button"
                    class="court-header__notification"
                    aria-label="알림"
                    @click="$emit('notification-click')"
                >
                    <BellIcon class="court-header__noti-icon" />
                    <span v-if="hasNotification" class="court-header__noti-dot"></span>
                </button>
            </div>

            <div v-if="prosecutorImage && quote" class="court-header__prosecutor">
                <img :src="prosecutorImage" :alt="prosecutorName" class="court-header__tangi-img" />
                <div class="court-header__speech">
                    <div class="court-header__speech-arrow"></div>
                    <h2 class="court-header__speech-text">{{ quote }}</h2>
                    <div class="court-header__speech-sub">담당 검사 · {{ prosecutorName }}</div>
                </div>
            </div>
        </div>

        <!-- 축소 모드: 좌측 현판 + 타이틀 -->
        <div v-else class="court-header__inner court-header__inner--compact">
            <div class="court-header__compact-left">
                <div class="court-header__compact-sign">
                    <img :src="courtImage" alt="" class="court-header__sign-img--compact" />
                    <div class="court-header__compact-date">{{ date }}</div>
                </div>
                <!-- eslint-disable-next-line vue/no-v-html -->
                <h2 class="court-header__compact-title" v-html="compactTitle"></h2>
            </div>
            <button
                type="button"
                class="court-header__notification"
                aria-label="알림"
                @click="$emit('notification-click')"
            >
                <BellIcon class="court-header__noti-icon" />
                <span v-if="hasNotification" class="court-header__noti-dot"></span>
            </button>
        </div>
    </header>
</template>

<style scoped src="./PersonalCourtHeader.css"></style>
