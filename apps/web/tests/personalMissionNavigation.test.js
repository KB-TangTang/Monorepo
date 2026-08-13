import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const viewPath = new URL(
    '../src/views/challenge/personal/PersonalMissionHomeView.vue',
    import.meta.url,
);
const storePath = new URL('../src/stores/personalMission.js', import.meta.url);

test('개인 미션 동의를 미루면 홈으로 이동한다', async () => {
    const source = await readFile(viewPath, 'utf8');

    assert.match(source, /@later="handleConsentLater"/);
    assert.match(source, /router\.push\(\{ name: 'home' \}\)/);
});

test('개발 모드에서 철회 후 오늘 미션이 없는 화면을 재현할 수 있다', async () => {
    const [viewSource, storeSource] = await Promise.all([
        readFile(viewPath, 'utf8'),
        readFile(storePath, 'utf8'),
    ]);

    assert.match(viewSource, /철회·미션 없음 화면/);
    assert.match(storeSource, /consentState = CHALLENGE_CONSENT_STATE\.WITHDRAWN/);
    assert.match(storeSource, /todayMission = null/);
});
