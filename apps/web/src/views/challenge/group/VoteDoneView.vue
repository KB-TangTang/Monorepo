<!--
  투표 플로우 ③ — 투표 완료 화면.
  제출 인장 + 투표 현황 + 돌아가기.
  /group-challenges/:id/vote/:indictmentId/done 으로 라우팅된다.
-->
<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import BaseButton from '@/components/common/BaseButton.vue';
import { MOCK_VOTE_DETAIL } from '@/fixtures/groupChallengeDetail';
import okayImg from '@/assets/images/emotions/41_okay.png';

const route = useRoute();
const router = useRouter();

/* ── 목데이터 ─────────────────────────── */
const detail = ref(null);

onMounted(() => {
    const id = Number(route.params.indictmentId);
    detail.value = MOCK_VOTE_DETAIL[id] ?? null;
    if (!detail.value) {
        router.replace({ name: 'groupChallenge' });
    }
});

/* 투표 제출 후이므로 현재 유저(나)도 투표 완료 상태로 표시 */
const votedCount = computed(() => {
    if (!detail.value) return 0;
    return detail.value.voteCount + 1; // 내 투표 반영
});

function goBack() {
    router.replace({
        name: 'groupChallengeDetail',
        params: { id: route.params.id },
    });
}
</script>

<template>
    <div v-if="detail" class="vote-done">
        <!-- ===== 라이트 헤더 ===== -->
        <header class="vote-done__header">
            <div class="vote-done__nav">
                <button
                    type="button"
                    class="vote-done__back"
                    aria-label="뒤로가기"
                    @click="goBack"
                >
                    <svg viewBox="0 0 9 16" fill="none" aria-hidden="true">
                        <path
                            d="M8 1L1 8l7 7"
                            stroke="currentColor"
                            stroke-width="2.2"
                            stroke-linecap="round"
                            stroke-linejoin="round"
                        />
                    </svg>
                </button>
                <span class="vote-done__title">투표 완료</span>
            </div>
        </header>

        <!-- ===== 본문 ===== -->
        <div class="vote-done__body">
            <!-- 마스코트 + 인장 -->
            <div class="vote-done__hero">
                <div class="vote-done__glow"></div>
                <img
                    :src="okayImg"
                    alt="탕이 제출 완료"
                    class="vote-done__mascot"
                >
                <div class="vote-done__stamp">
                    <div class="vote-done__stamp-inner">제출</div>
                </div>
            </div>

            <!-- 안내 문구 -->
            <h2 class="vote-done__heading">판결 의견을<br>제출했어요!</h2>
            <p class="vote-done__desc">모든 멤버의 투표가 끝나면 결과가 공개돼요.</p>

            <!-- 투표 현황 카드 -->
            <div class="vote-done__card">
                <div class="vote-done__card-top">
                    <div>
                        <div class="vote-done__card-tag">투표 현황</div>
                        <div class="vote-done__card-count">
                            <span class="vote-done__card-big">{{ votedCount }}</span>
                            <span class="vote-done__card-total">/ {{ detail.totalVoters }}명 제출</span>
                        </div>
                    </div>
                    <span class="vote-done__card-badge">익명 집계 · 피고 제외</span>
                </div>

                <div class="vote-done__card-divider"></div>

                <div class="vote-done__grid">
                    <div
                        v-for="m in detail.members"
                        :key="m.userId"
                        class="vote-done__member"
                    >
                        <!-- 투표 완료: 색상 아바타 -->
                        <div
                            v-if="m.voted || m.userId === 1"
                            class="vote-done__avatar"
                            :style="{ background: m.avatarColor }"
                        >
                            {{ m.initial }}
                        </div>
                        <!-- 미투표: 점선 아바타 -->
                        <div v-else class="vote-done__avatar vote-done__avatar--pending">
                            {{ m.initial }}
                        </div>

                        <span
                            class="vote-done__name"
                            :class="{ 'vote-done__name--pending': !m.voted && m.userId !== 1 }"
                        >
                            {{ m.nickname }}
                        </span>

                        <!-- 도장 (투표 완료) -->
                        <svg
                            v-if="m.voted || m.userId === 1"
                            class="vote-done__seal"
                            :style="{ transform: `rotate(${(m.userId * 5 - 10)}deg)` }"
                            width="30"
                            height="30"
                            viewBox="0 0 30 30"
                        >
                            <circle cx="15" cy="15" r="12.3" fill="none" stroke="#C24B31" stroke-width="2.4" />
                            <path d="M15 3.4V26.6M15 15.4l8.4 8.4" stroke="#C24B31" stroke-width="2.4" />
                        </svg>
                        <!-- 미투표 점선 원 -->
                        <div v-else class="vote-done__seal-empty"></div>
                    </div>
                </div>
            </div>
        </div>

        <!-- ===== 하단 버튼 ===== -->
        <div class="vote-done__footer">
            <BaseButton
                variant="dark"
                size="lg"
                block
                @click="goBack"
            >
                그룹 화면으로 돌아가기
            </BaseButton>
        </div>
    </div>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Noto+Serif+KR:wght@400;600;700&display=swap');

.vote-done {
    min-height: 100vh;
    background: var(--tt-bg-subtle);
    display: flex;
    flex-direction: column;
}

/* ── 라이트 헤더 ─────────────────────── */
.vote-done__header {
    flex: none;
    background: var(--tt-bg-subtle);
    padding-bottom: 10px;
}

