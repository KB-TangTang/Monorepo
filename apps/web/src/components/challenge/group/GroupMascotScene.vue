<script setup>
import mascotSmile from '@/assets/images/emotions/03_gentle_smile.png';
import mascotAnxious from '@/assets/images/emotions/17_anxious.png';
import mascotJudging from '@/assets/images/emotions/48_judging.png';

defineProps({
    /** 'peaceful' | 'indictment' | 'vote' */
    scene: { type: String, default: 'peaceful' },
});
</script>

<template>
    <div class="mascot-scene">
        <!-- ===== Peaceful · 차를 마시며 쉬는 탕이 ===== -->
        <template v-if="scene === 'peaceful'">
            <div class="mascot-scene__stage">
                <div class="mascot-scene__ground" />
                <div class="mascot-scene__glow mascot-scene__glow--green" />
                <div class="mascot-scene__float">
                    <img :src="mascotSmile" alt="차를 마시며 쉬는 탕이" class="mascot-scene__img" />
                </div>
                <!-- 찻잔 -->
                <div class="mascot-scene__cup">
                    <div class="mascot-scene__cup-body">
                        <div class="mascot-scene__cup-tea" />
                    </div>
                    <div class="mascot-scene__cup-handle" />
                    <div class="mascot-scene__cup-saucer" />
                </div>
                <!-- 증기 -->
                <span class="mascot-scene__steam mascot-scene__steam--1" />
                <span class="mascot-scene__steam mascot-scene__steam--2" />
                <span class="mascot-scene__steam mascot-scene__steam--3" />
                <!-- 파티클 -->
                <span class="mascot-scene__particle mascot-scene__particle--green" />
                <span class="mascot-scene__particle mascot-scene__particle--gold" />
            </div>
            <div class="mascot-scene__text">탕이도 차 한 잔 마시며 쉬고 있어요</div>
            <div class="mascot-scene__subtext">새 소식이 생기면 바로 알려드릴게요</div>
        </template>

        <!-- ===== Indictment · 서류를 나르는 탕이 ===== -->
        <template v-else-if="scene === 'indictment'">
            <div class="mascot-scene__stage">
                <div class="mascot-scene__ground" />
                <!-- 스피드 라인 -->
                <span class="mascot-scene__dash mascot-scene__dash--1" />
                <span class="mascot-scene__dash mascot-scene__dash--2" />
                <span class="mascot-scene__dash mascot-scene__dash--3" />
                <!-- 먼지 -->
                <span class="mascot-scene__puff mascot-scene__puff--1" />
                <span class="mascot-scene__puff mascot-scene__puff--2" />
                <!-- 달리는 탕이 + 서류 -->
                <div class="mascot-scene__runner">
                    <img :src="mascotAnxious" alt="바쁘게 달리는 탕이" class="mascot-scene__img" />
                    <div class="mascot-scene__paper">
                        <div class="mascot-scene__paper-line" />
                        <div class="mascot-scene__paper-line" />
                        <div class="mascot-scene__paper-line mascot-scene__paper-line--gold" />
                    </div>
                </div>
            </div>
            <div class="mascot-scene__text">탕이가 기소 서류를 나르고 있어요</div>
            <div class="mascot-scene__subtext">응답이 끝나면 투표가 열려요</div>
        </template>

        <!-- ===== Vote · 투표하는 탕이 ===== -->
        <template v-else-if="scene === 'vote'">
            <div class="mascot-scene__stage">
                <!-- 펄스 배경 -->
                <div class="mascot-scene__pulse" />
                <div class="mascot-scene__float mascot-scene__float--vote">
                    <img :src="mascotJudging" alt="투표하는 탕이" class="mascot-scene__img" />
                </div>
                <!-- 투표함 -->
                <div class="mascot-scene__ballot-box">
                    <div class="mascot-scene__ballot-slot" />
                    <div class="mascot-scene__ballot-label">VOTE</div>
                </div>
                <!-- 날아드는 투표지 -->
                <div class="mascot-scene__ballot-paper">
                    <div class="mascot-scene__paper-line" />
                    <div class="mascot-scene__paper-line" />
                    <div class="mascot-scene__paper-line mascot-scene__paper-line--green" />
                </div>
                <!-- 유·무 뱃지 -->
                <span class="mascot-scene__tally mascot-scene__tally--guilty">유</span>
                <span class="mascot-scene__tally mascot-scene__tally--not-guilty">무</span>
            </div>
            <div class="mascot-scene__text">소중한 한 표 부탁드려요!</div>
            <div class="mascot-scene__subtext">변론을 확인하고 투표를 진행해봐요</div>
        </template>
    </div>
