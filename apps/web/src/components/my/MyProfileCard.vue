<!--
  용도: 마이페이지 상단 프로필 카드. 이니셜 아바타 · 닉네임 · 'google · 이메일'.
  언제 쓰는지: MyPageView 안에서만.
  쓰면 안 되는 경우: 등급·레벨 표기 (DECISIONS.md 2026-07-15 등급제 미적용).
-->
<script setup>
import { computed } from 'vue';
import BaseCard from '@/components/common/BaseCard.vue';
import { profileInitial, providerLabel } from '@/utils/my';
import { resolveDisplayName } from '@/utils/user';

const props = defineProps({
    user: { type: Object, default: null },
});

/*
 * 표시명 규칙은 `nickname ?? socialName` 이고 서버가 displayName 으로 계산해 내려준다.
 * 규칙 자체는 utils/user.js 한 곳에 있다 — 이름과 아바타 이니셜이 서로 다른 값을 보는 일이 없게
 * **둘 다 같은 값에서 뽑는다.** (DECISIONS.md 2026-08-11)
 */
const displayName = computed(() => resolveDisplayName(props.user));
const initial = computed(() => profileInitial(displayName.value));
const subtitle = computed(() => {
    const provider = providerLabel(props.user?.socialProvider);
    const email = props.user?.email ?? '';
    return [provider, email].filter(Boolean).join(' · ');
});
</script>

<template>
    <BaseCard>
        <div class="my-profile">
            <span class="my-profile__avatar" aria-hidden="true">{{ initial }}</span>
            <div class="my-profile__text">
                <p class="my-profile__name">{{ displayName || '이름 없음' }}</p>
                <p class="my-profile__sub">{{ subtitle }}</p>
            </div>
        </div>
    </BaseCard>
</template>

<style scoped>
.my-profile {
    display: flex;
    align-items: center;
    gap: var(--tt-space-4);
}

.my-profile__avatar {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 64px;
    height: 64px;
    font-size: var(--tt-fs-section);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-primary);
    background: var(--tt-surface-strong);
    border-radius: var(--tt-radius-full);
}

.my-profile__name {
    font-size: var(--tt-fs-section);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text);
}

.my-profile__sub {
    font-size: var(--tt-fs-caption);
    color: var(--tt-text-muted);
}
</style>
