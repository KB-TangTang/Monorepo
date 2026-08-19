<!--
  투표 플로우 ① — 봉투 개봉 → 변론서/증거 열람 → 유·무죄 선택 → 확인 시트 → 최종 모달.
  /group-challenges/:id/vote/:indictmentId 로 라우팅된다.
-->
<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import DefenseCourtHeader from '@/components/challenge/group/DefenseCourtHeader.vue';
import BaseBottomSheet from '@/components/common/BaseBottomSheet.vue';
import BaseModal from '@/components/common/BaseModal.vue';
import BaseButton from '@/components/common/BaseButton.vue';
import { fetchTrialDetail, submitVote as postVote } from '@/api/groupChallenge';
import { canVote, toVoteScreen, verdictRouteName } from '@/utils/groupTrial';
import judgingImg from '@/assets/images/emotions/48_judging.png';
import verdictImg from '@/assets/images/emotions/49_verdict.png';

const route = useRoute();
const router = useRouter();

const trialParams = computed(() => ({
    id: route.params.id,
    indictmentId: route.params.indictmentId,
}));

/* ── 재판 상세 로드 ────────────────────── */
const detail = ref(null);

/*
 * 투표할 수 없는 재판이면 다른 화면으로 보낸다.
 *
 * **이미 확정된 재판은 판결 화면으로 보낸다**(이슈 #172). 개표 배치가 생기기 전에는 확정된
 * 재판이 없어 전부 진행 현황으로 보냈는데, 알림을 늦게 눌러 개표가 끝난 뒤 들어온 사람에게는
 * 그게 「타임라인만 보고 판결은 못 보는」 막다른 길이 된다.
 * 아직 투표 중인데 내가 이미 던졌다면 진행 현황이 맞다.
 */
onMounted(async () => {
    let loaded;
    try {
        loaded = await fetchTrialDetail(route.params.indictmentId);
    } catch {
        /* 참여자가 아니거나 없는 재판이다. 서버가 존재 자체를 알려주지 않으므로 홈으로 되돌린다. */
        router.replace({ name: 'groupChallenge' });
        return;
    }

    if (!canVote(loaded)) {
        router.replace({
            name: verdictRouteName(loaded) ?? 'trialProgress',
            params: trialParams.value,
        });
        return;
    }

    detail.value = toVoteScreen(loaded);
    syncRemaining();
    countdownTimer = setInterval(syncRemaining, 1000);
});

/* ── 마감 카운트다운 ───────────────────── */
const remainingSeconds = ref(0);
let countdownTimer = null;

/*
 * 매 초 1씩 빼지 않고 **절대 마감시각과의 차이를 다시 계산한다.** 모바일은 화면이 꺼지면
 * setInterval 을 멈추므로, 빼기만 하면 잠깐 잠들었다 돌아온 사용자에게 남은 시간이 부풀어 보인다.
 */
function syncRemaining() {
    const deadline = detail.value?.voteDeadline;
    if (!deadline) {
        remainingSeconds.value = 0;
        return;
    }
    const diff = Math.floor((new Date(deadline).getTime() - Date.now()) / 1000);
    remainingSeconds.value = Math.max(0, diff);
}

onUnmounted(() => {
    clearInterval(countdownTimer);
    clearTimeout(phaseTimer);
    clearTimeout(toastTimer);
});

const deadlineLabel = computed(() => {
    const s = remainingSeconds.value;
    const hh = String(Math.floor(s / 3600)).padStart(2, '0');
    const mm = String(Math.floor((s % 3600) / 60)).padStart(2, '0');
    const ss = String(s % 60).padStart(2, '0');
    return `마감 ${hh}:${mm}:${ss}`;
});

/* ── phase 상태 머신 ───────────────────── */
const phase = ref('drop'); // 'drop' → 'opening' → 'doc'
let phaseTimer = null;

function openEnvelope() {
    if (phase.value !== 'drop') return;
    phase.value = 'opening';
    phaseTimer = setTimeout(() => {
        phase.value = 'doc';
    }, 1650);
}

/* ── 문서 페이지 네비 ──────────────────── */
const docPage = ref(1);
const totalPages = computed(() => {
    if (!detail.value?.evidences?.length) return 1;
    return 2;
});

/* ── 증거 줌 오버레이 ──────────────────── */
const zoomEvidence = ref(null);

/* ── 판결 선택 ─────────────────────────── */
const verdict = ref(null); // 'INNOCENT' | 'GUILTY'

const isInnocent = computed(() => verdict.value === 'INNOCENT');
const isGuilty = computed(() => verdict.value === 'GUILTY');

function pickInnocent() { verdict.value = 'INNOCENT'; }
function pickGuilty() { verdict.value = 'GUILTY'; }

const verdictKo = computed(() => {
    if (isInnocent.value) return '무죄';
    if (isGuilty.value) return '유죄';
    return '';
});

/* ── 확인 바텀시트 ─────────────────────── */
const confirmSheetRef = ref(null);
const finalModalRef = ref(null);
const showConfirmSheet = ref(false);
const comment = ref('');
const MAX_COMMENT = 40;

function openConfirmSheet() {
    if (!verdict.value) return;
    showConfirmSheet.value = true;
}

function resetVerdict() {
    showConfirmSheet.value = false;
    verdict.value = null;
}

/* ── 최종 확인 모달 ────────────────────── */
const showFinalModal = ref(false);

function openFinalModal() {
    showFinalModal.value = true;
}

const submitting = ref(false);

/*
 * 던지고 나서 화면을 넘긴다. 히스토리 되감기를 먼저 하면 서버가 거절했을 때 되돌아올 자리가 없다.
 */
