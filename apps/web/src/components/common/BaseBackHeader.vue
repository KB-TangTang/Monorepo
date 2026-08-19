<!--
  용도: 뒤로가기 + 화면 제목만 담당하는 공용 상세화면 상단 바.
  언제 쓰는지: 자산 상세 4개 화면, 장부 상세내역 화면 등 "뒤로가기 헤더"가 필요한 모든 화면.
-->
<script setup>
import { getCurrentInstance } from 'vue';
import { useRouter } from 'vue-router';

defineProps({
    title: { type: String, required: true },
    backLabel: { type: String, default: '뒤로가기' },
});

/*
 * `@back` 을 건 화면은 **목적지를 스스로 정한다.** 걸지 않은 화면은 지금까지처럼 히스토리를 되돌린다.
 *
 * 이슈 #172 전까지 이 컴포넌트에 `defineEmits` 가 없어, `@back` 은 선언되지 않은 리스너로
 * 루트 `<header>` 에 흘러가 **버튼을 눌러도 영영 호출되지 않았다.** 판결 플로우 세 화면이
 * 그걸 모르고 `@back` 을 걸어 둔 탓에 뒤로가기가 전부 맹목적인 `router.back()` 이었다.
 */
const emit = defineEmits(['back']);
const instance = getCurrentInstance();
const router = useRouter();

function goBack() {
    if (instance.vnode.props?.onBack) {
        emit('back');
        return;
    }
    router.back();
}
</script>

<template>
    <header class="base-back-header">
        <button
            type="button"
            class="base-back-header__back"
            :aria-label="backLabel"
            @click="goBack"
        >
            <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="m15 4-8 8 8 8" />
            </svg>
        </button>
        <h1 class="base-back-header__title">{{ title }}</h1>
    </header>
</template>

<style scoped>
.base-back-header {
    display: flex;
    align-items: center;
    gap: var(--tt-space-3);
    margin-bottom: var(--tt-space-4);
}

.base-back-header__back {
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

.base-back-header__back svg {
    width: 28px;
    height: 28px;
    fill: none;
    stroke: currentColor;
    stroke-width: 2.2;
    stroke-linecap: round;
    stroke-linejoin: round;
}

.base-back-header__title {
    font-size: var(--tt-fs-title);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
}
</style>
