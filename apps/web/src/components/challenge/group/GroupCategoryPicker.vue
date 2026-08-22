<!--
  용도: 그룹 챌린지가 지켜볼 소비 카테고리를 고르는 트리거 + 바텀시트 (이슈 #352).
  언제 쓰는지: 만들기 위자드 2단계(GroupCreateStepScope). 같은 위자드의 GroupDatePicker 와 같은 꼴이다
  — 화면에는 한 줄짜리 필드만 두고 선택지는 시트 안에서 보여준다.
  왜 시트인지: 소분류 15개를 대분류 5섹션으로 늘어놓으면 400px 가까이 되어 규칙 입력칸과
  다음 버튼이 화면 밖으로 밀린다. 시트 본문은 자체 스크롤이라 아무리 길어져도 단계를 밀지 않는다.
  주의: 오버레이를 직접 만들지 않는다. 스크롤 잠금·ESC·뒤로가기는 BaseBottomSheet 가 처리한다.
-->
<script setup>
import { ref, onMounted } from 'vue';
import BaseBottomSheet from '@/components/common/BaseBottomSheet.vue';
import { fetchCategories } from '@/api/category';
import { GROUP_CATEGORY_ALL_LABEL, toGroupCategorySections } from '@/utils/groupCategory';

/** 카테고리를 고르지 않았을 때의 이름. 「전체 소비를 합산한다」는 뜻이다. */
const ALL_LABEL = GROUP_CATEGORY_ALL_LABEL;

defineProps({
    categoryId: { type: Number, default: null },
    /*
     * 트리거에 그대로 찍는다. id 로 이름을 다시 푸는 대신 부모가 들고 있는 값을 받는다 —
     * 목록을 못 불러온 상태로 이 단계에 되돌아와도 고른 이름이 「총 소비」로 둔갑하지 않는다.
     */
    /* defineProps 는 컴파일 시점에 따로 떼어내지므로 ALL_LABEL 을 참조할 수 없다 (vue/valid-define-props) */
    categoryName: { type: String, default: '총 소비' },
    label: { type: String, default: '대상 카테고리' },
});

/* id 와 이름을 함께 올려보낸다. 최종 확인 화면이 이름을 쓰는데, 거기서 목록을 한 번 더 부르면
 * 같은 이름을 두 곳에서 따로 풀게 된다. */
const emit = defineEmits(['update:category-id', 'update:category-name']);

const isOpen = ref(false);
const sections = ref([]);
const isLoading = ref(true);
const loadFailed = ref(false);

onMounted(async () => {
    try {
        sections.value = toGroupCategorySections(await fetchCategories());
    } catch (e) {
        /* 목록을 못 불러도 「총 소비」로는 만들 수 있어야 한다. 위자드를 막지 않는다. */
        console.error('[GroupCategoryPicker] 카테고리 목록을 불러오지 못했다.', e);
        loadFailed.value = true;
    } finally {
        isLoading.value = false;
    }
});

function select(id, name) {
    emit('update:category-id', id);
    emit('update:category-name', name);
    isOpen.value = false;
}
</script>

<template>
    <button type="button" class="gcp-trigger" @click="isOpen = true">
        <span class="gcp-trigger__label">{{ label }}</span>
        <span class="gcp-trigger__value">{{ categoryName || ALL_LABEL }}</span>
        <svg
            class="gcp-trigger__caret"
            width="12"
            height="7"
            viewBox="0 0 12 7"
            fill="none"
            aria-hidden="true"
        >
            <path
                d="M1 1l5 5 5-5"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
            />
        </svg>
    </button>

    <BaseBottomSheet v-model="isOpen" title="대상 카테고리">
        <p class="gcp-sheet__desc">고른 카테고리의 소비만 합산해요. 하나만 고를 수 있어요.</p>

        <div class="gcp-sheet__chips">
            <button
                type="button"
                class="gcp-chip"
                :class="{ 'gcp-chip--active': categoryId === null }"
                :aria-pressed="categoryId === null"
                @click="select(null, ALL_LABEL)"
            >
                {{ ALL_LABEL }}
            </button>
        </div>

        <p v-if="isLoading" class="gcp-sheet__status">카테고리를 불러오는 중이에요…</p>
        <p v-else-if="loadFailed" class="gcp-sheet__status gcp-sheet__status--error">
            카테고리를 불러오지 못했어요. '총 소비'로 만들거나 잠시 뒤 다시 시도해 주세요.
        </p>

        <div v-for="section in sections" :key="section.parentId" class="gcp-sheet__section">
            <p class="gcp-sheet__section-title">{{ section.parentName }}</p>
            <div class="gcp-sheet__chips gcp-sheet__chips--tight">
                <button
                    v-for="cat in section.items"
                    :key="cat.id"
                    type="button"
                    class="gcp-chip"
                    :class="{ 'gcp-chip--active': categoryId === cat.id }"
                    :aria-pressed="categoryId === cat.id"
                    @click="select(cat.id, cat.name)"
                >
                    {{ cat.name }}
                </button>
            </div>
        </div>
    </BaseBottomSheet>
</template>

<style scoped>
/* ── 트리거 (같은 위자드의 GroupDatePicker 와 같은 꼴) ── */
.gcp-trigger {
    position: relative;
    display: block;
    width: 100%;
    padding: 11px 34px 11px 13px;
    margin-top: var(--tt-space-3);
    text-align: left;
    cursor: pointer;
    background: var(--tt-bg);
    border: 1.5px solid var(--tt-border);
    border-radius: 13px;
    transition: border-color 0.15s ease;
}

.gcp-trigger:active {
    border-color: var(--tt-primary);
}

.gcp-trigger__label {
    display: block;
    font-size: var(--tt-fs-overline);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-muted);
}

.gcp-trigger__value {
    display: block;
    margin-top: 3px;
    font-size: 16px;
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
}

.gcp-trigger__caret {
    position: absolute;
    top: 50%;
    right: 13px;
    color: var(--tt-text-hint);
    transform: translateY(-50%);
}

/* ── 시트 본문 ─────────────────────────────── */
.gcp-sheet__desc {
    font-size: var(--tt-fs-caption);
    line-height: 1.5;
    color: var(--tt-text-muted);
}

.gcp-sheet__chips {
    display: flex;
    flex-wrap: wrap;
    gap: var(--tt-space-2);
    margin-top: var(--tt-space-3);
}

.gcp-sheet__chips--tight {
    margin-top: 6px;
}

.gcp-chip {
    padding: 8px 15px;
    font-size: 13px;
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-body);
    cursor: pointer;
    background: var(--tt-bg-fill);
    border: none;
    border-radius: var(--tt-radius-full);
    transition:
        background-color 0.15s ease,
        color 0.15s ease;
}

.gcp-chip--active {
    font-weight: var(--tt-fw-black);
    color: var(--tt-text-inverse);
    background: var(--tt-primary);
}

.gcp-sheet__section {
    margin-top: var(--tt-space-3);
}

.gcp-sheet__section-title {
    font-size: var(--tt-fs-overline);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text-hint);
}

.gcp-sheet__status {
    margin-top: var(--tt-space-3);
    font-size: var(--tt-fs-caption);
    color: var(--tt-text-muted);
}

.gcp-sheet__status--error {
    color: var(--tt-danger-deep);
}
</style>
