import test from 'node:test';
import assert from 'node:assert/strict';
import { toChallengeReportModel } from '../src/utils/challengeReport.js';

test('API 개인 성과 응답을 기존 챌린지 리포트 화면 모델로 변환한다', () => {
    const report = toChallengeReportModel({
        period: '2026-07',
        hasPreviousComparison: false,
        difficulties: [{ difficultyName: 'EASY', attempts: 3, successDays: 2, successRate: 66.67 }],
    });

    assert.equal(report.challengeName, '메인 챌린지');
    assert.equal(report.hasPreviousComparison, false);
    assert.deepEqual(report.difficulties[0], {
        difficultyName: 'EASY',
        attempts: 3,
        successDays: 2,
        successRate: 66.67,
        level: '하',
        tone: 'low',
    });
});

test('API 카테고리 효과를 성공 절감액과 실패 초과액 행으로 분리한다', () => {
    const report = toChallengeReportModel({
        categoryEffects: [
            {
                categoryId: 1,
                categoryName: '카페',
                successfulDays: 2,
                savedAmount: 12000,
                failedDays: 1,
                overspentAmount: 0,
            },
        ],
    });

    assert.deepEqual(report.categories, [
        { code: '카', name: '카페', days: 2, amount: 12000, status: 'success' },
        { code: '카', name: '카페', days: 1, amount: 0, status: 'failure' },
    ]);
});

test('빈 기존 categories가 있어도 API 카테고리 효과를 화면 행으로 사용한다', () => {
    const report = toChallengeReportModel({
        categories: [],
        categoryEffects: [
            {
                categoryId: 1,
                categoryName: '카페',
                successfulDays: 2,
                savedAmount: 12000,
                failedDays: 0,
                overspentAmount: 0,
            },
        ],
    });

    assert.deepEqual(report.categories, [
        { code: '카', name: '카페', days: 2, amount: 12000, status: 'success' },
    ]);
});

test('확정 그룹 전적은 API 필드명을 유지하고 전적 부재는 null로 보존한다', () => {
    const groupRecord = {
        participatingGroups: 2,
        survivedCount: 1,
        eliminatedCount: 1,
        indictedCount: 3,
        acquittedCount: 2,
        convictedCount: 1,
    };

    assert.deepEqual(toChallengeReportModel({ groupRecord }).groupRecord, groupRecord);
    assert.equal(toChallengeReportModel({ groupRecord: null }).groupRecord, null);
});
