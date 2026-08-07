import { MOCK_GROUPS, MOCK_INVITE_CODES } from '@/fixtures/groupChallenge';
import { MOCK_CHALLENGE_DETAILS, MOCK_CHALLENGE_RANKINGS } from '@/fixtures/groupChallengeDetail';

function clone(value) {
    return JSON.parse(JSON.stringify(value));
}

export async function fetchGroupDetail(groupId) {
    const id = Number(groupId);
    const group = MOCK_GROUPS[id];
    if (!group) {
        throw new Error('그룹을 찾을 수 없습니다.');
    }
    return clone(group);
}

export async function validateInviteCode(code) {
    const entry = MOCK_INVITE_CODES[code];
    if (!entry) {
        throw new Error('유효하지 않은 초대 코드입니다.');
    }
    const group = MOCK_GROUPS[entry.groupId];
    return {
        valid: !entry.expired,
        expired: entry.expired,
        group: group ? clone(group) : null,
    };
}

export async function joinGroup(groupId) {
    const id = Number(groupId);
    const group = MOCK_GROUPS[id];
    if (!group) {
        throw new Error('그룹을 찾을 수 없습니다.');
    }
    return { success: true, groupId: id };
}

/**
 * 그룹 챌린지 상세 조회.
 * API: GET /api/v1/groupChallenges/:groupId/detail
 */
export async function fetchGroupChallengeDetail(groupId) {
    const id = Number(groupId);
    const detail = MOCK_CHALLENGE_DETAILS[id];
    if (!detail) {
        throw new Error('챌린지를 찾을 수 없습니다.');
    }
    return clone(detail);
}

/**
 * 그룹 챌린지 생존/누적 순위 조회 (명예 법정).
 * API: GET /api/v1/groupChallenges/:groupId/ranking
 */
export async function fetchGroupChallengeRanking(groupId) {
    const id = Number(groupId);
    const ranking = MOCK_CHALLENGE_RANKINGS[id];
    if (!ranking) {
        throw new Error('순위 정보를 찾을 수 없습니다.');
    }
    return clone(ranking);
}
