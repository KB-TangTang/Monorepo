<!--
  용도: 마이 탭 진입 화면 (MY_01_01). 프로필과 설정 진입점을 모은 허브다.
  언제 쓰는지: router 의 /my.
  쓰면 안 되는 경우: 여기에 새 기능 화면을 만들지 말 것 — 참고화면에 있는 4개 진입점만 둔다.
-->
<script setup>
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import BaseBottomSheet from '@/components/common/BaseBottomSheet.vue';
import BaseButton from '@/components/common/BaseButton.vue';
import MyMenuList from '@/components/my/MyMenuList.vue';
import MyProfileCard from '@/components/my/MyProfileCard.vue';
import StateError from '@/components/common/StateError.vue';
import StateLoading from '@/components/common/StateLoading.vue';
import { fetchMe, logout } from '@/api/auth';
import { useAuthStore } from '@/stores/auth';
import { usePersonalMissionChallengeStore } from '@/stores/personalMission';
import { resetGroupTutorial } from '@/services/groupTutorialGuide';

const router = useRouter();
const auth = useAuthStore();
const personalMission = usePersonalMissionChallengeStore();

const MENU = [
    { key: 'accounts', label: '계좌 연결 관리' },
    { key: 'consents', label: '동의 관리' },
    { key: 'tutorial', label: '튜토리얼 다시 보기' },
];

const user = ref(null);
const loading = ref(false);
const errorMessage = ref('');
const tutorialSheetOpen = ref(false);
const logoutSheetOpen = ref(false);
const loggingOut = ref(false);

/*
 * 인증 스토어의 user 를 쓰지 않고 직접 조회한다.
 * 스토어의 user 는 /api/auth/refresh 응답이라 socialProvider 가 없다.
 */
async function load() {
    loading.value = true;
    errorMessage.value = '';
    try {
        user.value = await fetchMe();
    } catch (err) {
        errorMessage.value = err.message ?? '내 정보를 불러오지 못했어요.';
    } finally {
        loading.value = false;
    }
}

onMounted(load);

function onSelect(key) {
    if (key === 'accounts') {
        router.push({ name: 'connectedAccounts' });
    } else if (key === 'consents') {
        router.push({ name: 'myConsents' });
    } else {
        tutorialSheetOpen.value = true;
    }
}

/* 튜토리얼 컴포넌트를 여기에 복제하지 않는다. 플래그를 되돌리고 해당 홈으로 보낸다 */
function replayPersonal() {
    personalMission.replayTutorial();
    tutorialSheetOpen.value = false;
    router.push({ name: 'personalMissionChallenge' });
}

function replayGroup() {
    resetGroupTutorial();
    tutorialSheetOpen.value = false;
    router.push({ name: 'groupChallenge' });
}

async function confirmLogout() {
    loggingOut.value = true;
    try {
        await logout();
    } catch {
        /* 서버가 실패해도 클라이언트 세션은 끊는다 — 남겨두면 로그아웃한 줄 아는데 안 된 상태가 된다 */
    } finally {
        auth.clear();
        loggingOut.value = false;
        router.replace({ name: 'login' });
    }
}
</script>

<template>
    <div class="my-page">
        <h1 class="my-page__title">마이페이지</h1>

        <StateLoading v-if="loading" message="내 정보를 불러오는 중" />
        <StateError v-else-if="errorMessage" :message="errorMessage" @retry="load" />
        <template v-else>
            <MyProfileCard :user="user" />
            <MyMenuList :items="MENU" @select="onSelect" />

            <div class="my-page__logout">
                <button
                    type="button"
                    class="my-page__logout-button"
                    @click="logoutSheetOpen = true"
                >
                    로그아웃
                </button>
            </div>
        </template>

        <BaseBottomSheet v-model="tutorialSheetOpen" title="어떤 튜토리얼을 다시 볼까요?">
            <div class="my-page__sheet">
                <BaseButton variant="secondary" block @click="replayPersonal">개인 미션</BaseButton>
                <BaseButton variant="secondary" block @click="replayGroup">그룹 챌린지</BaseButton>
            </div>
        </BaseBottomSheet>

        <BaseBottomSheet v-model="logoutSheetOpen" title="로그아웃할까요?">
            <div class="my-page__sheet">
                <p class="my-page__sheet-text">다시 이용하려면 구글 로그인을 한 번 더 해야 해요.</p>
                <BaseButton variant="dark" block :disabled="loggingOut" @click="confirmLogout">
                    로그아웃
                </BaseButton>
                <BaseButton variant="ghost" block @click="logoutSheetOpen = false">취소</BaseButton>
            </div>
        </BaseBottomSheet>
    </div>
</template>

<style scoped>
.my-page {
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-4);
    min-height: calc(100vh - var(--tt-tabbar-height));
    padding: var(--tt-space-5);
    background: var(--tt-bg-subtle);
}

.my-page__title {
    font-size: var(--tt-fs-title);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
}

.my-page__logout {
    margin-top: var(--tt-space-2);
    text-align: center;
}

.my-page__logout-button {
    padding: var(--tt-space-2);
    border: 0;
    background: none;
    font-family: var(--tt-font-sans);
    font-size: var(--tt-fs-caption);
    color: var(--tt-text-muted);
    text-decoration: underline;
    cursor: pointer;
}

.my-page__sheet {
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-3);
}

.my-page__sheet-text {
    font-size: var(--tt-fs-caption);
    color: var(--tt-text-muted);
}
</style>
