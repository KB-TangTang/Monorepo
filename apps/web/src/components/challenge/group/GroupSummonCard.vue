<script setup>
defineProps({
    inviteCode: { type: String, required: true },
    variant: {
        type: String,
        default: 'solid',
        validator: (v) => ['solid', 'outline'].includes(v),
    },
});
</script>

<template>
    <div class="gsc-card" :class="`gsc-card--${variant}`">
        <!-- 좌우 펀치홀 -->
        <div class="gsc-punch gsc-punch--left" />
        <div class="gsc-punch gsc-punch--right" />

        <!-- 상단: SUMMONS + 소환장 -->
        <div class="gsc-top">
            <span class="gsc-overline">SUMMONS</span>
            <span class="gsc-label">소환장</span>
        </div>

        <!-- 점선 구분선 -->
        <div class="gsc-divider" />

        <!-- 하단: 초대 코드 -->
        <div class="gsc-code-label">초대 코드</div>
        <div class="gsc-code">{{ inviteCode }}</div>
    </div>
</template>

<style scoped>
.gsc-card {
    position: relative;
    width: 100%;
    border-radius: var(--tt-radius-xl);
    padding: 24px 22px;
    overflow: hidden;
}

/* ── solid variant (FRAME 7) ─────────── */
.gsc-card--solid {
    background: var(--tt-info);
    box-shadow: 0 16px 34px -14px rgba(62, 99, 214, 0.55);
}

.gsc-card--solid .gsc-overline {
    color: #fff;
}

.gsc-card--solid .gsc-label {
    color: #C6D2F8;
}

.gsc-card--solid .gsc-divider {
    border-top: 1.5px dashed rgba(255, 255, 255, 0.4);
}

.gsc-card--solid .gsc-code-label {
    color: #C6D2F8;
}

.gsc-card--solid .gsc-code {
    color: #fff;
}

/* ── outline variant (FRAME 8) ────────── */
.gsc-card--outline {
    background: var(--tt-info-subtle);
    border: 1.5px solid #C9D6F5;
}

.gsc-card--outline .gsc-overline {
    color: var(--tt-info);
}

.gsc-card--outline .gsc-label {
    color: #8FA4E0;
}

.gsc-card--outline .gsc-divider {
    border-top: 1.5px dashed #B9C6F2;
}

.gsc-card--outline .gsc-code-label {
    color: #7E93D6;
}

.gsc-card--outline .gsc-code {
    color: #2C46A8;
}

/* ── 펀치홀 ──────────────────────────── */
.gsc-punch {
    position: absolute;
    top: 50%;
    transform: translateY(-50%);
    width: 22px;
    height: 22px;
    border-radius: 50%;
    background: var(--tt-bg-subtle);
}

.gsc-card--outline .gsc-punch {
    border: 1.5px solid #C9D6F5;
}

.gsc-punch--left {
    left: -11px;
}

.gsc-card--outline .gsc-punch--left {
    border-right: none;
}

.gsc-punch--right {
    right: -11px;
}

.gsc-card--outline .gsc-punch--right {
    border-left: none;
}

/* ── 내부 요소 ───────────────────────── */
.gsc-top {
    display: flex;
    align-items: center;
    justify-content: space-between;
}

.gsc-overline {
    font-size: 13px;
    font-weight: var(--tt-fw-black);
    letter-spacing: 0.18em;
}

.gsc-label {
    font-size: 10.5px;
    font-weight: var(--tt-fw-black);
    letter-spacing: 0.06em;
}

.gsc-divider {
    margin: 16px 0;
}

.gsc-code-label {
    font-size: 11px;
    font-weight: var(--tt-fw-bold);
    letter-spacing: 0.04em;
    text-align: center;
}

.gsc-code {
    font-family: var(--tt-font-mono);
    font-size: 30px;
    font-weight: var(--tt-fw-black);
    letter-spacing: 0.14em;
    margin-top: 6px;
    text-align: center;
}
</style>
