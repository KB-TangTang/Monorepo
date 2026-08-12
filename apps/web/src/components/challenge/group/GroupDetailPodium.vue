<!--
  시상대 — 종료된 그룹 챌린지 상위 3명을 시각적으로 표시한다.
  1위가 가운데, 2위 왼쪽, 3위 오른쪽 — 높이가 다른 컬럼 형태.
-->
<script setup>
import UserAvatar from '@/components/common/UserAvatar.vue';

const props = defineProps({
    members: { type: Array, required: true },
});

/* 시상대 표시 순서: 2위(좌) · 1위(중) · 3위(우) */
const podiumOrder = [
    props.members.find((m) => m.finalRank === 2),
    props.members.find((m) => m.finalRank === 1),
    props.members.find((m) => m.finalRank === 3),
].filter(Boolean);
</script>

<template>
    <div class="podium">
        <!-- 크라운 -->
        <svg class="podium__crown" width="24" height="18" viewBox="0 0 24 18">
            <path d="M2 16V5l5.2 4L12 2l4.8 7L22 5v11z" fill="var(--tt-gold)" />
        </svg>

        <div class="podium__columns">
            <div
                v-for="m in podiumOrder"
                :key="m.userId"
                class="podium__col"
                :class="{ 'podium__col--first': m.finalRank === 1 }"
            >
                <span
                    class="podium__avatar-wrap"
                    :class="{ 'podium__avatar-wrap--first': m.finalRank === 1 }"
                >
                    <UserAvatar
                        :image-url="m.profileImage"
                        :name="m.nickname"
                        :color="m.avatarColor"
                        :size="m.finalRank === 1 ? 54 : 38"
                    />
                </span>
                <div class="podium__name-row">
                    <span
                        class="podium__name"
                        :class="{ 'podium__name--first': m.finalRank === 1 }"
                        >{{ m.nickname }}</span
                    >
                    <span v-if="m.userId === 1" class="podium__me-badge">나</span>
                </div>
                <div class="podium__rank-bar" :class="`podium__rank-bar--r${m.finalRank}`">
                    {{ m.finalRank }}
                </div>
            </div>
        </div>
    </div>
</template>

<style scoped>
.podium {
    background: var(--tt-ink);
    border-radius: 20px;
    padding: 16px 14px 0;
    position: relative;
    overflow: hidden;
    box-shadow: 0 14px 30px -16px rgba(35, 40, 66, 0.55);
}

.podium__crown {
    position: absolute;
    left: 50%;
    top: 8px;
    transform: translateX(-50%);
    z-index: 3;
}

.podium__columns {
    display: flex;
    align-items: flex-end;
    justify-content: center;
    gap: 8px;
    position: relative;
    z-index: 2;
}

.podium__col {
    flex: 1;
    text-align: center;
}

.podium__avatar-wrap {
    display: flex;
    margin: 0 auto;
    border-radius: var(--tt-radius-full);
    box-shadow: 0 0 0 2px rgba(255, 255, 255, 0.18);
}

.podium__avatar-wrap--first {
    box-shadow: 0 0 0 5px rgba(245, 185, 33, 0.24);
}

.podium__name-row {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 4px;
    margin-top: 6px;
}

.podium__name {
    font-size: var(--tt-fs-badge);
    font-weight: var(--tt-fw-black);
    color: var(--tt-white);
}

.podium__name--first {
    font-size: var(--tt-fs-body);
}

.podium__me-badge {
    background: rgba(245, 185, 33, 0.2);
    color: var(--tt-gold);
    font-size: 9px;
    font-weight: var(--tt-fw-black);
    padding: 2px 5px;
    border-radius: var(--tt-radius-full);
}

.podium__rank-bar {
    margin-top: 8px;
    border-radius: 10px 10px 0 0;
    font-weight: var(--tt-fw-black);
}

.podium__rank-bar--r1 {
    padding: 26px 0;
    font-size: 24px;
    background: var(--tt-gold);
    color: var(--tt-ink);
}

.podium__rank-bar--r2 {
    padding: 15px 0;
    font-size: 17px;
    background: rgba(255, 255, 255, 0.1);
    color: #aeb2cc;
}

.podium__rank-bar--r3 {
    padding: 9px 0;
    font-size: 17px;
    background: rgba(255, 255, 255, 0.1);
    color: #aeb2cc;
}
</style>
