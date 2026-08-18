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

/**
 * 이 재판에 지금 투표할 수 있는가 (이슈 #171).
 *
 * 서버가 같은 조건을 `VoteService` 에서 다시 본다 — 여기는 **화면을 헛되이 열지 않기 위한** 것이지
 * 검증이 아니다. 이 판단을 화면 안에 흩뿌리면 조건이 하나 빠진 화면이 생긴다.
 *
 * **마감 시각도 본다.** 개표 배치(#172)가 없어 마감이 지나도 상태는 `VOTING` 그대로다.
 * 상태만 보면 봉투를 열고 변론서까지 읽은 뒤 제출에서야 `VOTE_NOT_ALLOWED` 로 튕긴다.
 * 서버도 같은 식(`created_at + defense-hours + vote-hours`)으로 계산하므로 판단이 갈리지 않는다.
 */
export function canVote(detail) {
    if (!detail) return false;
    if (detail.status !== 'VOTING') return false;
    /* 피고는 자기 재판의 배심원이 아니다. */
    if (detail.accused?.isMine) return false;
    /* 이미 던졌다. 투표 수정은 지원하지 않는다 — 개표 직전 눈치싸움을 만들지 않기 위함이다. */
    if (detail.myVerdict) return false;
    return !isPast(detail.voteDeadline);
}

/** 마감 시각이 지났는가. 값이 없으면 「모른다」이므로 막지 않는다 — 막는 판단은 서버가 한다. */
function isPast(isoDateTime) {
    if (!isoDateTime) return false;
    const at = new Date(isoDateTime).getTime();
    return !Number.isNaN(at) && at < Date.now();
}

/**
 * 재판 상세 → 투표 화면(`VoteVerdictView`) 이 쓰는 모양.
 *
 * 입력은 `toTrialDetailViewModel` 을 이미 지난 값이다. 화면은 변론서를 법정 문서처럼 그리느라
 * 서버 계약과 다른 어휘(`defendant` · `defenseMessage` · `evidences`)를 쓴다.
 *
 * <b>변론이 없는 `VOTING` 재판이 존재한다.</b> 변론 마감 배치가 변론 없이 상태만 넘기기 때문이다
 * ({@code moveExpiredDefensesToVoting}). 변론이 없다고 투표를 막으면 그 재판은 영원히 끝나지
 * 않는다 — `hasDefense: false` 로 알리고 투표는 그대로 받는다.
 */
export function toVoteScreen(detail) {
    if (!detail) return null;

    const defense = detail.defense ?? null;

    return {
        ...detail,
        defendant: {
            userId: detail.accused?.userId ?? null,
            nickname: detail.accused?.nickname ?? '',
            profileImage: detail.accused?.profileImage ?? null,
        },
        hasDefense: defense !== null,
        defenseMessage: defense?.content ?? '',
        /*
         * 실제 부담금은 「없음」과 「0원」이 다르다. 변론이 없는데 0 으로 채우면 변론서에
         * 「실제 부담금 0원 인정 시 기준 내」라는 없는 주장이 적힌다.
         */
        actualCostAmount: defense?.actualBurdenAmount ?? null,
        defenseDate: defense?.createdAt ?? null,
        /* 증거는 이미지 URL 목록이다. 라벨(「증 제1호」)은 표시 전용이라 서버에 없다. */
        evidences: (defense?.images ?? []).map((url, index) => ({
            id: index + 1,
            label: `증 제${index + 1}호`,
            url,
        })),
        voteDeadlineLabel: formatDeadlineLabel(detail.voteDeadline),
    };
}
