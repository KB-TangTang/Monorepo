/*
 * 그룹 챌린지 API.
 *
 * 화면이 목데이터로 먼저 만들어졌기 때문에 서버 응답을 화면이 쓰던 모양으로
 * 한 번 옮겨준다(`toViewModel`). 화면 컴포넌트를 전부 고치는 대신 여기 한 곳에서 맞춘다.
 *
 * 목/실데이터 전환은 `services/devDataSource` 가 관리한다 (개발 전용).
 */
import http from '@/api/http';
import { isMockMode, notImplementedYet } from '@/services/devDataSource';
import {
    MOCK_GROUPS,
    MOCK_INVITE_CODES,
    MOCK_PRE_START_CHALLENGES,
    MOCK_ACTIVE_LIST_CHALLENGES,
    MOCK_ENDED_CHALLENGES,
    MOCK_TODO_ITEMS,
} from '@/fixtures/groupChallenge';
import { MOCK_CHALLENGE_DETAILS, MOCK_CHALLENGE_RANKINGS } from '@/fixtures/groupChallengeDetail';
import {
    MOCK_TRIAL_DETAIL,
    MOCK_TRIAL_DETAIL_CONFESSED,
    MOCK_TRIAL_DETAIL_DEFENDED,
    MOCK_TRIAL_TRANSACTIONS,
} from '@/fixtures/groupChallengeTrial';
import { formatDeadlineLabel, toTrialProgress } from '@/utils/groupTrial';

function clone(value) {
    return JSON.parse(JSON.stringify(value));
}

/** 지금부터 `ms` 뒤의 절대시각. 목데이터의 상대 마감을 실데이터 모양으로 맞출 때 쓴다. */
function afterNow(ms) {
    return new Date(Date.now() + ms).toISOString();
}

/** `2026-08-05` → `8월 5일`. 화면은 「8월 5일 결산」으로 읽는다. */
function formatMonthDay(isoDate) {
    if (!isoDate) return '';
    const [, month, day] = isoDate.split('-');
    return `${Number(month)}월 ${Number(day)}일`;
}

/**
 * 서버 DTO → 화면이 쓰는 모양.
 *
 * 서버는 ERD 컬럼명을 그대로 쓴다(`memo`, `owner`, `defendant`). 화면은 목데이터 시절의
 * 이름(`rules`, `isOwner`, `isDefendant`)을 쓴다. 아바타 이니셜은 표시 전용이라
 * 서버가 내려주지 않고 닉네임에서 만든다.
 *
 * 배지 값(`defendant` · `myVoteStatus` · `pendingTrialCount`)은 **목록 응답에서만** 채워진다.
 * 상세·참여·초대 미리보기에서는 서버가 `false` · `null` · `0` 을 준다 —
 * 카드들이 falsy 를 「순항중」으로 처리하므로 그대로 흘려보낸다.
 *
 * 채팅 요약(`unreadChatCount`·`lastChatMessage`·`lastChatTime`)은 서버가 그대로 내려주므로
 * 스프레드로 통과한다. 여기서 이름을 바꾸지 않는다.
 *
 * `savingsAmount` 는 아직 근거 데이터가 없다 (이슈 #172).
 */
function toViewModel(dto) {
    return {
        ...dto,
        rules: dto.memo,
        isOwner: dto.owner,
        isDefendant: dto.defendant,
        members: (dto.members ?? []).map((m) => ({
            ...m,
            initial: m.nickname ? m.nickname.charAt(0) : '?',
        })),
    };
}

/**
 * 그룹 챌린지 생성.
 * 화면의 자유 규칙 입력값(`rules`)은 ERD 컬럼명에 맞춰 `memo` 로 보낸다.
 * 정원은 서버가 6으로 고정하므로 보내지 않는다.
 */
export async function createGroupChallenge(form) {
    if (isMockMode.value) {
        return { groupId: 1, inviteCode: 'DL7K2' };
    }
    return http.post('/group-challenges', {
        groupName: form.groupName,
        categoryId: form.categoryId,
        limitAmount: form.limitAmount,
        evalType: form.evalType,
        startDate: form.startDate,
        endDate: form.endDate,
        memo: form.rules,
    });
}