async function submitVote() {
    if (submitting.value) return;
    submitting.value = true;

    try {
        await postVote(route.params.indictmentId, {
            verdict: verdict.value,
            comment: comment.value,
        });
    } catch (err) {
        submitting.value = false;
        closeOverlays();
        /*
         * 이미 던졌거나 투표 기간이 지났다. 같은 화면에 붙잡아 두면 계속 다시 누른다 —
         * 안내하고 진행 현황으로 보낸다. `VOTE_NOT_ALLOWED` 는 마감 후 제출에도 온다.
         */
        if (['VOTE_ALREADY_EXISTS', 'VOTE_NOT_ALLOWED', 'CANNOT_VOTE_OWN_TRIAL'].includes(err.code)) {
            flash(err.message ?? '투표할 수 없는 재판이에요.');
            setTimeout(() => {
                router.replace({ name: 'trialProgress', params: trialParams.value });
            }, 1200);
            return;
        }
        flash(err.message ?? '판결 제출에 실패했어요. 잠시 후 다시 시도해주세요.');
        return;
    }

    closeOverlays();

    /* 오버레이가 pushState 한 히스토리 항목 2개를 되돌려 VoteVerdict 위치로 복귀한 뒤,
     * VoteVerdict 을 VoteDone 으로 교체한다.
     * 이래야 뒤로가기 시 투표 화면이 다시 나타나지 않는다. */
    const params = trialParams.value;
    window.addEventListener('popstate', () => {
        router.replace({ name: 'voteDone', params });
    }, { once: true });
    window.history.go(-2);
}

function closeOverlays() {
    finalModalRef.value?.releaseHistory?.();
    confirmSheetRef.value?.releaseHistory?.();
    showFinalModal.value = false;
    showConfirmSheet.value = false;
}

/* ── 토스트 ────────────────────────────── */
const toast = ref(null);
let toastTimer = null;

function flash(message) {
    clearTimeout(toastTimer);
    toast.value = message;
    toastTimer = setTimeout(() => { toast.value = null; }, 2400);
}

/* ── 표시 포맷 ─────────────────────────── */
/** 금액은 BigDecimal 이라 문자열로 올 수 있다. `null` 은 「없음」이라 0 으로 채우지 않는다. */
function won(amount) {
    if (amount === null || amount === undefined) return '';
    return Number(amount).toLocaleString();
}

function formatDefenseDate(iso) {
    if (!iso) return '';
    const d = new Date(iso);
    if (Number.isNaN(d.getTime())) return '';
    return `${d.getFullYear()}년 ${d.getMonth() + 1}월 ${d.getDate()}일`;
}

/** 인장에 찍히는 한 글자. 서버는 닉네임만 준다. */
const defendantInitial = computed(() => detail.value?.defendant?.nickname?.charAt(0) ?? '');
</script>

