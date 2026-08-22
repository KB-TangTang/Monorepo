<!--
  용도: 홈 「나의 기록」 3칸. 연속 무죄 · 이번 주 판결 · 챌린지로 아낀 돈을 나란히 보여준다.
  값을 읽기만 하는 자리라 클릭 대상이 아니다 — 각 지표의 상세는 재판탭·자료실에 이미 입구가 있다.
  시안은 Material Symbols 아이콘을 쓰지만 그 폰트를 로딩하지 않아 인라인 SVG 로 대체했다.
-->
<script setup>
import { computed } from 'vue';
import { formatHomeAmount } from '@/utils/home';

const props = defineProps({
    /* GET /api/missions/streak 의 streakCount. 아직 못 불러왔으면 null. */
    streakDays: { type: Number, default: null },
    /* { innocent, guilty } — 이번 주 판결 집계. 백엔드가 합계를 안 줘서 프론트에서 센다. */
    weekly: { type: Object, default: null },
    /* 확정된 월간 리포트의 savedAmount(덜 쓴 돈). 이슈 #450 결정 ③. */
    savedAmount: { type: Number, default: null },
});

const items = computed(() => [
    {
        key: 'streak',
        title: '연속 무죄',
        value: props.streakDays === null ? '집계 중' : `${props.streakDays}일째`,
        icon: 'fire',
    },
    {
        key: 'weekly',
        title: '이번 주 판결',
        value: props.weekly
            ? `무죄 ${props.weekly.innocent} · 유죄 ${props.weekly.guilty}`
            : '집계 중',
        icon: 'medal',
    },
    {
        key: 'saved',
        title: '아낀 돈',
        value: props.savedAmount === null ? '집계 중' : `${formatHomeAmount(props.savedAmount)}원`,
        icon: 'savings',
    },
]);
</script>

<template>
    <ul class="record-stats">
        <li v-for="item in items" :key="item.key" class="record-stats__item">
            <p class="record-stats__title">{{ item.title }}</p>
            <p class="record-stats__value">{{ item.value }}</p>

            <svg
                class="record-stats__icon"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="1.8"
                stroke-linecap="round"
                stroke-linejoin="round"
                aria-hidden="true"
            >
                <template v-if="item.icon === 'fire'">
                    <path
                        d="M12 3c3 3.5 5 6 5 9a5 5 0 0 1-10 0c0-1.6.6-3 1.7-4.3.4 1.2 1.2 2 2.3 2.3C10 8 10.6 5.4 12 3Z"
                    />
                </template>
                <template v-else-if="item.icon === 'medal'">
                    <circle cx="12" cy="15" r="5" />
                    <path d="M8.5 10.5 6 3h12l-2.5 7.5" />
                </template>
                <template v-else>
                    <path
                        d="M3 11a6 6 0 0 1 6-6h4a6 6 0 0 1 6 6v3a3 3 0 0 1-3 3h-1v2H8v-2H6a3 3 0 0 1-3-3v-3Z"
                    />
                    <path d="M16 11h.01" />
                </template>
            </svg>
        </li>
    </ul>
</template>

<style scoped>
.record-stats {
    display: flex;
    gap: var(--tt-space-2);
    list-style: none;
}

.record-stats__item {
    position: relative;
    flex: 1;
    min-width: 0;
    min-height: 88px;
    padding: var(--tt-space-3);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-lg);
    background: var(--tt-bg);
    box-shadow: var(--tt-elevation-1);
}

.record-stats__title {
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-black);
}

.record-stats__value {
    margin-top: var(--tt-space-1);
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-black);
    color: var(--tt-info);
}

.record-stats__icon {
    position: absolute;
    right: var(--tt-space-2);
    bottom: var(--tt-space-2);
    width: 21px;
    height: 21px;
    color: var(--tt-border-divider);
}
</style>
