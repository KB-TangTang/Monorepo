<script setup>
import { storeToRefs } from 'pinia';
import { useGroupChatStore } from '@/stores/groupChat';

/*
 * 안 읽은 그룹 채팅이 있으면 지방법원 쪽에 점을 찍는다.
 *
 * **하단 5탭(TheTabBar)에는 찍지 않는다.** 거기까지 점이 번지면 「재판 탭 어딘가에 뭔가 있다」는
 * 뜻이 되어, 개인 미션 때문인지 채팅 때문인지 알 수 없다. 여기서 끊어야 점 하나가 곧 채팅이다.
 *
 * 값은 스토어에서 바로 읽는다 — 이 토글은 개인·그룹 두 홈이 함께 쓰는데, prop 으로 받으면
 * 채팅과 아무 상관없는 개인 미션 홈까지 채팅 상태를 알아야 한다.
 */
const { hasUnreadChat } = storeToRefs(useGroupChatStore());

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
            <!-- 개수는 적지 않는다. 18px 토글 안에 숫자를 넣으면 읽히지도 않고 폭이 흔들린다 -->
            <span
                v-if="hasUnreadChat"
                class="challenge-mode__dot"
                role="status"
                aria-label="안 읽은 그룹 채팅 있음"
            />
        </RouterLink>
    </nav>
</template>

<style scoped src="./ChallengeModeTabBar.css"></style>
