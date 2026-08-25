<!--
  그룹 챌린지 상세 — 시작 전(S5/S6) + 진행 중(S1/S2) 통합 뷰.
  status 와 evalType 에 따라 조건부로 다른 섹션을 렌더링한다.
  /group-challenges/:id 로 라우팅된다.
-->
<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import {
    fetchGroupChallengeDetail,
    deleteGroupChallenge,
    fetchGroupTrialRecords,
} from '@/api/groupChallenge';
import { toTrialRecordCard } from '@/utils/groupTrial';

import BaseModal from '@/components/common/BaseModal.vue';
import ChallengePageHeader from '@/components/challenge/ChallengePageHeader.vue';
import GroupDetailInkCard from '@/components/challenge/group/GroupDetailInkCard.vue';
import GroupDetailLivesBar from '@/components/challenge/group/GroupDetailLivesBar.vue';
import GroupDetailPeriodBar from '@/components/challenge/group/GroupDetailPeriodBar.vue';
import GroupDetailPromise from '@/components/challenge/group/GroupDetailPromise.vue';
import GroupDetailMemberGrid from '@/components/challenge/group/GroupDetailMemberGrid.vue';
import GroupDetailMemberTable from '@/components/challenge/group/GroupDetailMemberTable.vue';
import GroupDetailTrialCarousel from '@/components/challenge/group/GroupDetailTrialCarousel.vue';
import GroupTrialRecordCard from '@/components/challenge/group/GroupTrialRecordCard.vue';
import GroupHonorCourtEntry from '@/components/challenge/group/GroupHonorCourtEntry.vue';

import { ChevronRightIcon } from '@heroicons/vue/24/solid';
import mascotChat from '@/assets/images/emotions/57_chat.png';
import { entryState, resolveBack } from '@/utils/groupChallengeNavigation';

const route = useRoute();
const router = useRouter();

const challenge = ref(null);
const loading = ref(true);

onMounted(async () => {
    try {
        challenge.value = await fetchGroupChallengeDetail(route.params.id);
    } catch (e) {
        /*
         * 이 catch 가 조용하면 화면이 「클릭이 씹힌 것」처럼 보인다 — replace 라 히스토리도 안 남아
         * 목록에서 카드를 눌러도 아무 일이 없는 것과 구분되지 않는다.
         * 실제로 이것 때문에 원인 추적이 오래 걸렸다(이슈 #271). 되돌리기 전에 반드시 남긴다.
         */
        console.error('[GroupChallengeDetail] 상세를 불러오지 못해 목록으로 되돌린다.', e);
        router.replace({ name: 'groupChallengeList' });
    } finally {
        loading.value = false;
    }

    if (challenge.value?.status === 'CLOSED') await loadTrialRecords();
});

const ch = computed(() => challenge.value);
const isRecruiting = computed(() => ch.value?.status === 'RECRUITING');
const isActive = computed(() => ch.value?.status === 'ACTIVE');
const isClosed = computed(() => ch.value?.status === 'CLOSED');
const isDaily = computed(() => ch.value?.evalType === 'DAILY');

/*
 * 종료 화면의 재판 기록 — 링크가 아니라 **목록을 펼쳐서** 보여준다.
 *
 * 「재판 기록 보기」 한 줄만 두면 끝난 챌린지에서 제일 궁금한 것(누가 무슨 판결을 받았나)이
 * 한 번 더 눌러야 나온다. 종료 화면에 남은 내용이 결과 카드·약속뿐이라 자리도 남는다.
 *
 * **최근 3건만 펼친다.** 7일 챌린지면 기소가 수십 건까지 가는데 전부 펼치면 하단 명예 법정이
 * 스크롤 밖으로 밀린다. 나머지는 기록 화면(`groupTrialRecords`)이 맡는다 — 범위 필터·요약이
 * 거기 있고, 여기에 또 만들면 같은 목록이 두 자리에서 어긋난다.
 */
const trialRecords = ref([]);
const recordsFailed = ref(false);

async function loadTrialRecords() {
    try {
        const list = await fetchGroupTrialRecords(route.params.id);
        trialRecords.value = list.map(toTrialRecordCard);
    } catch {
        /* 기록을 못 받아도 상세는 그대로 그린다. 대신 아래 링크 행으로 되돌아간다 */
        recordsFailed.value = true;
    }
}

