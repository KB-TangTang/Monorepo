import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

import { trialPillView } from '@/utils/groupChat';

/*
 * 재판 알림 컴팩트 필 (이슈 #452).
 *
 * 카드 세 종(기록·판결·폴백 pill)을 한 줄 필 하나로 합치면서 프론트 전용 제목
 * (「소비가 적발됐습니다」 등)이 사라졌다. 이제 단계를 구분하는 것은 <b>색 + 버튼 문구</b>뿐이라
 * 그 조합이 어긋나면 화면에서 단계를 읽을 방법이 없어진다. 여기서 지키는 것은 넷이다.
 *
 *   1. 색은 systemType·verdict.outcome 으로만 정한다 — 문구를 고쳐도 색이 뒤집히지 않는다
 *   2. 무죄는 초록이다 — 한 줄 필에서는 색이 유일한 결과 신호라 붉은 무죄는 정보를 뒤집는다
 *   3. 누를 수 없는 버튼을 그리지 않는다 — 개시 시점은 아직 투표도 변론 열람도 불가능하다
 *   4. 넘칠 때 줄어드는 쪽은 닉네임이고 문장은 끝까지 남는다 — 무슨 일이 일어났는지가 정보다
 */

const link = '/group-challenges/7/trial/42';

function pill(overrides) {
    return trialPillView({ content: '', deepLink: link, verdict: null, ...overrides });
}

/* ── 색 + 버튼 조합 ───────────────────────────────────────────────── */

test('적발은 Ink 색에 「사건 보기」를 달고 서버 딥링크로 간다', () => {
    assert.deepEqual(
        pill({ systemType: 'VIOLATION_DETECTED', content: '지민님의 소비가 한도를 넘었어요.' }),
        {
            tone: 'ink',
            leadName: '지민',
            lead: '님의 소비가 한도를 넘었어요.',
            stamp: null,
            ctaLabel: '사건 보기',
            ctaTo: link,
            ctaQuiet: false,
        },
    );
});

/*
 * 개시 시점의 기소 상태는 DEFENSE_WAIT 다. 변론은 아직 없고, VoteService 는 이 상태의 투표를
 * VOTE_NOT_ALLOWED 로 거절한다. 버튼을 달면 눌러도 되돌려 보내지는 길이 된다.
 */
test('개시에는 버튼이 없다 — 아직 변론도 투표도 없는 시점이다', () => {
    const view = pill({
        systemType: 'TRIAL_OPENED',
        content: '지민님이 피고인이에요. 변론이 시작됩니다.',
    });

    assert.equal(view.tone, 'ink');
    assert.equal(view.ctaLabel, null, '누를 수 없는 버튼을 그리지 않는다');
    assert.equal(view.ctaTo, null);
});

/*
 * 변론 등록이 곧 투표 개시다 — DefenseService 가 변론을 저장하면서 상태를 VOTING 으로 넘기고
 * 이 이벤트를 쏜다. 그래서 시안의 「배심 투표 시작」 필은 이 systemType 에 붙는다.
 */
test('변론은 Gold 색에 「투표하기」를 달고 투표 화면으로 간다', () => {
    assert.deepEqual(
        pill({ systemType: 'DEFENSE_REGISTERED', content: '지민님이 변론을 냈어요.' }),
        {
            tone: 'gold',
            leadName: '지민',
            lead: '님이 변론을 냈어요.',
            stamp: null,
            ctaLabel: '투표하기',
            ctaTo: '/group-challenges/7/vote/42',
            ctaQuiet: false,
        },
    );
});

test('투표 화면은 변론서를 먼저 펼치므로 별도의 「변론 확인」 버튼을 두지 않는다', () => {
    const labels = ['VIOLATION_DETECTED', 'TRIAL_OPENED', 'DEFENSE_REGISTERED', 'VERDICT_CONFIRMED']
        .map((systemType) => pill({ systemType, content: '지민님.' }).ctaLabel)
        .filter(Boolean);

    assert.deepEqual(labels, ['사건 보기', '투표하기', '판결문']);
});

