import test from 'node:test';
import assert from 'node:assert/strict';
import {
    formatMissionAssignmentSummary,
    toWatchCategoryModel,
    toTodayMissionBriefing,
    toWeeklyVerdictModel,
} from '../src/services/personalMissionFlow.js';

test('이번 주 판정을 월요일부터 일요일 순서로 변환한다', () => {
    const model = toWeeklyVerdictModel(
        {
            streakCount: 5,
            longestStreakCount: 9,
            weekStartDate: '2026-08-10',
            weeklyResults: [
                { date: '2026-08-10', result: 'SUCCESS' },
                { date: '2026-08-11', result: 'FAIL' },
                { date: '2026-08-13', result: 'PENDING' },
            ],
        },
        new Date('2026-08-13T12:00:00'),
    );

    assert.equal(model.streakDays, 5);
    assert.equal(model.longestStreakDays, 9);
    assert.deepEqual(
        model.days.map(({ dow, status }) => ({ dow, status })),
        [
            { dow: '월', status: 'success' },
            { dow: '화', status: 'failed' },
            { dow: '수', status: 'pending' },
            { dow: '목', status: 'today' },
            { dow: '금', status: 'pending' },
            { dow: '토', status: 'pending' },
            { dow: '일', status: 'pending' },
        ],
    );
});

test('오늘 미션 응답을 브리핑 카드 표시 모델로 변환한다', () => {
    const briefing = toTodayMissionBriefing({
        missionTitle: '카페 지출 줄이기',
        missionContent: '오늘 카페 지출을 목표 안으로 지켜요.',
        categoryName: '카페/간식',
        parentCategoryName: '식비',
        missionType: 'RELATIVE',
        targetValue: '5500.60',
        currentAmount: '1200.00',
        difficultyName: 'NORMAL',
        assignDate: '2026-08-13',
        streakDays: 4,
        assignmentReason: '최근 카페 지출이 늘었어요.',
    });

    assert.deepEqual(briefing, {
        missionTitle: '카페 지출 줄이기',
        missionContent: '오늘 카페 지출을 목표 안으로 지켜요.',
        categoryName: '카페/간식',
        alibiCondition: '오늘 5,501원 이하',
        currentAmount: 1200,
        limitAmount: 5500.6,
        streakDays: 4,
        difficultyName: 'NORMAL',
        assignDate: '2026-08-13',
        assignmentReason: '최근 카페 지출이 늘었어요.',
    });
});

test('상위 소비 카테고리를 비중과 최근 미션 날짜·결과로 변환한다', () => {
    const model = toWatchCategoryModel({
        analysisStartDate: '2026-07-16',
        analysisEndDate: '2026-08-12',
        topCategories: [
            {
                categoryId: 18,
                categoryName: '카페/간식',
                spendingRatio: '18.42',
                latestMissionAssignDate: '2026-08-10',
                latestMissionResult: 'SUCCESS',
            },
        ],
    });

    assert.deepEqual(model, {
        period: '7/16 – 8/12',
        items: [
            {
                categoryId: 18,
                name: '카페/간식',
                ratio: 18,
                assignDate: '8/10',
                result: 'SUCCESS',
            },
        ],
    });
});

test('오늘 지출이 없으면 프로그래스바를 표시할 수 있도록 현재 금액을 0으로 변환한다', () => {
    const briefing = toTodayMissionBriefing({
        missionType: 'RELATIVE',
        targetValue: '65085.80',
    });

    assert.equal(briefing.alibiCondition, '오늘 65,086원 이하');
    assert.equal(briefing.currentAmount, 0);
});

test('세부 카테고리가 없으면 상위 카테고리를 사용한다', () => {
    const briefing = toTodayMissionBriefing({
        missionTitle: '무지출 미션',
        parentCategoryName: '교통',
        guideMessage: '오늘은 택시를 타지 않아요.',
        targetValue: null,
    });

    assert.equal(briefing.categoryName, '교통');
    assert.equal(briefing.alibiCondition, '오늘은 택시를 타지 않아요.');
    assert.equal(briefing.limitAmount, 0);
});

test('재배정 전후 비교용으로 난이도와 절감률, 목표 금액을 표시한다', () => {
    const summary = formatMissionAssignmentSummary({
        difficultyName: 'HARD',
        targetRate: '45.00',
        targetValue: '13200.00',
    });

    assert.equal(summary, 'HARD · 45% · 목표 13,200원');
    assert.equal(formatMissionAssignmentSummary(null), '배정 없음');
});
