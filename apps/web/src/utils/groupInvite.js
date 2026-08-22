/*
 * 친구 초대(소환장) 화면의 표시값 계산.
 *
 * 화면 컴포넌트에서 떼어 낸 이유는 두 가지다. 하나는 테스트할 수 있게 하려는 것이고,
 * 다른 하나는 **참여 가능 판정 기준을 서버와 한 줄로 맞춰 두기 위해서**다.
 * 기준이 갈리면 화면은 「마감」인데 참여는 되는(또는 그 반대의) 어긋남이 난다.
 *
 * 서버 규칙(`ChallengeGroupService#joinBlockReason`)은 지금 이렇다.
 *   RECRUITING                        → 열림
 *   ACTIVE 이고 오늘 <= 시작일         → 열림 (이슈 #350)
 *   그 밖(ACTIVE 이고 시작일이 지남 · JUDGING · CLOSED) → 닫힘
 *
 * ACTIVE 갈래에서만 날짜를 보는 것은 상태 전이 배치가 **시작일 00:01** 에 돌기 때문이다.
 * status 만 보면 시작일 하루가 통째로 막혀 「시작일 23:59까지 초대」가 거짓말이 된다.
 * 배치가 밀렸을 때는 status 가 RECRUITING 이라 이 조건에 애초에 닿지 않는다 —
 * #152 가 막으려던 「배치 지연으로 status 와 날짜가 어긋나는」 경우는 그대로 안전하다.
 */

import { formatMonthDay } from '@/utils/groupRanking';

/**
 * 소환장 발부일. `2026. 08. 01` 형태로 찍는다.
 *
 * 서버가 `createdAt` 을 ISO-8601 문자열로 내려준다(Jackson, WRITE_DATES_AS_TIMESTAMPS off).
 * 값이 없으면 빈 문자열을 돌려준다 — 소환장에서 그 줄을 통째로 감추기 위함이다.
 * 오늘 날짜로 대체하지 않는다. 재방문할 때마다 발부일이 바뀌면 서류로서 거짓이 된다.
 */
export function formatIssuedDate(createdAt) {
    if (!createdAt) return '';

    /* 'T' 앞부분만 본다. new Date() 로 파싱하면 타임존 보정이 끼어들어 하루 밀릴 수 있다 */
    const [date] = String(createdAt).split('T');
    const [year, month, day] = date.split('-');
    if (!year || !month || !day) return '';

    return `${year}. ${month}. ${day}`;
}

/**
 * 참여 마감 문구. `8월 1일 23:59까지`.
 *
 * 초대 코드에는 만료 컬럼이 없다. 시작일 당일 23:59 까지 모집한다(`docs/API_SPEC.md`).
 */
export function formatJoinDeadline(startDate) {
    const label = formatMonthDay(startDate);
    return label ? `${label} 23:59까지` : '';
}

/** 지금 이 그룹에 초대할 수 있는지. 서버와 같이 상태 · 시작일 · 정원을 본다 */
export function isInviteOpen(group, today = new Date()) {
    if (!isJoinWindowOpen(group, today)) return false;
    return memberCount(group) < (group.maxMembers ?? 6);
}

/**
 * 헤더 뱃지.
 *
 * 초대할 수 있을 때만 마감 시각을 함께 띄운다. 이미 닫힌 방에 마감을 붙여 봐야
 * 「아직 되는 건가?」 하고 헷갈리기만 한다.
 */
export function inviteBadges(group, today = new Date()) {
    if (!group) return [];

    if (isInviteOpen(group, today)) {
        const badges = [{ text: '초대 가능', variant: 'success' }];
        const deadline = formatJoinDeadline(group.startDate);
        if (deadline) badges.push({ text: deadline, variant: 'danger' });
        return badges;
    }

    /* 모집 기간 자체는 열려 있는데 못 들어가는 것이면 이유는 자리뿐이다 */
    if (isJoinWindowOpen(group, today)) {
        return [{ text: '정원 마감', variant: 'danger' }];
    }
    return [{ text: '모집 마감', variant: 'danger' }];
}

/**
 * 남은 자리 수. 초대할 수 없는 상태면 `null` — 화면이 자리 수 대신 안내 문구를 띄운다.
 *
 * 마감 시각은 여기 섞지 않는다. 이미 헤더 뱃지가 같은 말을 하고 있어, 한 화면에서 두 번
 * 읽히면 정작 **몇 자리 남았는지**가 문장 속에 묻힌다.
 */
export function remainingSeats(group, today = new Date()) {
    if (!isInviteOpen(group, today)) return null;
    return (group.maxMembers ?? 6) - memberCount(group);
}

/** 오늘이 시작일 당일인지. 참여 화면이 「오늘 0시부터 집계된다」를 이때만 알린다 */
export function isStartDateToday(group, today = new Date()) {
    if (!group?.startDate) return false;
    return dateOnly(group.startDate) === toDateStr(today);
}

/** 정원을 빼고 본 모집 창. 상태 전이 배치가 ACTIVE 로 바꾼 뒤에도 시작일 하루는 열려 있다 */
function isJoinWindowOpen(group, today) {
    if (!group) return false;
    if (group.status === 'RECRUITING') return true;
    if (group.status !== 'ACTIVE' || !group.startDate) return false;
    return toDateStr(today) <= dateOnly(group.startDate);
}

/*
 * 날짜 비교는 `YYYY-MM-DD` 문자열끼리 한다. Date 로 파싱하면 `2026-08-01` 은 UTC 자정으로
 * 읽혀 한국 시간대에서 하루가 밀린다(발부일 주석과 같은 함정이다).
 */
function dateOnly(value) {
    return String(value).split('T')[0];
}

function toDateStr(date) {
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${date.getFullYear()}-${month}-${day}`;
}

function memberCount(group) {
    return group.memberCount ?? group.members?.length ?? 0;
}
