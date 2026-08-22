<!--
  용도: 마이페이지 메뉴 목록. 행을 눌렀을 때 key 를 올려보내는 것까지만 한다.
  언제 쓰는지: MyPageView 안에서만.
  쓰면 안 되는 경우: 여기에서 router.push 를 하지 말 것 — 이동 결정은 화면이 한다.
-->
<script setup>
defineProps({
    items: { type: Array, required: true },
});

const emit = defineEmits(['select']);
</script>

<template>
    <ul class="my-menu">
        <li v-for="item in items" :key="item.key" class="my-menu__item">
            <button type="button" class="my-menu__button" @click="emit('select', item.key)">
                <span>{{ item.label }}</span>
                <span class="my-menu__chevron" aria-hidden="true">›</span>
            </button>
        </li>
    </ul>
</template>

<style scoped>
.my-menu {
    margin: 0;
    padding: 0;
    list-style: none;
    background: var(--tt-bg);
    border-radius: var(--tt-radius-lg);
    overflow: hidden;
}

.my-menu__item + .my-menu__item {
    border-top: 1px solid var(--tt-border);
}

.my-menu__button {
    display: flex;
    align-items: center;
    justify-content: space-between;
    width: 100%;
    padding: var(--tt-space-4) var(--tt-space-5);
    border: 0;
    background: none;
    font-family: var(--tt-font-sans);
    font-size: var(--tt-fs-body);
    color: var(--tt-text);
    cursor: pointer;
}

.my-menu__chevron {
    color: var(--tt-text-muted);
}
</style>