const RECORD_PREVIEW = 3;
const recentTrialRecords = computed(() => trialRecords.value.slice(0, RECORD_PREVIEW));
const recordGuiltyCount = computed(
    () => trialRecords.value.filter((item) => item.verdict === 'GUILTY').length,
);
const recordInnocentCount = computed(() => trialRecords.value.length - recordGuiltyCount.value);

/* 상태 뱃지 */
const statusBadges = computed(() => {
    if (!ch.value) return [];
    if (isRecruiting.value) {
        const dBadge = isDaily.value
            ? { bg: 'var(--tt-gold-soft)', color: 'var(--tt-gold-deep)' }
            : { bg: '#EAF0FF', color: '#3E63D6' };
        return [
            { label: '시작 전', bg: 'var(--tt-bg-fill)', color: 'var(--tt-text-body)' },
            { label: `D-${ch.value.daysUntilStart}`, ...dBadge },
        ];
    }
    if (isClosed.value) {
        /*
         * 「승소 / 패소」를 붙이지 않는다. 종료된 챌린지에서 이 한 단어가 화면에 두 번(히어로
         * 배지 · 결과 카드) 나오는데, 그 아래 재판 기록에는 재판별 유무죄 도장이 또 줄줄이 있어
         * 같은 화면에서 「승소」와 「유죄」가 뒤섞여 읽혔다. 재판의 승패는 재판 기록이 말하고
         * 챌린지의 결과는 순위·절약액이 말한다.
         */
        return [{ label: '종료됨', bg: 'var(--tt-bg-fill)', color: 'var(--tt-text-muted)' }];
    }
    return [
        { label: '진행 중', bg: 'var(--tt-green-soft)', color: 'var(--tt-green)' },
        {
            label: `DAY ${ch.value.currentDay} / ${ch.value.totalDays}`,
            bg: 'var(--tt-ink)',
            color: 'var(--tt-gold)',
        },
    ];
});

/* 기간 설명 */
const periodText = computed(() => {
    if (!ch.value) return '';
    const s = formatDate(ch.value.startDate);
    const e = formatDate(ch.value.endDate);
    if (isRecruiting.value) {
        return `${s} ~ ${e} · ${ch.value.totalDays}일간`;
    }
    if (isClosed.value) {
        return `${s} ~ ${e} · ${ch.value.totalDays}일간 완료`;
    }
    return `${s} ~ ${e} · ${ch.value.settleTime || '매일 23:30 결산'}`;
});

function formatDate(iso) {
    const d = new Date(iso);
    return `${d.getMonth() + 1}월 ${d.getDate()}일`;
}

/*
 * 네비게이션
 *
 * 뒤로가기는 **진입한 화면이 바로 앞에 있다고 확인된 경우에만** 히스토리를 되돌린다(이슈 #303).
 * 확인은 홈·목록이 push 할 때 남긴 history.state 로 한다 — 규칙은 utils/groupChallengeNavigation.
 *
 * 무조건 `router.back()` 으로 바꾸면 안 된다. 판결 플로우가 `replace` 로 이 화면에 돌아오므로
 * 그때 앞 항목은 방금 본 재판 화면이다(이슈 #172 가 홈 하드코딩을 넣은 이유). 그 경우는
 * 표시가 없어 종전대로 홈으로 나간다.
 */
function goBack() {
    const target = resolveBack(window.history.state, 'groupChallenge');

    if (target.type === 'back') {
        router.back();
        return;
    }
    router.push({ name: target.name });
}

function goToList() {
    router.push({ name: 'groupChallengeList' });
}

function goToRanking() {
    router.push({ name: 'groupChallengeRanking', params: { id: ch.value.id } });
}

/*
 * 「재판 기록」은 명예 법정이 아니다.
 *
 * 이 화면의 「재판 기록」 진입 두 곳(종료 화면의 카드 · 진행 중 푸터 버튼)이 둘 다
 * `goToRanking` 을 부르고 있었다 — 이름은 기록인데 목적지는 순위였고, 그래서 확정된 재판을
 * 볼 방법이 앱 안에 없었다. 순위 진입은 하단 명예 법정 배너가 맡는다.
 */
function goToTrialRecords() {
    router.push({ name: 'groupTrialRecords', params: { id: ch.value.id } });
}

