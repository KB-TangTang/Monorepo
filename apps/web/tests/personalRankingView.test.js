import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const viewSource = await readFile(
    new URL('../src/views/challenge/personal/PersonalRankingView.vue', import.meta.url),
    'utf8',
);

test('명예의 전당은 1~3위 전용 탕이 이미지를 사용한다', () => {
    assert.match(viewSource, /tang-ranking-first\.png/);
    assert.match(viewSource, /tang-ranking-second\.png/);
    assert.match(viewSource, /tang-ranking-third\.png/);
    assert.match(viewSource, /podiumImages\[member\.rank\]/);
});

test('4~10위와 내 순위는 공용 사용자 프로필 컴포넌트를 사용한다', () => {
    assert.match(viewSource, /topRankings\.slice\(3, 10\)/);
    assert.equal(viewSource.match(/<UserAvatar/g)?.length, 2);
    assert.match(viewSource, /ranking\.myRanking\.profileImageUrl/);
});

test('인증서 발급 버튼은 개인 인증서 페이지로 이동한다', () => {
    assert.match(viewSource, /name: 'personalCertificate'/);
    assert.match(viewSource, /query: \{ month: selectedPeriod\.value \}/);
    assert.match(viewSource, /@click="openCertificate"/);
});

test('명예의 전당에는 대법원·지방법원 전환 탭을 표시하지 않는다', () => {
    assert.doesNotMatch(viewSource, /ChallengeModeTabBar/);
});
