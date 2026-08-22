import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const viewSource = await readFile(
    new URL('../src/views/challenge/personal/PersonalRankingView.vue', import.meta.url),
    'utf8',
);
const monthPickerSource = await readFile(
    new URL(
        '../src/components/challenge/personal/ranking/PersonalRankingMonthPicker.vue',
        import.meta.url,
    ),
    'utf8',
);
const viewStyle = await readFile(
    new URL('../src/views/challenge/personal/PersonalRankingView.css', import.meta.url),
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

test('시상대는 1위, 2위, 3위 순으로 높고 탕이는 같은 크기를 사용한다', () => {
    assert.match(viewStyle, /podium-item--1\s*\{[^}]*min-height: 270px;/s);
    assert.match(viewStyle, /podium-item--2\s*\{[^}]*min-height: 250px;/s);
    assert.match(viewStyle, /podium-item--3\s*\{[^}]*min-height: 210px;/s);
    assert.doesNotMatch(viewStyle, /podium-item--1 \.personal-ranking__podium-avatar/);
    assert.match(
        viewStyle,
        /\.personal-ranking__podium-item\s*\{[^}]*justify-content: flex-end;/s,
    );
});

test('iPhone 14 Pro 너비에서는 내 닉네임 공간을 확보한다', () => {
    assert.match(viewStyle, /@media \(max-width: 430px\)/);
    assert.match(
        viewStyle,
        /grid-template-columns: 76px 48px minmax\(0, 1fr\) auto auto;/,
    );
    assert.doesNotMatch(
        viewStyle,
        /@media \(max-width: 430px\)[\s\S]*personal-ranking__mine-percentile\s*\{[^}]*display: none;/,
    );
});

test('랭킹 닉네임은 좁은 화면에서 줄바꿈하지 않고 말줄임 처리한다', () => {
    assert.match(
        viewStyle,
        /\.personal-ranking__podium-name\s*\{[^}]*text-overflow: ellipsis;[^}]*white-space: nowrap;/s,
    );
    assert.match(
        viewStyle,
        /\.personal-ranking__list li > span\s*\{[^}]*text-overflow: ellipsis;[^}]*white-space: nowrap;/s,
    );
    assert.match(
        viewStyle,
        /\.personal-ranking__mine > b\s*\{[^}]*font-size: var\(--tt-fs-label\);[^}]*text-overflow: ellipsis;[^}]*white-space: nowrap;/s,
    );
});

test('개발 환경에서도 fixture 대신 월간 랭킹 API를 호출한다', () => {
    assert.match(viewSource, /fetchMissionRankings\(selectedPeriod\.value\)/);
    assert.doesNotMatch(viewSource, /import\.meta\.env\.DEV/);
    assert.doesNotMatch(viewSource, /MOCK_PERSONAL_RANKINGS/);
});

test('성적표에 처음 진입하면 현재 달을 기본으로 조회한다', () => {
    assert.match(viewSource, /now\.getFullYear\(\)/);
    assert.match(viewSource, /now\.getMonth\(\) \+ 1/);
    assert.match(viewSource, /: currentPeriod/);
});

test('API의 랭킹 보유 월 목록으로 선택 가능한 월과 연도를 구성한다', () => {
    assert.match(viewSource, /fetchMissionRankingMonths\(\)/);
    assert.match(viewSource, /createRankingMonths\(years, availablePeriodsSet\)/);
    assert.match(viewSource, /:months="rankingMonths"/);
    assert.doesNotMatch(viewSource, /available: Boolean\(ranking\.value\)/);
});

test('월 선택창에서 데이터 유무와 관계없이 이전·다음 연도로 이동한다', () => {
    assert.match(monthPickerSource, /selectedYear\.value - 1/);
    assert.match(monthPickerSource, /selectedYear\.value \+ 1/);
    assert.doesNotMatch(monthPickerSource, /:disabled="!previousYear"/);
    assert.doesNotMatch(monthPickerSource, /:disabled="!nextYear"/);
});
