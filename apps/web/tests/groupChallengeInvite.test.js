import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

import {
    createdSubtitle,
    formatIssuedDate,
    formatJoinDeadline,
    inviteBadges,
    inviteNotice,
    isInviteOpen,
} from '@/utils/groupInvite';

/*
 * 친구 초대(소환장) 화면.
 *
 * 예전 화면은 「초대 가능」·「첫날 23:59까지」를 **생성 직후인지 여부만 보고** 고정 문자열로
 * 띄웠다. 목록에서 들어오면 마감된 방인데도 초대할 수 있는 것처럼 보였다.
 * 이제는 서버와 같은 근거(`status` + 정원)로 계산한다.
 */

function source(path) {
    return readFileSync(new URL(`../${path}`, import.meta.url), 'utf8');
}

const INVITE_VIEW = 'src/views/challenge/group/GroupInviteView.vue';
const CREATE_VIEW = 'src/views/challenge/group/GroupCreateView.vue';
const INVITE_HEADER = 'src/components/challenge/group/GroupInviteHeader.vue';

const RECRUITING = {
    status: 'RECRUITING',
    startDate: '2026-08-01',
    maxMembers: 6,
    memberCount: 2,
};

/* ── 발부일 ──────────────────────────────────────────────────────── */

test('발부일은 createdAt 을 그대로 찍는다', () => {
    assert.equal(formatIssuedDate('2026-07-29T21:14:00'), '2026. 07. 29');
});

/*
 * new Date() 로 파싱하면 UTC 로 읽은 뒤 로컬 타임존으로 되돌려 하루가 밀린다.
 * 한국(UTC+9)에서 자정 직후에 만든 그룹의 발부일이 전날로 찍히는 증상이다.
 */
test('자정 직후 생성분도 날짜가 밀리지 않는다', () => {
    assert.equal(formatIssuedDate('2026-08-01T00:30:00'), '2026. 08. 01');
});

test('createdAt 이 없으면 빈 문자열 — 소환장이 그 줄을 감춘다', () => {
    assert.equal(formatIssuedDate(null), '');
    assert.equal(formatIssuedDate(undefined), '');
    assert.equal(formatIssuedDate(''), '');
});

/* ── 참여 마감 ───────────────────────────────────────────────────── */

test('마감 문구는 시작일 당일 23:59 이다', () => {
    assert.equal(formatJoinDeadline('2026-08-01'), '8월 1일 23:59까지');
});

/* ── 초대 가능 판정 ──────────────────────────────────────────────── */

/*
 * 서버(ChallengeGroupService#joinBlockReason)는 status 하나만 본다. 화면이 startDate 와
 * 오늘을 따로 비교하면 상태 전이 배치가 밀렸을 때 「화면은 마감인데 참여는 되는」 어긋남이 난다.
 */
test('모집 중이고 자리가 남았으면 초대할 수 있다', () => {
    assert.equal(isInviteOpen(RECRUITING), true);
});

test('시작한 방은 시작일이 오늘이어도 초대할 수 없다 — 판단 근거는 status 하나다', () => {
    assert.equal(isInviteOpen({ ...RECRUITING, status: 'ACTIVE' }), false);
    assert.equal(isInviteOpen({ ...RECRUITING, status: 'JUDGING' }), false);
    assert.equal(isInviteOpen({ ...RECRUITING, status: 'CLOSED' }), false);
});

test('정원이 찼으면 모집 중이어도 초대할 수 없다', () => {
    assert.equal(isInviteOpen({ ...RECRUITING, memberCount: 6 }), false);
});

test('memberCount 가 없으면 members 길이로 센다 — 미리보기 응답이 그 모양이다', () => {
    const group = { ...RECRUITING, memberCount: undefined, members: [{}, {}, {}] };

    assert.equal(isInviteOpen(group), true);
});

/* ── 뱃지 ────────────────────────────────────────────────────────── */

test('초대할 수 있을 때만 마감 시각을 함께 띄운다', () => {
    assert.deepEqual(inviteBadges(RECRUITING), [
        { text: '초대 가능', variant: 'success' },
        { text: '8월 1일 23:59까지', variant: 'danger' },
    ]);
});

test('닫힌 방에는 마감 시각을 붙이지 않는다 — 아직 되는 줄 안다', () => {
    assert.deepEqual(inviteBadges({ ...RECRUITING, status: 'ACTIVE' }), [
        { text: '모집 마감', variant: 'danger' },
    ]);
    assert.deepEqual(inviteBadges({ ...RECRUITING, memberCount: 6 }), [
        { text: '정원 마감', variant: 'danger' },
    ]);
});

test('그룹을 아직 못 받았으면 뱃지를 만들지 않는다', () => {
    assert.deepEqual(inviteBadges(null), []);
});

/* ── 안내 문구 ───────────────────────────────────────────────────── */

test('안내 문구에 남은 자리 수가 실제로 들어간다', () => {
    const notice = inviteNotice(RECRUITING);

    assert.match(notice, /8월 1일 23:59/);
    assert.match(notice, /4자리/);
});

test('닫힌 방에는 초대를 권하지 않는다', () => {
    assert.doesNotMatch(inviteNotice({ ...RECRUITING, status: 'CLOSED' }), /초대할 수 있어요/);
});

