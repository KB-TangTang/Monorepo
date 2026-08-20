import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { toChallengeReportModel } from '../src/utils/challengeReport.js';
import { AVAILABLE_MONTHS, REPORTS } from '../src/fixtures/challengeReport.js';

function source(path) {
    return readFileSync(new URL(`../${path}`, import.meta.url), 'utf8');
}

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

test('그룹 전적 상태와 확정 전적을 API 필드명으로 보존한다', () => {
    const groupRecord = {
        participatingGroups: 2,
        survivedCount: 1,
        eliminatedCount: 1,
        indictedCount: 3,
        acquittedCount: 2,
        convictedCount: 1,
    };

    const ready = toChallengeReportModel({ groupRecordState: 'READY', groupRecord });
    const empty = toChallengeReportModel({ groupRecordState: 'EMPTY', groupRecord: null });

    assert.equal(ready.groupRecordState, 'READY');
    assert.deepEqual(ready.groupRecord, groupRecord);
    assert.equal(empty.groupRecordState, 'EMPTY');
    assert.equal(empty.groupRecord, null);
});

test('재판 진행 상태는 부분 확정 전적이 있어도 그대로 보존한다', () => {
    const report = toChallengeReportModel({
        groupRecordState: 'JUDGING',
        groupRecord: { participatingGroups: 1 },
    });

    assert.equal(report.groupRecordState, 'JUDGING');
    assert.deepEqual(report.groupRecord, { participatingGroups: 1 });
});

test('목업 5월과 6월은 각각 챌린지 동의 및 첫 리포트 준비 상태를 재현한다', () => {
    const may = AVAILABLE_MONTHS.find((month) => month.value === '2026-05');
    const june = AVAILABLE_MONTHS.find((month) => month.value === '2026-06');
    const challengeReportApi = source('src/api/challengeReport.js');

    assert.deepEqual(
        { hasReport: may.hasReport, entryState: may.entryState },
        { hasReport: false, entryState: 'NOT_AGREED' },
    );
    assert.deepEqual(
        { hasReport: june.hasReport, entryState: june.entryState },
        { hasReport: false, entryState: 'PREPARING_FIRST_REPORT' },
    );
    assert.equal(REPORTS['2026-05'].entryState, 'NOT_AGREED');
    assert.equal(REPORTS['2026-06'].entryState, 'PREPARING_FIRST_REPORT');
    assert.match(
        challengeReportApi,
        /month\.value <= latestPeriod && \(month\.hasReport \|\| Boolean\(month\.entryState\)\)/,
    );
});