/*
 * 펼쳐 둔 기록 행에서 바로 판결문으로 들어간다. 목적지는 `toTrialRecordCard` 가
 * `verdictRouteName` 으로 정해 놓았다 — 여기서 다시 분기하면 AI 판결을 모르는 진입로가 생긴다.
 * (기록 화면 `GroupTrialRecordsView.vue:93` 과 같은 처리다)
 */
function openRecord({ item }) {
    if (!item.routeName) return;
    router.push({
        name: item.routeName,
        params: { id: item.groupId, indictmentId: item.id },
    });
}

function goToInvite() {
    router.push({
        name: 'groupChallengeInvite',
        params: { groupId: ch.value.id },
        /* 초대 화면의 뒤로가기가 홈이 아니라 이 상세로 돌아오게 한다(이슈 #303) */
        state: entryState('groupChallengeDetail'),
    });
}

function goToChat() {
    router.push({ name: 'groupChallengeChat', params: { id: ch.value.id } });
}

function goToDefense(item) {
    router.push({
        name: 'defenseViolation',
        params: { id: ch.value.id, indictmentId: item.id },
    });
}

function goToVote(item) {
    router.push({
        name: 'voteVerdict',
        params: { id: ch.value.id, indictmentId: item.id },
    });
}

function goToTrialProgress(item) {
    router.push({
        name: 'trialProgress',
        params: { id: ch.value.id, indictmentId: item.id },
    });
}

/*
 * 목록 카드와 같은 평평한 필드를 본다. 서버 DTO 에 중첩 chat 객체는 없다 —
 * 예전 목데이터가 쓰던 `chat.unreadCount` 를 보고 있어서 배지가 항상 0 이었다(이슈 #271).
 */
const unreadCount = computed(() => ch.value?.unreadChatCount ?? 0);

/*
 * 그룹 삭제 (이슈 #352).
 *
 * **방장 + 모집 중일 때만** 버튼을 띄운다. 시작한 뒤로는 남의 목숨·기소 기록까지 사라져서
 * 막았다. ACTIVE 이후를 열어 줄지 검토했으나 2026-08-20 팀 논의에서 **열지 않기로 확정했다** -
 * 챌린지가 최대 7일이라 오래 매여 있지 않고, 진행 중인 재판과 집계가 갑자기 사라지는 쪽이 더 나쁘다.
 * 화면에서 가려도 서버가 다시 검증한다 — 여기 조건은 편의일 뿐 방어선이 아니다.
 */
const showDeleteModal = ref(false);
const isDeleting = ref(false);
const deleteError = ref('');
const canDelete = computed(() => Boolean(ch.value?.isOwner) && isRecruiting.value);

const DELETE_ERROR_MESSAGES = {
    GROUP_NOT_OWNER: '방장만 챌린지를 삭제할 수 있어요.',
    GROUP_NOT_DELETABLE: '이미 시작된 챌린지는 삭제할 수 없어요.',
    GROUP_NOT_FOUND: '이미 사라진 챌린지예요.',
};

function openDeleteModal() {
    deleteError.value = '';
    showDeleteModal.value = true;
}

async function handleDelete() {
    if (isDeleting.value) return;
    isDeleting.value = true;
    deleteError.value = '';
    try {
        await deleteGroupChallenge(ch.value.id);
        showDeleteModal.value = false;
        /* 방금 지운 그룹의 상세가 히스토리에 남으면 뒤로가기가 없는 방으로 되돌아간다. */
        router.replace({ name: 'groupChallengeList' });
    } catch (e) {
        deleteError.value =
            DELETE_ERROR_MESSAGES[e.code] ?? e.message ?? '챌린지를 삭제하지 못했어요.';
    } finally {
        isDeleting.value = false;
    }
}
</script>

