<script setup>
import { ref, reactive, computed } from 'vue';
import { useRouter } from 'vue-router';
import GroupCreateHeader from '@/components/challenge/group/GroupCreateHeader.vue';
import GroupCreateStepBasic from '@/components/challenge/group/GroupCreateStepBasic.vue';
import GroupCreateStepScope from '@/components/challenge/group/GroupCreateStepScope.vue';
import GroupCreateStepSettings from '@/components/challenge/group/GroupCreateStepSettings.vue';
import GroupCreateStepConfirm from '@/components/challenge/group/GroupCreateStepConfirm.vue';
import celebrationImg from '@/assets/images/emotions/10_celebration.png';

const router = useRouter();

const currentStep = ref(0); // 0~3 = 위자드, 4 = 성공 화면
const createdGroupId = ref(null);
const form = reactive({
    evalType: 'DAILY',
    groupName: '',
    categoryId: null,
    rules: '',
    startDate: '',
    endDate: '',
    limitAmount: null,
});

const STEP_META = [
    { title: '어떤 방식으로<br>함께 도전할까요?', subtitle: '기본 정보' },
    { title: '챌린지 대상', subtitle: '대상·규칙' },
    { title: '챌린지 기간', subtitle: '' },
    { title: '이 내용으로<br>그룹을 만들까요?', subtitle: '최종 확인' },
];

const stepMeta = computed(() => STEP_META[currentStep.value] ?? {});

const headerSubtitle = computed(() => {
    if (currentStep.value === 2) {
        // Step 3 에서는 eval_type 뱃지를 subtitle 위치에 표시
        return '';
    }
    return stepMeta.value.subtitle;
});

function goNext() {
    if (currentStep.value < 3) {
        currentStep.value++;
    }
}

function goPrev() {
    if (currentStep.value > 0) {
        currentStep.value--;
    } else {
        router.push({ name: 'groupChallenge' });
    }
}

function handleCreate() {
    // API 연동 전이므로 바로 성공 화면으로 전환
    createdGroupId.value = 1;
    currentStep.value = 4;
}

function goToInvite() {
    router.push({
        name: 'groupChallengeInvite',
        params: { groupId: createdGroupId.value },
        query: { from: 'create' },
    });
}

function goToGroupHome() {
    router.push({ name: 'groupChallenge' });
}

function formatDate(dateStr) {
    if (!dateStr) return '';
    const d = new Date(dateStr);
    return `${d.getMonth() + 1}월 ${d.getDate()}일`;
}
</script>

<template>
    <div class="gcv-page">
        <!-- ===== 위자드 (step 0~3) ===== -->
        <template v-if="currentStep < 4">
            <GroupCreateHeader
                :step="currentStep + 1"
                :title="stepMeta.title"
                :subtitle="headerSubtitle"
                @back="goPrev"
            >
                <!-- Step 3: eval_type 뱃지 -->
                <template v-if="currentStep === 2" #subtitle>
                    <span
                        class="gcv-eval-badge"
                        :class="form.evalType === 'DAILY' ? 'gcv-eval-badge--gold' : 'gcv-eval-badge--blue'"
                    >
                        {{ form.evalType === 'DAILY' ? '일일결산' : '기간결산' }}
                    </span>
                </template>
            </GroupCreateHeader>

            <Transition name="gcv-slide" mode="out-in">
                <GroupCreateStepBasic
                    v-if="currentStep === 0"
                    :key="0"
                    :eval-type="form.evalType"
                    :group-name="form.groupName"
                    @update:eval-type="form.evalType = $event"
                    @update:group-name="form.groupName = $event"
                    @next="goNext"
                />
                <GroupCreateStepScope
                    v-else-if="currentStep === 1"
                    :key="1"
                    :category-id="form.categoryId"
                    :rules="form.rules"
                    @update:category-id="form.categoryId = $event"
                    @update:rules="form.rules = $event"
                    @next="goNext"
                />
                <GroupCreateStepSettings
                    v-else-if="currentStep === 2"
                    :key="2"
                    :eval-type="form.evalType"
                    :start-date="form.startDate"
                    :end-date="form.endDate"
                    :limit-amount="form.limitAmount"
                    @update:start-date="form.startDate = $event"
                    @update:end-date="form.endDate = $event"
                    @update:limit-amount="form.limitAmount = $event"
                    @next="goNext"
                />
                <GroupCreateStepConfirm
                    v-else-if="currentStep === 3"
                    :key="3"
                    :form="form"
                    @confirm="handleCreate"
                />
            </Transition>
        </template>

        <!-- ===== 성공 화면 (step 4) ===== -->
        <template v-else>
            <header class="gcv-success-header">
                <div class="gcv-success-deco" />
                <div class="gcv-success-nav">
                    <span class="gcv-success-nav__label">그룹 만들기</span>
                    <span class="gcv-success-badge">생성 완료</span>
                </div>
            </header>

            <div class="gcv-success-body">
                <div class="gcv-mascot-wrap">
                    <img
                        :src="celebrationImg"
                        alt="생성 완료 축하"
                        class="gcv-mascot"
                    />
                </div>

                <h2 class="gcv-success-title">그룹을 제대로<br>만들었어요!</h2>

                <p class="gcv-success-sub">
                    <b>{{ form.groupName }}</b><br>
                    {{ formatDate(form.startDate) }}부터 친구들과 시작해요.
                </p>

                <div class="gcv-success-info-card">
                    <div class="gcv-success-info-left">
                        <div class="gcv-success-info-label">초대 가능 시간</div>
                        <div class="gcv-success-info-value gcv-success-info-value--danger">첫날 23:59까지</div>
                    </div>
                    <div class="gcv-success-info-right">
                        <div class="gcv-success-info-label">현재 멤버</div>
                        <div class="gcv-success-info-value">1명</div>
                    </div>
                </div>
            </div>

            <div class="gcv-success-bottom">
                <button type="button" class="gcv-gold-btn" @click="goToInvite">
                    친구 초대하기
                </button>
                <button type="button" class="gcv-link-btn" @click="goToGroupHome">
                    그룹 먼저 보기
                </button>
            </div>
        </template>
    </div>
