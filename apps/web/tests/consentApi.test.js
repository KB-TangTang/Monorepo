import test from 'node:test';
import assert from 'node:assert/strict';
import { registerHooks } from 'node:module';

const STUB_URL = new URL('./stubs/httpStub.js', import.meta.url).href;

registerHooks({
    resolve(specifier, context, nextResolve) {
        if (specifier === '@/api/http') {
            return { url: STUB_URL, shortCircuit: true };
        }
        return nextResolve(specifier, context);
    },
});

const stub = await import('./stubs/httpStub.js');
const { fetchMyConsents, submitConsents, withdrawConsent } = await import('../src/api/consent.js');
const {
    fetchMissionMonthlyScore,
    fetchMissionRankingMonths,
    fetchMissionRankings,
    fetchTodayMission,
    syncPersonalMissionUnlock,
    acknowledgePersonalMissionUnlock,
} =
    await import('../src/api/personalMission.js');

test('챌린지 참여 동의는 CHALLENGE scope와 단일 동의 항목으로 저장한다', async () => {
    stub.reset();

    await submitConsents('CHALLENGE', [{ type: 'CHALLENGE', agreed: true }]);

    assert.deepEqual(stub.calls[0], {
        method: 'post',
        args: [
            '/consents',
            {
                scope: 'CHALLENGE',
                agreements: [{ type: 'CHALLENGE', agreed: true }],
            },
        ],
    });
});

test('내 동의 현황과 챌린지 철회는 공통 consent API를 사용한다', async () => {
    stub.reset();

    await fetchMyConsents();
    await withdrawConsent('CHALLENGE');

    assert.equal(stub.calls[0].method, 'get');
    assert.equal(stub.calls[0].args[0], '/consents/me');
    assert.equal(stub.calls[1].method, 'post');
    assert.equal(stub.calls[1].args[0], '/consents/CHALLENGE/withdraw');
});

test('오늘 개인 미션은 today mission API로 조회한다', async () => {
    stub.reset();

    await fetchTodayMission();

    assert.equal(stub.calls[0].method, 'get');
    assert.equal(stub.calls[0].args[0], '/missions/today');
});

/* 동기화는 본문을 보내지 않는다 - 자격 판정은 서버가 한다(이슈 #315 (1)). */
test('맞춤 미션 개시 상태를 서버와 동기화하고 확인 처리한다', async () => {
    stub.reset();

    await syncPersonalMissionUnlock();
    await acknowledgePersonalMissionUnlock();

    assert.deepEqual(stub.calls[0], {
        method: 'post',
        args: ['/main-challenge/mission-unlock/status'],
    });
    assert.deepEqual(stub.calls[1], {
        method: 'patch',
        args: ['/main-challenge/mission-unlock/acknowledge'],
    });
});

test('개인 미션 월간 점수는 monthly score API로 조회한다', async () => {
    stub.reset();

    await fetchMissionMonthlyScore();

    assert.equal(stub.calls[0].method, 'get');
    assert.equal(stub.calls[0].args[0], '/missions/monthly-score');
});

test('개인 미션 랭킹은 선택한 월을 쿼리 파라미터로 조회한다', async () => {
    stub.reset();

    await fetchMissionRankings('2026-07');

    assert.deepEqual(stub.calls[0], {
        method: 'get',
        args: ['/missions/rankings', { params: { yearMonth: '2026-07' } }],
    });
});

test('랭킹 데이터가 있는 월 목록을 조회한다', async () => {
    stub.reset();

    await fetchMissionRankingMonths();

    assert.equal(stub.calls[0].method, 'get');
    assert.equal(stub.calls[0].args[0], '/missions/rankings/months');
});
