<script setup>
import { ref, computed, onMounted, watch } from 'vue';
import { useRouter } from 'vue-router';
import ChallengeModeTabBar from '@/components/challenge/ChallengeModeTabBar.vue';
import GroupTutorialOverlay from '@/components/challenge/group/GroupTutorialOverlay.vue';
import GroupJoinCodeSheet from '@/components/challenge/group/GroupJoinCodeSheet.vue';
import GroupTrialStatusCard from '@/components/challenge/group/GroupTrialStatusCard.vue';
import GroupTodoDoneCard from '@/components/challenge/group/GroupTodoDoneCard.vue';
import GroupPeacefulCard from '@/components/challenge/group/GroupPeacefulCard.vue';
import GroupMascotScene from '@/components/challenge/group/GroupMascotScene.vue';
import GroupTodoSheet from '@/components/challenge/group/GroupTodoSheet.vue';
import DevDataSourceFab from '@/components/dev/DevDataSourceFab.vue';
import DevBatchTriggerFab from '@/components/dev/DevBatchTriggerFab.vue';
import { hasSeenGroupTutorial, markGroupTutorialSeen } from '@/services/tutorialGuide';
import { fetchMyGroupChallenges, fetchAllMyTrials } from '@/api/groupChallenge';
import { dataSource } from '@/services/devDataSource';
import { useCountdown } from '@/utils/useCountdown';
import { ArrowPathIcon } from '@heroicons/vue/24/outline';
import ChallengeCourtHeader from '@/components/challenge/ChallengeCourtHeader.vue';
import CategoryIcon from '@/components/common/CategoryIcon.vue';
import buildingDistrict from '@/assets/images/court/building_district_v2.png';
import judgeImg from '@/assets/images/emotions/48_judging.png';
import { resolveCategoryIcon, resolveCategoryTone } from '@/utils/category';
import { toTrialStatusCard } from '@/utils/groupTrial';
import { GROUP_CATEGORY_ALL_LABEL } from '@/utils/groupCategory';
import { entryState } from '@/utils/groupChallengeNavigation';
import { useGroupChatStore } from '@/stores/groupChat';

const router = useRouter();

/*
 * 채팅 요약은 화면이 아니라 스토어가 들고 있다. 목록 API 가 주는 값과 실시간으로 도착하는 값을
 * 한군데서 합쳐야 하고, 지방법원 토글의 점도 같은 값을 봐야 하기 때문이다.
 */
const groupChat = useGroupChatStore();

/* ── 내 챌린지 (진행 중 + 시작 전) ─────── */
const myChallenges = ref([]);

async function loadMyChallenges() {
    try {
        /*
         * **시작 전(RECRUITING)까지 함께 받는다.** 서버가 시작일을 반드시 내일 이후로 막기 때문에
         * (ChallengeGroupService.validateCreate) 그룹을 만들거나 참여코드로 들어온 직후에는
         * ACTIVE 가 하나도 없다 — ACTIVE 만 부르던 예전에는 방금 한 행동이 홈에서 흔적도 없이
         * 사라지고 판사 탕이가 「새로운 챌린지에 참여해보세요」라고 되묻는 상태였다.
         */
        myChallenges.value = await fetchMyGroupChallenges(['ACTIVE', 'RECRUITING']);
        /* 서버 값으로 채팅 배지를 정정한다. 실시간으로 올려 둔 어림값이 여기서 진짜 수로 바뀐다 */
        groupChat.syncChatSummary(myChallenges.value);
    } catch {
        /* 위젯 하나 때문에 홈 전체를 막지 않는다. 비어 있으면 판사 탕이가
         * "새로운 챌린지에 참여해보세요" 로 안내한다. */
        myChallenges.value = [];
    }
}

/*
 * 시작 전인가. **`status` 만으로 가르지 않는다** — 목데이터의 진행 중 항목에는 `status` 가 없다.
 * `daysUntilStart` 는 서버가 시작 후 NULL 로 내려주므로 실서버·목데이터 양쪽에서 맞는다.
 */
function isUpcoming(challenge) {
    return challenge.status === 'RECRUITING' || challenge.daysUntilStart != null;
}

