import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { registerHooks } from 'node:module';

/*
 * 지방법원 홈 「재판 현황」 (이슈 #432).
 *
 * 「처리할 재판」은 내가 지금 행동할 수 있는 2가지만 보여줬다. 내가 심판받는 중인 재판처럼
 * **할 일은 없지만 가장 궁금한** 것이 아예 없었다. 이제 내 입장 6가지를 전부 보여준다.
 *
 * 판정이 한 곳(`utils/groupTrial.js`)에만 있는지를 함께 지킨다 — 화면마다 조건을 다시 쓰면
 * 하나씩 빠진다. 실제로 그룹 상세 캐러셀이 그렇게 두 군데 틀려 있었다.
 *
 * 렌더링 하네스가 없어(`node:test` + 순수 JS) 판정 함수는 실제로 호출하고,
 * 화면 쪽은 소스에 규칙이 들어 있는지를 검사한다.
 */

const HTTP_STUB = new URL('./stubs/httpStub.js', import.meta.url).href;
/* devDataSource 는 import.meta.env.DEV 를 읽는다 — Vite 밖에서는 import 만 해도 터진다 */
const DEV_STUB = new URL('./stubs/devDataSourceStub.js', import.meta.url).href;

registerHooks({
    resolve(specifier, context, nextResolve) {
        if (specifier === '@/api/http') {
            return { url: HTTP_STUB, shortCircuit: true };
        }
        if (specifier === '@/services/devDataSource') {
            return { url: DEV_STUB, shortCircuit: true };
        }
        return nextResolve(specifier, context);
    },
});

const { isMockMode } = await import(DEV_STUB);
const { trialStance, toTrialStatusCard, TRIAL_STEPS } = await import('../src/utils/groupTrial.js');
const { fetchAllMyTrials, fetchMyGroupChallenges } = await import('../src/api/groupChallenge.js');

function source(path) {
    return readFileSync(new URL(`../${path}`, import.meta.url), 'utf8');
}

const HOME = 'src/views/challenge/group/GroupChallengeHomeView.vue';
const CARD = 'src/components/challenge/group/GroupTrialStatusCard.vue';
const CAROUSEL = 'src/components/challenge/group/GroupDetailTrialCarousel.vue';
const TODO_GRID = 'src/components/challenge/group/GroupTrialTodoGrid.vue';
const ICON = 'src/components/challenge/group/TrialActionIcon.vue';

/** `toIndictmentViewModel` 을 지난 뒤의 모양. 화면 이름(`isMine`·`hasDefended`)을 쓴다. */
function trial(overrides = {}) {
    return {
        id: 42,
        groupId: 3,
        groupName: '배달 소비 줄이기',
        nickname: '지판',
        status: 'DEFENSE_WAIT',
        isMine: false,
        hasDefended: false,
        myVote: null,
        voteCount: 0,
        totalVoters: 5,
        exceededAmount: 6800,
        ...overrides,
    };
}

/* ══════════════════════════════════════════════════
 * 내 입장 6가지
 * ══════════════════════════════════════════════════ */

test('내 입장 6가지를 모두 구분한다', () => {
    const cases = [
        [{ isMine: true, status: 'DEFENSE_WAIT', hasDefended: false }, 'DEFENSE_NEEDED'],
        [{ isMine: true, status: 'DEFENSE_WAIT', hasDefended: true }, 'DEFENSE_SUBMITTED'],
        [{ isMine: true, status: 'VOTING' }, 'ON_TRIAL'],
        [{ isMine: false, status: 'DEFENSE_WAIT' }, 'DEFENSE_WAITING'],
        [{ isMine: false, status: 'VOTING', myVote: null }, 'VOTE_NEEDED'],
        [{ isMine: false, status: 'VOTING', myVote: 'GUILTY' }, 'VOTE_DONE'],
    ];

    for (const [overrides, expected] of cases) {
        assert.equal(trialStance(trial(overrides)), expected);
    }

    /* 6가지가 전부 서로 다른 값이어야 한다. 둘이 겹치면 카드가 같은 얼굴로 그려진다 */
    assert.equal(new Set(cases.map(([, stance]) => stance)).size, 6);
});

