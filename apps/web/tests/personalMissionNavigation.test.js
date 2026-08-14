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
    assert.match(viewSource, /13_sobbing\.png/);
    assert.match(
        viewSource,
        /screenState === 'withdrawn'[\s\S]{0,180}:court-image="courtSupreme"[\s\S]{0,100}:date="courtDate"/,
    );
    assert.match(storeSource, /consentState = CHALLENGE_CONSENT_STATE\.WITHDRAWN/);
    assert.match(storeSource, /todayMission = null/);
});

test('담당 탕이 시트는 열릴 때 홈의 현재 담당 탕이와 선택값을 맞춘다', async () => {
    const sheetPath = new URL(
        '../src/components/challenge/personal/PersonalTangiSheet.vue',
        import.meta.url,
    );
    const fixturePath = new URL('../src/fixtures/personalChallenge.js', import.meta.url);
    const [sheetSource, fixtureSource] = await Promise.all([
        readFile(sheetPath, 'utf8'),
        readFile(fixturePath, 'utf8'),
    ]);

    assert.match(sheetSource, /if \(isOpen\)[\s\S]*selectedId\.value = currentProsecutorId/);
    assert.match(fixtureSource, /id: 'NORMAL'[\s\S]{0,500}recommended: true/);
    assert.doesNotMatch(sheetSource, /tangi-sheet__card--selected[\s\S]{0,100}var\(--tt-text\)/);
    assert.match(sheetSource, /\.tangi-sheet__card:focus-visible\s*\{[^}]*outline:\s*none/s);
    assert.match(sheetSource, /\.tangi-sheet__card:active\s*\{[^}]*transform:/s);
});

test('담당 검사 선택을 같은 미션 난이도로 서버에 저장한다', async () => {
    const [viewSource, storeSource] = await Promise.all([
        readFile(viewPath, 'utf8'),
        readFile(storePath, 'utf8'),
    ]);

    assert.match(viewSource, /store\.saveProsecutorDifficulty\(prosecutorId\)/);
    assert.match(storeSource, /updateMyDifficulty\(prosecutorId\)/);
    assert.match(storeSource, /selectedDifficultyId = prosecutorId/);
});

test('재판 탭 진입 시 서버 판정을 조회하고 확인 성공 후에만 모달을 닫는다', async () => {
    const apiPath = new URL('../src/api/personalMission.js', import.meta.url);
    const [viewSource, storeSource, apiSource] = await Promise.all([
        readFile(viewPath, 'utf8'),
        readFile(storePath, 'utf8'),
        readFile(apiPath, 'utf8'),
    ]);

    assert.match(viewSource, /store\.loadPendingVerdict\(\)/);
    assert.match(viewSource, /await store\.acknowledgeVerdict\(\)/);
    assert.match(viewSource, /isVerdictOpen\.value = false/);
    assert.match(storeSource, /fetchPendingMissionVerdict\(\)/);
    assert.match(
        storeSource,
        /requestMissionVerdictAcknowledge\(this\.pendingVerdict\.assignmentId\)/,
    );
    assert.doesNotMatch(storeSource, /pendingVerdict:\s*this\.pendingVerdict/);
    assert.match(apiSource, /get\('\/missions\/verdicts\/pending'\)/);
    assert.match(apiSource, /post\(`\/missions\/verdicts\/\$\{assignmentId\}\/acknowledge`\)/);
});

test('판정 근거는 카테고리와 거래 건수를 한 아코디언 카드에 표시한다', async () => {
    const modalPath = new URL(
        '../src/components/challenge/personal/PersonalVerdictModal.vue',
        import.meta.url,
    );
    const source = await readFile(modalPath, 'utf8');

    assert.match(source, /const parts = \['판정 근거 보기'\]/);
    assert.match(source, /parts\.push\(props\.verdict\.categoryName\)/);
    assert.match(source, /class="verdict__evidence-card"/);
    assert.match(source, /class="verdict__evidence-toggle"[\s\S]*class="verdict__evidence-list"/);
});
