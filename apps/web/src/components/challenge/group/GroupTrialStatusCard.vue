<!--
  재판 현황 — 지방법원 홈 (이슈 #432 · #443 · #448)

  진행 중인 재판을 아코디언으로 보여준다.
  접히면 **두 줄**(행동 아이콘 · 할 일 라벨 / 누구·어느 그룹 · 투표 점 · 셰브론),
  펼치면 남은 시간 · 진행 스테퍼 · 투표 현황 · CTA 다.

  **왜 두 줄인가 (#448 2차).** 예전에는 한 줄에 완결된 문장을 넣었다 —
  「{닉}님 재판에 투표해주세요」. 한국어는 술어가 뒤에 오는데 말줄임은 뒤를 자른다.
  360px 에서 제목에 남는 폭이 152px 이라 11자에서 잘렸고, 그 결과
  「…투표해주세요」와 「…투표했어요」가 **화면에서 완전히 같은 줄**이 됐다.
  구분점(내가 뭘 하는가)을 첫 줄로 당기고, 잘려도 되는 맥락을 둘째 줄로 내린다.

  **왜 아바타가 아니라 아이콘이 왼쪽인가.** 이 목록에서 가장 중요한 건 「내가 뭘 해야 하나」다.
  게다가 내 재판일 때 아바타에는 **내 얼굴**이 떠서 아무것도 알려주지 않았다.
  아바타는 둘째 줄로 내리고, 남의 재판일 때만 그린다.

  접힌 줄에 **남은 시간은 두지 않는다**(#443). 6행이 매초 같이 떨려 훑기를 방해한다.
  카테고리·한도·초과금액도 두지 않는다 — CTA 가 여는 변론 화면이 거래내역 원본으로 다시 보여준다.

  판정·문구·아이콘·CTA 는 전부 `utils/groupTrial.js` 의 `toTrialStatusCard` 가 이미 정해서 준다.
  여기서 상태를 다시 보지 않는다 — 그렇게 하면 그룹 상세 캐러셀과 판정이 갈린다.
-->
<script setup>
import { ref } from 'vue';
import { ChevronDownIcon } from '@heroicons/vue/24/outline';
import { ClockIcon, ScaleIcon } from '@heroicons/vue/24/solid';
import UserAvatar from '@/components/common/UserAvatar.vue';
import { TRIAL_STEPS } from '@/utils/groupTrial';

const props = defineProps({
    /** `toTrialStatusCard` 를 지난 카드 배열. 정렬은 서버가 끝내 놓았다 */
    items: { type: Array, required: true },
    /** 아이템 id → { text, urgent } 맵 */
    countdowns: { type: Object, required: true },
    /*
     * 머리줄 앞말. 비면 머리줄 자체를 안 그린다 — 바텀시트는 시트 헤더가 이미 같은 말을 한다.
     * 홈에서는 필요하다: 「재판 현황」이 다크 헤더 곡면에 있어 스크롤하면 사라지고,
     * 흰 카드와 시각적으로 끊겨 있어 이 목록이 무엇인지 카드 안에서는 알 수 없었다.
     */
    heading: { type: String, default: '' },
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
        <!--
             무슨 목록인지 카드 안에서 말한다. 「재판 현황」은 다크 헤더 곡면에 있어
             스크롤하면 사라지고, 흰 카드와 시각적으로 끊겨 있어 이어 읽히지 않았다.
        -->
        <header v-if="heading" class="trial-status__head">
            <span class="trial-status__head-title">{{ heading }} {{ items.length }}건</span>
            <span class="trial-status__head-sort">마감 임박순</span>
        </header>

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
                    <!-- 왼쪽 앵커 = 할 일의 종류. 모양이 달라 색을 못 봐도 갈린다 -->
                    <span class="trial-status__icon" :class="`trial-status__icon--${item.tone}`">
                        <svg
                            v-if="item.icon === 'gavel'"
                            class="trial-status__glyph"
                            viewBox="0 0 24 24"
                            fill="currentColor"
                            aria-hidden="true"
                        >
                            <path
                                d="M1 21h12v2H1zM5.245 8.07l2.83-2.827 14.14 14.14-2.828 2.83zM12.317 1l5.657 5.657-2.83 2.83-5.654-5.66zM3.825 9.485l5.657 5.657-2.828 2.828-5.657-5.657z"
                            />
                        </svg>
                        <svg
                            v-else-if="item.icon === 'ballot'"
                            class="trial-status__glyph"
                            viewBox="0 0 24 24"
                            fill="currentColor"
                            aria-hidden="true"
                        >
                            <path
                                d="M18 13h-.68l-2 2h1.91L19 17H5l1.78-2h2.05l-2-2H6l-3 3v4c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2v-4l-3-3zm-1-5.05l-4.95 4.95-3.54-3.54 4.95-4.95 3.54 3.54zm-4.24-5.66L6.39 8.66c-.39.39-.39 1.02 0 1.41l4.95 4.95c.39.39 1.02.39 1.41 0l6.36-6.36c.39-.39.39-1.02 0-1.41L14.16 2.3c-.38-.4-1.01-.4-1.4-.01z"
                            />
                        </svg>
                        <ScaleIcon v-else-if="item.icon === 'scale'" class="trial-status__glyph" />
                        <ClockIcon v-else class="trial-status__glyph" />
                    </span>

                    <span class="trial-status__body">
                        <!-- 첫째 줄 = 구분점. 라벨이라 4~9자다 — 말줄임에 닿지 않는다 -->
                        <span
                            class="trial-status__title"
                            :class="{ 'trial-status__title--muted': !item.actionable }"
                        >
                            {{ item.title }}
                        </span>
                        <!-- 둘째 줄 = 맥락. 잘려도 되는 자리라 여기가 말줄임을 맡는다 -->
                        <span class="trial-status__sub">
                            <UserAvatar
                                v-if="!item.isMine"
                                :image-url="item.profileImage"
                                :name="item.nickname"
                                :size="16"
                                class="trial-status__sub-avatar"
                            />
                            <span class="trial-status__sub-text">
                                {{ item.subject
                                }}<template v-if="item.groupName"> · {{ item.groupName }}</template>
                            </span>
                        </span>
                    </span>

                    <!--
                         투표 현황을 **모양**으로. 맨 앞 칸이 내 표라 「나는 던졌는데 남들이 아직」과
                         「나도 아직」이 색 없이도 갈린다. 정원이 많으면 점이 줄을 밀어내므로 숫자로 떨어뜨린다.
                    -->
                    <span v-if="item.voteDots" class="trial-status__dots" aria-hidden="true">
                        <span
                            v-for="(dot, dotIndex) in item.voteDots"
                            :key="dotIndex"
                            class="trial-status__dot"
                            :class="`trial-status__dot--${dot}`"
                        />
                    </span>
                    <span v-else-if="item.showVote" class="trial-status__votenum">
                        {{ item.voteCount }}/{{ item.totalVoters }}
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

/* ── 머리줄 ─────────────────────────── */
.trial-status__head {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
    padding: 6px 2px 7px;
}
.trial-status__head-title {
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-black);
    letter-spacing: 0.02em;
    color: var(--tt-text-muted);
}
/* 정렬 기준을 밝힌다. 「왜 이 순서인가」를 묻지 않게 하는 한 줄이라 앞말보다 더 물러난다 */
.trial-status__head-sort {
    font-size: var(--tt-fs-badge);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-hint);
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
/*
 * 아이콘 박스. 글자 뱃지를 대신한다 — 낱말 두 개(「변론 중」·「투표 중」)로 6가지 입장을 눌러 담고
 * 색으로만 갈랐더니, 「내가 변론을 냈다」와 「남이 변론을 쓰는 중」이 회색 「변론 중」 둘로 같아졌다.
 * 색은 급함만 말하고, 종류는 모양이 말한다.
 *
 * 38px 에 --tt-radius-md(14px). pill 로 만들면 원이 돼 아래 아바타(16px 원)와 계열이 섞인다.
 */
.trial-status__icon {
    flex: none;
    width: 38px;
    height: 38px;
    border-radius: var(--tt-radius-md);
    display: flex;
    align-items: center;
    justify-content: center;
}
.trial-status__glyph {
    width: 19px;
    height: 19px;
}
.trial-status__icon--danger {
    background: var(--tt-red-soft);
    color: var(--tt-red-deep);
}
.trial-status__icon--primary {
    background: var(--tt-blue-soft);
    color: var(--tt-blue-deep);
}
.trial-status__icon--muted {
    background: var(--tt-bg-fill);
    color: var(--tt-text-muted);
}

/* 두 줄이 남는 폭을 전부 먹는다. min-width:0 이 없으면 flex 자식이 안 줄어들어 말줄임이 안 걸린다 */
.trial-status__body {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 2px;
}
.trial-status__title {
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
.trial-status__sub {
    display: flex;
    align-items: center;
    gap: 5px;
    min-width: 0;
}
.trial-status__sub-avatar {
    flex: none;
}
/*
 * 말줄임은 요소에만 걸린다 — flex 안의 익명 텍스트 노드에는 안 붙는다.
 * 그래서 글자를 span 으로 감싸고 여기에 min-width:0 을 준다.
 */
.trial-status__sub-text {
    min-width: 0;
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-medium);
    color: var(--tt-text-muted);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

/* ── 투표 점 ────────────────────────── */
.trial-status__dots {
    flex: none;
    display: flex;
    align-items: center;
    gap: 4px;
}
.trial-status__dot {
    width: 7px;
    height: 7px;
    border-radius: 50%;
    background: var(--tt-border-track);
    box-sizing: border-box;
}
.trial-status__dot--done {
    background: var(--tt-blue);
}
/* 내 칸은 맨 앞이고 테를 두른다. 색이 아니라 **테**라서 색을 못 보는 사람도 어디가 내 표인지 안다 */
.trial-status__dot--mine-done {
    background: var(--tt-blue);
    box-shadow: 0 0 0 2px var(--tt-blue-soft);
}
.trial-status__dot--mine-todo {
    background: transparent;
    border: 2px solid var(--tt-blue);
}
/* 정원이 많아 점을 못 쓸 때. 화면에서 거의 안 나오지만 나오면 여기로 떨어진다 */
.trial-status__votenum {
    flex: none;
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-badge);
    font-weight: var(--tt-fw-bold);
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
/*
 * 펼친 본문은 **사건 기록**이다 (#448). 접힌 줄 6행은 훑는 면이라 흰 바탕을 그대로 두고,
 * 열었을 때만 서류가 나온다 — 「펼친다 = 기록을 펼친다」로 아코디언 자체에 의미가 붙는다.
 * `openId` 가 단일이라 종이는 한 번에 하나만 나오고, 그래서 목록이 시끄러워지지 않는다.
 *
 * 어휘는 새로 만들지 않았다. `--tt-doc-*` 은 소환장(`GroupSummonCard`)이 쓰던 것을 그대로 쓴다.
 * 다만 소환장의 기울임·낙하·인장은 **한 번 보는 히어로**용이라 여기엔 가져오지 않는다.
 */
.trial-status__panel-inner {
    /* 아래 여백을 패딩이 아니라 margin 으로 준다 — 패널은 height:0 이 되어야 하므로 패딩을 못 갖는다.
       부모가 overflow:hidden 이라 이 margin 은 밖으로 새지 않고 scrollHeight 에 그대로 잡힌다 */
    margin: 0 2px 13px;
    background: var(--tt-doc-bg);
    border: 1px solid var(--tt-doc-rule);
    border-radius: var(--tt-radius-sm);
    padding: 12px 13px 13px;
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
/*
 * 칸 사이를 잇는 선. 마지막 칸 뒤에는 그리지 않는다.
 * 미완료 색은 진행바 트랙(`--tt-border-track` #e9ecf2, 회청)이 아니라 종이 괘선(`--tt-doc-rule`)이다 —
 * 아이보리 바탕 위에 회청 회색이 얹히면 그 조각만 서류 밖에서 온 것처럼 뜬다.
 */
.trial-status__step:not(:last-child)::after {
    content: '';
    position: absolute;
    top: 4px;
    left: 50%;
    width: 100%;
    height: 2px;
    background: var(--tt-doc-rule);
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
    background: var(--tt-doc-rule);
}
.trial-status__step--done .trial-status__step-dot {
    background: var(--tt-blue);
}
.trial-status__step--now .trial-status__step-dot {
    background: var(--tt-red);
    box-shadow: 0 0 0 4px var(--tt-red-soft);
}
/*
 * 4칸을 가로로 나눠 쓰므로 caption 까지 올리면 좁은 화면에서 줄바꿈된다.
 * 「기소 ─ 변론 ─ 투표 ─ 판결」은 재판 절차 그 자체다 — 서류 안이니 명조로 쓴다.
 * `--tt-font-serif`(나눔명조)는 `index.html:69` 에서 이미 전역으로 불러오고 있다.
 */
.trial-status__step-name {
    font-family: var(--tt-font-serif);
    font-size: var(--tt-fs-badge);
    font-weight: var(--tt-fw-bold);
    letter-spacing: 0.06em;
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
    /* 스테퍼와 같은 이유로 종이 괘선 톤 — 같은 서류 안에서 미완료 색이 둘이면 안 된다 */
    background: var(--tt-doc-rule);
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
