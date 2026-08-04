import test from 'node:test';
import assert from 'node:assert/strict';
import {
    buildAgreementState,
    buildAgreementStateFromConsents,
    canSubmit,
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
