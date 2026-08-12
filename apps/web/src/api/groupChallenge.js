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
} from '@/fixtures/groupChallenge';
import { MOCK_CHALLENGE_DETAILS, MOCK_CHALLENGE_RANKINGS } from '@/fixtures/groupChallengeDetail';

function clone(value) {
    return JSON.parse(JSON.stringify(value));
}

/**
 * 서버 DTO → 화면이 쓰는 모양.
 *
 * 서버는 ERD 컬럼명을 그대로 쓴다(`memo`, `owner`, `defendant`). 화면은 목데이터 시절의
 * 이름(`rules`, `isOwner`, `isDefendant`)을 쓴다. 아바타 이니셜은 표시 전용이라
 * 서버가 내려주지 않고 닉네임에서 만든다.
 *
 * 여기서 채우지 않는 필드(`myVoteStatus`, `savingsAmount`, `unreadChatCount` …)는
 * 근거 데이터가 아직 없다. 카드들이 falsy 를 기본 상태로 처리하므로 비워둔다.
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
            throw new Error('유효하지 않은 초대 코드입니다.');
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
 * 그룹 챌린지 상세 (재판 현황·랭킹 포함).
 * 서버 미구현 — 기소/투표(tbl_indictment)가 들어와야 만들 수 있다.
 */
export async function fetchGroupChallengeDetail(groupId) {
    if (!isMockMode.value) {
        throw notImplementedYet('그룹 챌린지 상세');
    }
    const detail = MOCK_CHALLENGE_DETAILS[Number(groupId)];
    if (!detail) {
        throw new Error('챌린지를 찾을 수 없습니다.');
    }
    return clone(detail);
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