<template>
    <div v-if="loading" class="gc-detail gc-detail--loading">
        <div class="gc-detail__spinner"></div>
    </div>

    <div v-else-if="ch" class="gc-detail">
        <!-- ── 히어로 영역 (장식 원 + 헤더 + 상태) ── -->
        <div class="gc-detail__hero">
            <!--
                 장식 원은 진행 중·시작 전에만 둔다. 종료 화면은 결과를 읽는 자리라
                 우상단이 비어 있는 편이 낫다 — 원본 디자인의 결과 화면에도 없다.
            -->
            <div v-if="!isClosed" class="gc-detail__deco gc-detail__deco--lg"></div>

            <!--
                 내비게이션 헤더. 종료 화면에 있던 「최종 순위」 바로가기는 뺐다 —
                 순위 진입은 스크롤 하단 명예 법정 배너 하나로 모은다.
            -->
            <ChallengePageHeader title="그룹 챌린지" class="gc-detail__nav" @back="goBack" />

            <!-- 상태 뱃지 + 챌린지 이름 -->
            <div class="gc-detail__status">
                <div class="gc-detail__badges">
                    <span
                        v-for="(b, i) in statusBadges"
                        :key="i"
                        class="gc-detail__badge"
                        :style="{ background: b.bg, color: b.color }"
                    >
                        {{ b.label }}
                    </span>
                </div>
                <h2 class="gc-detail__name">{{ ch.groupName }}</h2>
                <p class="gc-detail__period">{{ periodText }}</p>
            </div>
        </div>

        <!-- ── 콘텐츠 영역 ── -->
        <div class="gc-detail__content">
            <!--
                 종료: 내 결과 + 약속 + 재판 기록 + 명예 법정.
                 순위(시상대·전체 피고인 현황)는 이 화면에 두지 않는다 — 명예 법정
                 (`/group-challenges/:id/ranking`)이 전담한다. 두 화면이 같은
                 `tbl_group_member` 를 같은 순서로 읽어 시상대와 「전체 피고인 현황」을
                 각자 그리고 있었고, 제목 문자열까지 같아 어느 쪽이 원본인지 알 수 없었다.
            -->
            <template v-if="isClosed">
                <!-- 최종 결과 요약 -->
                <div class="gc-detail__result-card">
                    <span class="gc-detail__result-title">최종 결과</span>
                    <div class="gc-detail__result-stats">
                        <div class="gc-detail__result-stat">
                            <span class="gc-detail__result-label">최종 순위</span>
                            <span class="gc-detail__result-value">
                                {{ ch.finalRank }}위
                                <span class="gc-detail__result-total"
                                    >/ {{ ch.memberCount }}명</span
                                >
                            </span>
                        </div>
                        <div v-if="ch.savingsAmount" class="gc-detail__result-stat">
                            <span class="gc-detail__result-label">절약 금액</span>
                            <span class="gc-detail__result-value gc-detail__result-value--green">
                                {{ ch.savingsAmount.toLocaleString() }}원
                            </span>
                        </div>
                        <div v-if="ch.finalChargeAmount > 0" class="gc-detail__result-stat">
                            <span class="gc-detail__result-label">누적 혐의액</span>
                            <span class="gc-detail__result-value gc-detail__result-value--red">
                                {{ ch.finalChargeAmount.toLocaleString() }}원
                            </span>
                        </div>
                    </div>
                </div>

                <!-- 약속 아코디언 -->
                <GroupDetailPromise
                    v-if="ch.memo"
                    :memo="ch.memo"
                    :memo-author="ch.memoAuthor"
                    :memo-date="ch.memoDate"
                />

                <!--
                     재판 기록 — 최근 3건을 펼쳐 둔다. 목록·행 클릭은 기록 화면과 같은
                     `GroupTrialRecordCard` 가 그린다(행 구조가 두 벌이 되지 않게).
                -->
                <section v-if="trialRecords.length" class="gc-detail__records">
                    <div class="gc-detail__records-head">
                        <h3 class="gc-detail__records-title">재판 기록</h3>
                        <span class="gc-detail__records-count">
                            <span class="gc-detail__records-total"
                                >총 {{ trialRecords.length }}건</span
                            >
                            <span class="gc-detail__records-guilty"
                                >유죄 {{ recordGuiltyCount }}</span
                            >
                            <span class="gc-detail__records-dot">·</span>
                            <span class="gc-detail__records-innocent"
                                >무죄 {{ recordInnocentCount }}</span
                            >
                        </span>
                    </div>

                    <GroupTrialRecordCard :items="recentTrialRecords" @open="openRecord" />

                    <!-- 3건을 넘길 때만 나간다. 다 보이는데 「전체 보기」가 있으면 헛걸음이다 -->
                    <button
                        v-if="trialRecords.length > recentTrialRecords.length"
                        type="button"
                        class="gc-detail__records-more"
                        @click="goToTrialRecords"
                    >
                        재판 기록 {{ trialRecords.length }}건 전체 보기
                        <ChevronRightIcon class="gc-detail__records-more-icon" />
                    </button>
                </section>

                <!--
                     기록 요청이 실패했을 때의 물러설 자리. 상세 응답의 집계(`trialStats`)는
                     이미 손에 있으니 진입만이라도 남긴다 — 여기까지 없애면 종료 화면에서
                     재판 기록으로 갈 방법이 사라진다.
                -->
                <div
                    v-else-if="recordsFailed && ch.trialStats && ch.trialStats.totalTrials > 0"
                    class="gc-detail__trial-link"
                    @click="goToTrialRecords"
                >
                    <div class="gc-detail__trial-link-icon">
                        <svg viewBox="0 0 24 24" fill="currentColor" width="19" height="19">
                            <path
                                d="M17.29 5.71a1 1 0 0 0-1.41 0L13.5 8.09l-1.8-1.8a1 1 0 0 0-1.41 1.42l.38.38-5.3 5.3a1 1 0 0 0 0 1.41l2.83 2.83a1 1 0 0 0 1.41 0l5.3-5.3.38.38a1 1 0 0 0 1.42-1.42l-1.8-1.8 2.38-2.37a1 1 0 0 0 0-1.41ZM9.7 15.22 7.78 13.3l4.6-4.6 1.91 1.92ZM18 19H3v2h15Z"
                            />
                        </svg>
                    </div>
                    <div class="gc-detail__trial-link-body">
                        <span class="gc-detail__trial-link-title">재판 기록 보기</span>
                        <span class="gc-detail__trial-link-sub">
                            이 챌린지 재판 {{ ch.trialStats.totalTrials }}건 · 무죄
                            {{ ch.trialStats.innocentCount }} · 유죄 {{ ch.trialStats.guiltyCount }}
                        </span>
                    </div>
                    <ChevronRightIcon class="gc-detail__trial-link-arrow" />
                </div>

                <!--
                     명예 법정은 스크롤 맨 아래다. 순위가 이 화면에서 빠졌어도 위로
                     올리지 않는다 — 이 화면의 주어는 「내 결과」고, 순위 배너를 먼저
                     읽히면 남과의 비교가 앞선다. 이제 순위로 가는 길은 여기 하나다.
                -->
                <GroupHonorCourtEntry class="gc-detail__honor" @open="goToRanking" />
            </template>

            <!-- 시작 전 / 진행 중 -->
            <template v-else>
                <!-- Ink 스탯 카드 -->
                <GroupDetailInkCard
                    :eval-type="ch.evalType"
                    :status="ch.status"
                    :limit-amount="ch.limitAmount"
                    :category-name="ch.categoryName"
                    :current-amount="ch.myDailyAmount"
                    :usage-percent="ch.myUsagePercent"
                    :remaining-amount="ch.myRemainingAmount"
                />

                <!-- 목숨 바 (일일결산) / 기간 카드 (기간결산) -->
                <!--
                  ?? 를 쓴다. 목숨 0은 「탈락」이라는 유효한 값이라 || 로 두면
                  전부 잃은 사람에게 만땅이 그려진다.
                  폴백이 남아 있는 건 목데이터(isMockMode) 때문이다 — 상세 목데이터 6·7 에
                  livesCount 가 없다. 실 API 는 /detail 이 비참여자를 GROUP_NOT_MEMBER 로
                  막으므로 참여자에게는 항상 정수가 온다.
                -->
                <GroupDetailLivesBar
                    v-if="isDaily"
                    :lives-count="ch.livesCount ?? ch.maxLives"
                    :max-lives="ch.maxLives"
                    :mode="isRecruiting ? 'upcoming' : 'active'"
                    :size="isRecruiting ? 21 : 19"
                />
                <GroupDetailPeriodBar
                    v-else
                    :start-date="ch.startDate"
                    :end-date="ch.endDate"
                    :total-days="ch.totalDays"
                    :mode="isRecruiting ? 'upcoming' : 'active'"
                    :current-day="ch.currentDay || 0"
                />

                <!-- 재판 캐러셀 (일일결산 진행 중 + 기소 건이 있을 때) -->
                <GroupDetailTrialCarousel
                    v-if="isActive && isDaily && ch.indictments?.length"
                    :indictments="ch.indictments"
                    @defend="goToDefense"
                    @vote="goToVote"
                    @trial="goToTrialProgress"
                />

                <!-- 약속 아코디언 -->
                <GroupDetailPromise
                    :memo="ch.memo"
                    :memo-author="ch.memoAuthor"
                    :memo-date="ch.memoDate"
                    :show-subtitle="isRecruiting"
                />

                <!-- 시작 전: 참여 멤버 그리드 -->
                <GroupDetailMemberGrid
                    v-if="isRecruiting"
                    :members="ch.members"
                    :member-count="ch.memberCount"
                    :max-members="ch.maxMembers"
                    @invite="goToInvite"
                />

                <!-- 진행 중: 멤버 소비 상태 테이블 -->
                <GroupDetailMemberTable
                    v-if="isActive && ch.dailyMembers"
                    :members="ch.dailyMembers"
                    :eval-type="ch.evalType"
                />

                <GroupHonorCourtEntry class="gc-detail__honor" @open="goToRanking" />
            </template>
        </div>

        <!-- ── 하단 CTA ── -->
        <div class="gc-detail__footer">
            <div class="gc-detail__actions">
                <button
                    v-if="isActive"
                    class="gc-detail__btn gc-detail__btn--secondary"
                    @click="goToInvite"
                >
                    소환
                </button>
                <button
                    v-if="isActive"
                    class="gc-detail__btn gc-detail__btn--primary"
                    @click="goToTrialRecords"
                >
                    재판 기록
                </button>
                <button
                    v-if="!isActive"
                    class="gc-detail__btn gc-detail__btn--outline"
                    @click="goToList"
                >
                    그룹 목록으로
                </button>
                <button
                    v-if="canDelete"
                    class="gc-detail__btn gc-detail__btn--danger"
                    @click="openDeleteModal"
                >
                    삭제
                </button>
            </div>
        </div>

        <!-- 삭제 확인 (방장 · 모집 중에만 열린다) -->
        <BaseModal v-model="showDeleteModal" title="이 챌린지를 삭제할까요?">
            <p class="gc-detail__delete-text">
                <b>{{ ch.groupName }}</b> 을(를) 없앱니다.<br />
                참여 중인 친구들과 나눈 <b>채팅도 함께 사라져요.</b><br />
                <b>되돌릴 수 없어요.</b>
            </p>
            <p v-if="deleteError" class="gc-detail__delete-error">{{ deleteError }}</p>

            <template #footer>
                <button
                    class="gc-detail__btn gc-detail__btn--outline"
                    :disabled="isDeleting"
                    @click="showDeleteModal = false"
                >
                    그만두기
                </button>
                <button
                    class="gc-detail__btn gc-detail__btn--danger gc-detail__btn--grow"
                    :disabled="isDeleting"
                    @click="handleDelete"
                >
                    {{ isDeleting ? '삭제 중…' : '삭제할게요' }}
                </button>
            </template>
        </BaseModal>

        <!-- 채팅 플로팅 버튼 (종료 제외) -->
        <div v-if="!isClosed" class="gc-detail__chat-fab" @click="goToChat">
            <div class="gc-detail__chat-fab-circle">
                <img :src="mascotChat" alt="채팅" class="gc-detail__chat-fab-img" />
            </div>
            <span
                class="gc-detail__chat-fab-badge"
                :class="{ 'gc-detail__chat-fab-badge--zero': !unreadCount }"
                >{{ unreadCount }}</span
            >
        </div>
    </div>