/**
 * 내가 참여 중인 그룹 목록.
 * @param {string[]} statuses 비우면 전체. 「종료됨」 탭은 ['JUDGING','CLOSED'] 를 함께 넘긴다.
 */
export async function fetchMyGroupChallenges(statuses = []) {
    if (isMockMode.value) {
        return clone(mockGroupsByStatus(statuses));
    }
    const params = statuses.length ? { status: statuses.join(',') } : {};
    const list = await http.get('/group-challenges', { params });
    return list.map(toViewModel);
}

function mockGroupsByStatus(statuses) {
    if (statuses.includes('RECRUITING')) return MOCK_PRE_START_CHALLENGES;
    if (statuses.includes('ACTIVE')) return MOCK_ACTIVE_LIST_CHALLENGES;
    if (statuses.includes('CLOSED')) return MOCK_ENDED_CHALLENGES;
    return [
        ...MOCK_PRE_START_CHALLENGES,
        ...MOCK_ACTIVE_LIST_CHALLENGES,
        ...MOCK_ENDED_CHALLENGES,
    ];
}

/** 그룹 요약 조회. 참여자만 볼 수 있다. */
export async function fetchGroupDetail(groupId) {
    if (isMockMode.value) {
        const group = MOCK_GROUPS[Number(groupId)];
        if (!group) {
            throw new Error('그룹을 찾을 수 없습니다.');
        }
        return toViewModel(clone(group));
    }
    return toViewModel(await http.get(`/group-challenges/${groupId}`));
}

/**
 * 초대 코드 미리보기.
 *
 * 코드 자체가 없으면 reject 된다(`GROUP_INVITE_CODE_NOT_FOUND`).
 * 코드는 맞는데 참여만 못 하는 경우는 성공 응답에 `joinable:false` + `reason` 이 담긴다 —
 * 참여 확인 화면이 그룹 정보를 먼저 보여준 다음 사유를 안내해야 하기 때문이다.
 *
 * @returns {{ joinable: boolean, reason: string|null, group: object }}
 */
export async function previewInviteCode(code) {
    if (isMockMode.value) {
        const entry = MOCK_INVITE_CODES[code];
        if (!entry) {
            /* 목데이터에는 실제로 만든 방의 코드가 당연히 없다. 이걸 「존재하지 않는 방」으로
             * 보여주면 서버가 멀쩡한데도 코드가 틀린 줄 알게 된다. 사유를 구분해 던진다. */
            const error = new Error(
                `목데이터 모드에는 없는 코드예요 (${code}). 실제 그룹에 참여하려면 LIVE 로 전환하세요.`,
            );
            error.code = 'MOCK_CODE_NOT_FOUND';
            throw error;
        }
        const group = MOCK_GROUPS[entry.groupId];
        return {
            joinable: !entry.expired,
            reason: entry.expired ? 'EXPIRED' : null,
            group: group ? toViewModel(clone(group)) : null,
        };
    }
    const preview = await http.get(`/group-challenges/invite-codes/${code}`);
    return {
        joinable: preview.joinable,
        reason: preview.reason ?? null,
        group: toViewModel(preview.challenge),
    };
}

/** 참여. 참여 직후 화면이 상세로 넘어가므로 서버가 상세를 그대로 돌려준다. */
export async function joinGroup(groupId) {
    if (isMockMode.value) {
        const group = MOCK_GROUPS[Number(groupId)];
        if (!group) {
            throw new Error('그룹을 찾을 수 없습니다.');
        }
        return toViewModel(clone(group));
    }
    return toViewModel(await http.post(`/group-challenges/${groupId}/members`));
}

/**
 * 홈 「오늘의 할 일」 — 내가 변론하거나 투표해야 하는 재판. 마감 임박순이다.
 *
 * 변론 대기와 투표 대기가 **한 배열로** 섞여 온다. 화면이 하나의 목록으로 보여주고
 * 필터 칩으로만 나누기 때문이다.
 */