</template>

<style scoped>
.gcv-page {
    min-height: 100vh;
    background: var(--tt-bg-subtle);
    display: flex;
    flex-direction: column;
}

/* ── eval type badge (Step 3 header) ─────── */
.gcv-eval-badge {
    font-size: var(--tt-fs-overline);
    font-weight: var(--tt-fw-black);
    padding: 4px 10px;
    border-radius: var(--tt-radius-full);
}

.gcv-eval-badge--gold {
    background: rgba(245, 185, 33, 0.16);
    color: var(--tt-accent);
}

.gcv-eval-badge--blue {
    background: rgba(62, 99, 214, 0.28);
    color: #B9C6F2;
}

/* ── step transition ─────────────────────── */
.gcv-slide-enter-active,
.gcv-slide-leave-active {
    transition: opacity 0.2s ease, transform 0.2s ease;
}

.gcv-slide-enter-from {
    opacity: 0;
    transform: translateX(24px);
}

.gcv-slide-leave-to {
    opacity: 0;
    transform: translateX(-24px);
}

/* ============ SUCCESS SCREEN ============== */

.gcv-success-header {
    background: var(--tt-surface-inverse);
    border-radius: 0 0 var(--tt-radius-2xl) var(--tt-radius-2xl);
    padding: var(--tt-space-10) var(--tt-screen-padding) 30px;
    position: relative;
    overflow: hidden;
    flex: none;
}

.gcv-success-deco {
    position: absolute;
    top: -26px;
    right: -18px;
    width: 124px;
    height: 124px;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.05);
    pointer-events: none;
}

.gcv-success-nav {
    display: flex;
    align-items: center;
    justify-content: space-between;
    position: relative;
    z-index: 2;
}

.gcv-success-nav__label {
    font-size: var(--tt-fs-label);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text-inverse);
}

.gcv-success-badge {
    background: var(--tt-success-subtle);
    color: var(--tt-success);
    font-size: var(--tt-fs-overline);
    font-weight: var(--tt-fw-black);
    padding: 4px 10px;
    border-radius: var(--tt-radius-full);
}

/* ── success body ────────────────────────── */
.gcv-success-body {
    flex: 1;
    padding: 0 var(--tt-screen-padding);
    margin-top: -14px;
    position: relative;
    z-index: 3;
    display: flex;
    flex-direction: column;
    align-items: center;
    text-align: center;
}

.gcv-mascot-wrap {
    margin-top: 22px;
    width: 128px;
    height: 128px;
    border-radius: 50%;
    background: radial-gradient(circle, rgba(245, 185, 33, 0.22) 0%, rgba(245, 185, 33, 0) 70%);
    display: flex;
    align-items: center;
    justify-content: center;
}

.gcv-mascot {
    width: 100px;
    height: 100px;
    object-fit: contain;
    filter: drop-shadow(0 8px 14px rgba(35, 40, 66, 0.22));
    animation: tt-float 4s ease-in-out infinite;
}

.gcv-success-title {
    font-size: var(--tt-fs-title);
    font-weight: var(--tt-fw-black);
    margin-top: 14px;
    letter-spacing: -0.01em;
    line-height: 1.3;
    color: var(--tt-text);
}

.gcv-success-sub {
    font-size: var(--tt-fs-body);
    color: var(--tt-text-muted);
    margin-top: 10px;
    line-height: 1.6;
}

.gcv-success-sub b {
    color: var(--tt-text-body);
}

/* ── info card ────────────────────────────── */
.gcv-success-info-card {
    width: 100%;
    margin-top: var(--tt-space-6);
    background: var(--tt-bg);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-lg);
    box-shadow: var(--tt-elevation-2);
    padding: var(--tt-space-4) 18px;
    display: flex;
    align-items: center;
    justify-content: space-between;
}

.gcv-success-info-left {
    text-align: left;
}

.gcv-success-info-right {
    text-align: right;
}

.gcv-success-info-label {
    font-size: var(--tt-fs-overline);
    color: var(--tt-text-muted);
    font-weight: var(--tt-fw-bold);
}

.gcv-success-info-value {
    font-size: var(--tt-fs-label);
    font-weight: var(--tt-fw-black);
    margin-top: 3px;
    color: var(--tt-text);
}

.gcv-success-info-value--danger {
    color: var(--tt-danger-deep);
}

/* ── success bottom buttons ──────────────── */
.gcv-success-bottom {
    flex: none;
    padding: var(--tt-space-3) var(--tt-screen-padding) var(--tt-space-5);
}

.gcv-gold-btn {
    width: 100%;
    background: var(--tt-primary-gold);
    color: var(--tt-primary);
    font-weight: var(--tt-fw-black);
    font-size: 15.5px;
    padding: 16px;
    border-radius: 15px;
    border: none;
    text-align: center;
    cursor: pointer;
    font-family: inherit;
    transition: filter 0.15s ease;
}

.gcv-gold-btn:active {
    filter: brightness(0.95);
}

.gcv-link-btn {
    display: block;
    width: 100%;
    background: transparent;
    border: none;
    text-align: center;
    margin-top: var(--tt-space-3);
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-body);
    cursor: pointer;
    font-family: inherit;
}
</style>