/*
 * 진행 중을 위로 올린다. 서버 정렬은 `start_date DESC` 라 **시작 전이 늘 맨 위로 온다** —
 * 아직 시작도 안 한 그룹이 오늘 목숨이 걸린 그룹을 밀어내는 셈이었다.
 * `sort` 는 안정 정렬이라 같은 묶음 안에서는 서버 순서가 그대로 남는다.
 */
const sortedChallenges = computed(() =>
    [...myChallenges.value].sort((a, b) => Number(isUpcoming(a)) - Number(isUpcoming(b))),
);

const activeCount = computed(() => myChallenges.value.filter((ch) => !isUpcoming(ch)).length);

/* ── 재판 현황 (내가 속한 그룹의 진행 중인 재판 전부) ── */
const myTrials = ref([]);

/* 처리한 건을 화면에서만 지운다. DEV 토글이 「전부 완료」를 흉내낼 때도 쓴다. */
const doneIds = ref([]);

async function loadMyTrials() {
    try {
        /* 서버가 할 일 있는 것부터 마감 임박순으로 정렬해 준다. 여기서 다시 정렬하면 기준이 갈린다. */
        myTrials.value = await fetchAllMyTrials();
    } catch {
        /* 위젯 하나 때문에 홈 전체를 막지 않는다. 비면 판사 탕이가 다른 말을 한다. */
        myTrials.value = [];
    }
}

/*
 * 카드 한 장이 쓸 모양. 카테고리·한도는 **서버에 다시 묻지 않고** 이미 받아 둔 참여 그룹 목록에서
 * `groupId` 로 이어 붙인다 — 재판마다 그룹 정보를 또 내려보내면 같은 값이 여섯 번 실려 온다.
 * 목록이 아직 안 왔거나 방금 나간 그룹이면 조각이 비고, 카드는 그 줄만 지운다.
 */
const trialCards = computed(() =>
    myTrials.value
        .filter((item) => !doneIds.value.includes(item.id))
        .map((item) => {
            const group = myChallenges.value.find((ch) => ch.id === item.groupId);
            return toTrialStatusCard({
                ...item,
                categoryName: group?.categoryName ?? null,
                limitAmount: group?.limitAmount ?? null,
            });
        }),
);

/*
 * 바텀시트가 쓰는 얇은 행. 「할 일」만 담는다 — 시트는 훑어보고 바로 처리하는 자리라
 * 심판받는 중처럼 누를 것이 없는 재판이 섞이면 목록만 길어진다.
 *
 * 같은 `trialCards` 에서 뽑는다. 예전 `/my-trials` 를 따로 한 번 더 부르면 두 목록의
 * 건수가 어긋나는 순간이 생긴다(마감 필터를 서버가 각각 계산한다).
 */
const todoItems = computed(() =>
    trialCards.value
        .filter((card) => card.actionable)
        .map((card) => ({
            id: card.id,
            type: card.action === 'defend' ? 'accuse' : 'vote',
            title: card.title,
            amount: card.exceededAmount,
            challengeName: card.groupName,
            challengeId: card.groupId,
            indictmentId: card.id,
            tally: `${card.voteCount}/${card.totalVoters} 투표`,
            voteCount: card.voteCount,
            totalVoters: card.totalVoters,
            deadline: card.deadline,
        })),
);

const hasTodo = computed(() => trialCards.value.length > 0);
const allDone = computed(() => trialCards.value.length === 0 && doneIds.value.length > 0);

/* ── 카운트다운 ────────────────────────── */
const { countdowns } = useCountdown(trialCards);

/* ── 바텀시트 ──────────────────────────── */
const showSheet = ref(false);
const todoSheetRef = ref(null);

/* ── 토스트 ────────────────────────────── */
const toast = ref(null);
let toastTimer = null;

function flash(msg) {
    clearTimeout(toastTimer);
    toast.value = msg;
    toastTimer = setTimeout(() => {
        toast.value = null;
    }, 1800);
}

