import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

function source(path) {
    return readFileSync(new URL(`../${path}`, import.meta.url), 'utf8');
}

test('그룹 전적 이력은 종료된 그룹 챌린지 목록을 연다', () => {
    const reportView = source('src/views/challenge/report/ChallengeReportView.vue');
    const listView = source('src/views/challenge/group/GroupChallengeListView.vue');

    assert.match(
        reportView,
        /function openGroupHistory\(\) \{\s*router\.push\(\{ name: 'groupChallengeList', query: \{ tab: 'ended' \} \}\);\s*\}/,
        '그룹 전적 이력은 종료된 목록 탭으로 이동해야 한다',
    );
    assert.ok(
        reportView.includes('@open-group-history="openGroupHistory"'),
        '그룹 전적 카드의 이력 클릭 이벤트가 화면 이동 함수에 연결되어야 한다',
    );
    assert.match(
        listView,
        /ended:\s*\['JUDGING', 'CLOSED'\]/,
        '종료됨 탭은 판결 중 및 확정 완료 그룹 챌린지를 표시해야 한다',
    );
});
