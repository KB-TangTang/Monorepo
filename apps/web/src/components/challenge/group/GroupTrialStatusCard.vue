<!--
  재판 현황 — 지방법원 홈 (이슈 #432)

  내가 속한 그룹의 **진행 중인 재판 전부**를 아코디언으로 보여준다.
  접히면 한 줄(뱃지 · 제목 · 카운트다운), 펼치면 진행 스테퍼 · 투표 현황 · CTA 다.

  판정·뱃지·문구·CTA 는 전부 `utils/groupTrial.js` 의 `toTrialStatusCard` 가 이미 정해서 준다.
  여기서 상태를 다시 보지 않는다 — 그렇게 하면 그룹 상세 캐러셀과 판정이 갈린다.
-->
<script setup>
import { computed, ref } from 'vue';
import { ChevronDownIcon } from '@heroicons/vue/24/outline';
import UserAvatar from '@/components/common/UserAvatar.vue';
import CategoryIcon from '@/components/common/CategoryIcon.vue';
import { TRIAL_STEPS } from '@/utils/groupTrial';
import { resolveCategoryIcon } from '@/utils/category';

const props = defineProps({
    /** `toTrialStatusCard` 를 지난 카드 배열. 정렬은 서버가 끝내 놓았다 */
    items: { type: Array, required: true },
    /** 아이템 id → { text, urgent } 맵 */
    countdowns: { type: Object, required: true },
});

const emit = defineEmits(['open']);

const actionableCount = computed(() => props.items.filter((item) => item.actionable).length);

/*
 * 한 번에 하나만 펼친다. 여러 개가 열려 있으면 목록이 화면 몇 개 분량으로 늘어나
 * 「전체를 훑는다」는 이 카드의 목적이 사라진다.
 *
 * `undefined` 는 아직 사용자가 아무것도 누르지 않은 상태다 — 이때는 맨 위(가장 급한 것)를
 * 펼쳐 둔다. `null` 은 사용자가 직접 닫은 상태라 다시 열지 않는다. 둘을 한 값으로 합치면
 * 첫 항목을 닫는 순간 다시 열린다.
 */
const openId = ref(undefined);

function isOpen(item, index) {
    if (openId.value === undefined) return index === 0;
    return openId.value === item.id;
}

function toggle(item, index) {
    openId.value = isOpen(item, index) ? null : item.id;
}

function countdownOf(item) {
    return props.countdowns[item.id] ?? { text: '--:--:--', urgent: false };
}
</script>

<template>
    <section class="trial-status">
        <div class="trial-status__header">
            <div class="trial-status__heading">
                <span v-if="actionableCount" class="trial-status__dot" />
                <span class="trial-status__label">재판 현황 · {{ items.length }}건</span>
            </div>
            <span class="trial-status__sort">마감 임박순</span>
        </div>

        <ul class="trial-status__list">
            <li
                v-for="(item, index) in items"
                :key="item.id"
                class="trial-status__row"
                :class="{ 'trial-status__row--open': isOpen(item, index) }"
            >
                <!-- 접힌 줄 -->
                <button
                    type="button"
                    class="trial-status__summary"
                    :aria-expanded="isOpen(item, index)"
                    @click="toggle(item, index)"
                >
                    <UserAvatar
                        :image-url="item.profileImage"
                        :name="item.nickname"
                        :size="34"
                        class="trial-status__avatar"
                    />
                    <span class="trial-status__text">
                        <span class="trial-status__title">{{ item.title }}</span>
                        <span class="trial-status__sub">
                            <span
                                class="trial-status__badge"
                                :class="`trial-status__badge--${item.tone}`"
                            >
                                {{ item.badge }}
                            </span>
                            <span class="trial-status__sub-text">
                                {{ item.groupName }} · {{ item.settlementDate }} 결산
                            </span>
                        </span>
                    </span>
                    <span
                        class="trial-status__countdown"
                        :class="{
                            'trial-status__countdown--urgent': countdownOf(item).urgent,
                        }"
                    >
                        {{ countdownOf(item).text }}
                    </span>
                    <ChevronDownIcon class="trial-status__chevron" />
                </button>

                <!-- 펼친 본문 -->
                <div v-if="isOpen(item, index)" class="trial-status__panel">
                    <!-- 어느 그룹의 무슨 한도인가 -->
                    <div v-if="item.categoryName" class="trial-status__meta">
                        <CategoryIcon
                            :icon="resolveCategoryIcon(item.categoryName)"
                            class="trial-status__meta-icon"
                        />
                        <span class="trial-status__meta-text">
                            {{ item.categoryName }}
                            <template v-if="item.limitAmount">
                                · 한도 {{ item.limitAmount.toLocaleString() }}원
                            </template>
                        </span>
                        <span class="trial-status__exceeded">
                            +{{ item.exceededAmount.toLocaleString() }}원 초과
                        </span>
                    </div>

                    <!-- 진행 스테퍼 -->
                    <ol class="trial-status__steps">
                        <li
                            v-for="(step, stepIndex) in TRIAL_STEPS"
                            :key="step"
                            class="trial-status__step"
                            :class="{
                                'trial-status__step--done': stepIndex < item.stepIndex,
                                'trial-status__step--now': stepIndex === item.stepIndex,
                            }"
                        >
                            <span class="trial-status__step-dot" />
                            <span class="trial-status__step-name">{{ step }}</span>
                        </li>
                    </ol>

                    <!-- 투표 현황 — 변론이 끝나야 열린다 -->
                    <div v-if="item.showVote" class="trial-status__vote">
                        <div class="trial-status__vote-track">
                            <div
                                class="trial-status__vote-fill"
                                :style="{ width: `${item.votePercent}%` }"
                            />
                        </div>
                        <span class="trial-status__vote-count">
                            {{ item.voteCount }} / {{ item.totalVoters }}명 투표
                        </span>
                    </div>

                    <div class="trial-status__deadline">마감 {{ item.deadlineLabel }}</div>

                    <button
                        type="button"
                        class="trial-status__cta"
                        :class="`trial-status__cta--${item.tone}`"
                        @click="emit('open', { item, action: item.action })"
                    >
                        {{ item.cta }}
                    </button>
                </div>
            </li>
        </ul>
    </section>