<template>
    <div v-if="detail" class="vote-page">
        <!-- ===== 다크 헤더 ===== -->
        <DefenseCourtHeader>
            <template #nav-right>
                <span class="vote-page__deadline">{{ deadlineLabel }}</span>
            </template>

            <div class="vote-page__header-body">
                <div class="vote-page__header-text">
                    <div class="vote-page__header-tag">TRIAL</div>
                    <h2 class="vote-page__header-title">
                        {{ detail.defendant.nickname }}님의 변론을<br>확인해주세요
                    </h2>
                </div>
                <img
                    :src="judgingImg"
                    alt="판사 탕이"
                    class="vote-page__header-mascot"
                >
            </div>
        </DefenseCourtHeader>

        <!-- ===== 본문 ===== -->
        <div class="vote-page__body">

            <!-- Phase: drop (봉투 떨어짐) -->
            <div v-if="phase === 'drop'" class="vote-page__env" @click="openEnvelope">
                <div class="vote-page__env-drop">
                    <div class="vote-page__envelope">
                        <!-- 봉투 몸통 -->
                        <div class="vote-page__env-shadow"></div>
                        <div class="vote-page__env-body">
                            <div class="vote-page__env-line" style="top:118px"></div>
                            <div class="vote-page__env-line" style="top:132px"></div>
                            <div class="vote-page__env-line vote-page__env-line--short" style="top:146px"></div>
                            <div class="vote-page__env-label">변론서 재중</div>
                            <div class="vote-page__env-stamp">재판 문서</div>
                        </div>
                        <!-- 봉투 뚜껑 -->
                        <div class="vote-page__env-flap">
                            <div class="vote-page__env-seal vote-page__env-seal--top"></div>
                        </div>
                        <!-- 실 -->
                        <div class="vote-page__env-string vote-page__env-string--left"></div>
                        <div class="vote-page__env-string vote-page__env-string--right"></div>
                        <div class="vote-page__env-seal vote-page__env-seal--mid"></div>
                        <div class="vote-page__env-seal vote-page__env-seal--bot"></div>
                    </div>
                </div>
                <div class="vote-page__hint">
                    <svg class="vote-page__hint-icon" width="24" height="24" viewBox="0 0 24 24" fill="currentColor">
                        <path d="M9 11.24V7.5a2.5 2.5 0 0 1 5 0v3.74c1.21-.81 2-2.18 2-3.74a4.5 4.5 0 0 0-9 0c0 1.56.79 2.93 2 3.74zm9.84 4.63-4.54-2.26c-.17-.07-.35-.11-.54-.11H13v-6a.5.5 0 0 0-1 0v8.76l-3.47-.7c-.16-.03-.33 0-.46.09l-.97.65 4.2 4.2c.18.18.43.29.69.29H17c.69 0 1.25-.56 1.25-1.25v-2.59c0-.5-.29-.94-.72-1.14l-.69-.34z"/>
                    </svg>
                    <span class="vote-page__hint-text">봉투를 눌러 변론서를 확인해주세요</span>
                </div>
            </div>

            <!-- Phase: opening (개봉 애니메이션) -->
            <div v-else-if="phase === 'opening'" class="vote-page__env vote-page__env--opening">
                <div class="vote-page__envelope vote-page__envelope--opened">
                    <div class="vote-page__env-shadow"></div>
                    <div class="vote-page__env-body">
                        <div class="vote-page__env-body-top"></div>
                        <div class="vote-page__env-line" style="top:118px"></div>
                        <div class="vote-page__env-line" style="top:132px"></div>
                        <div class="vote-page__env-line vote-page__env-line--short" style="top:146px"></div>
                        <div class="vote-page__env-label">변론서 재중</div>
                        <div class="vote-page__env-stamp">재판 문서</div>
                        <div class="vote-page__env-seal vote-page__env-seal--bot"></div>
                    </div>
                    <!-- 뚜껑 열림 -->
                    <div class="vote-page__env-flap vote-page__env-flap--open">
                        <div class="vote-page__env-seal vote-page__env-seal--top"></div>
                    </div>
                    <!-- 편지지 올라옴 -->
                    <div class="vote-page__paper-clip">
                        <div class="vote-page__paper-out">
                            <div class="vote-page__paper-title">변론 요지서</div>
                            <div class="vote-page__paper-line"></div>
                            <div class="vote-page__paper-bar"></div>
                            <div class="vote-page__paper-bar"></div>
                            <div class="vote-page__paper-bar vote-page__paper-bar--short"></div>
                            <div class="vote-page__paper-bar" style="margin-top:14px"></div>
                            <div class="vote-page__paper-bar vote-page__paper-bar--half"></div>
                        </div>
                    </div>
                </div>
            </div>

            <!-- Phase: doc (변론서 + 투표) -->
            <template v-else-if="phase === 'doc'">
                <div class="vote-page__doc">
                    <!-- 페이지 1: 변론 요지서 -->
                    <div v-if="docPage === 1" class="vote-page__doc-page">
                        <div class="vote-page__doc-shadow"></div>
                        <div class="vote-page__doc-sheet">
                            <div class="vote-page__doc-heading">변 론 요 지 서</div>
                            <div class="vote-page__doc-meta">
                                <div>사&nbsp;&nbsp;&nbsp;&nbsp;건&nbsp;:&nbsp;{{ detail.challengeName }} · {{ detail.evalType === 'DAILY' ? '일일결산' : '기간평가' }}</div>
                                <div>피&nbsp;고&nbsp;인&nbsp;:&nbsp;{{ detail.defendant.nickname }}&nbsp;&nbsp;<span class="vote-page__doc-muted">(초과액 {{ won(detail.exceededAmount) }}원)</span></div>
                            </div>

                            <!--
                              변론 마감 배치가 변론 없이 상태만 VOTING 으로 넘긴 재판이다.
                              변론서를 지어내지 않고 그 사실을 그대로 적는다 — 투표는 그대로 받는다.
                            -->
                            <template v-if="!detail.hasDefense">
                                <div class="vote-page__doc-intro">피고인은 변론 기간 내에 변론서를 제출하지 않았습니다.</div>
                                <div class="vote-page__doc-label">다&nbsp;음</div>
                                <div class="vote-page__doc-para">
                                    <span class="vote-page__doc-num">1.</span>
                                    <span>기소 거래는 {{ won(detail.currentAmount) }}원으로 기준 {{ won(detail.limitAmount) }}원을
                                        <b class="vote-page__doc-text--red">{{ won(detail.exceededAmount) }}원 초과</b>했습니다.</span>
                                </div>
                                <div class="vote-page__doc-para">
                                    <span class="vote-page__doc-num">2.</span>
                                    <span class="vote-page__doc-muted">제출된 변론과 증거가 없습니다. 기소 내용만으로 판결해주세요.</span>
                                </div>
                            </template>

                            <template v-else>
                                <div class="vote-page__doc-intro">위 사건에 관하여 피고인은 다음과 같이 변론합니다.</div>
                                <div class="vote-page__doc-label">다&nbsp;음</div>
                                <div class="vote-page__doc-para">
                                    <span class="vote-page__doc-num">1.</span>
                                    <span class="vote-page__doc-text--bold">{{ detail.defenseMessage }}</span>
                                </div>
                                <div class="vote-page__doc-para">
                                    <span class="vote-page__doc-num">2.</span>
                                    <span>기소 거래는 {{ won(detail.currentAmount) }}원이며,
                                        <b class="vote-page__doc-text--bold">실제 부담금 {{ won(detail.actualCostAmount) }}원</b> 인정 시
                                        <template v-if="Number(detail.actualCostAmount) <= Number(detail.limitAmount)">
                                            기준 내 · <b class="vote-page__doc-text--green">{{ won(detail.limitAmount - detail.actualCostAmount) }}원 여유</b>입니다.
                                        </template>
                                        <template v-else>
                                            기준 초과 · <b class="vote-page__doc-text--red">{{ won(detail.actualCostAmount - detail.limitAmount) }}원 초과</b>입니다.
                                        </template>
                                    </span>
                                </div>
                            </template>

                            <div class="vote-page__doc-spacer"></div>
                            <div class="vote-page__doc-date">{{ formatDefenseDate(detail.defenseDate) }}</div>
                            <div class="vote-page__doc-sign">
                                피고인&nbsp;&nbsp;{{ detail.defendant.nickname }}<span class="vote-page__doc-seal">{{ defendantInitial }}</span>
                            </div>
                            <div class="vote-page__doc-court">탕탕 법정 귀중</div>
                        </div>
                    </div>

                    <!-- 페이지 2: 증거 자료 -->
                    <div v-if="docPage === 2" class="vote-page__doc-page">
                        <div class="vote-page__doc-shadow"></div>
                        <div class="vote-page__doc-sheet">
                            <div class="vote-page__doc-heading">증 거 자 료</div>
                            <div class="vote-page__doc-ev-desc">피고인이 제출한 증빙 {{ detail.evidences.length }}건. 이미지를 눌러 크게 볼 수 있어요.</div>
                            <div
                                v-for="ev in detail.evidences"
                                :key="ev.id"
                                class="vote-page__doc-ev"
                            >
                                <div class="vote-page__doc-ev-label">{{ ev.label }}</div>
                                <div
                                    class="vote-page__doc-ev-thumb"
                                    @click="zoomEvidence = ev"
                                >
                                    <img :src="ev.url" :alt="ev.label" class="vote-page__doc-ev-img">
                                </div>
                            </div>
                            <div class="vote-page__doc-spacer"></div>
                            <div class="vote-page__doc-ev-footer">이상 증거자료 {{ detail.evidences.length }}건 제출을 마칩니다.</div>
                        </div>
                    </div>
                </div>

                <!-- 페이지 네비게이션 + 판결 선택 -->
                <div class="vote-page__controls">
                    <div v-if="totalPages > 1" class="vote-page__pager">
                        <button
                            type="button"
                            class="vote-page__pager-btn"
                            :class="{ 'vote-page__pager-btn--disabled': docPage === 1 }"
                            @click="docPage = Math.max(1, docPage - 1)"
                        >
                            <svg width="19" height="19" viewBox="0 0 24 24" fill="currentColor"><path d="M15.41 7.41 14 6l-6 6 6 6 1.41-1.41L10.83 12z"/></svg>
                        </button>
                        <span class="vote-page__pager-label">{{ docPage }} / {{ totalPages }}</span>
                        <button
                            type="button"
                            class="vote-page__pager-btn"
                            :class="{ 'vote-page__pager-btn--disabled': docPage === totalPages }"
                            @click="docPage = Math.min(totalPages, docPage + 1)"
                        >
                            <svg width="19" height="19" viewBox="0 0 24 24" fill="currentColor"><path d="M10 6 8.59 7.41 13.17 12l-4.58 4.59L10 18l6-6z"/></svg>
                        </button>
                    </div>

                    <div class="vote-page__verdict-label">나의 판결</div>
                    <div class="vote-page__verdict-row">
                        <button
                            type="button"
                            class="vote-page__verdict-btn vote-page__verdict-btn--innocent"
                            :class="{ 'vote-page__verdict-btn--selected': isInnocent, 'vote-page__verdict-btn--faded': isGuilty }"
                            @click="pickInnocent"
                        >
                            무죄
                        </button>
                        <button
                            type="button"
                            class="vote-page__verdict-btn vote-page__verdict-btn--guilty"
                            :class="{ 'vote-page__verdict-btn--selected': isGuilty, 'vote-page__verdict-btn--faded': isInnocent }"
                            @click="pickGuilty"
                        >
                            유죄
                        </button>
                    </div>
                    <button
                        type="button"
                        class="vote-page__submit"
                        :class="{ 'vote-page__submit--active': verdict }"
                        :disabled="!verdict"
                        @click="openConfirmSheet"
                    >
                        판결 제출하기
                    </button>
                </div>
            </template>
        </div>

        <!-- ===== 증거 줌 오버레이 ===== -->
        <Teleport to="body">
            <Transition name="tt-modal">
                <div
                    v-if="zoomEvidence"
                    class="vote-page__zoom"
                    @click="zoomEvidence = null"
                >
                    <div class="vote-page__zoom-close">
                        <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><path d="M19 6.41 17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/></svg>
                    </div>
                    <div class="vote-page__zoom-img">
                        <img :src="zoomEvidence.url" :alt="zoomEvidence.label" class="vote-page__zoom-file">
                    </div>
                    <div class="vote-page__zoom-label">{{ zoomEvidence.label }}</div>
                    <div class="vote-page__zoom-hint">화면을 누르면 닫혀요</div>
                </div>
            </Transition>
        </Teleport>

        <!-- ===== 확인 바텀시트 ===== -->
        <BaseBottomSheet ref="confirmSheetRef" v-model="showConfirmSheet">
            <div class="vote-page__confirm">
                <div class="vote-page__confirm-top">
                    <img
                        :src="verdictImg"
                        alt="탕이"
                        class="vote-page__confirm-mascot"
                    >
                    <div class="vote-page__confirm-info">
                        <span class="vote-page__confirm-badge">번복 불가</span>
                        <div class="vote-page__confirm-title">
                            <span :class="isInnocent ? 'vote-page__confirm-title--blue' : 'vote-page__confirm-title--red'">{{ verdictKo }}</span>로 판결할까요?
                        </div>
                    </div>
                </div>

                <div class="vote-page__confirm-comment">
                    <div class="vote-page__confirm-comment-label">
                        한 줄 코멘트 <span class="vote-page__confirm-comment-sub">· 선택 · 익명으로 공개돼요</span>
                    </div>
                    <div class="vote-page__confirm-comment-field">
                        <input
                            v-model="comment"
                            type="text"
                            class="vote-page__confirm-comment-input"
                            placeholder="피고에게 남길 한 마디를 적어주세요"
                            :maxlength="MAX_COMMENT"
                        >
                        <span class="vote-page__confirm-comment-count">{{ comment.length }}/{{ MAX_COMMENT }}</span>
                    </div>
                </div>
            </div>

            <template #footer>
                <button
                    type="button"
                    class="vote-page__confirm-btn vote-page__confirm-btn--reset"
                    @click="resetVerdict"
                >
                    다시 고르기
                </button>
                <button
                    type="button"
                    class="vote-page__confirm-btn vote-page__confirm-btn--submit"
                    @click="openFinalModal"
                >
                    제출하기
                </button>
            </template>
        </BaseBottomSheet>

        <!-- ===== 최종 확인 모달 ===== -->
        <BaseModal
            ref="finalModalRef"
            v-model="showFinalModal"
            :show-close="false"
            :close-on-overlay="false"
        >
            <div class="vote-page__final">
                <h3 class="vote-page__final-title">정말 제출하시겠습니까?</h3>
                <p class="vote-page__final-desc">판결은 번복할 수 없습니다.</p>
            </div>
            <template #footer>
                <BaseButton
                    variant="ghost"
                    size="lg"
                    block
                    @click="showFinalModal = false"
                >
                    취소
                </BaseButton>
                <BaseButton
                    variant="dark"
                    size="lg"
                    block
                    :disabled="submitting"
                    @click="submitVote"
                >
                    {{ submitting ? '제출 중…' : '확인' }}
                </BaseButton>
            </template>
        </BaseModal>

        <!-- ===== 토스트 (제출 실패 안내) ===== -->
        <Teleport to="body">
            <Transition name="tt-toast">
                <div v-if="toast" class="vote-page__toast">{{ toast }}</div>
            </Transition>
        </Teleport>
    </div>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Noto+Serif+KR:wght@400;600;700&display=swap');

