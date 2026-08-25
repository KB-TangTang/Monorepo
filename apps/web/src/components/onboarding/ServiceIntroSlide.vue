<script setup>
/*
 * 서비스 소개 온보딩의 한 면 (이슈 #459).
 *
 * 제목·설명은 위, 일러스트는 가운데 무대(stage)에 겹쳐 놓는다.
 *
 * 무대가 **디자인 원본의 종횡비를 유지**하는 게 이 컴포넌트의 핵심이다.
 * 원본은 328×728 고정 캔버스에 좌표를 px 로 찍어 만들었는데, 실제 기기는 세로 길이가
 * 제각각이라 px 를 그대로 옮기면 화면이 길수록 일러스트가 위쪽에 뭉치고 아래가 빈다.
 * 무대를 원본 비율의 상자로 잡아 남는 세로를 상자 **밖** 여백으로 밀어내면,
 * 상자 안 좌표를 전부 %로 적을 수 있어 어느 기기에서든 구도가 그대로 유지된다.
 * 비율은 면마다 제목 줄 수·하단 버튼 유무가 달라 --intro-stage-ratio 로 따로 받는다.
 *
 * png 매핑이 여기 있는 이유 — utils/welcome.js 는 node:test 가 그대로 불러 읽는 파일이라
 * 에셋 import 를 넣을 수 없다. 슬라이드 데이터는 레이어 **이름**만 갖고, 이름 → png 는
 * 여기서 붙인다.
 */
import { computed } from 'vue';

import assetBank from '@/assets/images/onboarding/asset-bank.png';
import assetCard from '@/assets/images/onboarding/asset-card.png';
import assetSummary from '@/assets/images/onboarding/asset-summary-card.png';
import assetTangi from '@/assets/images/onboarding/asset-tangi.png';
import challengeBooks from '@/assets/images/onboarding/challenge-books.png';
import challengeCard from '@/assets/images/onboarding/challenge-card.png';
import challengeTangi from '@/assets/images/onboarding/challenge-tangi.png';
import groupBubbleBlue from '@/assets/images/onboarding/group-bubble-blue.png';
import groupBubblePink from '@/assets/images/onboarding/group-bubble-pink.png';
import groupCrowd from '@/assets/images/onboarding/group-crowd.png';
import groupTangi from '@/assets/images/onboarding/group-tangi.png';
import introCourt from '@/assets/images/onboarding/intro-court-confetti.png';
import introTangi from '@/assets/images/onboarding/intro-tangi-judge.png';
import outroTangi from '@/assets/images/onboarding/outro-tangi.png';
import sharedChart from '@/assets/images/onboarding/shared-growth-chart.png';

/*
 * 키는 `{면}-{레이어이름}` 이다. SERVICE_INTRO_SLIDES 의 layers[].name 과 짝을 이루고
 * CSS 클래스 접미사로도 그대로 쓴다 — 셋 중 하나만 고치면 테스트가 잡는다.
 */
const INTRO_IMAGES = {
    'intro-court': introCourt,
    'intro-tangi': introTangi,
    'challenge-card': challengeCard,
    'challenge-books': challengeBooks,
    'challenge-tangi': challengeTangi,
    'group-crowd': groupCrowd,
    'group-bubble-pink': groupBubblePink,
    'group-bubble-blue': groupBubbleBlue,
    'group-tangi': groupTangi,
    'asset-summary': assetSummary,
    'asset-card': assetCard,
    'asset-bank': assetBank,
    'asset-chart': sharedChart,
    'asset-tangi': assetTangi,
    'outro-chart': sharedChart,
    'outro-tangi': outroTangi,
};

const props = defineProps({
    /** SERVICE_INTRO_SLIDES 의 한 항목 */
    slide: {
        type: Object,
        required: true,
    },
    /**
     * 지금 보고 있는 면인가.
     * 안 보이는 면은 등장 애니메이션을 돌리지 않는다 — 5면이 한 줄에 다 렌더돼 있어서
     * 그냥 두면 첫 진입에 다섯 면이 동시에 애니메이션을 끝내고, 넘겼을 때 이미 끝나 있다.
     */
    active: {
        type: Boolean,
        default: false,
    },
});

