<!--
  그룹 챌린지 시작 배치 수동 실행 버튼 (개발 전용, 이슈 #152).

  상태 전이는 매일 00:01 배치가 한다. 시연·검증 때 자정을 기다릴 수 없어 화면에 뺐다.
  개인 미션 홈의 재배정 버튼(PersonalMissionHomeView)과 같은 방식이다.

  ⚠ **「이 챌린지 시작하기」 버튼이 아니다.** 배치를 그대로 돌리므로 조건을 만족한 그룹만 바뀐다.
     오늘 시작 + 2명 이상 → ACTIVE / 오늘 시작 + 1명 → CLOSED / 시작일이 미래 → 그대로.

  짧게 누르면 오늘 기준, **길게 누르면 기준일을 직접 넣는다.** 길게 누르기가 필요한 이유는
  로컬 테스트 데이터의 시작일이 대부분 미래이기 때문이다 — 그룹 생성 검증이 과거 시작일을
  막아서(`GROUP_START_DATE_INVALID`) "오늘 시작" 인 데이터를 만들려면 매번 새로 만들어야 한다.
  기준일을 넣으면 이미 있는 그룹으로 바로 확인할 수 있다.

  결과는 버튼 라벨에 잠깐 띄운다. 두 화면(홈·전체보기)에 붙는데 한쪽에는 토스트가 없어서,
  부모마다 토스트를 심는 대신 스스로 표시한다. 목록 갱신은 `done` 이벤트로 부모에 맡긴다.
-->
<script setup>
import { ref, computed, onBeforeUnmount } from 'vue';
import { BoltIcon } from '@heroicons/vue/24/outline';
import { runGroupChallengeStatusBatch } from '@/api/groupChallenge';

const props = defineProps({
    /** 탭바 위쪽으로 띄울 거리(px). 다른 DEV 버튼과 겹칠 때 올린다. */
    offset: { type: Number, default: 96 },
});

const emit = defineEmits(['done']);

const isDev = import.meta.env.DEV;
const running = ref(false);
const result = ref('');

/** 길게 누르기 판정 시간. 스크롤 제스처와 헷갈리지 않을 만큼은 길어야 한다. */
const LONG_PRESS_MS = 600;

let pressTimer = null;
let longPressed = false;
let resultTimer = null;

const label = computed(() => {
    if (running.value) return '실행 중…';
    if (result.value) return result.value;
    return '챌린지 시작 배치';
});

function showResult(message) {
    clearTimeout(resultTimer);
    result.value = message;
    resultTimer = setTimeout(() => { result.value = ''; }, 2400);
}

function todayIso() {
    const d = new Date();
    const mm = String(d.getMonth() + 1).padStart(2, '0');
    const dd = String(d.getDate()).padStart(2, '0');
    return `${d.getFullYear()}-${mm}-${dd}`;
}

async function run(date) {
    if (running.value) return;
    running.value = true;
    try {
        const res = await runGroupChallengeStatusBatch(date);
        showResult(res.affected > 0 ? `${res.affected}건 전이됨` : '대상 없음');
        /* 0건이어도 갱신한다 — 다른 창에서 바뀌었을 수 있다 */
        emit('done', res);
    } catch (err) {
        showResult(err.message ?? '실행 실패');
    } finally {
        running.value = false;
    }
}

function askDateAndRun() {
    /* 날짜 입력은 DEV 도구라 prompt 로 충분하다. 이걸 위해 모달을 만들면
     * 프로덕션 번들에 들어갈지 신경 써야 하는 파일이 하나 더 는다. */
    const input = window.prompt(
        '배치 기준일 (yyyy-MM-dd)\n\n'
            + '이 날짜에 배치가 돈 것으로 치고 대상을 고릅니다 (start_date <= 기준일).\n'
            + '상태 변경과 알림은 지금 실제로 일어납니다.',
        todayIso(),
    );
    if (input === null) return;

    const date = input.trim();
    if (!/^\d{4}-\d{2}-\d{2}$/.test(date)) {
        showResult('날짜 형식 오류');
        return;
    }
    run(date);
}

function onPressStart() {
    longPressed = false;
    pressTimer = setTimeout(() => {
        longPressed = true;
        askDateAndRun();
    }, LONG_PRESS_MS);
}

function onPressEnd() {
    clearTimeout(pressTimer);
}

function onClick() {
    /* 길게 눌러 이미 실행됐으면 뒤따라오는 click 은 흘려보낸다 */
    if (longPressed) {
        longPressed = false;
        return;
    }
    run();
}

onBeforeUnmount(() => {
    clearTimeout(pressTimer);
    clearTimeout(resultTimer);
});
</script>

<template>
    <button
        v-if="isDev"
        type="button"
        class="dbt-fab"
        :style="{ bottom: `calc(var(--tt-tabbar-height) + ${props.offset}px)` }"
        :disabled="running"
        title="짧게: 오늘 기준 / 길게: 기준일 지정"
        @pointerdown="onPressStart"
        @pointerup="onPressEnd"
        @pointerleave="onPressEnd"
        @pointercancel="onPressEnd"
        @contextmenu.prevent
        @click="onClick"
    >
        <BoltIcon class="dbt-fab__icon" />
        {{ label }}
    </button>
</template>

<style scoped>
.dbt-fab {
    position: fixed;
    right: 16px;
    height: 32px;
    padding: 0 12px;
    border-radius: var(--tt-radius-full);
    border: 1.5px solid rgba(245, 185, 33, 0.5);
    background: rgba(35, 40, 66, 0.9);
    color: var(--tt-gold);
    font-size: var(--tt-fs-overline);
    font-weight: var(--tt-fw-black);
    font-family: inherit;
    cursor: pointer;
    z-index: var(--tt-z-tabbar);
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
    display: flex;
    align-items: center;
    gap: 4px;
    /* 길게 누를 때 모바일에서 텍스트가 선택되지 않도록 */
    user-select: none;
    -webkit-user-select: none;
    touch-action: manipulation;
}

.dbt-fab:disabled {
    opacity: 0.6;
    cursor: default;
}

.dbt-fab__icon {
    width: 14px;
    height: 14px;
}
</style>