</template>

<style scoped>
/* 선도 그림자도 두지 않는다. 카드를 띄우는 일은 페이지 배경(--tt-bg-page)이 맡는다 */
.trial-status {
    background: var(--tt-bg);
    border-radius: var(--tt-radius-xl);
    padding: 14px 15px 4px;
}

/* ── 헤더 ───────────────────────────── */
.trial-status__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 2px 8px;
}
.trial-status__heading {
    display: flex;
    align-items: center;
    gap: 8px;
}
.trial-status__dot {
    width: 10px;
    height: 10px;
    border-radius: 50%;
    background: var(--tt-red);
    flex: none;
    animation: tt-tick 1.6s ease-in-out infinite;
}
.trial-status__label {
    font-size: var(--tt-fs-overline);
    font-weight: var(--tt-fw-black);
    letter-spacing: 0.08em;
    color: var(--tt-red-deep);
}
.trial-status__sort {
    background: var(--tt-bg-fill);
    color: var(--tt-text-hint);
    font-size: var(--tt-fs-overline);
    font-weight: var(--tt-fw-black);
    padding: 4px 9px;
    border-radius: var(--tt-radius-full);
}

/* ── 목록 ───────────────────────────── */
.trial-status__list {
    list-style: none;
    margin: 0;
    padding: 0;
}
.trial-status__row + .trial-status__row {
    border-top: 1px solid var(--tt-border-light);
}

/* ── 접힌 줄 ────────────────────────── */
.trial-status__summary {
    width: 100%;
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 11px 2px;
    background: none;
    border: none;
    text-align: left;
    cursor: pointer;
}
.trial-status__avatar {
    flex: none;
}
.trial-status__text {
    flex: 1;
    min-width: 0;
}
.trial-status__title {
    display: block;
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}
.trial-status__sub {
    display: flex;
    align-items: center;
    gap: 5px;
    margin-top: 3px;
    font-size: var(--tt-fs-overline);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-hint);
}
/*
 * 그룹명을 span 으로 감싼 이유. 이 줄은 flex 라 부모에 text-overflow 를 걸어도 말줄임이 안 붙는다
 * — 익명 flex 아이템이 된 텍스트 노드에는 스타일이 닿지 않아 그냥 잘렸다(실측).
 */
