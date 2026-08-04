<!--
  용도: 동의 화면의 항목 1행. [필수]/[선택] 배지 + 라벨 + 체크박스 + 약관 보기 링크.
  언제 쓰는지: 서비스 동의 화면. 항목 목록은 서버 카탈로그에서 받는다.
  쓰면 안 되는 경우: 일반 폼의 체크박스(공용 컴포넌트가 생기면 그걸 쓴다).

  약관 본문은 앱에 없다. termsUrl 은 노션 공개 페이지이고 새 탭으로 연다.
  새 탭이라 이 화면의 체크 상태는 그대로 유지된다.
-->
<script setup>
defineProps({
    type: { type: String, required: true },
    label: { type: String, required: true },
    required: { type: Boolean, default: false },
    termsUrl: { type: String, default: '' },
    modelValue: { type: Boolean, default: false },
});

const emit = defineEmits(['update:modelValue']);

function onToggle(event) {
    emit('update:modelValue', event.target.checked);
}
</script>

<template>
    <div class="consent-row">
        <label class="consent-row__main">
            <input
                class="consent-row__check"
                type="checkbox"
                :checked="modelValue"
                :aria-label="label"
                @change="onToggle"
            />
            <span class="consent-row__badge" :class="{ 'consent-row__badge--required': required }">
                {{ required ? '[필수]' : '[선택]' }}
            </span>
            <span class="consent-row__label">{{ label }}</span>
        </label>

        <a
            v-if="termsUrl"
            class="consent-row__link"
            :href="termsUrl"
            target="_blank"
            rel="noopener noreferrer"
        >
            보기 ›
        </a>
    </div>
</template>

<style scoped>
.consent-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: var(--tt-space-3);
    padding: var(--tt-space-4) var(--tt-space-2);
    border-bottom: 1px solid var(--tt-border);
}

.consent-row__main {
    display: flex;
    align-items: center;
    gap: var(--tt-space-3);
    cursor: pointer;
    min-width: 0;
}

.consent-row__check {
    width: 20px;
    height: 20px;
    accent-color: var(--tt-primary);
    flex-shrink: 0;
}

.consent-row__badge {
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-muted);
    flex-shrink: 0;
}

.consent-row__badge--required {
    color: var(--tt-danger);
}

.consent-row__label {
    font-size: var(--tt-fs-body);
    color: var(--tt-text);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.consent-row__link {
    font-size: var(--tt-fs-caption);
    color: var(--tt-text-muted);
    text-decoration: none;
    flex-shrink: 0;
}

.consent-row__link:hover {
    color: var(--tt-text);
}
</style>
