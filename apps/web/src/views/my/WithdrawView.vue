<!--
  용도: 회원 탈퇴 확인 화면 (MY_01_05). 파기 항목과 복구 불가를 고지하고 탈퇴를 실행한다.
  언제 쓰는지: router 의 /my/withdraw. MyPageView 의 "회원 탈퇴" 버튼이 이 화면으로 보낸다.
  쓰면 안 되는 경우: 로그아웃 — 그쪽은 MyPageView 의 바텀시트다. 동의 개별 철회 — ConsentManageView 다.
-->
<script setup>
import { computed, ref } from 'vue';
import { useRouter } from 'vue-router';
import BaseBackButton from '@/components/common/BaseBackButton.vue';
import BaseButton from '@/components/common/BaseButton.vue';
import { withdrawMe } from '@/api/user';
import { useAuthStore } from '@/stores/auth';

const router = useRouter();
const auth = useAuthStore();

const agreed = ref(false);
const submitting = ref(false);
const errorMessage = ref('');

const canSubmit = computed(() => agreed.value && !submitting.value);

/*
 * ⚠ MyPageView 의 confirmLogout() 을 복사하지 말 것.
 * 로그아웃은 finally 에서 무조건 세션을 끊고 나가지만(서버가 실패해도 끊는 게 맞다),
 * 탈퇴는 **성공했을 때만** 나가야 한다. 실패했는데 로그인 화면으로 보내면
 * 사용자는 탈퇴된 줄 아는데 계정은 살아 있는 상태가 된다.
 * 그래서 이동은 catch 밖·try 뒤에 둔다.
 */
async function confirmWithdraw() {
    if (!canSubmit.value) {
        return;
    }
    submitting.value = true;
    errorMessage.value = '';
    try {
        await withdrawMe();
    } catch (err) {
        errorMessage.value = err?.message ?? '탈퇴에 실패했습니다. 잠시 후 다시 시도해 주세요.';
        return;
    } finally {
        submitting.value = false;
    }
    auth.clear();
    router.replace({ name: 'login', query: { notice: 'goodbye' } });
}
</script>

<template>
    <div class="withdraw">
        <header class="withdraw__header">
            <!-- to 를 주지 않는다 — router.back() 이라야 들어온 자리로 돌아간다 -->
            <div class="withdraw__head-row">
                <BaseBackButton label="이전 화면으로 돌아가기" />
                <h1 class="withdraw__title">회원 탈퇴</h1>
            </div>
        </header>

        <p class="withdraw__lead">정말 탈퇴하시겠어요?</p>

        <section class="withdraw__section">
            <h2 class="withdraw__subtitle">아래 정보가 즉시 삭제됩니다</h2>
            <ul class="withdraw__list">
                <li>이메일 · 닉네임 · 프로필 사진</li>
                <li>연동한 계좌 · 카드 (연결 해제)</li>
                <li>금융정보 활용 동의</li>
            </ul>
        </section>

        <!--
          이 화면에서 가장 중요한 문장이다. 재가입이 되니 데이터도 돌아올 것이라
          오해하기 쉬운데, 이 설계의 유일한 UX 리스크가 그 기대 불일치다.
        -->
        <p class="withdraw__warning">
            거래내역 · 미션 기록 · 챌린지 이력은 복구할 수 없습니다.<br />
            같은 구글 계정으로 다시 가입할 수 있지만, 처음부터 시작하게 됩니다.
        </p>

        <label class="withdraw__check">
            <input v-model="agreed" type="checkbox" />
            <span>위 내용을 확인했습니다</span>
        </label>

        <p v-if="errorMessage" class="withdraw__error" role="alert">{{ errorMessage }}</p>

        <div class="withdraw__actions">
            <BaseButton
                variant="danger"
                block
                :disabled="!agreed"
                :loading="submitting"
                @click="confirmWithdraw"
            >
                탈퇴하기
            </BaseButton>
            <BaseButton variant="ghost" block :disabled="submitting" @click="router.back()">
                취소
            </BaseButton>
        </div>
    </div>
</template>

<style scoped>
.withdraw {
    padding: var(--tt-space-4) var(--tt-space-4) var(--tt-space-10);
}

.withdraw__head-row {
    display: flex;
    align-items: center;
    gap: var(--tt-space-2);
}

.withdraw__title {
    font-size: var(--tt-fs-subtitle);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text);
}

.withdraw__lead {
    margin: var(--tt-space-6) 0 var(--tt-space-4);
    font-size: var(--tt-fs-title);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text);
}

.withdraw__section {
    background: var(--tt-bg-subtle);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-lg);
    padding: var(--tt-space-4);
    margin-bottom: var(--tt-space-4);
}

.withdraw__subtitle {
    font-size: var(--tt-fs-label);
    font-weight: var(--tt-fw-semibold);
    color: var(--tt-text);
    margin-bottom: var(--tt-space-2);
}

.withdraw__list {
    margin: 0;
    padding-left: var(--tt-space-5);
    color: var(--tt-text-muted);
    font-size: var(--tt-fs-body);
    line-height: 1.7;
}

.withdraw__warning {
    color: var(--tt-danger);
    font-size: var(--tt-fs-body);
    line-height: 1.7;
    margin-bottom: var(--tt-space-6);
}

.withdraw__check {
    display: flex;
    align-items: center;
    gap: var(--tt-space-2);
    font-size: var(--tt-fs-body);
    color: var(--tt-text);
    margin-bottom: var(--tt-space-4);
    cursor: pointer;
}

.withdraw__error {
    color: var(--tt-danger);
    font-size: var(--tt-fs-caption);
    margin-bottom: var(--tt-space-3);
}

.withdraw__actions {
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-2);
}
</style>
