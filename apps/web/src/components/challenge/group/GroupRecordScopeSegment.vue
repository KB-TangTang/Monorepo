<!--
  재판 기록 범위 세그먼트 — 전체 · 내 재판 · 그룹원 재판

  **`GroupListSegment` 를 일반화해 돌려쓰지 않았다.** 저쪽은 지방법원 홈의 그룹 목록이
  쓰는데 탭 3개가 상수로 박혀 있어, 탭 목록을 prop 으로 빼는 순간 그 화면까지 같이 바뀐다.
  두 번째 사용처는 지역 컴포넌트로 두고 세 번째에 `common/` 으로 올린다(`AGENTS.md` 3의 법칙).
  대신 **생김새는 그대로 따랐다** — 알약 트랙 · 활성 탭만 강조색. 앱 안에 세그먼트가 두 종류로
  보이면 안 된다.

  「친구」가 아니라 **「그룹원」**이다. 이 앱의 사용자 문구는 전부 그룹원으로 통일돼 있다
  (`DefenseDoneView.vue:77` 「이제 그룹원들이 투표해요」). 「배심원」은 코드 안에서만 쓰는 말이다.
-->
<script setup>
defineProps({
    /** `'all' | 'mine' | 'others'` */
    modelValue: { type: String, required: true },
    /** 탭별 건수. 0 인 탭도 눌러서 「없다」를 확인할 수 있어야 해 비활성화하지 않는다 */
    counts: {
        type: Object,
        default: () => ({ all: 0, mine: 0, others: 0 }),
    },
});

defineEmits(['update:modelValue']);

const TABS = [
    { key: 'all', label: '전체' },
    { key: 'mine', label: '내 재판' },
    { key: 'others', label: '그룹원 재판' },
];
</script>

<template>
    <div class="record-scope">
        <button
            v-for="tab in TABS"
            :key="tab.key"
            type="button"
            class="record-scope__tab"
            :class="{ 'record-scope__tab--active': modelValue === tab.key }"
            :aria-pressed="modelValue === tab.key"
            @click="$emit('update:modelValue', tab.key)"
        >
            {{ tab.label }}
            <span v-if="counts[tab.key]" class="record-scope__count">{{ counts[tab.key] }}</span>
        </button>
    </div>
</template>

<style scoped>
.record-scope {
    display: flex;
    gap: 3px;
    padding: var(--tt-space-1);
    background: var(--tt-bg-fill);
    border-radius: var(--tt-radius-full);
}

.record-scope__tab {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 4px;
    padding: 8px 0;
    border: none;
    border-radius: var(--tt-radius-full);
    background: transparent;
    font-family: inherit;
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-muted);
    white-space: nowrap;
    cursor: pointer;
    transition:
        background 0.2s ease,
        color 0.2s ease;
}
.record-scope__tab--active {
    background: var(--tt-accent);
    color: var(--tt-text);
    font-weight: var(--tt-fw-black);
}

.record-scope__count {
    font-size: var(--tt-fs-badge);
    font-weight: var(--tt-fw-black);
}
</style>