export async function fetchMyTrials() {
    if (isMockMode.value) {
        return MOCK_TODO_ITEMS.map((item) => ({
            ...item,
            deadline: afterNow(item.deadlineMinutes * 60000),
        }));
    }
    const list = await http.get('/group-challenges/my-trials');
    return list.map(toTrialViewModel);
}

/**
 * 서버 DTO → TO-DO 아이템.
 *
 * 서버는 재료(`type` · `defendantNickname` · `voteCount`)만 주고 **문구는 여기서 만든다.**
 * 문구를 서버가 만들면 디자인을 고칠 때마다 war 를 다시 올려야 한다.
 */
function toTrialViewModel(dto) {
    const isAccuse = dto.type === 'accuse';
    return {
        ...dto,
        /* 목록 key 이자 카운트다운 키. 기소 ID 는 그룹을 넘어 유일하다. */
        id: dto.indictmentId,
        title: isAccuse
            ? '기소 되어 변론이 필요해요!'
            : `${dto.defendantNickname}님의 변론에 투표하세요`,
        tally: isAccuse ? null : `${dto.voteCount}/${dto.totalVoters} 투표`,
    };
}

/**
 * 그룹 챌린지 상세 (재판 카드 · 참여자 소비 상태 포함).
 *
 * 그룹 정보는 한 겹 없이 평평하게 내려온다(`@JsonUnwrapped`). 그래서 목록과 같은
 * `toViewModel` 을 그대로 태울 수 있다.
 */
export async function fetchGroupChallengeDetail(groupId) {
    if (isMockMode.value) {
        const detail = MOCK_CHALLENGE_DETAILS[Number(groupId)];
        if (!detail) {
            throw new Error('챌린지를 찾을 수 없습니다.');
        }
        return withMockDeadlines(clone(detail));
    }
    return toDetailViewModel(await http.get(`/group-challenges/${groupId}/detail`));
}

/**
 * 상세 응답 → 화면이 쓰는 모양.
 *
 * 서버는 ERD·Lombok 이 정한 이름(`mine`, `defended`, `exceeded`, `profileImageUrl`)을 준다.
 * 화면은 목데이터 시절의 이름을 쓴다. 아바타 이니셜·색은 `UserAvatar` 가 닉네임에서 만들므로
 * 서버가 내려주지 않는다.
 */
function toDetailViewModel(dto) {
    return {
        ...toViewModel(dto),
        indictments: (dto.indictments ?? []).map((item) => ({
            ...item,
            profileImage: item.profileImageUrl,
            isMine: item.mine,
            hasDefended: item.defended,
            settlementDate: formatMonthDay(item.settlementDate),
            /* 서버는 마감 두 개를 다 주지만 카드는 하나만 그린다. 고르는 일을 여기서 끝낸다. */
            deadline: item.status === 'DEFENSE_WAIT' ? item.defenseDeadline : item.voteDeadline,
        })),
        dailyMembers: (dto.dailyMembers ?? []).map((member) => ({
            ...member,
            profileImage: member.profileImageUrl,
            isExceeded: member.exceeded,
        })),
    };
}

/**
 * 목데이터의 마감은 `'02:14:03'` 같은 **남은 시간 문자열**이라 카운트다운이 돌지 않는다.
 * 호출 시점 기준 절대시각으로 바꿔 실데이터와 같은 모양으로 맞춘다.
 * (픽스처에 절대시각을 박아 두면 하루만 지나도 전부 「마감됨」이 된다)
 */
function withMockDeadlines(detail) {
    return {
        ...detail,
        indictments: (detail.indictments ?? []).map((item) => {
            const remaining =
                item.status === 'DEFENSE_WAIT' ? item.defenseDeadline : item.voteDeadline;
            return { ...item, deadline: remainingToDeadline(remaining) };
        }),
    };
}

function remainingToDeadline(text) {
    if (!text) return null;
    const [h = 0, m = 0, s = 0] = text.split(':').map(Number);
    return afterNow(((h * 60 + m) * 60 + s) * 1000);
}

