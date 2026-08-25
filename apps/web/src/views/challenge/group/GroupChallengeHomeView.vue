<script setup>
import { ref, computed, onMounted, watch } from 'vue';
import { useRouter } from 'vue-router';
import ChallengeModeTabBar from '@/components/challenge/ChallengeModeTabBar.vue';
import GroupTutorialOverlay from '@/components/challenge/group/GroupTutorialOverlay.vue';
import GroupJoinCodeSheet from '@/components/challenge/group/GroupJoinCodeSheet.vue';
import GroupTrialTodoGrid from '@/components/challenge/group/GroupTrialTodoGrid.vue';
import GroupTodoDoneCard from '@/components/challenge/group/GroupTodoDoneCard.vue';
import GroupPeacefulCard from '@/components/challenge/group/GroupPeacefulCard.vue';
import GroupMascotScene from '@/components/challenge/group/GroupMascotScene.vue';
import GroupTrialListSheet from '@/components/challenge/group/GroupTrialListSheet.vue';
import DevDataSourceFab from '@/components/dev/DevDataSourceFab.vue';
import DevBatchTriggerFab from '@/components/dev/DevBatchTriggerFab.vue';
import { hasSeenGroupTutorial, markGroupTutorialSeen } from '@/services/tutorialGuide';
import {
    fetchMyGroupChallenges,
    fetchAllMyTrials,
    fetchAllMyTrialRecords,
} from '@/api/groupChallenge';
import { dataSource } from '@/services/devDataSource';
import { useCountdown } from '@/utils/useCountdown';
import { ArrowPathIcon } from '@heroicons/vue/24/outline';
import ChallengeCourtHeader from '@/components/challenge/ChallengeCourtHeader.vue';
import CategoryIcon from '@/components/common/CategoryIcon.vue';
import buildingDistrict from '@/assets/images/court/building_district_v2.png';
import judgeImg from '@/assets/images/emotions/48_judging.png';
import objIndictImage from '@/assets/images/judgment/obj_indict.png';
import objDefenseImage from '@/assets/images/judgment/obj_defense.png';
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
         *
         * **재판 중(JUDGING)까지 부르되 카드에서는 뺀다.** 채팅은 JUDGING 에서도 입장이 열려 있어
         * (ChatRoomAccessService — CLOSED 만 막는다) 배지는 그 방까지 세야 한다. 반면 카드는
         * `currentDay`·`daysUntilStart` 가 둘 다 NULL 로 내려와 「D-7」·「null일차」가 찍힌다.
         *
         * **아래 세 줄은 순서가 계약이다.** `syncChatSummary` 에 `raw` 가 아니라
         * `myChallenges.value` 를 넘기면 JUDGING 이 이미 빠진 배열이 들어가 배지 수정이
         * 통째로 무효가 된다 — 그런데도 테스트는 통과한다. 한 줄로 합치지 말 것.
         */
        const raw = await fetchMyGroupChallenges(['ACTIVE', 'RECRUITING', 'JUDGING']);
        /* 서버 값으로 채팅 배지를 정정한다. 실시간으로 올려 둔 어림값이 여기서 진짜 수로 바뀐다 */
        groupChat.syncChatSummary(raw);
        /* 목데이터의 진행 중 항목에는 `status` 가 없다 — 화이트리스트가 아니라 `!==` 여야 한다 */
        myChallenges.value = raw.filter((ch) => ch.status !== 'JUDGING');
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

/*
 * 홈은 목록 화면이 아니다. 참여 그룹이 늘수록 이 섹션이 세로로 그대로 자라 위의 재판 현황을
 * 화면 밖으로 밀어냈다 — 지금 할 일(재판)이 지금 할 일이 아닌 것(그룹 명부)에 밀리는 구조다.
 * 홈은 상위 몇 건까지만 보여주고 나머지는 「전체보기」가 여는 목록 화면이 맡는다.
 * `sortedChallenges` 가 진행 중을 위로 올려 두었으므로 잘려 나가는 쪽은 늘 시작 전이다.
 */
const HOME_CHALLENGE_LIMIT = 3;

