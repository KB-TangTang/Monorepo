import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { registerHooks } from 'node:module';

/*
 * 홈 「오늘 쓴 돈」 카드 ↔ GET /api/transactions/summary/daily 연결 (이슈 #450 A블록).
 *
 * #450 C블록이 카드를 먼저 만들면서 값을 안 넘겨 「—」·「이번 달 집계 중」으로 떠 있었다.
 * 렌더링 하네스가 없어(`node:test` + 순수 JS) 소스에 연결이 들어 있는지를 검사한다.
 */

const STUB_URL = new URL('./stubs/httpStub.js', import.meta.url).href;

registerHooks({
    resolve(specifier, context, nextResolve) {
        if (specifier === '@/api/http') {
            return { url: STUB_URL, shortCircuit: true };
        }
        return nextResolve(specifier, context);
    },
});

const stub = await import(STUB_URL);
const { fetchHomeSpendingSummary } = await import('../src/api/ledger.js');

function source(path) {
    return readFileSync(new URL(`../${path}`, import.meta.url), 'utf8');
}

const HOME = 'src/views/HomeView.vue';

test('오늘 쓴 돈 요약은 거래내역 하위 경로로 GET 한다', () => {
    stub.reset();
    stub.setGetResponse({
        date: '2026-08-23',
        todayAmount: 32000,
        monthAmount: 486300,
        changeRate: -20.0,
    });

    return fetchHomeSpendingSummary().then((summary) => {
        assert.deepEqual(stub.calls[0], {
            method: 'get',
            args: ['/transactions/summary/daily'],
        });
        /* http.js 인터셉터가 {success,data} 를 이미 벗기므로 data 가 그대로 온다 */
        assert.equal(summary.monthAmount, 486300);
    });
});

test('홈이 카드에 세 값을 실제로 넘긴다', () => {
    /*
     * 이 셋 중 하나라도 빠지면 카드가 그 자리만 「—」·「집계 중」으로 남는데,
     * 화면상으로는 API 실패와 구분이 안 돼 조용히 묻힌다.
     */
    const src = source(HOME);
    assert.match(src, /:today-amount="spending\.todayAmount"/);
    assert.match(src, /:month-amount="spending\.monthAmount"/);
    assert.match(src, /:change-rate="spending\.changeRate"/);
});

test('「아직 백엔드가 없다」 자리표시 주석이 남아 있지 않다', () => {
    /* 주석이 남으면 다음 사람이 「아직 안 붙었다」로 읽고 같은 일을 다시 한다 */
    const src = source(HOME);
    assert.doesNotMatch(src, /아직 백엔드가 없다/);
    assert.doesNotMatch(src, /<HomeSpendingCard @open-ledger/);
});

test('요약 호출은 홈의 allSettled 묶음 안에 들어간다', () => {
    /*
     * 따로 await 하면 이 카드 하나가 실패했을 때 홈 전체가 에러 화면으로 넘어간다 —
     * 기존 규약은 「한 조각이 실패해도 나머지는 그린다」다.
     */
    const src = source(HOME);
    assert.match(src, /Promise\.allSettled\(\[[\s\S]*?fetchHomeSpendingSummary\(\),[\s\S]*?\]\)/);
    assert.match(src, /spending\.value = toHomeSpending\(\s*spendingResult\.status === 'fulfilled'/);
});