.vote-done__nav {
    display: flex;
    align-items: center;
    gap: 11px;
    padding: 4px var(--tt-screen-padding) 0;
}

.vote-done__back {
    display: grid;
    place-items: center;
    width: 28px;
    height: 40px;
    padding: 0;
    color: var(--tt-text);
    background: transparent;
    border: 0;
    cursor: pointer;
}

.vote-done__back svg {
    width: 9px;
    height: 16px;
}

.vote-done__title {
    font-size: 14.5px;
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
}

/* ── 본문 ─────────────────────────────── */
.vote-done__body {
    flex: 1;
    min-height: 0;
    overflow-y: auto;
    padding: 0 var(--tt-screen-padding);
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 14px;
}

/* 마스코트 + 인장 */
.vote-done__hero {
    position: relative;
    width: 112px;
    height: 112px;
    display: flex;
    align-items: center;
    justify-content: center;
}

.vote-done__glow {
    position: absolute;
    inset: 0;
    border-radius: 50%;
    background: radial-gradient(circle, rgba(194, 75, 49, 0.14) 0%, rgba(194, 75, 49, 0) 70%);
}

.vote-done__mascot {
    position: relative;
    width: 88px;
    height: 88px;
    object-fit: contain;
    filter: drop-shadow(0 8px 14px rgba(35, 40, 66, 0.16));
}

.vote-done__stamp {
    position: absolute;
    right: -6px;
    bottom: -2px;
    width: 56px;
    height: 56px;
    border-radius: 50%;
    border: 2.5px solid rgba(194, 75, 49, 0.78);
    background: rgba(255, 255, 255, 0.74);
    color: rgba(194, 75, 49, 0.88);
    transform: rotate(-13deg);
    display: flex;
    align-items: center;
    justify-content: center;
}

.vote-done__stamp-inner {
    width: 46px;
    height: 46px;
    border-radius: 50%;
    border: 1px solid rgba(194, 75, 49, 0.55);
    display: flex;
    align-items: center;
    justify-content: center;
    font-family: 'Noto Serif KR', serif;
    font-size: 15.5px;
    font-weight: 700;
    letter-spacing: 0.02em;
    line-height: 1;
}

/* 안내 */
.vote-done__heading {
    text-align: center;
    font-size: 24px;
    font-weight: var(--tt-fw-black);
    letter-spacing: -0.01em;
    line-height: 1.3;
}

.vote-done__desc {
    font-size: 13px;
    color: var(--tt-text-hint, #8A8FA3);
    line-height: 1.55;
    font-weight: var(--tt-fw-medium);
}

/* ── 투표 현황 카드 ──────────────────── */
.vote-done__card {
    width: 100%;
    background: var(--tt-white);
    border: 1px solid var(--tt-border);
    border-radius: 20px;
    padding: 16px;
    box-shadow: 0 10px 26px -14px rgba(35, 40, 66, 0.14);
    margin-top: 4px;
}

.vote-done__card-top {
    display: flex;
    align-items: flex-end;
    justify-content: space-between;
}

.vote-done__card-tag {
    font-size: 11px;
    font-weight: var(--tt-fw-black);
    letter-spacing: 0.12em;
    color: var(--tt-text-hint, #A6A9B6);
}

.vote-done__card-count {
    display: flex;
    align-items: baseline;
    gap: 4px;
    margin-top: 5px;
}

.vote-done__card-big {
    font-size: 22px;
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
    letter-spacing: -0.01em;
}

.vote-done__card-total {
    font-size: 13px;
    font-weight: var(--tt-fw-black);
    color: var(--tt-text-hint, #8A8FA3);
}

.vote-done__card-badge {
    background: var(--tt-bg-fill, #EFF1F5);
    color: var(--tt-text-hint, #8A8FA3);
    font-size: 10.5px;
    font-weight: var(--tt-fw-black);
    padding: 5px 10px;
    border-radius: var(--tt-radius-full);
}

.vote-done__card-divider {
    height: 1px;
    background: #F0F1F5;
    margin: 14px 0 12px;
}

/* ── 멤버 그리드 ─────────────────────── */
.vote-done__grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 9px 22px;
}

.vote-done__member {
    display: flex;
    align-items: center;
    gap: 8px;
}

.vote-done__avatar {
    width: 26px;
    height: 26px;
    border-radius: 50%;
    color: var(--tt-white);
    font-size: 11px;
    font-weight: var(--tt-fw-black);
    display: flex;
    align-items: center;
    justify-content: center;
    flex: none;
}

.vote-done__avatar--pending {
    background: transparent;
    border: 1.5px dashed #D8D3C8;
    color: #C0C3CE;
}

.vote-done__name {
    font-size: 12px;
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text);
    flex: 1;
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.vote-done__name--pending {
    color: var(--tt-text-hint, #A6A9B6);
}

.vote-done__seal {
    flex: none;
}

.vote-done__seal-empty {
    width: 30px;
    height: 30px;
    border-radius: 50%;
    border: 1.5px dashed #D8D3C8;
    flex: none;
}

/* ── 하단 버튼 ───────────────────────── */
.vote-done__footer {
    flex: none;
    padding: 12px var(--tt-screen-padding) 18px;
    background: var(--tt-bg-subtle);
}
</style>