.vote-page {
    height: 100vh;
    height: 100dvh;
    background: var(--tt-bg-subtle);
    display: flex;
    flex-direction: column;
    overflow: hidden;
}

/* ── 헤더 ─────────────────────────────── */
.vote-page__deadline {
    background: rgba(224, 102, 75, 0.2);
    color: #FF9E86;
    font-size: 11.5px;
    font-weight: var(--tt-fw-black);
    padding: 5px 12px;
    border-radius: var(--tt-radius-full);
    font-family: var(--tt-font-mono);
}

.vote-page__header-body {
    display: flex;
    align-items: flex-end;
    justify-content: space-between;
    gap: 6px;
}

.vote-page__header-tag {
    font-size: 11px;
    font-weight: var(--tt-fw-black);
    letter-spacing: 0.14em;
    color: var(--tt-accent);
}

.vote-page__header-title {
    font-size: 21px;
    font-weight: var(--tt-fw-black);
    color: var(--tt-white);
    letter-spacing: -0.01em;
    line-height: 1.3;
    margin-top: 5px;
}

.vote-page__header-mascot {
    width: 64px;
    height: 64px;
    object-fit: contain;
    margin-bottom: -4px;
    flex: none;
    filter: drop-shadow(0 8px 12px rgba(0, 0, 0, 0.28));
}