/* ── 판사 탕이 말풍선 ─────────────────── */
const judgeQuote = computed(() => {
    const todoCount = todoItems.value.length;
    if (todoCount > 0) return `밀린 할 일이 ${todoCount}건 있어요,\n서두르세요!`;
    if (allDone.value) return '모든 할 일을 처리했군요,\n훌륭해요!';
    if (activeCount.value > 0) return `진행 중인 챌린지가\n${activeCount.value}건 있어요`;
    /* 시작 전만 있는 상태. 여기서 「참여해보세요」라고 하면 방금 만든 사람에게 거짓말이 된다 */
    const upcomingCount = myChallenges.value.length - activeCount.value;
    if (upcomingCount > 0) return `곧 시작할 챌린지가\n${upcomingCount}건 있어요`;
    return '새로운 챌린지에\n참여해보세요!';
});

/* ── 튜토리얼 ─────────────────────────── */
const showTutorial = ref(false);
const showJoinSheet = ref(false);

onMounted(() => {
    if (!hasSeenGroupTutorial()) {
        showTutorial.value = true;
    }
    loadMyChallenges();
    loadMyTrials();
});

watch(dataSource, () => {
    loadMyChallenges();
    loadMyTrials();
});

/*
 * 완료 저장은 서버(tbl_user.group_tutorial_seen_at)로 나간다 — 비동기다.
 * 저장 실패는 markGroupTutorialSeen() 안에서 삼킨다(다음에 한 번 더 뜨는 게 전부).
 */
async function onTutorialComplete() {
    await markGroupTutorialSeen();
}

/* ── DEV 상태 전환 ─────────────────────── */
const isDev = import.meta.env.DEV;
const DEV_STATES = ['할 일 있음', '전부 완료', '초기화'];
const devStateIndex = ref(0);
const devStateLabel = computed(() => DEV_STATES[devStateIndex.value]);

function cycleDevState() {
    devStateIndex.value = (devStateIndex.value + 1) % DEV_STATES.length;
    if (devStateIndex.value === 1) {
        doneIds.value = myTrials.value.map((i) => i.id);
    } else if (devStateIndex.value === 2) {
        doneIds.value = [];
        devStateIndex.value = 0;
    }
}

/* ── 이벤트 핸들러 ─────────────────────── */

/** CTA 가 여는 화면. 어느 화면인지는 `utils/groupTrial.js` 가 이미 정해서 준다. */
const TRIAL_ROUTE = {
    defend: 'defenseViolation',
    vote: 'voteVerdict',
    trial: 'trialProgress',
};

function goToTrial(action, groupId, indictmentId) {
    /* 바텀시트 안에서 이동할 때는 시트의 history 항목을 먼저 양도한 뒤
     * router.replace 를 써야 한다. 그렇지 않으면 시트가 닫히면서
     * history.back() 이 라우터 이동을 되감는다 (useOverlay 주석 참고). */
    if (showSheet.value) {
        todoSheetRef.value?.releaseHistory?.();
        showSheet.value = false;
    }
    router.replace({
        name: TRIAL_ROUTE[action],
        params: { id: groupId, indictmentId },
    });
}

/** 「재판 현황」 카드의 CTA. 어떤 입장이냐에 따라 변론·투표·현황 셋 중 하나로 간다. */
function onOpenTrial({ item, action }) {
    goToTrial(action, item.groupId, item.id);
}

/** 바텀시트의 얇은 행. 여기는 할 일만 담기므로 변론·투표 둘뿐이다. */
function onOpenTodo(item) {
    goToTrial(item.type === 'accuse' ? 'defend' : 'vote', item.challengeId, item.indictmentId);
}

function goToAllChallenges() {
    router.push({ name: 'groupChallengeList' });
}

/*
 * 그룹챌린지 생성 진입로. 만들기 화면(`/group-challenges/create`)은 라우트만 있고
 * 홈·목록 어느 쪽에도 버튼이 없어 주소를 직접 쳐야만 갈 수 있었다(이슈 #172).
 */
function goToCreate() {
    router.push({ name: 'groupChallengeCreate' });
}

/*
 * 홈의 진행 중 카드에는 클릭이 아예 없었다. 목록의 GroupActiveCard 와 달리 그 자리에서 만든
 * 별도 마크업이라 진입로가 함께 붙지 않았다 — 눌러도 반응이 없어 「전체보기 ›」로 목록까지
 * 들어가야만 상세에 갈 수 있었다.
 *
 * 목록 카드처럼 채팅 영역을 따로 두지는 않는다. 홈 카드에는 대화 미리보기가 없어 나눌 자리가 없다.
 */
