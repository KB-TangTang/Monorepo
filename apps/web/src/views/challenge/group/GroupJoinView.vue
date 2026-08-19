<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import GroupInviteHeader from '@/components/challenge/group/GroupInviteHeader.vue';
import DevDataSourceFab from '@/components/dev/DevDataSourceFab.vue';
import { previewInviteCode, joinGroup } from '@/api/groupChallenge';

const route = useRoute();
const router = useRouter();

const group = ref(null);
const isLoading = ref(false);
const joinError = ref('');
const loadError = ref('');

/* 서버가 내려주는 참여 불가 사유. 코드 그대로 보여주면 사용자가 못 읽는다. */
const BLOCK_MESSAGE = {
    ALREADY_JOINED: '이미 참여 중인 그룹이에요.',
    EXPIRED: '모집이 마감된 그룹이에요. 초대는 시작일 23:59까지만 가능해요.',
    FULL: '자리가 가득 찼어요. 한 그룹에는 최대 6명까지 들어갈 수 있어요.',
    CLOSED: '이미 판결이 끝난 그룹이에요.',
};

const evalTypeLabel = computed(() => {
    if (!group.value) return '';
    return group.value.evalType === 'DAILY' ? '일일결산' : '기간결산';
});

const limitLabel = computed(() => {
    if (!group.value) return '';
    const prefix = group.value.evalType === 'DAILY' ? '하루' : '기간';
    return `${prefix} ${group.value.limitAmount.toLocaleString('ko-KR')}원`;
});

function formatDateRange(start, end) {
    if (!start || !end) return '';
    const s = new Date(start);
    const e = new Date(end);
    return `${s.getMonth() + 1}월 ${s.getDate()}일 ~ ${e.getMonth() + 1}월 ${e.getDate()}일`;
}

/* 참여도 이 코드로 한다(#346). 미리보기가 알려준 `group.id` 를 쓰면 안 된다 —
 * 그 순간 코드는 groupId 를 알아내는 수단이 될 뿐 참여 자격이 아니게 된다. */
const inviteCode = computed(() => route.params.code);

onMounted(async () => {
    const code = inviteCode.value;
    if (!code) {
        router.replace({ name: 'groupChallenge' });
        return;
    }
    try {
        /* 참여 불가 사유는 바텀시트가 이미 걸러 낸다. 여기까지 왔는데 막혀 있다면
         * 그 사이에 상태가 바뀐 것이다 — 조용히 되돌리면 사용자도 개발자도 원인을 알 수 없어
         * 사유를 화면에 남긴다. */
        const result = await previewInviteCode(code);
        if (!result.joinable || !result.group) {
            loadError.value = BLOCK_MESSAGE[result.reason] ?? '지금은 이 그룹에 참여할 수 없어요.';
            return;
        }
        group.value = result.group;
    } catch (e) {
        loadError.value = e.message ?? '초대 코드를 확인하지 못했습니다.';
    }
});

async function handleJoin() {
    if (!group.value || isLoading.value) return;
    isLoading.value = true;
    joinError.value = '';
    try {
        await joinGroup(inviteCode.value);
        /* 상세 화면(재판 현황)은 아직 서버가 없다. 목록으로 보내되 「시작 전」 탭을 열어야
         * 방금 들어간 그룹이 보인다 — 기본 탭인 「진행 중」으로 떨어지면 빈 화면이라
         * 참여가 실패한 것처럼 보인다. 상세 API 가 붙으면 그리로 바꾼다. */
        router.replace({ name: 'groupChallengeList', query: { tab: 'pre-start' } });
    } catch (e) {
        joinError.value = e.message ?? '그룹 참여에 실패했습니다.';
    } finally {
        isLoading.value = false;
    }
}

function goBack() {
    router.back();
}
</script>

