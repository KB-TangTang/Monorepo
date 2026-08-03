<!--
  용도: 아직 담당자가 붙지 않은 탭의 임시 화면. 라우트 meta.title 을 그대로 보여준다.
  언제 쓰는지: 탭 구조를 먼저 세워두고 화면은 나중에 붙일 때. 담당자는 router/index.js 의 component 를 자기 뷰로 바꾼다.
  쓰면 안 되는 경우: 실제 기능 화면. 이 파일에 기능을 덧붙이지 말 것 — 곧 교체될 자리다.
-->
<script setup>
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import StateEmpty from '@/components/common/StateEmpty.vue';

const route = useRoute();
const title = computed(() => route.meta.title ?? '준비 중');
</script>

<template>
    <StateEmpty
        :title="`${title} 화면 준비 중`"
        description="담당자가 router/index.js 의 component 를 자기 뷰로 바꾸면 됩니다."
    />
</template>