function goToDetail(challenge) {
    router.push({
        name: 'groupChallengeDetail',
        params: { id: challenge.id },
        /* 상세가 스크롤 위치까지 되살리며 여기로 돌아오게 한다(이슈 #303) */
        state: entryState('groupChallenge'),
    });
}

/* ── 행 표기 ───────────────────────────── */

/*
 * 한도 문구. 목록의 GroupActiveCard·GroupPreStartCard 와 같은 규칙을 쓴다 —
 * 같은 챌린지가 화면마다 다르게 읽히면 안 된다.
 * **0 원은 미입력이 아니라 무지출 챌린지의 정상값이다.**
 */
function limitDesc(challenge) {
    const type = challenge.evalType === 'DAILY' ? '일일결산' : '기간평가';
    if (challenge.limitAmount > 0) {
        const prefix = challenge.evalType === 'DAILY' ? '일' : '총';
        return `${type} · ${prefix} ${challenge.limitAmount.toLocaleString()}원`;
    }
    return challenge.limitAmount === 0 ? `${type} · 무지출` : type;
}

/*
 * 카테고리는 서버가 안 줄 수 있다(`categoryId` 가 NULL 인 그룹). 조각을 빼지 않고 「총 소비」로 적는다 —
 * 빼 버리면 「전체 소비를 합산하는 챌린지」라는 정보가 화면에서 사라져 카테고리 챌린지와 구별이 안 된다.
 */
function metaLine(challenge) {
    const parts = [];
    parts.push(challenge.categoryName ?? GROUP_CATEGORY_ALL_LABEL);
    if (isUpcoming(challenge)) {
        parts.push(`${challenge.memberCount}/${challenge.maxMembers}명 모집 중`);
    } else {
        parts.push(`${challenge.memberCount}명`);
    }
    parts.push(limitDesc(challenge));
    if (!isUpcoming(challenge)) parts.push(`${challenge.currentDay}일차`);
    return parts.join(' · ');
}

/*
 * 우측 칩. 시작 전은 시작까지, 진행 중은 종료까지 남은 날이다.
 * `currentDay` 는 1부터 세므로 마지막 날에는 남은 날이 0 이 된다 — 「D-0」 대신 「마지막 날」로 적는다.
 */
function dayChip(challenge) {
    if (isUpcoming(challenge)) {
        /*
         * **`daysUntilStart` 가 NULL 인 시작 전 그룹이 있다.** 서버는 이 값을 status 가 아니라
         * 시작일만 보고 계산해서(ChallengeGroupService.daysUntilStart) 시작일이 지나면 NULL 을 준다.
         * 그런데 RECRUITING → ACTIVE 전이는 **하루 한 번 도는 배치**(cron `0 1 0 * * *`)라,
         * 그 시각에 서버가 꺼져 있었으면 status 는 RECRUITING 인 채로 남는다.
         * 로컬에서는 밤새 API 를 안 띄우니 사실상 매번 이 상태가 된다.
         * 그대로 문자열에 끼우면 **「시작 D-null」** 이 찍힌다.
         */
        return challenge.daysUntilStart == null
            ? '시작 대기'
            : `시작 D-${challenge.daysUntilStart}`;
    }
    const remaining = challenge.totalDays - challenge.currentDay;
    return remaining > 0 ? `D-${remaining}` : '마지막 날';
}

/*
 * 카테고리가 없는 그룹은 **「기타」가 아니라 「총 소비」**다 (GroupCategoryPicker 가 지원하는 정상 선택지).
 * `resolveCategoryIcon` 의 폴백 `EllipsisHorizontalCircle` 은 하필 「기타」 카테고리 아이콘과
 * 같은 모양이라 그대로 두면 뜻이 정반대로 읽힌다. 지갑으로 「전부 합산」을 나타낸다.
 */
function categoryIcon(challenge) {
    return challenge.categoryName ? resolveCategoryIcon(challenge.categoryName) : 'Wallet';
}

