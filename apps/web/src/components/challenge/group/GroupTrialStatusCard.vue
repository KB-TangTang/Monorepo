<!--
  재판 현황 — 지방법원 홈 (이슈 #432)

  내가 속한 그룹의 **진행 중인 재판 전부**를 아코디언으로 보여준다.
  접히면 **한 줄**(아바타 · 뱃지 · 제목 · 셰브론), 펼치면 남은 시간 · 진행 스테퍼 · 투표 현황 · CTA 다.

  접힌 줄에 정보를 더 얹지 않는다(#443). 재판이 6건씩 뜨면 행마다 카테고리·그룹명·초과금액·타이머가
  같이 붙어 목록 전체가 읽히지 않았다. 한 행이 답하는 질문은 **「누구의 어느 단계 재판이고
  내가 뭘 해야 하나」** 하나뿐이고, 나머지는 펼치거나 CTA 로 들어가야 나온다:
  - 카테고리·한도·초과금액 → CTA 가 여는 변론 화면이 **거래내역 원본으로** 다시 보여준다
  - 남은 시간 → 펼친 본문. 접힌 줄에 두면 6행이 **매초 같이 떨려** 훑기를 방해한다

  판정·뱃지·문구·CTA 는 전부 `utils/groupTrial.js` 의 `toTrialStatusCard` 가 이미 정해서 준다.
  여기서 상태를 다시 보지 않는다 — 그렇게 하면 그룹 상세 캐러셀과 판정이 갈린다.
-->
<script setup>
import { ref } from 'vue';
import { ChevronDownIcon } from '@heroicons/vue/24/outline';
import UserAvatar from '@/components/common/UserAvatar.vue';
import { TRIAL_STEPS } from '@/utils/groupTrial';

const props = defineProps({
    /** `toTrialStatusCard` 를 지난 카드 배열. 정렬은 서버가 끝내 놓았다 */
    items: { type: Array, required: true },
    /** 아이템 id → { text, urgent } 맵 */
    countdowns: { type: Object, required: true },
});

const emit = defineEmits(['open']);

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

/*
 * 아코디언 높이 애니메이션. 패널 높이가 항목마다 다르다 — 투표 바가 조건부(`item.showVote`)라
 * 고정 max-height 로는 넘치면 잘리고 못 미치면 열림이 눈에 띄게 늦다. 실제 높이를 재서 움직인다.
 */
function onEnter(el) {
    el.style.height = '0';
    void el.offsetHeight; /* 강제 리플로우 — 없으면 첫 프레임을 건너뛰고 그냥 나타난다 */
    el.style.height = `${el.scrollHeight}px`;
}

function onAfterEnter(el) {
    el.style.height = 'auto'; /* 열린 뒤 내용이 바뀌어도(투표 수 갱신) 따라간다 */
}

function onLeave(el) {
    el.style.height = `${el.scrollHeight}px`;
    void el.offsetHeight;
    el.style.height = '0';
}
</script>

<template>
    <section class="trial-status">
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
                    <span class="trial-status__badge" :class="`trial-status__badge--${item.tone}`">
                        {{ item.badge }}
                    </span>
                    <span
                        class="trial-status__title"
                        :class="{ 'trial-status__title--muted': !item.actionable }"
                    >
                        {{ item.title }}
                    </span>
                    <ChevronDownIcon class="trial-status__chevron" />
                </button>

                <!--
                     펼친 본문. 바깥(`__panel`)은 높이만 움직이는 껍데기라 패딩을 갖지 않는다 —
                     `scrollHeight` 가 패딩을 포함해서, 한 겹이면 열릴 때 첫 프레임이 튄다.
                -->
                <Transition
                    name="panel"
                    @enter="onEnter"
                    @after-enter="onAfterEnter"
                    @leave="onLeave"
                >
                    <div v-if="isOpen(item, index)" class="trial-status__panel">
                        <div class="trial-status__panel-inner">
                            <!-- 남은 시간. 접힌 줄에 두면 6행이 매초 같이 떨려 훑기가 어렵다 -->
                            <div
                                class="trial-status__countdown"
                                :class="{
                                    'trial-status__countdown--urgent': countdownOf(item).urgent,
                                }"
                            >
                                남은 시간 {{ countdownOf(item).text }}
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

                            <button
                                type="button"
                                class="trial-status__cta"
                                :class="`trial-status__cta--${item.tone}`"
                                @click="emit('open', { item, action: item.action })"
                            >
                                {{ item.cta }}
                            </button>
                        </div>
                    </div>
                </Transition>
            </li>
        </ul>
    </section>
</template>

<style scoped>
/*
 * 선은 두지 않는다. 카드를 띄우는 일은 페이지 배경(--tt-bg-page)이 맡는다.
 * 머리줄이 없어 첫 행이 곧바로 온다 — 그 행이 이미 padding: 11px 을 갖고 있어 위는 얇게 준다.
 *
 * 그림자만은 예외다(#448). 아래 「내 챌린지」 행들이 같은 흰 카드라 두 블록이 평평하게 읽혔다.
 * 이 카드는 마감이 걸린 할 일 큐고 아래는 그룹 명부다 — 할 일 쪽만 바닥에서 떼어 놓는다.
 */
.trial-status {
    background: var(--tt-bg);
    border-radius: var(--tt-radius-xl);
    padding: 6px 15px 4px;
    box-shadow: var(--tt-elevation-2);
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
/* 한 줄이라 제목이 남는 폭을 전부 먹는다. 넘치면 말줄임 */
.trial-status__title {
    flex: 1;
    min-width: 0;
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}
/* 지금 내가 할 일이 없는 행은 한 단계 물러난다. 「할 일」 같은 글자를 더 붙이지 않고 내 차례를 먼저 읽힌다 */
.trial-status__title--muted {
    color: var(--tt-text-muted);
}

/*
 * pill 이 아니라 모서리만 둥근 사각형이다. 카드가 --tt-radius-xl(22px) 이므로
 * 그 안의 태그는 한 단계가 아니라 세 단계 아래인 --tt-radius-xs(8px) — 토큰이 「태그」용으로 정의돼 있다.
 * 높이 20px 에 8px 이면 pill(999px)처럼 뭉개지지 않으면서 카드와 같은 계열로 읽힌다.
 */
.trial-status__badge {
    flex: none;
    font-size: var(--tt-fs-badge);
    font-weight: var(--tt-fw-black);
    padding: 3px 7px;
    border-radius: var(--tt-radius-xs);
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

.trial-status__chevron {
    flex: none;
    width: 16px;
    height: 16px;
    color: var(--tt-text-hint);
    /* 패널과 같은 시간에 끝나야 한 동작으로 읽힌다 */
    transition: transform 0.24s cubic-bezier(0.22, 1, 0.36, 1);
}
.trial-status__row--open .trial-status__chevron {
    transform: rotate(180deg);
}

/* ── 펼친 본문 ──────────────────────── */
/* 바깥은 높이만 움직인다. 패딩·gap 은 안쪽(`__panel-inner`)이 갖는다 — 그래야 scrollHeight 가 맞는다 */
.trial-status__panel {
    overflow: hidden;
}
.trial-status__panel-inner {
    padding: 2px 2px 13px;
    display: flex;
    flex-direction: column;
    gap: 11px;
}

.panel-enter-active,
.panel-leave-active {
    transition:
        height 0.24s cubic-bezier(0.22, 1, 0.36, 1),
        opacity 0.18s ease;
}
.panel-enter-from,
.panel-leave-to {
    opacity: 0;
}

/* mono 는 같은 크기에서도 폭이 넓어 한 단계 올리면 좁은 화면에서 넘친다 — caption 에 남긴다 */
.trial-status__countdown {
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-muted);
}
.trial-status__countdown--urgent {
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
/* 4칸을 가로로 나눠 쓰므로 caption 까지 올리면 좁은 화면에서 줄바꿈된다 */
.trial-status__step-name {
    font-size: var(--tt-fs-badge);
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
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-black);
    color: var(--tt-blue-deep);
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

/* 동작 줄이기를 켠 사용자에게는 여닫기를 즉시 끝낸다 */
@media (prefers-reduced-motion: reduce) {
    .panel-enter-active,
    .panel-leave-active,
    .trial-status__chevron {
        transition: none;
    }
}
</style>
