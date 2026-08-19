<script setup>
import { ref, reactive, computed } from 'vue';
import { useRouter } from 'vue-router';
import GroupCreateHeader from '@/components/challenge/group/GroupCreateHeader.vue';
import GroupCreateStepBasic from '@/components/challenge/group/GroupCreateStepBasic.vue';
import GroupCreateStepScope from '@/components/challenge/group/GroupCreateStepScope.vue';
import GroupCreateStepSettings from '@/components/challenge/group/GroupCreateStepSettings.vue';
import GroupCreateStepConfirm from '@/components/challenge/group/GroupCreateStepConfirm.vue';
import DevDataSourceFab from '@/components/dev/DevDataSourceFab.vue';
import { createGroupChallenge } from '@/api/groupChallenge';

const router = useRouter();

const currentStep = ref(0); // 0~3 = 위자드. 생성이 끝나면 이 화면을 떠나므로 그 뒤 단계는 없다
const isCreating = ref(false);
const createError = ref('');
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

async function handleCreate() {
    if (isCreating.value) return;
    isCreating.value = true;
    createError.value = '';
    try {
        const created = await createGroupChallenge(form);
        goToInvite(created.groupId);
    } catch (e) {
        /* 서버 검증(기간 7일 초과·시작일 과거 등)은 사유가 그대로 내려온다.
         * 최종 확인 화면에 남겨서 사용자가 앞 단계로 돌아가 고칠 수 있게 한다. */
        createError.value = e.message ?? '그룹을 만들지 못했습니다.';
    } finally {
        isCreating.value = false;
    }
}

/*
 * 생성이 끝나면 축하 화면을 거치지 않고 곧바로 초대 화면으로 간다. 축하 문구와 탕이는
 * 초대 화면 머리에 들어가 있다 — 중간에 「다음」만 하는 화면을 두면 버튼을 한 번 더 누르게
 * 할 뿐이고, 그 화면이 알려주던 초대 마감·인원은 초대 화면이 실데이터로 이미 보여준다.
 *
 * push 가 아니라 replace 다. push 하면 완료된 위자드가 히스토리에 남아 기기 뒤로가기가
 * 그리로 되돌아가, 이미 만든 그룹을 다시 만드는 것처럼 보인다. 자리를 덮어 두면 초대
 * 화면에서 뒤로가기를 눌러도 위자드 이전으로 나간다.
 * 초대 화면은 이 흐름을 `from=create` 로 알아보고 상단에 ← 대신 × 를 띄운다.
 */
function goToInvite(groupId) {
    router.replace({
        name: 'groupChallengeInvite',
        params: { groupId },
        query: { from: 'create' },
    });
}
</script>

<template>
    <div class="gcv-page">
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
                    :class="
                        form.evalType === 'DAILY' ? 'gcv-eval-badge--gold' : 'gcv-eval-badge--blue'
                    "
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

        <p v-if="createError" class="gcv-error">{{ createError }}</p>

        <DevDataSourceFab />
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
    color: #b9c6f2;
}

/* ── 생성 실패 안내 ──────────────────────── */
.gcv-error {
    margin: 0 var(--tt-screen-padding) var(--tt-space-4);
    padding: 10px 14px;
    background: var(--tt-danger-subtle);
    border-radius: var(--tt-radius-md);
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-danger-deep);
    text-align: center;
    line-height: 1.5;
}

/* ── step transition ─────────────────────── */
.gcv-slide-enter-active,
.gcv-slide-leave-active {
    transition:
        opacity 0.2s ease,
        transform 0.2s ease;
}

.gcv-slide-enter-from {
    opacity: 0;
    transform: translateX(24px);
}

.gcv-slide-leave-to {
    opacity: 0;
    transform: translateX(-24px);
}
</style>