/*
 * 판결은 이미 끝난 일이라 지금 눌러야 하는 적발·투표와 무게가 다르다. 채운 뱃지를 네 번 다 쓰면
 * 마감이 있는 「투표하기」가 나머지에 묻힌다. 시안도 판결 확정 필만 배경 없는 텍스트 버튼이다.
 */
test('판결문만 조용한 버튼이다 — 지금 눌러야 하는 것과 무게를 나눈다', () => {
    const quiet = (systemType) => pill({ systemType, content: '지민님.' }).ctaQuiet;

    assert.equal(quiet('VERDICT_CONFIRMED'), true);
    assert.equal(quiet('VIOLATION_DETECTED'), false);
    assert.equal(quiet('DEFENSE_REGISTERED'), false);
});

test('버튼이 없으면 조용할 것도 없다', () => {
    assert.equal(pill({ systemType: 'TRIAL_OPENED', content: '지민님.' }).ctaQuiet, false);
    assert.equal(
        trialPillView({ systemType: 'VERDICT_CONFIRMED', content: '지민님.', deepLink: null })
            .ctaQuiet,
        false,
        '갈 곳이 없어 버튼을 접었는데 조용한 버튼이라고 말하면 화면이 빈 글자를 그린다',
    );
});

/* ── 판결 도장 ────────────────────────────────────────────────────── */

/*
 * 판결 필의 아바타(49_verdict.png)는 유죄와 무죄에 같은 그림을 쓴다 — 「판결이 났다」까지만
 * 말하고 결과는 말하지 않는다. 도장으로 바꾸면 색 말고도 결과를 읽을 방법이 하나 생긴다.
 */
test('결과를 알면 아바타 자리에 도장이 들어간다', () => {
    const stamped = (outcome) =>
        pill({ systemType: 'VERDICT_CONFIRMED', content: '지민님.', verdict: { outcome } }).stamp;

    assert.equal(stamped('GUILTY'), 'GUILTY');
    assert.equal(stamped('INNOCENT'), 'INNOCENT');

    /* 도장은 두 장뿐이다. 결과를 모르는 옛 판결(#304 이전)은 탕이 얼굴로 남는다 */
    for (const unknown of [undefined, null, 'PENDING']) {
        assert.equal(stamped(unknown), null, `${unknown} 에 띄울 도장은 없다`);
    }
});

test('적발·변론은 도장을 달지 않는다 — 도장은 결과를 말하는 그림이다', () => {
    assert.equal(pill({ systemType: 'VIOLATION_DETECTED', content: '지민님.' }).stamp, null);
    assert.equal(pill({ systemType: 'DEFENSE_REGISTERED', content: '지민님.' }).stamp, null);
    assert.equal(pill({ systemType: 'TRIAL_OPENED', content: '지민님.' }).stamp, null);
});

/*
 * 도장은 아바타 자리의 그림이고 버튼은 오른쪽 끝의 링크다. 딥링크가 없어 「판결문」을 접어도
 * 결과는 그대로 알려야 한다 — 둘을 한 조건에 묶으면 링크가 빠진 판결이 얼굴만 남는다.
 */
test('도장은 버튼과 무관하다 — 갈 곳이 없어도 결과는 알린다', () => {
    const noLink = trialPillView({
        systemType: 'VERDICT_CONFIRMED',
        content: '지민님, 유죄예요.',
        deepLink: null,
        verdict: { outcome: 'GUILTY' },
    });

    assert.equal(noLink.ctaLabel, null, '갈 곳이 없으면 버튼은 접는다');
    assert.equal(noLink.stamp, 'GUILTY', '버튼을 접는다고 결과까지 감추지 않는다');
});

/* ── 판결 색 ──────────────────────────────────────────────────────── */

