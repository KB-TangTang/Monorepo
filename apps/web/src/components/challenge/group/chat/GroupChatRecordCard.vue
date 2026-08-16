<script setup>
import { computed } from 'vue';
import { useRouter } from 'vue-router';

/*
 * 재판 기록 카드 — 적발 · 개시 · 변론.
 *
 * 대화 흐름 안에서 이것만 어두운 면(Ink)을 쓴다. 참여자 말풍선과 같은 종이 위에 있으면
 * "봇이 한마디 거들었다" 로 읽히는데, 실제로는 재판 기록이 방에 들어온 순간이다.
 *
 * 제목은 systemType 이 정한다. 서버가 주는 content 는 본문 한 줄뿐이라, 문구에서 제목을
 * 잘라내려면 파싱이 필요하고 문구가 바뀌는 순간 깨진다.
 */
const props = defineProps({
    message: { type: Object, required: true },
});

const router = useRouter();

const TITLES = {
    VIOLATION_DETECTED: '소비가 적발됐습니다',
    TRIAL_OPENED: '재판이 개시됐습니다',
    DEFENSE_REGISTERED: '변론이 등록됐습니다',
};

const title = computed(() => TITLES[props.message.systemType] ?? '재판 기록');

/* 적발은 아직 재판 전이라 "재판 보러가기" 가 아니라 사건을 여는 표현을 쓴다 */
const ctaLabel = computed(() =>
    props.message.systemType === 'VIOLATION_DETECTED' ? '사건 보러가기' : '재판 보러가기',
);

const timeLabel = computed(() => {
    const d = props.message.sentAt;
    if (!d) return '';
    return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
});

function openTrial() {
    router.push(props.message.deepLink);
}
</script>

<template>
    <article class="record-card">
        <div class="record-card__head">
            <span class="record-card__badge" aria-hidden="true">
                <svg width="16" height="16" viewBox="0 0 20 20" fill="none">
                    <path
                        d="M3 17h8M6.5 3.5l5 5M4.7 7.1l3.4-3.4M11.6 8.6 8.2 12M13 5.5 16.5 9M12.2 4.3l4.5 4.5"
                        stroke="currentColor"
                        stroke-width="1.6"
                        stroke-linecap="round"
                        stroke-linejoin="round"
                    />
                </svg>
            </span>
            <h3 class="record-card__title">{{ title }}</h3>
        </div>

        <p class="record-card__body">{{ message.content }}</p>

        <div class="record-card__foot">
            <span v-if="message.caseNo" class="record-card__case">사건 {{ message.caseNo }}</span>
            <span v-else class="record-card__case">{{ timeLabel }}</span>

            <button v-if="message.deepLink" class="record-card__cta" @click="openTrial">
                {{ ctaLabel }}
                <span aria-hidden="true">›</span>
            </button>
        </div>
    </article>
</template>

<style scoped>
.record-card {
    background: var(--tt-surface-inverse);
    border-radius: var(--tt-radius-xl);
    padding: var(--tt-space-4) var(--tt-space-4) var(--tt-space-3);
    box-shadow: var(--tt-elevation-4);
    animation: record-enter 0.28s ease-out both;
}

.record-card__head {
    display: flex;
    align-items: center;
    gap: var(--tt-space-2);
    margin-bottom: 10px;
}

.record-card__badge {
    width: 30px;
    height: 30px;
    border-radius: var(--tt-radius-sm);
    background: rgba(245, 185, 33, 0.16);
    color: var(--tt-accent-bright);
    display: flex;
    align-items: center;
    justify-content: center;
    flex: none;
}

.record-card__title {
    margin: 0;
    font-size: var(--tt-fs-label);
    font-weight: var(--tt-fw-black);
    color: var(--tt-accent-bright);
    letter-spacing: -0.01em;
}

.record-card__body {
    margin: 0;
    font-size: var(--tt-fs-body);
    line-height: var(--tt-lh-normal);
    color: rgba(255, 255, 255, 0.86);
    word-break: break-word;
}

.record-card__foot {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: var(--tt-space-3);
    margin-top: var(--tt-space-4);
}

.record-card__case {
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-overline);
    letter-spacing: 0.04em;
    color: rgba(255, 255, 255, 0.42);
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.record-card__cta {
    flex: none;
    display: inline-flex;
    align-items: center;
    gap: 4px;
    border: none;
    cursor: pointer;
    background: var(--tt-accent);
    color: var(--tt-ink);
    font-family: inherit;
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-black);
    padding: 9px 14px;
    border-radius: var(--tt-radius-md);
}

.record-card__cta:active {
    transform: translateY(1px);
}

.record-card__cta:focus-visible {
    outline: 2px solid var(--tt-accent-bright);
    outline-offset: 2px;
}

@keyframes record-enter {
    from {
        opacity: 0;
        transform: translateY(8px);
    }
}

@media (prefers-reduced-motion: reduce) {
    .record-card {
        animation: none;
    }
}
</style>
