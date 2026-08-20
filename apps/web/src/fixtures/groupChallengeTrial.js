/*
 * 소비 재판 목데이터 (이슈 #170).
 *
 * **서버 DTO 모양 그대로 적는다.** 화면이 쓰는 이름(`caseNumber`·`challengeName`·`isMine`)으로
 * 적으면 목데이터만 통과하고 실데이터에서 깨지는 매핑 실수를 못 잡는다 —
 * `api/groupChallenge.js` 의 `toTrialDetailViewModel` 을 목/실 양쪽이 함께 지나간다.
 *
 * 근거 계약: `challenge/dto/GroupTrialDetailDto` · `TrialTransactionsDto`.
 */

/** 기준 시각을 고정하지 않는다 — 픽스처에 절대시각을 박으면 하루 뒤 전부 「마감됨」이 된다. */
function isoAfter(hours) {
    return new Date(Date.now() + hours * 3600000).toISOString().slice(0, 19);
}

function isoBefore(hours) {
    return isoAfter(-hours);
}

function today() {
    return new Date().toISOString().slice(0, 10);
}

/**
 * 변론 대기 중인 일일결산 기소 한 건.
 *
 * 소비액 37,400원은 아래 `MOCK_TRIAL_TRANSACTIONS` 의 거래 합과 **같아야 한다.**
 * 두 값이 어긋나면 변론 화면의 「실제 부담금 합계 vs 한도」 판정이 목데이터에서만 틀어진다.
 */
export const MOCK_TRIAL_DETAIL = {
    indictmentId: 1,
    groupId: 1,
    groupName: '배달 소비 줄이기',
    status: 'DEFENSE_WAIT',
    result: null,
    verdictMethod: null,
    message: '하루 기준 금액을 12,400원 넘겼어요.',
    accused: {
        userId: 1,
        nickname: '지민',
        profileImageUrl: null,
        mine: true,
    },
    evalType: 'DAILY',
    challengeDate: today(),
    startDate: today(),
    endDate: today(),
    categoryId: 3,
    categoryName: '배달앱',
    limitAmount: 25000,
    currentAmount: 37400,
    exceededAmount: 12400,
    createdAt: isoBefore(3),
    defenseDeadline: isoAfter(3),
    voteDeadline: isoAfter(27),
    defense: null,
    myVerdict: null,
    voteCount: 0,
    totalVoters: 3,
};

/** 이미 변론을 낸 상태 — 변론 완료 화면이 쓴다. */
export const MOCK_TRIAL_DETAIL_DEFENDED = {
    ...MOCK_TRIAL_DETAIL,
    status: 'VOTING',
    defense: {
        content: '팀 회식비를 제가 먼저 결제하고 나중에 정산받았어요. 실제로 제가 쓴 금액은 8,000원입니다.',
        actualBurdenAmount: 8000,
        deductionAmount: 29400,
        imageUrls: [],
        createdAt: isoBefore(1),
    },
};

/** 혐의 인정으로 종결된 상태 — 혐의 인정 완료 화면이 쓴다. */
export const MOCK_TRIAL_DETAIL_CONFESSED = {
    ...MOCK_TRIAL_DETAIL,
    status: 'GUILTY',
    result: true,
    verdictMethod: 'CONFESSION',
};

/**
 * 결산 구간의 거래 목록.
 *
 * 일일결산이라 묶음이 하나다. 기간결산은 거래가 있는 날만큼 여러 개가 온다.
 * `transactions[].amount` 의 합 = `days[].dailyAmount`, 그 합 = `currentAmount` 다.
 */
export const MOCK_TRIAL_TRANSACTIONS = {
    evalType: 'DAILY',
    limitAmount: 25000,
    currentAmount: 37400,
    days: [
        {
            date: today(),
            dailyAmount: 37400,
            transactions: [
                {
                    transactionId: 101,
                    time: '18:42',
                    merchantName: '배달의민족',
                    amount: 24000,
                    categoryName: '배달앱',
                    paymentMethod: 'KB국민카드',
                    isRefund: false,
                },
                {
                    transactionId: 102,
                    time: '12:10',
                    merchantName: '스타벅스 강남점',
                    amount: 9800,
                    categoryName: '카페/간식',
                    paymentMethod: 'KB국민카드',
                    isRefund: false,
                },
                {
                    transactionId: 103,
                    time: null,
                    merchantName: '편의점',
                    amount: 3600,
                    categoryName: null,
                    paymentMethod: 'KB국민은행 입출금',
                    isRefund: false,
                },
            ],
        },
    ],
};