const homeChallenges = computed(() => sortedChallenges.value.slice(0, HOME_CHALLENGE_LIMIT));

/* 잘린 게 있을 때만 「전체보기」가 총 개수를 밝힌다 — 3건 이하면 셀 것도 없다 */
const hiddenChallengeCount = computed(
    () => sortedChallenges.value.length - homeChallenges.value.length,
);

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
 * 끝난 재판 건수.
 *
 * **개수만 들고 있는다.** 목록은 기록 화면이 다시 읽는다 — 여기에 목록까지 쥐면
 * 홈과 기록 화면이 서로 다른 시점의 목록을 그리게 되고, 홈이 무거워진다.
 * 실패해도 홈은 그대로 그린다(0 이면 줄이 통째로 사라질 뿐이다).
 */
const recordCount = ref(0);

async function loadTrialRecordCount() {
    try {
        recordCount.value = (await fetchAllMyTrialRecords()).length;
    } catch {
        recordCount.value = 0;
    }
}

function goToTrialRecords() {
    router.push({ name: 'groupTrialRecordsAll' });
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
 * 재판을 **「내 차례」와 「기다리는 중」으로 가른다**(#448).
 * 식별자는 `watching` 그대로 둔다 — 화면 문구만 바뀐 것이라 여기까지 옮기면
 * `openSheet` 값·CSS 클래스·테스트가 같이 흔들린다.
 *
 * 예전에는 진행 중인 재판 전부를 한 아코디언에 쌓았다. 그룹 여섯 개에 들어 있으면 재판도
 * 여섯 줄이 되는데, 그중 내가 지금 손댈 수 있는 건 보통 한둘이다 — 나머지는 「내 변론을
 * 제출했어요」·「○○님이 변론을 쓰는 중이에요」처럼 **눌러도 할 게 없는 줄**이라
 * 정작 마감이 걸린 줄을 가렸다.
 *
 * 기준은 `STANCE.actionable` 하나다. 서버 정렬(`GroupTrialService#findAllMyTrials`)이
 * 같은 기준으로 앞세우므로 여기서 나눠도 각 묶음 안의 마감 임박순은 그대로 남는다.
 * 기다리는 쪽은 없애지 않는다 — 할 일은 아니어도 「내 재판이 어떻게 되고 있나」는
 * 이 화면에서 가장 궁금한 것이다. 홈에는 한 줄로 접고 목록은 시트가 받는다.
 */
const myTurnTrials = computed(() => trialCards.value.filter((card) => card.actionable));
const watchingTrials = computed(() => trialCards.value.filter((card) => !card.actionable));

/*
 * 내 차례를 다시 **행동별로** 가른다 — 할 일 격자의 두 칸이 이 둘이다.
 * `action` 은 `utils/groupTrial.js` 의 `STANCE.action` 이 정한 값이라 여기서 다시 판정하지 않는다.
 * 내 차례에는 `trial`(기다리기)이 섞이지 않는다 — 그건 `actionable: false` 쪽이다.
 */
const defendTrials = computed(() => myTurnTrials.value.filter((card) => card.action === 'defend'));
const voteTrials = computed(() => myTurnTrials.value.filter((card) => card.action === 'vote'));

/* 카드 자리는 이제 `myTurnTrials` · `watchingTrials` 가 직접 가른다 — `hasTodo` 는 지웠다 */
const allDone = computed(() => trialCards.value.length === 0 && doneIds.value.length > 0);

/* ── 카운트다운 ────────────────────────── */
const { countdowns } = useCountdown(trialCards);

/* 접힌 자리가 마감을 감춰 버리지 않게, 6시간 안쪽이 하나라도 있으면 그것만 밖으로 알린다 */
function anyUrgent(list) {
    return list.some((card) => countdowns.value[card.id]?.urgent);
}
const defendUrgent = computed(() => anyUrgent(defendTrials.value));
const voteUrgent = computed(() => anyUrgent(voteTrials.value));
const watchingUrgent = computed(() => anyUrgent(watchingTrials.value));

/*
 * ── 재판 목록 바텀시트 ─────────────────
 * 세 묶음(변론·투표·기다리기)이 **시트 하나**를 돌려 쓴다. 목록 내용만 다르고 줄 모양은 같아서
 * `showDefendSheet` · `showVoteSheet` … 로 늘리면 셋이 조금씩 어긋나기만 한다.
 * 열려 있는 종류를 문자열 하나로 들고, 제목과 항목은 거기서 파생시킨다.
 */
const openSheet = ref(null); /* 'defend' | 'vote' | 'watching' | null */
const sheetRef = ref(null);

/*
 * 시트 제목. 「기다리는 재판」은 한때 「지켜보는 재판」이었다 — 「지켜본다」도 **내 행동**이라
 * 격자 두 칸(변론·투표)과 결이 겹쳐 「할 일이 아닌 쪽」이라는 게 낱말에서 안 갈렸다.
 * 「판결 대기중」은 안 쓴다. `DEFENSE_WAITING`·`DEFENSE_SUBMITTED` 는 투표조차 시작 전이라
 * 판결까지 두 단계 남았고, 넓게 읽으면 격자의 변론·투표 재판도 전부 판결 대기중이라 갈리지 않는다.
 */
const SHEET_TITLE = { defend: '변론할 재판', vote: '투표할 재판', watching: '기다리는 재판' };
const sheetTitle = computed(() => SHEET_TITLE[openSheet.value] ?? '');

const SHEET_SOURCE = {
    defend: defendTrials,
    vote: voteTrials,
    watching: watchingTrials,
};
const sheetItems = computed(() => SHEET_SOURCE[openSheet.value]?.value ?? []);

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
    const todoCount = myTurnTrials.value.length;
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
    loadTrialRecordCount();
});

