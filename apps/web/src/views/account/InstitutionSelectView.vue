<!--
  용도: 연결할 금융기관을 다중 선택한다 (AC_01_01). 연결 플로우 1단계.
  언제 쓰는지: 금융데이터 수집 동의 직후(온보딩), 그리고 자산 탭 → 연결 계좌 관리 → 추가 연결.
  쓰면 안 되는 경우: 이미 연결된 계좌를 보는 화면(ConnectedAccountView).

  레이아웃은 Figma 확정본 `금융기관 선택` 기준이다 —
  네비 제목 + 2줄 헤드라인 · 검색 · 업권 칩 필터 · 3열 카드 그리드 · 하단 요약 + 네이비 CTA.
  업권을 세로로 다 늘어놓지 않고 칩으로 하나씩 보여준다. 기관이 36곳이라 한 화면에 다 담으면
  스크롤만 길어지고 무엇을 고르는 화면인지 흐려진다.
  업권은 은행·카드·증권·대출·페이머니 5종이다 (이슈 #344 에서 대출·페이머니가 늘었다).

  온보딩과 추가 연결을 한 화면이 겸한다 (`?mode=add`). FIX_C_계좌추가연결_홈정리.md 가 요구한 차이가
  헤더 문구 · 연결된 기관 표시 · 탭바 노출뿐이라 뷰를 나누지 않았다.
-->
<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import { storeToRefs } from 'pinia';
import BaseButton from '@/components/common/BaseButton.vue';
import StateError from '@/components/common/StateError.vue';
import StateLoading from '@/components/common/StateLoading.vue';
import InstitutionTile from '@/components/account/InstitutionTile.vue';
import LinkStepHeader from '@/components/account/LinkStepHeader.vue';
import { useAccountStore } from '@/stores/account';
import { INSTITUTION_GROUPS, linkStepPosition } from '@/utils/account';

const route = useRoute();
const store = useAccountStore();
const { institutions, loading, error, selectedCount } = storeToRefs(store);

const isAddMode = computed(() => route.query.mode === 'add');
const position = linkStepPosition('institutions');

/** 지금 보고 있는 업권. 은행부터 시작한다 — 계좌 연동의 기본 대상이다. */
const activeGroup = ref(INSTITUTION_GROUPS[0].key);
const keyword = ref('');

const allInstitutions = computed(() =>
    INSTITUTION_GROUPS.flatMap((group) => institutions.value?.[group.key] ?? []),
);

/**
 * 기관이 하나도 없는 업권은 칩을 그리지 않는다.
 *
 * 서버가 **공급자가 다룰 수 있는 기관만** 내려주기 때문에 업권이 통째로 비는 경우가 있다 —
 * 실 CODEF 는 은행 20곳만 제공한다. 빈 칩을 눌러 빈 화면을 보게 두지 않는다.
 */
const availableGroups = computed(() =>
    INSTITUTION_GROUPS.filter((group) => (institutions.value?.[group.key] ?? []).length > 0),
);

/** 설명 문구도 실제로 고를 수 있는 업권만 말한다. 없는 업권을 약속하지 않는다. */
const groupSummary = computed(() =>
    availableGroups.value.length
        ? availableGroups.value.map((group) => group.label).join('·')
        : '금융기관',
);

/**
 * 헤더 설명.
 * 이미 연결한 기관도 고를 수 있다는 걸 **여기서 말한다** — 타일에 "연결됨" 이라고 써 있는데
 * 눌리기까지 하면 사용자는 그게 의도인지 오류인지 알 수 없다.
 */
const description = computed(() =>
    allInstitutions.value.some((item) => item.connected)
        ? '이미 연결한 기관을 다시 고르면 남은 계좌를 추가할 수 있어요.'
        : `${groupSummary.value}에서 여러 곳을 고를 수 있어요.`,
);

/**
 * 화면에 그릴 기관.
 * 검색어가 있으면 업권을 넘어 전체에서 찾는다 — 어느 칩에 있는지 모르고 검색하기 때문이다.
 */
const visible = computed(() => {
    const query = keyword.value.trim();
    if (query) {
        return allInstitutions.value.filter((item) => item.name.includes(query));
    }
    return institutions.value?.[activeGroup.value] ?? [];
});

/**
 * 대출 칩을 보고 있을 때만 띄우는 안내.
 *
 * 마이데이터에서 대출은 독립 업권이 아니라 **은행 업권 산하 상품**이다 —
 * 은행을 연결하면 그 은행의 대출까지 함께 내려온다. 그래서 이 칩에는 은행이 없고
 * 은행 밖 공급자(캐피탈·저축은행)만 남는다. 이유를 말해주지 않으면 사용자는
 * "내 주거래은행 대출이 왜 여기 없지" 하고 없는 기관을 검색하게 된다.
 * 검색 중에는 칩 자체가 사라지므로 안내도 같이 감춘다.
 */
const showLoanNotice = computed(() => !keyword.value.trim() && activeGroup.value === 'loans');

onMounted(async () => {
    /* 추가 연결로 들어오면 이전 플로우의 선택이 남아 있을 수 있다. */
    store.resetFlow();
    try {
        await store.loadInstitutions();
        /*
         * ⚠ 기본값으로 전체를 고르지 않는다.
         *   예전에는 뱅크샐러드처럼 전체 선택으로 시작했는데, 뱅크샐러드는 사용자가 실제로 보유한
         *   기관만 보여주는 반면 우리 목록은 36곳 전부다. 그대로 두면 사용자가 한 곳만 고른 줄 알고
         *   넘어가도 **비씨카드·롯데카드까지 목록에 있는 기관 전부로 인증·조회 요청이 나간다**
         *   (2026-08-05 실측, 당시 22곳).
         *   화면 문구도 "선택하세요" 이므로 빈 상태에서 시작하는 편이 말과 동작이 맞는다.
         */
        /* 은행이 비어 있는 공급자도 있을 수 있다. 첫 화면이 빈 칸이 되지 않게 맞춰준다. */
        if (!availableGroups.value.some((group) => group.key === activeGroup.value)) {
            activeGroup.value = availableGroups.value[0]?.key ?? activeGroup.value;
        }
    } catch {
        /* 오류는 StateError 가 보여준다. */
    }
});

function onNext() {
    if (selectedCount.value === 0) {
        return;
    }
    store.goNextStep('institutions');
}
</script>

<template>
    <div class="institution-select">
        <LinkStepHeader
            :title="isAddMode ? '금융기관 추가 연결' : '금융기관 연결'"
            :headline="isAddMode ? '연결할 기관을|더 골라주세요' : '심사할 기관을|선택하세요'"
            :description="description"
            :step="position.current"
            :total-steps="position.total"
            @back="store.goPrevStep('institutions')"
        />

        <StateLoading v-if="loading && !institutions" message="금융기관을 불러오는 중" />
        <StateError
            v-else-if="error && !institutions"
            :message="error"
            @retry="store.loadInstitutions"
        />

        <div v-else-if="institutions" class="institution-select__body">
            <label class="institution-select__search">
                <span class="institution-select__search-icon" aria-hidden="true">⌕</span>
                <input
                    v-model="keyword"
                    class="institution-select__search-input"
                    type="search"
                    autocomplete="off"
                    placeholder="금융기관 검색"
                />
            </label>

            <!-- 검색 중에는 업권 칩이 의미가 없다. 결과가 업권을 가로지르기 때문이다. -->
            <div
                v-if="!keyword.trim() && availableGroups.length > 1"
                class="institution-select__chips"
            >
                <button
                    v-for="group in availableGroups"
                    :key="group.key"
                    type="button"
                    class="institution-select__chip"
                    :class="{ 'institution-select__chip--on': activeGroup === group.key }"
                    :aria-pressed="activeGroup === group.key"
                    @click="activeGroup = group.key"
                >
                    {{ group.label }}
                </button>
            </div>

            <p v-if="showLoanNotice" class="institution-select__notice">
                은행 대출은 은행을 연결하면 함께 조회돼요. 여기서는 캐피탈·저축은행을 골라요.
            </p>

            <div v-if="visible.length" class="institution-select__grid">
                <InstitutionTile
                    v-for="item in visible"
                    :key="item.code"
                    :institution="item"
                    :selected="store.isInstitutionSelected(item.code)"
                    @toggle="store.toggleInstitution"
                />
            </div>
            <p v-else class="institution-select__empty">찾는 금융기관이 없어요.</p>
        </div>

        <!--
          기관 목록을 못 불러왔으면 CTA 를 감춘다.
          이전 선택이 스토어에 남아 있으면 목록이 비어도 "선택한 기관 N곳"이 뜨고
          버튼이 눌려, 존재를 확인하지 못한 기관으로 인증 요청이 나간다.
        -->
        <div v-if="institutions" class="institution-select__cta">
            <p class="institution-select__summary">
                <span>선택한 기관</span>
                <strong>{{ selectedCount }}곳</strong>
            </p>
            <BaseButton
                variant="dark"
                block
                size="lg"
                :disabled="selectedCount === 0"
                @click="onNext"
            >
                본인 인증 진행
            </BaseButton>
        </div>
    </div>
</template>

<style scoped>
.institution-select {
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-5);
    min-height: 100vh;
    padding: var(--tt-space-5);
    padding-bottom: var(--tt-space-12);
    background: var(--tt-bg-subtle);
}

