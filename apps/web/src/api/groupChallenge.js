import { MOCK_GROUPS, MOCK_INVITE_CODES } from '@/fixtures/groupChallenge';

function clone(value) {
    return JSON.parse(JSON.stringify(value));
}

export async function fetchGroupDetail(groupId) {
    const group = MOCK_GROUPS[groupId];
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
    const group = MOCK_GROUPS[groupId];
    if (!group) {
        throw new Error('그룹을 찾을 수 없습니다.');
    }
    return { success: true, groupId };
}
