import test from 'node:test';
import assert from 'node:assert/strict';
import {
    buildAgreeAgainPayload,
    buildAgreementState,
    buildAgreementStateFromConsents,
    canAgreeAgain,
    canSubmit,
    canWithdraw,
    consentStatusText,
    isAllChecked,
    toAgreements,
    toggleAll,
} from '../src/utils/consent.js';

const ITEMS = [
    { type: 'TERMS', required: true, label: '서비스 이용약관', termsUrl: 'u/terms' },
    { type: 'PRIVACY', required: true, label: '개인정보 수집·이용', termsUrl: 'u/privacy' },
    { type: 'FINANCIAL_DATA', required: true, label: '금융정보 수집·이용', termsUrl: 'u/fin' },
    { type: 'AI_USAGE', required: false, label: 'AI 소비 분석 활용', termsUrl: 'u/ai' },
    { type: 'MARKETING', required: false, label: '마케팅 알림 수신', termsUrl: 'u/mkt' },
];

test('초기 상태는 모든 항목이 미체크다', () => {
    assert.deepEqual(buildAgreementState(ITEMS), {
        TERMS: false,
        PRIVACY: false,
        FINANCIAL_DATA: false,
        AI_USAGE: false,
        MARKETING: false,
    });
});

test('전체 동의를 켜면 모든 항목이 켜지고, 끄면 모두 꺼진다', () => {
    const allOn = toggleAll(ITEMS, true);
    assert.equal(Object.values(allOn).every(Boolean), true);

    const allOff = toggleAll(ITEMS, false);
    assert.equal(Object.values(allOff).some(Boolean), false);
});

test('개별 항목이 하나라도 꺼지면 전체 동의는 해제 상태다', () => {
    const state = toggleAll(ITEMS, true);
    assert.equal(isAllChecked(ITEMS, state), true);

    state.MARKETING = false;
    assert.equal(isAllChecked(ITEMS, state), false);
});

test('필수 항목이 모두 켜져야 제출할 수 있다', () => {
    const state = buildAgreementState(ITEMS);
    assert.equal(canSubmit(ITEMS, state), false);

    state.TERMS = true;
    state.PRIVACY = true;
    assert.equal(canSubmit(ITEMS, state), false, '금융정보가 남아 있으면 아직 불가');

    state.FINANCIAL_DATA = true;
    assert.equal(canSubmit(ITEMS, state), true, '선택 항목은 제출 조건과 무관');
});

test('선택 항목만 켜져 있으면 제출할 수 없다', () => {
    const state = buildAgreementState(ITEMS);
    state.AI_USAGE = true;
    state.MARKETING = true;
    assert.equal(canSubmit(ITEMS, state), false);
});

test('요청 본문 형태로 변환한다', () => {
    const state = buildAgreementState(ITEMS);
    state.TERMS = true;

    assert.deepEqual(toAgreements(state), [
        { type: 'TERMS', agreed: true },
        { type: 'PRIVACY', agreed: false },
        { type: 'FINANCIAL_DATA', agreed: false },
        { type: 'AI_USAGE', agreed: false },
        { type: 'MARKETING', agreed: false },
    ]);
});

test('기존 동의 행이 없으면(최초 사용자) 모든 항목이 미체크로 시작한다', () => {
    assert.deepEqual(buildAgreementStateFromConsents(ITEMS, []), {
        TERMS: false,
        PRIVACY: false,
        FINANCIAL_DATA: false,
        AI_USAGE: false,
        MARKETING: false,
    });
});

test('기존에 동의한 선택 항목은 체크된 상태로 시작한다', () => {
    const myConsents = [
        { type: 'TERMS', agreed: true },
        { type: 'PRIVACY', agreed: true },
        { type: 'FINANCIAL_DATA', agreed: true },
        { type: 'AI_USAGE', agreed: true },
        { type: 'MARKETING', agreed: false },
    ];

    const state = buildAgreementStateFromConsents(ITEMS, myConsents);
    assert.equal(state.AI_USAGE, true, '동의한 선택 항목은 재동의 화면에서도 유지돼야 한다');
    assert.equal(state.MARKETING, false);
});

test('기존에 철회한 선택 항목은 미체크로 시작한다', () => {
    const myConsents = [{ type: 'AI_USAGE', agreed: false }];

    const state = buildAgreementStateFromConsents(ITEMS, myConsents);
    assert.equal(state.AI_USAGE, false);
});

