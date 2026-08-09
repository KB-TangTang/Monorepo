<!--
  참여 멤버 그리드 — 시작 전 상세화면에서 참여 멤버 + 소환(초대) 버튼을 표시한다.
  5열 그리드로 멤버 아바타와 닉네임, 남은 자리에 초대 버튼을 보여준다.
-->
<script setup>
import { computed } from 'vue';

const props = defineProps({
    members: { type: Array, required: true },
    memberCount: { type: Number, required: true },
    maxMembers: { type: Number, required: true },
});

const emit = defineEmits(['invite']);

const hasRoom = computed(() => props.memberCount < props.maxMembers);
const recruitLabel = computed(() =>
    `${props.memberCount} / ${props.maxMembers}명 · 첫날 23:59까지 모집`
);
</script>

<template>
    <div class="member-grid">
        <div class="member-grid__header">
            <span class="member-grid__title">참여 멤버</span>
            <span class="member-grid__label">{{ recruitLabel }}</span>
        </div>
        <div class="member-grid__grid">
            <div
                v-for="m in members"
                :key="m.userId"
                class="member-grid__item"
            >
                <div
                    class="member-grid__avatar"
                    :style="{ background: m.avatarColor }"
                >
                    {{ m.initial }}
                </div>
                <span class="member-grid__name">{{ m.nickname }}</span>
            </div>

            <!-- 소환(초대) 버튼 -->
            <div v-if="hasRoom" class="member-grid__item" @click="emit('invite')">
                <div class="member-grid__invite-btn">
                    <svg class="member-grid__invite-icon" viewBox="0 0 24 24" fill="currentColor">
                        <path d="M15 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm-9-2V7H4v3H1v2h3v3h2v-3h3v-2H6zm9 4c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z" />
                    </svg>
                </div>
                <span class="member-grid__invite-label">소환</span>
            </div>
        </div>
    </div>
</template>

<style scoped>
.member-grid {
    background: var(--tt-bg);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-lg);
    box-shadow: var(--tt-elevation-2);
    padding: 11px 14px;
}

.member-grid__header {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
}

.member-grid__title {
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
}

.member-grid__label {
    font-size: var(--tt-fs-badge);
    color: var(--tt-text-muted);
    font-weight: var(--tt-fw-bold);
}

.member-grid__grid {
    display: grid;
    grid-template-columns: repeat(5, 1fr);
    gap: 6px;
    margin-top: 9px;
}

.member-grid__item {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 5px;
}

.member-grid__avatar {
    width: 42px;
    height: 42px;
    border-radius: 50%;
    color: var(--tt-white);
    font-size: 14px;
    font-weight: var(--tt-fw-black);
    display: flex;
    align-items: center;
    justify-content: center;
}

.member-grid__name {
    font-size: var(--tt-fs-overline);
    color: var(--tt-text-body);
    font-weight: var(--tt-fw-bold);
}

.member-grid__invite-btn {
    width: 42px;
    height: 42px;
    border-radius: 50%;
    border: 1.5px dashed var(--tt-blue);
    background: var(--tt-blue-soft);
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
}

.member-grid__invite-icon {
    width: 20px;
    height: 20px;
    color: var(--tt-blue);
}

.member-grid__invite-label {
    font-size: var(--tt-fs-overline);
    color: var(--tt-blue);
    font-weight: var(--tt-fw-black);
}
</style>
