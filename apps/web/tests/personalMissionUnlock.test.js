import test from 'node:test';
import assert from 'node:assert/strict';
import { shouldShowPersonalMissionUnlock } from '../src/services/personalMissionFlow.js';

test('소비 데이터가 부족했던 동의 사용자에게 데이터 충족 후 맞춤 미션 개시를 보여준다', () => {
    assert.equal(
        shouldShowPersonalMissionUnlock({
            hasAgreed: true,
            hasEnoughData: true,
            wasDataInsufficient: true,
            hasSeenDataUnlock: false,
        }),
        true,
    );
});

test('신규 충분 데이터 사용자나 이미 확인한 사용자에게는 맞춤 미션 개시를 다시 보여주지 않는다', () => {
    assert.equal(
        shouldShowPersonalMissionUnlock({
            hasAgreed: true,
            hasEnoughData: true,
            wasDataInsufficient: false,
            hasSeenDataUnlock: false,
        }),
        false,
    );

    assert.equal(
        shouldShowPersonalMissionUnlock({
            hasAgreed: true,
            hasEnoughData: true,
            wasDataInsufficient: true,
            hasSeenDataUnlock: true,
        }),
        false,
    );
});