/*
 * 메타 줄 대신 보여줄 마지막 대화 한 줄. 없으면 null 이고 그때는 메타 줄이 그대로 남는다.
 *
 * 스토어 값이 목록 응답보다 우선이다 — 홈에 머무는 동안 도착한 메시지는 스토어에만 있다.
 *
 * ⚠ **시작 전(RECRUITING)이라고 빼면 안 된다.** 한때 「아직 아무 일도 안 일어난 방」이라고
 * 단정하고 걸러냈는데 틀렸다 — `ChatMessageService` 에는 상태 검사가 아예 없어서 방을 만든
 * 직후부터 대화가 된다. 오히려 서버가 시작일을 반드시 내일 이후로 막기 때문에(validateCreate)
 * **만들고 나서 처음 떠드는 구간이 전부 RECRUITING 이다.** 그 구간을 빼면 정작 대화가 가장
 * 활발할 때 홈에 아무것도 안 뜬다.
 */
function chatLine(challenge) {
    return groupChat.lastMessageByGroup[challenge.id] ?? challenge.lastChatMessage ?? null;
}

/** 안 읽은 개수. 배지 문자열이며 0 이면 null 이라 배지 자체가 그려지지 않는다. */
function chatBadge(challenge) {
    const count = groupChat.unreadByGroup[challenge.id] ?? challenge.unreadChatCount ?? 0;
    if (count <= 0) return null;
    /* 세 자리가 되면 배지가 이름을 밀어낸다. 카카오톡과 같은 방식으로 접는다 */
    return count > 99 ? '99+' : String(count);
}

/*
 * 대화 줄만 채팅방으로 보낸다. 행 전체는 그대로 상세로 간다.
 * `.stop` 이 빠지면 부모의 @click 까지 함께 타서 채팅방을 열자마자 상세로 새어 나간다
 * (목록 화면 GroupActiveCard 에서 같은 문제를 이미 겪었다).
 */
function goToChat(challenge) {
    router.push({ name: 'groupChallengeChat', params: { id: challenge.id } });
}

/* 목숨(판사봉)은 여기서 보여주지 않는다 — 챌린지마다 한 줄을 더 먹는데, 홈에서 할 일이 없는 값이다.
   상세 화면이 그대로 보여준다. */
</script>

