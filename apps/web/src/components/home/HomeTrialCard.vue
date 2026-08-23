<!--
  용도: 홈의 재판 입구 카드. 대법원(개인 챌린지) · 지방법원(그룹 챌린지) 두 줄로
        지금 상태를 요약하고 각각 해당 화면으로 보낸다.
  아이콘은 인라인 SVG 다 — 법원 건물 일러스트(building_*_v2.png)는 어두운 배경에 문구까지 박힌
  가로형 배너라 36px 정사각 배지 안에서는 무엇인지 알아볼 수 없었다(#450 수정).
  개인/그룹을 사람 수로 구분한다. 캡션 뒷말(개인챌린지 · 그룹챌린지)과 같은 뜻이라 읽기가 어긋나지 않는다.
-->
<script setup>
import { computed } from 'vue';
import BaseCard from '@/components/common/BaseCard.vue';
import tangImage from '@/assets/images/tangtang-cutout.png';

const props = defineProps({
    /* { text: 표시 문구, badge: 우측 뱃지 문구 | null } — 아직 불러오는 중이면 null */
    personal: { type: Object, default: null },
    group: { type: Object, default: null },
});

const emit = defineEmits(['open-personal', 'open-group']);

const rows = computed(() => [
    {
        key: 'personal',
        caption: '대법원 · 개인챌린지',
        icon: 'person',
        tone: 'info',
        text: props.personal?.text ?? '불러오는 중이에요',
        badge: props.personal?.badge ?? null,
        badgeTone: 'info',
        event: 'open-personal',
    },
    {
        key: 'group',
        caption: '지방법원 · 그룹챌린지',
        icon: 'group',
        tone: 'accent',
        text: props.group?.text ?? '불러오는 중이에요',
        badge: props.group?.badge ?? null,
        badgeTone: 'danger',
        event: 'open-group',
    },
]);
</script>

<template>
    <BaseCard class="trial" padding="none">
        <h2 class="trial__headline">오늘 재판도 무죄로<br />마무리해 보세요!</h2>

        <img class="trial__mascot" :src="tangImage" alt="" />

        <ul class="trial__rows">
            <li v-for="row in rows" :key="row.key">
                <button type="button" class="trial-row" @click="emit(row.event)">
                    <span
                        class="trial-row__icon"
                        :class="`trial-row__icon--${row.tone}`"
                        aria-hidden="true"
                    >
                        <svg
                            viewBox="0 0 24 24"
                            fill="none"
                            stroke="currentColor"
                            stroke-width="1.8"
                            stroke-linecap="round"
                            stroke-linejoin="round"
                        >
                            <template v-if="row.icon === 'person'">
                                <circle cx="12" cy="8.5" r="3.3" />
                                <path d="M5.5 19.5c0-3.3 2.9-5.4 6.5-5.4s6.5 2.1 6.5 5.4" />
                            </template>
                            <template v-else>
                                <circle cx="9.2" cy="9.2" r="2.9" />
                                <path d="M3.2 19.2c0-2.9 2.7-4.7 6-4.7s6 1.8 6 4.7" />
                                <path d="M16.6 6.7a2.9 2.9 0 0 1 0 5.4" />
                                <path d="M18.2 14.9c1.7.7 2.6 2.1 2.6 4" />
                            </template>
                        </svg>
                    </span>

                    <span class="trial-row__body">
                        <span class="trial-row__caption">{{ row.caption }}</span>
                        <span class="trial-row__headline">
                            <span class="trial-row__text">{{ row.text }}</span>
                            <span
                                v-if="row.badge"
                                class="trial-row__badge"
                                :class="`trial-row__badge--${row.badgeTone}`"
                            >
                                {{ row.badge }}
                            </span>
                        </span>
                    </span>

                    <span class="trial-row__chevron" aria-hidden="true">›</span>
                </button>
            </li>
        </ul>
    </BaseCard>
</template>

<style scoped>
.trial {
    position: relative;
    padding: var(--tt-space-4) var(--tt-space-4) var(--tt-space-2);
    border-radius: var(--tt-radius-xl);
}

.trial__headline {
    padding-right: 72px;
    font-size: var(--tt-fs-subtitle);
    font-weight: var(--tt-fw-black);
    line-height: var(--tt-lh-snug);
    letter-spacing: -0.01em;
}

.trial__mascot {
    position: absolute;
    top: var(--tt-space-2);
    right: var(--tt-space-3);
    width: 64px;
    height: auto;
    pointer-events: none;
}

.trial__rows {
    margin-top: var(--tt-space-3);
    list-style: none;
}

.trial__rows > li + li {
    border-top: 1px solid var(--tt-border-light);
}

.trial-row {
    display: flex;
    align-items: center;
    gap: var(--tt-space-3);
    width: 100%;
    padding: var(--tt-space-3) 0;
    border: 0;
    background: none;
    font-family: inherit;
    text-align: left;
    cursor: pointer;
}

.trial-row__icon {
    display: flex;
    align-items: center;
    justify-content: center;
    flex: none;
    width: 36px;
    height: 36px;
    border-radius: var(--tt-radius-full);
}

.trial-row__icon svg {
    width: 20px;
    height: 20px;
}

.trial-row__icon--info {
    background: var(--tt-info-subtle);
    color: var(--tt-info);
}

.trial-row__icon--accent {
    background: var(--tt-accent-subtle);
    color: var(--tt-accent-deep);
}

.trial-row__body {
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-1);
    flex: 1;
    min-width: 0;
}

.trial-row__caption {
    font-size: var(--tt-fs-overline);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-muted);
}

.trial-row__headline {
    display: flex;
    align-items: center;
    gap: var(--tt-space-2);
    min-width: 0;
}

.trial-row__text {
    overflow: hidden;
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-black);
    white-space: nowrap;
    text-overflow: ellipsis;
}

.trial-row__badge {
    flex: none;
    padding: 2px var(--tt-space-2);
    border-radius: var(--tt-radius-xs);
    font-size: var(--tt-fs-overline);
    font-weight: var(--tt-fw-black);
}

.trial-row__badge--info {
    background: var(--tt-info-subtle);
    color: var(--tt-info);
}

.trial-row__badge--danger {
    background: var(--tt-danger-subtle);
    color: var(--tt-danger-deep);
}

.trial-row__chevron {
    flex: none;
    color: var(--tt-text-hint);
    font-size: var(--tt-fs-label);
}
</style>
