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
        /**
         * 「판결 보기」가 열 화면 (이슈 #172). 확정 전이면 `null`.
         *
         * 여기서 미리 정해 두는 이유는 위에서 `status` 를 화면 어휘(`VERDICT_DONE`)로 덮기
         * 때문이다. 화면 쪽에서 다시 판단하려면 `serverStatus` 를 원래 자리에 되돌려 넣어야 한다.
         */
        verdictRoute: verdictRouteName(detail),
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

/* ──────────────────────────────────────────────────────────────
 * 재판 현황 — 내 입장 6가지 (이슈 #432)
 * ────────────────────────────────────────────────────────────── */

/** 재판 진행 4단계. 카드의 스테퍼가 이 순서 그대로 그려진다. */
export const TRIAL_STEPS = ['기소', '변론', '투표', '판결'];

/**
 * 진행 중인 재판 하나에 대해 내가 어떤 입장인가 — 6가지.
 *
 * <pre>
 *  피고  상태           조건            stance              할 일
 *  ────────────────────────────────────────────────────────────
 *  나    DEFENSE_WAIT   변론 전         DEFENSE_NEEDED       ✅
 *  나    DEFENSE_WAIT   변론 냄         DEFENSE_SUBMITTED
 *  나    VOTING         —               ON_TRIAL
 *  남    DEFENSE_WAIT   —               DEFENSE_WAITING
 *  남    VOTING         내 표 없음      VOTE_NEEDED          ✅
 *  남    VOTING         내 표 있음      VOTE_DONE
 * </pre>
 *
 * <b>이 판정은 여기서만 한다.</b> 뱃지·문구·CTA 를 화면마다 다시 만들면 조건이 하나씩 빠진다 —
 * 실제로 `GroupDetailTrialCarousel` 이 `defended` 를 안 봐서 변론을 낸 뒤에도
 * 「내 변론이 필요해요」를 띄웠고, 변론 대기 중인 남의 재판에 투표 버튼을 그렸다.
 *
 * 입력의 상태는 `DEFENSE_WAIT` · `VOTING` 둘 중 하나다. 확정된 재판은 서버가
 * 두 조회(`findOpenByGroupId` · `findOpenByUserId`) 모두에서 걸러 내려보내지 않는다.
 */
export function trialStance(trial) {
    if (trial.isMine) {
        if (trial.status !== 'DEFENSE_WAIT') return 'ON_TRIAL';
        return trial.hasDefended ? 'DEFENSE_SUBMITTED' : 'DEFENSE_NEEDED';
    }
    if (trial.status === 'DEFENSE_WAIT') return 'DEFENSE_WAITING';
    return trial.myVote ? 'VOTE_DONE' : 'VOTE_NEEDED';
}

/**
 * 입장별 표시 정보.
 *
 * `actionable` 은 **지금 내가 할 일이 있는가**다. 서버 정렬(`GroupTrialService#findAllMyTrials`)이
 * 같은 기준으로 앞에 세우므로 둘이 어긋나면 「할 일」 뱃지가 붙은 카드가 아래로 밀린다.
 *
 * `action` 은 CTA 가 열 화면이다 — 라우팅은 화면이 안다. 여기서 경로를 만들면
 * 그룹 상세와 지방법원 홈이 서로 다른 되돌아갈 곳을 갖게 된다.
 *
 * `icon` 은 접힌 줄 왼쪽 앵커다. **글자 뱃지를 대신한다.**
 * 한때 「변론 중 / 투표 중」 두 낱말로 6가지 입장을 눌러 담고 `tone`(색)으로만 갈랐는데,
 * 그러면 「내가 변론을 냈다」와 「남이 변론을 쓰는 중」이 화면에서 똑같아진다 — 회색 「변론 중」 둘.
 * 모양이 다르면 색을 못 보는 사람도 갈라 읽는다.
 */
const STANCE = {
    DEFENSE_NEEDED: {
        icon: 'gavel',
        tone: 'danger',
        actionable: true,
        cta: '변론 작성하기',
        action: 'defend',
    },
    DEFENSE_SUBMITTED: {
        icon: 'clock',
        tone: 'muted',
        actionable: false,
        cta: '재판 현황 보기',
        action: 'trial',
    },
    ON_TRIAL: {
        icon: 'scale',
        tone: 'danger',
        actionable: false,
        cta: '재판 현황 보기',
        action: 'trial',
    },
    DEFENSE_WAITING: {
        icon: 'clock',
        tone: 'muted',
        actionable: false,
        cta: '재판 현황 보기',
        action: 'trial',
    },
    VOTE_NEEDED: {
        icon: 'ballot',
        tone: 'primary',
        actionable: true,
        cta: '변론 확인하고 투표하기',
        action: 'vote',
    },
    VOTE_DONE: {
        icon: 'ballot',
        tone: 'muted',
        actionable: false,
        cta: '재판 현황 보기',
        action: 'trial',
    },
};