test('유죄는 붉은색이다', () => {
    const view = pill({
        systemType: 'VERDICT_CONFIRMED',
        content: '지민님, 유죄예요. 목숨 1개가 차감됐어요.',
        verdict: { outcome: 'GUILTY', guiltyVotes: 4, innocentVotes: 2, livesLost: 1 },
    });

    assert.equal(view.tone, 'danger');
    assert.equal(view.ctaLabel, '판결문');
    assert.equal(view.ctaTo, `${link}/verdict`);
});

test('무죄는 초록색이다 — 붉은 무죄는 색이 담는 정보를 뒤집는다', () => {
    const view = pill({
        systemType: 'VERDICT_CONFIRMED',
        content: '지민님, 무죄예요.',
        verdict: { outcome: 'INNOCENT', guiltyVotes: 2, innocentVotes: 4, livesLost: 0 },
    });

    assert.equal(view.tone, 'success');
    assert.notEqual(view.tone, 'danger');
});

test('색은 문구가 아니라 outcome 이 정한다', () => {
    const guiltyWords = { systemType: 'VERDICT_CONFIRMED', content: '지민님, 유죄예요.' };

    assert.equal(pill({ ...guiltyWords, verdict: { outcome: 'INNOCENT' } }).tone, 'success');
    assert.equal(pill({ ...guiltyWords, verdict: { outcome: 'GUILTY' } }).tone, 'danger');
});

test('결과가 없는 옛 판결 메시지는 중립색으로 남는다', () => {
    assert.equal(pill({ systemType: 'VERDICT_CONFIRMED', content: '판결.' }).tone, 'muted');
    assert.equal(
        pill({ systemType: 'VERDICT_CONFIRMED', content: '판결.', verdict: { outcome: 'PENDING' } })
            .tone,
        'muted',
        '모르는 값을 유죄로 넘기지 않는다',
    );
});

/* ── 문구 ─────────────────────────────────────────────────────────── */

/*
 * 서버가 만드는 판결 요약 네 가지(GroupVerdictTransitionService). 첫 문장이 「누구 + 결과」로
 * 딱 떨어지는 것은 금액이 「12,400원」이라 마침표가 없기 때문이다.
 * 버리는 뒷문장은 푸시 알림에 전문이 실려 나가므로(GROUP_JUDGMENT 틀이 {content}) 사라지지 않는다.
 */
test('판결 문구 네 가지 모두 첫 문장이 「누구 + 결과」로 끊긴다', () => {
    const shown = (content) => {
        const view = pill({ systemType: 'VERDICT_CONFIRMED', content });
        return view.leadName + view.lead;
    };

    assert.equal(shown('지민님, 유죄예요. 목숨 1개가 차감됐어요.'), '지민님, 유죄예요.');
    assert.equal(shown('지민님, 유죄예요. 남은 목숨이 없어요.'), '지민님, 유죄예요.');
    assert.equal(shown('지민님, 무죄예요.'), '지민님, 무죄예요.');
    assert.equal(shown('지민님, 무죄예요. 12,400원이 소비액에서 빠졌어요.'), '지민님, 무죄예요.');
});

/*
 * 닉네임은 1~50자다(utils/user.js · tbl_user.nickname VARCHAR(50)). 두 줄로도 안 담기는 것이
 * 정상 경로라 이 규칙이 정하는 것은 <b>무엇을 먼저 버릴지</b>다.
 *
 * 한 덩어리로 두면 뒤가 잘려 「가나다라마…」만 남고 무슨 일이 일어났는지가 통째로 사라진다.
 * 닉네임만 따로 떼어 두면 CSS 가 그쪽만 줄이고 문장은 끝까지 살아남는다.
 * 자르는 일 자체는 여전히 CSS 가 한다 — JS 로 자르면 폰트·화면 폭에 따라 어긋나고
 * 잘린 자리가 데이터가 된다.
 */
