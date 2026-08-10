<script setup>
import { useRouter } from 'vue-router';
import BaseButton from '@/components/common/BaseButton.vue';
import BaseCard from '@/components/common/BaseCard.vue';
import DefenseCourtHeader from '@/components/challenge/group/DefenseCourtHeader.vue';

const router = useRouter();
</script>

<template>
    <main class="detail-page">
        <DefenseCourtHeader title="재판 세부 정보" />
        <section class="detail-page__body">
            <BaseCard padding="lg" class="detail-page__card">
                <span class="detail-page__label">COMING SOON</span>
                <h1>재판 세부 정보 안내</h1>
                <p>상세 재판 기록은 준비 중이에요.</p>
            </BaseCard>
        </section>
        <footer class="detail-page__footer">
            <BaseButton variant="dark" size="lg" block @click="router.back()"
                >판결 결과로 돌아가기</BaseButton
            >
        </footer>
    </main>
</template>

<style scoped>
.detail-page {
    min-height: 100dvh;
    display: flex;
    flex-direction: column;
    background: var(--tt-bg-subtle);
}
.detail-page__body {
    display: flex;
    flex: 1;
    align-items: center;
    padding: var(--tt-space-5);
}
.detail-page__card {
    text-align: center;
}
.detail-page__label {
    color: var(--tt-primary);
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-mono-chip);
    font-weight: var(--tt-fw-bold);
    letter-spacing: 0.15em;
}
.detail-page__card h1 {
    margin-top: var(--tt-space-3);
    font-size: var(--tt-fs-title);
    font-weight: var(--tt-fw-black);
    letter-spacing: -0.01em;
}
.detail-page__card p {
    margin-top: var(--tt-space-3);
    color: var(--tt-text-muted);
    font-size: var(--tt-fs-body);
}
.detail-page__footer {
    padding: var(--tt-space-3) var(--tt-space-5) var(--tt-space-6);
}
</style>
