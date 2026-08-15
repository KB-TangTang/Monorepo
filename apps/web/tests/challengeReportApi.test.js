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