<template>
    <div class="gc-page">
        <!-- ===== 다크 헤더 (지방법원 건물) ===== -->
        <ChallengeCourtHeader
            variant="district"
            :court-image="buildingDistrict"
            court-alt="탕탕 지방법원"
            subtitle="함께 모여 소비 습관을 심판하는 곳"
            :tangi-image="judgeImg"
            tangi-role="판사"
            tangi-name="탕이"
            :quote="judgeQuote"
        />

        <!-- ===== 본문 ===== -->
        <main class="gc-body">
            <!-- 재판 현황. 카드는 진행 중인 재판 전부를 담고, 시트는 그중 할 일만 얇은 행으로 훑는다 -->
            <div v-if="hasTodo" class="gc-section-top gc-section-top--todo">
                <span class="gc-section-title">재판 현황</span>
                <button
                    v-if="todoItems.length"
                    type="button"
                    class="gc-view-all"
                    @click="showSheet = true"
                >
                    할 일 {{ todoItems.length }}건 ›
                </button>
            </div>

            <!-- 재판 현황 / 방금 다 처리함 / 애초에 진행 중인 재판이 없음 -->
            <GroupTrialStatusCard
                v-if="hasTodo"
                :items="trialCards"
                :countdowns="countdowns"
                @open="onOpenTrial"
            />
            <GroupTodoDoneCard v-else-if="allDone" />
            <!-- 기소·투표가 아예 없는 평온 상태. 이 분기가 없으면 자리 전체가 빈 화면이 된다.
                 doneIds 는 DEV 토글로만 채워지므로 allDone 은 실사용에서 거의 오지 않는다. -->
            <template v-else>
                <GroupPeacefulCard />
                <GroupMascotScene scene="peaceful" />
            </template>

            <!-- 내 챌린지 — 진행 중과 시작 전을 한 카드에 행으로 쌓는다 -->
            <div class="gc-section">
                <div class="gc-section-top">
                    <span class="gc-section-title">내 챌린지</span>
                    <button type="button" class="gc-view-all" @click="goToAllChallenges">
                        전체보기 ›
                    </button>
                </div>

                <div class="gc-groups">
                    <p v-if="!sortedChallenges.length" class="gc-groups__empty">
                        아직 참여 중인 챌린지가 없어요
                    </p>

                    <div
                        v-for="ch in sortedChallenges"
                        :key="ch.id"
                        class="gc-group-row"
                        @click="goToDetail(ch)"
                    >
                        <span
                            class="gc-group-row__icon"
                            :class="`gc-group-row__icon--${resolveCategoryTone(ch.categoryName)}`"
                            aria-hidden="true"
                        >
                            <CategoryIcon :icon="categoryIcon(ch)" />
                        </span>

                        <div class="gc-group-row__body">
                            <span class="gc-group-row__name">{{ ch.groupName }}</span>
                            <!--
                              대화가 있으면 메타 줄을 마지막 메시지로 바꾼다. 한 줄을 더 쌓지 않는
                              이유는 행 높이다 — 여기서 한 줄이 늘면 카드가 화면 밖으로 밀린다.
                              한도·정원은 상세 화면이 그대로 보여주므로 사라지는 게 아니다.
                            -->
                            <span
                                v-if="chatLine(ch)"
                                class="gc-group-row__chat"
                                @click.stop="goToChat(ch)"
                            >
                                {{ chatLine(ch) }}
                            </span>
                            <span v-else class="gc-group-row__meta">{{ metaLine(ch) }}</span>
                        </div>

                        <!--
                          D-day 칩과 안 읽은 배지를 오른쪽에 세로로 쌓는다. 배지를 대화 줄 옆에
                          두면 이름 길이에 따라 자리가 춤추고 칩과도 붙어 보인다.
                        -->
                        <div class="gc-group-row__aside">
                            <span
                                class="gc-group-row__chip"
                                :class="{ 'gc-group-row__chip--upcoming': isUpcoming(ch) }"
                            >
                                {{ dayChip(ch) }}
                            </span>
                            <span
                                v-if="chatBadge(ch)"
                                class="gc-group-row__badge"
                                @click.stop="goToChat(ch)"
                            >
                                {{ chatBadge(ch) }}
                            </span>
                        </div>
                    </div>

                    <!-- 두 진입로는 챌린지가 아니다. 한 카드에 묶어 행 카드와 섞이지 않게 한다 -->
                    <div class="gc-groups__actions">
                        <!-- 생성 진입로. 만들기 화면으로 가는 유일한 버튼이다 -->
                        <button type="button" class="gc-groups__cta" @click="goToCreate">
                            <span class="gc-groups__cta-plus">＋</span>
                            <span>새 그룹챌린지 만들기</span>
                        </button>

                        <!-- 참여코드 진입로. 목록 「시작 전」 탭까지 들어가지 않아도 코드를 넣을 수 있다 -->
                        <button type="button" class="gc-groups__cta" @click="showJoinSheet = true">
                            <span class="gc-groups__cta-plus">＃</span>
                            <span>참여코드로 들어가기</span>
                        </button>
                    </div>
                </div>
            </div>
        </main>

        <!-- ===== 토스트 ===== -->
        <Transition name="tt-toast">
            <div v-if="toast" class="gc-toast">
                <span class="gc-toast__text">{{ toast }}</span>
            </div>
        </Transition>

        <!-- ===== 개인/그룹 세그먼트 (팀 공용) ===== -->
        <ChallengeModeTabBar active-mode="group" />

        <!-- ===== TO-DO 바텀시트 ===== -->
        <GroupTodoSheet
            ref="todoSheetRef"
            v-model="showSheet"
            :items="todoItems"
            :countdowns="countdowns"
            @open="onOpenTodo"
        />

        <!-- ===== 참여코드 입장 바텀시트 ===== -->
        <GroupJoinCodeSheet v-model="showJoinSheet" />

        <!-- ===== 튜토리얼 오버레이 ===== -->
        <GroupTutorialOverlay v-model="showTutorial" @complete="onTutorialComplete" />

        <!-- DEV: 데이터 출처 전환 -->
        <DevDataSourceFab />

        <!-- DEV: TO-DO 상태 전환 (데이터 출처 버튼 위에 쌓는다) -->
        <button v-if="isDev" type="button" class="gc-dev-fab" @click="cycleDevState">
            <ArrowPathIcon class="gc-dev-fab__icon" />
            {{ devStateLabel }}
        </button>

        <!-- DEV: 시작 상태 전이 배치 즉시 실행 (자정을 기다리지 않기 위한 문) -->
        <DevBatchTriggerFab :offset="96" @done="loadMyChallenges" />
    </div>