test('할 일이 있는 입장은 변론 필요·투표 필요 둘뿐이다', () => {
    /*
     * 서버 정렬(`GroupTrialService#actionable`)이 같은 기준으로 앞에 세운다.
     * 여기가 늘어나면 「할 일」 뱃지가 붙은 카드가 목록 아래로 밀린다.
     */
    const actionable = [
        [{ isMine: true, status: 'DEFENSE_WAIT', hasDefended: false }, true],
        [{ isMine: true, status: 'DEFENSE_WAIT', hasDefended: true }, false],
        [{ isMine: true, status: 'VOTING' }, false],
        [{ isMine: false, status: 'DEFENSE_WAIT' }, false],
        [{ isMine: false, status: 'VOTING', myVote: null }, true],
        [{ isMine: false, status: 'VOTING', myVote: 'GUILTY' }, false],
    ];

    for (const [overrides, expected] of actionable) {
        assert.equal(toTrialStatusCard(trial(overrides)).actionable, expected);
    }
});

/* ══════════════════════════════════════════════════
 * 함께 고친 판정 버그 2건 (GroupDetailTrialCarousel)
 * ══════════════════════════════════════════════════ */

test('변론을 낸 뒤에는 「변론 작성하기」를 띄우지 않는다', () => {
    /*
     * 캐러셀이 `hasDefended` 를 안 봐서 변론 제출 뒤에도 「내 변론이 필요해요」가 떴다.
     * 눌러 들어가면 서버가 이미 변론이 있다며 거절한다.
     */
    const card = toTrialStatusCard(
        trial({ isMine: true, status: 'DEFENSE_WAIT', hasDefended: true }),
    );

    assert.equal(card.stance, 'DEFENSE_SUBMITTED');
    assert.notEqual(card.action, 'defend');
    assert.equal(card.actionable, false);
});

test('남이 변론 중인 재판에 투표 버튼을 그리지 않는다', () => {
    /*
     * `!isMine && !myVote` 만 보던 시절에는 변론 대기 중(= 투표가 아직 열리지도 않은)
     * 남의 재판까지 「투표 필요」로 잡혔다. 투표 화면은 서버가 `VOTE_NOT_ALLOWED` 로 튕긴다.
     */
    const card = toTrialStatusCard(trial({ isMine: false, status: 'DEFENSE_WAIT', myVote: null }));

    assert.equal(card.stance, 'DEFENSE_WAITING');
    assert.notEqual(card.action, 'vote');
    assert.equal(card.showVote, false);
});

test('캐러셀은 판정을 다시 쓰지 않고 공유 함수를 부른다', () => {
    const code = source(CAROUSEL);

    assert.match(code, /import \{ trialStance \} from '@\/utils\/groupTrial'/);
    assert.match(code, /return CARD_TYPE\[trialStance\(item\)\]/);
    /* 조건을 직접 나열하던 옛 판정이 되살아나면 같은 버그가 다시 난다 */
    assert.doesNotMatch(code, /!item\.isMine && !item\.myVote/);
});

/* ══════════════════════════════════════════════════
 * 카드가 쓰는 파생값
 * ══════════════════════════════════════════════════ */

test('투표 현황은 변론이 끝난 뒤에만 그린다', () => {
    assert.equal(toTrialStatusCard(trial({ status: 'DEFENSE_WAIT' })).showVote, false);
    assert.equal(toTrialStatusCard(trial({ status: 'VOTING' })).showVote, true);
});

test('스테퍼의 현재 칸은 상태를 따라간다', () => {
    assert.deepEqual(TRIAL_STEPS, ['기소', '변론', '투표', '판결']);
    assert.equal(toTrialStatusCard(trial({ status: 'DEFENSE_WAIT' })).stepIndex, 1);
    assert.equal(toTrialStatusCard(trial({ status: 'VOTING' })).stepIndex, 2);
});

test('배심원이 없어도 투표 진행바가 사라지지 않는다', () => {
    /* 0 으로 나누면 NaN 이 width 로 들어가 바가 통째로 없어진다 */
    const card = toTrialStatusCard(trial({ status: 'VOTING', voteCount: 0, totalVoters: 0 }));
    assert.equal(card.votePercent, 0);

    assert.equal(
        toTrialStatusCard(trial({ status: 'VOTING', voteCount: 3, totalVoters: 5 })).votePercent,
        60,
    );
});