</template>

<style scoped>
/* ── 공통 ──────────────────────────────── */
.mascot-scene {
    padding: 0 6px;
    margin-top: var(--tt-space-5);
}

/* 무대 안의 요소는 전부 `left: Npx` 절대좌표다. 무대가 부모를 따라 늘어나면
 * 그 좌표들이 왼쪽에 그대로 남아 마스코트가 왼쪽으로 쏠린다.
 * 무대를 240px 고정폭으로 두고 가운데 정렬해 세 씬의 기준을 하나로 맞춘다.
 * ⚠ 아래 좌표를 손볼 때는 「무대 폭 240 · 중심 x=120」을 기준으로 계산한다. */
.mascot-scene__stage {
    position: relative;
    width: 240px;
    max-width: 100%;
    height: 112px;
    margin: 0 auto;
}

.mascot-scene__ground {
    position: absolute;
    left: 14px;
    right: 14px;
    bottom: 20px;
    height: 1.5px;
    background: rgba(35, 40, 66, 0.07);
}

.mascot-scene__img {
    width: 88px;
    height: 88px;
    object-fit: contain;
}

.mascot-scene__text {
    font-size: var(--tt-fs-button);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
    text-align: center;
    margin-top: 2px;
}

.mascot-scene__subtext {
    font-size: var(--tt-fs-caption);
    color: var(--tt-text-muted);
    text-align: center;
    margin-top: 6px;
}

