<!--
  용도: /dev/ui 카탈로그의 항목 한 칸. 이름 · 용도 한 줄 · 데모 영역 · 복사용 코드로 구성된다.
  언제 쓰는지: UiCatalogView.vue 에서만 쓴다. 카탈로그에 컴포넌트를 추가할 때 이걸로 감싼다.
  쓰면 안 되는 경우: 실제 서비스 화면. 개발용 화면 전용이라 프로덕션 빌드에 포함되지 않는다.
-->
<script setup>
import { ref } from 'vue';

defineProps({
    name: { type: String, required: true },
    purpose: { type: String, default: '' },
    code: { type: String, default: '' },
});

const copied = ref(false);
let timer = null;

async function copy(code) {
    try {
        await navigator.clipboard.writeText(code);
        copied.value = true;
        clearTimeout(timer);
        timer = setTimeout(() => (copied.value = false), 1500);
    } catch (err) {
        console.warn('클립보드 복사 실패', err);
    }
}
</script>

<template>
    <section class="item">
        <header class="item__head">
            <h3 class="item__name">{{ name }}</h3>
            <p v-if="purpose" class="item__purpose">{{ purpose }}</p>
        </header>

        <div class="item__demo"><slot /></div>

        <div v-if="code" class="item__code">
            <button class="item__copy" type="button" @click="copy(code)">
                {{ copied ? '복사됨' : '복사' }}
            </button>
            <pre><code>{{ code }}</code></pre>
        </div>
    </section>
</template>

<style scoped>
.item {
    padding: var(--tt-space-5);
    background: var(--tt-bg);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-md);
}

.item__head {
    margin-bottom: var(--tt-space-4);
}

.item__name {
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text);
}

.item__purpose {
    margin-top: var(--tt-space-1);
    font-size: var(--tt-fs-caption);
    color: var(--tt-text-muted);
}

.item__demo {
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-4);
    padding: var(--tt-space-4);
    background: var(--tt-bg-subtle);
    border-radius: var(--tt-radius-sm);
}

.item__code {
    position: relative;
    margin-top: var(--tt-space-4);
}

.item__code pre {
    overflow-x: auto;
    padding: var(--tt-space-3);
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-mono-chip);
    line-height: 1.7;
    color: var(--tt-text-inverse);
    background: var(--tt-gray-900);
    border-radius: var(--tt-radius-sm);
}

.item__copy {
    position: absolute;
    top: var(--tt-space-2);
    right: var(--tt-space-2);
    padding: 2px var(--tt-space-2);
    font-family: var(--tt-font-sans);
    font-size: var(--tt-fs-mono-chip);
    color: var(--tt-text-inverse);
    cursor: pointer;
    background: transparent;
    border: 1px solid var(--tt-gray-700);
    border-radius: var(--tt-radius-xs);
}

.item__copy:hover {
    background: var(--tt-gray-800);
}
</style>
