<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import GroupInviteHeader from '@/components/challenge/group/GroupInviteHeader.vue';
import GroupSummonCard from '@/components/challenge/group/GroupSummonCard.vue';
import { fetchGroupDetail } from '@/api/groupChallenge';
import { resolveBack } from '@/utils/groupChallengeNavigation';
import { createdSubtitle, inviteBadges, inviteNotice, isInviteOpen } from '@/utils/groupInvite';
import celebrationImg from '@/assets/images/emotions/10_celebration.png';

const route = useRoute();
const router = useRouter();

const group = ref(null);

/*
 * 생성 완료 직후인지. 되돌아갈 앞 화면이 「방금 끝낸 생성 위자드」라 다른 진입로와 다르게
 * 다뤄야 한다 — 상단 버튼도, 축하 문구도, 하단 링크 문구도 여기서 갈린다.
 */
const isPostCreate = computed(() => route.query.from === 'create');

const headerBadges = computed(() => inviteBadges(group.value));
const noticeText = computed(() => inviteNotice(group.value));
const canInvite = computed(() => isInviteOpen(group.value));

/*
 * 생성 완료는 따로 화면을 두지 않는다. 축하만 보여주고 버튼을 한 번 더 누르게 하면
 * 그 화면이 하는 일이 「다음」밖에 없다. 축하 문구·탕이를 이 화면 머리로 가져와 한 번에 끝낸다.
 */
const headerTitle = computed(() =>
    isPostCreate.value ? '그룹을 제대로<br>만들었어요!' : '배심원을<br>초대해주세요!',
);
const headerSubtitle = computed(() => (isPostCreate.value ? createdSubtitle(group.value) : ''));
const headerMascot = computed(() => (isPostCreate.value ? celebrationImg : ''));

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
 * 나갔다(이슈 #303). 상세·목록은 push 하며 표시를 남기므로 그 자리로 되돌아간다.
 *
 * **생성 완료는 이 함수를 타지 않는다.** 표시를 남기지 않는 것만으로는 부족해서다 —
 * 히스토리에는 완료된 위자드가 그대로 남아 있어 기기 뒤로가기가 그리로 돌아갔다.
 * 생성 화면이 push 대신 `replace` 로 이 화면에 들어오고(GroupCreateView#goToInvite),
 * 상단 버튼도 ← 가 아니라 × 로 두어 `closeToGroupHome` 을 쓴다.
 */
function goBack() {
    const target = resolveBack(window.history.state, 'groupChallenge');

    if (target.type === 'back') {
        router.back();
        return;
    }
    router.push({ name: target.name });
}

/** 생성 완료 흐름의 「닫기」. 위자드 자리를 이미 replace 로 덮었으므로 홈으로 한 칸 더 replace 한다 */
function closeToGroupHome() {
    router.replace({ name: 'groupChallenge' });
}

/** 상단 버튼과 하단 링크가 같이 쓴다 — 두 곳의 나가는 곳이 어긋나면 사용자가 혼란스럽다 */
function leaveScreen() {
    if (isPostCreate.value) {
        closeToGroupHome();
        return;
    }
    goBack();
}
</script>

<template>
    <div class="giv-page">
        <GroupInviteHeader
            nav-label="친구 초대"
            :title="headerTitle"
            :subtitle="headerSubtitle"
            :mascot="headerMascot"
            :badges="headerBadges"
            :nav-mode="isPostCreate ? 'close' : 'back'"
            @back="leaveScreen()"
        />

        <div v-if="group" class="giv-body">
            <!-- 안내 카드 -->
            <div class="giv-info-card" v-html="noticeText" />

            <!-- 소환장 -->
            <div class="giv-card-area">
                <GroupSummonCard
                    :group-name="group.groupName"
                    :invite-code="group.inviteCode"
                    :issued-at="group.createdAt"
                />
            </div>
        </div>

        <div v-if="group" class="giv-bottom">
            <button type="button" class="giv-gold-btn" :disabled="!canInvite" @click="handleShare">
                초대 링크 공유하기
            </button>
            <button type="button" class="giv-link-btn" @click="leaveScreen()">
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
    box-shadow: var(--tt-elevation-3);
    padding: 16px 18px;
    font-size: 12.5px;
    color: var(--tt-text-body);
    line-height: 1.6;
}

.giv-info-card :deep(b) {
    color: var(--tt-text);
}

.giv-card-area {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    /* 기울어진 소환장 모서리가 화면 밖으로 나가지 않도록 좌우에 여유를 준다 */
    padding: 20px 4px;
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

.giv-gold-btn:disabled {
    background: var(--tt-bg-fill);
    color: var(--tt-text-hint);
    cursor: default;
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
