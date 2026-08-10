<script setup>
defineProps({
    ranking: { type: Object, required: true },
});
</script>

<template>
    <section class="ranking-summary-card">
        <small>이번 달 내 순위</small>
        <strong>상위 {{ ranking.percentile }}%</strong>
        <p>
            누적 {{ ranking.score.toLocaleString('ko-KR') }}점 · 전체
            {{ ranking.totalUsers.toLocaleString('ko-KR') }}명 중
            {{ ranking.myRank.toLocaleString('ko-KR') }}위
        </p>

        <div class="ranking-summary-card__progress" aria-hidden="true">
            <span :style="{ width: `${100 - ranking.percentile}%` }"></span>
        </div>
        <div class="ranking-summary-card__progress-label">
            <span>하위</span>
            <span>상위</span>
        </div>
    </section>
</template>

<style scoped src="./PersonalRankingSummaryCard.css"></style>
