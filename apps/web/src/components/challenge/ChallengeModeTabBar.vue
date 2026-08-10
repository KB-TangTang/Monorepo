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
            대법원
        </RouterLink>

        <RouterLink
            :class="{ 'challenge-mode__link--active': activeMode === 'group' }"
            :aria-current="activeMode === 'group' ? 'page' : undefined"
            :to="{ name: 'groupChallenge' }"
        >
            지방법원
        </RouterLink>
    </nav>
</template>

<style scoped src="./ChallengeModeTabBar.css"></style>