/**
 * 시작 상태 전이 배치를 즉시 돌린다 (개발 전용, 이슈 #152).
 *
 * 원래는 매일 00:01 에 도는 배치다. 시연·검증 때 자정을 기다릴 수 없어서 열어 둔 문이다.
 * 서버가 `app.env=local` 이 아니면 `DEV_API_DISABLED` 로 거절한다 — 배포 환경에서는 눌러도 막힌다.
 *
 * 목데이터 모드에서도 실제 서버로 나간다. 배치는 DB 를 건드리는 일이라 흉내낼 것이 없다.
 *
 * @param {string} [date] 기준일 `yyyy-MM-dd`. 생략하면 서버가 오늘로 잡는다.
 *   **조회 조건(`start_date <= 기준일`)에만 쓰인다** — 시계를 되감는 게 아니라서
 *   상태 변경과 알림 발송은 호출한 지금 일어난다.
 * @returns {{ batch: string, baseDate: string, affected: number }} affected = 상태가 바뀐 그룹 수
 */
export async function runGroupChallengeStatusBatch(date) {
    return http.post('/dev/batches/group-challenge-status', null, {
        params: date ? { date } : {},
    });
}

/* ──────────────────────────────────────────────────────────────
 * 소비 재판 — 상세 · 결산 구간 거래 목록 · 변론 · 혐의 인정 (이슈 #170)
 * ────────────────────────────────────────────────────────────── */

/**
 * 재판 상세. 기소 안내 · 변론 작성 · 변론 완료 화면이 **같은 응답 하나**를 쓴다.
 * 화면마다 다른 엔드포인트를 두면 같은 소비액을 네 곳에서 다르게 계산하게 된다.
 *
 * 그룹 참여자면 누구나 볼 수 있다(배심원도 봐야 한다). 참여자가 아니면 서버가
 * `TRIAL_NOT_FOUND` 로 거절한다 — 기소의 존재 자체를 노출하지 않기 위함이다.
 */
export async function fetchTrialDetail(indictmentId) {
    if (isMockMode.value) {
        return toTrialDetailViewModel(clone(mockTrialDetailNow()));
    }
    return toTrialDetailViewModel(await http.get(`/group-challenges/trials/${indictmentId}`));
}

/*
 * 목데이터 모드의 재판 진행 단계.
 *
 * 상수 하나만 돌려주면 **변론 제출 → 변론 완료** 화면에서 변론이 `null` 이라 요약 카드가
 * 통째로 사라지고, 혐의 인정 완료 화면도 `verdictMethod` 가 없어 판결 문구가 비어 버린다.
 * 목데이터로 흐름을 끝까지 걸어볼 수 없으면 검증 도구로서 쓸모가 없다.
 * 새로고침하면 초기 단계로 돌아간다 — 목데이터에 영속성을 만들 이유는 없다.
 */
let mockStage = 'DEFENSE_WAIT';

function mockTrialDetailNow() {
    if (mockStage === 'DEFENDED') return MOCK_TRIAL_DETAIL_DEFENDED;
    if (mockStage === 'CONFESSED') return MOCK_TRIAL_DETAIL_CONFESSED;
    return MOCK_TRIAL_DETAIL;
}

/**
 * 서버 재판 상세 → 화면이 쓰는 모양.
 *
 * 목데이터도 이 함수를 지나간다 — 픽스처를 화면 이름으로 적어 두면 실데이터에서만
 * 깨지는 매핑 실수를 목데이터 모드가 가려버린다.
 *
 * `caseNumber` · `evalLabel` · `settlementLabel` 은 서버에 없다. 사건 번호와 「일일결산」
 * 같은 문구는 표시 전용이라 서버가 내려주면 디자인을 고칠 때마다 war 를 다시 올려야 한다.
 *
 * ⚠ `guiltyCount` · `innocentCount` · `comments` 는 **손대지 않고 그대로 통과시킨다**(이슈 #171).
 * 개표 전에는 셋 다 `null` 로 오는데, 이것이 「아직 모른다」는 신호다. `?? 0` · `?? []` 로
 * 채우면 투표 중인 재판이 화면에 「0 : 0 · 코멘트 없음」으로 그려져 만장일치 0표처럼 보인다.
 */