test('닉네임이 50자여도 줄어드는 쪽은 닉네임이고 문장은 통째로 남는다', () => {
    const nickname = '가'.repeat(50);
    const view = pill({
        systemType: 'VIOLATION_DETECTED',
        content: `${nickname}님의 소비가 한도를 넘었어요.`,
    });

    assert.equal(view.leadName, nickname, '닉네임을 JS 로 자르지 않는다 — 줄이는 건 CSS 다');
    assert.equal(view.lead, '님의 소비가 한도를 넘었어요.', '문장은 닉네임 길이와 무관하다');
});

test('마침표가 없는 문구는 통째로 쓴다 — 자르기 실패가 화면을 비우지 않는다', () => {
    const view = pill({ systemType: 'TRIAL_OPENED', content: '지민님이 피고인이에요' });

    assert.equal(view.leadName, '지민');
    assert.equal(view.lead, '님이 피고인이에요');
});

/*
 * 「님」을 못 찾거나 맨 앞에서 찾으면 가르지 않는다. 닉네임에 「님」을 쓸 수 있어서
 * (「님님」) 앞이 빈 조각이 나오는 경우가 실제로 있다.
 */
test('가를 자리를 못 찾으면 문구를 통째로 그린다', () => {
    const asIs = (content) => pill({ systemType: 'VIOLATION_DETECTED', content });

    assert.deepEqual(asIs('님님의 소비가 한도를 넘었어요.'), {
        tone: 'ink',
        leadName: '',
        lead: '님님의 소비가 한도를 넘었어요.',
        stamp: null,
        ctaLabel: '사건 보기',
        ctaTo: link,
        ctaQuiet: false,
    });
    assert.equal(asIs('한도를 넘은 소비가 있어요.').leadName, '', '「님」이 없으면 가르지 않는다');
});

/* ── systemType 을 모르는 메시지 ──────────────────────────────────── */

/*
 * 이 필드가 생기기 전에 Redis 에 쌓인 메시지다. TTL(종료일 + 2일) 동안 계속 조회된다.
 * 문구의 모양을 모르므로 첫 문장 자르기를 적용하지 않는다 — 뒤쪽을 소리 없이 버리게 된다.
 */
test('systemType 이 없는 옛 메시지는 문구를 자르지 않고 버튼도 없다', () => {
    assert.deepEqual(
        pill({ systemType: null, content: '재판이 시작됐어요. 모두 참여해 주세요.' }),
        {
            tone: 'muted',
            leadName: '',
            lead: '재판이 시작됐어요. 모두 참여해 주세요.',
            stamp: null,
            ctaLabel: null,
            ctaTo: null,
            ctaQuiet: false,
        },
    );
});

test('모르는 systemType 도 같은 폴백으로 떨어진다 — 서버가 값을 늘려도 화면이 비지 않는다', () => {
    const view = pill({ systemType: 'APPEAL_FILED', content: '항소했어요. 다시 다툽니다.' });

    assert.equal(view.tone, 'muted');
    assert.equal(view.lead, '항소했어요. 다시 다툽니다.', '모양을 모르는 문구를 자르지 않는다');
    assert.equal(view.ctaLabel, null);
});

/* ── 딥링크 ───────────────────────────────────────────────────────── */

test('딥링크가 없으면 버튼도 없다', () => {
    const view = trialPillView({
        systemType: 'VIOLATION_DETECTED',
        content: '지민님.',
        deepLink: null,
    });

    assert.equal(view.ctaLabel, null, '눌러도 아무 일이 없는 버튼을 그리지 않는다');
    assert.equal(view.ctaTo, null);
});

/*
 * 서버 deepLink 는 네 종류 모두 재판 진행 현황(타임라인)을 가리킨다. 「판결문」을 그대로 두면
 * 문구와 도착지가 어긋나 타임라인에서 「판결 보기」를 한 번 더 눌러야 했다.
 * VerdictResultView 가 확정 여부를 스스로 보고 아니면 진행 현황으로 되돌리므로 안전하다.
 */
