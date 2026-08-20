import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

function source(path) {
    return readFileSync(new URL(`../${path}`, import.meta.url), 'utf8');
}

test('챌린지 리포트 온보딩 CTA는 개인 챌린지 메인으로 이동한다', () => {
    const reportView = source('src/views/challenge/report/ChallengeReportView.vue');
    const onboarding = source('src/components/challenge/report/ChallengeReportOnboarding.vue');

    assert.match(
        reportView,
        /function startPersonalChallenge\(\) \{\s*router\.push\(\{ name: 'personalMissionChallenge' \}\);\s*\}/,
    );
    assert.ok(
        reportView.includes('@start-challenge="startPersonalChallenge"'),
        '온보딩 CTA 이벤트가 개인 챌린지 이동 함수에 연결되어야 한다',
    );
    assert.match(reportView, /entryState\.value = report\.value\.entryState \?\? null;/);
    assert.match(onboarding, /챌린지 하러가기/);
});

test('첫 리포트 준비 중 상태에는 챌린지 이동 CTA를 노출하지 않는다', () => {
    const onboarding = source('src/components/challenge/report/ChallengeReportOnboarding.vue');

    assert.match(
        onboarding,
        /const isPreparing = computed\(\(\) => props\.state === 'preparing'\);/,
    );
    assert.match(onboarding, /v-if="!isPreparing"/);
    assert.match(onboarding, /첫 재판 보고서에서 확인할 수 있어요/);
    assert.match(onboarding, /prefers-reduced-motion: reduce/);
});
