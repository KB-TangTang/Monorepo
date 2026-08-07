<script setup>
defineProps({
    activeMode: {
        type: String,
        required: true,
        validator: (value) => ['personal', 'group'].includes(value),
    },
});
</script>

<template>
    <nav class="challenge-mode" aria-label="챌린지 유형 선택">
        <RouterLink
            :class="{ 'challenge-mode__link--active': activeMode === 'personal' }"
            :aria-current="activeMode === 'personal' ? 'page' : undefined"
            :to="{ name: 'personalMissionChallenge' }"
        >
            <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
                <circle cx="12" cy="8" r="3" />
                <path d="M6.5 19a5.5 5.5 0 0 1 11 0" />
            </svg>
            개인
        </RouterLink>

        <RouterLink
            :class="{ 'challenge-mode__link--active': activeMode === 'group' }"
            :aria-current="activeMode === 'group' ? 'page' : undefined"
            :to="{ name: 'groupChallenge' }"
        >
            <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
                <circle cx="8" cy="9" r="2.5" />
                <circle cx="16" cy="9" r="2.5" />
                <path d="M3.5 19a4.5 4.5 0 0 1 9 0M11.5 19a4.5 4.5 0 0 1 9 0" />
            </svg>
            그룹
        </RouterLink>
    </nav>
</template>

<style scoped src="./ChallengeModeTabBar.css"></style>
