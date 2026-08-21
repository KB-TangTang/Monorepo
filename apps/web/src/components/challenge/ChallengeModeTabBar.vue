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
            <!--
              대법원 = 개인 미션. 사람 하나로 「나 혼자 하는 재판」을 나타낸다.
              법원 건물을 그리면 지방법원과 18px 에서 구분이 안 되고,
              저울은 자료실의 「재판 보고서」가 이미 쓰고 있다.
            -->
            <svg viewBox="0 0 24 24" aria-hidden="true">
                <circle cx="12" cy="8.5" r="3.5" />
                <path d="M5.5 19.5a6.5 6.5 0 0 1 13 0" />
            </svg>
            <span>대법원</span>
        </RouterLink>

        <RouterLink
            :class="{ 'challenge-mode__link--active': activeMode === 'group' }"
            :aria-current="activeMode === 'group' ? 'page' : undefined"
            :to="{ name: 'groupChallenge' }"
        >
            <!-- 지방법원 = 그룹 챌린지. 사람 둘로 「여럿이 하는 재판」을 나타낸다 -->
            <svg viewBox="0 0 24 24" aria-hidden="true">
                <circle cx="9" cy="8.5" r="3.2" />
                <path d="M2.5 19.5a6.5 6.5 0 0 1 13 0" />
                <circle cx="17.5" cy="9.5" r="2.3" />
                <path d="M16.5 19.5h5a5 5 0 0 0-3.2-4.7" />
            </svg>
            <span>지방법원</span>
        </RouterLink>
    </nav>
</template>

<style scoped src="./ChallengeModeTabBar.css"></style>