.institution-select__body {
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-4);
    flex: 1;
}

.institution-select__search {
    display: flex;
    align-items: center;
    gap: var(--tt-space-2);
    height: 48px;
    padding: 0 var(--tt-space-4);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-md);
    background: var(--tt-bg);
}

.institution-select__search:focus-within {
    border-color: var(--tt-primary);
}

.institution-select__search-icon {
    font-size: var(--tt-fs-section);
    color: var(--tt-text-soft);
}

.institution-select__search-input {
    flex: 1;
    min-width: 0;
    border: 0;
    background: none;
    font-family: var(--tt-font-sans);
    font-size: var(--tt-fs-body);
    color: var(--tt-text);
}

.institution-select__search-input:focus {
    outline: none;
}

/*
 * 업권이 5종으로 늘어 좁은 화면에서는 한 줄에 다 들어가지 않는다.
 * 줄바꿈(flex-wrap) 대신 가로 스크롤을 유지한다 — 줄이 늘면 아래 그리드가 밀려
 * 3종만 내려오는 공급자에서 기존 모양이 달라진다. 스크롤바는 숨겨 높이를 고정한다.
 */
.institution-select__chips {
    display: flex;
    gap: var(--tt-space-2);
    overflow-x: auto;
    scrollbar-width: none;
}