</template>

<style scoped>
.gc-page {
    min-height: 100vh;
    /* 카드에 선도 그림자도 없으므로 배경이 유일한 경계다 — 개인 미션 홈과 같은 값 */
    background: var(--tt-bg-page);
    /*
     * 탭바를 피하는 몫은 base.css 의 .tt-app__content > * 가 잡는다.
     * 여기서는 이 화면 하단에 떠 있는 ChallengeModeTabBar 를 피할 만큼만 더 준다.
     */
    padding-bottom: var(--tt-float-toggle-inset);
}

.gc-dev-fab {
    position: fixed;
    right: 16px;
    bottom: calc(var(--tt-tabbar-height) + 56px); /* DevDataSourceFab 위에 쌓는다 */
    height: 32px;
    padding: 0 12px;
    border-radius: var(--tt-radius-full);
    border: 1.5px solid rgba(245, 185, 33, 0.5);
    background: rgba(35, 40, 66, 0.9);
    color: var(--tt-gold);
    font-size: var(--tt-fs-overline);
    font-weight: var(--tt-fw-black);
    font-family: inherit;
    cursor: pointer;
    z-index: var(--tt-z-tabbar);
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
    display: flex;
    align-items: center;
    gap: 4px;
}

.gc-dev-fab__icon {
    width: 14px;
    height: 14px;
}

/* ── 본문 ──────────────────────────────── */
.gc-body {
    padding: 10px 22px 0;
    position: relative;
    z-index: 3;
}

/* ── 진행 중인 챌린지 섹션 ─────────────── */
.gc-section {
    margin-top: 10px;
}

.gc-section-top {
    display: flex;
    align-items: center;
    justify-content: space-between;
}

/* TO-DO 카드 바로 위에 붙는 머리줄. 카드가 곧바로 이어지므로 아래 여백만 조금 준다 */
.gc-section-top--todo {
    padding-bottom: 8px;
}

.gc-section-title {
    font-size: var(--tt-fs-label);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
}

.gc-view-all {
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-muted);
    background: none;
    border: none;
    cursor: pointer;
    font-family: inherit;
    padding: 0;
}

/* ── 내 챌린지 목록 ───────────────────── */
/*
 * 챌린지 하나 = 카드 하나다. 한 카드 안에 구분선으로 쌓아 두었더니 서로 이어진 한 덩어리로
 * 읽혔는데, 실제로는 각각 독립된 챌린지다. 목록 화면(GroupChallengeListView)도 카드를 띄워
 * 나열하므로 두 화면의 시각 언어가 이걸로 같아진다.
 *
 * 선도 그림자도 두지 않는다. 카드를 띄우는 일은 페이지 배경(--tt-bg-page)이 맡는다.
 */
.gc-groups {
    margin-top: 11px;
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-3);
}

.gc-groups__empty {
    padding: 22px 0;
    background: var(--tt-bg);
    border-radius: 18px;
    text-align: center;
    font-size: var(--tt-fs-caption);
    color: var(--tt-text-muted);
}

/* ── 그룹 카드 ────────────────────────── */
.gc-group-row {
    display: flex;
    align-items: center;
    gap: 11px;
    padding: 13px 15px;
    background: var(--tt-bg);
    border-radius: 18px;
    cursor: pointer;
    transition: opacity 0.15s ease;
}

.gc-group-row:active {
    opacity: 0.6;
}

.gc-group-row__icon {
    width: 42px;
    height: 42px;
    border-radius: 14px;
    flex: none;
    display: flex;
    align-items: center;
    justify-content: center;
}