test('피고 닉네임은 제목이 아니라 둘째 줄에 들어간다', () => {
    /*
     * 예전에는 제목이 「{닉}님 재판에 투표해주세요」였다. 한국어는 술어가 뒤에 오는데
     * 말줄임은 뒤를 자른다 — 360px 에서 제목에 남는 폭이 152px 이라 11자에서 잘렸고,
     * 「…투표해주세요」와 「…투표했어요」가 화면에서 같은 줄이 됐다.
     * 닉네임은 잘려도 되는 자리(`subject`)로 내리고, 제목은 구분점만 갖는다.
     */
    assert.match(toTrialStatusCard(trial({ status: 'VOTING' })).subject, /지판/);
    assert.doesNotMatch(toTrialStatusCard(trial({ status: 'VOTING' })).title, /지판/);

    /* 내 재판에 내 닉네임을 넣으면 「지판님 재판」이 나에게 뜬다 */
    assert.equal(
        toTrialStatusCard(trial({ isMine: true, status: 'VOTING', nickname: '지판' })).subject,
        '내 재판',
    );
});

/* ══════════════════════════════════════════════════
 * 목데이터 · 화면 연결
 * ══════════════════════════════════════════════════ */

test('목데이터가 6가지 입장을 전부 담는다', async () => {
    /* 하나라도 빠지면 그 상태의 카드를 목모드에서 확인할 방법이 없다 */
    isMockMode.value = true;
    try {
        const list = await fetchAllMyTrials();
        const stances = new Set(list.map(trialStance));

        assert.equal(stances.size, 6, [...stances].join(', '));
        for (const item of list) {
            /* 마감은 절대시각이어야 카운트다운이 돈다. 상대값이면 화면을 열어 둔 채 굳는다 */
            assert.ok(!Number.isNaN(new Date(item.deadline).getTime()));
        }
    } finally {
        isMockMode.value = false;
    }
});

test('목재판의 groupId 가 모두 목 그룹 목록에 있다', async () => {
    /*
     * 홈이 `myChallenges` 에서 `groupId` 로 찾아 그룹 정보를 붙인다. 없는 그룹을 가리키면
     * 조인이 조용히 실패한다 — 에러도 경고도 안 뜬다. 실제로 처음 목데이터가 없는 그룹(3번)을
     * 가리키고 있었다. 재판과 그룹이 서로 다른 이름을 보여주는 것도 여기서 잡는다.
     *
     * ⚠ #443 에서 카드가 카테고리·한도를 쓰지 않게 됐다(조인 자체는 남아 있다 — `state/BACKLOG.md`).
     */
    isMockMode.value = true;
    try {
        const [trials, groups] = await Promise.all([
            fetchAllMyTrials(),
            fetchMyGroupChallenges(['ACTIVE', 'RECRUITING']),
        ]);
        const byId = new Map(groups.map((group) => [group.id, group]));

        for (const item of trials) {
            const group = byId.get(item.groupId);
            assert.ok(group, `목 그룹에 없는 groupId: ${item.groupId}`);
            /* 이름까지 어긋나면 카드와 그룹 목록이 서로 다른 이름을 보여준다 */
            assert.equal(item.groupName, group.groupName);
            assert.ok(group.categoryName);
            assert.ok(group.limitAmount > 0);
        }
    } finally {
        isMockMode.value = false;
    }
});

test('홈은 새 엔드포인트를 부르고 그룹 정보를 화면에서 이어 붙인다', () => {
    const code = source(HOME);

    assert.match(code, /fetchAllMyTrials/);
    /* 카테고리·한도를 서버가 재판마다 또 내려보내면 같은 값이 여섯 번 실려 온다 */
    assert.match(code, /myChallenges\.value\.find\(\(ch\) => ch\.id === item\.groupId\)/);
    assert.match(code, /categoryName: group\?\.categoryName/);
});

