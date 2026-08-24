<!--
  재판 기록 목록 — 확정된 재판(GUILTY · INNOCENT) 한 줄씩

  `GroupTrialStatusCard` 와 같이 **배열을 받아 목록 전체를 그린다.** 한 장짜리 카드가 아니다 —
  행마다 컴포넌트를 세우면 구분선·라운드가 행 단위로 갈려서 목록으로 안 읽힌다.

  **진행 중 카드와 축이 다르다.** 저쪽(`GroupTrialStatusCard` · `GroupDetailTrialCarousel`)은
  「지금 내가 뭘 해야 하나」라 마감·CTA 가 주인공이다. 여기는 이미 끝난 일이라
  **판결과 개표**가 주인공이고 누를 것이 없다 — 행 전체가 판결문으로 가는 링크일 뿐이다.

  **개표를 가리지 않는다.** 투표 중에 표 수를 감춘 건 편승 투표를 막으려던 것이고(이슈 #171),
  그 근거는 투표가 열려 있는 동안에만 성립한다. 확정된 뒤에 감추면 기록이 기록이 아니게 된다.

  **행에 담는 건 딱 네 가지 — 판결 · 챌린지 · 누구 · 개표.** 결산일 · 초과액 · 판결 사유는
  판결문에 전부 있고, 목록에서까지 늘어놓으면 세 줄이 다 회색 잔글씨가 돼 판결 도장이 묻힌다.
  목록은 「어느 재판이었는지 알아보고 눌러 들어가는」 자리다.

  판정·문구·톤·목적지는 전부 `utils/groupTrial.js` 의 `toTrialRecordCard` 가 정해서 준다.
  여기서 `status` 를 다시 보지 않는다 — 그러면 화면마다 유무죄 어휘가 갈린다.
-->
<script setup>
import UserAvatar from '@/components/common/UserAvatar.vue';
import { ChevronRightIcon } from '@heroicons/vue/24/outline';

defineProps({
    /** `toTrialRecordCard` 를 지난 기록 배열. 최근 확정순 정렬은 서버가 끝내 놓았다 */
    items: { type: Array, required: true },
});

const emit = defineEmits(['open']);
</script>

<template>
    <section class="trial-record">
        <ul class="trial-record__list">
            <li v-for="item in items" :key="item.id" class="trial-record__row">
                <!--
                     내 재판을 왼쪽 강조선으로 표시하지 않는다. 선이 폭을 먹어 그 행만 도장이
                     안쪽으로 밀리고, 목록을 세로로 훑을 때 유무죄 도장이 한 줄로 안 선다.
                     구분은 배지가 맡는다 — 도장 위치는 모든 행에서 같아야 한다.
                -->
                <button type="button" class="trial-record__button" @click="emit('open', { item })">
                    <!--
                         왼쪽 판결 도장. 캐러셀의 `__stamp` 와 같은 어휘(원형 테두리 · 기울기 · 반투명)를
                         쓰되 색만 유무죄로 가른다. 목록이라 캐러셀(58px)보다 한 단계 작다.
                    -->
                    <span
                        class="trial-record__stamp"
                        :class="`trial-record__stamp--${item.tone}`"
                        aria-hidden="true"
                    >
                        {{ item.verdictLabel }}
                    </span>

                    <span class="trial-record__body">
                        <!--
                             첫 줄 = 어느 챌린지였나. 그룹 기록에서는 모든 행이 같은 값이라
                             중복이지만, 빼면 그 화면만 두 줄이 돼 도장 세로 위치가 전체 기록과
                             달라진다. 행 구조를 두 벌로 만들지 않는다.
                        -->
                        <span class="trial-record__title">{{ item.groupName }}</span>

                        <!-- 둘째 줄 = 누구의 재판인가. 같은 챌린지에서 행을 가르는 유일한 값 -->
                        <span class="trial-record__head">
                            <UserAvatar
                                class="trial-record__avatar"
                                :image-url="item.profileImage"
                                :name="item.nickname"
                                :size="20"
                            />
                            <span class="trial-record__nickname">{{ item.nickname }}</span>
                            <span v-if="item.isMine" class="trial-record__mine">내 재판</span>
                        </span>

                        <!--
                             셋째 줄 = 개표. 자백·무투표 건은 표가 없어 「0 : 0」이 되는데,
                             그걸 그리면 아무도 투표 안 한 게 아니라 전원 기권한 것처럼 읽힌다.
                             자리는 비우지 않는다 — 그 행만 두 줄이 되면 도장이 위로 올라붙는다.
                        -->
                        <span class="trial-record__tally">
                            <template v-if="item.totalVoters > 0">
                                <span class="trial-record__votes-guilty">{{
                                    item.guiltyCount
                                }}</span>
                                <span class="trial-record__votes-sep">:</span>
                                <span class="trial-record__votes-innocent">{{
                                    item.innocentCount
                                }}</span>
                                <span class="trial-record__votes-total"
                                    >/ {{ item.totalVoters }}명</span
                                >
                            </template>
                            <span v-else class="trial-record__novote">투표 없이 확정</span>
                        </span>
                    </span>

                    <ChevronRightIcon class="trial-record__chevron" />
                </button>
            </li>
        </ul>
    </section>
</template>

<style scoped>
.trial-record {
    background: var(--tt-bg);
    border-radius: var(--tt-radius-xl);
    padding: 4px 14px;
    box-shadow: var(--tt-elevation-2);
}

.trial-record__list {
    list-style: none;
    margin: 0;
    padding: 0;
}
.trial-record__row + .trial-record__row {
    border-top: 1px solid var(--tt-border-light);
}

.trial-record__button {
    width: 100%;
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 13px 2px;
    background: none;
    border: none;
    text-align: left;
    cursor: pointer;
}

/* ── 판결 도장 ──────────────────────── */
.trial-record__stamp {
    flex: none;
    width: 46px;
    height: 46px;
    border-radius: 50%;
    border: 2.5px solid currentColor;
    display: flex;
    align-items: center;
    justify-content: center;
    transform: rotate(-11deg);
    opacity: 0.78;
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-black);
    letter-spacing: -0.02em;
}
.trial-record__stamp--guilty {
    color: var(--tt-danger);
}
.trial-record__stamp--innocent {
    color: var(--tt-success);
}

