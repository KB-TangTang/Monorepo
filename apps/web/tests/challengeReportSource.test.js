import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { createPinia, setActivePinia } from 'pinia';
import { useChallengeReportStore } from '../src/stores/challengeReport.js';

function source(path) {
    return readFileSync(new URL(`../${path}`, import.meta.url), 'utf8');
}

test('챌린지 리포트 데이터 소스 선택은 화면 이동 동안 유지된다', () => {
    setActivePinia(createPinia());
    const reportStore = useChallengeReportStore();

    assert.equal(reportStore.reportSource, 'mock');
    reportStore.setReportSource('api');

    assert.equal(useChallengeReportStore().reportSource, 'api');
});

test('API 모드 랭킹 조회 실패는 Mock 리포트로 대체하지 않는다', () => {
    const reportView = source('src/views/challenge/report/ChallengeReportView.vue');

    assert.match(
        reportView,
        /else \{\s*const \[challengeReport, ranking\] = await Promise\.all\(\[\s*fetchChallengeReport\(selectedPeriod\.value\),\s*fetchMissionRankings\(selectedPeriod\.value\),\s*\]\);/,
    );
    assert.match(
        reportView,
        /if \(challengeReportStore\.reportSource === 'mock'\) \{\s*report\.value = await fetchMockChallengeReport\(selectedPeriod\.value\);/,
    );
});

test('랭킹 카드에 챌린지 요약을 결합하고 순 절감액 카드보다 먼저 배치한다', () => {
    const content = source('src/components/challenge/report/ChallengeReportContent.vue');
    const rankingCard = source('src/components/challenge/report/ChallengeRankingSummaryCard.vue');
    const groupRecordCard = source('src/components/challenge/report/ChallengeGroupRecordCard.vue');
    const rankingIndex = content.indexOf('<ChallengeRankingSummaryCard');
    const savingsIndex = content.indexOf('<ChallengeConsumptionHabitDropdown');

    assert.ok(rankingIndex >= 0);
    assert.ok(savingsIndex > rankingIndex);
    assert.ok(content.includes('<ChallengeGroupRecordCard'));
    assert.ok(content.includes(':challenge-days="report.challengeDays"'));
    assert.ok(content.includes(':best-streak-days="report.bestStreakDays"'));
    assert.doesNotMatch(content, /획득 점수/);
    assert.doesNotMatch(content, /challenge-summary/);
    assert.match(rankingCard, /도전 일수/);
    assert.match(rankingCard, /최고 연속 성공/);
    assert.match(rankingCard, /var\(--tt-bg\)/);
    assert.match(rankingCard, /가장 잘 지킨 요일/);
    assert.doesNotMatch(rankingCard, /이번 달 챌린지 순위예요/);
    assert.match(rankingCard, /150deg, var\(--tt-accent\) 0 37%/);
    assert.match(content, /탕탕 대법원 월간 판결문/);
    assert.match(content, /지방법원 법정 기록/);
    assert.match(groupRecordCard, /참여한 법정 기록이 없어요!',\s*'친구와 함께 법정을 열어봐요!/);
});
