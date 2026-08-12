<!--
  용도: 마이 탭 진입 화면 (MY_01_01). 프로필과 설정 진입점을 모은 허브다.
  언제 쓰는지: router 의 /my.
  쓰면 안 되는 경우: 여기에 새 기능 화면을 만들지 말 것 — 참고화면에 있는 4개 진입점만 둔다.
-->
<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import BaseBottomSheet from '@/components/common/BaseBottomSheet.vue';
import BaseButton from '@/components/common/BaseButton.vue';
import BaseInput from '@/components/common/BaseInput.vue';
import MyMenuList from '@/components/my/MyMenuList.vue';
import MyProfileCard from '@/components/my/MyProfileCard.vue';
import StateError from '@/components/common/StateError.vue';
import StateLoading from '@/components/common/StateLoading.vue';
import { fetchMe, logout } from '@/api/auth';
import { deleteMyProfileImage, updateMyNickname, uploadMyProfileImage } from '@/api/user';
import { useAuthStore } from '@/stores/auth';
import { resetPersonalTutorial, resetGroupTutorial } from '@/services/tutorialGuide';
import { NICKNAME_MAX_LENGTH, resolveDisplayName, validateNickname } from '@/utils/user';

const router = useRouter();
const auth = useAuthStore();

const photoSheetOpen = ref(false);
const photoSheet = ref(null);
const photoInput = ref(null);
const photoBusy = ref(false);
const photoError = ref('');

async function runPhoto(action) {
    photoBusy.value = true;
    photoError.value = '';
    try {
        const updated = await action();
        /* 이 화면은 fetchMe() 결과를 따로 들고 있다 — 스토어와 함께 갈아끼워야 카드가 바뀐다 */
        user.value = { ...user.value, ...updated };
        auth.mergeUser(updated);
        photoSheet.value?.releaseHistory();
        photoSheetOpen.value = false;
    } catch (err) {
        photoError.value = err.message ?? '사진을 바꾸지 못했어요.';
    } finally {
        photoBusy.value = false;
    }
}

function onPickPhoto(event) {
    const file = event.target.files?.[0];
    event.target.value = '';
    if (file) {
        runPhoto(() => uploadMyProfileImage(file));
    }
}

const MENU = [
    { key: 'nickname', label: '닉네임 수정' },
    { key: 'accounts', label: '계좌 연결 관리' },
    { key: 'consents', label: '동의 관리' },
    { key: 'tutorial', label: '튜토리얼 다시 보기' },
];

const user = ref(null);
const loading = ref(false);
const errorMessage = ref('');
const tutorialSheetOpen = ref(false);
const tutorialError = ref('');
const replaying = ref(false);
const logoutSheetOpen = ref(false);
const loggingOut = ref(false);
const tutorialSheet = ref(null);
const logoutSheet = ref(null);
const nicknameSheetOpen = ref(false);
const nicknameDraft = ref('');
const nicknameError = ref('');
const savingNickname = ref(false);

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
    if (key === 'nickname') {
        openNicknameSheet();
    } else if (key === 'accounts') {
        router.push({ name: 'connectedAccounts' });
    } else if (key === 'consents') {
        router.push({ name: 'myConsents' });
    } else {
        tutorialError.value = '';
        tutorialSheetOpen.value = true;
    }
}

/*
 * 닉네임 수정 (MY_01_03).
 *
 * 화면을 따로 만들지 않고 이 화면의 바텀시트를 쓴다 — 입력 한 칸짜리 편집이고,
 * 저장 결과가 바로 위 프로필 카드에 나타나야 "바뀌었다"가 눈에 보이기 때문이다.
 * 오버레이를 직접 만들지 않고 로그아웃·튜토리얼과 같은 BaseBottomSheet 를 쓴다.
 */
const nicknameValidation = computed(() => validateNickname(nicknameDraft.value));

function openNicknameSheet() {
    /* 현재 닉네임을 미리 채운다. 아직 없는 사용자(온보딩 전)에게는 표시명을 출발점으로 준다. */
    nicknameDraft.value = user.value?.nickname || resolveDisplayName(user.value);
    nicknameError.value = '';
    nicknameSheetOpen.value = true;
}

