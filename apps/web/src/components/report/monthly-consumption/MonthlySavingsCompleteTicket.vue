<script setup>
import okayTangi from '@/assets/images/emotions/41_okay.png';
</script>

<template>
    <section class="savings-complete" aria-labelledby="savings-complete-title">
        <span class="savings-complete__stamp" aria-hidden="true">✓</span>
        <div>
            <p>절약 감정서</p>
            <h2 id="savings-complete-title">고정 지출 의심 0건</h2>
            <span>이번 달은 모두 깨끗해요</span>
        </div>
        <i aria-hidden="true"></i>
        <img :src="okayTangi" alt="확인 완료를 알리는 탕이" />
    </section>
</template>

<style scoped>
.savings-complete {
    position: relative;
    display: flex;
    align-items: center;
    gap: var(--tt-space-4);
    min-height: 132px;
    overflow: hidden;
    padding: var(--tt-space-5) 116px var(--tt-space-5) var(--tt-space-6);
    background:
        radial-gradient(
            circle at 88% 20%,
            color-mix(in srgb, var(--tt-success) 16%, transparent) 0 42px,
            transparent 43px
        ),
        radial-gradient(
            circle at 73% 115%,
            color-mix(in srgb, var(--tt-success) 10%, transparent) 0 68px,
            transparent 69px
        ),
        linear-gradient(
            135deg,
            color-mix(in srgb, var(--tt-success-subtle) 72%, var(--tt-bg)),
            var(--tt-bg)
        );
    border: 0;
    border-radius: var(--tt-radius-xl);
    box-shadow: var(--tt-elevation-1);
}
.savings-complete::before,
.savings-complete::after {
    position: absolute;
    top: 50%;
    z-index: 3;
    width: 26px;
    height: 26px;
    content: '';
    background: var(--tt-bg-subtle);
    border: 0;
    border-radius: var(--tt-radius-full);
    transform: translateY(-50%);
}
.savings-complete::before {
    left: -14px;
}
.savings-complete::after {
    right: -14px;
}
.savings-complete__stamp {
    position: relative;
    z-index: 2;
    display: grid;
    flex: 0 0 54px;
    width: 54px;
    height: 54px;
    color: var(--tt-text-inverse);
    font-size: var(--tt-fs-title);
    font-weight: var(--tt-fw-black);
    background: var(--tt-success);
    border: 5px solid color-mix(in srgb, var(--tt-bg) 72%, transparent);
    border-radius: var(--tt-radius-full);
    box-shadow: 0 0 0 1px color-mix(in srgb, var(--tt-success) 30%, transparent);
    place-items: center;
}
.savings-complete div {
    position: relative;
    z-index: 2;
}
.savings-complete p {
    margin-bottom: var(--tt-space-1);
    color: var(--tt-success-deep);
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-black);
}
.savings-complete h2 {
    font-size: var(--tt-fs-section);
    font-weight: var(--tt-fw-black);
}
.savings-complete div > span {
    display: block;
    margin-top: var(--tt-space-1);
    color: var(--tt-success-deep);
    font-size: var(--tt-fs-caption);
}
.savings-complete i {
    position: absolute;
    top: var(--tt-space-4);
    right: 104px;
    bottom: var(--tt-space-4);
    border-left: 2px dashed color-mix(in srgb, var(--tt-success) 24%, transparent);
}
.savings-complete img {
    position: absolute;
    right: var(--tt-space-2);
    bottom: 4px;
    z-index: 2;
    width: 98px;
    height: 108px;
    object-fit: contain;
}
@media (max-width: 360px) {
    .savings-complete {
        gap: var(--tt-space-3);
        padding-left: var(--tt-space-5);
    }
    .savings-complete__stamp {
        flex-basis: 46px;
        width: 46px;
        height: 46px;
    }
}
</style>
