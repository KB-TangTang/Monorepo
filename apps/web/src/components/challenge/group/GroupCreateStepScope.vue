<script setup>
import { ref, onMounted } from 'vue';
import BaseInput from '@/components/common/BaseInput.vue';
import BaseButton from '@/components/common/BaseButton.vue';
import { fetchCategories } from '@/api/category';
import { toGroupCategorySections } from '@/utils/groupCategory';

defineProps({
    categoryId: { type: Number, default: null },
    rules: { type: String, required: true },
});

/*
 * 선택한 카테고리는 **id 와 이름을 함께** 올려보낸다. 최종 확인 화면이 「대상」 줄에 이름을
 * 보여줘야 하는데, 거기서 목록을 한 번 더 부르면 같은 이름을 두 곳에서 따로 풀게 된다.
 */
const emit = defineEmits(['update:category-id', 'update:category-name', 'update:rules', 'next']);

const sections = ref([]);
const isLoading = ref(true);
const loadFailed = ref(false);

onMounted(async () => {
    try {
        sections.value = toGroupCategorySections(await fetchCategories());
    } catch (e) {
        /* 목록을 못 불러도 「총 소비」로는 만들 수 있어야 한다. 위자드를 막지 않는다. */
        console.error('[GroupCreateStepScope] 카테고리 목록을 불러오지 못했다.', e);
        loadFailed.value = true;
    } finally {
        isLoading.value = false;
    }
});

function select(id, name) {
    emit('update:category-id', id);
    emit('update:category-name', name);
}
</script>

<template>
    <div class="step-scope">
        <div class="step-scope__card step-scope__card--raised">
            <p class="step-scope__desc">지켜볼 소비 카테고리 하나를 선택할 수 있어요.</p>
            <div class="step-scope__chips">
                <button
                    type="button"
                    class="scope-chip"
                    :class="{ 'scope-chip--active': categoryId === null }"
                    @click="select(null, '총 소비')"
                >
                    총 소비
                </button>
            </div>

            <p v-if="isLoading" class="step-scope__status">카테고리를 불러오는 중이에요…</p>
            <p v-else-if="loadFailed" class="step-scope__status step-scope__status--error">
                카테고리를 불러오지 못했어요. '총 소비'로 만들거나 잠시 뒤 다시 시도해 주세요.
            </p>

            <div v-for="section in sections" :key="section.parentId" class="step-scope__section">
                <p class="step-scope__section-title">{{ section.parentName }}</p>
                <div class="step-scope__chips step-scope__chips--tight">
                    <button
                        v-for="cat in section.items"
                        :key="cat.id"
                        type="button"
                        class="scope-chip"
                        :class="{ 'scope-chip--active': categoryId === cat.id }"
                        @click="select(cat.id, cat.name)"
                    >
                        {{ cat.name }}
                    </button>
                </div>
            </div>

            <div class="step-scope__info-box">
                기타 소비를 목록에 넣고 싶다면 카테고리 대신 <b>'총 소비'</b>를 선택해요.
            </div>
        </div>

        <div class="step-scope__card">
            <BaseInput
                :model-value="rules"
                label="우리 그룹 규칙"
                hint="친구들과 상황 내용을 자유롭게 작성해요."
                placeholder="야식 배달은 오후 9시 이후 금지"
                :maxlength="200"
                :rows="4"
                multiline
                @update:model-value="emit('update:rules', $event)"
            />
        </div>

        <div class="step-scope__bottom">
            <BaseButton variant="primary" size="lg" block @click="emit('next')">
                기간과 금액 설정
            </BaseButton>
        </div>
    </div>
</template>

<style scoped>
.step-scope {
    flex: 1;
    display: flex;
    flex-direction: column;
    overflow-y: auto;
    margin-top: -22px;
    position: relative;
    z-index: 2;
}

.step-scope__card {
    background: var(--tt-bg);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-xl);
    box-shadow: var(--tt-elevation-2);
    padding: var(--tt-space-4);
    margin: 0 var(--tt-screen-padding);
}

.step-scope__card--raised {
    box-shadow: var(--tt-elevation-3);
}

.step-scope__card + .step-scope__card {
    margin-top: 14px;
}

.step-scope__desc {
    font-size: var(--tt-fs-caption);
    color: var(--tt-text-muted);
    line-height: 1.5;
}

/* ── category chips ──────────────────────── */
.step-scope__chips {
    margin-top: var(--tt-space-3);
    display: flex;
    flex-wrap: wrap;
    gap: var(--tt-space-2);
}

.scope-chip {
    background: var(--tt-bg-fill);
    color: var(--tt-text-body);
    font-size: 13px;
    font-weight: var(--tt-fw-bold);
    padding: 8px 15px;
    border-radius: var(--tt-radius-full);
    border: none;
    cursor: pointer;
    transition: background-color 0.15s ease, color 0.15s ease;
}

.scope-chip--active {
    background: var(--tt-primary);
    color: var(--tt-text-inverse);
    font-weight: var(--tt-fw-black);
}

/* ── 대분류 섹션 ─────────────────────────── */
.step-scope__section {
    margin-top: var(--tt-space-3);
}

.step-scope__section-title {
    font-size: var(--tt-fs-overline);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text-hint);
}

.step-scope__chips--tight {
    margin-top: 6px;
}

.step-scope__status {
    margin-top: var(--tt-space-3);
    font-size: var(--tt-fs-caption);
    color: var(--tt-text-muted);
}

.step-scope__status--error {
    color: var(--tt-danger-deep);
}

/* ── info box (warm yellow) ──────────────── */
.step-scope__info-box {
    margin-top: 13px;
    background: #FFF6E2;
    border: 1px solid #F0E0B8;
    border-radius: 13px;
    padding: 11px 13px;
    font-size: 11.5px;
    color: #8A6A16;
    line-height: 1.5;
}

.step-scope__info-box b {
    font-weight: var(--tt-fw-black);
}

/* ── bottom button ────────────────────────── */
.step-scope__bottom {
    margin-top: auto;
    padding: var(--tt-space-3) var(--tt-screen-padding) var(--tt-space-5);
}
</style>