test('카탈로그에는 있지만 기존 동의 행이 없는 항목은 미체크로 시작한다', () => {
    const myConsents = [{ type: 'TERMS', agreed: true }];

    const state = buildAgreementStateFromConsents(ITEMS, myConsents);
    assert.equal(state.TERMS, true);
    assert.equal(state.MARKETING, false, '행이 없는 항목은 안전하게 미동의로 취급한다');
});

test('canWithdraw: 동의 중이면서 철회 가능한 항목만 끌 수 있다', () => {
    assert.equal(canWithdraw({ agreed: true, withdrawable: true }), true);
    assert.equal(
        canWithdraw({ agreed: false, withdrawable: true }),
        false,
        '이미 철회된 항목은 잠근다',
    );
    assert.equal(canWithdraw({ agreed: true, withdrawable: false }), false, '필수 약관은 잠근다');
    assert.equal(canWithdraw({ agreed: false, withdrawable: false }), false);
});

test('consentStatusText: 철회 상태면 철회함만 보여준다', () => {
    assert.equal(consentStatusText({ agreed: false, termsVersion: 'v2.3' }), '철회함');
});

test('consentStatusText: 동의 상태면 버전·만료일을 이어 붙인다', () => {
    assert.equal(consentStatusText({ agreed: true }), '동의함');
    assert.equal(consentStatusText({ agreed: true, termsVersion: 'v2.3' }), '동의함 · v2.3');
    assert.equal(
        consentStatusText({ agreed: true, termsVersion: 'v2.3', expiresAt: '2027-08-06T10:00:00' }),
        '동의함 · v2.3 · 만료 2027-08-06',
    );
});

test('canAgreeAgain: 철회한 항목만 다시 켤 수 있다', () => {
    assert.equal(canAgreeAgain({ agreed: false, withdrawable: true, scope: 'SIGNUP' }), true);
    assert.equal(
        canAgreeAgain({ agreed: true, withdrawable: true, scope: 'SIGNUP' }),
        false,
        '이미 동의 중이면 켤 게 없다',
    );
    assert.equal(
        canAgreeAgain({ agreed: false, withdrawable: false, scope: 'SIGNUP' }),
        false,
        '철회 불가 항목은 이 화면에서 되살리지 않는다',
    );
    assert.equal(
        canAgreeAgain({ agreed: false, withdrawable: true }),
        false,
        'scope 를 모르면 어느 묶음으로 저장할지 알 수 없다',
    );
});

test('buildAgreeAgainPayload: 같은 scope 항목을 현재 상태 그대로 싣고 대상만 켠다', () => {
    const myConsents = [
        { type: 'TERMS', scope: 'SIGNUP', agreed: true },
        { type: 'PRIVACY', scope: 'SIGNUP', agreed: true },
        { type: 'FINANCIAL_DATA', scope: 'SIGNUP', agreed: true },
        { type: 'AI_USAGE', scope: 'SIGNUP', agreed: false },
        { type: 'MARKETING', scope: 'SIGNUP', agreed: false },
        { type: 'THIRD_PARTY', scope: 'FINANCIAL', agreed: true },
    ];

    const payload = buildAgreeAgainPayload(myConsents, { type: 'MARKETING', scope: 'SIGNUP' });

    assert.deepEqual(payload, [
        { type: 'TERMS', agreed: true },
        { type: 'PRIVACY', agreed: true },
        { type: 'FINANCIAL_DATA', agreed: true },
        { type: 'AI_USAGE', agreed: false },
        { type: 'MARKETING', agreed: true },
    ]);
});

test('buildAgreeAgainPayload: 다른 scope 항목은 건드리지 않는다', () => {
    const myConsents = [
        { type: 'MARKETING', scope: 'SIGNUP', agreed: true },
        { type: 'THIRD_PARTY', scope: 'FINANCIAL', agreed: false },
    ];

    const payload = buildAgreeAgainPayload(myConsents, { type: 'THIRD_PARTY', scope: 'FINANCIAL' });

    assert.deepEqual(
        payload,
        [{ type: 'THIRD_PARTY', agreed: true }],
        'FINANCIAL 저장이 SIGNUP 의 마케팅 동의를 끄면 안 된다',
    );
});

test('buildAgreeAgainPayload: CHALLENGE 재동의는 공통 챌린지 동의만 켠다', () => {
    const myConsents = [
        { type: 'MARKETING', scope: 'SIGNUP', agreed: true },
        { type: 'CHALLENGE', scope: 'CHALLENGE', agreed: false },
    ];

    const payload = buildAgreeAgainPayload(myConsents, {
        type: 'CHALLENGE',
        scope: 'CHALLENGE',
    });

    assert.deepEqual(payload, [{ type: 'CHALLENGE', agreed: true }]);
});
