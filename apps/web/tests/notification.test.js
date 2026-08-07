import test from 'node:test';
import assert from 'node:assert/strict';
import {
    formatRelativeTime,
    groupByDay,
    notificationVisual,
    resolveDeepLink,
} from '../src/utils/notification.js';

const NOW = new Date('2026-08-06T12:00:00');

test('오늘·어제·그 이전으로 묶는다', () => {
    const items = [
        { id: 3, createdAt: '2026-08-06T11:50:00' },
        { id: 2, createdAt: '2026-08-05T09:00:00' },
        { id: 1, createdAt: '2026-08-04T09:00:00' },
    ];
    const groups = groupByDay(items, NOW);
    assert.deepEqual(
        groups.map((g) => g.label),
        ['오늘', '어제', '2026-08-04'],
    );
    assert.equal(groups[0].items.length, 1);
});

test('자정 직후는 어제가 아니라 오늘이다', () => {
    const groups = groupByDay([{ id: 1, createdAt: '2026-08-06T00:01:00' }], NOW);
    assert.equal(groups[0].label, '오늘');
});

test('상대 시간을 분·시간·어제로 나눈다', () => {
    assert.equal(formatRelativeTime('2026-08-06T11:50:00', NOW), '10분 전');
    assert.equal(formatRelativeTime('2026-08-06T11:00:00', NOW), '1시간 전');
    assert.equal(formatRelativeTime('2026-08-05T09:00:00', NOW), '어제 09:00');
    assert.equal(formatRelativeTime('2026-08-06T11:59:30', NOW), '방금');
});

test('종류마다 아이콘과 톤이 있다', () => {
    for (const type of [
        'ACCOUNT_RECONNECT',
        'GROUP_JUDGMENT',
        'GROUP_TRIAL_OPENED',
        'MISSION_DEADLINE',
        'MONTHLY_REPORT',
        'PAYMENT_DUE',
    ]) {
        const visual = notificationVisual(type);
        assert.ok(visual.icon, `${type} 아이콘 없음`);
        assert.ok(visual.tone, `${type} 톤 없음`);
    }
});

test('모르는 종류도 기본값으로 그린다', () => {
    assert.ok(notificationVisual('UNKNOWN_TYPE').icon);
});

test('딥링크는 내부 경로만 허용한다', () => {
    assert.equal(resolveDeepLink('/asset/accounts/9/reconnect'), '/asset/accounts/9/reconnect');
    assert.equal(resolveDeepLink('//evil.com'), null);
    assert.equal(resolveDeepLink('/\\evil.com'), null);
    assert.equal(resolveDeepLink('http://evil.com'), null);
    assert.equal(resolveDeepLink('javascript:alert(1)'), null);
    assert.equal(resolveDeepLink(null), null);
    assert.equal(resolveDeepLink(''), null);
});