test('판결문은 타임라인이 아니라 최종 판결 화면으로 간다', () => {
    const view = pill({
        systemType: 'VERDICT_CONFIRMED',
        content: '지민님, 무죄예요.',
        verdict: { outcome: 'INNOCENT' },
    });

    assert.equal(view.ctaTo, '/group-challenges/7/trial/42/verdict');
    assert.notEqual(view.ctaTo, link, '문구는 판결문인데 타임라인에 떨어뜨리지 않는다');
});

test('딥링크 모양이 예상과 다르면 서버가 준 값을 그대로 쓴다', () => {
    const view = trialPillView({
        systemType: 'DEFENSE_REGISTERED',
        content: '지민님이 변론을 냈어요.',
        deepLink: '/group-challenges/7/trial/42/verdict',
    });

    assert.equal(
        view.ctaTo,
        '/group-challenges/7/trial/42/verdict',
        '최악이 「진행 현황으로 간다」여야지 빈 화면이면 안 된다',
    );
});

/* ── 필이 실제로 그 규칙을 쓰는지 ─────────────────────────────────── */

function pillSource() {
    return readFileSync(
        new URL('../src/components/challenge/group/chat/GroupChatSystemPill.vue', import.meta.url),
        'utf8',
    );
}

test('필은 표시 규칙을 utils 에서 가져다 쓴다', () => {
    const src = pillSource();

    assert.match(src, /import \{[^}]*\btrialPillView\b[^}]*\} from '@\/utils\/groupChat'/);
    assert.doesNotMatch(
        src,
        /content\s*\.\s*(includes|indexOf|match|split)/,
        '문구를 파싱해 색이나 버튼을 정하면 문구 한 글자에 화면이 뒤집힌다',
    );
});

function rule(selector) {
    const found = pillSource().match(new RegExp(`\\${selector} \\{[\\s\\S]*?\\}`));
    assert.ok(found, `${selector} 규칙을 찾지 못했다`);
    return found[0];
}

/*
 * flex 자식의 기본값은 min-width:auto 라 내용보다 작아지지 못한다. min-width:0 이 빠지면
 * 줄이기가 걸리지 않고 필이 통째로 화면 밖으로 나간다 — 닉네임 50자에서 바로 재현된다.
 *
 * 두 줄까지 허용하는 이유는 360px 에서 한 줄이 약 13자인데 적발 문구가 닉네임 2자에서도
 * 17자라서다. 한 줄로 고정하면 사실상 모든 알림이 잘린다(브라우저에서 확인하고 뒤집었다).
 */
test('문구 칸은 두 줄까지 줄어들고 아바타·버튼은 줄어들지 않는다', () => {
    const lead = rule('.sys-pill__lead');

    assert.match(lead, /min-width:\s*0/, 'min-width:0 이 없으면 아예 줄어들지 않는다');
    assert.match(lead, /-webkit-line-clamp:\s*2/, '한 줄로 고정하면 대부분의 문구가 잘린다');
    assert.match(lead, /overflow:\s*hidden/);

    /* systemType 을 모르는 옛 메시지는 문구 모양을 알 수 없다. 공백 없는 긴 문자열이 오면
       끊을 자리가 없어 필이 통째로 화면 밖으로 나간다 — 합치기 전 폴백 pill 에도 있던 규칙이다 */
    assert.match(lead, /overflow-wrap:\s*break-word/, '끊을 자리가 없는 문구가 필을 밀어낸다');

    for (const cls of ['avatar', 'cta']) {
        assert.match(
            rule(`.sys-pill__${cls}`),
            /flex-shrink:\s*0/,
            `${cls} 가 줄어들면 문구 대신 아이콘이 찌그러진다`,
        );
    }
});

/*
 * 줄어드는 쪽을 닉네임으로 못박는 규칙. inline-block + max-width:100% 라 한 줄을 다 쓰면
 * 자기 자리에서 ellipsis 로 줄고, 뒤에 오는 문장은 잘리지 않는다.
 */
