import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const certificateViewSource = await readFile(
    new URL('../src/views/challenge/personal/PersonalCertificateView.vue', import.meta.url),
    'utf8',
);
const certificateSource = await readFile(
    new URL(
        '../src/components/challenge/personal/ranking/PersonalHonorCertificate.vue',
        import.meta.url,
    ),
    'utf8',
);

test('인증서 페이지는 새 월간 랭킹 응답의 내 순위 구조를 사용한다', () => {
    assert.match(certificateViewSource, /myRanking\.topPercent/);
    assert.match(certificateSource, /myRanking\.rank/);
    assert.match(certificateSource, /myRanking\.topPercent/);
    assert.match(certificateSource, /myRanking\.totalScore/);
});
