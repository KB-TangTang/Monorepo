<!--
  용도: 공통 컴포넌트·디자인 토큰을 한 화면에 모아 보는 개발용 카탈로그(/dev/ui). Storybook 대체.
  언제 쓰는지: 화면 작업 전에 "이거 이미 있나?" 를 확인할 때. 공통 컴포넌트를 추가하면 여기에도 등록한다.
  쓰면 안 되는 경우: 서비스 화면. 라우트가 import.meta.env.DEV 로 막혀 있어 프로덕션 빌드에 들어가지 않는다.
-->
<script setup>
import { onMounted, ref } from 'vue';
import CatalogItem from '@/components/dev/CatalogItem.vue';
import BaseBadge from '@/components/common/BaseBadge.vue';
import BaseBottomSheet from '@/components/common/BaseBottomSheet.vue';
import BaseButton from '@/components/common/BaseButton.vue';
import BaseCard from '@/components/common/BaseCard.vue';
import BaseInput from '@/components/common/BaseInput.vue';
import BaseModal from '@/components/common/BaseModal.vue';
import StateEmpty from '@/components/common/StateEmpty.vue';
import StateError from '@/components/common/StateError.vue';
import StateLoading from '@/components/common/StateLoading.vue';
import BentoStats from '@/components/common/cards/BentoStats.vue';
import JudgmentCard from '@/components/common/cards/JudgmentCard.vue';
import LifeGauge from '@/components/common/cards/LifeGauge.vue';
import ReceiptCard from '@/components/common/cards/ReceiptCard.vue';
import RecordCard from '@/components/common/cards/RecordCard.vue';
import StampSeal from '@/components/common/cards/StampSeal.vue';
import SummonsCard from '@/components/common/cards/SummonsCard.vue';

/* ── 목차 ─────────────────────────────────────────── */
const SECTIONS = [
    { id: 'color', label: '컬러 토큰' },
    { id: 'type', label: '타이포' },
    { id: 'tabbar', label: 'TheTabBar' },
    { id: 'card', label: 'BaseCard' },
    { id: 'button', label: 'BaseButton' },
    { id: 'input', label: 'BaseInput' },
    { id: 'modal', label: 'BaseModal' },
    { id: 'sheet', label: 'BaseBottomSheet' },
    { id: 'badge', label: 'BaseBadge' },
    { id: 'state', label: '상태 3종' },
    { id: 'cards', label: '카드 라이브러리 7종' },
];

/* ── 카드 라이브러리 데모 데이터 ──────────────────── */
const RECEIPT_ITEMS = [
    { label: '편의점 · GS25', amount: 4800 },
    { label: '카페 · 스타벅스', amount: 6300 },
    { label: '배달 · 배민', amount: 18500 },
];

const BENTO_ITEMS = [
    { label: '누적 도전일수', value: 42, unit: '일', tone: 'primary', span: 2 },
    { label: '연속 스트릭', value: 7, unit: '일', tone: 'accent' },
    { label: '명예 점수', value: 1280, tone: 'success' },
    { label: '이번 달 유죄', value: 3, unit: '건', tone: 'danger' },
    { label: '절감액', value: 48300, unit: '원', caption: '지난달 대비 +12%' },
];

/* ── 토큰 목록 ────────────────────────────────────── */
const SEMANTIC_TOKENS = [
    '--tt-primary',
    '--tt-primary-hover',
    '--tt-primary-subtle',
    '--tt-danger',
    '--tt-danger-subtle',
    '--tt-success',
    '--tt-success-subtle',
    '--tt-accent',
    '--tt-accent-subtle',
    '--tt-text',
    '--tt-text-muted',
    '--tt-text-inverse',
    '--tt-bg',
    '--tt-bg-subtle',
    '--tt-border',
    '--tt-border-strong',
    '--tt-overlay-dim',
    '--tt-notch-bg',
];

