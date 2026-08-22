import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

import {
    formatIssuedDate,
    formatJoinDeadline,
    inviteBadges,
    isInviteOpen,
    isStartDateToday,
    remainingSeats,
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
const JOIN_VIEW = 'src/views/challenge/group/GroupJoinView.vue';

const RECRUITING = {
    status: 'RECRUITING',
    startDate: '2026-08-01',
    maxMembers: 6,
    memberCount: 2,
};

/* 시작일 하루 전 · 당일 · 다음 날. 로컬 자정으로 만들어야 문자열 변환이 밀리지 않는다 */
const BEFORE_START = new Date(2026, 6, 31);
const ON_START = new Date(2026, 7, 1);
const AFTER_START = new Date(2026, 7, 2);

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
 * 서버(ChallengeGroupService#joinBlockReason)와 같은 규칙을 쓴다.
 * RECRUITING 이거나, ACTIVE 이면서 오늘이 시작일을 넘지 않았으면 열려 있다(이슈 #350).
 */
test('모집 중이고 자리가 남았으면 초대할 수 있다', () => {
    assert.equal(isInviteOpen(RECRUITING, BEFORE_START), true);
});

/*
 * 상태 전이 배치는 시작일 00:01 에 돌아 ACTIVE 로 바꾼다. status 만 보면 시작일 하루가
 * 통째로 막혀 「시작일 23:59까지 초대」가 거짓말이 된다(이슈 #350).
 */
test('시작일 당일에는 ACTIVE 라도 초대할 수 있다', () => {
    assert.equal(isInviteOpen({ ...RECRUITING, status: 'ACTIVE' }, ON_START), true);
});

test('시작일이 지난 ACTIVE 는 초대할 수 없다', () => {
    assert.equal(isInviteOpen({ ...RECRUITING, status: 'ACTIVE' }, AFTER_START), false);
});

test('재판·종결 상태는 시작일 당일이어도 초대할 수 없다', () => {
    assert.equal(isInviteOpen({ ...RECRUITING, status: 'JUDGING' }, ON_START), false);
    assert.equal(isInviteOpen({ ...RECRUITING, status: 'CLOSED' }, ON_START), false);
});

/*
 * 배치가 밀리면 시작일이 지나도 RECRUITING 이다. 이때는 열어 둔다 — 화면이 날짜로 따로
 * 판정하면 「화면은 마감인데 참여는 되는」 어긋남이 난다(#152 가 막으려던 것).
 */
test('배치가 안 돌아 RECRUITING 이면 시작일이 지나도 초대할 수 있다', () => {
    assert.equal(isInviteOpen(RECRUITING, AFTER_START), true);
});

test('정원이 찼으면 모집 중이어도 초대할 수 없다', () => {
    assert.equal(isInviteOpen({ ...RECRUITING, memberCount: 6 }, BEFORE_START), false);
});

test('memberCount 가 없으면 members 길이로 센다 — 미리보기 응답이 그 모양이다', () => {
    const group = { ...RECRUITING, memberCount: undefined, members: [{}, {}, {}] };

    assert.equal(isInviteOpen(group, BEFORE_START), true);
});

/* ── 뱃지 ────────────────────────────────────────────────────────── */

test('초대할 수 있을 때만 마감 시각을 함께 띄운다', () => {
    assert.deepEqual(inviteBadges(RECRUITING, BEFORE_START), [
        { text: '초대 가능', variant: 'success' },
        { text: '8월 1일 23:59까지', variant: 'danger' },
    ]);
});

test('닫힌 방에는 마감 시각을 붙이지 않는다 — 아직 되는 줄 안다', () => {
    assert.deepEqual(inviteBadges({ ...RECRUITING, status: 'ACTIVE' }, AFTER_START), [
        { text: '모집 마감', variant: 'danger' },
    ]);
    assert.deepEqual(inviteBadges({ ...RECRUITING, memberCount: 6 }, BEFORE_START), [
        { text: '정원 마감', variant: 'danger' },
    ]);
});

/* 시작일 당일 ACTIVE 는 아직 모집 중이다 — 자리 때문에 막힌 것이면 「정원 마감」이라고 말해야 한다 */
test('시작일 당일 ACTIVE 이면서 정원이 찼으면 정원 마감이다', () => {
    assert.deepEqual(inviteBadges({ ...RECRUITING, status: 'ACTIVE', memberCount: 6 }, ON_START), [
        { text: '정원 마감', variant: 'danger' },
    ]);
});

test('그룹을 아직 못 받았으면 뱃지를 만들지 않는다', () => {
    assert.deepEqual(inviteBadges(null), []);
});

/* ── 남은 자리 ───────────────────────────────────────────────────── */

test('남은 자리는 정원에서 인원을 뺀 수다', () => {
    assert.equal(remainingSeats(RECRUITING, BEFORE_START), 4);
});

/*
 * 0 이 아니라 null 이다. 「0자리 남았어요」는 자리를 세는 말투로 마감을 알리는 셈이라
 * 화면이 그대로 그리면 어색하다. null 이면 화면이 다른 문구로 갈아탄다.
 */
test('초대할 수 없는 방은 자리 수 대신 null 을 준다', () => {
    assert.equal(remainingSeats({ ...RECRUITING, memberCount: 6 }, BEFORE_START), null);
    assert.equal(remainingSeats({ ...RECRUITING, status: 'ACTIVE' }, AFTER_START), null);
    assert.equal(remainingSeats(null, BEFORE_START), null);
});

/* ── 시작일 당일 참여 안내 ───────────────────────────────────────── */

test('오늘이 시작일일 때만 참여 안내 대상이다', () => {
    assert.equal(isStartDateToday(RECRUITING, ON_START), true);
    assert.equal(isStartDateToday(RECRUITING, BEFORE_START), false);
    assert.equal(isStartDateToday(RECRUITING, AFTER_START), false);
    assert.equal(isStartDateToday(null, ON_START), false);
});

/*
 * 시작일에 들어와도 그 날 0시부터의 소비가 전부 집계된다(이슈 #350). 참여 시각으로 자르지
 * 않기로 한 결정이라, 들어오기 전에 화면이 한 줄로 알려야 한다.
 */
test('참여 화면은 시작일 당일에만 집계 시점을 알린다', () => {
    const src = source(JOIN_VIEW);

    assert.match(src, /isStartDateToday\(/, '판정을 화면이 직접 계산하고 있다');
    assert.match(src, /오늘 0시부터의 소비가 집계돼요/);
    assert.match(
        src,
        /v-if="joiningOnStartDate"/,
        '항상 노출되면 시작 전 참여자에게 거짓말이 된다',
    );
});

/* ── 화면이 규칙을 실제로 쓰는지 ─────────────────────────────────── */

test('초대 화면은 뱃지·남은 자리를 하드코딩하지 않는다', () => {
    const src = source(INVITE_VIEW);

    assert.doesNotMatch(src, /초대 가능/, '뱃지 문자열이 화면에 박혀 있다');
    assert.match(src, /inviteBadges\(/);
    assert.match(src, /remainingSeats\(/);
});

/*
 * 마감 시각은 헤더 뱃지가 이미 말한다. 본문에 또 적으면 문장이 길어지고
 * 정작 「몇 자리 남았는지」가 그 안에 묻힌다.
 */
test('마감 시각을 본문에 한 번 더 적지 않는다', () => {
    assert.doesNotMatch(source(INVITE_VIEW), /23:59/);
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

/*
 * 헤더가 그리는 문자열은 `title` 하나뿐이고 그것은 우리가 쓴 리터럴이라 `v-html` 로 둔다.
 * 그룹 이름 같은 **사용자 입력을 헤더에 넘기지 않는다** — 넘기는 순간 v-html 이 태그를 실행한다.
 */
test('헤더는 사용자 입력을 그리지 않는다', () => {
    const src = source(INVITE_HEADER);

    assert.doesNotMatch(src, /subtitle/);
    assert.equal(src.match(/v-html/g).length, 1);
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
