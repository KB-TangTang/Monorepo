<!--
  재판 현황 — 지방법원 홈 (이슈 #432 · #443 · #448)

  진행 중인 재판을 아코디언으로 보여준다.
  접히면 **두 줄**(오브젝트 그림 · 할 일 라벨 / 그룹명 · 셰브론),
  펼치면 진행 스테퍼 · 계기판(투표 수 · 남은 시간 · 투표 바) · CTA 다.

  **이제 홈이 아니라 시트가 이 카드를 쓴다**(#448 3차). 홈은 할 일을 격자 두 칸으로 접고
  (`GroupTrialTodoGrid`), 목록은 `GroupTrialListSheet` 가 받는다. 머리줄은 시트 헤더가
  그리므로 여기서는 그리지 않는다 — 두 자리에서 「변론할 재판 2건」이 두 번 뜨면 안 된다.

  **왜 두 줄인가 (#448 2차).** 예전에는 한 줄에 완결된 문장을 넣었다 —
  「{닉}님 재판에 투표해주세요」. 한국어는 술어가 뒤에 오는데 말줄임은 뒤를 자른다.
  360px 에서 제목에 남는 폭이 152px 이라 11자에서 잘렸고, 그 결과
  「…투표해주세요」와 「…투표했어요」가 **화면에서 완전히 같은 줄**이 됐다.
  구분점(내가 뭘 하는가)을 첫 줄로 당기고, 잘려도 되는 맥락을 둘째 줄로 내린다.

  **왜 아바타가 아니라 그림이 왼쪽인가.** 이 목록에서 가장 중요한 건 「내가 뭘 해야 하나」다.
  게다가 내 재판일 때 아바타에는 **내 얼굴**이 떠서 아무것도 알려주지 않았다.
  아바타는 한때 둘째 줄에 남의 재판일 때만 그렸는데, 그것도 걷었다(#448 7차) —
  둘째 줄에 아바타·「지판님 재판」·그룹명 셋이 들어가 좁은 화면에서 **그룹명이 먼저 잘렸다.**
  여러 그룹에 겹쳐 있을 때 행을 가르는 건 그룹명이다. 누구의 재판인지는 CTA 가 여는 화면이 말한다.

  접힌 줄에 **남은 시간은 두지 않는다**(#443). 6행이 매초 같이 떨려 훑기를 방해한다.
  카테고리·한도·초과금액도 두지 않는다 — CTA 가 여는 변론 화면이 거래내역 원본으로 다시 보여준다.
  **투표 점도 두지 않는다**(#448 7차). 몇 명이 던졌는지는 펼친 본문의 투표 바가 이미 말하고,
  접힌 줄에서는 파란 점 무리가 셰브론 옆에서 「눌러야 할 것」처럼 읽혔다.

  판정·문구·아이콘·CTA 는 전부 `utils/groupTrial.js` 의 `toTrialStatusCard` 가 이미 정해서 준다.
  여기서 상태를 다시 보지 않는다 — 그렇게 하면 그룹 상세 캐러셀과 판정이 갈린다.
-->
<script setup>
import { ref } from 'vue';
import { ChevronDownIcon } from '@heroicons/vue/24/outline';
import objDefenseImage from '@/assets/images/judgment/obj_defense.png';
import objVoteImage from '@/assets/images/judgment/obj_vote.png';
import objIndictImage from '@/assets/images/judgment/obj_indict.png';
import { TRIAL_STEPS } from '@/utils/groupTrial';

/*
 * 왼쪽 앵커 그림. 할 일 격자(`GroupTrialTodoGrid`)와 **같은 오브젝트를 같은 키로** 쓴다 —
 * 타일에서 본 판사봉과 시트에서 본 판사봉이 다른 사물이면 「이 타일이 저 목록을 연다」가 안 읽힌다.
 *
 * `STANCE.icon` 은 이름이 넷인데 그림은 셋이라 둘을 합쳤다. 합치는 축은 **재판 단계**다.
 * - `clock`(변론 제출함 / 남의 변론 기다리는 중) → 기소장. 서류만 나오고 변론이 아직 안 끝난 단계다.
 * - `scale`(내 재판이 심판받는 중) → 투표함. `ballot` 과 같은 **투표가 도는 단계**라 같은 사물이 맞다.
 *
 * 합친 만큼 모양이 말하는 구분은 3단계(변론 / 서류 / 투표)로 줄었다. 「내가 하는가 남이 하는가」는
 * 첫 줄 제목이 여전히 6가지로 가른다(`STANCE_LABEL`).
 */
const ART = {
    gavel: objDefenseImage,
    clock: objIndictImage,
    ballot: objVoteImage,
    scale: objVoteImage,
};

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
                    <!-- 왼쪽 앵커 = 재판이 어느 단계인가. 격자 타일과 같은 사물이라 목록이 이어져 읽힌다 -->
                    <img class="trial-status__art" :src="ART[item.icon]" alt="" />

                    <span class="trial-status__body">
                        <!-- 첫째 줄 = 구분점. 라벨이라 4~9자다 — 말줄임에 닿지 않는다 -->
                        <span
                            class="trial-status__title"
                            :class="{ 'trial-status__title--muted': !item.actionable }"
                        >
                            {{ item.title }}
                        </span>
                        <!--
                             둘째 줄 = 어느 그룹인가. 아바타·닉네임(「지판님 재판」)은 뺐다 —
                             한 줄에 세 조각이 들어가 좁은 화면에서 그룹명이 먼저 잘렸는데,
                             **여러 그룹에 겹쳐 있을 때 행을 가르는 건 그룹명**이다.
                             누구의 재판인지는 CTA 가 여는 화면이 원본으로 보여준다.
                        -->
                        <span v-if="item.groupName" class="trial-status__sub-text">
                            {{ item.groupName }}
                        </span>
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

                            <!--
                                 계기판 — 「지금 눌러야 하나」를 판단하는 두 값을 CTA 바로 위에 붙인다.
                                 남은 시간은 한때 패널 맨 위 왼쪽의 회색 캡션이었다. 이 패널에서
                                 **유일하게 급한 값인데 가장 약하게** 그려져 있었다.
                                 투표 수(왼쪽) ↔ 남은 시간(오른쪽)으로 양끝에 건다. 변론 단계라
                                 투표가 없으면 남은 시간만 남는데, `margin-left: auto` 덕에
                                 자리가 그대로 오른쪽이다 — 행마다 시계가 움직이면 안 된다.
                            -->
                            <div class="trial-status__meter">
                                <div class="trial-status__meter-head">
                                    <span v-if="item.showVote" class="trial-status__vote-count">
                                        {{ item.voteCount }} / {{ item.totalVoters }}명 투표
                                    </span>
                                    <span
                                        class="trial-status__countdown"
                                        :class="{
                                            'trial-status__countdown--urgent':
                                                countdownOf(item).urgent,
                                        }"
                                    >
                                        남은 시간 {{ countdownOf(item).text }}
                                    </span>
                                </div>

                                <!-- 투표 바 — 변론이 끝나야 열린다 -->
                                <div v-if="item.showVote" class="trial-status__vote-track">
                                    <div
                                        class="trial-status__vote-fill"
                                        :style="{ width: `${item.votePercent}%` }"
                                    />
                                </div>
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
/*
 * 왼쪽 앵커. 예전에는 38px 톤 틴트 상자 안에 단색 SVG 를 담았는데, 컬러 오브젝트로 바꾸면서
 * 상자를 걷었다 — 그림이 자기 색(판사봉 **노랑·남색**)과 자기 그림자를 갖고 있어
 * 빨강·파랑 틴트 위에 얹으면 색이 부딪히고 그림자가 얼룩이 된다. 격자 타일과 같은 판단이다.
 *
 * 상자를 걷은 만큼 그림을 키운다(19px 글리프 → 34px). 격자 타일(44px)보다는 작다 —
 * 시트는 목록이라 그림이 제목보다 커지면 안 된다.
 *
 * 톤(급함)은 이제 첫 줄 제목의 `--muted` 와 펼친 본문 CTA 색이 맡는다.
 */
.trial-status__art {
    flex: none;
    width: 34px;
    height: 34px;
    object-fit: contain;
    /* 그림 아래 그림자 여백만큼 왼쪽에 빈 공간이 남는다. 광학적으로 글자 줄과 맞춘다 */
    margin-left: -3px;
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
/* 이 줄이 말줄임을 맡는다 — 첫 줄 제목은 라벨이라 4~9자로 짧다 */
.trial-status__sub-text {
    min-width: 0;
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-medium);
    color: var(--tt-text-muted);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
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
 * **서류 상자를 걷었다** (#448 8차). 한때 아이보리 종이(`--tt-doc-bg` + `--tt-doc-rule` 테두리)를
 * 깔아 「펼친다 = 사건 기록을 펼친다」로 읽히려 했는데, 화면에서 그렇게 안 읽혔다.
 *
 * 1. **면이 안 보인다.** `--tt-doc-bg`(#fdfbf3)와 이 카드 바탕(#ffffff)의 차이가
 *    R 2 · G 4 · B 12 다. 실제로 보이는 건 종이가 아니라 `#d8d3c4` 테두리 1px 뿐이었다.
 * 2. **표면이 3겹이었다.** 바텀시트 → 흰 카드(elevation-2) → 이 상자. 겹칠 때마다 쓸 대비를
 *    까먹는데 세 번째에는 남은 게 없다.
 *
 * 소환장(`GroupSummonCard`)에서 종이가 먹히는 건 **화면을 꽉 채우고** 명조·괘선·인장이 같이
 * 있어서다. 여기는 90px 상자에 위젯 셋이라 그 신호를 못 싣는다 — 실으려면 행이 커지는데
 * 목록 6행에서는 그게 더 큰 손해다. 서류 어휘는 **스테퍼의 명조 한 줄만** 남긴다.
 *
 * 상자가 없어졌으니 미완료 색도 종이 괘선(`--tt-doc-rule`)에서 진행바 트랙(`--tt-border-track`)
 * 으로 되돌린다. 흰 바탕 위에서는 회청이 제자리다.
 */
.trial-status__panel-inner {
    /* 여백을 패딩이 아니라 margin 으로 준다 — 패널은 height:0 이 되어야 하므로 패딩을 못 갖는다.
       부모가 overflow:hidden 이라 이 margin 은 밖으로 새지 않고 scrollHeight 에 그대로 잡힌다 */
    margin: 4px 2px 14px;
    display: flex;
    flex-direction: column;
    gap: 13px;
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
/*
 * 4칸을 가로로 나눠 쓰므로 caption 까지 올리면 좁은 화면에서 줄바꿈된다.
 * 「기소 ─ 변론 ─ 투표 ─ 판결」은 재판 절차 그 자체다 — **서류 어휘 중 유일하게 남긴 것**이
 * 이 명조다. 상자는 걷었지만 이 네 글자는 흰 바탕에서도 자기 몫을 한다(위 주석 참고).
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

/* ── 계기판 (투표 수 · 남은 시간 · 투표 바) ─── */
.trial-status__meter {
    display: flex;
    flex-direction: column;
    gap: 7px;
}
/*
 * 두 값을 양끝에 건다. `baseline` 이라 sans(투표 수)와 mono(시계)의 밑선이 맞는다 —
 * `center` 로 두면 두 글꼴의 x-height 차이만큼 어긋나 보인다.
 */
.trial-status__meter-head {
    display: flex;
    align-items: baseline;
    gap: var(--tt-space-2);
}
/* 투표 수와 같은 급으로 맞춘다. 한 줄에 둘이 서니 크기가 다르면 한쪽이 부속처럼 읽힌다 */
.trial-status__vote-count {
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-black);
    color: var(--tt-blue-deep);
}
/*
 * mono 는 같은 크기에서도 폭이 넓어 한 단계 올리면 좁은 화면에서 넘친다 — caption 에 남긴다.
 * `margin-left: auto` 라 왼쪽에 투표 수가 없어도(변론 단계) 자리가 오른쪽 그대로다.
 * 색은 예전의 `--tt-text-muted` 보다 한 단계 올렸다. 이 패널에서 유일하게 급한 값이다.
 */
.trial-status__countdown {
    margin-left: auto;
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-body);
}
.trial-status__countdown--urgent {
    font-weight: var(--tt-fw-black);
    color: var(--tt-red-deep);
}
.trial-status__vote-track {
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
