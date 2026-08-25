<script setup>
/*
 * 서비스 소개 온보딩 5면 (이슈 #459).
 *
 * 앱을 처음 여는 사용자가 로그인보다 **먼저** 만나는 화면이다.
 * 나가는 길은 둘이고 둘 다 같은 곳으로 간다 — 우측 상단 「건너뛰기」와 마지막 면의
 * 「탕탕 시작하기」. 나갈 때 열람 기록을 남긴다.
 *
 * 면을 넘기는 버튼은 하단에 「이전(왼쪽) · 인디케이터 · 다음(오른쪽)」으로 둔다.
 * 「건너뛰기」를 하단에서 위로 올린 이유 — 나가는 버튼과 넘기는 버튼이 한 줄에 섞여
 * 있으면 왼쪽 버튼이 뒤로 가는 것인지 밖으로 나가는 것인지 눌러 보기 전엔 모른다.
 *
 * 다섯 면을 한 줄(track)에 늘어놓고 통째로 옆으로 민다. v-if 로 한 면만 그리면
 * 넘길 때마다 png 를 새로 붙여 첫 프레임이 빈 채로 지나간다.
 */
import { computed, ref } from 'vue';
import { useRouter } from 'vue-router';

import ServiceIntroDots from '@/components/onboarding/ServiceIntroDots.vue';
import ServiceIntroSlide from '@/components/onboarding/ServiceIntroSlide.vue';
import {
    SERVICE_INTRO_COUNT,
    SERVICE_INTRO_SLIDES,
    clampSlideIndex,
    isLastSlide,
    markWelcomeSeen,
} from '@/utils/welcome';

/** 손을 뗐을 때 면을 넘길 최소 이동량. 무대 폭 대비 비율이라 기기 크기를 안 탄다 */
const SWIPE_RATIO = 0.18;

/** 위 비율이 너무 작아지는 좁은 화면을 위한 하한 (px) */
const SWIPE_MIN_PX = 48;

const router = useRouter();

const current = ref(0);
const dragX = ref(0);
const dragging = ref(false);

/* 드래그 시작 지점. 반응형일 필요가 없어 ref 로 만들지 않는다 */
let pointerStartX = 0;
let pointerStartY = 0;
let trackWidth = 0;
/*
 * 세로로 긁는 동작을 가로 스와이프로 오해하지 않기 위한 판정 상태.
 * null 이면 아직 방향이 안 정해진 것 — 처음 몇 px 을 보고 한 번만 정한다.
 */
let horizontal = null;

const slides = SERVICE_INTRO_SLIDES;
const total = SERVICE_INTRO_COUNT;

const currentSlide = computed(() => slides[current.value]);
const onLastSlide = computed(() => isLastSlide(current.value));

/* 면을 미는 거리 + 손가락을 따라가는 여분 */
const trackStyle = computed(() => ({
    transform: `translate3d(calc(${-current.value * 100}% + ${dragX.value}px), 0, 0)`,
}));

function goTo(index) {
    current.value = clampSlideIndex(index);
}

function goNext() {
    goTo(current.value + 1);
}

function goPrev() {
    goTo(current.value - 1);
}

/** 온보딩을 끝내고 로그인으로 보낸다. 「건너뛰기」와 마지막 면 버튼이 함께 쓴다 */
function finish() {
    /*
     * 기록에 실패해도(프라이빗 모드 등) 로그인으로는 보낸다.
     * 다음에 온보딩이 한 번 더 뜨는 건 참을 수 있지만, 여기 갇히는 건 안 된다.
     */
    markWelcomeSeen();
    router.replace({ name: 'login' });
}

function onDragStart(event) {
    /* 마우스 오른쪽 버튼·보조 포인터는 무시한다 */
    if (event.button !== 0) {
        return;
    }
    dragging.value = true;
    pointerStartX = event.clientX;
    pointerStartY = event.clientY;
    trackWidth = event.currentTarget.clientWidth || 1;
    horizontal = null;
    event.currentTarget.setPointerCapture?.(event.pointerId);
}

function onDragMove(event) {
    if (!dragging.value) {
        return;
    }

    const dx = event.clientX - pointerStartX;
    const dy = event.clientY - pointerStartY;

    /*
     * 방향을 한 번만 정한다. 매 프레임 비교하면 대각선으로 긋는 동안 판정이 오락가락해
     * 화면이 떨린다. 10px 은 손떨림과 의도를 가르는 실측 기준선이다.
     */
    if (horizontal === null) {
        if (Math.abs(dx) < 10 && Math.abs(dy) < 10) {
            return;
        }
        horizontal = Math.abs(dx) > Math.abs(dy);
    }

    if (!horizontal) {
        return;
    }

    /*
     * 첫 면에서 오른쪽으로, 마지막 면에서 왼쪽으로 당기면 저항을 준다.
     * 그냥 막으면 화면이 굳은 것처럼 느껴지고, 그대로 따라가면 빈 여백이 드러난다.
     */
    const pullingPastStart = current.value === 0 && dx > 0;
    const pullingPastEnd = current.value === total - 1 && dx < 0;
    dragX.value = pullingPastStart || pullingPastEnd ? dx * 0.28 : dx;
}

