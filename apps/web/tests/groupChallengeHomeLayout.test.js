import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

/*
 * 지방법원 홈 레이아웃 위계 회귀 방지 (이슈 #448).
 *
 * 이 화면은 성격이 다른 세 덩어리를 세로로 쌓는다 — 마감이 걸린 **할 일 큐**(재판 현황),
 * 내가 속한 그룹 **명부**(내 챌린지), 그리고 **진입로**(만들기·참여코드).
 * 셋이 같은 몸집의 흰 카드로 나오면 화면 전체가 평평해져 「카드가 계속 나온다」로만 읽힌다.
 *
 * 렌더링 하네스가 없어(`node:test` + 순수 JS) 소스에 규칙이 들어 있는지를 검사한다.
 */

function source(path) {
    return readFileSync(new URL(`../${path}`, import.meta.url), 'utf8');
}

const HOME = 'src/views/challenge/group/GroupChallengeHomeView.vue';
const TRIAL_CARD = 'src/components/challenge/group/GroupTrialStatusCard.vue';
const LIST_SHEET = 'src/components/challenge/group/GroupTrialListSheet.vue';
const TODO_GRID = 'src/components/challenge/group/GroupTrialTodoGrid.vue';

test('홈은 내 챌린지를 상위 몇 건까지만 그린다', () => {
    /*
     * 전부 그리면 참여 그룹이 늘수록 이 섹션이 세로로 그대로 자라, 위의 재판 현황을 화면
     * 밖으로 밀어낸다 — 지금 할 일이 지금 할 일 아닌 것에 밀리는 구조다.
     * 나머지는 이미 있는 목록 화면(`groupChallengeList`)이 맡는다.
     */
    const src = source(HOME);
    assert.match(src, /const HOME_CHALLENGE_LIMIT = \d+;/);
    assert.match(src, /homeChallenges = computed\([\s\S]*?\.slice\(0, HOME_CHALLENGE_LIMIT\)/);
    assert.match(src, /v-for="ch in homeChallenges"/);
    /* 여기가 sortedChallenges 로 되돌아가면 자르기가 통째로 무효가 된다 */
    assert.doesNotMatch(src, /v-for="ch in sortedChallenges"/);
});

test('잘려 나간 건수를 전체보기 자리가 밝힌다', () => {
    /* 안 밝히면 홈이 「내 챌린지는 3개뿐」이라고 잘못 말한다 */
    const src = source(HOME);
    assert.match(src, /hiddenChallengeCount = computed\(/);
    assert.match(src, /hiddenChallengeCount \?/);
});

test('빈 상태 판정은 자른 목록이 아니라 전체로 본다', () => {
    /*
     * `!homeChallenges.length` 로 바꿔도 지금은 결과가 같지만, LIMIT 이 0 이 되거나
     * 필터가 하나 끼는 순간 챌린지가 있는데 「아직 없어요」가 뜬다.
     */
    assert.match(source(HOME), /v-if="!sortedChallenges\.length"/);
});

test('할 일 큐만 바닥에서 떠 있다', () => {
    /*
     * 위계를 만드는 건 그림자 하나가 아니라 **차이**다. 할 일 격자에 그림자를 주면서
     * 아래 명부 행에도 같이 주면 아무것도 안 준 것과 같다.
     */
    assert.match(source(TODO_GRID), /\.todo-grid__tile \{[^}]*box-shadow: var\(--tt-elevation-2\)/);
    assert.doesNotMatch(source(HOME), /\.gc-group-row \{[^}]*box-shadow/);
});

test('명부 행은 할 일 카드보다 한 급 작다', () => {
    /*
     * 재판 현황 카드는 `--tt-radius-xl`(22px)에 아이콘이 없는 한 줄, 명부 행은 그보다
     * 작은 라운드에 아이콘을 단다. 여기가 같아지면 두 블록이 다시 같은 무게로 읽힌다.
     */
    const src = source(HOME);
    const radius = src.match(/\.gc-group-row \{[^}]*border-radius: (\d+)px/);
    assert.ok(radius, '.gc-group-row 에 border-radius 가 있어야 한다');
    assert.ok(
        Number(radius[1]) < 22,
        `명부 행 라운드(${radius[1]}px)가 카드(22px)보다 작아야 한다`,
    );
});

test('두 섹션 사이는 토큰 간격으로 벌린다', () => {
    /*
     * 10px 로는 두 덩어리가 한 스크롤 흐름으로 이어져 섹션 제목이 앞 카드에 붙어 보였다.
     * 임의 px 로 되돌아가면 다음 사람이 다시 눈대중으로 만진다.
     */
    assert.match(source(HOME), /\.gc-section \{[^}]*margin-top: var\(--tt-space-\d\)/);
});

/* ── 「내 차례」 큐 (시안 A) ────────────────────────────── */

test('재판을 「내 차례」와 「기다리는 중」으로 가른다', () => {
    /*
     * 기준은 `STANCE.actionable` 하나여야 한다. 여기서 status 를 다시 보면 그룹 상세
     * 캐러셀과 판정이 갈린다 — 같은 재판이 화면마다 다른 뱃지를 달게 된다.
     */
    const src = source(HOME);
    assert.match(
        src,
        /myTurnTrials = computed\(\(\) => trialCards\.value\.filter\(\(card\) => card\.actionable\)/,
    );
    assert.match(
        src,
        /watchingTrials = computed\(\(\) => trialCards\.value\.filter\(\(card\) => !card\.actionable\)/,
    );
});

test('내 차례를 다시 변론·투표로 가른다', () => {
    /*
     * 격자의 두 칸이 이 둘이다. 기준은 `STANCE.action` 이어야 한다 — 여기서 status 를
     * 다시 보면 CTA 가 여는 화면과 타일이 여는 목록이 어긋난다.
     */
    const src = source(HOME);
    assert.match(src, /defendTrials = computed\([\s\S]*?card\.action === 'defend'/);
    assert.match(src, /voteTrials = computed\([\s\S]*?card\.action === 'vote'/);
});

test('홈은 내 차례를 목록이 아니라 격자 두 칸으로 접는다', () => {
    /*
     * 목록을 그대로 쌓으면 그룹이 늘수록 홈이 세로로 자란다 — #448 이 없애려던 문제다.
     * 격자는 건수와 무관하게 높이가 같다. 여기가 `GroupTrialStatusCard` 로 되돌아가면
     * 「행 개수만큼 홈이 길어진다」가 그대로 살아난다.
     */
    const src = source(HOME);
    assert.match(src, /<GroupTrialTodoGrid\s+v-if="myTurnTrials\.length"/);
    assert.match(src, /:defend-count="defendTrials\.length"/);
    assert.match(src, /:vote-count="voteTrials\.length"/);
    assert.doesNotMatch(src, /<GroupTrialStatusCard/);
});

test('0건인 칸도 지우지 않고 흐리게 남긴다', () => {
    /*
     * 격자의 값은 「항상 같은 모양」이다. 한 칸이 사라지면 남은 칸이 전체 폭으로 늘어나
     * 매번 다른 화면이 된다 — 목록을 접은 이유가 통째로 무효가 된다.
     */
    const src = source(TODO_GRID);
    assert.match(src, /todo-grid__tile--empty/);
    assert.match(src, /:disabled="!tile\.count"/);
    assert.doesNotMatch(src, /v-if="tile\.count"/);
});

test('두 칸은 건수와 무관하게 정사각형이다', () => {
    /*
     * `aspect-ratio` 는 **셀**이 갖는다. 버튼에 걸면 안쪽 내용이 넘칠 때 비율이 아니라
     * 내용이 이겨서 두 칸 높이가 어긋난다.
     */
    assert.match(source(TODO_GRID), /\.todo-grid__cell \{[^}]*aspect-ratio: 1/);
});

test('두 칸은 숫자만 다른 같은 그림이 아니다', () => {
    /*
     * 톤이 38px 아이콘 상자 안에만 있으면 색이 닿는 면적이 정사각형의 4% 뿐이라 두 칸이
     * 같아 보인다. 가장 큰 글자(건수)와 워터마크로 색과 실루엣을 둘 다 가른다.
     */
    const src = source(TODO_GRID);
    for (const tone of ['danger', 'info']) {
        assert.match(
            src,
            new RegExp(`\\.todo-grid__tile--\\w+ \\.todo-grid__count \\{[^}]*--tt-${tone}-deep`),
        );
    }
    /* 워터마크는 글자를 늘리지 않고 면을 채우는 수단이다 — 늘리면 말줄임이 되살아난다 */
    assert.match(src, /class="todo-grid__watermark"/);
    assert.match(src, /\.todo-grid__tile \{[^}]*overflow: hidden/);
});

test('오브젝트 그림을 톤 틴트 상자에 담지 않는다', () => {
    /*
     * obj_*.png 는 자기 색(판사봉 노랑·남색)과 자기 그림자를 갖고 있다. 빨강·파랑 틴트 위에
     * 얹으면 색이 부딪히고 그림자 타원이 얼룩이 된다. 상자를 되살리면 같은 문제가 돌아온다.
     */
    const src = source(TODO_GRID);
    assert.doesNotMatch(src, /todo-grid__icon/);
    assert.doesNotMatch(src, /\.todo-grid__tile--\w+ \.todo-grid__art \{[^}]*background:/);
});

test('0건인 칸은 그림까지 물러난다', () => {
    /*
     * 글자는 `--tt-text-hint` 로 내리면 되지만 그림은 래스터라 색을 못 바꾼다.
     * 컬러 판사봉만 그대로 남으면 「못 누르는데 제일 눈에 띄는 칸」이 된다.
     */
    assert.match(
        source(TODO_GRID),
        /\.todo-grid__tile--empty \.todo-grid__art \{[^}]*filter: grayscale\(1\)/,
    );
});

test('격자는 원시 팔레트가 아니라 의미 토큰을 참조한다', () => {
    /* apps/web/AGENTS.md — 「컴포넌트는 의미 토큰만 참조한다」. 원시 토큰은 tokens.css 안에서만 */
    assert.doesNotMatch(source(TODO_GRID), /--tt-(red|blue|brand|guilty|innocent|gray)-/);
});

test('타일은 눌리는 것으로 보인다', () => {
    /* 격자가 목록을 여는 「버튼」이라는 걸 손끝으로 알린다. 0건 칸은 눌러도 반응하지 않는다 */
    const src = source(TODO_GRID);
    assert.match(src, /\.todo-grid__tile:active \{[^}]*transform: scale\(/);
    assert.match(src, /\.todo-grid__tile:disabled:active \{[^}]*transform: none/);
    assert.match(src, /@media \(prefers-reduced-motion: reduce\)/);
});

test('기다리는 재판은 지우지 않고 한 줄로 접는다', () => {
    /*
     * 할 일은 아니어도 「내 재판이 심판받는 중」은 이 화면에서 가장 궁금한 것이다.
     * 큐에서 뺀다고 화면에서까지 사라지면 상세 화면까지 들어가야만 알 수 있다.
     * 격자에 세 번째 칸으로 넣지도 않는다 — 기다리는 것은 할 일이 아니다.
     */
    const src = source(HOME);
    assert.match(src, /v-if="watchingTrials\.length"[\s\S]*?openSheet = 'watching'/);
    assert.match(src, /기다리는 재판 \{\{ watchingTrials\.length \}\}건/);
});

test('접힌 줄은 격자와 같은 낱말을 쓰지 않는다', () => {
    /*
     * 격자 두 칸은 「변론」·「투표」라는 **내 동사**다. 이 줄이 「지켜보는」이면 그것도 내 행동이라
     * 「할 일 아님」이 낱말에서 안 갈린다. 「판결 대기중」도 안 된다 — 넓게 읽으면 격자의
     * 두 칸도 전부 판결 대기중이고, 좁게 읽으면 변론 전인 재판에 거짓말이 된다.
     */
    /*
     * 주석은 「판결 대기중」을 **왜 안 썼는지** 적어 두려고 그 말을 인용한다.
     * 그래서 파일 전체가 아니라 화면에 실제로 나가는 두 자리(요약 줄 라벨 · 시트 제목)만 본다.
     */
    const src = source(HOME);
    const label = src.match(/<span class="gc-watching__label">[\s\S]*?<\/span>/)[0];
    const sheetTitle = src.match(/SHEET_TITLE = \{[^}]*\}/)[0];
    for (const text of [label, sheetTitle]) {
        assert.doesNotMatch(text, /지켜보는/);
        assert.doesNotMatch(text, /판결 대기/);
    }
});

test('접힌 줄은 면은 갖되 격자처럼 뜨지는 않는다', () => {
    /*
     * `background: none` 이던 시절엔 위아래 흰 덩어리 사이의 빈틈으로 보여 누를 수 있다는
     * 신호가 `›` 하나뿐이었다. 그렇다고 그림자를 얹으면 격자의 세 번째 칸이 되어
     * 「할 일이 하나 더 있다」로 읽힌다 — 접어 둔 이유가 사라진다.
     * 페이지 배경이 `--tt-bg-page` 라 그림자 없이도 흰 면이 선다.
     */
    const src = source(HOME);
    const rule = src.match(/\.gc-watching \{[^}]*\}/)[0];
    assert.match(rule, /background: var\(--tt-bg\)/);
    assert.doesNotMatch(rule, /box-shadow/);
    assert.match(src, /\.gc-page \{[^}]*background: var\(--tt-bg-page\)/);
});

test('접은 자리가 마감을 통째로 감추지 않는다', () => {
    /*
     * 접으면 매초 도는 타이머가 같이 사라진다. 6시간 안쪽(useCountdown 의 `urgent`)이
     * 하나라도 있으면 그것만 밖으로 알려야 열어 볼 이유가 생긴다.
     * 격자 두 칸과 기다리기 줄 **셋 다** 접힌 자리라 셋 다 알려야 한다.
     */
    const src = source(HOME);
    assert.match(
        src,
        /function anyUrgent\(list\) \{[\s\S]*?countdowns\.value\[card\.id\]\?\.urgent/,
    );
    for (const name of ['defendUrgent', 'voteUrgent', 'watchingUrgent']) {
        assert.match(src, new RegExp(`${name} = computed\\(\\(\\) => anyUrgent\\(`), name);
    }
    assert.match(src, /v-if="watchingUrgent"/);
    assert.match(source(TODO_GRID), /v-if="tile\.urgent"/);
});

test('재판은 도는데 내가 할 게 없는 상태를 「평온」이라고 하지 않는다', () => {
    /*
     * `GroupPeacefulCard` 는 「오늘까지 모두 기준 안에서 소비했어요」라고 말한다.
     * 남의 재판이 돌고 있는데 이게 뜨면 화면이 거짓말을 한다.
     */
    const src = source(HOME);
    assert.match(src, /v-else-if="watchingTrials\.length" class="gc-trial-idle"/);
    /* 평온 분기는 그 뒤에 와야 한다 — 순서가 뒤집히면 위 분기가 영영 안 걸린다 */
    const idleAt = src.indexOf('gc-trial-idle');
    const peacefulAt = src.indexOf('<GroupPeacefulCard');
    assert.ok(idleAt < peacefulAt, '「할 일 없음」 분기가 평온 분기보다 앞에 와야 한다');
});

test('세 목록이 시트 하나를 돌려 쓴다', () => {
    /*
     * 변론·투표·기다리기는 제목과 담긴 항목만 다르고 줄 모양·정렬·CTA 가 전부 같다.
     * 시트를 셋으로 나누면 그 셋이 조금씩 어긋난다.
     * 같은 재판을 두 자리에서 다르게 그리지 않도록 줄은 `GroupTrialStatusCard` 가 그린다.
     */
    const sheet = source(LIST_SHEET);
    assert.match(sheet, /import GroupTrialStatusCard from/);
    assert.match(
        sheet,
        /import BaseBottomSheet from '@\/components\/common\/BaseBottomSheet\.vue'/,
    );

    const src = source(HOME);
    assert.match(src, /const openSheet = ref\(null\)/);
    assert.match(src, /SHEET_TITLE = \{[^}]*defend: '변론할 재판'[^}]*watching: '기다리는 재판'/);
    assert.match(src, /<GroupTrialListSheet[\s\S]{0,300}?:items="sheetItems"/);
});

test('시트 머리줄은 시트만 그린다', () => {
    /*
     * 카드에 머리줄을 되살리면 「변론할 재판 2건」이 시트 헤더와 카드 안에 두 번 뜬다.
     */
    assert.match(source(LIST_SHEET), /\{\{ title \}\} \{\{ items\.length \}\}건/);
    assert.doesNotMatch(source(TRIAL_CARD), /trial-status__head/);
});

/* ── 펼친 본문 ─────────────────────────────────────── */

test('펼친 본문은 상자를 갖지 않는다', () => {
    /*
     * 한때 아이보리 서류(`--tt-doc-bg` + `--tt-doc-rule` 테두리)를 깔았다가 걷었다(#448 8차).
     * `--tt-doc-bg`(#fdfbf3)와 카드 바탕(#ffffff)의 차이가 B 12/255 뿐이라 종이는 안 보이고
     * 베이지 테두리만 보였고, 바텀시트 → 흰 카드 → 이 상자로 **표면이 3겹**이 됐다.
     *
     * 되살리고 싶으면 종이를 다시 칠하는 게 아니라 면적과 신호(괘선·명조 소제목·인장)를
     * 같이 실어야 한다. 그건 행 높이를 늘리는 일이라 목록 6행에서는 다른 결정이다.
     */
    const code = source(TRIAL_CARD);
    const inner = code.match(/\.trial-status__panel-inner \{[^}]*\}/)[0];
    assert.doesNotMatch(inner, /background/);
    assert.doesNotMatch(inner, /border/);
    /* 종이 토큰이 이 파일에서 완전히 빠졌는지 — 스테퍼·투표 바에도 남아 있으면 안 된다 */
    assert.doesNotMatch(code, /var\(--tt-doc-/);
});

test('서류 어휘는 스테퍼의 명조 한 줄만 남긴다', () => {
    /*
     * 상자는 걷었지만 「기소 ─ 변론 ─ 투표 ─ 판결」은 재판 절차 그 자체라 명조를 유지한다.
     * `--tt-font-serif` 는 `GroupSummonCard`(초대코드 소환장) 때문에 이미 있는 토큰이다 —
     * 여기서 HEX 를 새로 박거나 토큰을 신설하면 같은 「법정 서류」가 두 벌이 된다.
     */
    const code = source(TRIAL_CARD);
    assert.match(code, /\.trial-status__step-name \{[^}]*font-family: var\(--tt-font-serif\)/);
    /* 상자가 없으니 미완료 색은 종이 괘선이 아니라 진행바 트랙(회청)으로 돌아온다 */
    assert.match(code, /\.trial-status__step-dot \{[^}]*background: var\(--tt-border-track\)/);
    assert.match(code, /\.trial-status__dot \{[^}]*background: var\(--tt-border-track\)/);
});

test('투표 현황은 막대가 아니라 점이다', () => {
    /*
     * `groupInvite.js:52` 가 `maxMembers ?? 6` 이라 그룹은 최대 6명, 피고를 빼면 배심원은 5명이다.
     * 막대는 연속량 인코딩이라 표현할 상태가 6개뿐인데 폭을 꽉 채워서 「3명이 던졌다」가 아니라
     * 「60% 로딩됨」으로 읽혔다. 점은 이산량이라 인코딩이 맞고 계기판이 한 줄로 접힌다.
     */
    const code = source(TRIAL_CARD);
    assert.doesNotMatch(code, /vote-track|vote-fill|votePercent/);
    assert.match(code, /\.trial-status__dot--mine-todo/);
    /* 점은 aria-hidden 이라, 읽어 줄 숫자가 같이 없으면 스크린리더에 아무것도 안 간다 */
    assert.match(code, /aria-hidden="true"/);
    assert.match(code, /\{\{ item\.voteCount \}\} \/ \{\{ item\.totalVoters \}\}명 투표/);
});

test('남은 시간은 CTA 쪽에 붙는다', () => {
    /*
     * 한때 패널 맨 위 왼쪽의 회색 캡션이었다 — 이 패널에서 **유일하게 급한 값인데 가장 약하게**
     * 그려져 있었다. 「지금 눌러야 하나」를 판단하는 두 값(투표 수 · 남은 시간)을 버튼 바로
     * 위에 모은다. 스테퍼 → 계기판 → CTA 순서가 뒤집히면 그 판단이 다시 흩어진다.
     */
    const code = source(TRIAL_CARD);
    const steps = code.indexOf('trial-status__steps');
    const meter = code.indexOf('trial-status__meter');
    const cta = code.indexOf('trial-status__cta');
    assert.ok(steps < meter && meter < cta, '스테퍼 → 계기판 → CTA 순서여야 한다');

    /* 변론 단계라 투표 현황이 없어도 시계 자리가 오른쪽 그대로여야 한다 — 행마다 움직이면 안 된다 */
    assert.match(code, /\.trial-status__countdown \{[^}]*margin-left: auto/);
    /* 투표 현황은 `v-if`, 남은 시간은 항상 나온다 */
    assert.match(code, /v-if="item\.showVote" class="trial-status__vote"/);
});

test('패널 껍데기는 여전히 패딩을 갖지 않는다', () => {
    /*
     * 서류 상자를 만들면서 여백을 `__panel` 로 올리고 싶어지는데, 그러면 height:0 이 돼도
     * 패딩만큼 남아 닫힌 행이 부풀고 여닫기 애니메이션이 그 높이에서 시작·종료한다.
     * 여백은 안쪽의 margin 이 갖는다(부모가 overflow:hidden 이라 scrollHeight 에 잡힌다).
     */
    assert.doesNotMatch(source(TRIAL_CARD), /\.trial-status__panel \{[^}]*padding/);
});

test('시트 안에서 이동할 때 history 를 먼저 양도한다', () => {
    /*
     * 안 하면 시트가 닫히면서 history.back() 이 라우터 이동을 되감는다
     * (`common/useOverlay.js` 주석 참고).
     */
    assert.match(
        source(LIST_SHEET),
        /releaseHistory: \(\) => sheetRef\.value\?\.releaseHistory\?\.\(\)/,
    );
    assert.match(
        source(HOME),
        /if \(openSheet\.value\) \{[\s\S]*?sheetRef\.value\?\.releaseHistory\?\.\(\)/,
    );
});
