<script setup>
import tangiSignature from '@/assets/images/signatures/tangi-signature-face.png';

defineProps({
    ranking: { type: Object, required: true },
    periodLabel: { type: String, required: true },
    judgmentDate: { type: String, required: true },
    title: { type: String, required: true },
    ratio: {
        type: String,
        default: 'portrait',
        validator: (value) => ['portrait', 'square', 'story'].includes(value),
    },
});
</script>

<template>
    <article class="honor-certificate" :class="`honor-certificate--${ratio}`">
        <header class="honor-certificate__court">HONOR COURT · {{ periodLabel }}</header>

        <div class="honor-certificate__content">
            <div class="honor-certificate__heading">
                <h2>{{ title }}</h2>
                <p>이달의 소비 재판, 판사 자격 갱신 완료</p>
            </div>

            <section class="honor-certificate__rank">
                <small>전체 명예 법정 순위</small>
                <strong>
                    {{ ranking.myRank.toLocaleString('ko-KR') }}<em>위</em>
                    <span>/ {{ ranking.totalUsers.toLocaleString('ko-KR') }}명</span>
                </strong>
                <b>상위<br />{{ ranking.percentile }}%</b>
            </section>

            <dl class="honor-certificate__stats">
                <div>
                    <dd>{{ ranking.score.toLocaleString('ko-KR') }}</dd>
                    <dt>누적 점수</dt>
                </div>
                <div>
                    <dd>{{ ranking.streakDays }}<small>일</small></dd>
                    <dt>연속</dt>
                </div>
                <div>
                    <dd>{{ ranking.bestStreakDays }}<small>일</small></dd>
                    <dt>최고</dt>
                </div>
            </dl>

            <footer class="honor-certificate__verdict">
                <p>
                    피고인 서명 · 나<br /><strong>{{ judgmentDate }} 판결 확정</strong>
                </p>
                <span class="honor-certificate__signature">
                    <img :src="tangiSignature" alt="탕이 서명" />
                </span>
            </footer>

            <div class="honor-certificate__meta">
                <span>탕탕 · 명예 법정 · {{ periodLabel }}</span>
            </div>
        </div>
    </article>
</template>

<style scoped src="./PersonalHonorCertificate.css"></style>
