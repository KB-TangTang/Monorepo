import test from 'node:test';
import assert from 'node:assert/strict';
import { profileInitial, providerLabel, avatarColor } from '../src/utils/my.js';

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

test('같은 이름은 항상 같은 아바타 색을 받는다 — 화면마다 색이 달라지면 같은 사람으로 안 보인다', () => {
    assert.equal(avatarColor('김지갑'), avatarColor('김지갑'));
    assert.equal(avatarColor('jaehan'), avatarColor('jaehan'));
});

test('다른 이름은 (대체로) 다른 색을 받는다', () => {
    const colors = ['김지갑', '이유현', '박준서', '최세영'].map(avatarColor);
    assert.ok(new Set(colors).size > 1);
});

test('이름이 없어도 색을 돌려준다 — 아바타가 투명해지면 카드가 깨져 보인다', () => {
    assert.match(avatarColor(''), /^#[0-9A-Fa-f]{6}$/);
    assert.match(avatarColor(null), /^#[0-9A-Fa-f]{6}$/);
});