const layers = computed(() =>
    props.slide.layers.map((layer) => ({
        ...layer,
        key: `${props.slide.key}-${layer.name}`,
        src: INTRO_IMAGES[`${props.slide.key}-${layer.name}`],
    })),
);
</script>

<template>
    <section
        class="intro-slide"
        :class="[`intro-slide--${slide.key}`, { 'intro-slide--active': active }]"
        :aria-hidden="active ? undefined : 'true'"
    >
        <header class="intro-slide__head">
            <h2 class="intro-slide__title">
                <template v-for="(line, lineIndex) in slide.title" :key="lineIndex">
                    <br v-if="lineIndex > 0" />
                    <span
                        v-for="(part, partIndex) in line"
                        :key="partIndex"
                        :class="{
                            'intro-slide__accent': part.accent,
                            'intro-slide__hero': part.hero,
                        }"
                        >{{ part.text }}</span
                    >
                </template>
            </h2>
            <p class="intro-slide__lead">
                <template v-for="(line, index) in slide.lead" :key="index">
                    <br v-if="index > 0" />{{ line }}
                </template>
            </p>
        </header>

        <div class="intro-slide__stage">
            <div class="intro-slide__canvas">
                <!--
                  챌린지 면에만 있는 목표 필. 이미지가 아니라 텍스트라 화면 폭에 맞춰
                  글자가 커지고 스크린리더에도 읽힌다.
                -->
                <div v-if="slide.pill" class="intro-slide__pill">
                    <svg
                        class="intro-slide__pill-icon"
                        viewBox="0 0 16 16"
                        fill="none"
                        aria-hidden="true"
                    >
                        <path
                            d="M3 5h10l-.8 8.2a1 1 0 0 1-1 .8H4.8a1 1 0 0 1-1-.8L3 5Z"
                            stroke="currentColor"
                            stroke-width="1.4"
                            stroke-linejoin="round"
                        />
                        <path
                            d="M5.8 6.6V4.4a2.2 2.2 0 0 1 4.4 0v2.2"
                            stroke="currentColor"
                            stroke-width="1.4"
                            stroke-linecap="round"
                        />
                    </svg>
                    <span>{{ slide.pill }}</span>
                </div>

                <img
                    v-for="layer in layers"
                    :key="layer.key"
                    class="intro-slide__layer"
                    :class="`intro-slide__layer--${layer.key}`"
                    :src="layer.src"
                    :alt="layer.alt"
                    draggable="false"
                />

                <span
                    v-for="index in slide.sparkles"
                    :key="`sparkle-${index}`"
                    class="intro-slide__sparkle"
                    :class="`intro-slide__sparkle--${slide.key}-${index}`"
                    aria-hidden="true"
                ></span>
            </div>
        </div>
    </section>
</template>

<style scoped>
/*
 * 좌표계 안내 — 아래 % 는 전부 .intro-slide__canvas 기준이다.
 * 가로는 원본 캔버스 328px, 세로는 면마다 다른 무대 높이(aspect-ratio 주석 참고)를
 * 100% 로 놓고 원본 px 를 나눈 값이다. 예) left: 14.3% == 원본 47px / 328px.
 * 값을 손볼 때는 px 로 되돌려 계산하지 말고 디자인 원본의 px 를 다시 나눈다.
 */
/*
 * 높이에 퍼센트를 쓰지 않는다 — 트랙의 높이가 flex 로 계산된 값이라 퍼센트가 auto 로
 * 떨어진다(자세한 경위는 ServiceIntroView.css 의 .intro__viewport 주석). 가로 트랙의
 * 항목이므로 `align-items: stretch` 기본값이 세로를 알아서 채운다.
 */
.intro-slide {
    display: flex;
    width: 100%;
    flex: 0 0 100%;
    flex-direction: column;
}

/* ── 제목 · 설명 ───────────────────────────────────── */
.intro-slide__head {
    flex: none;
    padding: var(--tt-space-6) var(--tt-space-6) 0;
    text-align: center;
}

