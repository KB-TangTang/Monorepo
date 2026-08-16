<script setup>
import { computed } from 'vue';
import { useRouter } from 'vue-router';

/*
 * 판결 확정 카드.
 *
 * 기록 카드(Ink)와 반대로 흰 종이 + 이중 괘선을 쓴다. 판결문 화면(VerdictDocumentCard)이
 * 이미 쓰는 어법이라 같은 사건을 두 화면에서 봐도 같은 물건으로 읽힌다.
 *
 * 도장은 유죄/무죄가 아니라 중립 "판결" 이다 — 서버가 주는 값은 요약 문구 한 줄뿐이고
 * 승패를 알려주지 않는다. 결과가 이벤트에 실리면(#169~#172) 그때 기존 유죄·무죄 도장 자산으로
 * 바꾼다. 지금 색을 고르면 무죄 판결에 붉은 도장이 찍힌다.
 */
const props = defineProps({
    message: { type: Object, required: true },
});

const router = useRouter();

const timeLabel = computed(() => {
    const d = props.message.sentAt;
    if (!d) return '';
    return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
});

function openVerdict() {
    router.push(props.message.deepLink);
}
</script>

<template>
    <article class="verdict-card">
        <div class="verdict-card__rule" aria-hidden="true"></div>

        <div class="verdict-card__main">
            <div class="verdict-card__stamp" aria-hidden="true">
                <span class="verdict-card__stamp-label">판결</span>
                <span class="verdict-card__stamp-sub">VERDICT</span>
            </div>

            <div class="verdict-card__text">
                <h3 class="verdict-card__title">판결이 확정됐습니다</h3>
                <p class="verdict-card__body">{{ message.content }}</p>
            </div>
        </div>

        <div class="verdict-card__foot">
            <span class="verdict-card__time">
                <template v-if="timeLabel">{{ timeLabel }} 확정</template>
                <template v-else>확정</template>
            </span>
            <button v-if="message.deepLink" class="verdict-card__cta" @click="openVerdict">
                판결문 보기
                <span aria-hidden="true">›</span>
            </button>
        </div>
    </article>
</template>

<style scoped>
.verdict-card {
    position: relative;
    background: var(--tt-bg);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-xl);
    padding: var(--tt-space-4);
    box-shadow: var(--tt-elevation-2);
    animation: verdict-enter 0.28s ease-out both;
}

/* 이중 괘선 — 판결문 카드와 같은 어법 */
.verdict-card__rule {
    position: absolute;
    inset: 5px;
    border: 1px solid var(--tt-border-light);
    border-radius: 17px;
    pointer-events: none;
}

.verdict-card__main {
    display: flex;
    align-items: center;
    gap: var(--tt-space-3);
}

.verdict-card__stamp {
    width: 62px;
    height: 62px;
    border-radius: 50%;
    border: 2px solid var(--tt-primary);
    color: var(--tt-primary);
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 1px;
    flex: none;
    transform: rotate(-8deg);
}

.verdict-card__stamp-label {
    font-size: var(--tt-fs-label);
    font-weight: var(--tt-fw-black);
    letter-spacing: 0.06em;
}

.verdict-card__stamp-sub {
    font-family: var(--tt-font-mono);
    font-size: 8px;
    letter-spacing: 0.12em;
    opacity: 0.7;
}

.verdict-card__text {
    min-width: 0;
}

.verdict-card__title {
    margin: 0 0 3px;
    font-size: var(--tt-fs-label);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
    letter-spacing: -0.01em;
}

.verdict-card__body {
    margin: 0;
    font-size: var(--tt-fs-body);
    line-height: 1.5;
    color: var(--tt-text-body);
    word-break: break-word;
}

.verdict-card__foot {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: var(--tt-space-3);
    margin-top: var(--tt-space-3);
    padding-top: var(--tt-space-3);
    border-top: 1px dashed var(--tt-border-divider);
}

.verdict-card__time {
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-overline);
    color: var(--tt-text-hint);
    letter-spacing: 0.04em;
}

.verdict-card__cta {
    border: none;
    background: none;
    cursor: pointer;
    display: inline-flex;
    align-items: center;
    gap: 3px;
    padding: 2px;
    font-family: inherit;
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-black);
    color: var(--tt-info);
}

.verdict-card__cta:focus-visible {
    outline: 2px solid var(--tt-info);
    outline-offset: 2px;
    border-radius: var(--tt-radius-xs);
}

@keyframes verdict-enter {
    from {
        opacity: 0;
        transform: translateY(8px);
    }
}

@media (prefers-reduced-motion: reduce) {
    .verdict-card {
        animation: none;
    }

    .verdict-card__stamp {
        transform: none;
    }
}
</style>