const PALETTE_GROUPS = [
    {
        title: 'Brand (Trust Blue)',
        tokens: [
            '--tt-brand-900',
            '--tt-brand-700',
            '--tt-brand-500',
            '--tt-brand-200',
            '--tt-brand-100',
            '--tt-brand-50',
        ],
    },
    {
        title: 'Accent (Gold)',
        tokens: ['--tt-accent-700', '--tt-accent-500', '--tt-accent-50'],
    },
    {
        title: 'Cool Gray',
        tokens: [
            '--tt-gray-900',
            '--tt-gray-800',
            '--tt-gray-700',
            '--tt-gray-600',
            '--tt-gray-500',
            '--tt-gray-400',
            '--tt-gray-300',
            '--tt-gray-200',
            '--tt-gray-100',
            '--tt-gray-50',
            '--tt-ink',
            '--tt-white',
        ],
    },
    {
        title: '판정 — 유죄 / 무죄',
        tokens: [
            '--tt-guilty-700',
            '--tt-guilty-300',
            '--tt-guilty-50',
            '--tt-innocent-800',
            '--tt-innocent-700',
            '--tt-innocent-300',
            '--tt-innocent-50',
        ],
    },
    {
        title: '종이 · 목재 (판사봉 · 인장 · 질감 전용)',
        tokens: ['--tt-wood', '--tt-kraft'],
    },
];

const TYPE_SCALE = [
    {
        token: '--tt-fs-display',
        spec: '44 / 800',
        sample: '48,300원',
        weight: 'var(--tt-fw-black)',
    },
    {
        token: '--tt-fs-title',
        spec: '26 / 800',
        sample: '이번 달 판결문',
        weight: 'var(--tt-fw-black)',
    },
    {
        token: '--tt-fs-numeric',
        spec: '34 / 800',
        sample: '1,240,000',
        weight: 'var(--tt-fw-black)',
    },
    {
        token: '--tt-fs-section',
        spec: '18 / 700',
        sample: '고정지출 기소 목록',
        weight: 'var(--tt-fw-bold)',
    },
    {
        token: '--tt-fs-body',
        spec: '15 / 400',
        sample: '본문입니다. 기본 읽기 크기.',
        weight: 'var(--tt-fw-regular)',
    },
    {
        token: '--tt-fs-caption',
        spec: '13 / 500',
        sample: '보조 설명 · 날짜 · 라벨',
        weight: 'var(--tt-fw-medium)',
    },
];

/* 토큰 실제 값은 CSS 에서 읽어온다 — 카탈로그에 HEX 를 적어두면 tokens.css 와 어긋난다. */
const tokenValues = ref({});

onMounted(() => {
    const style = getComputedStyle(document.documentElement);
    const all = [...SEMANTIC_TOKENS, ...PALETTE_GROUPS.flatMap((g) => g.tokens)];
    tokenValues.value = Object.fromEntries(all.map((t) => [t, style.getPropertyValue(t).trim()]));
});

/* ── 데모 상태 ────────────────────────────────────── */
const modalOpen = ref(false);
const sheetOpen = ref(false);
const nickname = ref('');
const plea = ref('편의점에서 산 게 아니라 생필품이었습니다');
const loadingDemo = ref(false);

function toggleLoading() {
    loadingDemo.value = true;
    setTimeout(() => (loadingDemo.value = false), 1200);
}

