import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const rowSource = readFileSync(
    new URL('../src/components/account/ConnectedAccountRow.vue', import.meta.url),
    'utf8',
);

test('대출을 포함한 모든 연결 자산 행은 더보기 버튼을 표시한다', () => {
    assert.match(rowSource, /class="connected-row__more"/);
    assert.doesNotMatch(rowSource, /v-if="account\.manageable !== false"/);
});