</template>

<style scoped>
.gc-detail {
    min-height: 100vh;
    background: var(--tt-bg-subtle);
    display: flex;
    flex-direction: column;
}

.gc-detail--loading {
    align-items: center;
    justify-content: center;
}

.gc-detail__spinner {
    width: 32px;
    height: 32px;
    border: 3px solid var(--tt-border);
    border-top-color: var(--tt-ink);
    border-radius: 50%;
    animation: spin 0.8s linear infinite;
}

@keyframes spin {
    to {
        transform: rotate(360deg);
    }
}

/* ── 히어로 영역 ── */
.gc-detail__hero {
    position: relative;
    flex: none;
    overflow: hidden;
    padding-bottom: 6px;
}

.gc-detail__deco {
    position: absolute;
    border-radius: 50%;
}

.gc-detail__deco--lg {
    top: -34px;
    right: -26px;
    width: 132px;
    height: 132px;
    background: #e9eefb;
}

/* ── 네비게이션 (ChallengePageHeader) ── */
.gc-detail__nav {
    padding: var(--tt-space-3) var(--tt-screen-padding) 0;
    position: relative;
    z-index: 2;
}

/* ── 채팅 플로팅 버튼 (좌측 하단 고정) ── */
.gc-detail__chat-fab {
    position: fixed;
    right: var(--tt-screen-padding);
    bottom: 90px;
    z-index: 10;
    cursor: pointer;
}