test('시안에 있는 사건번호와 마스코트는 카드에 넣지 않는다', () => {
    const code = source(CARD);

    /* 사건번호는 DB 에 대응하는 컬럼이 없다. 화면에서 지어내면 매번 다른 번호가 된다 */
    assert.doesNotMatch(code, /사건번호|No\.G-/);
    /* 카드가 커지면서 마스코트는 뺐다 (옛 GroupTodoCard 의 todo-mascot) */
    assert.doesNotMatch(code, /mascot/i);
});

test('아코디언은 한 번에 하나만 펼친다', () => {
    const code = source(CARD);

    /*
     * `undefined` = 아직 아무것도 안 누름(맨 위를 펼쳐 둔다), `null` = 사용자가 직접 닫음.
     * 둘을 한 값으로 합치면 첫 항목을 닫는 순간 다시 열린다.
     */
    assert.match(code, /const openId = ref\(undefined\)/);
    assert.match(code, /if \(openId\.value === undefined\) return index === 0/);
    assert.match(code, /openId\.value = isOpen\(item, index\) \? null : item\.id/);
});

/* ══════════════════════════════════════════════════
 * 카드가 같은 말을 두 번 하지 않는다 (이슈 #443)
 * ══════════════════════════════════════════════════ */

/** 입장 6가지를 만드는 최소 override. 여러 테스트가 같은 배열을 본다 */
const ALL_SIX = [
    { isMine: true, status: 'DEFENSE_WAIT', hasDefended: false },
    { isMine: true, status: 'DEFENSE_WAIT', hasDefended: true },
    { isMine: true, status: 'VOTING' },
    { isMine: false, status: 'DEFENSE_WAIT' },
    { isMine: false, status: 'VOTING', myVote: null },
    { isMine: false, status: 'VOTING', myVote: 'GUILTY' },
];

/* ══════════════════════════════════════════════════
 * 접힌 줄 두 줄 재구성 (이슈 #448)
 * ══════════════════════════════════════════════════ */

test('제목은 6가지 입장을 전부 다른 낱말로 가른다', () => {
    /*
     * #443 에서 뱃지를 「변론 중 / 투표 중」 두 낱말로 줄이고 급함은 색(`tone`)에 맡겼다.
     * 그 결과 「내가 변론을 냈다」(DEFENSE_SUBMITTED)와 「남이 변론을 쓰는 중」(DEFENSE_WAITING)이
     * 회색 「변론 중」 둘로 화면에서 완전히 같아졌다. 글자가 다시 6가지를 갈라야 한다.
     */
    const titles = ALL_SIX.map((overrides) => toTrialStatusCard(trial(overrides)).title);

    assert.equal(new Set(titles).size, 6, titles.join(' / '));
    for (const title of titles) {
        /* 제목은 문장이 아니라 라벨이다. 길면 다시 말줄임에 걸린다 */
        assert.ok(title.length <= 9, title);
        /* 명령형(「~해주세요」)은 잔소리로 읽혀 #443 에서 걷어냈다 */
        assert.doesNotMatch(title, /하세요|해주세요/);
    }
});

test('투표 현황은 색이 아니라 모양으로 내 표를 가른다', () => {
    /* 「나는 던졌는데 남들이 아직」과 「나도 아직」이 사용자가 가장 자주 헷갈리는 두 상태다 */
    const notYet = toTrialStatusCard(trial({ status: 'VOTING', myVote: null, voteCount: 2 }));
    const voted = toTrialStatusCard(trial({ status: 'VOTING', myVote: 'GUILTY', voteCount: 3 }));

    assert.equal(notYet.voteDots[0], 'mine-todo');
    assert.equal(voted.voteDots[0], 'mine-done');

    /* 내 표는 `voteCount` 에 이미 들어 있다 — 빼지 않으면 내 칸과 남의 칸에 두 번 그려진다 */
    assert.deepEqual(voted.voteDots, ['mine-done', 'done', 'done', 'todo', 'todo']);
    assert.deepEqual(notYet.voteDots, ['mine-todo', 'done', 'done', 'todo', 'todo']);

    /* 피고는 자기 재판의 배심원이 아니다(`canVote` 와 같은 규칙) — 내 칸을 만들면 남의 표를 내 것처럼 그린다 */
    const mine = toTrialStatusCard(trial({ isMine: true, status: 'VOTING', voteCount: 2 }));
    assert.deepEqual(mine.voteDots, ['done', 'done', 'todo', 'todo', 'todo']);

    /* 변론 대기 중에 0/5 를 그리면 아무도 안 던진 것처럼 보인다 */
    assert.equal(toTrialStatusCard(trial({ status: 'DEFENSE_WAIT' })).voteDots, null);
    /* 정원이 많으면 점이 줄을 밀어낸다 — 화면이 숫자로 떨어뜨리도록 null 을 준다 */
    assert.equal(toTrialStatusCard(trial({ status: 'VOTING', totalVoters: 20 })).voteDots, null);
});

