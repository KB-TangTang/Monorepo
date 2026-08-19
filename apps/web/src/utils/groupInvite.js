/*
 * 친구 초대(소환장) 화면의 표시값 계산.
 *
 * 화면 컴포넌트에서 떼어 낸 이유는 두 가지다. 하나는 테스트할 수 있게 하려는 것이고,
 * 다른 하나는 **참여 가능 판정 기준을 서버와 한 줄로 맞춰 두기 위해서**다.
 * 서버는 `status` 하나만 본다(ChallengeGroupService#joinBlockReason, 이슈 #152).
 * 여기서 `startDate` 와 오늘을 비교해 따로 판정하면, 상태 전이 배치가 밀렸을 때
 * 화면은 「마감」인데 참여는 되는 어긋남이 생긴다.
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

/** 지금 이 그룹에 초대할 수 있는지. 서버와 같이 `status` 와 정원만 본다 */
export function isInviteOpen(group) {
    if (!group || group.status !== 'RECRUITING') return false;
    return memberCount(group) < (group.maxMembers ?? 6);
}

/**
 * 헤더 뱃지.
 *
 * 초대할 수 있을 때만 마감 시각을 함께 띄운다. 이미 닫힌 방에 마감을 붙여 봐야
 * 「아직 되는 건가?」 하고 헷갈리기만 한다.
 */
export function inviteBadges(group) {
    if (!group) return [];

    if (isInviteOpen(group)) {
        const badges = [{ text: '초대 가능', variant: 'success' }];
        const deadline = formatJoinDeadline(group.startDate);
        if (deadline) badges.push({ text: deadline, variant: 'danger' });
        return badges;
    }

    if (group.status === 'RECRUITING') {
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
export function remainingSeats(group) {
    if (!isInviteOpen(group)) return null;
    return (group.maxMembers ?? 6) - memberCount(group);
}

function memberCount(group) {
    return group.memberCount ?? group.members?.length ?? 0;
}
