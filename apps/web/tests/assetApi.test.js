import test from 'node:test';
import assert from 'node:assert/strict';
import { registerHooks } from 'node:module';

const STUB_URL = new URL('./stubs/assetApiStub.js', import.meta.url).href;

registerHooks({
    resolve(specifier, context, nextResolve) {
        if (specifier === './http') {
            return { url: STUB_URL, shortCircuit: true };
        }
        return nextResolve(specifier, context);
    },
});

const stub = await import(STUB_URL);
const { fetchCheckingAccountDetail, fetchSavingsAccountDetail, fetchPaymoneyDetail, fetchLoanAccountDetail } =
    await import('../src/api/asset.js');

test('입출금 계좌 상세는 백엔드 bankCode 를 institutionCode 로 그대로 전달한다 (금융기관 아이콘 매칭용)', async () => {
    stub.reset();
    stub.setGetResponses([
        {
            total: 1000000,
            accounts: [
                {
                    accountId: 1,
                    bankCode: '0004',
                    bankName: 'KB국민은행',
                    shortLabel: 'KB',
                    accountName: 'KB Star 통장',
                    accountNoMasked: '110-***-****23',
                    balance: 1000000,
                    lastSyncAt: '2026-08-19T09:00:00',
                },
            ],
        },
    ]);

    const detail = await fetchCheckingAccountDetail();

    assert.equal(detail.accounts[0].institutionCode, '0004');
});

test('예적금·페이머니 상세도 같은 방식으로 institutionCode 를 전달한다', async () => {
    stub.reset();
    stub.setGetResponses([
        { total: 500000, accounts: [{ accountId: 2, bankCode: '0088', shortLabel: '신한' }] },
    ]);
    const savings = await fetchSavingsAccountDetail();
    assert.equal(savings.accounts[0].institutionCode, '0088');

    stub.reset();
    stub.setGetResponses([
        { total: 30000, accounts: [{ accountId: 3, bankCode: 'PAY_KAKAO', shortLabel: '카카오' }] },
    ]);
    const paymoney = await fetchPaymoneyDetail();
    assert.equal(paymoney.accounts[0].institutionCode, 'PAY_KAKAO');
});

test('대출도 tbl_loan.bank_code(#379 마이그레이션) 를 institutionCode 로 그대로 전달한다', async () => {
    stub.reset();
    stub.setGetResponses([
        {
            total: 5000000,
            loans: [
                {
                    loanId: 1,
                    bankName: 'KB캐피탈',
                    bankCode: 'CP_KB',
                    loanType: '신용대출',
                    balance: 5000000,
                    interestRate: 4.5,
                    maturityDate: '2028-01-01',
                },
            ],
        },
    ]);

    const detail = await fetchLoanAccountDetail();

    assert.equal(detail.loans[0].institutionCode, 'CP_KB');
});

test('2026-08-20 마이그레이션 이전에 동기화된 옛 대출 행은 bank_code 가 없어 이니셜 배지로 대체된다', async () => {
    stub.reset();
    stub.setGetResponses([
        {
            total: 5000000,
            loans: [
                {
                    loanId: 2,
                    bankName: 'KB캐피탈',
                    loanType: '신용대출',
                    balance: 5000000,
                    interestRate: 4.5,
                    maturityDate: '2028-01-01',
                },
            ],
        },
    ]);

    const detail = await fetchLoanAccountDetail();

    assert.equal(detail.loans[0].institutionCode, undefined);
});
