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

test('남의 재판 제목에는 피고 닉네임이 들어간다', () => {
    assert.match(toTrialStatusCard(trial({ status: 'VOTING' })).title, /지판/);
    /* 내 재판에 내 닉네임을 넣으면 「지판님 재판에 투표해주세요」가 나에게 뜬다 */
    assert.doesNotMatch(
        toTrialStatusCard(trial({ isMine: true, status: 'VOTING', nickname: '지판' })).title,
        /지판/,
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

test('뱃지는 명령이 아니라 재판 단계를 말한다', () => {
    /* 「변론 필요」는 바로 옆 제목 「내 변론이 필요해요」와 같은 말이었다 */
    assert.equal(
        toTrialStatusCard(trial({ isMine: true, status: 'DEFENSE_WAIT' })).badge,
        '변론 중',
    );
    assert.equal(toTrialStatusCard(trial({ isMine: false, status: 'VOTING' })).badge, '투표 중');

    /* 명령형이 되살아나면 같은 지적을 다시 받는다 — 6가지 입장 전부를 본다 */
    const allSix = [
        { isMine: true, status: 'DEFENSE_WAIT', hasDefended: false },
        { isMine: true, status: 'DEFENSE_WAIT', hasDefended: true },
        { isMine: true, status: 'VOTING' },
        { isMine: false, status: 'DEFENSE_WAIT' },
        { isMine: false, status: 'VOTING', myVote: null },
        { isMine: false, status: 'VOTING', myVote: 'GUILTY' },
    ];
    for (const overrides of allSix) {
        assert.doesNotMatch(toTrialStatusCard(trial(overrides)).badge, /필요|하세요/);
    }
});

test('접힌 줄은 아바타 · 뱃지 · 제목 한 줄로 끝난다', () => {
    const code = source(CARD);
    const summary = code.slice(code.indexOf('trial-status__summary'), code.indexOf('<Transition'));

    /*
     * 재판이 6건씩 뜬다. 행마다 카테고리·그룹명·초과금액·타이머가 같이 붙으면 목록이 안 읽힌다.
     * 카테고리·한도·초과금액은 CTA 가 여는 변론 화면이 거래내역 원본으로 다시 보여주고,
     * 남은 시간은 펼친 본문으로 내렸다 — 접힌 줄에 두면 6행이 매초 같이 떨린다.
     */
    assert.doesNotMatch(summary, /categoryName|exceededAmount|groupName|countdownOf/);
    /* 「마감 오늘 22:00」 같은 글 표기는 카운트다운과 같은 말이라 되살리지 않는다 */
    assert.doesNotMatch(code, /마감 \{\{/);
    /* 카운트다운 자체는 살아 있어야 한다 — 펼친 본문에서 */
    assert.match(code, /countdownOf\(item\)\.text/);
});

test('뱃지는 pill 이 아니라 모서리만 둥근 사각형이다', () => {
    const code = source(CARD);
    const badge = code.slice(code.indexOf('.trial-status__badge {'));

    /* 카드가 --tt-radius-xl(22px). 그 안의 태그는 --tt-radius-xs(8px) — 토큰이 「태그」용으로 정의돼 있다 */
    assert.match(badge.slice(0, 200), /border-radius: var\(--tt-radius-xs\)/);
    assert.doesNotMatch(badge.slice(0, 200), /--tt-radius-full/);
});

test('아코디언은 고정 높이가 아니라 실제 높이로 여닫는다', () => {
    const code = source(CARD);

    assert.match(code, /<Transition/);
    assert.match(code, /el\.scrollHeight/);
    /* DefenseWriteView 의 고정 max-height 패턴은 투표 바가 조건부인 이 패널에서 잘린다 */
    assert.doesNotMatch(code, /max-height:\s*\d+px/);
});
