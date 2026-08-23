<!--
  명예 법정 진입 배너 — 스크롤 하단

  「재판 기록」과 「명예 법정」은 서로 다른 곳인데, 그룹 상세에서는 둘 다 같은 순위 화면으로
  가고 있었다. 기록 진입을 실제 기록 화면으로 되돌리면서 **순위 진입은 이 배너가 맡는다.**

  **왜 하단인가.** 순위는 「지금 뭘 해야 하나」가 아니라 「끝나고 나면 어떻게 되나」다.
  위에 두면 진행 중인 재판·멤버 현황보다 먼저 읽혀 화면의 우선순위가 뒤집힌다.
  스크롤을 끝까지 내린 사람은 이미 할 일을 다 본 사람이라 그 자리가 맞다.

  갈색 토큰(`--tt-kraft` · `--tt-wood`)을 쓴다. 팀 규칙상 갈색은 판사봉·인장·종이 질감에만
  허용되는데, 이 배너가 그리는 것이 **법대와 트로피**라 정당한 용처다.
  주변 카드가 전부 흰 면이라 이 한 장만 종이색이면 「다른 곳으로 나간다」가 형태로 읽힌다.
-->
<script setup>
import { onMounted, onUnmounted, ref } from 'vue';
import { TrophyIcon, ChevronRightIcon } from '@heroicons/vue/24/solid';

defineProps({
    /**
     * `closed` = 챌린지가 끝난 뒤(확정 순위), `active` = 진행 중(중간 순위).
     * 문구만 가른다 — 진행 중에 「최종 순위」라고 적으면 아직 안 끝난 순위를 확정으로 읽는다.
     */
    variant: { type: String, default: 'active' },
    /** 비우면 variant 기본 문구를 쓴다. 화면이 더 구체적인 말을 갖고 있으면 그걸 넘긴다 */
    subtitle: { type: String, default: '' },
});

const emit = defineEmits(['open']);

const root = ref(null);
const shown = ref(false);
let observer = null;

/*
 * 뷰포트에 들어올 때 한 번만 떠오른다. 하단에 있어 처음에는 화면 밖이라,
 * 마운트 시점에 애니메이션을 돌리면 사용자가 내려왔을 때는 이미 끝나 있다.
 *
 * 동작 줄이기를 켠 사용자에게는 관찰 자체를 걸지 않고 즉시 보여준다 —
 * 관찰만 걸고 CSS 로 트랜지션을 끄면 `shown` 이 false 인 동안 배너가 투명하게 남는다.
 */
onMounted(() => {
    const reduced = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches;
    if (reduced || typeof IntersectionObserver === 'undefined') {
        shown.value = true;
        return;
    }

    observer = new IntersectionObserver(
        (entries) => {
            if (!entries.some((entry) => entry.isIntersecting)) return;
            shown.value = true;
            observer?.disconnect();
            observer = null;
        },
        { threshold: 0.25 },
    );
    if (root.value) observer.observe(root.value);
});

onUnmounted(() => {
    observer?.disconnect();
    observer = null;
});
</script>

<template>
    <button
        ref="root"
        type="button"
        class="honor-entry"
        :class="{ 'honor-entry--shown': shown }"
        @click="emit('open')"
    >
        <span class="honor-entry__seal" aria-hidden="true">
            <TrophyIcon class="honor-entry__trophy" />
        </span>

        <span class="honor-entry__body">
            <span class="honor-entry__title">명예 법정</span>
            <span class="honor-entry__subtitle">
                {{
                    subtitle ||
                    (variant === 'closed'
                        ? '이번 챌린지의 최종 순위를 확인해요'
                        : '지금까지의 절약 순위를 확인해요')
                }}
            </span>
        </span>

        <span class="honor-entry__cta">
            순위 보기
            <ChevronRightIcon class="honor-entry__chevron" />
        </span>
    </button>
</template>

<style scoped>
.honor-entry {
    width: 100%;
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 15px 16px;
    border: 1.5px solid var(--tt-wood);
    border-radius: var(--tt-radius-lg);
    background: var(--tt-kraft);
    text-align: left;
    cursor: pointer;

    /* 진입 전 상태. `--shown` 이 붙으면 제자리로 올라온다 */
    opacity: 0;
    transform: translateY(12px);
    transition:
        opacity 0.42s ease,
        transform 0.42s cubic-bezier(0.22, 1, 0.36, 1);
}
.honor-entry--shown {
    opacity: 1;
    transform: none;
}

/* 인장 — 원형 테두리 안의 트로피. 도장 어휘를 재판 기록 카드와 공유한다 */
.honor-entry__seal {
    flex: none;
    width: 38px;
    height: 38px;
    border-radius: 50%;
    border: 1.5px solid var(--tt-wood);
    display: flex;
    align-items: center;
    justify-content: center;
}
.honor-entry__trophy {
    width: 20px;
    height: 20px;
    color: var(--tt-accent);
}

.honor-entry__body {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 2px;
}
.honor-entry__title {
    font-family: var(--tt-font-serif);
    font-size: var(--tt-fs-label);
    font-weight: var(--tt-fw-black);
    letter-spacing: 0.04em;
    color: var(--tt-text);
}
.honor-entry__subtitle {
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-medium);
    color: var(--tt-text-body);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.honor-entry__cta {
    flex: none;
    display: flex;
    align-items: center;
    gap: 1px;
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-wood);
}
.honor-entry__chevron {
    width: 13px;
    height: 13px;
}

/*
 * 관찰을 아예 걸지 않아 `shown` 이 곧바로 true 이지만, 클래스가 붙는 프레임에
 * 트랜지션이 한 번 돌 수 있다. 여기서도 끊어 둔다.
 */
@media (prefers-reduced-motion: reduce) {
    .honor-entry {
        opacity: 1;
        transform: none;
        transition: none;
    }
}
</style>
