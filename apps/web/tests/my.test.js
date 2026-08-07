import test from 'node:test';
import assert from 'node:assert/strict';
import { profileInitial, providerLabel } from '../src/utils/my.js';

test('닉네임 첫 글자를 아바타에 쓴다', () => {
    assert.equal(profileInitial('김지갑'), '김');
    assert.equal(profileInitial('jaehan'), 'J');
});

test('닉네임이 없으면 물음표로 대신한다 — 아바타가 비면 카드가 깨져 보인다', () => {
    assert.equal(profileInitial(''), '?');
    assert.equal(profileInitial(null), '?');
    assert.equal(profileInitial('   '), '?');
});

test('소셜 제공자를 소문자 표기로 바꾼다', () => {
    assert.equal(providerLabel('GOOGLE'), 'google');
});

test('모르는 제공자는 그대로 소문자로 보여준다', () => {
    assert.equal(providerLabel('KAKAO'), 'kakao');
    assert.equal(providerLabel(null), '');
});