.intro-slide__title {
    color: var(--tt-text);
    font-size: min(6.4vw, 25px);
    font-weight: var(--tt-fw-black);
    line-height: 1.42;
    letter-spacing: -0.01em;
}

.intro-slide__accent {
    color: var(--tt-blue);
}

/* 1면의 「탕탕!」만 한 단계 크다 */
.intro-slide__hero {
    font-size: min(7.5vw, 29px);
}

.intro-slide__lead {
    margin-top: var(--tt-space-3);
    color: var(--tt-text-body);
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-medium);
    line-height: 1.68;
}

/* ── 무대 ─────────────────────────────────────────── */
.intro-slide__stage {
    display: flex;
    min-height: 0;
    flex: 1;
    /*
     * stretch 로 두면 캔버스의 aspect-ratio 를 덮어써 비율이 깨진다.
     * 캔버스가 스스로 높이를 갖게 두고 남는 세로는 위아래로 나눈다.
     */
    align-items: center;
    justify-content: center;
    /* 캔버스가 이 상자의 높이를 100cqh 로 읽는다 — 아래 캔버스 주석 참고 */
    container-type: size;
}

/*
 * 원본 비율을 지키는 상자. object-fit: contain 과 같은 「잘리지도 찌그러지지도 않는」 동작.
 *
 * ⚠ **폭을 높이에서 역산하면 안 된다. WebKit(= iOS 전 브라우저)이 못 한다.**
 *
 * 실패한 두 방식이 여기 다 적혀 있다. 다시 시도하지 말 것.
 *
 *  1) `position:absolute; inset:0; margin:auto`
 *     top·bottom 이 둘 다 0 이고 height 가 auto 면 브라우저가 비율을 버리고 높이를 늘린다.
 *     (실측: 비율대로면 650 이어야 할 상자가 무대 높이 504 로 나왔다)
 *
 *  2) flex 항목으로 두고 `stretch 로 받은 높이` × aspect-ratio 로 폭을 구하기
 *     크롬은 되고 **iOS 는 폭이 0 이 됐다.** Flexbox §9.8 의 「stretch 된 아이템의 cross
 *     size 는 definite 하다」를 WebKit 이 따르지 않아 width:auto 가 max-content 로 떨어지는데,
 *     자식이 전부 position:absolute 라 max-content 가 0 이다.
 *     레이어는 폭이 % 라 전부 사라지고, 폭이 px 로 고정된 반짝임만 화면 정중앙에 남았다.
 *     (실측: 높이 611px 은 정상인데 폭만 0. iOS 는 크롬·파이어폭스도 엔진이 WebKit 이라 전멸)
 *
 * 그래서 **정방향(폭 → 높이)으로만 계산한다.**
 *   - 무대 높이를 컨테이너 쿼리 단위 100cqh 로 직접 읽어 폭을 정한다 (역산이 아니다)
 *   - 그 폭에서 aspect-ratio 가 높이를 만든다 — 이 방향은 어느 엔진에서나 동작한다
 *   - min(100%, …) 이 가로가 좁은 기기에서 넘치는 것을 막는다
 */
.intro-slide__canvas {
    position: relative;
    aspect-ratio: var(--intro-canvas-ratio, 328 / 490);
    /*
     * cqh 미지원(iOS 15 이하) 폴백. 세로가 짧은 기기에서 아래가 잘리지만 **보이기는 한다** —
     * 폭 0 으로 통째로 사라지는 것보다 낫다. 아래 줄을 지우면 폴백이 없어진다.
     */
    width: 100%;
    width: min(100%, calc(100cqh * (var(--intro-canvas-ratio, 328 / 490))));
}

/* 면마다 제목 줄 수·하단 버튼 유무가 달라 무대에 남는 세로가 다르다 */
.intro-slide--intro .intro-slide__canvas {
    --intro-canvas-ratio: 328 / 444;
}

.intro-slide--outro .intro-slide__canvas {
    --intro-canvas-ratio: 328 / 450;
}