/* ── 본문 ─────────────────────────────── */
.vote-page__body {
    flex: 1;
    min-height: 0;
    display: flex;
    flex-direction: column;
}

/* ── Phase: drop ─────────────────────── */
.vote-page__env {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 30px;
    cursor: pointer;
    padding-bottom: 24px;
}

.vote-page__env--opening {
    cursor: default;
    animation: tt-fadeout 0.35s ease 1.3s both;
}

.vote-page__env-drop {
    animation: tt-envdrop 1.1s cubic-bezier(0.32, 1.06, 0.44, 1) both;
}

.vote-page__envelope {
    position: relative;
    width: 180px;
    height: 200px;
}

.vote-page__env-shadow {
    position: absolute;
    left: 6px;
    right: 6px;
    bottom: -16px;
    height: 18px;
    border-radius: 50%;
    background: rgba(35, 40, 66, 0.18);
    filter: blur(7px);
}

.vote-page__env-body {
    position: absolute;
    inset: 0;
    background: linear-gradient(180deg, #EFBE68, #E4AA4C);
    border-radius: 10px;
    box-shadow: 0 16px 30px -14px rgba(140, 96, 20, 0.55);
}

.vote-page__env-body-top {
    position: absolute;
    left: 0;
    right: 0;
    top: 0;
    height: 9px;
    background: rgba(90, 55, 10, 0.22);
    border-radius: 10px 10px 0 0;
}

.vote-page__env-line {
    position: absolute;
    left: 40px;
    right: 40px;
    height: 7px;
    background: rgba(160, 105, 25, 0.32);
    border-radius: 4px;
}

.vote-page__env-line--short {
    left: 66px;
    right: 66px;
}

.vote-page__env-label {
    position: absolute;
    left: 0;
    right: 0;
    top: 166px;
    text-align: center;
    font-size: 9.5px;
    font-weight: var(--tt-fw-black);
    letter-spacing: 0.3em;
    color: rgba(120, 76, 14, 0.55);
}

.vote-page__env-stamp {
    position: absolute;
    left: 13px;
    top: 88px;
    transform: rotate(-7deg);
    border: 1.5px solid rgba(194, 75, 49, 0.75);
    color: rgba(194, 75, 49, 0.85);
    font-size: 8.5px;
    font-weight: var(--tt-fw-black);
    letter-spacing: 0.16em;
    padding: 3px 6px;
    border-radius: 3px;
}

.vote-page__env-flap {
    position: absolute;
    left: 0;
    right: 0;
    top: 0;
    height: 74px;
    background: linear-gradient(180deg, #E3A94D, #D89C3F);
    clip-path: polygon(0 0, 100% 0, 100% 52%, 54% 100%, 46% 100%, 0 52%);
    border-radius: 10px 10px 0 0;
    filter: drop-shadow(0 3px 3px rgba(120, 76, 14, 0.25));
}

.vote-page__env-flap--open {
    transform-origin: top center;
    animation: tt-flapopen 0.5s ease-in both;
    z-index: 3;
}

.vote-page__env-string {
    position: absolute;
    left: 50%;
    top: 64px;
    width: 2px;
    height: 32px;
    margin-left: -1px;
    background: #7A6A52;
    border-radius: 2px;
}

.vote-page__env-string--left { transform: rotate(22deg); }
.vote-page__env-string--right { transform: rotate(-22deg); }

.vote-page__env-seal {
    position: absolute;
    width: 17px;
    height: 17px;
    border-radius: 50%;
    background: radial-gradient(circle at 35% 30%, #C2B29A, #8F7C61);
    box-shadow: 0 1px 2px rgba(0, 0, 0, 0.25);
}

.vote-page__env-seal--top {
    left: 50%;
    top: 58px;
    margin-left: -8.5px;
}

.vote-page__env-seal--mid {
    left: 50%;
    top: 64px;
    margin-left: -8.5px;
}

.vote-page__env-seal--bot {
    left: 50%;
    top: 94px;
    margin-left: -8.5px;
}

/* 터치 힌트 */
.vote-page__hint {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 5px;
    animation: tt-taphint 1.7s ease-in-out infinite;
}

.vote-page__hint-icon {
    font-size: 24px;
    color: var(--tt-text-muted);
}

.vote-page__hint-text {
    font-size: 12.5px;
    font-weight: var(--tt-fw-black);
    color: var(--tt-text-muted);
}

/* ── Phase: opening — 편지지 ──────────── */
.vote-page__envelope--opened {
    perspective: 620px;
}

.vote-page__envelope--opened .vote-page__env-body {
    z-index: 2;
}

.vote-page__paper-clip {
    position: absolute;
    left: 15px;
    right: 15px;
    top: -156px;
    height: 158px;
    overflow: hidden;
    z-index: 4;
}

.vote-page__paper-out {
    position: absolute;
    left: 2px;
    right: 2px;
    top: 158px;
    height: 150px;
    background: var(--tt-white);
    border-radius: 4px;
    box-shadow: 0 -4px 14px rgba(0, 0, 0, 0.14);
    padding: 14px 12px;
    animation: tt-paperout 0.9s cubic-bezier(0.3, 0.9, 0.36, 1) 0.5s both;
}

.vote-page__paper-title {
    font-family: 'Noto Serif KR', serif;
    font-size: 9.5px;
    font-weight: 700;
    letter-spacing: 0.3em;
    text-align: center;
    color: #33333B;
}

.vote-page__paper-line {
    height: 1px;
    background: #33333B;
    margin-top: 7px;
}

.vote-page__paper-bar {
    height: 4px;
    background: #E7EAF0;
    border-radius: 3px;
    margin-top: 7px;
}

.vote-page__paper-bar:first-of-type {
    margin-top: 12px;
}

.vote-page__paper-bar--short { width: 72%; }
.vote-page__paper-bar--half { width: 56%; }

/* ── Phase: doc ──────────────────────── */
.vote-page__doc {
    flex: 1;
    min-height: 0;
    padding: 10px 16px 0;
    animation: tt-docin 0.55s cubic-bezier(0.22, 1, 0.36, 1) both;
    overflow-y: auto;
}

.vote-page__doc-page {
    position: relative;
    display: flex;
    margin-top: 11px;
    margin-bottom: 12px;
}

.vote-page__doc-shadow {
    position: absolute;
    left: 7px;
    right: 7px;
    top: -7px;
    bottom: 9px;
    background: #F5F2E9;
    border: 1px solid #E3DCCB;
    border-radius: 12px;
}

.vote-page__doc-sheet {
    position: relative;
    flex: 1 1 auto;
    width: 100%;
    min-width: 0;
    background: #FCFBF6;
    background-image:
        linear-gradient(rgba(62, 99, 214, 0.05) 1px, transparent 1px),
        linear-gradient(90deg, rgba(62, 99, 214, 0.05) 1px, transparent 1px);
    background-size: 15px 15px;
    border: 1px solid #E7E1D2;
    border-radius: 12px;
    box-shadow: 0 8px 26px -8px rgba(35, 40, 66, 0.16);
    padding: 18px;
    font-family: 'Noto Serif KR', serif;
    color: #33333B;
    display: flex;
    flex-direction: column;
}

.vote-page__doc-heading {
    text-align: center;
    font-size: 16.5px;
    font-weight: 700;
    letter-spacing: 0.3em;
    padding-bottom: 9px;
    border-bottom: 1.5px solid #33333B;
}

.vote-page__doc-meta {
    margin-top: 10px;
    font-size: 11.5px;
    line-height: 1.8;
}

.vote-page__doc-muted {
    color: #8A8378;
}

.vote-page__doc-intro {
    margin-top: 7px;
    font-size: 11.5px;
    line-height: 1.75;
}

.vote-page__doc-label {
    text-align: center;
    font-size: 12px;
    font-weight: 700;
    letter-spacing: 0.6em;
    margin: 8px 0 5px;
}

.vote-page__doc-para {
    font-size: 11.5px;
    line-height: 1.8;
    display: flex;
    gap: 4px;
    margin-top: 6px;
}

.vote-page__doc-para:first-of-type {
    margin-top: 0;
}

.vote-page__doc-num {
    flex: none;
}

.vote-page__doc-text--bold {
    font-weight: 700;
    color: #1F1F26;
}

.vote-page__doc-text--green {
    color: var(--tt-success);
}

.vote-page__doc-text--red {
    color: var(--tt-danger);
}

.vote-page__doc-spacer {
    flex: 1;
    min-height: 14px;
}

.vote-page__doc-date {
    text-align: center;
    font-size: 10.5px;
    letter-spacing: 0.24em;
    color: #5A554A;
}

.vote-page__doc-sign {
    margin-top: 5px;
    display: flex;
    justify-content: flex-end;
    align-items: center;
    gap: 8px;
    font-size: 11.5px;
}

.vote-page__doc-seal {
    width: 25px;
    height: 25px;
    border: 1.5px solid rgba(194, 75, 49, 0.8);
    border-radius: 50%;
    color: rgba(194, 75, 49, 0.85);
    font-size: 8.5px;
    font-weight: 700;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    transform: rotate(-10deg);
    flex: none;
}

.vote-page__doc-court {
    margin-top: 5px;
    font-size: 12.5px;
    font-weight: 700;
    letter-spacing: 0.08em;
}

/* 증거 자료 */
.vote-page__doc-ev-desc {
    margin-top: 9px;
    font-size: 11px;
    line-height: 1.7;
    color: #5A554A;
}

.vote-page__doc-ev {
    margin-top: 14px;
}

.vote-page__doc-ev-label {
    font-size: 11px;
    line-height: 1.7;
}

.vote-page__doc-ev-thumb {
    margin-top: 5px;
    height: 150px;
    border: 1px solid #DCD5C4;
    background: var(--tt-white);
    background-image: repeating-linear-gradient(135deg, #F2F0EA 0 8px, #FBFAF6 8px 16px);
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    border-radius: 6px;
}

/* 썸네일은 잘라 채우고(cover), 줌 오버레이에서 전체를 본다. */
.vote-page__doc-ev-img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    border-radius: 6px;
}

.vote-page__doc-ev-footer {
    font-size: 11px;
    color: #5A554A;
    line-height: 1.7;
}

/* ── 페이지 네비 + 판결 ──────────────── */
.vote-page__controls {
    flex: none;
    padding: 10px 20px 16px;
    background: var(--tt-bg-subtle);
}

.vote-page__pager {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 14px;
    margin-bottom: 9px;
}

.vote-page__pager-btn {
    width: 30px;
    height: 30px;
    border-radius: 50%;
    background: var(--tt-white);
    border: 1px solid var(--tt-border);
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    box-shadow: 0 3px 8px rgba(35, 40, 66, 0.06);
    font-family: inherit;
}

.vote-page__pager-btn .material-symbols-rounded {
    font-size: 19px;
    color: var(--tt-text-muted);
}

.vote-page__pager-btn--disabled {
    opacity: 0.35;
    cursor: default;
}

.vote-page__pager-label {
    font-size: 11.5px;
    font-weight: var(--tt-fw-black);
    color: var(--tt-text-muted);
    min-width: 44px;
    text-align: center;
}

.vote-page__verdict-label {
    font-size: 12px;
    font-weight: var(--tt-fw-black);
    color: var(--tt-text-muted);
    margin-bottom: 8px;
}

.vote-page__verdict-row {
    display: flex;
    gap: 9px;
}

.vote-page__verdict-btn {
    flex: 1;
    border-radius: 14px;
    padding: 15px 0;
    text-align: center;
    font-size: 16px;
    font-weight: var(--tt-fw-black);
    cursor: pointer;
    transition: all 0.15s ease;
    font-family: inherit;
}

.vote-page__verdict-btn--innocent {
    border: 1.5px solid #C9D6F5;
    background: var(--tt-white);
    color: #3E63D6;
}

.vote-page__verdict-btn--innocent.vote-page__verdict-btn--selected {
    border: 2px solid #3E63D6;
    background: #EAF0FF;
}

.vote-page__verdict-btn--guilty {
    border: 1.5px solid #F3D3C9;
    background: var(--tt-white);
    color: #C24B31;
}

.vote-page__verdict-btn--guilty.vote-page__verdict-btn--selected {
    border: 2px solid #C24B31;
    background: #FBE9E4;
}

.vote-page__verdict-btn--faded {
    opacity: 0.55;
}

.vote-page__submit {
    margin-top: 10px;
    width: 100%;
    background: #E9EAEE;
    color: #A6A9B6;
    font-weight: var(--tt-fw-black);
    font-size: 15px;
    padding: 14px;
    border-radius: 14px;
    text-align: center;
    border: none;
    cursor: not-allowed;
    font-family: inherit;
}

.vote-page__submit--active {
    background: var(--tt-surface-inverse);
    color: var(--tt-white);
    cursor: pointer;
    box-shadow: 0 10px 24px -12px rgba(35, 40, 66, 0.6);
}

/* ── 증거 줌 오버레이 ────────────────── */
.vote-page__zoom {
    position: fixed;
    inset: 0;
    background: rgba(16, 19, 32, 0.86);
    z-index: var(--tt-z-overlay);
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 14px;
    padding: 20px;
    cursor: pointer;
}

.vote-page__zoom-close {
    align-self: flex-end;
    width: 30px;
    height: 30px;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.14);
    display: flex;
    align-items: center;
    justify-content: center;
}

.vote-page__zoom-close .material-symbols-rounded {
    font-size: 18px;
    color: var(--tt-white);
}

.vote-page__zoom-img {
    width: 100%;
    height: 340px;
    border-radius: 8px;
    background: var(--tt-white);
    background-image: repeating-linear-gradient(135deg, #F2F0EA 0 10px, #FBFAF6 10px 20px);
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: 0 20px 50px -20px rgba(0, 0, 0, 0.7);
}

/* 증거는 영수증 사진이라 세로가 길다. 잘라내면 금액이 사라지므로 contain 으로 전부 보여준다. */
.vote-page__zoom-file {
    max-width: 100%;
    max-height: 100%;
    object-fit: contain;
}

/* ── 토스트 ───────────────────────────── */
.vote-page__toast {
    position: fixed;
    left: 22px;
    right: 22px;
    bottom: 96px;
    /* 모달·바텀시트보다 위에 떠야 한다. 제출 실패 안내는 그 둘이 열린 채로 나온다. */
    z-index: calc(var(--tt-z-modal, 60) + 1);
    background: rgba(35, 40, 66, 0.94);
    color: var(--tt-text-inverse);
    font-size: 12.5px;
    font-weight: var(--tt-fw-bold);
    line-height: 1.5;
    text-align: center;
    padding: 11px 16px;
    border-radius: var(--tt-radius-full);
    pointer-events: none;
}

.tt-toast-enter-active {
    animation: vote-toast-in 0.22s ease-out both;
}

.tt-toast-leave-active {
    animation: vote-toast-in 0.18s ease-in reverse both;
}

@keyframes vote-toast-in {
    0% { transform: translateY(10px); opacity: 0; }
    100% { transform: none; opacity: 1; }
}

.vote-page__zoom-label {
    font-size: 12px;
    font-weight: var(--tt-fw-black);
    color: var(--tt-white);
}

.vote-page__zoom-hint {
    font-size: 11px;
    color: rgba(255, 255, 255, 0.6);
}

/* ── 확인 바텀시트 ───────────────────── */
.vote-page__confirm {
    display: flex;
    flex-direction: column;
    gap: 13px;
}

.vote-page__confirm-top {
    display: flex;
    align-items: center;
    gap: 12px;
}

.vote-page__confirm-mascot {
    width: 56px;
    height: 56px;
    object-fit: contain;
    flex: none;
}

.vote-page__confirm-badge {
    display: inline-block;
    background: var(--tt-bg-fill, #EFF1F5);
    color: var(--tt-text-hint, #8A8FA3);
    font-size: 10.5px;
    font-weight: var(--tt-fw-black);
    padding: 4px 10px;
    border-radius: var(--tt-radius-full);
}

.vote-page__confirm-title {
    font-size: 17px;
    font-weight: var(--tt-fw-black);
    line-height: 1.35;
    margin-top: 6px;
}

.vote-page__confirm-title--blue { color: #3E63D6; }
.vote-page__confirm-title--red { color: #C24B31; }

.vote-page__confirm-comment {
    margin-top: 2px;
}

.vote-page__confirm-comment-label {
    font-size: 11.5px;
    font-weight: var(--tt-fw-black);
    color: var(--tt-text-muted);
    margin-bottom: 6px;
}

.vote-page__confirm-comment-sub {
    color: var(--tt-text-hint, #A6A9B6);
    font-weight: var(--tt-fw-bold);
}

.vote-page__confirm-comment-field {
    display: flex;
    align-items: center;
    gap: 6px;
    background: var(--tt-bg-subtle);
    border: 1px solid var(--tt-border);
    border-radius: 12px;
    padding: 2px 12px 2px 4px;
}

.vote-page__confirm-comment-input {
    flex: 1;
    min-width: 0;
    border: none;
    background: transparent;
    outline: none;
    font-size: 12.5px;
    font-weight: var(--tt-fw-semibold);
    color: var(--tt-text);
    padding: 11px 10px;
    font-family: inherit;
}

.vote-page__confirm-comment-input::placeholder {
    color: var(--tt-text-hint, #A6A9B6);
}

.vote-page__confirm-comment-count {
    font-size: 10.5px;
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-hint, #A6A9B6);
}

.vote-page__confirm-btn {
    flex: 1;
    padding: 13px 0;
    border-radius: 14px;
    font-weight: var(--tt-fw-black);
    text-align: center;
    cursor: pointer;
    font-family: inherit;
    font-size: 14px;
}

.vote-page__confirm-btn--reset {
    border: 1.5px solid #DDD9D0;
    background: var(--tt-white);
    color: var(--tt-text-muted);
}

.vote-page__confirm-btn--submit {
    flex: 1.4;
    border: none;
    background: var(--tt-surface-inverse);
    color: var(--tt-white);
    font-size: 14.5px;
    box-shadow: 0 10px 24px -12px rgba(35, 40, 66, 0.6);
}

/* ── 최종 확인 모달 ──────────────────── */
.vote-page__final {
    text-align: center;
    padding: var(--tt-space-2) 0;
}

.vote-page__final-title {
    font-size: var(--tt-fs-section);
    font-weight: var(--tt-fw-black);
}

.vote-page__final-desc {
    margin-top: var(--tt-space-2);
    font-size: var(--tt-fs-body);
    color: var(--tt-text-muted);
}

/* ── 애니메이션 ──────────────────────── */
@keyframes tt-envdrop {
    0%   { transform: translateY(-560px) rotate(-8deg); }
    55%  { transform: translateY(0) rotate(0deg); }
    70%  { transform: translateY(-26px) rotate(2deg); }
    84%  { transform: translateY(0); }
    93%  { transform: translateY(-9px); }
    100% { transform: translateY(0); }
}

@keyframes tt-flapopen {
    0%   { transform: rotateX(0deg); }
    100% { transform: rotateX(-170deg); }
}

@keyframes tt-paperout {
    0%   { transform: translateY(0); }
    100% { transform: translateY(-152px); }
}

@keyframes tt-fadeout {
    to { opacity: 0; }
}

@keyframes tt-docin {
    0%   { transform: translateY(26px) scale(0.965); opacity: 0; }
    100% { transform: none; opacity: 1; }
}

@keyframes tt-taphint {
    0%, 100% { opacity: 0.55; transform: translateY(0); }
    50%      { opacity: 1; transform: translateY(-4px); }
}

/* transition for zoom overlay */
.tt-modal-enter-active,
.tt-modal-leave-active {
    transition: opacity 0.18s ease;
}

.tt-modal-enter-from,
.tt-modal-leave-to {
    opacity: 0;
}
</style>
