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
import objDefenseImage from '@/assets/images/judgment/obj_defense.png';
import objVoteImage from '@/assets/images/judgment/obj_vote.png';
import wmDefenseImage from '@/assets/images/judgment/wm_sil_defense.png';
import wmVoteImage from '@/assets/images/judgment/wm_sil_vote.png';

/*
 * 그림은 `STANCE.icon` 이름으로 찾는다. 목록 줄(`GroupTrialStatusCard`)은 작아서 단색 SVG
 * (`TrialActionIcon`)를, 타일은 커서 컬러 오브젝트를 쓴다 — **그림체는 달라도 사물은 같아야**
 * 「이 타일이 저 목록을 연다」가 읽힌다. 같은 키를 쓰면 한쪽만 엉뚱한 사물로 갈아 끼울 수 없다.
 */
const ART = { gavel: objDefenseImage, ballot: objVoteImage };
const WATERMARK = { gavel: wmDefenseImage, ballot: wmVoteImage };

const props = defineProps({
    defendCount: { type: Number, required: true },
    voteCount: { type: Number, required: true },
    /** 마감 6시간 안쪽이 하나라도 섞여 있는가. 접힌 칸이 마감을 감추지 않게 밖으로 알린다 */
    defendUrgent: { type: Boolean, default: false },
    voteUrgent: { type: Boolean, default: false },
});

const emit = defineEmits(['open']);

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
                <!--
                  같은 사물의 실루엣을 크게 깔아 빈 면을 채운다. 글자를 늘리지 않고 두 칸을
                  모양으로 가르는 게 목적이라 `alt` 는 빈 문자열이다 — 읽어 줄 내용이 없다.
                  DOM 맨 앞에 둬야 뒤 형제들이 위에 그려진다.
                -->
                <img class="todo-grid__watermark" :src="WATERMARK[tile.icon]" alt="" />

                <img class="todo-grid__art" :src="ART[tile.icon]" alt="" />

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
    /* 워터마크가 모서리에서 잘려 나가야 「면을 채운 것」으로 보인다 */
    overflow: hidden;
    transition: transform 0.14s cubic-bezier(0.22, 1, 0.36, 1);
}
/* 누르면 들어간다 — 격자가 목록을 여는 「버튼」이라는 걸 손끝으로 알린다 */
.todo-grid__tile:active {
    transform: scale(0.97);
}
.todo-grid__tile:disabled {
    cursor: default;
}
.todo-grid__tile:disabled:active {
    transform: none;
}

/*
 * 오브젝트는 틴트 상자에 담지 않는다. 그림 자체가 컬러(판사봉 노랑·남색 / 투표함 남색·크림)이고
 * 아래에 자기 그림자를 갖고 있어, 빨강·파랑 틴트 위에 얹으면 색이 부딪히고 그림자가 얼룩이 된다.
 * 상자를 걷은 만큼 그림을 크게 쓴다 — 톤 구분은 건수 숫자 색이 맡는다.
 */
.todo-grid__art {
    width: 44px;
    height: 44px;
    object-fit: contain;
    flex: none;
    /* 그림 아래 그림자 여백만큼 왼쪽/위 여백이 남는다. 광학적으로 글자 줄과 맞춘다 */
    margin: -4px 0 0 -4px;
}

/*
 * 정사각형은 내용보다 넓어서 오른쪽 아래가 통째로 빈다. 글자를 더 넣으면 말줄임이 되살아나므로
 * 같은 사물의 실루엣을 크게 깔아 면만 채운다.
 *
 * **음수 오프셋이 큰 이유** — PNG 는 480px 캔버스인데 그림이 왼쪽 위 약 80% 에 몰려 있다.
 * 나머지는 투명 여백이라, 이만큼 밀어야 그림이 실제로 모서리에 닿는다.
 */
.todo-grid__watermark {
    position: absolute;
    right: -20px;
    bottom: -22px;
    width: 116px;
    height: 116px;
    z-index: 0;
    pointer-events: none;
}
/* 워터마크는 z-index 0 이라, 내용은 명시적으로 그 위로 올린다 */
.todo-grid__art,
.todo-grid__count,
.todo-grid__label,
.todo-grid__sub {
    position: relative;
    z-index: 1;
}

/* 건수를 아래로 밀어 붙인다 — 그림은 위, 글자 묶음은 아래. 두 칸의 기준선이 같아진다 */
.todo-grid__count {
    margin-top: auto;
    font-size: var(--tt-fs-display);
    font-weight: var(--tt-fw-black);
    line-height: 1.1;
}
/*
 * 틴트 상자를 걷은 뒤로 **톤(급함 = 빨강 / 남의 일 = 파랑)을 말하는 곳은 여기 하나뿐이다.**
 * 오브젝트 그림의 색(노랑·남색)은 사물 고유색이지 급함을 뜻하지 않는다.
 */
.todo-grid__tile--danger .todo-grid__count {
    color: var(--tt-danger-deep);
}
.todo-grid__tile--primary .todo-grid__count {
    color: var(--tt-info-deep);
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
 * 0건은 지우지 않고 물러나게 한다. 글자는 색을 각각 무채색으로 바꾸지만, 그림은 래스터라
 * 색을 못 바꾼다 — `grayscale` 로 톤만 빼고 옅게 내린다. 타일 전체에 `opacity` 를 걸면
 * 그림자까지 흐려져 「고장 난 칸」이 된다.
 */
.todo-grid__tile--empty {
    box-shadow: none;
    background: var(--tt-bg-subtle);
}
.todo-grid__tile--empty .todo-grid__art {
    filter: grayscale(1);
    opacity: 0.4;
}
.todo-grid__tile--empty .todo-grid__watermark {
    opacity: 0.5;
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
    background: var(--tt-danger-subtle);
    font-size: var(--tt-fs-badge);
    font-weight: var(--tt-fw-black);
    color: var(--tt-danger-deep);
}

/* 동작 줄이기를 켠 사용자에게는 눌림 효과를 끈다 (src/ 안 27개 파일이 같은 처리를 한다) */
@media (prefers-reduced-motion: reduce) {
    .todo-grid__tile {
        transition: none;
    }
    .todo-grid__tile:active {
        transform: none;
    }
}
</style>