/**
 * 접힌 줄 첫째 줄 — **내가 무엇을 하는가**. 문장이 아니라 라벨이다.
 *
 * 예전에는 여기가 「{닉}님 재판에 투표해주세요」 같은 완결된 문장이었다. 한국어는 술어가 뒤에
 * 오는데 말줄임은 뒤를 자른다 — 360px 에서 제목에 남는 폭이 152px 뿐이라 11자에서 잘렸고,
 * 「…투표해주세요」와 「…투표했어요」가 **화면에서 완전히 같은 줄**이 됐다.
 * 구분점을 앞으로 당기고, 잘려도 되는 맥락(누구의·어느 그룹)은 둘째 줄로 내린다.
 */
const STANCE_LABEL = {
    DEFENSE_NEEDED: '변론 쓰기',
    DEFENSE_SUBMITTED: '변론 제출함',
    ON_TRIAL: '심판받는 중',
    DEFENSE_WAITING: '변론 기다리는 중',
    VOTE_NEEDED: '투표하기',
    VOTE_DONE: '개표 기다리는 중',
};

/** 접힌 줄 둘째 줄 앞머리 — **누구의 재판인가**. 잘려도 되는 자리다. */
function stanceSubject(trial) {
    return trial.isMine ? '내 재판' : `${trial.nickname}님 재판`;
}

/**
 * 점이 아니라 숫자로 떨어뜨릴 정원. 이보다 많으면 점이 줄을 밀어낸다.
 * 그룹 정원은 보통 5명 이하라 실사용에서 거의 걸리지 않는다.
 */
const VOTE_DOT_LIMIT = 6;

/**
 * 투표 현황을 **모양**으로 그리기 위한 칸 배열. 없으면 `null` 이고 화면이 숫자로 떨어뜨린다.
 *
 * 「나는 던졌는데 남들이 아직」과 「나도 아직」은 사용자가 가장 자주 헷갈리는 두 상태인데,
 * 예전에는 둘 다 「투표 중」 뱃지에 색만 달랐다. 여기서 **내 칸을 맨 앞에 따로 두어**
 * 색을 못 봐도 갈리게 한다.
 *
 * 피고에게는 내 칸을 만들지 않는다 — 자기 재판의 배심원이 아니라서(`canVote` 와 같은 규칙),
 * 칸을 만들면 남의 표를 내 것처럼 그리게 된다.
 */
function buildVoteDots(trial, voting) {
    if (!voting) return null;
    const total = trial.totalVoters ?? 0;
    if (total <= 0 || total > VOTE_DOT_LIMIT) return null;

    const dots = [];
    if (!trial.isMine) dots.push(trial.myVote ? 'mine-done' : 'mine-todo');

    /* 내 표는 `voteCount` 에 이미 들어 있다. 빼지 않으면 내 칸과 남의 칸에 두 번 그려진다 */
    const others = Math.max(0, (trial.voteCount ?? 0) - (dots[0] === 'mine-done' ? 1 : 0));
    /* 남은 칸 수를 미리 잡는다 — 조건에서 `dots.length` 를 읽으면 push 할 때마다 목표가 줄어 절반만 찬다 */
    const rest = total - dots.length;
    for (let i = 0; i < rest; i += 1) {
        dots.push(i < others ? 'done' : 'todo');
    }
    return dots;
}

/**
 * 진행 중인 재판 → 「재판 현황」 아코디언 카드 한 장.
 *
 * 입력은 `toIndictmentViewModel` 을 지난 값이다(`isMine` · `hasDefended` · `deadline`).
 * 그룹 상세 캐러셀과 지방법원 홈이 **같은 함수를 지난 같은 모양**을 그린다.
 *
 * 카테고리·한도는 여기서 채우지 않는다. 화면이 이미 들고 있는 참여 그룹 목록과
 * `groupId` 로 이어 붙이는 편이 응답을 두 번 내려보내는 것보다 싸다.
 */
export function toTrialStatusCard(trial) {
    const stance = trialStance(trial);
    const meta = STANCE[stance];
    const voting = trial.status === 'VOTING';
    const totalVoters = trial.totalVoters ?? 0;
    const voteCount = trial.voteCount ?? 0;

    return {
        ...trial,
        stance,
        ...meta,
        /** 첫째 줄 — 내가 무엇을 하는가. 4~9자라 말줄임에 걸리지 않는다 */
        title: STANCE_LABEL[stance],
        /** 둘째 줄 앞머리 — 누구의 재판인가. 화면이 그룹명과 이어 붙인다 */
        subject: stanceSubject(trial),
        /* 투표는 변론이 끝나야 열린다. 변론 대기 중에 0/5 를 그리면 아무도 안 던진 것처럼 보인다. */
        showVote: voting,
        voteDots: buildVoteDots(trial, voting),
        voteCount,
        totalVoters,
        /* 스테퍼에서 지금 칸. 앞 칸은 지난 것, 뒤 칸은 아직 오지 않은 것으로 그린다. */
        stepIndex: voting ? 2 : 1,
        deadlineLabel: formatDeadlineLabel(trial.deadline),
    };
}

/* ──────────────────────────────────────────────────────────────
 * 판결 확정 (이슈 #172)
 * ────────────────────────────────────────────────────────────── */

