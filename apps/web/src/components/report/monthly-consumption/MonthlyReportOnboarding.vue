<script setup>
import analyzingTangi from '@/assets/images/prosecutor_tangtang/magnifying_stare.png';

defineProps({
    displayName: { type: String, default: '고객' },
});
</script>

<template>
    <section class="report-onboarding" aria-labelledby="onboarding-title">
        <div class="report-onboarding__visual" aria-hidden="true">
            <span class="report-onboarding__orbit"></span>
            <span class="report-onboarding__paper report-onboarding__paper--left"></span>
            <span class="report-onboarding__paper report-onboarding__paper--right"></span>
            <img :src="analyzingTangi" alt="" />
        </div>
        <span class="report-onboarding__status">소비 내역 분석 중</span>
        <h2 id="onboarding-title">탕이가 {{ displayName }}님의 소비 내역을<br />분석하고 있어요</h2>
        <p class="report-onboarding__copy">
            이번 달 기록을 차곡차곡 모아<br />다음 달 첫 소비 판결문으로 알려드릴게요.
        </p>
        <div class="report-onboarding__guide">
            <p class="report-onboarding__guide-title">첫 판결문에서 확인할 수 있어요</p>
            <ul>
                <li>한 달 총 소비</li>
                <li>카테고리별<br />소비 분석</li>
                <li>탕이의<br />소비 한마디</li>
            </ul>
            <p class="report-onboarding__publish">
                <strong>다음 달 초</strong> 첫 소비 판결문이 열려요
            </p>
        </div>
    </section>
</template>

<style scoped>
.report-onboarding {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding-top: clamp(var(--tt-space-5), 4.5vh, 42px);
    text-align: center;
}
.report-onboarding__visual {
    position: relative;
    display: grid;
    width: 166px;
    height: 166px;
    margin-bottom: var(--tt-space-5);
    place-items: center;
}
.report-onboarding__visual::before {
    position: absolute;
    inset: 9px;
    content: '';
    background: linear-gradient(145deg, var(--tt-bg), var(--tt-accent-subtle));
    border: 1px solid var(--tt-accent-subtle-border);
    border-radius: var(--tt-radius-full);
}
.report-onboarding__orbit {
    position: absolute;
    inset: 0;
    border: 1px dashed color-mix(in srgb, var(--tt-accent-strong) 26%, transparent);
    border-radius: var(--tt-radius-full);
    animation: onboarding-orbit 24s linear infinite;
}
.report-onboarding__paper {
    position: absolute;
    z-index: 1;
    display: grid;
    width: 34px;
    height: 38px;
    background: var(--tt-bg);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-md);
    box-shadow: var(--tt-elevation-2);
    place-items: center;
}
.report-onboarding__paper::before {
    width: 18px;
    height: 2px;
    content: '';
    background: var(--tt-border-strong);
    border-radius: var(--tt-radius-full);
    box-shadow: 0 6px 0 var(--tt-border);
}
.report-onboarding__paper--left {
    top: 43px;
    left: -1px;
    transform: rotate(-9deg);
}
.report-onboarding__paper--right {
    right: -1px;
    bottom: 25px;
    transform: rotate(8deg);
}
.report-onboarding__visual img {
    position: relative;
    z-index: 2;
    width: 142px;
    height: 142px;
    object-fit: contain;
    filter: drop-shadow(0 12px 12px color-mix(in srgb, var(--tt-text) 12%, transparent));
    animation: onboarding-breathe 3.2s ease-in-out infinite;
}
.report-onboarding__status {
    display: inline-flex;
    align-items: center;
    gap: var(--tt-space-2);
    min-height: 29px;
    margin-bottom: var(--tt-space-3);
    padding: var(--tt-space-1) var(--tt-space-3);
    color: var(--tt-accent-strong);
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-black);
    background: var(--tt-accent-subtle);
    border-radius: var(--tt-radius-full);
}
.report-onboarding__status::before {
    width: 7px;
    height: 7px;
    content: '';
    background: var(--tt-accent);
    border-radius: var(--tt-radius-full);
    animation: onboarding-pulse 1.8s ease-out infinite;
}
.report-onboarding h2 {
    font-size: clamp(20px, 5.1vw, 23px);
    line-height: 1.4;
    font-weight: var(--tt-fw-black);
    letter-spacing: -0.055em;
    word-break: keep-all;
}
.report-onboarding__copy {
    margin-top: var(--tt-space-2);
    color: var(--tt-text-muted);
    font-size: var(--tt-fs-body);
    line-height: 1.65;
    word-break: keep-all;
}
.report-onboarding__guide {
    width: 100%;
    margin-top: clamp(var(--tt-space-6), 4.5vh, 38px);
    padding: var(--tt-space-4);
    text-align: left;
    background: var(--tt-bg);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-xl);
    box-shadow: var(--tt-elevation-1);
}
.report-onboarding__guide-title {
    font-weight: var(--tt-fw-black);
}
.report-onboarding__guide-title::before {
    display: inline-grid;
    width: 24px;
    height: 24px;
    margin-right: var(--tt-space-2);
    content: 'i';
    color: var(--tt-accent-strong);
    background: var(--tt-accent-subtle);
    border-radius: var(--tt-radius-full);
    place-items: center;
}
.report-onboarding ul {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: var(--tt-space-2);
    margin-top: var(--tt-space-3);
}
.report-onboarding li {
    display: grid;
    min-height: 56px;
    padding: var(--tt-space-2);
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-muted);
    text-align: center;
    background: var(--tt-bg-subtle);
    border-radius: var(--tt-radius-lg);
    place-items: center;
}
.report-onboarding__publish {
    margin-top: var(--tt-space-3);
    font-size: var(--tt-fs-caption);
    color: var(--tt-text-muted);
    text-align: center;
}
.report-onboarding__publish strong {
    margin-right: var(--tt-space-2);
    color: var(--tt-text);
}
@keyframes onboarding-breathe {
    0%,
    100% {
        transform: translateY(0) rotate(-1deg);
    }
    50% {
        transform: translateY(-6px) rotate(1deg);
    }
}
@keyframes onboarding-pulse {
    0% {
        box-shadow: 0 0 0 0 color-mix(in srgb, var(--tt-accent) 38%, transparent);
    }
    70%,
    100% {
        box-shadow: 0 0 0 7px transparent;
    }
}
@keyframes onboarding-orbit {
    to {
        transform: rotate(360deg);
    }
}
@media (prefers-reduced-motion: reduce) {
    .report-onboarding__orbit,
    .report-onboarding__status::before,
    .report-onboarding__visual img {
        animation: none;
    }
}
@media (max-width: 360px) {
    .report-onboarding {
        padding-top: var(--tt-space-4);
    }
    .report-onboarding__visual {
        width: 150px;
        height: 150px;
        margin-bottom: var(--tt-space-4);
    }
    .report-onboarding__visual img {
        width: 128px;
        height: 128px;
    }
}
</style>