test('넘칠 때 잘리는 것은 닉네임 쪽이다', () => {
    const name = rule('.sys-pill__name');

    assert.match(name, /max-width:\s*100%/);
    assert.match(name, /text-overflow:\s*ellipsis/);
    assert.match(name, /white-space:\s*nowrap/, 'nowrap 이 없으면 닉네임이 줄바꿈돼 안 줄어든다');
});

/*
 * 시안의 Ink 필은 완전 라운드(999px)가 아니라 둥근 모서리다. 999px 은 Gold·Red 필에만 쓰던
 * 값이라 네 종에 다 적용하면 시안과 어긋난다 — 브라우저에서 확인하고 되돌렸다.
 *
 * 안쪽 버튼은 그보다 한 단계 작아야 한다. 중첩 반경은 「바깥 − 여백」이라야 두 곡선이 나란히
 * 도는데, 바깥만 18px 로 줄이고 버튼을 999px 로 두면 버튼이 필 위에 얹힌 것처럼 보인다.
 */
test('필과 버튼의 곡률이 나란히 돈다 — 어느 쪽도 완전 라운드가 아니다', () => {
    assert.match(rule('.sys-pill'), /border-radius:\s*var\(--tt-radius-lg\)/);
    assert.match(rule('.sys-pill__cta'), /border-radius:\s*var\(--tt-radius-sm\)/);

    /* 999px 은 필 전체가 알약이던 시절의 값이다 */
    for (const selector of ['.sys-pill', '.sys-pill__cta']) {
        assert.doesNotMatch(rule(selector), /border-radius:\s*var\(--tt-radius-full\)/);
    }
});

/* 판결문은 배경을 빼고 문구와 같은 크기로 둔다 — 문장 끝에 이어지는 링크처럼 읽힌다 */
test('판결문 버튼은 채운 뱃지가 아니다', () => {
    const quiet = rule('.sys-pill__cta--quiet');

    assert.match(quiet, /background:\s*none/);
    assert.match(quiet, /font-size:\s*var\(--tt-fs-caption\)/);
});

/*
 * 도장은 427px 원본을 줄여 쓴다. 아바타 크기인 30px 에서는 「유죄」·「무죄」가 뭉개져 색 얼룩이 되고
 * 40px 부터 한글이 읽힌다 — 실제로 줄여 보고 정했다. 이 값이 작아지면 도장을 쓰는
 * 이유(결과를 색이 아니라 글자로도 말한다)가 통째로 사라지므로 하한을 못박는다.
 */
test('도장은 글자가 읽히는 크기로 그린다', () => {
    const stamp = rule('.sys-pill__avatar--stamp');
    const size = Number(stamp.match(/width:\s*(\d+)px/)[1]);

    assert.ok(size >= 40, `도장이 ${size}px 이면 유죄·무죄 글자가 뭉개진다`);
    assert.match(stamp, /height:\s*40px/, '원본이 정사각형이라 가로세로가 같아야 안 찌그러진다');
    assert.match(
        stamp,
        /background:\s*none/,
        '도장은 원본이 원이라 흰 원판을 깔면 테두리가 겹친다',
    );
});

/*
 * 도장 그림은 결과가 정한다. 필이 문구에서 「유죄」를 찾아 그림을 고르면 문구 한 글자에
 * 무죄 판결이 유죄 도장을 달게 된다 — 색을 outcome 으로만 정하는 것과 같은 이유다.
 */
test('도장 그림은 문구가 아니라 stamp 가 고른다', () => {
    const src = pillSource();

    assert.match(src, /STAMPS\[view\.value\.stamp\]/);
    assert.match(src, /guilty_stamp\.png/);
    assert.match(src, /innocent_stamp\.png/);
});

/*
 * 도장에는 alt 가 있어야 한다. 옆 문구가 「유죄예요」를 이미 말하니 중복처럼 보이지만 그 문구는
 * 서버가 만드는 값이고(GroupVerdictTransitionService), 문장이 바뀌면 결과가 그림에만 남는다.
 * 합치기 전 GroupChatVerdictCard 가 쓰던 값을 그대로 가져왔다.
 */
