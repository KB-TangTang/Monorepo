export const MOCK_GROUPS = {
    'mock-group-1': {
        id: 'mock-group-1',
        groupName: '배달 소비 줄이기',
        evalType: 'DAILY',
        categoryName: '총 소비',
        limitAmount: 15000,
        startDate: '2026-08-06',
        endDate: '2026-08-12',
        inviteCode: '22222',
        displayCode: 'TANG-0806',
        status: 'RECRUITING',
        memberCount: 1,
        maxMembers: 6,
        rules: '하루 배달 1회 이하, 커피는 허용',
    },
    'mock-group-2': {
        id: 'mock-group-2',
        groupName: '카페비 방어단',
        evalType: 'PERIOD',
        categoryName: '카페',
        limitAmount: 70000,
        startDate: '2026-08-10',
        endDate: '2026-08-16',
        inviteCode: '11111',
        displayCode: 'TANG-0810',
        status: 'RECRUITING',
        memberCount: 4,
        maxMembers: 6,
        rules: '평일에는 테이크아웃 커피만 인정해요.',
    },
};

export const MOCK_INVITE_CODES = {
    '22222': { groupId: 'mock-group-1', expired: false },
    '11111': { groupId: 'mock-group-2', expired: true },
};