test('접힌 줄은 두 줄이고 타이머는 여전히 없다', () => {
    const code = source(CARD);
    const summary = code.slice(code.indexOf('trial-status__summary'), code.indexOf('<Transition'));

    /* 구분점은 첫 줄, 잘려도 되는 맥락은 둘째 줄 */
    assert.match(summary, /trial-status__title/);
    assert.match(summary, /trial-status__sub-text/);
    /* 그룹명은 둘째 줄로 들어왔다. 카테고리·초과금액은 CTA 가 여는 변론 화면이 원본으로 보여준다 */
    assert.doesNotMatch(summary, /categoryName|exceededAmount/);
    /* 남은 시간은 펼친 본문에 있다 — 접힌 줄에 두면 6행이 매초 같이 떨린다 */
    assert.doesNotMatch(summary, /countdownOf/);
    /* 「마감 오늘 22:00」 같은 글 표기는 카운트다운과 같은 말이라 되살리지 않는다 */
    assert.doesNotMatch(code, /마감 \{\{/);
    /* 카운트다운 자체는 살아 있어야 한다 — 펼친 본문에서 */
    assert.match(code, /countdownOf\(item\)\.text/);
});

test('왼쪽 앵커는 아바타가 아니라 할 일 아이콘이다', () => {
    const code = source(CARD);
    const summary = code.slice(code.indexOf('trial-status__summary'), code.indexOf('<Transition'));

    /* 내 재판일 때 아바타에는 내 얼굴이 떠서 아무것도 알려주지 않았다 */
    assert.match(summary, /trial-status__icon/);
    assert.match(summary, /v-if="!item\.isMine"/);
    /* 아바타는 둘째 줄로 내렸다 — 34px 짜리가 왼쪽에 남아 있으면 앵커가 둘이 된다 */
    assert.doesNotMatch(summary, /:size="34"/);
    /* 뱃지 글자는 아이콘이 대신한다 */
    assert.doesNotMatch(code, /item\.badge/);
});

test('타일과 목록 줄이 같은 사물을 가리킨다', () => {
    /*
     * 타일에서 본 망치와 목록에서 본 망치가 다른 사물이면 「이 타일이 저 목록을 연다」가 안 읽힌다.
     * 그림체는 다르다 — 목록 줄은 16~20px 라 단색 SVG, 타일은 44px 라 컬러 오브젝트다.
     * **키를 `STANCE.icon` 이름으로 맞춰** 한쪽만 엉뚱한 사물로 갈아 끼우지 못하게 한다.
     */
    assert.match(source(CARD), /import TrialActionIcon from '\.\/TrialActionIcon\.vue'/);
    assert.match(source(ICON), /\['gavel', 'ballot', 'clock', 'scale'\]/);

    const grid = source(TODO_GRID);
    assert.match(grid, /const ART = \{ gavel: objDefenseImage, ballot: objVoteImage \}/);
    assert.match(grid, /const WATERMARK = \{ gavel: wmDefenseImage, ballot: wmVoteImage \}/);
    /* 타일이 그 키를 실제로 들고 있어야 매핑이 산다 */
    assert.match(grid, /icon: 'gavel'/);
    assert.match(grid, /icon: 'ballot'/);
});

test('아코디언은 고정 높이가 아니라 실제 높이로 여닫는다', () => {
    const code = source(CARD);

    assert.match(code, /<Transition/);
    assert.match(code, /el\.scrollHeight/);
    /* DefenseWriteView 의 고정 max-height 패턴은 투표 바가 조건부인 이 패널에서 잘린다 */
    assert.doesNotMatch(code, /max-height:\s*\d+px/);
});
