import test from 'node:test';
import assert from 'node:assert/strict';
import {
    clampHomeProgress,
    formatHomeAmount,
    formatHomeRate,
    getCurrentYearMonth,
    getDaysUntilNextMonth,
    getHomeAssetChange,
    getHomeGroupStatus,
    toHomeMission,
    toHomeReportSummary,
} from '../src/utils/home.js';

test('홈 금액을 천 단위 구분자로 표시한다', () => {
    assert.equal(formatHomeAmount(12840000), '12,840,000');
    assert.equal(formatHomeAmount(43487.7), '43,488');
    assert.equal(formatHomeAmount(43487.4), '43,487');
    assert.equal(formatHomeAmount(null), '0');
});

test('홈 증감률에 방향 부호를 붙인다', () => {
    assert.equal(formatHomeRate(8.4), '+8.4%');
    assert.equal(formatHomeRate(-3.2), '-3.2%');
});

test('순자산 전월 대비 증감 방향에 맞는 문구와 색상 상태를 만든다', () => {
    assert.deepEqual(getHomeAssetChange(3.51), {
        text: '전월 대비 3.51% 증가',
        tone: 'success',
    });
    assert.deepEqual(getHomeAssetChange(-2.1), {
        text: '전월 대비 2.1% 감소',
        tone: 'danger',
    });
    assert.deepEqual(getHomeAssetChange(0), { text: '전월과 동일', tone: 'muted' });
    assert.equal(getHomeAssetChange(null), null);
});

test('챌린지 진행률을 0에서 100 사이로 제한한다', () => {
    assert.equal(clampHomeProgress(-10), 0);
    assert.equal(clampHomeProgress(56), 56);
    assert.equal(clampHomeProgress(120), 100);
});

test('그룹 재판 할 일이 있으면 마감이 가장 임박한 첫 항목을 보여준다', () => {
    const first = {
        id: 10,
        type: 'accuse',
        title: '변론이 필요해요',
        challengeName: '카페 방어전',
    };
    const second = { id: 11, type: 'vote', title: '투표해 주세요' };

    const status = getHomeGroupStatus({ trials: [first, second], activeGroupCount: 1 });

    assert.equal(status.kind, 'accuse');
    assert.equal(status.item, first);
    assert.equal(status.caption, '그 외 할 일 1건');
});

test('진행 중인 그룹에서 할 일이 없으면 순항 상태를 보여준다', () => {
    const status = getHomeGroupStatus({ trials: [], activeGroupCount: 2 });

    assert.equal(status.kind, 'cruising');
    assert.equal(status.title, '그룹 챌린지가 순항 중이에요');
});

test('그룹 조회 실패를 할 일 없음으로 표시하지 않는다', () => {
    const status = getHomeGroupStatus({ trials: [], activeGroupCount: 1, failed: true });

    assert.equal(status.kind, 'error');
    assert.match(status.title, /확인하지 못했어요/);
});

test('오늘 미션을 홈 진행 카드 모델로 변환한다', () => {
    const mission = toHomeMission({
        missionTitle: '카페 방어전',
        targetValue: 8000,
        currentAmount: 4500,
    });

    assert.deepEqual(mission, {
        title: '카페 방어전',
        limitAmount: 8000,
        spentAmount: 4500,
        remainingAmount: 3500,
        progressRate: 56,
    });
});

test('현재 월과 다음 리포트 공개까지 남은 날짜를 계산한다', () => {
    const today = new Date(2026, 7, 18, 12);

    assert.equal(getCurrentYearMonth(today), '2026-08');
    assert.equal(getDaysUntilNextMonth(today), 14);
});

test('가장 절감액이 큰 카테고리로 홈 리포트 요약을 만든다', () => {
    const summary = toHomeReportSummary({
        period: '2026-07',
        hasChallengeHistory: true,
        savedAmount: 128000,
        categoryEffects: [
            { categoryName: '배달', savedAmount: 20000 },
            { categoryName: '카페·간식', savedAmount: 72000 },
        ],
    });

    assert.deepEqual(summary, {
        period: '2026-07',
        month: 7,
        savedAmount: 128000,
        topCategoryName: '카페·간식',
    });
});