/* ── 복사용 코드 ──────────────────────────────────── */
const CODE = {
    tabbar: `<!-- App.vue 에 이미 한 번 렌더돼 있다. 화면에서 다시 붙이지 않는다. -->
<TheTabBar />`,
    card: `<BaseCard variant="record" padding="md" clickable @click="open">
    <template #header>넷플릭스 스탠다드</template>
    <p>매달 13,500원 · 최근 3회 연속 결제</p>
    <template #footer>다음 결제 2026-08-15</template>
</BaseCard>`,
    button: `<BaseButton variant="primary" size="md" :loading="isSaving" block @click="save">
    기소하기
</BaseButton>`,
    input: `<BaseInput v-model="plea" label="변론" :maxlength="200" multiline
    hint="1회만 제출할 수 있습니다" />`,
    modal: `<BaseModal v-model="isOpen" title="정말 해지할까요?">
    <p>해지하면 이번 달 절감액 집계에서 빠집니다.</p>
    <template #footer>
        <BaseButton variant="ghost" block @click="isOpen = false">취소</BaseButton>
        <BaseButton variant="danger" block @click="confirm">해지</BaseButton>
    </template>
</BaseModal>`,
    sheet: `<BaseBottomSheet v-model="isOpen" title="카테고리 선택">
    <ul><li>구독</li><li>통신</li><li>보험</li></ul>
</BaseBottomSheet>`,
    badge: `<BaseBadge variant="progress">진행 중 3</BaseBadge>
<BaseBadge variant="deadline">마감 02:14:03</BaseBadge>
<BaseBadge variant="guilty">유죄</BaseBadge>`,
    state: `<StateLoading v-if="isLoading" message="불러오는 중" />
<StateError v-else-if="error" :message="error" @retry="load" />
<StateEmpty v-else-if="!items.length" title="아직 기소된 지출이 없어요">
    <template #action><BaseButton size="sm">계좌 연동하기</BaseButton></template>
</StateEmpty>`,
    record: `<RecordCard
    case-no="TT-2026-0815"
    title="야식 금지 챌린지"
    :defendant-count="4"
    period="8/1 ~ 8/7"
    :charged-amount="128400"
    @click="goDetail"
>
    <template #badge><BaseBadge variant="progress">진행 중 3</BaseBadge></template>
</RecordCard>`,
    judgment: `<JudgmentCard case-no="TT-2026-0815" title="야식 금지 챌린지" date="2026-08-08" verdict="guilty">
    피고인은 심야 시간대 배달 결제 3건, 합계 48,300원을 지출하였다.
    이는 챌린지 한도를 초과한 것으로 유죄를 선고한다.
</JudgmentCard>`,
    receipt: `<ReceiptCard title="8월 1일 지출" issued-at="2026-08-01" :items="items" :limit="20000" />
<!-- items: [{ label: '편의점 · GS25', amount: 4800 }, … ] · 소계·초과액은 컴포넌트가 계산한다 -->`,
    stamp: `<StampSeal verdict="guilty" size="md" :angle="-10" />
<!-- verdict: guilty | innocent | confirmed -->`,
    summons: `<SummonsCard
    group-name="퇴근길 편의점 금지"
    period="8/5 ~ 8/11"
    member-text="3/6명"
    join-code="TT-4821"
    @join="join"
/>`,
    life: `<LifeGauge :remaining="2" :total="3" />
<!-- 다른 카드 안에 넣을 때는 카드 중첩을 피하려고 bare 를 켠다 -->
<LifeGauge :remaining="2" :total="3" bare />`,
    bento: `<BentoStats :items="[
    { label: '누적 도전일수', value: 42, unit: '일', tone: 'primary', span: 2 },
    { label: '연속 스트릭', value: 7, unit: '일', tone: 'accent' },
]" />`,
};
</script>

