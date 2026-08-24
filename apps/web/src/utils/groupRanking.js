/*
 * 명예 법정(생존자 랭킹) 화면용 변환 (이슈 #173).
 *
 * 서버 계약(`challenge/dto/GroupRankingDto`)과 화면이 쓰는 어휘가 다르다.
 * `api/groupChallenge.js` 는 `http`·목데이터 스위치를 import 해서 node:test 로 부를 수 없다 —
 * 순수 변환만 여기로 모아 테스트가 직접 검증한다 (`utils/groupTrial.js` 와 같은 이유).
 */

/** `2026-08-05` → `8월 5일`. 화면은 「8월 5일 결산 반영」으로 읽는다. 값이 없으면 빈 문자열. */
export function formatMonthDay(isoDate) {
    if (!isoDate) return '';
    const [, month, day] = isoDate.split('-');
    return `${Number(month)}월 ${Number(day)}일`;
}

/**
 * 랭킹 응답 → 명예 법정 화면(`GroupHonorCourtView`)이 쓰는 모양.
 *
 * 아바타 색·이니셜은 만들지 않는다 — `UserAvatar` 가 닉네임에서 파생한다(팀 관례).
 */
export function toRankingViewModel(dto) {
    const daily = dto.evalType === 'DAILY';
    const closed = dto.status === 'CLOSED';
    const rankings = (dto.members ?? []).map((m) =>
        daily ? toDailyRow(m, dto.maxLives, closed) : toPeriodRow(m, dto.limitAmount),
    );
    return {
        evalType: dto.evalType,
        status: dto.status,
        limitAmount: dto.limitAmount,
        memo: dto.memo,
        lastSettlementDate: formatMonthDay(dto.lastSettlementDate),
        /* 공동 순위면 3위가 넷일 수 있다 — 시상대는 세 칸이라 앞의 셋만 올린다. */
        podium: rankings.filter((r) => r.rank <= 3).slice(0, 3),
        rankings,
    };
}

function baseRow(member) {
    return {
        rank: member.rank,
        userId: member.userId,
        nickname: member.nickname,
        /* 이미지가 없으면 null — `UserAvatar` 가 이니셜 아바타로 대체한다. */
        profileImage: member.profileImageUrl ?? null,
        isMe: member.mine === true,
    };
}

function toDailyRow(member, maxLives, closed) {
    return {
        ...baseRow(member),
        livesCount: member.livesCount ?? 0,
        maxLives,
        ...lifeStatus(member, maxLives, closed),
    };
}

/**
 * 목숨 상태 배지. **「탈락」은 확정값으로만 그린다** — 진행 중의 목숨 0 은 그날 밤 결산·재판
 * 결과에 따라 살아날 수 있는 「탈락 위기」다 (`GroupRankingDto` 머리말과 같은 정책).
 */
function lifeStatus(member, maxLives, closed) {
    if (closed && member.finalOutcome === 'ELIMINATED') {
        return { isEliminated: true, statusType: 'eliminated', statusLabel: '탈락' };
    }
    const lives = member.livesCount ?? 0;
    if (lives === 0) {
        return { isEliminated: false, statusType: 'danger', statusLabel: '탈락 위기' };
    }
    /* 남은 목숨이 최대치의 절반 미만이면 위험. 경계는 「절반 이상 = 생존」이다. */
    return {
        isEliminated: false,
        statusType: lives * 2 < maxLives ? 'danger' : 'alive',
        statusLabel: String(lives),
    };
}

function toPeriodRow(member, limitAmount) {
    const amount = Number(member.totalConsumption ?? 0);
    return {
        ...baseRow(member),
        currentAmount: amount,
        usagePercent: usagePercent(amount, limitAmount),
    };
}

/**
 * 사용률. 100 을 넘을 수 있다(초과 중). 한도 0원(무지출 챌린지)은 나눌 수 없다 —
 * 한 푼이라도 쓰면 100 으로 본다 (상세 화면의 `usagePercent` 서버 규칙과 동일).
 */
function usagePercent(amount, limitAmount) {
    if (!limitAmount || limitAmount <= 0) {
        return amount > 0 ? 100 : 0;
    }
    return Math.round((amount / limitAmount) * 100);
}