.institution-select__chips::-webkit-scrollbar {
    display: none;
}

.institution-select__chip {
    flex-shrink: 0;
    padding: var(--tt-space-2) var(--tt-space-4);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-full);
    background: var(--tt-bg);
    font-family: var(--tt-font-sans);
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-muted);
    cursor: pointer;
}

/* 선택된 칩은 네이비로 채운다 — 확정본의 강조 방식이다. */
.institution-select__chip--on {
    border-color: var(--tt-surface-strong);
    background: var(--tt-surface-strong);
    color: var(--tt-text-inverse);
}

.institution-select__grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: var(--tt-space-3);
}

.institution-select__notice {
    margin: 0;
    padding: var(--tt-space-3);
    border-radius: var(--tt-radius-sm);
    background: var(--tt-primary-subtle);
    font-size: var(--tt-fs-caption);
    line-height: var(--tt-lh-normal);
    color: var(--tt-text-muted);
}

.institution-select__empty {
    margin: var(--tt-space-6) 0;
    font-size: var(--tt-fs-caption);
    color: var(--tt-text-muted);
    text-align: center;
}

.institution-select__cta {
    position: sticky;
    bottom: 0;
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-3);
    margin-top: auto;
    padding-top: var(--tt-space-4);
    background: var(--tt-bg-subtle);
}

/* 참고화면 0-5: 라벨(왼쪽 회색) — 개수(오른쪽 파란 강조) 양끝 배치 */
.institution-select__summary {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
    margin: 0;
    font-size: var(--tt-fs-caption);
    color: var(--tt-text-muted);
}

.institution-select__summary strong {
    color: var(--tt-primary);
}
</style>