/* ── 화면이 규칙을 실제로 쓰는지 ─────────────────────────────────── */

test('초대 화면은 뱃지·안내를 하드코딩하지 않는다', () => {
    const src = source(INVITE_VIEW);

    assert.doesNotMatch(src, /초대 가능/, '뱃지 문자열이 화면에 박혀 있다');
    assert.match(src, /inviteBadges\(/);
    assert.match(src, /inviteNotice\(/);
});

test('소환장에 그룹 이름과 발부일을 넘긴다 — 더미로 남겨 두지 않는다', () => {
    const src = source(INVITE_VIEW);
    const tag = src.match(/<GroupSummonCard[\s\S]*?\/>/);

    assert.ok(tag, '소환장 카드를 찾지 못했다');
    assert.match(tag[0], /:group-name="group\.groupName"/);
    assert.match(tag[0], /:invite-code="group\.inviteCode"/);
    assert.match(tag[0], /:issued-at="group\.createdAt"/);
});

/*
 * 생성 완료 직후에는 되돌아갈 앞 화면이 「방금 끝낸 위자드」다.
 * ← 를 띄우면 그리로 돌아가겠다는 약속이 되므로 × 를 쓴다.
 */
test('생성 완료 직후에는 상단이 × 다', () => {
    const src = source(INVITE_VIEW);

    assert.match(src, /:nav-mode="isPostCreate \? 'close' : 'back'"/);
});

/*
 * 아이콘만 바꾸면 기기 뒤로가기가 여전히 완료된 위자드로 돌아간다.
 * 생성 화면이 push 가 아니라 replace 로 들어와야 그 자리가 덮인다.
 */
test('생성 화면은 초대 화면으로 replace 한다 — push 면 뒤로가기가 위자드로 돌아간다', () => {
    const src = source(CREATE_VIEW);
    const goToInvite = src.match(/function goToInvite\([^)]*\)\s*\{[\s\S]*?\n\}/);

    assert.ok(goToInvite, 'goToInvite 를 찾지 못했다');
    assert.match(goToInvite[0], /router\.replace\(/);
    assert.doesNotMatch(goToInvite[0], /router\.push\(/);
});

test('초대 화면 헤더는 두 가지 상단 아이콘을 모두 갖고 있다', () => {
    const src = source(INVITE_HEADER);

    assert.match(src, /XMarkIcon/);
    assert.match(src, /ChevronLeftIcon/);
    assert.match(src, /navMode/);
});

/* ── 생성 완료 화면 통합 ─────────────────────────────────────────── */

test('생성 완료 한 줄에 그룹 이름과 시작일이 함께 들어간다', () => {
    const line = createdSubtitle({ ...RECRUITING, groupName: '배달 소비 줄이기' });

    assert.equal(line, '배달 소비 줄이기 · 8월 1일부터 시작해요');
});

test('시작일을 못 읽으면 그룹 이름만 남긴다 — 「· 부터 시작해요」 같은 반쪽 문장을 만들지 않는다', () => {
    assert.equal(createdSubtitle({ groupName: '배달 소비 줄이기' }), '배달 소비 줄이기');
    assert.equal(createdSubtitle(null), '');
});

/*
 * 그룹 이름은 사용자가 적은 값이다. inviteNotice 와 달리 `<b>` 를 섞지 않는 이유가 그것이다 —
 * 헤더가 v-html 로 그리면 이름에 넣은 태그가 그대로 실행된다.
 */
test('생성 완료 한 줄에는 태그를 섞지 않는다', () => {
    const line = createdSubtitle({ ...RECRUITING, groupName: '<img src=x onerror=alert(1)>' });

    assert.doesNotMatch(line, /<b>/);
    assert.match(source(INVITE_HEADER), /class="gih-subtitle">\{\{ subtitle \}\}/);
});

/*
 * 축하만 보여주고 「친구 초대하기」를 한 번 더 누르게 하던 4단계 화면을 없앴다.
 * 그 화면이 알려주던 초대 마감·인원은 초대 화면이 실데이터로 이미 보여준다.
 */
test('생성 위자드에 성공 화면이 남아 있지 않다', () => {
    const src = source(CREATE_VIEW);

    assert.doesNotMatch(src, /currentStep\.value = 4/, '4단계로 넘어가는 코드가 남아 있다');
    assert.doesNotMatch(src, /gcv-success/, '성공 화면 마크업·스타일이 남아 있다');
    assert.doesNotMatch(src, /celebration/, '탕이는 초대 화면으로 옮겼다');
});

test('생성에 성공하면 화면을 갈아타지 않고 곧바로 초대 화면으로 간다', () => {
    const src = source(CREATE_VIEW);
    const handleCreate = src.match(/async function handleCreate\(\)\s*\{[\s\S]*?\n\}/);

    assert.ok(handleCreate, 'handleCreate 를 찾지 못했다');
    assert.match(handleCreate[0], /goToInvite\(created\.groupId\)/);
});

test('축하 문구와 탕이는 생성 완료로 들어왔을 때만 나온다', () => {
    const src = source(INVITE_VIEW);

    assert.match(src, /isPostCreate\.value \? '그룹을 제대로<br>만들었어요!' : '배심원을/);
    assert.match(src, /isPostCreate\.value \? celebrationImg : ''/);
});