.gc-detail__chat-fab-circle {
    width: 52px;
    height: 52px;
    border-radius: 50%;
    background: var(--tt-bg);
    border: 1.5px solid var(--tt-border);
    box-shadow: 0 4px 12px rgba(35, 40, 66, 0.15);
    display: flex;
    align-items: center;
    justify-content: center;
}

.gc-detail__chat-fab-img {
    width: 34px;
    height: auto;
}

.gc-detail__chat-fab-badge {
    position: absolute;
    top: -3px;
    right: -5px;
    min-width: 18px;
    height: 18px;
    padding: 0 5px;
    border-radius: var(--tt-radius-full);
    background: var(--tt-red);
    color: var(--tt-white);
    font-size: 10px;
    font-weight: var(--tt-fw-black);
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: 0 2px 6px rgba(0, 0, 0, 0.15);
}

.gc-detail__chat-fab-badge--zero {
    background: var(--tt-border-strong);
}

/* ── 상태 + 제목 ── */
.gc-detail__status {
    padding: var(--tt-space-3) var(--tt-screen-padding) 0;
    position: relative;
    z-index: 2;
}

.gc-detail__badges {
    display: flex;
    gap: 6px;
}

.gc-detail__badge {
    font-size: var(--tt-fs-overline);
    font-weight: var(--tt-fw-black);
    padding: 3px 9px;
    border-radius: var(--tt-radius-full);
}

