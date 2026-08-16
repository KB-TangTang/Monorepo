<script setup>
import judgeImg from '@/assets/images/judgment/judge_tangtang_default.png';

/*
 * 재판 봇(type: 'SYSTEM') 메시지.
 *
 * 서버는 본문 문자열 하나만 준다(ChatMessageDto: type · content · sentAt). 목업에 있던
 * 시스템 서브타입·메타데이터(투표 마감 시각 · 판결 결과 · 재판 딥링크 CTA)는 서버 계약에 없어
 * 화면에서도 만들지 않는다 — 문구는 ChatSystemMessageListener 가 완성해서 보낸다.
 */
defineProps({
    message: { type: Object, required: true },
});
</script>

<template>
    <div class="sys-pill-wrap">
        <div class="sys-pill">
            <img :src="judgeImg" alt="판사 탕이" class="sys-pill__avatar" />
            <span class="sys-pill__text">{{ message.content }}</span>
        </div>
    </div>
</template>

<style scoped>
.sys-pill-wrap {
    display: flex;
    justify-content: center;
    padding: 14px 0;
    margin: 6px 0;
    border-top: 1px solid var(--tt-border);
    border-bottom: 1px solid var(--tt-border);
}

.sys-pill {
    display: flex;
    align-items: center;
    gap: 8px;
    border-radius: var(--tt-radius-full);
    font-size: 11.5px;
    max-width: 280px;
    background: var(--tt-accent-subtle);
    border: 1px solid #f2e2b8;
    color: #8a6510;
    padding: 5px 11px 5px 6px;
}

.sys-pill__avatar {
    width: 22px;
    height: 22px;
    border-radius: 50%;
    background: #fff;
    object-fit: contain;
    flex: none;
}

.sys-pill__text {
    flex: 1;
    min-width: 0;
    font-weight: var(--tt-fw-semibold);
    line-height: 1.4;
    word-break: break-word;
}
</style>
