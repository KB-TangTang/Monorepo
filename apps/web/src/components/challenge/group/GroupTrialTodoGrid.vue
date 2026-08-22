<!--
  할 일 격자 — 지방법원 홈 (이슈 #448, 시안 D)

  내 차례인 재판을 **목록으로 쌓지 않고** 두 칸으로 접는다. 누르면 그 종류만 담긴 시트가 열린다.

  **왜 목록을 접었나.** 이 이슈가 계속 싸운 건 「행 개수」다. 그룹 여섯 개에 들어 있으면
  재판도 여섯 줄이 되고, #448 이 「내 차례」만 남겨 줄였어도 **재판이 늘면 홈이 다시 늘어난다.**
  격자는 건수와 무관하게 높이가 고정이라 그 성질 자체를 없앤다.

  **대신 탭이 하나 늘어난다.** 예전에는 내 차례가 1건일 때 그 한 건이 펼쳐진 채로 떠서
  CTA 가 바로 눌렸다. 이제는 타일 → 시트 → 행 → 화면이다. 알고 받아들인 값이다 —
  홈의 첫 화면이 항상 같은 모양인 것을 더 크게 봤다.

  **왜 「변론 / 투표」인가.** 둘 다 **내가 하는 행동**이라 짝이 맞는다. 「기소」는 나에게
  일어난 일(재판 단계 이름)이라, 「기소 / 투표」로 두면 한쪽은 사건 이름 다른 쪽은 내 행동이 된다.

  0건인 칸도 지운다고 없애지 않는다. 격자의 값이 「항상 같은 모양」이라 한 칸이 사라지면
  남은 칸이 전체 폭으로 늘어나 매번 다른 화면이 된다. 흐리게 두고 못 누르게만 한다.
-->
<script setup>
import { computed } from 'vue';
import TrialActionIcon from './TrialActionIcon.vue';

const props = defineProps({
    defendCount: { type: Number, required: true },
    voteCount: { type: Number, required: true },
    /** 마감 6시간 안쪽이 하나라도 섞여 있는가. 접힌 칸이 마감을 감추지 않게 밖으로 알린다 */
    defendUrgent: { type: Boolean, default: false },
    voteUrgent: { type: Boolean, default: false },
});

const emit = defineEmits(['open']);

/*
 * 아이콘은 `STANCE.icon` 과 같은 이름을 쓴다 — 타일의 망치와 목록 줄의 망치가 같은 그림이라야
 * 「이 타일이 저 목록을 연다」가 읽힌다.
 */
const tiles = computed(() => [
    {
        kind: 'defend',
        icon: 'gavel',
        tone: 'danger',
        label: '변론',
        sub: '내 소비를 해명해요',
        count: props.defendCount,
        urgent: props.defendUrgent,
    },
    {
        kind: 'vote',
        icon: 'ballot',
        tone: 'primary',
        label: '투표',
        sub: '남의 변론을 심판해요',
        count: props.voteCount,
        urgent: props.voteUrgent,
    },
]);
</script>

<template>
    <ul class="todo-grid">
        <li v-for="tile in tiles" :key="tile.kind" class="todo-grid__cell">
            <button
                type="button"
                class="todo-grid__tile"
                :class="[
                    `todo-grid__tile--${tile.tone}`,
                    { 'todo-grid__tile--empty': !tile.count },
                ]"
                :disabled="!tile.count"
                @click="emit('open', tile.kind)"
            >
                <span class="todo-grid__icon">
                    <TrialActionIcon :name="tile.icon" class="todo-grid__glyph" />
                </span>

                <span class="todo-grid__count">
                    {{ tile.count }}<span class="todo-grid__unit">건</span>
                </span>
                <span class="todo-grid__label">{{ tile.label }}</span>
                <!-- 0건일 때는 「내 소비를 해명해요」가 거짓말이 된다 -->
                <span class="todo-grid__sub">{{ tile.count ? tile.sub : '지금은 없어요' }}</span>

                <span v-if="tile.urgent" class="todo-grid__urgent">마감 임박</span>
            </button>
        </li>
    </ul>
</template>

<style scoped>
.todo-grid {
    list-style: none;
    margin: 0;
    padding: 0;
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: var(--tt-space-3);
}

/*
 * 정사각형은 셀이 잡는다. 버튼에 `aspect-ratio` 를 걸면 안쪽 내용이 넘칠 때
 * 비율이 아니라 내용이 이겨서 두 칸 높이가 어긋난다.
 */
.todo-grid__cell {
    aspect-ratio: 1;
}

.todo-grid__tile {
    width: 100%;
    height: 100%;
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    padding: 15px 14px;
    border: none;
    border-radius: var(--tt-radius-xl);
    background: var(--tt-bg);
    /* 재판 카드가 쓰던 그림자를 그대로 물려받는다 — 같은 「할 일」 블록이라 바닥에서 같이 떠야 한다 */
    box-shadow: var(--tt-elevation-2);
    text-align: left;
    cursor: pointer;
    position: relative;
}
.todo-grid__tile:disabled {
    cursor: default;
}

/* 아이콘 박스는 재판 현황 카드의 접힌 줄과 같은 규격(38px · --tt-radius-md) */
.todo-grid__icon {
    width: 38px;
    height: 38px;
    border-radius: var(--tt-radius-md);
    display: flex;
    align-items: center;
    justify-content: center;
    flex: none;
}
.todo-grid__glyph {
    width: 20px;
    height: 20px;
}
.todo-grid__tile--danger .todo-grid__icon {
    background: var(--tt-red-soft);
    color: var(--tt-red-deep);
}
.todo-grid__tile--primary .todo-grid__icon {
    background: var(--tt-blue-soft);
    color: var(--tt-blue-deep);
}

/* 건수를 아래로 밀어 붙인다 — 아이콘은 위, 글자 묶음은 아래. 두 칸의 기준선이 같아진다 */
.todo-grid__count {
    margin-top: auto;
    font-size: var(--tt-fs-display);
    font-weight: var(--tt-fw-black);
    line-height: 1.1;
    color: var(--tt-text);
}
.todo-grid__unit {
    margin-left: 2px;
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-muted);
}
.todo-grid__label {
    font-size: var(--tt-fs-label);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
}
.todo-grid__sub {
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-medium);
    color: var(--tt-text-muted);
}

/*
 * 0건은 지우지 않고 물러나게 한다. `opacity` 로 한 번에 내리면 아이콘 배경까지 흐려져
 * 「고장 난 칸」처럼 보이므로, 색을 각각 무채색으로 바꾼다.
 */
.todo-grid__tile--empty {
    box-shadow: none;
    background: var(--tt-bg-subtle);
}
.todo-grid__tile--empty .todo-grid__icon {
    background: var(--tt-bg-fill);
    color: var(--tt-text-hint);
}
.todo-grid__tile--empty .todo-grid__count,
.todo-grid__tile--empty .todo-grid__label {
    color: var(--tt-text-hint);
}

/* 접힌 칸이 마감을 감추지 않게. 오른쪽 위는 카드에서 비어 있는 유일한 자리다 */
.todo-grid__urgent {
    position: absolute;
    top: 13px;
    right: 12px;
    padding: 3px 7px;
    border-radius: var(--tt-radius-xs);
    background: var(--tt-red-soft);
    font-size: var(--tt-fs-badge);
    font-weight: var(--tt-fw-black);
    color: var(--tt-red-deep);
}
</style>