<template>
    <div class="gjv-page">
        <GroupInviteHeader
            nav-label="그룹 참여"
            title="이 그룹에<br>참여할까요?"
            :badges="[{ text: '초대 확인', variant: 'success' }]"
            @back="goBack"
        />

        <div v-if="group" class="gjv-body">
            <!-- 그룹 정보 카드 -->
            <div class="gjv-info-card">
                <span class="gjv-eval-badge">{{ evalTypeLabel }}</span>
                <div class="gjv-group-name">{{ group.groupName }}</div>
                <div class="gjv-category">{{ group.categoryName }} 소비 줄이기</div>

                <div class="gjv-table">
                    <div class="gjv-row">
                        <span class="gjv-row__label">기간</span>
                        <span class="gjv-row__value">{{ formatDateRange(group.startDate, group.endDate) }}</span>
                    </div>
                    <div class="gjv-row">
                        <span class="gjv-row__label">초과 기준</span>
                        <span class="gjv-row__value">{{ limitLabel }}</span>
                    </div>
                    <div class="gjv-row gjv-row--last">
                        <span class="gjv-row__label">현재 멤버</span>
                        <span class="gjv-row__value">{{ group.memberCount }}명</span>
                    </div>
                </div>
            </div>

            <!-- 그룹 규칙 카드 -->
            <div v-if="group.rules" class="gjv-rules-card">
                <div class="gjv-rules-label">우리 그룹 규칙</div>
                <div class="gjv-rules-text">{{ group.rules }}</div>
            </div>

            <!-- 주의 문구 -->
            <div class="gjv-warning">
                참여하면 그룹 멤버에게 이름과 소비 상태가 보여요.
            </div>

            <p v-if="joinError" class="gjv-error">{{ joinError }}</p>
        </div>

        <!-- 그룹 정보를 못 가져온 경우 — 빈 화면 대신 사유를 보여준다 -->
        <div v-else-if="loadError" class="gjv-body">
            <p class="gjv-error">{{ loadError }}</p>
            <button type="button" class="gjv-link-btn" @click="goBack">
                다른 코드 입력
            </button>
        </div>

        <div v-if="group" class="gjv-bottom">
            <button
                type="button"
                class="gjv-gold-btn"
                :disabled="isLoading"
                @click="handleJoin"
            >
                그룹 참여하기
            </button>
            <button type="button" class="gjv-link-btn" @click="goBack">
                다른 코드 입력
            </button>
        </div>

        <DevDataSourceFab />
    </div>
</template>

<style scoped>
.gjv-page {
    min-height: 100vh;
    background: var(--tt-bg-subtle);
    display: flex;
    flex-direction: column;
}

/* ── body ──────────────────────────────── */
.gjv-body {
    flex: 1;
    padding: 0 20px;
    margin-top: -22px;
    position: relative;
    z-index: 3;
    display: flex;
    flex-direction: column;
    gap: 13px;
}

/* ── 그룹 정보 카드 ────────────────────── */
.gjv-info-card {
    background: var(--tt-bg);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-xl);
    box-shadow: 0 12px 28px rgba(35, 40, 66, 0.1);
    padding: 18px;
}

.gjv-eval-badge {
    display: inline-block;
    background: rgba(62, 99, 214, 0.1);
    color: var(--tt-info);
    font-size: 11px;
    font-weight: var(--tt-fw-black);
    padding: 4px 10px;
    border-radius: var(--tt-radius-full);
}

.gjv-group-name {
    font-size: var(--tt-fs-subtitle);
    font-weight: var(--tt-fw-black);
    margin-top: 10px;
    letter-spacing: -0.01em;
    color: var(--tt-text);
}

.gjv-category {
    font-size: 12.5px;
    color: var(--tt-text-muted);
    margin-top: 3px;
}

.gjv-table {
    margin-top: 14px;
    border-top: 1px solid #F0EDE6;
}

.gjv-row {
    display: flex;
    justify-content: space-between;
    padding: 11px 0;
    border-bottom: 1px solid #F0EDE6;
}

.gjv-row--last {
    border-bottom: none;
}

.gjv-row__label {
    font-size: 12.5px;
    color: var(--tt-text-muted);
    font-weight: var(--tt-fw-semibold);
}

.gjv-row__value {
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
}

/* ── 그룹 규칙 카드 ────────────────────── */
.gjv-rules-card {
    background: var(--tt-info-subtle);
    border: 1px solid #C9D6F5;
    border-radius: 16px;
    padding: 13px 15px;
}

.gjv-rules-label {
    font-size: 11px;
    font-weight: var(--tt-fw-black);
    color: var(--tt-info);
}

.gjv-rules-text {
    font-size: 12.5px;
    color: var(--tt-info-deep);
    margin-top: 5px;
    line-height: 1.5;
}

/* ── 주의 문구 ─────────────────────────── */
.gjv-warning {
    font-size: 11.5px;
    color: var(--tt-text-hint);
    text-align: center;
    line-height: 1.5;
}

/* ── 참여 실패 안내 ────────────────────── */
.gjv-error {
    padding: 10px 14px;
    background: var(--tt-danger-subtle);
    border-radius: var(--tt-radius-md);
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-danger-deep);
    text-align: center;
    line-height: 1.5;
}

/* ── bottom buttons ────────────────────── */
.gjv-bottom {
    flex: none;
    padding: 12px 20px 20px;
    background: var(--tt-bg-subtle);
}

.gjv-gold-btn {
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

.gjv-gold-btn:active {
    filter: brightness(0.95);
}

.gjv-gold-btn:disabled {
    opacity: 0.45;
    cursor: not-allowed;
}

.gjv-link-btn {
    display: block;
    width: 100%;
    background: transparent;
    border: none;
    text-align: center;
    margin-top: 12px;
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-body);
    cursor: pointer;
    font-family: inherit;
}
</style>
