import test from 'node:test';
import assert from 'node:assert/strict';
import { toTodayMissionBriefing } from '../src/services/personalMissionFlow.js';

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
        assignmentReason: '최근 카페 지출이 늘었어요.',
    });

    assert.deepEqual(briefing, {
        missionTitle: '카페 지출 줄이기',
        missionContent: '오늘 카페 지출을 목표 안으로 지켜요.',
        categoryName: '카페/간식',
        alibiCondition: '오늘 5,501원 이하',
        currentAmount: 1200,
        limitAmount: 5500.6,
        streakDays: 0,
        difficultyName: 'NORMAL',
        assignDate: '2026-08-13',
        assignmentReason: '최근 카페 지출이 늘었어요.',
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
