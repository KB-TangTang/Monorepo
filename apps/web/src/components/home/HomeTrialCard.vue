<!--
  용도: 홈의 「진행 중인 재판」 카드. 대법원(개인 챌린지) · 지방법원(그룹 챌린지) 두 줄로
        지금 상태를 요약하고 각각 해당 화면으로 보낸다. 하단은 재판 기록 입구.
  아이콘은 재판탭 법원 헤더와 같은 건물 일러스트를 쓴다 — 같은 곳으로 가는 입구는 같게 생겨야 한다.
-->
<script setup>
import { computed } from 'vue';
import BaseCard from '@/components/common/BaseCard.vue';
import gavelImage from '@/assets/images/challenge_live/gavel-alive.png';
import supremeImage from '@/assets/images/court/building_supreme_v2.png';
import districtImage from '@/assets/images/court/building_district_v2.png';

const props = defineProps({
    /* { text: 표시 문구, badge: 우측 뱃지 문구 | null } — 아직 불러오는 중이면 null */
    personal: { type: Object, default: null },
    group: { type: Object, default: null },
});

const emit = defineEmits(['open-personal', 'open-group', 'open-records']);

const rows = computed(() => [
    {
        key: 'personal',
        caption: '대법원 · 개인챌린지',
        image: supremeImage,
        tone: 'info',
        text: props.personal?.text ?? '불러오는 중이에요',
        badge: props.personal?.badge ?? null,
        badgeTone: 'info',
        event: 'open-personal',
    },
    {
        key: 'group',
        caption: '지방법원 · 그룹챌린지',
        image: districtImage,
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
        <p class="trial__label">진행 중인 재판</p>

        <h2 class="trial__headline">오늘 재판도 무죄로<br />마무리해 보세요!</h2>

        <img class="trial__gavel" :src="gavelImage" alt="" />

        <ul class="trial__rows">
            <li v-for="row in rows" :key="row.key">
                <button type="button" class="trial-row" @click="emit(row.event)">
                    <span class="trial-row__icon" :class="`trial-row__icon--${row.tone}`">
                        <img :src="row.image" alt="" />
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

        <button type="button" class="trial__records" @click="emit('open-records')">
            재판 기록 보기 ›
        </button>
    </BaseCard>
</template>

<style scoped>
.trial {
    position: relative;
    padding: var(--tt-space-4) var(--tt-space-4) 0;
    border-radius: var(--tt-radius-xl);
}

.trial__label {
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text-muted);
}

.trial__headline {
    margin-top: var(--tt-space-1);
    font-size: var(--tt-fs-subtitle);
    font-weight: var(--tt-fw-black);
    line-height: var(--tt-lh-snug);
    letter-spacing: -0.01em;
}

.trial__gavel {
    position: absolute;
    top: var(--tt-space-4);
    right: var(--tt-space-4);
    width: 54px;
    height: auto;
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
    border-radius: var(--tt-radius-sm);
    overflow: hidden;
}

.trial-row__icon img {
    width: 26px;
    height: auto;
}

.trial-row__icon--info {
    background: var(--tt-info-subtle);
}

.trial-row__icon--accent {
    background: var(--tt-accent-subtle);
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

.trial__records {
    width: 100%;
    padding: var(--tt-space-3) 0;
    border: 0;
    border-top: 1px solid var(--tt-border);
    background: none;
    color: var(--tt-text-body);
    font-family: inherit;
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-black);
    cursor: pointer;
}
</style>
