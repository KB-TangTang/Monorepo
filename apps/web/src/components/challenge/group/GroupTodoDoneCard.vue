<script setup>
import { CheckCircleIcon } from '@heroicons/vue/24/solid';
import mascotCelebration from '@/assets/images/emotions/10_celebration.png';
</script>

<template>
    <div class="done-card">
        <div class="done-card__header">
            <CheckCircleIcon class="done-card__check" />
            <span class="done-card__label">TO-DO · 처리 완료</span>
        </div>

        <div class="done-card__mascot">
            <div class="done-card__glow" />
            <img :src="mascotCelebration" alt="기뻐하는 탕이" class="done-card__img" />
        </div>

        <div class="done-card__title">밀린 일이 없어요</div>
        <div class="done-card__desc">
            기소·투표를 모두 처리했어요.
        </div>
    </div>
</template>

<style scoped>
.done-card {
    background: var(--tt-bg);
    border: 1px solid var(--tt-border);
    border-radius: 22px;
    box-shadow: 0 12px 28px rgba(35, 40, 66, 0.1);
    padding: 20px 16px 18px;
    display: flex;
    flex-direction: column;
    align-items: center;
    animation: tt-fadein 0.3s ease-out both;
}

/* ── 헤더 ───────────────────────────── */
.done-card__header {
    display: flex;
    align-items: center;
    gap: 7px;
}
.done-card__check {
    width: 16px;
    height: 16px;
    color: var(--tt-success);
    flex: none;
}
.done-card__label {
    font-size: var(--tt-fs-overline);
    font-weight: var(--tt-fw-black);
    letter-spacing: 0.08em;
    color: var(--tt-success);
}

/* ── 마스코트 ───────────────────────── */
.done-card__mascot {
    position: relative;
    width: 76px;
    height: 76px;
    margin-top: 8px;
}
.done-card__glow {
    position: absolute;
    inset: 0;
    border-radius: 50%;
    background: radial-gradient(circle, var(--tt-green-soft) 0%, rgba(228, 243, 236, 0) 70%);
    animation: tt-breathe 4.6s ease-in-out infinite;
}
.done-card__img {
    position: absolute;
    inset: 0;
    width: 76px;
    height: 76px;
    object-fit: contain;
    animation: tt-float 3.2s ease-in-out infinite;
}

/* ── 텍스트 ─────────────────────────── */
.done-card__title {
    margin-top: 6px;
    font-size: var(--tt-fs-button);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
}
.done-card__desc {
    margin-top: 4px;
    font-size: var(--tt-fs-badge);
    color: var(--tt-text-muted);
    text-align: center;
    line-height: 1.6;
}

/* ── 키프레임 ───────────────────────── */
@keyframes tt-fadein {
    0% { opacity: 0; }
    100% { opacity: 1; }
}
@keyframes tt-float {
    0%, 100% { transform: translateY(0); }
    50% { transform: translateY(-5px); }
}
@keyframes tt-breathe {
    0%, 100% { transform: scale(1); opacity: 0.32; }
    50% { transform: scale(1.07); opacity: 0.18; }
}
</style>
