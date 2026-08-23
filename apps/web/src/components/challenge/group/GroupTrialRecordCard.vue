<!--
  재판 기록 목록 — 확정된 재판(GUILTY · INNOCENT) 한 줄씩

  `GroupTrialStatusCard` 와 같이 **배열을 받아 목록 전체를 그린다.** 한 장짜리 카드가 아니다 —
  행마다 컴포넌트를 세우면 구분선·라운드가 행 단위로 갈려서 목록으로 안 읽힌다.

  **진행 중 카드와 축이 다르다.** 저쪽(`GroupTrialStatusCard` · `GroupDetailTrialCarousel`)은
  「지금 내가 뭘 해야 하나」라 마감·CTA 가 주인공이다. 여기는 이미 끝난 일이라
  **판결과 개표**가 주인공이고 누를 것이 없다 — 행 전체가 판결문으로 가는 링크일 뿐이다.

  **개표를 가리지 않는다.** 투표 중에 표 수를 감춘 건 편승 투표를 막으려던 것이고(이슈 #171),
  그 근거는 투표가 열려 있는 동안에만 성립한다. 확정된 뒤에 감추면 기록이 기록이 아니게 된다.

  판정·문구·톤·목적지는 전부 `utils/groupTrial.js` 의 `toTrialRecordCard` 가 정해서 준다.
  여기서 `status` 를 다시 보지 않는다 — 그러면 화면마다 유무죄 어휘가 갈린다.
-->
<script setup>
import UserAvatar from '@/components/common/UserAvatar.vue';
import { ChevronRightIcon } from '@heroicons/vue/24/outline';

defineProps({
    /** `toTrialRecordCard` 를 지난 기록 배열. 최근 확정순 정렬은 서버가 끝내 놓았다 */
    items: { type: Array, required: true },
    /**
     * 그룹명을 함께 보여줄지. 전체 기록(지방법원 홈 진입)에서만 켠다 —
     * 그룹 상세에서 켜면 모든 행에 같은 그룹명이 반복될 뿐 행을 가르지 못한다.
     */
    showGroup: { type: Boolean, default: false },
});

const emit = defineEmits(['open']);

/** `1200` → `1,200원`. 초과액이 0 이면 표시하지 않는다(자백·무투표 건에 실제로 0 이 온다). */
function formatAmount(amount) {
    return `${Number(amount ?? 0).toLocaleString('ko-KR')}원`;
}
</script>

<template>
    <section class="trial-record">
        <ul class="trial-record__list">
            <li v-for="item in items" :key="item.id" class="trial-record__row">
                <button
                    type="button"
                    class="trial-record__button"
                    :class="{ 'trial-record__button--mine': item.isMine }"
                    @click="emit('open', { item })"
                >
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
                        <!-- 첫 줄 = 누구의 재판인가. 같은 날 두 명이 확정되면 여기서만 갈린다 -->
                        <span class="trial-record__head">
                            <UserAvatar
                                class="trial-record__avatar"
                                :image-url="item.profileImage"
                                :name="item.nickname"
                                :size="22"
                            />
                            <span class="trial-record__nickname">{{ item.nickname }}</span>
                            <span v-if="item.isMine" class="trial-record__mine">내 재판</span>
                        </span>

                        <!--
                             둘째 줄 = 언제·얼마. 그룹명은 전체 기록에서만 앞에 붙는다.
                             확정 시각(`confirmedLabel`)이 아니라 결산일(`settlementDate`)이
                             먼저다 — 사용자가 기억하는 건 「그날 얼마 썼는가」다.
                        -->
                        <span class="trial-record__meta">
                            <span v-if="showGroup && item.groupName" class="trial-record__group">
                                {{ item.groupName }}
                            </span>
                            <span>{{ item.settlementDate }} 결산</span>
                            <span
                                v-if="item.exceededAmount > 0"
                                class="trial-record__amount"
                                :class="`trial-record__amount--${item.tone}`"
                            >
                                +{{ formatAmount(item.exceededAmount) }}
                            </span>
                        </span>

                        <!--
                             셋째 줄 = 어떻게 그렇게 됐나. 개표가 있으면 숫자를, 없으면(자백·무투표)
                             사유 문구만 남는다. 「0 : 0」을 그리면 아무도 투표 안 한 것처럼 읽힌다.
                        -->
                        <span class="trial-record__tally">
                            <span v-if="item.totalVoters > 0" class="trial-record__votes">
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
                            </span>
                            <span v-if="item.myVote" class="trial-record__myvote">내 표</span>
                            <span class="trial-record__reason">{{ item.reason }}</span>
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
/*
 * 내 재판은 왼쪽에 강조선을 세운다. 배경을 칠하면 행 높이가 달라 보여 목록이 울퉁불퉁해진다.
 * 선은 폭만 먹고 리듬을 건드리지 않는다.
 */
.trial-record__button--mine {
    box-shadow: inset 3px 0 0 -0.5px var(--tt-primary);
    padding-left: 8px;
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
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}
.trial-record__mine {
    flex: none;
    padding: 1px 6px;
    border-radius: var(--tt-radius-full);
    background: var(--tt-primary-subtle);
    font-size: var(--tt-fs-badge);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-primary);
}

.trial-record__meta {
    display: flex;
    align-items: center;
    gap: 6px;
    min-width: 0;
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-medium);
    color: var(--tt-text-muted);
}
/* 전체 기록에서만 뜬다. 가장 먼저 잘려도 되는 값이라 말줄임을 여기 건다 */
.trial-record__group {
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-body);
}
.trial-record__amount {
    flex: none;
    font-family: var(--tt-font-mono);
    font-weight: var(--tt-fw-bold);
}
.trial-record__amount--guilty {
    color: var(--tt-danger);
}
.trial-record__amount--innocent {
    color: var(--tt-text-muted);
}

.trial-record__tally {
    display: flex;
    align-items: center;
    gap: 6px;
    min-width: 0;
    font-size: var(--tt-fs-caption);
}
.trial-record__votes {
    flex: none;
    display: flex;
    align-items: baseline;
    gap: 3px;
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
.trial-record__myvote {
    flex: none;
    padding: 1px 6px;
    border-radius: var(--tt-radius-full);
    background: var(--tt-bg-fill);
    font-size: var(--tt-fs-badge);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-body);
}
/* 사유는 가장 길고 가장 덜 급하다. 남는 폭을 먹고 넘치면 잘린다 */
.trial-record__reason {
    min-width: 0;
    font-weight: var(--tt-fw-medium);
    color: var(--tt-text-hint);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.trial-record__chevron {
    flex: none;
    width: 15px;
    height: 15px;
    color: var(--tt-text-hint);
}
</style>
