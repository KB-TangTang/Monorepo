/*
 * 그룹 챌린지 동점 AI 판결 개발용 데이터 공급자.
 * 백엔드가 준비되면 이 모듈의 조회 함수를 groupChallenge API 호출로 교체한다.
 */
export const AI_VERDICT_OUTCOMES = {
    GUILTY: 'GUILTY',
    INNOCENT: 'INNOCENT',
};

let developmentOutcome = AI_VERDICT_OUTCOMES.GUILTY;

const BASE_VERDICT = {
    caseNumber: '2026-재판-0619',
    challengeName: '배달 소비 줄이기',
    evaluationLabel: '일일결산',
    guiltyVotes: 3,
    innocentVotes: 3,
};

const OUTCOME_COPY = {
    [AI_VERDICT_OUTCOMES.GUILTY]: {
        outcome: AI_VERDICT_OUTCOMES.GUILTY,
        label: '유죄',
        englishLabel: 'GUILTY',
        resultTitle: '유죄로\n판결됐어요',
        resultDescription: '소비 기준 위반이 인정됐습니다.',
        reviewMessage: '소비 기준선이 넘어간 기록이 확인됐어요. 이번 건은 유죄로 판결합니다.',
    },
    [AI_VERDICT_OUTCOMES.INNOCENT]: {
        outcome: AI_VERDICT_OUTCOMES.INNOCENT,
        label: '무죄',
        englishLabel: 'NOT GUILTY',
        resultTitle: '무죄로\n판결됐어요',
        resultDescription: '변론 내용과 실제 부담 사정이 인정됐습니다.',
        reviewMessage: '제출된 기록에서 실제 부담 사정이 확인됐어요. 이번 건은 무죄로 판결합니다.',
    },
};

export function getAiVerdict(outcome = developmentOutcome) {
    const resolvedOutcome = OUTCOME_COPY[outcome] ? outcome : developmentOutcome;

    return {
        ...BASE_VERDICT,
        ...OUTCOME_COPY[resolvedOutcome],
    };
}

export function toggleDevelopmentAiVerdict() {
    developmentOutcome =
        developmentOutcome === AI_VERDICT_OUTCOMES.GUILTY
            ? AI_VERDICT_OUTCOMES.INNOCENT
            : AI_VERDICT_OUTCOMES.GUILTY;

    return getAiVerdict();
}
