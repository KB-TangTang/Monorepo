<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import GroupInviteHeader from '@/components/challenge/group/GroupInviteHeader.vue';
import GroupSummonCard from '@/components/challenge/group/GroupSummonCard.vue';
import { fetchGroupDetail } from '@/api/groupChallenge';
import { resolveBack } from '@/utils/groupChallengeNavigation';

const route = useRoute();
const router = useRouter();

const group = ref(null);
const isPostCreate = computed(() => route.query.from === 'create');

const headerBadges = computed(() => {
    if (!isPostCreate.value) return [];
    return [
        { text: '초대 가능', variant: 'success' },
        { text: '첫날 23:59까지', variant: 'danger' },
    ];
});

const cardVariant = computed(() => (isPostCreate.value ? 'solid' : 'outline'));

const infoText = computed(() => {
    if (isPostCreate.value) {
        return '생성일부터 <b>첫날 23:59</b> 전까지 초대할 수 있어요. 초대 전에는 그룹 홈에서 다시 보여줄 수 있어요.';
    }
    return '초대 코드를 공유하면 배심원이 입장해요.<br><b>최대 6명</b>까지 함께할 수 있어요.';
});

const subLinkText = computed(() => (isPostCreate.value ? '그룹 화면으로' : '나중에 초대하기'));

onMounted(async () => {
    try {
        group.value = await fetchGroupDetail(route.params.groupId);
    } catch {
        router.replace({ name: 'groupChallenge' });
    }
});

async function handleShare() {
    const shareData = {
        title: `탕탕 그룹 챌린지 초대`,
        text: `${group.value?.groupName} 그룹에 함께해요! 초대 코드: ${group.value?.inviteCode}`,
        url: `${window.location.origin}/group-challenges/join/${group.value?.inviteCode}`,
    };

    try {
        if (navigator.share) {
            await navigator.share(shareData);
        } else {
            await navigator.clipboard.writeText(`${shareData.text}\n${shareData.url}`);
            alert('초대 링크가 클립보드에 복사되었습니다.');
        }
    } catch {
        // 사용자가 공유 다이얼로그를 닫은 경우
    }
}

/*
 * 진입로가 셋이다 — 상세·목록·생성 완료. 홈으로 하드코딩돼 있어 어디서 들어와도 홈으로
 * 나갔다(이슈 #303). 상세·목록은 push 하며 표시를 남기므로 그 자리로 되돌아가고,
 * **생성 완료는 표시가 없어 홈으로 나간다** — 방금 만든 폼으로 되돌아가면 안 되기 때문이다.
 */
function goBack() {
    const target = resolveBack(window.history.state, 'groupChallenge');

    if (target.type === 'back') {
        router.back();
        return;
    }
    router.push({ name: target.name });
}
</script>

<template>
    <div class="giv-page">
        <GroupInviteHeader
            nav-label="친구 초대"
            title="배심원을<br>초대해주세요!"
            :badges="headerBadges"
            @back="goBack"
        />

        <div v-if="group" class="giv-body">
            <!-- 안내 카드 -->
            <div
                class="giv-info-card"
                :class="{ 'giv-info-card--center': !isPostCreate }"
                v-html="infoText"
            />

            <!-- 소환장 카드 -->
            <div class="giv-card-area">
                <GroupSummonCard :invite-code="group.inviteCode" :variant="cardVariant" />
            </div>
        </div>

        <div v-if="group" class="giv-bottom">
            <button type="button" class="giv-gold-btn" @click="handleShare">
                초대 링크 공유하기
            </button>
            <button type="button" class="giv-link-btn" @click="goBack">
                {{ subLinkText }}
            </button>
        </div>
    </div>
</template>

<style scoped>
.giv-page {
    min-height: 100vh;
    background: var(--tt-bg-subtle);
    display: flex;
    flex-direction: column;
}

/* ── body ──────────────────────────────── */
.giv-body {
    flex: 1;
    padding: 0 22px;
    margin-top: -16px;
    position: relative;
    z-index: 3;
    display: flex;
    flex-direction: column;
}

.giv-info-card {
    background: var(--tt-bg);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-xl);
    box-shadow: 0 12px 28px rgba(35, 40, 66, 0.1);
    padding: 16px 18px;
    font-size: 12.5px;
    color: var(--tt-text-body);
    line-height: 1.6;
}

.giv-info-card--center {
    text-align: center;
}

.giv-info-card :deep(b) {
    color: var(--tt-text);
}

.giv-card-area {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
}

/* ── bottom buttons ────────────────────── */
.giv-bottom {
    flex: none;
    padding: 12px 22px 20px;
    background: var(--tt-bg-subtle);
}

.giv-gold-btn {
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

.giv-gold-btn:active {
    filter: brightness(0.95);
}

.giv-link-btn {
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
