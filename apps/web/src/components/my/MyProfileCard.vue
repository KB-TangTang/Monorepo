<!--
  용도: 마이페이지 상단 프로필 카드. 이니셜 아바타 · 닉네임 · 'google · 이메일'.
  언제 쓰는지: MyPageView 안에서만.
  쓰면 안 되는 경우: 등급·레벨 표기 (DECISIONS.md 2026-07-15 등급제 미적용).
-->
<script setup>
import { computed } from 'vue';
import BaseCard from '@/components/common/BaseCard.vue';
import UserAvatar from '@/components/common/UserAvatar.vue';
import { providerLabel } from '@/utils/my';
import { resolveDisplayName } from '@/utils/user';

const props = defineProps({
    user: { type: Object, default: null },
});

/*
 * 표시명 규칙은 `nickname ?? socialName` 이고 서버가 displayName 으로 계산해 내려준다.
 * 규칙 자체는 utils/user.js 한 곳에 있다.
 */
const displayName = computed(() => resolveDisplayName(props.user));
const subtitle = computed(() => {
    const provider = providerLabel(props.user?.socialProvider);
    const email = props.user?.email ?? '';
    return [provider, email].filter(Boolean).join(' · ');
});
</script>

<template>
    <BaseCard>
        <div class="my-profile">
            <!--
              UserAvatar 통일 전에는 64px · 배경 --tt-surface-strong · 글자 --tt-primary 였다.
              size="lg"(96px)로 커진 회귀만 여기서 되돌린다. 배경·글자색은 되돌리지 않는다 —
              --tt-surface-strong 은 2026-07-31 토큰 개편으로 값이 --tt-ink(#232842, 다크
              스탯카드용, "하위 호환 · 제거 예정" 주석 붙음)로 바뀌어, 지금 그대로 넘기면 옛
              라이트 배경이 아니라 새로운 다크 원이 되어 또 다른 회귀가 된다. 또한 UserAvatar 는
              배경만 color prop 으로 덮어쓸 수 있고 이니셜 글자색(--tt-white 고정)은 prop 이 없어
              --tt-primary 로 되돌릴 수 없다. 구조상 정확한 복원이 불가능해 크기만 되돌린다.
            -->
            <UserAvatar :image-url="user?.profileImageUrl" :name="displayName" :size="64" />
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
