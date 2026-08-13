import test from 'node:test';
import assert from 'node:assert/strict';
import { registerHooks } from 'node:module';

const STUB_URL = new URL('./stubs/monthlyConsumptionApiStub.js', import.meta.url).href;

registerHooks({
    resolve(specifier, context, nextResolve) {
        if (specifier === '@/api/http') {
            return { url: STUB_URL, shortCircuit: true };
        }
        return nextResolve(specifier, context);
    },
});

const stub = await import(STUB_URL);
const { fetchMonthlyConsumptionReport } = await import('../src/api/monthlyConsumption.js');

function setMonthlyReportResponses(aiAnalysis) {
    stub.setGetResponses([
        {
            yearMonth: '2026-06',
            totalSpent: 2887999,
            hasPreviousComparison: true,
            monthOverMonthRate: -28.87,
            fixedExpenseCandidateCount: 0,
        },
        { items: [] },
        { parentCategories: [], categories: [] },
        aiAnalysis,
    ]);
}

test('NOT_REQUESTED 분석은 리포트 진입 시 한 번 생성하고 결과를 화면 모델에 결합한다', async () => {
    stub.reset();
    setMonthlyReportResponses({ status: 'NOT_REQUESTED', feedbacks: [], savingsAnalogy: null });
    stub.setPostResponses([
        {
            status: 'COMPLETED',
            feedbacks: ['새 분석 결과'],
            savingsAnalogy: '이번달 아낀 10,000원은 커피 2잔',
        },
    ]);

    const report = await fetchMonthlyConsumptionReport('2026-06');

    assert.equal(stub.calls.filter((call) => call.method === 'get').length, 4);
    assert.deepEqual(stub.calls[4], {
        method: 'post',
        args: [
            '/reports/monthly/ai-analysis',
            null,
            { params: { yearMonth: '2026-06' }, timeout: 30_000 },
        ],
    });
    assert.equal(report.aiAnalysisStatus, 'COMPLETED');
    assert.deepEqual(report.feedbacks, ['새 분석 결과']);
    assert.equal(report.savingsAnalogy, '이번달 아낀 10,000원은 커피 2잔');
});

test('완료·진행 중·실패 분석은 리포트 진입 시 자동 생성하지 않는다', async () => {
    for (const status of ['COMPLETED', 'IN_PROGRESS', 'FAILED']) {
        stub.reset();
        setMonthlyReportResponses({ status, feedbacks: [], savingsAnalogy: null });

        await fetchMonthlyConsumptionReport('2026-06');

        assert.equal(
            stub.calls.some((call) => call.method === 'post'),
            false,
            `${status} 상태에서는 자동 AI 생성 요청을 보내면 안 됩니다.`,
        );
    }
});
