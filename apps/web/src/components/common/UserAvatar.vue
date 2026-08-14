<!--
  용도: 사용자 아바타. 이미지가 있으면 사진, 없으면 이니셜 + 색상 원.
  언제 쓰는지: 프로필 카드 · 그룹 멤버 목록 · 랭킹 · 재판 화면 등 사람을 그리는 모든 자리.
  쓰면 안 되는 경우: 사람이 아닌 것(가맹점 로고·기관 아이콘). 그건 각 도메인 컴포넌트가 그린다.

  ⚠ 폴백 규칙을 여기 한 곳에만 둔다. 화면마다 각자 이니셜 원을 그리면
    이미지 도입 같은 변경이 화면 수만큼 반복된다.
-->
<script setup>
import { computed, ref, watch } from 'vue';
import { avatarColor, profileInitial } from '@/utils/my';

const props = defineProps({
    imageUrl: { type: String, default: null },
    name: { type: String, default: '' },
    /* 'sm' | 'md' | 'lg' 또는 픽셀 숫자. 명예법정 포디움처럼 순위별로 크기가 다른 자리가 있다. */
    size: { type: [String, Number], default: 'md' },
    /* 넘기면 이름 해시 계산을 덮어쓴다. fixture 의 avatarColor 를 그대로 흘려보내는 용도다. */
    color: { type: String, default: null },
});

const SIZES = { sm: 32, md: 48, lg: 96 };

const pixels = computed(() =>
    typeof props.size === 'number' ? props.size : (SIZES[props.size] ?? SIZES.md),
);
const initial = computed(() => profileInitial(props.name));
const background = computed(() => props.color ?? avatarColor(props.name));

/* 저장소가 죽어도 목록이 깨지지 않게 이니셜로 되돌린다. */
const failed = ref(false);
watch(
    () => props.imageUrl,
    () => {
        failed.value = false;
    },
);
const showImage = computed(() => Boolean(props.imageUrl) && !failed.value);
</script>

<template>
    <img
        v-if="showImage"
        class="user-avatar user-avatar--image"
        :src="imageUrl"
        :alt="name"
        :style="{ width: pixels + 'px', height: pixels + 'px' }"
        @error="failed = true"
    />
    <span
        v-else
        class="user-avatar"
        aria-hidden="true"
        :style="{
            width: pixels + 'px',
            height: pixels + 'px',
            background,
            fontSize: Math.round(pixels * 0.4) + 'px',
        }"
    >
        {{ initial }}
    </span>
</template>

<style scoped>
.user-avatar {
    display: inline-flex;
    flex: none;
    align-items: center;
    justify-content: center;
    /* 교체된 9곳 전부가 --tt-fw-black 이었다. 통일 과정에서 --tt-fw-bold 로 내려간 회귀를 되돌린다. */
    font-weight: var(--tt-fw-black);
    color: var(--tt-white);
    border-radius: var(--tt-radius-full);
}

.user-avatar--image {
    object-fit: cover;
}
</style>