.gc-detail__name {
    font-size: var(--tt-fs-title);
    font-weight: var(--tt-fw-black);
    letter-spacing: -0.01em;
    margin-top: 9px;
    color: var(--tt-text);
}

.gc-detail__period {
    font-size: var(--tt-fs-caption);
    color: var(--tt-text-muted);
    margin-top: 4px;
}

/* ── 콘텐츠 ── */
.gc-detail__content {
    flex: 1;
    overflow-y: auto;
    padding: 6px var(--tt-screen-padding) 0;
    display: flex;
    flex-direction: column;
    gap: 7px;
}

/* ── 하단 ── */
.gc-detail__footer {
    flex: none;
    padding: 10px var(--tt-screen-padding) 24px;
    background: var(--tt-bg-subtle);
}

.gc-detail__actions {
    display: flex;
    gap: 10px;
}

.gc-detail__btn {
    padding: 14px;
    border-radius: var(--tt-radius-md);
    font-size: var(--tt-fs-label);
    font-weight: var(--tt-fw-black);
    text-align: center;
    border: none;
    cursor: pointer;
}

.gc-detail__btn--primary {
    flex: 1;
    background: var(--tt-ink);
    color: var(--tt-white);
}

/*
 * 중립 액션(「그룹 목록으로」 · 모달의 「그만두기」). `BaseButton` 의 ghost 와 같은 처방이다 —
 * 흰 면 · `--tt-border` 선 · 본문색 글자.
 *
 * 잉크색 1.5px 테두리를 두르고 있었는데, 종료 화면에서는 이게 유일한 버튼이라 화면 폭을
 * 가로지르는 검은 테두리 상자가 됐고 모집 중에는 옆의 연한 「소환」·붉은 「삭제」와 굵기가
 * 어긋났다. 나가는 버튼이 화면에서 제일 센 요소일 이유가 없다.
 * 선 두께는 같은 줄에 서는 `--secondary`·`--danger` 에 맞춰 1.5px 로 둔다.
 */
.gc-detail__btn--outline {
    flex: 1;
    background: var(--tt-bg);
    border: 1.5px solid var(--tt-border);
    color: var(--tt-text-body);
}

.gc-detail__btn--secondary {
    flex: none;
    width: 64px;
    background: var(--tt-bg);
    border: 1.5px solid var(--tt-border);
    color: var(--tt-text-body);
    font-size: var(--tt-fs-body);
}