/* ── 본문 세 줄 ─────────────────────── */
.trial-record__body {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 3px;
}

/* 첫 줄이 이 행의 제목이다. 아래 두 줄보다 굵고 진해야 훑을 때 눈이 여기서 멈춘다 */
.trial-record__title {
    min-width: 0;
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.trial-record__head {
    display: flex;
    align-items: center;
    gap: 6px;
    min-width: 0;
}
.trial-record__avatar {
    flex: none;
}
.trial-record__nickname {
    min-width: 0;
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-body);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}
/*
 * 연한 배지(`--tt-primary-subtle` 위 파란 글자)는 옆 닉네임과 명도가 비슷해 그냥 글자로 읽혔다.
 * 목록에서 내 재판을 찾는 게 이 배지의 유일한 일이라 면을 채워 대비를 세운다.
 * `flex: none` 이라 닉네임이 길어져도 이 배지가 먼저 잘리지 않는다.
 */
.trial-record__mine {
    flex: none;
    padding: 2px 7px;
    border-radius: var(--tt-radius-full);
    background: var(--tt-primary);
    font-size: var(--tt-fs-badge);
    font-weight: var(--tt-fw-black);
    line-height: 1.3;
    color: var(--tt-bg);
}

.trial-record__tally {
    display: flex;
    align-items: baseline;
    gap: 3px;
    min-width: 0;
    font-size: var(--tt-fs-caption);
    font-family: var(--tt-font-mono);
    font-weight: var(--tt-fw-bold);
}
.trial-record__votes-guilty {
    color: var(--tt-danger);
}
.trial-record__votes-sep {
    color: var(--tt-text-hint);
}
.trial-record__votes-innocent {
    color: var(--tt-success);
}
.trial-record__votes-total {
    font-size: var(--tt-fs-badge);
    color: var(--tt-text-hint);
}
/* 숫자가 없는 행. 개표 자리를 대신 채워 세 줄 리듬을 지킨다 */
.trial-record__novote {
    font-family: var(--tt-font-sans);
    font-weight: var(--tt-fw-medium);
    color: var(--tt-text-hint);
}

.trial-record__chevron {
    flex: none;
    width: 15px;
    height: 15px;
    color: var(--tt-text-hint);
}
</style>
