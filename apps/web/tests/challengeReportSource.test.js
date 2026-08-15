import test from 'node:test';
import assert from 'node:assert/strict';
import { createPinia, setActivePinia } from 'pinia';
import { useChallengeReportStore } from '../src/stores/challengeReport.js';

test('챌린지 리포트 데이터 소스 선택은 화면 이동 동안 유지된다', () => {
    setActivePinia(createPinia());
    const reportStore = useChallengeReportStore();

    assert.equal(reportStore.reportSource, 'mock');
    reportStore.setReportSource('api');

    assert.equal(useChallengeReportStore().reportSource, 'api');
});