/* ── 삭제 (방장 · 모집 중) ── */
.gc-detail__btn--danger {
    flex: none;
    background: transparent;
    border: 1.5px solid var(--tt-danger);
    color: var(--tt-danger);
}

.gc-detail__btn--grow {
    flex: 1;
}

.gc-detail__btn:disabled {
    opacity: 0.5;
    cursor: default;
}

.gc-detail__delete-text {
    font-size: var(--tt-fs-body);
    color: var(--tt-text-body);
    line-height: 1.6;
}

.gc-detail__delete-text b {
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
}

.gc-detail__delete-error {
    margin-top: var(--tt-space-3);
    padding: 10px 12px;
    background: var(--tt-danger-subtle);
    border-radius: var(--tt-radius-md);
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-danger-deep);
}

/* ── 종료 결과 카드 ── */
.gc-detail__result-card {
    background: var(--tt-bg);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-xl);
    box-shadow: var(--tt-elevation-2);
    padding: 18px 16px;
}

.gc-detail__result-title {
    display: block;
    font-size: var(--tt-fs-button);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
}

.gc-detail__result-stats {
    margin-top: 14px;
    display: flex;
    flex-direction: column;
    gap: 10px;
}

.gc-detail__result-stat {
    display: flex;
    align-items: center;
    justify-content: space-between;
}

.gc-detail__result-label {
    font-size: var(--tt-fs-body);
    color: var(--tt-text-muted);
}

.gc-detail__result-value {
    font-size: var(--tt-fs-button);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
}

.gc-detail__result-total {
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-medium);
    color: var(--tt-text-muted);
}

.gc-detail__result-value--green {
    color: var(--tt-green);
}

.gc-detail__result-value--red {
    color: var(--tt-red-deep);
}

/* ── 재판 기록 (종료 화면에 펼쳐 두는 최근 3건) ── */
.gc-detail__records {
    display: flex;
    flex-direction: column;
    gap: 8px;
}

.gc-detail__records-head {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
    gap: var(--tt-space-2);
    padding: 0 4px;
}

.gc-detail__records-title {
    margin: 0;
    font-size: var(--tt-fs-button);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
}

.gc-detail__records-count {
    display: flex;
    align-items: baseline;
    gap: 5px;
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-muted);
}
.gc-detail__records-total {
    color: var(--tt-text-body);
}
.gc-detail__records-guilty {
    color: var(--tt-danger);
}
.gc-detail__records-innocent {
    color: var(--tt-success);
}
.gc-detail__records-dot {
    color: var(--tt-text-hint);
}

/* 목록 밖으로 나가는 줄. 카드가 아니라 링크로 읽히게 면도 선도 주지 않는다 */
.gc-detail__records-more {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 2px;
    padding: 6px;
    background: none;
    border: none;
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-muted);
    cursor: pointer;
}

.gc-detail__records-more-icon {
    width: 14px;
    height: 14px;
}

/* ── 재판 기록 링크 카드 (기록 요청 실패 시의 대체 진입) ── */
/* 본문 마지막 조각. 위 카드들과 한 칸 더 떼어 「여기서 나간다」를 여백으로 말한다 */
.gc-detail__honor {
    margin-top: 6px;
}

.gc-detail__trial-link {
    background: var(--tt-bg);
    border: 1px solid var(--tt-border);
    border-radius: 16px;
    padding: 12px 14px;
    display: flex;
    align-items: center;
    gap: 11px;
    cursor: pointer;
    box-shadow: var(--tt-elevation-2);
}

.gc-detail__trial-link-icon {
    width: 34px;
    height: 34px;
    border-radius: 11px;
    background: var(--tt-gold-soft);
    display: flex;
    align-items: center;
    justify-content: center;
    flex: none;
}

.gc-detail__trial-link-icon svg {
    color: var(--tt-gold-deep);
}

.gc-detail__trial-link-body {
    flex: 1;
    min-width: 0;
}

.gc-detail__trial-link-title {
    display: block;
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
}

.gc-detail__trial-link-sub {
    display: block;
    font-size: var(--tt-fs-overline);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-hint);
    margin-top: 2px;
}

.gc-detail__trial-link-arrow {
    width: 20px;
    height: 20px;
    color: var(--tt-border-strong);
    flex: none;
}
</style>