function onDragEnd() {
    if (!dragging.value) {
        return;
    }

    const threshold = Math.max(trackWidth * SWIPE_RATIO, SWIPE_MIN_PX);
    const moved = dragX.value;

    dragging.value = false;
    dragX.value = 0;
    horizontal = null;

    if (Math.abs(moved) < threshold) {
        return;
    }
    goTo(current.value + (moved < 0 ? 1 : -1));
}
</script>

<template>
    <!--
      role="group" + aria-roledescription 으로 스크린리더가 「캐러셀」임을 알린다.
      면 전환 자체는 ServiceIntroDots 의 role="status" 가 읽어 준다.
    -->
    <div class="intro" role="group" aria-roledescription="캐러셀" aria-label="서비스 소개">
        <!--
          마지막 면에서는 바로 아래 「탕탕 시작하기」가 같은 곳으로 가므로 건너뛰기를 지운다.
          다만 v-if 로 없애면 상단 바가 접히면서 아래 그림이 통째로 밀려 올라간다.
          자리는 남기고 보이지만 않게 한다.
        -->
        <header class="intro__topbar">
            <button
                type="button"
                class="intro__skip"
                :class="{ 'intro__skip--hidden': onLastSlide }"
                :tabindex="onLastSlide ? -1 : 0"
                :aria-hidden="onLastSlide"
                @click="finish"
            >
                건너뛰기
            </button>
        </header>

        <div
            class="intro__viewport"
            @pointerdown="onDragStart"
            @pointermove="onDragMove"
            @pointerup="onDragEnd"
            @pointercancel="onDragEnd"
            @keydown.left.prevent="goTo(current - 1)"
            @keydown.right.prevent="goNext"
        >
            <div
                class="intro__track"
                :class="{ 'intro__track--dragging': dragging }"
                :style="trackStyle"
            >
                <ServiceIntroSlide
                    v-for="(slide, index) in slides"
                    :key="slide.key"
                    :slide="slide"
                    :active="index === current"
                />
            </div>
        </div>

        <!--
          하단은 면이 바뀌어도 구조가 같다 — 마지막 면에만 Ink 버튼이 한 줄 더 얹힌다.
          면마다 다른 footer 를 그리면 「이전」이 마지막 면에서만 사라지거나 좌우로 튄다.
        -->
        <footer class="intro__footer">
            <button v-if="currentSlide.cta" type="button" class="intro__primary" @click="finish">
                {{ currentSlide.cta }}
            </button>

            <!--
              쓸 수 없는 버튼은 지우지 않고 숨기기만 한다 (1면의 「이전」, 마지막 면의 「다음」).
              양옆 버튼이 남는 폭을 반씩 나눠 가져 인디케이터를 가운데로 밀고 있어서,
              한쪽을 없애면 점이 통째로 반대쪽으로 옮겨 간다.
            -->
            <div class="intro__nav-row">
                <button
                    type="button"
                    class="intro__nav intro__nav--prev"
                    :class="{ 'intro__nav--hidden': current === 0 }"
                    :tabindex="current === 0 ? -1 : 0"
                    :aria-hidden="current === 0"
                    @click="goPrev"
                >
                    <svg viewBox="0 0 6 10" fill="none" aria-hidden="true">
                        <path
                            d="M5 1L1 5l4 4"
                            stroke="currentColor"
                            stroke-width="1.8"
                            stroke-linecap="round"
                            stroke-linejoin="round"
                        />
                    </svg>
                    이전
                </button>

                <ServiceIntroDots class="intro__dots" :current="current" :total="total" />

                <button
                    type="button"
                    class="intro__nav intro__nav--next"
                    :class="{ 'intro__nav--hidden': onLastSlide }"
                    :tabindex="onLastSlide ? -1 : 0"
                    :aria-hidden="onLastSlide"
                    @click="goNext"
                >
                    다음
                    <svg viewBox="0 0 6 10" fill="none" aria-hidden="true">
                        <path
                            d="M1 1l4 4-4 4"
                            stroke="currentColor"
                            stroke-width="1.8"
                            stroke-linecap="round"
                            stroke-linejoin="round"
                        />
                    </svg>
                </button>
            </div>
        </footer>
    </div>
</template>

<style scoped src="./ServiceIntroView.css"></style>