/*
 * ⚠ max-width: none 이 필요하다. base.css:112 의 전역 `img, svg { max-width: 100% }` 가
 * 무대보다 넓은 레이어를 조용히 잘라낸다 — 1면 법원은 원본이 123.2% 라 100% 로 찌그러졌었다.
 * 에러도 경고도 안 나고 그림만 작아져서 눈으로 대조하기 전엔 모른다.
 * (#464 에서도 같은 규칙이 탕이를 통째로 지운 적이 있다. 이 저장소에서 두 번째다.)
 */
.intro-slide__layer {
    position: absolute;
    max-width: none;
    height: auto;
    -webkit-user-drag: none;
    user-select: none;
}

/* 앞에 세우는 탕이 — 다섯 면 공통 그림자 */
.intro-slide__layer[class*='-tangi'] {
    filter: drop-shadow(0 16px 22px rgb(46 72 140 / 18%));
}

/* 배경 뒤로 물리는 소품 */
.intro-slide__layer[class*='-books'],
.intro-slide__layer[class*='-bubble-'],
.intro-slide__layer--asset-card,
.intro-slide__layer--asset-bank {
    filter: drop-shadow(0 8px 14px rgb(46 72 140 / 12%));
}

/* ── 1면 · 인트로 ─────────────────────────────────── */
.intro-slide__layer--intro-court {
    top: 0.5%;
    left: 50%;
    width: 123.2%;
    opacity: 0.5;
    translate: -50%;
}

.intro-slide__layer--intro-tangi {
    bottom: 0;
    left: 50%;
    width: 72.6%;
    translate: -50%;
}

/* ── 2면 · 챌린지 ─────────────────────────────────── */
.intro-slide__pill {
    position: absolute;
    top: 1.6%;
    left: 50%;
    display: flex;
    align-items: center;
    gap: 5px;
    padding: 6px 12px;
    border: 1px solid var(--tt-blue-soft-border);
    border-radius: var(--tt-radius-full);
    background: var(--tt-bg);
    box-shadow: 0 8px 18px -8px rgb(35 40 66 / 20%);
    color: var(--tt-text);
    font-size: 11.5px;
    font-weight: var(--tt-fw-black);
    white-space: nowrap;
    translate: -50%;
}

.intro-slide__pill-icon {
    width: 14px;
    height: 14px;
    color: var(--tt-blue);
}

.intro-slide__layer--challenge-card {
    top: 7.1%;
    left: 50%;
    width: 68%;
    translate: -50%;
}

.intro-slide__layer--challenge-books {
    bottom: 10.4%;
    left: 14.3%;
    width: 18.3%;
    opacity: 0.85;
}

.intro-slide__layer--challenge-tangi {
    bottom: 10.2%;
    left: 50%;
    width: 64%;
    translate: -50%;
}

/* ── 3면 · 그룹 심판 ──────────────────────────────── */
.intro-slide__layer--group-crowd {
    top: 2.9%;
    left: 50%;
    width: 60.4%;
    opacity: 0.5;
    translate: -50%;
}

.intro-slide__layer--group-bubble-pink {
    top: 0.4%;
    left: 8.5%;
    width: 21.3%;
}

.intro-slide__layer--group-bubble-blue {
    top: 10.6%;
    right: 7.9%;
    width: 22.6%;
}

.intro-slide__layer--group-tangi {
    bottom: 8.2%;
    left: 50%;
    width: 70.1%;
    translate: -50%;
}

/* ── 4면 · 자산관리 ───────────────────────────────── */
.intro-slide__layer--asset-summary {
    top: 2.4%;
    left: 50%;
    width: 65.9%;
    filter: drop-shadow(0 12px 22px rgb(46 72 140 / 16%));
    translate: -50%;
}

.intro-slide__layer--asset-card {
    top: 26.1%;
    left: 7.3%;
    width: 20.1%;
}

.intro-slide__layer--asset-bank {
    top: 23.7%;
    right: 7.9%;
    width: 20.7%;
}

.intro-slide__layer--asset-chart {
    right: 4.3%;
    bottom: 11.4%;
    width: 28%;
    opacity: 0.45;
}