function toTrialDetailViewModel(dto) {
    const isDaily = dto.evalType === 'DAILY';
    return {
        ...dto,
        /* 화면 목록 key 이자 카운트다운 키. */
        id: dto.indictmentId,
        caseNumber: toCaseNumber(isDaily ? dto.challengeDate : dto.endDate),
        challengeName: dto.groupName,
        evalLabel: isDaily ? '일일결산' : '기간결산',
        settlementLabel: isDaily
            ? formatMonthDay(dto.challengeDate)
            : `${formatMonthDay(dto.startDate)} ~ ${formatMonthDay(dto.endDate)}`,
        defenseDeadlineLabel: formatDeadlineLabel(dto.defenseDeadline),
        accused: {
            ...(dto.accused ?? {}),
            profileImage: dto.accused?.profileImageUrl ?? null,
            isMine: dto.accused?.mine ?? false,
        },
        /* `defense` 가 null 이면 아직 변론이 없다는 뜻이다. 빈 객체로 채우지 않는다. */
        defense: dto.defense
            ? { ...dto.defense, images: dto.defense.imageUrls ?? [] }
            : null,
    };
}

/** `2026-08-05` → `2026-재판-0805`. 사건 번호는 컬럼이 아니라 결산일에서 만드는 라벨이다. */
function toCaseNumber(isoDate) {
    if (!isoDate) return '';
    const [year, month, day] = isoDate.split('-');
    return `${year}-재판-${month}${day}`;
}

/**
 * 재판 진행 현황 화면 한 벌 (`/group-challenges/:id/trial/:indictmentId`).
 *
 * 상세와 **같은 응답**을 쓴다. 진행 현황만을 위한 엔드포인트를 따로 두면 같은 재판의 단계를
 * 두 곳에서 다르게 계산하게 된다. 화면이 쓰는 4단계 어휘로 옮기는 일만 `toTrialProgress` 가 한다.
 *
 * 상태가 계약을 벗어나면 `null` 이다. 호출부가 그룹 챌린지 홈으로 되돌린다.
 */
export async function fetchTrialProgress(indictmentId) {
    return toTrialProgress(await fetchTrialDetail(indictmentId));
}

/**
 * 결산 구간의 거래 목록. **피고 본인만** 받는다(상세와 엔드포인트를 나눈 이유).
 *
 * 기소를 발화시킨 거래 1건이 아니라 **구간 전체**다. 한도 초과는 누적으로 판정되므로
 * 발화 거래가 문제의 거래라는 보장이 없다 — 친구들 몫까지 대납한 큰 거래는 그 시점에
 * 한도 미달이라 기소를 만들지 않고, 뒤따르는 평범한 소비가 기소를 만든다.
 * 발화 거래만 보이면 정작 대납한 금액을 신고할 방법이 없다.
 *
 * `days[].transactions[].amount` 의 합이 `currentAmount` 와 맞는다(무죄 감액이 생기기 전까지).
 * 화면이 「실제 부담금 합계 vs 한도」를 계산하는 기준이라 어긋나면 판정이 무너진다.
 */
export async function fetchTrialTransactions(indictmentId) {
    if (isMockMode.value) {
        return toTrialTransactionsViewModel(clone(MOCK_TRIAL_TRANSACTIONS));
    }
    return toTrialTransactionsViewModel(
        await http.get(`/group-challenges/trials/${indictmentId}/transactions`),
    );
}

function toTrialTransactionsViewModel(dto) {
    return {
        ...dto,
        days: (dto.days ?? []).map((day) => ({
            ...day,
            dateLabel: formatMonthDay(day.date),
            transactions: (day.transactions ?? []).map((tx) => ({
                ...tx,
                /* LocalTime 은 초가 0이면 `18:42`, 아니면 `18:42:30` 으로 온다. 표시는 분까지. */
                timeLabel: tx.time ? tx.time.slice(0, 5) : '',
            })),
        })),
    };
}