<template>
    <div class="catalog">
        <header class="catalog__head">
            <p class="catalog__tag">DEV ONLY</p>
            <h1 class="catalog__title">공통 컴포넌트 카탈로그</h1>
            <p class="catalog__lead">
                화면을 만들기 전에 여기서 먼저 찾는다. 있으면 쓰고, 없으면
                <code>components/&lt;도메인&gt;/</code> 에 만든다. 세 번째 화면에서 또 필요해지면
                <code>components/common/</code> 으로 올린다.
            </p>
            <nav class="catalog__toc">
                <a v-for="s in SECTIONS" :key="s.id" class="catalog__toclink" :href="`#${s.id}`">
                    {{ s.label }}
                </a>
            </nav>
        </header>

        <!-- ── 컬러 토큰 ─────────────────────────────── -->
        <section id="color" class="catalog__section">
            <h2 class="catalog__h2">컬러 토큰</h2>
            <p class="catalog__note">
                컴포넌트는 <strong>의미 토큰만</strong> 참조한다. 원시 팔레트는 tokens.css 안에서만
                쓴다. HEX 하드코딩 금지.
            </p>

            <h3 class="catalog__h3">의미 토큰</h3>
            <ul class="swatches">
                <li v-for="t in SEMANTIC_TOKENS" :key="t" class="swatch">
                    <span class="swatch__chip" :style="{ background: `var(${t})` }"></span>
                    <span class="swatch__name">{{ t }}</span>
                    <span class="swatch__value">{{ tokenValues[t] }}</span>
                </li>
            </ul>

            <template v-for="g in PALETTE_GROUPS" :key="g.title">
                <h3 class="catalog__h3">{{ g.title }}</h3>
                <ul class="swatches">
                    <li v-for="t in g.tokens" :key="t" class="swatch">
                        <span class="swatch__chip" :style="{ background: `var(${t})` }"></span>
                        <span class="swatch__name">{{ t }}</span>
                        <span class="swatch__value">{{ tokenValues[t] }}</span>
                    </li>
                </ul>
            </template>
        </section>

        <!-- ── 타이포 ────────────────────────────────── -->
        <section id="type" class="catalog__section">
            <h2 class="catalog__h2">타이포 스케일</h2>
            <p class="catalog__note">
                본문은 Pretendard(<code>--tt-font-sans</code>), 수치·코드는 Roboto
                Mono(<code>--tt-font-mono</code>).
            </p>
            <ul class="typelist">
                <li v-for="t in TYPE_SCALE" :key="t.token" class="typerow">
                    <span
                        class="typerow__sample"
                        :style="{ fontSize: `var(${t.token})`, fontWeight: t.weight }"
                    >
                        {{ t.sample }}
                    </span>
                    <span class="typerow__meta">{{ t.token }} · {{ t.spec }}</span>
                </li>
                <li class="typerow">
                    <span class="typerow__sample typerow__sample--mono">TT-2026-0815</span>
                    <span class="typerow__meta">--tt-fs-mono-chip · 12 · Roboto Mono</span>
                </li>
            </ul>
        </section>

        <!-- ── 컴포넌트 ──────────────────────────────── -->
        <section id="tabbar" class="catalog__section">
            <h2 class="catalog__h2">TheTabBar</h2>
            <CatalogItem
                name="TheTabBar"
                purpose="하단 고정 5탭(재판·자산·홈·장부·마이). 앱 셸에서 한 번만 렌더한다."
                :code="CODE.tabbar"
            >
                <p class="catalog__note">
                    이 화면 아래에 이미 실제로 렌더돼 있다 — 탭을 눌러 활성 표시를 확인한다.
                </p>
            </CatalogItem>
        </section>

        <section id="card" class="catalog__section">
            <h2 class="catalog__h2">BaseCard</h2>
            <CatalogItem
                name="BaseCard"
                purpose="카드 셸만 담당. 내용은 header/default/footer 슬롯으로 받는다."
                :code="CODE.card"
            >
                <div v-for="v in ['plain', 'record', 'judgment', 'receipt']" :key="v" class="demo">
                    <span class="demo__label">variant="{{ v }}"</span>
                    <BaseCard :variant="v">
                        <template #header>넷플릭스 스탠다드</template>
                        <p>매달 13,500원 · 최근 3회 연속 결제</p>
                        <template #footer>다음 결제 2026-08-15</template>
                    </BaseCard>
                </div>
                <div class="demo">
                    <span class="demo__label">clickable · padding="sm"</span>
                    <BaseCard clickable padding="sm">눌리는 카드 (버튼으로 렌더된다)</BaseCard>
                </div>
                <div class="demo">
                    <span class="demo__label">padding="none"</span>
                    <BaseCard padding="none">여백 없는 카드 — 목록·이미지를 꽉 채울 때</BaseCard>
                </div>
            </CatalogItem>
        </section>

        <section id="button" class="catalog__section">
            <h2 class="catalog__h2">BaseButton</h2>
            <CatalogItem
                name="BaseButton"
                purpose="모든 액션 버튼. variant 6종 × size 3종 + block · disabled · loading."
                :code="CODE.button"
            >
                <div
                    v-for="v in ['primary', 'secondary', 'ghost', 'danger', 'dark', 'accent']"
                    :key="v"
                    class="demo"
                >
                    <span class="demo__label">variant="{{ v }}"</span>
                    <div class="demo__row">
                        <BaseButton v-for="s in ['sm', 'md', 'lg']" :key="s" :variant="v" :size="s">
                            {{ s }}
                        </BaseButton>
                        <BaseButton :variant="v" disabled>disabled</BaseButton>
                        <BaseButton :variant="v" loading>loading</BaseButton>
                    </div>
                </div>
                <div class="demo">
                    <span class="demo__label">block · 클릭 시 1.2초 로딩</span>
                    <BaseButton block :loading="loadingDemo" @click="toggleLoading">
                        기소하기
                    </BaseButton>
                </div>
            </CatalogItem>
        </section>

        <section id="input" class="catalog__section">
            <h2 class="catalog__h2">BaseInput</h2>
            <CatalogItem
                name="BaseInput"
                purpose="label · hint · error · 글자수 카운터. multiline 으로 textarea 가 된다."
                :code="CODE.input"
            >
                <BaseInput
                    v-model="nickname"
                    label="닉네임"
                    placeholder="6자 이내"
                    :maxlength="6"
                />
                <BaseInput
                    v-model="nickname"
                    label="필수 입력"
                    required
                    hint="hint 는 도움말, error 가 있으면 error 가 우선한다"
                />
                <BaseInput
                    v-model="nickname"
                    label="에러 상태"
                    error="이미 사용 중인 닉네임입니다"
                />
                <BaseInput label="비활성" model-value="수정할 수 없음" disabled />
                <BaseInput
                    v-model="plea"
                    label="변론 (multiline)"
                    multiline
                    :maxlength="200"
                    :rows="3"
                />
            </CatalogItem>
        </section>

        <section id="modal" class="catalog__section">
            <h2 class="catalog__h2">BaseModal</h2>
            <CatalogItem
                name="BaseModal"
                purpose="오버레이 클릭 · ESC · 뒤로가기로 닫힘. 스크롤 잠금·포커스 트랩 포함. 모달은 반드시 이걸 쓴다."
                :code="CODE.modal"
            >
                <BaseButton variant="ghost" @click="modalOpen = true">모달 열기</BaseButton>
                <p class="catalog__note">
                    열고 나서 ESC · 오버레이 클릭 · 브라우저 뒤로가기 · Tab 이동을 모두 확인해 볼
                    것.
                </p>
                <p class="catalog__note">
                    ⚠ 겹쳐 뜨는 레이어를 새로 만들 일이 생기면 오버레이 동작을 직접 짜지 말고
                    <code>components/common/useOverlay.js</code> 를 쓴다. 스크롤 잠금(참조 카운트) ·
                    ESC · 포커스 트랩 · 뒤로가기 닫기 · 포커스 복원이 그 파일 하나에 모여 있고,
                    <code>BaseModal</code> 과 <code>BaseBottomSheet</code> 가 이걸 공유한다. 각자
                    만들면 화면마다 동작이 갈린다.
                </p>
                <BaseModal v-model="modalOpen" title="정말 해지할까요?">
                    <p>해지하면 이번 달 절감액 집계에서 빠집니다.</p>
                    <template #footer>
                        <BaseButton variant="ghost" block @click="modalOpen = false">
                            취소
                        </BaseButton>
                        <BaseButton variant="danger" block @click="modalOpen = false">
                            해지
                        </BaseButton>
                    </template>
                </BaseModal>
            </CatalogItem>
        </section>

        <section id="sheet" class="catalog__section">
            <h2 class="catalog__h2">BaseBottomSheet</h2>
            <CatalogItem
                name="BaseBottomSheet"
                purpose="아래에서 올라오는 시트. BaseModal 과 동작 규칙을 공유하고 핸들을 끌어 닫을 수 있다."
                :code="CODE.sheet"
            >
                <BaseButton variant="ghost" @click="sheetOpen = true">바텀시트 열기</BaseButton>
                <BaseBottomSheet v-model="sheetOpen" title="카테고리 선택">
                    <ul class="sheetdemo">
                        <li>구독</li>
                        <li>통신</li>
                        <li>보험</li>
                        <li>멤버십</li>
                    </ul>
                    <template #footer>
                        <BaseButton block @click="sheetOpen = false">선택 완료</BaseButton>
                    </template>
                </BaseBottomSheet>
            </CatalogItem>
        </section>

        <section id="badge" class="catalog__section">
            <h2 class="catalog__h2">BaseBadge</h2>
            <CatalogItem
                name="BaseBadge"
                purpose="상태 라벨 7종. deadline 은 숫자가 흔들리지 않게 모노 폰트로 그린다."
                :code="CODE.badge"
            >
                <div class="demo__row">
                    <BaseBadge>기본</BaseBadge>
                    <BaseBadge variant="progress">진행 중 3</BaseBadge>
                    <BaseBadge variant="daily">일일결산</BaseBadge>
                    <BaseBadge variant="period">기간결산</BaseBadge>
                    <BaseBadge variant="deadline">마감 02:14:03</BaseBadge>
                    <BaseBadge variant="guilty">유죄</BaseBadge>
                    <BaseBadge variant="innocent">무혐의 처분</BaseBadge>
                </div>
            </CatalogItem>
        </section>

        <section id="state" class="catalog__section">
            <h2 class="catalog__h2">상태 3종</h2>
            <CatalogItem
                name="StateLoading / StateError / StateEmpty"
                purpose="로딩 · 실패 · 0건. 화면마다 다른 스피너를 만들지 않기 위한 것."
                :code="CODE.state"
            >
                <div class="demo">
                    <span class="demo__label">StateLoading — size sm / md / lg · inline</span>
                    <div class="demo__row">
                        <StateLoading size="sm" inline message="sm" />
                        <StateLoading size="md" inline message="md" />
                        <StateLoading size="lg" inline message="lg" />
                    </div>
                    <StateLoading message="판결문을 불러오는 중" />
                </div>
                <div class="demo">
                    <span class="demo__label">StateError</span>
                    <StateError message="서버와 통신할 수 없습니다." />
                </div>
                <div class="demo">
                    <span class="demo__label">StateEmpty (action 슬롯)</span>
                    <StateEmpty
                        title="아직 기소된 지출이 없어요"
                        description="계좌를 연동하면 반복 결제를 자동으로 찾아냅니다."
                    >
                        <template #action>
                            <BaseButton size="sm">계좌 연동하기</BaseButton>
                        </template>
                    </StateEmpty>
                </div>
            </CatalogItem>
        </section>

        <!-- ── 카드 라이브러리 7종 ───────────────────── -->
        <section id="cards" class="catalog__section">
            <h2 class="catalog__h2">카드 라이브러리 7종</h2>
            <p class="catalog__note">
                전부 <code>BaseCard</code> 를 감싼 형태다(StampSeal 제외 — 카드가 아니라 카드 위에
                얹는 장식). 질감(괘선·펀치홀·절취선)은 이미지 없이 CSS 로만 그린다. 갈색
                계열(<code>--tt-wood</code>·<code>--tt-kraft</code>)은 판사봉·인장·종이에만 쓴다.
            </p>

            <CatalogItem
                name="① RecordCard — 사건기록부"
                purpose="사건번호(모노)·제목·피고 N인·기간·누적 혐의액. 목록 항목용."
                :code="CODE.record"
            >
                <RecordCard
                    case-no="TT-2026-0815"
                    title="야식 금지 챌린지"
                    :defendant-count="4"
                    period="8/1 ~ 8/7"
                    :charged-amount="128400"
                >
                    <template #badge>
                        <BaseBadge variant="progress">진행 중 3</BaseBadge>
                    </template>
                </RecordCard>
            </CatalogItem>

            <CatalogItem
                name="② JudgmentCard — 판결문"
                purpose="JUDGMENT 헤더 · 괘선 본문 · 하단 인장. 확정된 결과에만 쓴다."
                :code="CODE.judgment"
            >
                <JudgmentCard
                    case-no="TT-2026-0815"
                    title="야식 금지 챌린지"
                    date="2026-08-08"
                    verdict="guilty"
                >
                    피고인은 심야 시간대 배달 결제 3건, 합계 48,300원을 지출하였다. 이는 챌린지
                    한도를 초과한 것으로 유죄를 선고한다.
                </JudgmentCard>
            </CatalogItem>

            <CatalogItem
                name="③ ReceiptCard — 영수증"
                purpose="항목 괘선 + 절취선 아래 소계·한도·초과액. 합계는 컴포넌트가 계산한다."
                :code="CODE.receipt"
            >
                <div class="demo">
                    <span class="demo__label">한도 초과 (초과액 · danger)</span>
                    <ReceiptCard
                        title="8월 1일 지출"
                        issued-at="2026-08-01"
                        :items="RECEIPT_ITEMS"
                        :limit="20000"
                    />
                </div>
                <div class="demo">
                    <span class="demo__label">한도 이내 (잔여 · success)</span>
                    <ReceiptCard
                        title="8월 2일 지출"
                        issued-at="2026-08-02"
                        :items="RECEIPT_ITEMS.slice(0, 1)"
                        :limit="20000"
                    />
                </div>
            </CatalogItem>

            <CatalogItem
                name="④ StampSeal — 도장·인장"
                purpose="유죄/무죄/확정 인장. -8~-12° 기울여 찍는다. 한 화면에 하나만."
                :code="CODE.stamp"
            >
                <div class="demo__row">
                    <StampSeal verdict="guilty" size="sm" />
                    <StampSeal verdict="innocent" size="md" />
                    <StampSeal verdict="confirmed" size="lg" :angle="-8" />
                </div>
            </CatalogItem>

            <CatalogItem
                name="⑤ SummonsCard — 소환장·티켓"
                purpose="그룹명 · 참여 코드(모노) · 참여 CTA. 펀치홀 절취선은 CSS."
                :code="CODE.summons"
            >
                <SummonsCard
                    group-name="퇴근길 편의점 금지"
                    period="8/5 ~ 8/11"
                    member-text="3/6명"
                    join-code="TT-4821"
                />
            </CatalogItem>

            <CatalogItem
                name="⑥ LifeGauge — 목숨 게이지"
                purpose="남은 목숨 N/M 을 성한 판사봉 ↔ 부러진 판사봉으로. 1개 이하면 빨강."
                :code="CODE.life"
            >
                <LifeGauge :remaining="3" :total="3" />
                <LifeGauge :remaining="1" :total="3" label="남은 목숨 (위험)" />
                <div class="demo">
                    <span class="demo__label">bare — 다른 카드 안에 넣을 때</span>
                    <BaseCard variant="record">
                        <template #header>퇴근길 편의점 금지</template>
                        <LifeGauge :remaining="2" :total="3" bare />
                    </BaseCard>
                </div>
            </CatalogItem>

            <CatalogItem
                name="⑦ BentoStats — 벤토 그리드"
                purpose="통계 타일 2열 격자. tone 5종 · span 2 로 가로 전체 차지."
                :code="CODE.bento"
            >
                <BentoStats :items="BENTO_ITEMS" />
            </CatalogItem>
        </section>
    </div>