.intro-slide__layer--asset-tangi {
    bottom: 10.2%;
    left: 50%;
    width: 60.4%;
    translate: -50%;
}

/* ── 5면 · 아웃트로 ───────────────────────────────── */
.intro-slide__layer--outro-chart {
    top: -1.1%;
    right: 9.8%;
    width: 79.3%;
    opacity: 0.3;
}

.intro-slide__layer--outro-tangi {
    bottom: 4.4%;
    left: 50%;
    width: 62.8%;
    translate: -50%;
}

/* ── 반짝임 ───────────────────────────────────────── */
.intro-slide__sparkle {
    position: absolute;
    width: 8px;
    height: 8px;
    border-radius: 2px;
    background: var(--tt-onb-sparkle);
    opacity: 0.15;
    rotate: 45deg;
}

.intro-slide__sparkle--intro-1 {
    top: 7.7%;
    left: 9.1%;
}

.intro-slide__sparkle--intro-2 {
    top: 21.6%;
    right: 8.5%;
    width: 7px;
    height: 7px;
    background: var(--tt-gold);
}

.intro-slide__sparkle--outro-1 {
    top: 31.1%;
    left: 10.4%;
    width: 7px;
    height: 7px;
}

/* ── 애니메이션 ───────────────────────────────────── */
/*
 * 보고 있는 면에서만 돈다. 다섯 면이 한 줄에 모두 렌더돼 있어서 조건 없이 걸면
 * 첫 진입에 전부 재생을 마치고, 정작 넘겼을 때는 이미 끝나 있어 아무 일도 안 일어난다.
 */
@keyframes intro-rise {
    from {
        opacity: 0;
        transform: translateY(14px);
    }

    to {
        opacity: 1;
        transform: translateY(0);
    }
}

@keyframes intro-fade {
    from {
        opacity: 0;
    }

    to {
        opacity: 1;
    }
}

@keyframes intro-twinkle {
    0%,
    100% {
        opacity: 0.15;
        scale: 0.7;
    }

    50% {
        opacity: 0.85;
        scale: 1;
    }
}

.intro-slide--active .intro-slide__title {
    animation: intro-rise 0.65s cubic-bezier(0.22, 1, 0.36, 1) 0.08s backwards;
}

.intro-slide--active .intro-slide__lead {
    animation: intro-rise 0.65s cubic-bezier(0.22, 1, 0.36, 1) 0.18s backwards;
}

.intro-slide--active .intro-slide__canvas {
    animation: intro-fade 0.8s ease-out 0.15s backwards;
}

/*
 * 레이어에는 상시 애니메이션을 걸지 않는다.
 * 2면 책·3면 말풍선을 계속 위아래로 띄웠었는데, 캔버스가 통째로 페이드인하는 와중에
 * 일부 조각만 따로 떠다녀 그림이 하나로 안 읽혔다. 등장 연출(캔버스 fade)만 남긴다.
 * 무한 반복은 반짝임(sparkle)에만 둔다 — 그건 위치가 고정이라 그림을 흔들지 않는다.
 */
.intro-slide--active .intro-slide__sparkle {
    animation: intro-twinkle 4.4s ease-in-out 0.9s infinite;
}

.intro-slide--active .intro-slide__sparkle--intro-2 {
    animation-duration: 4.8s;
    animation-delay: 1.7s;
}

/*
 * 「움직임 줄이기」를 켠 사용자에게는 전부 멈춘다. 등장 애니메이션은 backwards 라
 * 그냥 끄면 opacity:0 인 채로 남을 수 있어, 없애는 대신 즉시 끝난 상태로 만든다.
 */
@media (prefers-reduced-motion: reduce) {
    .intro-slide--active :is(.intro-slide__title, .intro-slide__lead, .intro-slide__canvas) {
        animation: none;
        opacity: 1;
        transform: none;
    }

    .intro-slide--active .intro-slide__sparkle {
        animation: none;
    }

    .intro-slide__sparkle {
        opacity: 0.5;
    }
}
</style>
