<script setup>
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import guiltyStampImage from '@/assets/images/judgment/guilty_stamp.png';
import innocentStampImage from '@/assets/images/judgment/innocent_stamp.png';
import { verdictMetaParts, verdictStampKind } from '@/utils/groupChat';

/*
 * 판결 확정 카드.
 *
 * 기록 카드(Ink)와 반대로 흰 종이 + 이중 괘선을 쓴다. 판결문 화면(VerdictDocumentCard)이
 * 이미 쓰는 어법이라 같은 사건을 두 화면에서 봐도 같은 물건으로 읽힌다.
 *
 * 도장은 서버가 주는 verdict.outcome 으로 고른다(이슈 #304). 문구에서 "유죄" 를 찾아 고르지
 * 않는다 — 문구를 한 글자만 고쳐도 무죄 판결에 붉은 도장이 찍힌다.
 *
 * **verdict 가 없는 메시지가 실제로 있다.** 이 필드가 생기기 전에 Redis 에 쌓인 판결 메시지가
 * 그렇다(TTL 이 남아 있는 동안 계속 조회된다). 그때는 예전처럼 중립 "판결" 도장을 찍는다.
 */
const props = defineProps({
    message: { type: Object, required: true },
});

const router = useRouter();

/* 표시 규칙은 utils/groupChat 에 있다 — 이 저장소에는 SFC 렌더링 테스트 하네스가 없다 */
const stampKind = computed(() => verdictStampKind(props.message.verdict));
const metaParts = computed(() => verdictMetaParts(props.message.verdict));

const stampImage = computed(() => {
    if (!stampKind.value) return null;
    return stampKind.value === 'GUILTY' ? guiltyStampImage : innocentStampImage;
});

const stampAlt = computed(() =>
    stampKind.value === 'GUILTY' ? '유죄 판결 인장' : '무죄 판결 인장',
);

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
    <article class="verdict-card" :class="stampKind ? `verdict-card--${stampKind}` : null">
        <div class="verdict-card__rule" aria-hidden="true"></div>

        <div class="verdict-card__main">
            <img
                v-if="stampImage"
                :src="stampImage"
                :alt="stampAlt"
                class="verdict-card__stamp-image"
            />
            <div v-else class="verdict-card__stamp" aria-hidden="true">
                <span class="verdict-card__stamp-label">판결</span>
                <span class="verdict-card__stamp-sub">VERDICT</span>
            </div>

            <div class="verdict-card__text">
                <h3 class="verdict-card__title">판결이 확정됐습니다</h3>
                <p class="verdict-card__body">{{ message.content }}</p>
                <p v-if="metaParts.length" class="verdict-card__meta">
                    {{ metaParts.join(' · ') }}
                </p>
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

/* 결과가 실린 판결 — 그려 만든 원 대신 인장 이미지를 쓴다(판결문 화면과 같은 자산) */
.verdict-card__stamp-image {
    width: 62px;
    height: 62px;
    flex: none;
    object-fit: contain;
    transform: rotate(-8deg);
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

/* 「투표 4:2 · 목숨 1 차감」 — 문구가 아니라 수치라서 등폭 글꼴을 쓴다 */
.verdict-card__meta {
    margin: 4px 0 0;
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-overline);
    letter-spacing: 0.04em;
    color: var(--tt-text-hint);
}

.verdict-card--GUILTY .verdict-card__meta {
    color: var(--tt-danger);
}

.verdict-card--INNOCENT .verdict-card__meta {
    color: var(--tt-success);
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

    .verdict-card__stamp,
    .verdict-card__stamp-image {
        transform: none;
    }
}
</style>