/* ── Peaceful 씬 ──────────────────────── */
.mascot-scene__glow--green {
    position: absolute;
    left: 50%;
    bottom: 8px;
    width: 124px;
    height: 124px;
    /* 탕이(64~152)+찻잔(132~175) 묶음 중심 x=120 에 후광 중심을 맞춘다 */
    margin-left: -62px;
    border-radius: 50%;
    background: radial-gradient(circle, #DDF1E6 0%, rgba(221, 241, 230, 0) 70%);
    animation: tt-breathe 4.6s ease-in-out infinite;
}

.mascot-scene__float {
    position: absolute;
    left: 64px;
    bottom: 12px;
    width: 88px;
    height: 88px;
    animation: tt-float 3.6s ease-in-out infinite;
}

.mascot-scene__cup {
    position: absolute;
    left: 132px;
    bottom: 18px;
    width: 48px;
    height: 40px;
    animation: tt-sip 4.6s ease-in-out infinite;
    transform-origin: 20% 100%;
    z-index: 2;
}

.mascot-scene__cup-body {
    position: absolute;
    left: 2px;
    bottom: 5px;
    width: 32px;
    height: 24px;
    border-radius: 5px 5px 12px 12px;
    background: var(--tt-white);
    border: 1.5px solid #C9CBD4;
    box-shadow: 0 5px 12px rgba(35, 40, 66, 0.12);
}

.mascot-scene__cup-tea {
    height: 4px;
    margin: 3px 5px 0;
    border-radius: var(--tt-radius-full);
    background: #E7A70C;
}

.mascot-scene__cup-handle {
    position: absolute;
    left: 32px;
    bottom: 11px;
    width: 11px;
    height: 13px;
    border: 1.5px solid #C9CBD4;
    border-left: none;
    border-radius: 0 9px 9px 0;
}

.mascot-scene__cup-saucer {
    position: absolute;
    left: 0;
    bottom: 0;
    width: 40px;
    height: 5px;
    border-radius: var(--tt-radius-full);
    background: #D4D8E2;
}

.mascot-scene__steam {
    position: absolute;
    width: 5px;
    border-radius: var(--tt-radius-full);
    z-index: 2;
    animation: tt-steam 2.6s ease-out infinite;
}
.mascot-scene__steam--1 {
    left: 140px; bottom: 44px;
    height: 13px;
    background: rgba(46, 158, 107, 0.4);
}
.mascot-scene__steam--2 {
    left: 150px; bottom: 46px;
    height: 11px;
    background: rgba(46, 158, 107, 0.32);
    animation-delay: 0.85s;
}
.mascot-scene__steam--3 {
    left: 159px; bottom: 44px;
    width: 4px; height: 10px;
    background: rgba(46, 158, 107, 0.24);
    animation-delay: 1.6s;
}

.mascot-scene__particle {
    position: absolute;
    border-radius: 50%;
}
.mascot-scene__particle--green {
    left: 42px; top: 12px;
    width: 7px; height: 7px;
    background: rgba(46, 158, 107, 0.3);
    animation: tt-float 3.2s ease-in-out infinite;
}
.mascot-scene__particle--gold {
    left: 196px; top: 26px;
    width: 5px; height: 5px;
    background: rgba(245, 185, 33, 0.5);
    animation: tt-float 4.1s ease-in-out infinite;
    animation-delay: 0.6s;
}

/* ── Indictment 씬 ────────────────────── */
.mascot-scene__dash {
    position: absolute;
    left: 158px;
    height: 4px;
    border-radius: var(--tt-radius-full);
    animation: tt-dash 0.72s linear infinite;
}
.mascot-scene__dash--1 {
    top: 30px; width: 30px;
    background: rgba(245, 185, 33, 0.75);
}
.mascot-scene__dash--2 {
    top: 48px; width: 44px;
    background: #D4D8E2;
    animation-delay: 0.18s;
}
.mascot-scene__dash--3 {
    top: 66px; width: 24px;
    background: rgba(245, 185, 33, 0.45);
    animation-delay: 0.36s;
}

.mascot-scene__puff {
    position: absolute;
    border-radius: 50%;
    animation: tt-puff 0.9s ease-out infinite;
}
.mascot-scene__puff--1 {
    left: 102px; bottom: 14px;
    width: 14px; height: 14px;
    background: #DFE2EA;
}
.mascot-scene__puff--2 {
    left: 116px; bottom: 12px;
    width: 10px; height: 10px;
    background: #E5E8EF;
    animation-delay: 0.35s;
}

/* 서류(러너 기준 -6px)까지 포함한 묶음 74~168 → 중심 x=121 */
.mascot-scene__runner {
    position: absolute;
    left: 80px;
    bottom: 10px;
    width: 88px;
    height: 88px;
    animation: tt-run 0.46s ease-in-out infinite alternate;
    transform-origin: 50% 90%;
}

.mascot-scene__paper {
    position: absolute;
    left: -6px;
    top: 28px;
    width: 26px;
    height: 32px;
    border-radius: 4px;
    background: var(--tt-white);
    border: 1px solid var(--tt-border);
    box-shadow: 0 5px 12px rgba(35, 40, 66, 0.14);
    animation: tt-paper 0.52s ease-in-out infinite alternate;
    transform-origin: 60% 100%;
}

.mascot-scene__paper-line {
    height: 3px;
    background: var(--tt-border-track);
    margin: 7px 5px 0;
    border-radius: 2px;
}
.mascot-scene__paper-line + .mascot-scene__paper-line {
    margin-top: 4px;
}
.mascot-scene__paper-line--gold {
    background: var(--tt-gold);
    margin-right: 9px;
}
.mascot-scene__paper-line--green {
    background: var(--tt-green);
    margin-right: 8px;
}

/* ── Vote 씬 ──────────────────────────── */
.mascot-scene__pulse {
    position: absolute;
    left: 50%;
    bottom: 14px;
    width: 104px;
    height: 104px;
    margin-left: -52px;
    border-radius: 50%;
    background: var(--tt-gold);
    opacity: 0.35;
    animation: tt-pulse 1.9s ease-in-out infinite;
}

/* 유·무 뱃지(21~) ~ 투표함(~219) 묶음 중심 x=120 */
.mascot-scene__float--vote {
    left: 50%;
    margin-left: -43px;
    bottom: 6px;
    animation: tt-float 2.4s ease-in-out infinite;
}

/* right 로 붙이면 무대가 넓어질 때 탕이와 투표함 사이가 벌어진다 — left 고정 */
.mascot-scene__ballot-box {
    position: absolute;
    left: 169px;
    bottom: 12px;
    width: 50px;
    height: 40px;
    border-radius: var(--tt-radius-xs);
    background: var(--tt-ink);
    box-shadow: 0 8px 16px rgba(35, 40, 66, 0.2);
}

.mascot-scene__ballot-slot {
    position: absolute;
    left: 11px;
    top: -4px;
    width: 28px;
    height: 5px;
    border-radius: 3px;
    background: var(--tt-gold);
}

.mascot-scene__ballot-label {
    position: absolute;
    left: 0;
    right: 0;
    bottom: 9px;
    text-align: center;
    font-size: 9px;
    font-weight: var(--tt-fw-black);
    letter-spacing: 0.1em;
    color: var(--tt-gold);
}

.mascot-scene__ballot-paper {
    position: absolute;
    left: 183px;
    bottom: 44px;
    width: 22px;
    height: 26px;
    border-radius: 3px;
    background: var(--tt-white);
    border: 1px solid var(--tt-border);
    box-shadow: 0 4px 10px rgba(35, 40, 66, 0.14);
    animation: tt-ballot 1.9s ease-in infinite;
}

.mascot-scene__ballot-paper .mascot-scene__paper-line {
    margin: 6px 4px 0;
}
.mascot-scene__ballot-paper .mascot-scene__paper-line + .mascot-scene__paper-line {
    margin-top: 3px;
}

.mascot-scene__tally {
    position: absolute;
    width: 26px;
    height: 26px;
    border-radius: 50%;
    color: var(--tt-white);
    font-size: var(--tt-fs-overline);
    font-weight: var(--tt-fw-black);
    display: flex;
    align-items: center;
    justify-content: center;
    border: 2px solid var(--tt-white);
    box-shadow: 0 4px 10px rgba(35, 40, 66, 0.12);
    animation: tt-tally 1.9s ease-out infinite;
}
.mascot-scene__tally--guilty {
    left: 21px; top: 6px;
    background: var(--tt-green);
}
.mascot-scene__tally--not-guilty {
    left: 47px; top: 34px;
    background: var(--tt-blue);
    animation-delay: 0.55s;
}

/* ── 키프레임 ─────────────────────────── */
@keyframes tt-float {
    0%, 100% { transform: translateY(0); }
    50% { transform: translateY(-5px); }
}

@keyframes tt-breathe {
    0%, 100% { transform: scale(1); opacity: 0.32; }
    50% { transform: scale(1.07); opacity: 0.18; }
}

@keyframes tt-sip {
    0%, 64%, 100% { transform: rotate(0); }
    76%, 86% { transform: rotate(-9deg); }
}

@keyframes tt-steam {
    0% { transform: translateY(0) scaleX(1); opacity: 0; }
    22% { opacity: 0.5; }
    100% { transform: translateY(-28px) scaleX(1.7); opacity: 0; }
}

@keyframes tt-run {
    0% { transform: translateY(0) rotate(-5deg); }
    100% { transform: translateY(-8px) rotate(4deg); }
}

@keyframes tt-dash {
    0% { transform: translateX(26px); opacity: 0; }
    25% { opacity: 0.95; }
    100% { transform: translateX(-96px); opacity: 0; }
}

@keyframes tt-puff {
    0% { transform: scale(0.4); opacity: 0.5; }
    100% { transform: scale(1.6) translate(-16px, -8px); opacity: 0; }
}

@keyframes tt-paper {
    0% { transform: rotate(-10deg) translateY(0); }
    100% { transform: rotate(9deg) translateY(-5px); }
}

@keyframes tt-pulse {
    0%, 100% { transform: scale(0.86); opacity: 0.3; }
    50% { transform: scale(1.14); opacity: 0.08; }
}

@keyframes tt-ballot {
    0% { transform: translateY(-24px) rotate(-8deg); opacity: 0; }
    20% { opacity: 1; }
    62% { transform: translateY(16px) rotate(3deg); opacity: 1; }
    78%, 100% { transform: translateY(22px) rotate(3deg); opacity: 0; }
}

@keyframes tt-tally {
    0% { transform: scale(0.5); opacity: 0; }
    30% { transform: scale(1.08); opacity: 1; }
    45%, 100% { transform: scale(1); opacity: 1; }
}
</style>