watch(dataSource, () => {
    loadMyChallenges();
    loadMyTrials();
    loadTrialRecordCount();
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
    if (openSheet.value) {
        sheetRef.value?.releaseHistory?.();
        openSheet.value = null;
    }
    router.replace({
        name: TRIAL_ROUTE[action],
        params: { id: groupId, indictmentId },
    });
}

/** 시트 안 재판 줄의 CTA. 어떤 입장이냐에 따라 변론·투표·현황 셋 중 하나로 간다. */
function onOpenTrial({ item, action }) {
    goToTrial(action, item.groupId, item.id);
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
        <!--
          ===== 다크 헤더 (지방법원 건물) =====
          첫 섹션 제목 「재판 현황」은 헤더 하단 곡면에 얹는다 — 개인 미션 홈(「오늘의 미션」)과
          같은 구조라야 두 화면의 본문 시작점이 같게 읽힌다.
          개인 미션 홈처럼 상태에 따라 비우지는 않는다. 평온 상태에서도 「재판 현황」 아래에
          「기소도 투표도 없어요」가 오는 편이 자연스럽고, 곡면 높이는 어차피 고정이다.
        -->
        <ChallengeCourtHeader
            variant="district"
            :court-image="buildingDistrict"
            court-alt="탕탕 지방법원"
            subtitle="함께 모여 소비 습관을 심판하는 곳"
            :tangi-image="judgeImg"
            tangi-role="판사"
            tangi-name="탕이"
            :quote="judgeQuote"
            skirt-title="재판 현황"
        />

        <!-- ===== 본문 ===== -->
        <main class="gc-body">
            <!--
              내 차례인 재판을 **격자 두 칸으로 접는다**(#448). 목록을 그대로 쌓으면 그룹 수만큼
              홈이 길어져 이 이슈가 없애려던 문제가 그대로 남는다. 격자는 건수와 무관하게 높이가 같다.
              기다리는 재판은 아래 요약 줄이 받는다.
              분기: 내 차례 있음 / 방금 다 처리함 / 기다리는 것만 남음 / 애초에 재판이 없음
            -->
            <GroupTrialTodoGrid
                v-if="myTurnTrials.length"
                :defend-count="defendTrials.length"
                :vote-count="voteTrials.length"
                :defend-urgent="defendUrgent"
                :vote-urgent="voteUrgent"
                @open="openSheet = $event"
            />
            <GroupTodoDoneCard v-else-if="allDone" />
            <!-- 재판은 도는데 내가 할 게 없는 상태. 「평온」이라고 하면 거짓말이 된다 -->
            <p v-else-if="watchingTrials.length" class="gc-trial-idle">지금 내가 할 일은 없어요</p>
            <!-- 기소·투표가 아예 없는 평온 상태. 이 분기가 없으면 자리 전체가 빈 화면이 된다.
                 doneIds 는 DEV 토글로만 채워지므로 allDone 은 실사용에서 거의 오지 않는다. -->
            <template v-else>
                <GroupPeacefulCard />
                <GroupMascotScene scene="peaceful" />
            </template>

            <!--
              기다리는 재판은 한 줄로 접는다. 없애지 않는 이유는 「내 재판이 심판받는 중」이
              할 일은 아니어도 이 화면에서 가장 궁금한 것이기 때문이다.
              마감이 6시간 안쪽인 게 섞여 있으면 접힌 채로도 그것만 밖으로 알린다.

              **0건이어도 줄을 지우지 않는다.** 투표를 마치고 결과를 기다리는 동안 여기를
              들여다보게 되는데, 줄이 통째로 사라지면 「어디서 보던 거였지」를 매번 다시
              찾아야 한다. 자리는 남기고 격자의 0건 칸과 **같은 방식**으로 물러나게 한다 —
              흐리게 + `disabled`(`GroupTrialTodoGrid` 의 `todo-grid__tile--empty`).
              화살표는 뗀다. `›` 는 「눌러서 갈 곳이 있다」는 약속이라 못 누르는 줄에 남기면 거짓말이다.
            -->
            <button
                type="button"
                class="gc-watching"
                :class="{ 'gc-watching--empty': !watchingTrials.length }"
                :disabled="!watchingTrials.length"
                @click="openSheet = 'watching'"
            >
                <!-- 기소장 — 「내 재판이 기소돼 심판받는 중」이 기다림의 대표 상태다 -->
                <img class="gc-watching__art" :src="objIndictImage" alt="" />
                <span class="gc-watching__label">
                    기다리는 재판 {{ watchingTrials.length }}건
                </span>
                <span v-if="watchingUrgent" class="gc-watching__urgent">마감 임박</span>
                <span v-if="watchingTrials.length" class="gc-watching__arrow" aria-hidden="true">
                    ›
                </span>
            </button>

            <!--
              끝난 재판. 기다리는 재판과 **같은 모양**으로 바로 아래 붙인다 —
              둘 다 「내 할 일은 아니지만 궁금한 것」이라 같은 어휘가 맞다.
              시트를 새로 만들지 않고 기록 화면으로 나간다. 시트를 또 만들면
              같은 목록이 두 자리에 생겨 한쪽만 고쳐지는 날이 온다.

              **라벨은 「지난 재판」이 아니라 「재판 기록」이다.** 앞의 줄과 모양이 같은데
              끝이 둘 다 「…재판 N건」이면 두 줄이 한 낱말 차이로만 갈린다. 게다가 위 줄은
              기다리는 재판이 0건이면 통째로 사라져서, 이 줄이 그 자리로 올라와 「기다리는
              재판이 지난 재판으로 바뀐 것」처럼 읽힌다(실제로 그렇게 읽혔다).
              목적지 화면의 제목이 「재판 기록」이므로 진입 이름을 거기에 맞춘다 —
              이 작업 자체가 「이름과 목적지가 어긋나 있다」에서 출발했다.
            -->
            <button
                v-if="recordCount"
                type="button"
                class="gc-watching gc-watching--record"
                @click="goToTrialRecords"
            >
                <!-- 판결이 끝난 재판이라 투표함이 아니라 판사봉이다 -->
                <img class="gc-watching__art" :src="objDefenseImage" alt="" />
                <span class="gc-watching__label">재판 기록 {{ recordCount }}건</span>
                <span class="gc-watching__arrow" aria-hidden="true">›</span>
            </button>

            <!-- 내 챌린지 — 진행 중과 시작 전을 한 카드에 행으로 쌓는다 -->
            <div class="gc-section">
                <div class="gc-section-top">
                    <span class="gc-section-title">내 챌린지</span>
                    <!-- 잘린 게 있으면 몇 개가 더 있는지 여기서 밝힌다. 안 그러면 홈이
                         「내 챌린지는 3개뿐」이라고 잘못 말한다 -->
                    <button type="button" class="gc-view-all" @click="goToAllChallenges">
                        {{ hiddenChallengeCount ? `+${hiddenChallengeCount}개 더` : '전체보기' }} ›
                    </button>
                </div>

                <div class="gc-groups">
                    <p v-if="!sortedChallenges.length" class="gc-groups__empty">
                        아직 참여 중인 챌린지가 없어요
                    </p>

                    <div
                        v-for="ch in homeChallenges"
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

        <!--
          ===== 재판 목록 바텀시트 (#448) =====
          변론·투표·기다리기 **세 목록이 이 하나를 돌려 쓴다.** 격자의 두 칸과 아래 요약 줄이
          모두 여기로 들어온다 — `openSheet` 가 어느 목록인지만 정하고 줄 모양은 전부 같다.
        -->
        <GroupTrialListSheet
            ref="sheetRef"
            :model-value="openSheet !== null"
            :title="sheetTitle"
            :items="sheetItems"
            :countdowns="countdowns"
            @update:model-value="openSheet = $event ? openSheet : null"
            @open="onOpenTrial"
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
    /* 헤더 곡면(court-header__skirt)과 좌우가 같아야 한다 — 곡면에 얹힌 섹션 제목과
       아래 카드의 왼쪽 끝이 어긋나 보인다. 개인 미션 홈 .personal-home__content 와 같은 값 */
    padding: var(--tt-space-2) var(--tt-screen-padding) 0;
    position: relative;
    z-index: 3;
}

/* ── 기다리는 재판 요약 줄 ─────────────── */
/*
 * **흰 면은 주되 그림자는 주지 않는다.** 한때 배경조차 없었는데(`background: none`),
 * 위의 격자 두 칸과 아래 「내 챌린지」 카드가 둘 다 흰 면이라 이 줄만 **두 덩어리 사이의
 * 빈틈**으로 보였다 — 누를 수 있다는 신호가 오른쪽 `›` 하나뿐이었다.
 *
 * 그렇다고 `--tt-elevation-*` 을 얹으면 격자의 세 번째 칸이 되어 「할 일이 하나 더 있다」로
 * 읽힌다. 접어 둔 이유가 그것인데 그러면 도로아미타불이다.
 *
 * 페이지 배경이 `--tt-bg-page`(흰색과 13/255 차이)라 **그림자 없이도 흰 면이 선다.**
 * 그 토큰이 #423 에서 들어온 이유가 정확히 이것이다(`tokens.css` 주석 참고).
 * 결과적으로 계층이 「그림자 유무」로 갈린다 — 격자는 떠 있고, 이 줄은 바닥에 붙어 있다.
 */
.gc-watching {
    width: 100%;
    display: flex;
    align-items: center;
    gap: 6px;
    margin-top: var(--tt-space-2);
    padding: 11px 15px;
    background: var(--tt-bg);
    border: none;
    border-radius: 14px;
    font-family: inherit;
    cursor: pointer;
    transition: background 0.15s ease;
}

/* 0건일 때는 눌리는 느낌을 주지 않는다 — disabled 라 실제로 열리지도 않는다 */
.gc-watching:not(:disabled):active {
    background: var(--tt-bg-fill);
}

/*
 * 0건 줄. 격자의 빈 칸(`todo-grid__tile--empty`)과 같은 어휘를 쓴다 —
 * 면은 한 단계 내리고, 그림은 래스터라 색을 못 바꾸니 `grayscale` 로 톤만 뺀다.
 * 줄 전체에 `opacity` 를 걸지 않는 것도 같은 이유다. 그러면 「고장 난 줄」로 보인다.
 */
.gc-watching--empty {
    background: var(--tt-bg-subtle);
    cursor: default;
}
.gc-watching--empty .gc-watching__art {
    filter: grayscale(1);
    opacity: 0.4;
}
.gc-watching--empty .gc-watching__label {
    color: var(--tt-text-hint);
}

/*
 * 끝난 재판은 진행 중인 것보다 한 단계 물러난다. 흰 면을 쓰면 위 줄(기다리는 재판)과
 * 완전히 같아져서, 마감이 걸린 것과 이미 끝난 것이 화면에서 구분되지 않는다.
 * 위 줄과 붙여 한 묶음으로 읽히게 여백은 좁게 준다.
 */
.gc-watching--record {
    margin-top: 6px;
    background: var(--tt-bg-subtle);
}

/*
 * 격자 두 칸이 판사봉·투표함을 들고 있어서, 이 줄만 그림이 없으면 「같은 묶음의 세 번째」로
 * 안 읽힌다. 다만 **할 일이 아니므로** 타일보다 확실히 작게(26px) 둔다.
 */
.gc-watching__art {
    width: 26px;
    height: 26px;
    object-fit: contain;
    flex: none;
    margin-left: -3px;
}

.gc-watching__label {
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-muted);
}

/* 접힌 줄이 마감을 통째로 감추지 않게 하는 유일한 장치다. 색만으로 말한다 */
.gc-watching__urgent {
    font-size: var(--tt-fs-badge);
    font-weight: var(--tt-fw-black);
    color: var(--tt-danger);
    padding: 2px 7px;
    border-radius: var(--tt-radius-xs);
    background: var(--tt-danger-subtle);
}

.gc-watching__arrow {
    margin-left: auto;
    font-size: var(--tt-fs-body);
    color: var(--tt-text-hint);
}

/* 재판은 도는데 내가 할 게 없는 상태. 카드 자리를 비우지 않을 만큼만 차지한다 */
.gc-trial-idle {
    margin: 0;
    padding: 18px 0;
    background: var(--tt-bg);
    border-radius: var(--tt-radius-xl);
    box-shadow: var(--tt-elevation-2);
    text-align: center;
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-muted);
}

/* ── 진행 중인 챌린지 섹션 ─────────────── */
/*
 * 재판 현황과 내 챌린지는 성격이 다르다 — 앞은 마감이 걸린 할 일 큐고, 뒤는 내가 속한 그룹의
 * 명부다. 10px 로는 두 덩어리가 한 스크롤 흐름으로 이어져 「카드가 계속 나온다」로 읽혔다.
 * 섹션 제목이 자기 앞에 숨 쉴 자리를 갖도록 벌린다.
 */
.gc-section {
    margin-top: var(--tt-space-6);
}

.gc-section-top {
    display: flex;
    align-items: center;
    justify-content: space-between;
}

.gc-section-title {
    font-size: var(--tt-fs-label);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
}

.gc-view-all {
    font-size: var(--tt-fs-body);
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
 * **그림자가 없는 것이 여기서는 위계다** — 위의 재판 현황만 떠 있고 이 명부는 바닥에 붙는다.
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
    font-size: var(--tt-fs-body);
    color: var(--tt-text-muted);
}

/* ── 그룹 카드 ────────────────────────── */
/*
 * 재판 현황 카드보다 한 급 작게 잡는다(패딩·라운드·아이콘). 두 블록이 같은 몸집이면
 * 「무엇을 먼저 봐야 하나」가 안 정해져 화면 전체가 평평하게 읽힌다.
 */
.gc-group-row {
    display: flex;
    align-items: center;
    gap: 11px;
    padding: 11px 14px;
    background: var(--tt-bg);
    border-radius: 16px;
    cursor: pointer;
    transition: opacity 0.15s ease;
}

.gc-group-row:active {
    opacity: 0.6;
}

.gc-group-row__icon {
    width: 38px;
    height: 38px;
    border-radius: 13px;
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
    font-size: var(--tt-fs-badge);
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
    font-size: var(--tt-fs-caption);
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
    font-size: var(--tt-fs-body);
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
    left: var(--tt-screen-padding);
    right: var(--tt-screen-padding);
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
