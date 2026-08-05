<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import GroupInviteHeader from '@/components/challenge/group/GroupInviteHeader.vue';
import { validateInviteCode, joinGroup } from '@/api/groupChallenge';

const route = useRoute();
const router = useRouter();

const group = ref(null);
const isLoading = ref(false);

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
    return `${s.getMonth() + 1}월 ${s.getDate()}일 – ${e.getMonth() + 1}월 ${e.getDate()}일`;
}

onMounted(async () => {
    const code = route.params.code;
    if (!code) {
        router.replace({ name: 'groupChallenge' });
        return;
    }
    try {
        const result = await validateInviteCode(code);
        if (!result.valid || !result.group) {
            router.replace({ name: 'groupChallenge' });
            return;
        }
        group.value = result.group;
    } catch {
        router.replace({ name: 'groupChallenge' });
    }
});

async function handleJoin() {
    if (!group.value || isLoading.value) return;
    isLoading.value = true;
    try {
        await joinGroup(group.value.id);
        router.replace({ name: 'groupChallenge' });
    } catch {
        alert('그룹 참여에 실패했습니다.');
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