.trial-status__sub-text {
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.trial-status__badge {
    flex: none;
    font-size: var(--tt-fs-overline);
    font-weight: var(--tt-fw-black);
    padding: 2px 7px;
    border-radius: var(--tt-radius-full);
}
.trial-status__badge--danger {
    background: var(--tt-red-soft);
    color: var(--tt-red-deep);
}
.trial-status__badge--primary {
    background: var(--tt-blue-soft);
    color: var(--tt-blue-deep);
}
.trial-status__badge--muted {
    background: var(--tt-bg-fill);
    color: var(--tt-text-muted);
}

.trial-status__countdown {
    flex: none;
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-muted);
}
.trial-status__countdown--urgent {
    color: var(--tt-red-deep);
}

.trial-status__chevron {
    flex: none;
    width: 16px;
    height: 16px;
    color: var(--tt-text-hint);
    transition: transform 0.18s ease;
}
.trial-status__row--open .trial-status__chevron {
    transform: rotate(180deg);
}

/* ── 펼친 본문 ──────────────────────── */
.trial-status__panel {
    padding: 2px 2px 13px;
    display: flex;
    flex-direction: column;
    gap: 11px;
}

.trial-status__meta {
    display: flex;
    align-items: center;
    gap: 7px;
    background: var(--tt-bg-subtle);
    border-radius: var(--tt-radius-sm);
    padding: 9px 11px;
}
.trial-status__meta-icon {
    flex: none;
    width: 17px;
    height: 17px;
    color: var(--tt-text-muted);
}
.trial-status__meta-text {
    flex: 1;
    min-width: 0;
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-body);
}
.trial-status__exceeded {
    flex: none;
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-black);
    color: var(--tt-red-deep);
}

/* ── 진행 스테퍼 ────────────────────── */
.trial-status__steps {
    list-style: none;
    margin: 0;
    padding: 0;
    display: flex;
}
.trial-status__step {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 5px;
    position: relative;
}
/* 칸 사이를 잇는 선. 마지막 칸 뒤에는 그리지 않는다 */
.trial-status__step:not(:last-child)::after {
    content: '';
    position: absolute;
    top: 4px;
    left: 50%;
    width: 100%;
    height: 2px;
    background: var(--tt-border-track);
}
.trial-status__step--done:not(:last-child)::after {
    background: var(--tt-blue);
}
.trial-status__step-dot {
    position: relative;
    z-index: 1;
    width: 10px;
    height: 10px;
    border-radius: 50%;
    background: var(--tt-border-track);
}
.trial-status__step--done .trial-status__step-dot {
    background: var(--tt-blue);
}
.trial-status__step--now .trial-status__step-dot {
    background: var(--tt-red);
    box-shadow: 0 0 0 4px var(--tt-red-soft);
}
.trial-status__step-name {
    font-size: var(--tt-fs-overline);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-hint);
}
.trial-status__step--done .trial-status__step-name {
    color: var(--tt-text-muted);
}
.trial-status__step--now .trial-status__step-name {
    color: var(--tt-red-deep);
    font-weight: var(--tt-fw-black);
}

/* ── 투표 현황 ──────────────────────── */
.trial-status__vote {
    display: flex;
    align-items: center;
    gap: 9px;
}
.trial-status__vote-track {
    flex: 1;
    height: 6px;
    border-radius: var(--tt-radius-full);
    background: var(--tt-border-track);
    overflow: hidden;
}
.trial-status__vote-fill {
    height: 100%;
    border-radius: var(--tt-radius-full);
    background: var(--tt-blue);
    transition: width 0.25s ease;
}
.trial-status__vote-count {
    flex: none;
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-black);
    color: var(--tt-blue-deep);
}

.trial-status__deadline {
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-hint);
}

/* ── CTA ────────────────────────────── */
.trial-status__cta {
    width: 100%;
    padding: 12px;
    border: none;
    border-radius: var(--tt-radius-md);
    font-size: var(--tt-fs-button);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text-inverse);
    cursor: pointer;
}
.trial-status__cta--danger {
    background: var(--tt-red-deep);
}
.trial-status__cta--primary {
    background: var(--tt-blue);
}
/* 할 일이 없는 카드의 CTA 는 「보기」다. 꽉 찬 버튼으로 그리면 급한 것과 구분되지 않는다 */
.trial-status__cta--muted {
    background: transparent;
    border: 1.5px solid var(--tt-border);
    color: var(--tt-text-body);
}

@keyframes tt-tick {
    0%,
    100% {
        opacity: 1;
    }
    50% {
        opacity: 0.45;
    }
}
</style>