</template>

<style scoped>
.catalog {
    padding: var(--tt-space-5) var(--tt-space-4);
    background: var(--tt-bg-subtle);
}

.catalog__head {
    margin-bottom: var(--tt-space-8);
}

.catalog__tag {
    display: inline-block;
    padding: 2px var(--tt-space-2);
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-mono-chip);
    color: var(--tt-text-inverse);
    background: var(--tt-gray-900);
    border-radius: var(--tt-radius-xs);
}

.catalog__title {
    margin: var(--tt-space-2) 0;
    font-size: var(--tt-fs-title);
    font-weight: var(--tt-fw-black);
    line-height: var(--tt-lh-tight);
}

.catalog__lead,
.catalog__note {
    font-size: var(--tt-fs-caption);
    line-height: var(--tt-lh-normal);
    color: var(--tt-text-muted);
}

.catalog__lead code,
.catalog__note code {
    font-family: var(--tt-font-mono);
    color: var(--tt-text);
}

.catalog__toc {
    display: flex;
    flex-wrap: wrap;
    gap: var(--tt-space-2);
    margin-top: var(--tt-space-4);
}

.catalog__toclink {
    padding: 3px var(--tt-space-2);
    font-size: var(--tt-fs-mono-chip);
    color: var(--tt-primary);
    background: var(--tt-primary-subtle);
    border-radius: var(--tt-radius-full);
}

