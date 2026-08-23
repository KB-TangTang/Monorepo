<!--
  재판 현황 — 지방법원 홈 (이슈 #432 · #443 · #448)

  진행 중인 재판을 아코디언으로 보여준다.
  접히면 **두 줄**(오브젝트 그림 · 챌린지 이름 / 피고 얼굴 · 이름 · 셰브론),
  펼치면 진행 스테퍼 · 계기판(투표 점 · 남은 시간 뱃지) · CTA 다.

  **이제 홈이 아니라 시트가 이 카드를 쓴다**(#448 3차). 홈은 할 일을 격자 두 칸으로 접고
  (`GroupTrialTodoGrid`), 목록은 `GroupTrialListSheet` 가 받는다. 머리줄은 시트 헤더가
  그리므로 여기서는 그리지 않는다 — 두 자리에서 「변론할 재판 2건」이 두 번 뜨면 안 된다.

  **왜 두 줄인가 (#448 2차).** 예전에는 한 줄에 완결된 문장을 넣었다 —
  「{닉}님 재판에 투표해주세요」. 한국어는 술어가 뒤에 오는데 말줄임은 뒤를 자른다.
  360px 에서 제목에 남는 폭이 152px 이라 11자에서 잘렸고, 그 결과
  「…투표해주세요」와 「…투표했어요」가 **화면에서 완전히 같은 줄**이 됐다.
  구분점(내가 뭘 하는가)을 첫 줄로 당기고, 잘려도 되는 맥락을 둘째 줄로 내린다.

  **왜 아바타가 아니라 그림이 왼쪽인가.** 왼쪽 앵커는 재판 단계를 말한다 — 격자 타일과 같은
  사물이라 「이 타일이 저 목록을 연다」가 이어져 읽힌다. 아바타는 그 일을 못 한다.

  **아바타는 둘째 줄에서 크게 쓴다**(#448 10차). 한때 셋(아바타·「지판님 재판」·그룹명)이
  둘째 줄에 들어가 그룹명이 먼저 잘려서 통째로 걷었는데(7차), 그때 **같은 챌린지에서 두 명이
  동시에 기소되면 두 행이 완전히 같아지는** 문제가 생겼다. 첫 줄을 챌린지 이름으로 올리고
  둘째 줄을 얼굴 + 이름만 남겨 셋을 둘로 줄였다. 내 재판이면 내 얼굴이 뜨는데, 아바타의 일이
  「내가 뭘 하나」에서 「누가 피고인가」로 바뀌었으므로 이제 그게 맞는 답이다.

  접힌 줄에 **남은 시간은 두지 않는다**(#443). 6행이 매초 같이 떨려 훑기를 방해한다.
  카테고리·한도·초과금액도 두지 않는다 — CTA 가 여는 변론 화면이 거래내역 원본으로 다시 보여준다.
  **투표 점도 두지 않는다**(#448 7차). 접힌 줄에서는 파란 점 무리가 셰브론 바로 옆에 붙어
  「눌러야 할 것」처럼 읽혔다 — 라벨이 없고 자기도 누를 수 있는 버튼 안이라 그렇다.
  점 자체를 버린 건 아니고 **펼친 본문으로 옮겼다**(#448 9차). 거기서는 「3 / 5명 투표」라는
  라벨을 달고 있고 누를 수 없는 블록이라 같은 오해가 생기지 않는다.

  판정·문구·아이콘·CTA 는 전부 `utils/groupTrial.js` 의 `toTrialStatusCard` 가 이미 정해서 준다.
  여기서 상태를 다시 보지 않는다 — 그렇게 하면 그룹 상세 캐러셀과 판정이 갈린다.
-->
<script setup>
import { ref } from 'vue';
import { ChevronDownIcon, ClockIcon } from '@heroicons/vue/24/outline';
import UserAvatar from '@/components/common/UserAvatar.vue';
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
                        <!--
                             첫째 줄 = 어느 챌린지인가. 한때 「투표하기」 같은 할 일 라벨이었는데
                             걷었다(#448 10차) — **시트 제목이 이미 그 말을 하고 있다.**
                             「투표할 재판」 시트를 열면 모든 행이 투표라서, 라벨이 여섯 번 반복될
                             뿐 행을 가르지 못했다. 챌린지 이름은 행마다 다르다.
                             그룹 목록이 아직 안 왔으면 `groupName` 이 비므로 라벨로 되돌린다.
                        -->
                        <span
                            class="trial-status__title"
                            :class="{ 'trial-status__title--muted': !item.actionable }"
                        >
                            {{ item.groupName || item.title }}
                        </span>
                        <!--
                             둘째 줄 = 누가 기소됐나. 같은 그룹에서 두 명이 동시에 기소되면
                             챌린지 이름만으로는 두 행이 똑같아진다 — 그때 행을 가르는 건 얼굴이다.
                             내 재판이면 내 얼굴이 뜨는데, 여기서는 그게 맞다.
                             아바타의 일이 「내가 뭘 하나」가 아니라 「누가 피고인가」로 바뀌었다.
                        -->
                        <span class="trial-status__sub">
                            <UserAvatar
                                class="trial-status__sub-avatar"
                                :image-url="item.profileImage"
                                :name="item.nickname"
                                :size="26"
                            />
                            <span class="trial-status__sub-text">{{ item.subject }}</span>
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
                                 투표 현황(왼쪽) ↔ 남은 시간(오른쪽)으로 양끝에 건다. 변론 단계라
                                 투표가 없으면 남은 시간만 남는데, `margin-left: auto` 덕에
                                 자리가 그대로 오른쪽이다 — 행마다 시계가 움직이면 안 된다.
                            -->
                            <div class="trial-status__meter">
                                <span v-if="item.showVote" class="trial-status__vote">
                                    <!-- 점은 숫자를 그림으로 옮긴 것뿐이라 읽어 줄 내용이 없다 -->
                                    <span
                                        v-if="item.voteDots"
                                        class="trial-status__dots"
                                        aria-hidden="true"
                                    >
                                        <span
                                            v-for="(dot, dotIndex) in item.voteDots"
                                            :key="dotIndex"
                                            class="trial-status__dot"
                                            :class="`trial-status__dot--${dot}`"
                                        />
                                    </span>
                                    <span class="trial-status__vote-count">
                                        {{ item.voteCount }} / {{ item.totalVoters }}명 투표
                                    </span>
                                </span>

                                <!--
                                     「남은 시간」이라는 라벨은 뗐다(#448 10차). 시계 그림이
                                     그 말을 대신하고, 뱃지 자체가 「이건 재는 값이다」를 말한다.
                                     라벨을 두면 계기판 한 줄에서 글자가 절반을 먹었다.
                                -->
                                <span
                                    class="trial-status__timer"
                                    :class="{
                                        'trial-status__timer--urgent': countdownOf(item).urgent,
                                    }"
                                >
                                    <ClockIcon class="trial-status__timer-icon" />
                                    {{ countdownOf(item).text }}
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
/*
 * 아바타는 **작게 두지 않는다** (#448 10차). 예전에는 「지판님 재판 · 그룹명」 사이에 끼어
 * 17px 짜리 장식이었는데, 지금은 이 행이 누구 것인지 가르는 유일한 조각이다 —
 * 같은 챌린지에서 두 명이 동시에 기소되면 첫 줄이 똑같아지고 얼굴만 다르다.
 * 26px 이라 둘째 줄이 첫 줄보다 높아지지만, 그만큼 행에 무게가 생긴다.
 */
.trial-status__sub {
    display: flex;
    align-items: center;
    gap: 7px;
    min-width: 0;
}
.trial-status__sub-avatar {
    flex: none;
}
/* 말줄임은 이 줄이 맡는다. 익명 텍스트 노드에는 안 걸려서 span 으로 감싼다 */
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

/* ── 계기판 (투표 현황 · 남은 시간) ────── */
/*
 * `baseline` 이라 sans(투표 수)와 mono(시계)의 밑선이 맞는다 —
 * `center` 로 두면 두 글꼴의 x-height 차이만큼 어긋나 보인다.
 */
.trial-status__meter {
    display: flex;
    align-items: baseline;
    gap: var(--tt-space-2);
}
.trial-status__vote {
    display: flex;
    align-items: center;
    gap: 6px;
}
/*
 * **막대가 아니라 점이다** (#448 9차). 한때 폭을 꽉 채운 진행바였는데,
 * `groupInvite.js:52` 가 `maxMembers ?? 6` 이라 그룹은 최대 6명이고 피고를 빼면
 * 배심원은 **최대 5명**이다. 막대는 연속량 인코딩이라 표현할 수 있는 상태가 6개뿐인데
 * 300px 를 쓰고 한 칸이 20% 씩 뛰어서, 「3명이 던졌다」가 아니라 「60% 로딩됨」으로 읽혔다.
 *
 * 점은 이산량이라 인코딩이 맞고, 세로 두 줄이던 계기판이 한 줄로 접혀 패널도 짧아진다.
 * 정원이 6을 넘으면 `buildVoteDots` 가 `null` 을 주고 옆의 숫자만 남는다.
 */
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
/* 점은 `aria-hidden` 이라 읽어 주는 건 이 숫자다. 점만 남기면 스크린리더에 아무것도 안 간다 */
.trial-status__vote-count {
    flex: none;
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-black);
    color: var(--tt-blue-deep);
}
/*
 * 남은 시간은 **뱃지**다 (#448 10차). 글자만 줄줄이 있던 계기판에서 이 값만 면을 갖는다 —
 * 패널에서 유일하게 매초 변하고 유일하게 급한 값이라 다른 어휘를 쓸 자격이 있다.
 * 「남은 시간」 라벨은 뗐다. 시계 그림이 그 말을 하고, 라벨이 한 줄의 절반을 먹고 있었다.
 *
 * mono 는 같은 크기에서도 폭이 넓어 한 단계 올리면 좁은 화면에서 넘친다 — caption 에 남긴다.
 * `margin-left: auto` 라 왼쪽에 투표 현황이 없어도(변론 단계) 자리가 오른쪽 그대로다.
 */
.trial-status__timer {
    margin-left: auto;
    flex: none;
    display: flex;
    align-items: center;
    gap: 4px;
    padding: 4px 9px 4px 7px;
    border-radius: var(--tt-radius-full);
    background: var(--tt-bg-fill);
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-body);
    /* 뱃지가 되면서 줄 높이가 커진다. 옆의 투표 점과 밑선을 맞추려고 baseline 을 끊는다 */
    align-self: center;
}
/* 6시간 안쪽. 면까지 붉어지므로 색맹이어도 「이것만 다르다」가 형태로 남는다 */
.trial-status__timer--urgent {
    background: var(--tt-danger-subtle);
    color: var(--tt-red-deep);
    font-weight: var(--tt-fw-black);
}
.trial-status__timer-icon {
    width: 13px;
    height: 13px;
    stroke-width: 2.2;
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
