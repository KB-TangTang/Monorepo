<!--
  재판 입장 글리프 (이슈 #448)

  `STANCE.icon` 이 내려주는 네 가지(`gavel` · `ballot` · `clock` · `scale`)를 그린다.
  색은 `currentColor` 라 부모가 정한다 — 이 조각은 **모양만** 안다.

  재판 현황 카드(접힌 줄 아이콘 박스)와 할 일 격자(타일)가 같은 그림을 써야 한다.
  타일에서 본 망치와 목록에서 본 망치가 다르면 「이 타일이 저 목록을 연다」가 안 읽힌다.
  망치·투표함은 heroicons 에 없어 path 를 직접 들고 있다.
-->
<script setup>
import { ClockIcon, ScaleIcon } from '@heroicons/vue/24/solid';

defineProps({
    name: {
        type: String,
        required: true,
        validator: (v) => ['gavel', 'ballot', 'clock', 'scale'].includes(v),
    },
});
</script>

<template>
    <svg v-if="name === 'gavel'" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
        <path
            d="M1 21h12v2H1zM5.245 8.07l2.83-2.827 14.14 14.14-2.828 2.83zM12.317 1l5.657 5.657-2.83 2.83-5.654-5.66zM3.825 9.485l5.657 5.657-2.828 2.828-5.657-5.657z"
        />
    </svg>
    <svg v-else-if="name === 'ballot'" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
        <path
            d="M18 13h-.68l-2 2h1.91L19 17H5l1.78-2h2.05l-2-2H6l-3 3v4c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2v-4l-3-3zm-1-5.05l-4.95 4.95-3.54-3.54 4.95-4.95 3.54 3.54zm-4.24-5.66L6.39 8.66c-.39.39-.39 1.02 0 1.41l4.95 4.95c.39.39 1.02.39 1.41 0l6.36-6.36c.39-.39.39-1.02 0-1.41L14.16 2.3c-.38-.4-1.01-.4-1.4-.01z"
        />
    </svg>
    <ScaleIcon v-else-if="name === 'scale'" />
    <ClockIcon v-else />
</template>