/**
 * 변론 등록 (multipart). 피고 본인만, 변론 마감 전까지 한 번만.
 *
 * **건별 입력값은 올리지 않는다** — 서버는 합계(`actualBurdenAmount`)만 받는다.
 * 거래-변론 연결 테이블을 만들지 않기로 했기 때문이다. 감액(`deductionAmount`)도
 * 클라이언트를 믿지 않고 서버가 `소비액 - 부담금` 으로 직접 계산한다.
 *
 * ⚠ `headers: { 'Content-Type': undefined }` 는 **기본값을 지우는 것**이 핵심이다.
 * `http.js` 인스턴스가 `application/json` 을 박아 두므로 그대로 두면 axios 가 FormData 를
 * JSON.stringify 해버려 파일이 브라우저를 떠나지 못한다 (`api/user.js:49-72` 와 같은 함정).
 *
 * @param {number|string} indictmentId
 * @param {{ content: string, actualBurdenAmount: number, images?: File[] }} payload
 */
export async function submitDefense(indictmentId, { content, actualBurdenAmount, images = [] }) {
    if (isMockMode.value) {
        mockStage = 'DEFENDED';
        return { mocked: true };
    }
    const form = new FormData();
    form.append('content', content);
    form.append('actualBurdenAmount', String(actualBurdenAmount));
    images.forEach((file) => form.append('images', file));

    return http.post(`/group-challenges/trials/${indictmentId}/defense`, form, {
        headers: { 'Content-Type': undefined },
    });
}

/**
 * 혐의 인정 — 투표 없이 즉시 유죄로 종결한다. **되돌릴 수 없다.**
 *
 * 보낼 값이 없어 본문이 비어 있다. 목숨 차감은 여기서 일어나지 않는다(이슈 #172).
 */
export async function confessIndictment(indictmentId) {
    if (isMockMode.value) {
        mockStage = 'CONFESSED';
        return { mocked: true };
    }
    return http.post(`/group-challenges/trials/${indictmentId}/confession`);
}

/**
 * 투표 — 유·무죄 + 익명 한줄 코멘트 (이슈 #171).
 *
 * **응답 본문이 없다.** 여기서 표수를 돌려주면 「개표 전 비율 비공개」 정책이 이 한 곳에서
 * 뚫린다. 제출 뒤 화면은 재판 상세를 다시 읽어 그린다.
 *
 * 코멘트는 선택이다. 서버도 trim 후 빈 값을 NULL 로 저장하지만, 빈 문자열을 실어 보내면
 * 「코멘트를 남겼는데 안 보인다」는 오해를 만든다 — 비어 있으면 `null` 로 보낸다.
 *
 * 실패는 `err.code` 로 갈린다: `VOTE_ALREADY_EXISTS` · `VOTE_NOT_ALLOWED` ·
 * `CANNOT_VOTE_OWN_TRIAL` · `INVALID_VERDICT` · `COMMENT_TOO_LONG` · `TRIAL_NOT_FOUND`.
 *
 * @param {number|string} indictmentId
 * @param {{ verdict: 'GUILTY'|'INNOCENT', comment?: string }} payload
 */
export async function submitVote(indictmentId, { verdict, comment = '' }) {
    if (isMockMode.value) {
        /*
         * 목데이터로는 투표 화면을 끝까지 걸을 수 없다. 재판 상세 픽스처의 피고가 「나」라서
         * (`MOCK_TRIAL_DETAIL.accused.mine = true`) 투표 화면이 진입 단계에서 되돌려 보낸다.
         * 배심원 시점 픽스처를 하나 더 두면 같은 재판이 목/실에서 다른 사람 것이 되어 더 헷갈린다 —
         * 투표 흐름 검증은 DEV 데이터 출처 토글로 실서버를 쓴다.
         */
        return { mocked: true };
    }
    const text = (comment ?? '').trim();
    return http.post(`/group-challenges/trials/${indictmentId}/votes`, {
        verdict,
        comment: text === '' ? null : text,
    });
}

/**
 * 그룹 챌린지 생존/누적 순위 (명예 법정).
 * 서버 미구현 — 일일 평가 결과가 쌓여야 만들 수 있다.
 */
export async function fetchGroupChallengeRanking(groupId) {
    if (!isMockMode.value) {
        throw notImplementedYet('명예 법정 순위');
    }
    const ranking = MOCK_CHALLENGE_RANKINGS[Number(groupId)];
    if (!ranking) {
        throw new Error('순위 정보를 찾을 수 없습니다.');
    }
    return clone(ranking);
}
