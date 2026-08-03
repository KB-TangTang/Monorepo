import test from 'node:test';
import assert from 'node:assert/strict';
import {
    hasSeenNetSavingsGuide,
    markNetSavingsGuideSeen,
} from '../src/services/challengeReportGuide.js';

test('최초 안내 확인 상태를 월과 무관하게 저장한다', async () => {
    const values = new Map();
    const storage = {
        getItem: (key) => values.get(key) ?? null,
        setItem: (key, value) => values.set(key, value),
    };

    assert.equal(await hasSeenNetSavingsGuide(storage), false);
    await markNetSavingsGuideSeen(storage);
    assert.equal(await hasSeenNetSavingsGuide(storage), true);
});