async function saveNickname() {
    if (!nicknameValidation.value.valid || savingNickname.value) {
        return;
    }
    savingNickname.value = true;
    nicknameError.value = '';
    try {
        const updated = await updateMyNickname(nicknameValidation.value.value);
        auth.mergeUser(updated);
        /*
         * 이 화면의 user 는 fetchMe 결과라 스토어와 별개다(위 load() 주석 참고).
         * 함께 갈아끼워야 프로필 카드가 그 자리에서 바뀐다 — 다시 조회하러 나갔다 오게 만들지 않는다.
         */
        user.value = { ...user.value, ...updated };
        nicknameSheetOpen.value = false;
    } catch (err) {
        nicknameError.value = err.message ?? '닉네임을 저장하지 못했어요.';
    } finally {
        savingNickname.value = false;
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

/*
 * 플래그 해제는 서버(tbl_user.tutorial_seen_at / group_tutorial_seen_at)로 나간다.
 * 실패하면 이동하지 않고 시트 안에 오류를 남긴다 — 그대로 옮겨가면 튜토리얼이 안 뜨는
 * 화면에 도착해 고장으로 보인다. 오류 표시는 이 프로젝트가 쓰는 인라인 문구 방식
 * (ConnectedAccountView 의 `__error`)을 따른다.
 *
 * 성공 시 갱신된 사용자 정보가 인증 스토어에 반영되고(tutorialGuide), 이 화면의 user 는
 * 다시 들어올 때 onMounted(load) 로 새로 받으므로 화면 상태가 어긋나지 않는다.
 */
async function replay(reset, routeName) {
    if (replaying.value) return;
    replaying.value = true;
    tutorialError.value = '';
    try {
        await reset();
        leaveViaSheet(routeName);
    } catch (err) {
        tutorialError.value = err.message ?? '튜토리얼을 되돌리지 못했어요.';
    } finally {
        replaying.value = false;
    }
}

function replayPersonal() {
    return replay(resetPersonalTutorial, 'personalMissionChallenge');
}

function replayGroup() {
    return replay(resetGroupTutorial, 'groupChallenge');
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
                <button type="button" class="my-page__profile" @click="photoSheetOpen = true">
                    <MyProfileCard :user="user" />
                </button>
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

        <BaseBottomSheet v-model="nicknameSheetOpen" title="닉네임 수정">
            <div class="my-page__sheet">
                <BaseInput
                    v-model="nicknameDraft"
                    label="닉네임"
                    placeholder="닉네임을 입력해주세요"
                    :maxlength="NICKNAME_MAX_LENGTH"
                    :disabled="savingNickname"
                    required
                />
                <p v-if="nicknameError" class="my-page__sheet-error" role="alert">
                    {{ nicknameError }}
                </p>
                <BaseButton
                    block
                    :disabled="!nicknameValidation.valid"
                    :loading="savingNickname"
                    @click="saveNickname"
                >
                    저장
                </BaseButton>
            </div>
        </BaseBottomSheet>

        <BaseBottomSheet
            ref="tutorialSheet"
            v-model="tutorialSheetOpen"
            title="어떤 튜토리얼을 다시 볼까요?"
        >
            <div class="my-page__sheet">
                <p v-if="tutorialError" class="my-page__sheet-error">{{ tutorialError }}</p>
                <BaseButton variant="secondary" block :disabled="replaying" @click="replayPersonal">
                    대법원 (개인)
                </BaseButton>
                <BaseButton variant="secondary" block :disabled="replaying" @click="replayGroup">
                    지방법원 (그룹)
                </BaseButton>
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

        <BaseBottomSheet ref="photoSheet" v-model="photoSheetOpen" title="프로필 사진">
            <div class="my-page__sheet">
                <BaseButton block :loading="photoBusy" @click="photoInput?.click()">
                    사진 변경
                </BaseButton>
                <BaseButton
                    v-if="user?.profileImageUrl"
                    block
                    variant="ghost"
                    :loading="photoBusy"
                    @click="runPhoto(deleteMyProfileImage)"
                >
                    기본 이미지로 되돌리기
                </BaseButton>
                <p v-if="photoError" class="my-page__sheet-error" role="alert">{{ photoError }}</p>
                <input
                    ref="photoInput"
                    type="file"
                    accept="image/*"
                    class="my-page__file"
                    @change="onPickPhoto"
                />
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

.my-page__sheet-error {
    margin: 0;
    padding: var(--tt-space-3);
    border-radius: var(--tt-radius-md);
    background: var(--tt-danger-subtle);
    font-size: var(--tt-fs-caption);
    color: var(--tt-danger);
}

.my-page__sheet-text {
    font-size: var(--tt-fs-caption);
    color: var(--tt-text-muted);
}

.my-page__profile {
    display: block;
    width: 100%;
    padding: 0;
    text-align: left;
    background: none;
    border: 0;
    cursor: pointer;
}

.my-page__file {
    display: none;
}
</style>
