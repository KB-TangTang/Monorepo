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
import { resetPersonalTutorial, resetGroupTutorial } from '@/services/tutorialGuide';

const router = useRouter();
const auth = useAuthStore();

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
const tutorialSheet = ref(null);
const logoutSheet = ref(null);

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

/*
 * 튜토리얼 컴포넌트를 여기에 복제하지 않는다. 플래그를 되돌리고 해당 홈으로 보낸다.
 *
 * 시트 안에서 이동할 때는 releaseHistory() 로 시트가 쌓아둔 히스토리 항목을 넘겨받은 뒤
 * push 가 아닌 replace 로 옮긴다. 그러지 않으면 시트가 닫히며 실행하는 history.back() 이
 * 방금 한 이동을 되감아 이 화면에 그대로 남는다. (useOverlay 의 releaseHistory 주석 참고)
 */
function leaveViaSheet(routeName) {
    tutorialSheet.value?.releaseHistory();
    tutorialSheetOpen.value = false;
    router.replace({ name: routeName });
}

function replayPersonal() {
    resetPersonalTutorial();
    leaveViaSheet('personalMissionChallenge');
}

function replayGroup() {
    resetGroupTutorial();
    leaveViaSheet('groupChallenge');
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
        /* 튜토리얼 시트와 같은 이유로 히스토리 소유권을 넘겨받고 나간다 */
        logoutSheet.value?.releaseHistory();
        logoutSheetOpen.value = false;
        router.replace({ name: 'login' });
    }
}
</script>

<template>
    <div class="my-page">
        <h1 class="my-page__title">마이페이지</h1>

        <StateLoading v-if="loading" message="내 정보를 불러오는 중" />
        <template v-else>
            <!--
              StateError 가 전체를 대체해도 로그아웃 버튼만은 밖에 둔다.
              /users/me 조회가 계속 실패하면 이 화면에서 로그아웃할 방법이 없어져 사용자가 갇힌다.
            -->
            <StateError v-if="errorMessage" :message="errorMessage" @retry="load" />
            <template v-else>
                <MyProfileCard :user="user" />
                <MyMenuList :items="MENU" @select="onSelect" />
            </template>

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

        <BaseBottomSheet
            ref="tutorialSheet"
            v-model="tutorialSheetOpen"
            title="어떤 튜토리얼을 다시 볼까요?"
        >
            <div class="my-page__sheet">
                <BaseButton variant="secondary" block @click="replayPersonal">대법원 (개인)</BaseButton>
                <BaseButton variant="secondary" block @click="replayGroup">지방법원 (그룹)</BaseButton>
            </div>
        </BaseBottomSheet>

        <BaseBottomSheet ref="logoutSheet" v-model="logoutSheetOpen" title="로그아웃할까요?">
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