.catalog__section {
    margin-bottom: var(--tt-space-8);
    scroll-margin-top: var(--tt-space-4);
}

.catalog__h2 {
    margin-bottom: var(--tt-space-3);
    font-size: var(--tt-fs-section);
    font-weight: var(--tt-fw-black);
}

.catalog__h3 {
    margin: var(--tt-space-4) 0 var(--tt-space-2);
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-muted);
}

/* ── 스와치 ───────────────────────────────────────── */
.swatches {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
    gap: var(--tt-space-2);
}

.swatch {
    display: flex;
    align-items: center;
    gap: var(--tt-space-2);
    padding: var(--tt-space-2);
    background: var(--tt-bg);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-sm);
}

.swatch__chip {
    flex: none;
    width: 26px;
    height: 26px;
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-xs);
}

.swatch__name {
    flex: 1;
    overflow: hidden;
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-mono-chip);
    text-overflow: ellipsis;
    white-space: nowrap;
}

.swatch__value {
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-mono-chip);
    color: var(--tt-text-muted);
    text-transform: uppercase;
}

/* ── 타이포 ───────────────────────────────────────── */
.typelist {
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-3);
}

.typerow {
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-1);
    padding: var(--tt-space-3);
    background: var(--tt-bg);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-sm);
}

.typerow__sample {
    line-height: var(--tt-lh-tight);
    color: var(--tt-text);
}

.typerow__sample--mono {
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-mono-chip);
}

.typerow__meta {
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-mono-chip);
    color: var(--tt-text-muted);
}

/* ── 데모 ─────────────────────────────────────────── */
.demo {
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-2);
}

.demo__label {
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-mono-chip);
    color: var(--tt-text-muted);
}

.demo__row {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: var(--tt-space-2);
}

.sheetdemo li {
    padding: var(--tt-space-3) 0;
    border-bottom: 1px solid var(--tt-border);
}
</style>
