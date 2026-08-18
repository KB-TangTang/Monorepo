/*
 * 소비 재판 화면용 변환.
 *
 * 서버 계약(`challenge/dto/GroupTrialDetailDto`)과 화면이 쓰는 어휘가 다르다.
 * 변환을 화면 안에 두면 목데이터에서만 맞는 매핑이 다시 생긴다.
 * `api/groupChallenge.js` 의 `toTrialDetailViewModel` 과 같은 이유로 여기 한 곳에 모은다.
 */

/**
 * `2026-08-05T22:00:00` → `오늘 22:00` · `내일 02:00` · `8월 6일 02:00`.
 *
 * 변론 마감은 기소 시각 + 6시간이라 대개 오늘 안이지만 자정을 넘길 수 있다.
 * 「오늘」로 고정해 두면 밤에 기소된 사람에게 지난 시각을 마감으로 보여준다.
 */
export function formatDeadlineLabel(isoDateTime) {
    if (!isoDateTime) return '';
    const at = new Date(isoDateTime);
    if (Number.isNaN(at.getTime())) return '';

    const time = `${String(at.getHours()).padStart(2, '0')}:${String(at.getMinutes()).padStart(2, '0')}`;
    const midnight = (d) => new Date(d.getFullYear(), d.getMonth(), d.getDate()).getTime();
    const days = Math.round((midnight(at) - midnight(new Date())) / 86400000);

    if (days === 0) return `오늘 ${time}`;
    if (days === 1) return `내일 ${time}`;
    return `${at.getMonth() + 1}월 ${at.getDate()}일 ${time}`;
}

/** `2026-08-05T20:14:00` → `20:14`. 타임라인 칸은 시각만 쓴다. 값이 없으면 null 이다. */
function formatTime(isoDateTime) {
    if (!isoDateTime) return null;
    const at = new Date(isoDateTime);
    if (Number.isNaN(at.getTime())) return null;
    return `${String(at.getHours()).padStart(2, '0')}:${String(at.getMinutes()).padStart(2, '0')}`;
}

/**
 * 서버 기소 상태 → 진행 현황 화면의 4단계.
 *
 * **`DEFENSE_SUBMITTED` 는 만들지 않는다.** 변론을 내면 서버가 그 자리에서
 * `DEFENSE_WAIT` → `VOTING` 으로 넘기므로(`DefenseService`) 「변론은 냈고 투표는 아직」이라는
 * 중간 상태가 실제로는 존재하지 않는다. 타임라인의 두 번째 칸은 `VOTING` 부터 완료로 그려진다.
 */
const PROGRESS_STATUS = {
    DEFENSE_WAIT: 'INDICTED',
    VOTING: 'VOTING',
    GUILTY: 'VERDICT_DONE',
    INNOCENT: 'VERDICT_DONE',
};

/**
 * 재판 상세 → 진행 현황 화면(`TrialProgressView`) 이 쓰는 모양.
 *
 * 입력은 `toTrialDetailViewModel` 을 이미 지난 값이다(`caseNumber` · `evalLabel` 이 붙어 있다).
 * 여기서는 화면이 추가로 쓰는 `status` · `steps` · `vote` 만 얹는다.
 */
export function toTrialProgress(detail) {
    if (!detail) return null;

    const status = PROGRESS_STATUS[detail.status];
    /*
     * `tbl_indictment.status` 는 CHECK 제약(ck_ind_status)으로 위 4개만 허용한다.
     * 벗어난 값이 오면 화면이 단계를 지어내는 대신 null 로 돌려보내고 호출부가 되돌아간다.
     * 모르는 상태를 「기소 접수」로 그리면 이미 끝난 재판에 변론을 쓰라고 안내하게 된다.
     */
    if (!status) return null;

    const totalVoters = detail.totalVoters ?? 0;
    const votedCount = detail.voteCount ?? 0;

    return {
        ...detail,
        status,
        /** 서버가 준 원래 상태. 유죄·무죄를 구분해야 하는 곳이 쓴다. */
        serverStatus: detail.status,
        steps: {
            indictedAt: formatTime(detail.createdAt),
            /* 혐의 인정으로 끝난 재판은 변론이 없다. 타임라인이 `--:--` 로 그린다. */
            defenseSubmittedAt: formatTime(detail.defense?.createdAt),
            voteDeadline: formatTime(detail.voteDeadline),
            /* 서버에 판결 확정 시각이 없다. 화면은 투표 마감 시각으로 대신 그린다. */
            verdictAt: null,
        },
        voteDeadlineLabel: formatDeadlineLabel(detail.voteDeadline),
        vote: {
            votedCount,
            totalVoters,
            remainingVoters: Math.max(0, totalVoters - votedCount),
        },
    };
}
