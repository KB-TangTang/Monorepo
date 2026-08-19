<script setup>
import { computed } from 'vue';
import { formatIssuedDate } from '@/utils/groupInvite';

const props = defineProps({
    /** 소환장 본문에 들어갈 그룹 이름 */
    groupName: { type: String, default: '' },
    inviteCode: { type: String, required: true },
    /** 그룹 생성 시각(ISO-8601). 소환장 발부일로 찍는다. 없으면 발부일 줄을 감춘다 */
    issuedAt: { type: String, default: '' },
});

const issuedDate = computed(() => formatIssuedDate(props.issuedAt));
</script>

<template>
    <div class="gsc-paper">
        <div class="gsc-frame">
            <div class="gsc-court">탕탕 법정</div>
            <div class="gsc-title">소환장</div>

            <div class="gsc-rule">
                <span class="gsc-rule__line" />
                <span class="gsc-rule__diamond" />
                <span class="gsc-rule__line" />
            </div>

            <p class="gsc-body">
                귀하를 <b>{{ props.groupName }}</b> 그룹 법정의<br />배심원으로 소환합니다.
            </p>

            <div class="gsc-code-box">
                <div class="gsc-code-box__label">초대 코드</div>
                <div class="gsc-code-box__value">{{ props.inviteCode }}</div>
            </div>

            <div v-if="issuedDate" class="gsc-issued">{{ issuedDate }}<br />탕탕 지방법원</div>

            <div class="gsc-seal" aria-hidden="true"><span>소환</span></div>
        </div>
    </div>
</template>

<style scoped>
/* ── 종이 ────────────────────────────────── */
.gsc-paper {
    position: relative;
    width: 100%;
    background: var(--tt-doc-bg);
    border: 2px solid var(--tt-text);
    border-radius: 6px;
    padding: 6px;
    box-shadow: 0 18px 36px -14px rgba(35, 40, 66, 0.4);
    /* 반듯하게 두면 인쇄물이 아니라 카드로 보인다. 살짝 틀어 「올려 둔 서류」로 만든다 */
    transform: rotate(-1.2deg);
    animation: gsc-drop 0.7s cubic-bezier(0.22, 1, 0.36, 1) both;
}

.gsc-frame {
    border: 1px solid var(--tt-doc-frame);
    border-radius: 3px;
    padding: 20px 18px 18px;
    display: flex;
    flex-direction: column;
    align-items: center;
    text-align: center;
    position: relative;
}

/* ── 머리 ────────────────────────────────── */
.gsc-court {
    font-size: 9.5px;
    font-weight: var(--tt-fw-bold);
    letter-spacing: 0.34em;
    color: var(--tt-text-muted);
}

.gsc-title {
    font-family: var(--tt-font-serif);
    font-size: 27px;
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
    letter-spacing: 0.42em;
    /* 자간이 마지막 글자 뒤에도 붙어 오른쪽으로 밀린다. 같은 값만큼 들여써서 가운데를 맞춘다 */
    text-indent: 0.42em;
    margin-top: 8px;
}

/* ── 마름모 구분선 ───────────────────────── */
.gsc-rule {
    display: flex;
    align-items: center;
    gap: 8px;
    width: 100%;
    margin: 12px 0;
}

.gsc-rule__line {
    flex: 1;
    border-top: 1px solid var(--tt-doc-rule);
}

.gsc-rule__diamond {
    width: 5px;
    height: 5px;
    background: var(--tt-text);
    transform: rotate(45deg);
}

/* ── 본문 ────────────────────────────────── */
.gsc-body {
    font-family: var(--tt-font-serif);
    font-size: 12.5px;
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-body);
    line-height: 1.75;
    /* 그룹 이름이 길면 줄을 넘긴다. 서류 폭을 밀어내지 않도록 강제 줄바꿈을 허용한다 */
    overflow-wrap: anywhere;
}

.gsc-body b {
    color: var(--tt-text);
}

/* ── 초대 코드 ───────────────────────────── */
.gsc-code-box {
    margin-top: 14px;
    width: 100%;
    border: 1.5px dashed var(--tt-doc-inset-border);
    border-radius: var(--tt-radius-xs);
    padding: 10px 12px 12px;
    background: var(--tt-doc-inset-bg);
}

.gsc-code-box__label {
    font-size: 10px;
    font-weight: var(--tt-fw-black);
    color: var(--tt-doc-label);
    letter-spacing: 0.12em;
}

.gsc-code-box__value {
    font-family: var(--tt-font-mono);
    font-size: 29px;
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
    letter-spacing: 0.3em;
    text-indent: 0.3em;
    margin-top: 4px;
}

/* ── 발부일 ──────────────────────────────── */
.gsc-issued {
    margin-top: 13px;
    width: 100%;
    padding-right: 2px;
    text-align: right;
    font-family: var(--tt-font-serif);
    font-size: 10.5px;
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-muted);
    letter-spacing: 0.14em;
    line-height: 1.7;
}

/* ── 인장 ────────────────────────────────── */
.gsc-seal {
    position: absolute;
    right: 8px;
    bottom: 58px;
    width: 52px;
    height: 52px;
    border: 2.5px solid var(--tt-doc-seal);
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    opacity: 0.9;
    animation: gsc-seal-hit 0.45s cubic-bezier(0.22, 1, 0.36, 1) 0.55s both;
}

.gsc-seal span {
    font-family: var(--tt-font-serif);
    font-size: 15px;
    font-weight: var(--tt-fw-black);
    color: var(--tt-doc-seal);
    letter-spacing: 0.1em;
    text-indent: 0.1em;
}

/* ── 애니메이션 ──────────────────────────── */
@keyframes gsc-drop {
    0% {
        opacity: 0;
        transform: translateY(-26px) rotate(3deg) scale(0.94);
    }
    60% {
        opacity: 1;
        transform: translateY(4px) rotate(-2deg) scale(1.01);
    }
    100% {
        opacity: 1;
        transform: translateY(0) rotate(-1.2deg) scale(1);
    }
}

@keyframes gsc-seal-hit {
    0% {
        opacity: 0;
        transform: rotate(-12deg) scale(2.2);
    }
    60% {
        opacity: 1;
        transform: rotate(-12deg) scale(0.92);
    }
    100% {
        opacity: 0.9;
        transform: rotate(-12deg) scale(1);
    }
}

/*
 * 낙하·도장은 연출일 뿐이라 없어도 정보가 빠지지 않는다.
 * 도장은 기운 각도까지 지우면 스티커처럼 보여서 최종 상태만 그대로 둔다.
 */
@media (prefers-reduced-motion: reduce) {
    .gsc-paper {
        animation: none;
    }

    .gsc-seal {
        animation: none;
        transform: rotate(-12deg);
    }
}
</style>