/* 장부 거래 행과 같은 톤 규칙을 쓴다(utils/category.js) — 같은 카테고리가 화면마다 달라 보이면 안 된다 */
.gc-group-row__icon--primary {
    color: var(--tt-primary);
    background: var(--tt-primary-subtle);
}
.gc-group-row__icon--accent {
    color: var(--tt-gold-deep);
    background: var(--tt-accent-subtle);
}
.gc-group-row__icon--success {
    color: var(--tt-success);
    background: var(--tt-success-subtle);
}
.gc-group-row__icon--muted {
    color: var(--tt-text-muted);
    background: var(--tt-bg-fill);
}

.gc-group-row__body {
    flex: 1;
    min-width: 0;
}

.gc-group-row__name {
    display: block;
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

/* 칩과 배지를 세로로 쌓는 오른쪽 열. gap 이 둘 사이 여백을 만든다 */
.gc-group-row__aside {
    flex: none;
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    gap: 7px;
}

.gc-group-row__chip {
    flex: none;
    font-size: var(--tt-fs-overline);
    font-weight: var(--tt-fw-black);
    padding: 3px 8px;
    border-radius: var(--tt-radius-full);
    background: var(--tt-bg-fill);
    color: var(--tt-text-hint);
}

/* 시작 전은 아직 아무 일도 안 일어난 상태다. 진행 중 칩과 색으로 구분한다 */
.gc-group-row__chip--upcoming {
    background: var(--tt-gold-soft);
    color: var(--tt-gold-deep);
}

.gc-group-row__meta,
.gc-group-row__chat {
    display: block;
    margin-top: 3px;
    font-size: var(--tt-fs-overline);
    color: var(--tt-text-muted);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

/* 대화 줄만 채팅방으로 간다 — 글자 위가 아니면 부모의 @click 이 상세로 보낸다 */
.gc-group-row__chat {
    width: fit-content;
    max-width: 100%;
}

.gc-group-row__chat:active {
    opacity: 0.6;
}

/* 목록 화면 GroupActiveCard 의 배지와 같은 규격이다. 두 화면에서 다르게 보이면 안 된다 */
.gc-group-row__badge {
    display: flex;
    align-items: center;
    justify-content: center;
    min-width: 17px;
    height: 17px;
    padding: 0 5px;
    border-radius: var(--tt-radius-full);
    background: var(--tt-danger);
    color: var(--tt-text-inverse);
    font-size: 10px;
    font-weight: var(--tt-fw-black);
    line-height: 1;
}

/* ── 생성 · 참여코드 진입로 ───────────── */
/* 챌린지 카드와 같은 흰 카드지만 누르는 곳이 둘이라 안에서 점선으로 나눈다 */
.gc-groups__actions {
    background: var(--tt-bg);
    border-radius: 18px;
    padding: 0 15px;
}

.gc-groups__cta {
    width: 100%;
    padding: 13px 0;
    display: flex;
    align-items: center;
    gap: 9px;
    background: none;
    border: none;
    cursor: pointer;
    font-family: inherit;
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-black);
    color: var(--tt-primary);
    transition: opacity 0.15s ease;
}

/* 카드 맨 위에는 선을 두지 않는다 — 두면 카드가 두 조각으로 보인다 */
.gc-groups__cta + .gc-groups__cta {
    border-top: 1px dashed var(--tt-border-divider);
}

.gc-groups__cta:active {
    opacity: 0.6;
}

.gc-groups__cta-plus {
    font-size: var(--tt-fs-body);
    line-height: 1;
}

/* ── 토스트 ────────────────────────────── */
.gc-toast {
    position: fixed;
    left: 22px;
    right: 22px;
    bottom: calc(var(--tt-tabbar-height) + 80px);
    z-index: var(--tt-z-toast, 50);
    display: flex;
    justify-content: center;
    pointer-events: none;
}
.gc-toast__text {
    background: rgba(35, 40, 66, 0.94);
    color: var(--tt-text-inverse);
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
    padding: 10px 16px;
    border-radius: var(--tt-radius-full);
    box-shadow: 0 12px 26px rgba(21, 24, 40, 0.32);
}

.tt-toast-enter-active {
    animation: tt-toastin 0.22s ease-out both;
}
.tt-toast-leave-active {
    animation: tt-toastin 0.18s ease-in reverse both;
}

@keyframes tt-toastin {
    0% {
        transform: translateY(10px);
        opacity: 0;
    }
    100% {
        transform: none;
        opacity: 1;
    }
}
</style>