/** 개표가 끝난 재판인가. 판결 화면 4개는 전부 이 조건을 먼저 본다. */
export function isVerdictConfirmed(detail) {
    return detail?.status === 'GUILTY' || detail?.status === 'INNOCENT';
}

/**
 * 표가 동률이라 판사 탕이가 판결한 재판인가.
 *
 * **`status` 로는 구분되지 않는다.** 다수결로 난 유죄와 AI 가 낸 유죄는 둘 다 `GUILTY` 다.
 * 서버가 `verdict_method` 를 따로 내려주는 이유가 이것이다.
 */
export function isAiVerdict(detail) {
    return isVerdictConfirmed(detail) && detail.verdictMethod === 'AI_JUDGMENT';
}

/**
 * 확정된 재판이 열어야 할 화면.
 *
 * 진입로가 여러 곳(홈 · 그룹 상세 · 진행 현황 · 투표 화면)이라 분기를 각 화면에 두면
 * 한 곳만 AI 판결을 모르는 상태가 생긴다. 확정 전이면 `null` — 호출부가 진행 현황으로 보낸다.
 */
export function verdictRouteName(detail) {
    if (!isVerdictConfirmed(detail)) return null;
    return isAiVerdict(detail) ? 'groupVoteTie' : 'verdictResult';
}

/** 확정 판결의 유무죄 어휘. `tbl_indictment.status` 가 그대로 화면 어휘가 된다. */
const VERDICT_COPY = {
    GUILTY: {
        label: '유죄',
        englishLabel: 'GUILTY',
        resultTitle: '유죄로\n판결됐어요',
        resultDescription: '소비 기준 위반이 인정됐습니다.',
    },
    INNOCENT: {
        label: '무죄',
        englishLabel: 'NOT GUILTY',
        resultTitle: '무죄로\n판결됐어요',
        resultDescription: '변론 내용과 실제 부담 사정이 인정됐습니다.',
    },
};

/**
 * 판결 방식별 사유 문구.
 *
 * **AI 판결만 서버가 문장을 만든다**(`aiVerdictReason`). 나머지 셋은 방식 자체가 사유라
 * 서버에 문장이 없다 — `ck_ind_ai_reason` 이 `AI_JUDGMENT` 가 아니면 사유 저장을 막는다.
 */
const METHOD_REASON = {
    VOTE: '배심원 투표 결과를 그대로 따랐어요.',
    NO_VOTE: '투표한 배심원이 없어 무죄 추정 원칙에 따라 무죄로 확정했어요.',
    CONFESSION: '피고가 혐의를 인정해 변론 없이 확정됐어요.',
};

/**
 * 재판 상세 → 판결 화면 5개(최종 판결 · 판결 상세 · 동점 안내 · 탕이 판결 · 판결 결과)가 쓰는 모양.
 *
 * 입력은 `toTrialDetailViewModel` 을 이미 지난 값이다. 확정 전이면 `null` 을 돌려준다 —
 * 개표 전에는 `guiltyCount` 가 `null` 이라 「0 : 0 만장일치」로 그려지기 때문이다(이슈 #171).
 */
export function toVerdictScreen(trial) {
    if (!isVerdictConfirmed(trial)) return null;

    const copy = VERDICT_COPY[trial.status];
    const reason =
        trial.verdictMethod === 'AI_JUDGMENT'
            ? (trial.aiVerdictReason ?? '')
            : (METHOD_REASON[trial.verdictMethod] ?? '');

    return {
        ...trial,
        ...copy,
        outcome: trial.status,
        evaluationLabel: trial.evalLabel,
        /* 개표가 끝났으므로 숫자가 온다. `?? 0` 은 판결 방식이 CONFESSION 인 옛 데이터 대비다. */
        guiltyVotes: trial.guiltyCount ?? 0,
        innocentVotes: trial.innocentCount ?? 0,
        /*
         * 확정된 재판만 여기까지 오므로 코멘트는 배열로 확정한다(이슈 #172).
         * 코멘트를 남기지 않은 표는 서버가 이미 걸러 내려주므로 표 수와 개수가 다를 수 있다.
         */
        comments: trial.comments ?? [],
        isAiVerdict: trial.verdictMethod === 'AI_JUDGMENT',
        /*
         * 타이핑 애니메이션이 한 글자씩 읽는 문장이다. 판사 탕이의 사유 원문을 그대로 쓴다 —
         * 고정 문구를 넣으면 어떤 재판에서도 같은 말을 하는 판사가 된다.
         */
        reviewMessage: reason || copy.resultDescription,
        detail: {
            judgmentDate: trial.settlementLabel,
            currentAmount: trial.currentAmount,
            limitAmount: trial.limitAmount,
            /*
             * 변론이 없으면 「없음」이다. 0 으로 채우면 판결문에 「실제 부담금 0원」이라는
             * 하지도 않은 주장이 적힌다 — 무변론 재판이 실제로 존재한다.
             */
            actualCostAmount: trial.defense?.actualBurdenAmount ?? null,
            reason,
        },
    };
}
