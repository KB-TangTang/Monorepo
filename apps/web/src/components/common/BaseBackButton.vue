<!--
  용도: 화면 좌측 상단의 뒤로가기(‹). 히스토리를 한 칸 되돌리거나, 갈 곳을 지정해 보낼 수 있다.
  언제 쓰는지: 상세·목록 화면의 상단 헤더.
  쓰면 안 되는 경우: 연결 플로우 단계 화면. 그쪽은 단계 이동 규칙이 따로 있어 LinkStepHeader 를 쓴다.

  ⚠ to 를 주지 않으면 router.back() 이다. 히스토리 뒤가 라우터 가드에 막히는 화면이라면
    to 로 갈 곳을 명시할 것 — 가드가 되돌려보내면 눌러도 아무 일이 없는 것처럼 보인다
    (2026-08-06 계좌 연동 플로우에서 실제로 겪은 문제).
-->
<script setup>
import { useRouter } from 'vue-router';

const props = defineProps({
    /** 접근성 레이블. 어디로 가는지 말해준다 */
    label: { type: String, default: '뒤로 가기' },
    /** 목적지. 주지 않으면 히스토리를 한 칸 되돌린다 */
    to: { type: [String, Object], default: null },
});

const router = useRouter();

function goBack() {
    if (props.to) {
        router.push(props.to);
        return;
    }
    router.back();
}
</script>

<template>
    <button type="button" class="tt-back" :aria-label="label" @click="goBack">
        <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="m15 4-8 8 8 8" />
        </svg>
    </button>
</template>

<style scoped>
.tt-back {
    display: grid;
    flex: 0 0 32px;
    width: 32px;
    height: 40px;
    padding: 0;
    color: var(--tt-text);
    background: transparent;
    border: 0;
    cursor: pointer;
    place-items: center;
}

.tt-back svg {
    width: 28px;
    height: 28px;
    fill: none;
    stroke: currentColor;
    stroke-width: 2.2;
    stroke-linecap: round;
    stroke-linejoin: round;
}
</style>
