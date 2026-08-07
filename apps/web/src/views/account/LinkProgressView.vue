<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue';
import { onBeforeRouteLeave } from 'vue-router';
import { storeToRefs } from 'pinia';
import BaseButton from '@/components/common/BaseButton.vue';
import LinkProgressRow from '@/components/account/LinkProgressRow.vue';
import { useAccountStore } from '@/stores/account';
import { PROGRESS_STATUS, isPollExpired } from '@/utils/account';

const store = useAccountStore();
const { progressInstitutions, progressPercent, progressDone, error } = storeToRefs(store);

const failed = ref(false);
let timer = null;
let startedAt = null;

/** 지금 조회 중인 기관. 진행률 옆에 이름을 띄워 "멈춘 게 아니다"를 알린다. */
const currentName = computed(() => {
    const fetching = progressInstitutions.value.find(
        (item) => item.status === PROGRESS_STATUS.FETCHING,
    );
    return fetching ? fetching.name : '';
});

const doneCount = computed(
    () =>
        progressInstitutions.value.filter(
            (item) =>
                item.status === PROGRESS_STATUS.DONE || item.status === PROGRESS_STATUS.FAILED,
        ).length,
);

function stop() {
    if (timer) {
        clearInterval(timer);
        timer = null;
    }
}

async function tick() {
    if (isPollExpired(startedAt, new Date())) {
        stop();
        failed.value = true;
        return;
    }
    try {
        const result = await store.checkProgress();
        if (result.done) {
            stop();
            store.goNextStep('progress');
        }
    } catch (err) {
        stop();
        failed.value = true;
        error.value = err.message;
    }
}

function start() {
    stop();
    failed.value = false;
    error.value = '';
    startedAt = new Date();
    timer = setInterval(tick, 1000);
    tick();
}

/*
 * 진행 중 이탈을 막는다. 중간에 나가면 connectionId 만 남고 화면이 어긋난다.
 * 실패했거나 조회가 끝난 뒤에는 자유롭게 나갈 수 있다.
 */
onBeforeRouteLeave(() => failed.value || progressDone.value);

onMounted(start);
onUnmounted(stop);
</script>

<template>
    <div class="link-progress">
        <header class="link-progress__head">
            <p class="link-progress__nav">계좌 연결</p>
            <h1 class="link-progress__title">거래내역을<br />가져오고 있어요</h1>
            <p class="link-progress__desc">최대 1분 걸릴 수 있어요</p>
        </header>

        <!-- 진행률. 이 화면에서 유일하게 큰 수치라 numeric 스케일을 쓴다. -->
        <section class="link-progress__meter">
            <p class="link-progress__percent">
                {{ progressPercent }}<span class="link-progress__unit">%</span>
            </p>
            <div
                class="link-progress__bar"
                role="progressbar"
                :aria-valuenow="progressPercent"
                aria-valuemin="0"
                aria-valuemax="100"
            >
                <span class="link-progress__bar-fill" :style="{ width: progressPercent + '%' }" />
            </div>
            <p class="link-progress__count">
                <template v-if="currentName">{{ currentName }} 조회 중</template>
                <template v-else>기관 {{ doneCount }} / {{ progressInstitutions.length }}</template>
            </p>
        </section>

        <ul class="link-progress__list">
            <LinkProgressRow
                v-for="item in progressInstitutions"
                :key="item.code"
                :name="item.name"
                :status="item.status"
            />
        </ul>

        <p class="link-progress__notice">
            <strong>기관이 많으면 조금 더 걸려요.</strong> 실패한 기관은 연결 계좌 관리에서 다시
            시도할 수 있어요.
        </p>

        <div v-if="failed" class="link-progress__failed">
            <p class="link-progress__failed-text">
                {{ error || '연결이 지연되고 있어요. 다시 시도해 주세요.' }}
            </p>
            <BaseButton variant="dark" block size="lg" @click="start">다시 시도</BaseButton>
        </div>
    </div>
</template>

<style scoped>
.link-progress {
    position: relative;
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-5);
    min-height: 100vh;
    padding: var(--tt-space-6) var(--tt-space-5) var(--tt-space-8);
    background: var(--tt-bg-subtle);
    overflow: hidden;
}

.link-progress__head {
    position: relative;
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-2);
}

.link-progress__nav {
    margin: 0;
    font-size: var(--tt-fs-section);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text);
}

.link-progress__title {
    margin: var(--tt-space-2) 0 0;
    font-size: var(--tt-fs-title);
    font-weight: var(--tt-fw-black);
    line-height: var(--tt-lh-tight);
    color: var(--tt-text);
}

.link-progress__desc {
    margin: 0;
    font-size: var(--tt-fs-caption);
    color: var(--tt-text-muted);
}

.link-progress__meter {
    position: relative;
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-2);
}

.link-progress__percent {
    margin: 0;
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-numeric);
    font-weight: var(--tt-fw-black);
    line-height: var(--tt-lh-tight);
    color: var(--tt-primary);
}

.link-progress__unit {
    font-size: var(--tt-fs-section);
}

.link-progress__bar {
    height: 6px;
    border-radius: var(--tt-radius-full);
    background: var(--tt-primary-subtle);
    overflow: hidden;
}

.link-progress__bar-fill {
    display: block;
    height: 100%;
    border-radius: var(--tt-radius-full);
    background: var(--tt-primary);
    transition: width 0.4s ease;
}

@media (prefers-reduced-motion: reduce) {
    .link-progress__bar-fill {
        transition: none;
    }
}

.link-progress__count {
    margin: 0;
    font-size: var(--tt-fs-caption);
    color: var(--tt-text-muted);
}

/* 목록은 흰 카드 하나. 행 사이는 줄무늬로 나눈다 (「동기화 상태」 화면 방식). */
.link-progress__list {
    position: relative;
    margin: 0;
    padding: 0;
    border-radius: var(--tt-radius-lg);
    background: var(--tt-bg);
    box-shadow: var(--tt-elevation-1);
    list-style: none;
    overflow: hidden;
}

.link-progress__list :deep(li:nth-child(even)) {
    background: var(--tt-surface-row);
}

.link-progress__notice {
    position: relative;
    margin: 0;
    padding: var(--tt-space-4);
    border-radius: var(--tt-radius-md);
    background: var(--tt-primary-subtle);
    font-size: var(--tt-fs-caption);
    line-height: var(--tt-lh-normal);
    color: var(--tt-text-muted);
}

.link-progress__notice strong {
    color: var(--tt-primary);
}

.link-progress__failed {
    position: relative;
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-3);
    margin-top: auto;
}

.link-progress__failed-text {
    margin: 0;
    font-size: var(--tt-fs-caption);
    color: var(--tt-danger);
}
</style>