test('도장에 alt 가 있다 — 결과를 그림으로만 말하지 않는다', () => {
    const src = pillSource();

    assert.match(src, /alt:\s*'유죄 판결 인장'/);
    assert.match(src, /alt:\s*'무죄 판결 인장'/);
    assert.doesNotMatch(
        src,
        /guiltyStamp,\s*alt:\s*''/,
        '도장 alt 를 비우면 판결 결과를 색과 그림으로만 말하게 된다',
    );
});

function pillStyles() {
    return pillSource().split('<style')[1];
}

/* 주석에는 「이래서 안 쓴다」는 설명으로 금지 값이 등장한다. 규칙은 실제 선언에만 걸어야 한다 */
function pillDeclarations() {
    return pillStyles().replace(/\/\*[\s\S]*?\*\//g, '');
}

test('색은 HEX 가 아니라 디자인 토큰으로 쓴다', () => {
    assert.doesNotMatch(
        pillDeclarations(),
        /#[0-9a-fA-F]{3,8}\b/,
        '토큰을 두고 HEX 를 박으면 테마가 갈라진다',
    );
});

/*
 * 문구를 투명도로 흐리지 않는다. 필 색이 다섯 종이라 같은 opacity 라도 배경마다 대비가 다르게
 * 깎인다 — 0.78 을 얹었을 때 gold 4.56 → 3.06, danger 4.11 → 2.97 로 12px 본문의 AA 기준
 * (4.5)을 크게 밑돌았다. --tt-accent-warn 처럼 「이 배경 위에서 쓰라」고 잡아 둔 토큰의 짝이
 * 통째로 깨진다. 강약은 굵기(--tt-fw-black vs semibold)로만 준다.
 */
test('문구를 투명도로 흐리지 않는다 — 필 색마다 대비가 다르게 깎인다', () => {
    /* 등장 애니메이션의 opacity: 0 은 대상이 아니다. 여기서 막는 것은 화면에 남는 반투명 글자다 */
    assert.doesNotMatch(
        pillDeclarations(),
        /opacity:\s*0?\.\d/,
        '연한 글자가 필요하면 투명도가 아니라 토큰으로 색을 정한다',
    );
});

/*
 * 시각은 합치기 전 기록·판결 카드에 있었는데 필로 옮기면서 통째로 빠졌었다. 시스템 메시지는
 * GroupChatBubble 이 아니라 이 컴포넌트로 가므로 화면이 넘기는 show-time 이 닿지 않는다 —
 * 여기서 직접 그려야 방 안의 재판 알림만 시각 없는 줄로 남지 않는다.
 */
test('필도 시각을 그린다 — 재판 알림만 시각 없는 줄로 남지 않는다', () => {
    const src = pillSource();

    assert.match(src, /clockLabel\(props\.message\.sentAt\)/, '시각 포맷은 utils 것을 쓴다');
    assert.match(src, /v-if="timeLabel"/, 'sentAt 이 없으면 빈 자리를 만들지 않는다');
    assert.match(
        rule('.sys-pill__time'),
        /font-size:\s*var\(--tt-fs-overline\)/,
        '말풍선의 시각(.bubble-row__time)과 같은 크기여야 한 화면에서 따로 놀지 않는다',
    );
});

/*
 * 등장 애니메이션과 그 예외. 합치기 전 두 카드 모두 갖고 있던 쌍이라 한쪽만 되살리면
 * 「어지러움 설정을 켰는데도 움직이는 화면」이 된다.
 */
test('등장 애니메이션에는 prefers-reduced-motion 예외가 따라온다', () => {
    const styles = pillStyles();

    assert.match(styles, /animation:\s*sys-pill-enter/);
    assert.match(styles, /@keyframes sys-pill-enter/);
    assert.match(
        styles,
        /@media \(prefers-reduced-motion: reduce\)[\s\S]*?animation:\s*none/,
        '움직임을 끈 사용자에게도 도는 애니메이션은 접근성 회귀다',
    );
});
