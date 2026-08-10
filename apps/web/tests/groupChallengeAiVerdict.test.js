import test from 'node:test';
import assert from 'node:assert/strict';
import {
    AI_VERDICT_OUTCOMES,
    getAiVerdict,
    toggleDevelopmentAiVerdict,
} from '../src/fixtures/groupChallengeAiVerdict.js';

test('AI 판결 목업의 기본 결과는 유죄이며 동점 투표를 제공한다', () => {
    const verdict = getAiVerdict();

    assert.equal(verdict.outcome, AI_VERDICT_OUTCOMES.GUILTY);
    assert.equal(verdict.guiltyVotes, 3);
    assert.equal(verdict.innocentVotes, 3);
});

test('개발용 판결 전환은 유죄와 무죄를 번갈아 제공한다', () => {
    const innocent = toggleDevelopmentAiVerdict();
    const guilty = toggleDevelopmentAiVerdict();

    assert.equal(innocent.outcome, AI_VERDICT_OUTCOMES.INNOCENT);
    assert.equal(guilty.outcome, AI_VERDICT_OUTCOMES.GUILTY);
});
