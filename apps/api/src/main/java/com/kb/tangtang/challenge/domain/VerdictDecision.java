package com.kb.tangtang.challenge.domain;

/**
 * 개표 결과 한 건 (이슈 #172). {@code tbl_indictment} 에 그대로 옮겨 적을 값들이다.
 *
 * <p>{@code status} · {@code result} · {@code verdictMethod} 를 <b>같이</b> 들고 다닌다.
 * DB CHECK({@code ck_ind_result})가 상태와 {@code result} 의 조합만 허용하므로 셋을 따로
 * 넘기면 호출부마다 조합을 다시 맞춰야 하고, 한 곳만 틀려도 UPDATE 단계에서 죽는다.
 *
 * <p>{@link #delegateToAi()} 는 「아직 결론이 없다」는 표시다 — 동률이라 판사 탕이에게
 * 넘겨야 하는 상태. 배치가 {@link #needsAi()} 로 갈라 LLM 을 부른 뒤 {@link #byAi} 로 바꾼다.
 */
public final class VerdictDecision {

    /** {@code ai_verdict_reason VARCHAR(300)}. 넘치면 UPDATE 가 죽는다. */
    public static final int AI_REASON_MAX_LENGTH = 300;

    private static final VerdictDecision AI_DELEGATION =
            new VerdictDecision(null, VerdictMethod.AI_JUDGMENT, null);

    /** 결론이 나지 않았으면(=AI 위임) NULL. */
    private final IndictmentStatus status;

    private final VerdictMethod method;

    /** {@link VerdictMethod#AI_JUDGMENT} 일 때만 값이 있다({@code ck_ind_ai_reason}). */
    private final String aiReason;

    private VerdictDecision(IndictmentStatus status, VerdictMethod method, String aiReason) {
        this.status = status;
        this.method = method;
        this.aiReason = aiReason;
    }

    /** 배심원 다수결 — 유죄. */
    public static VerdictDecision guiltyByVote() {
        return new VerdictDecision(IndictmentStatus.GUILTY, VerdictMethod.VOTE, null);
    }

    /** 배심원 다수결 — 무죄. */
    public static VerdictDecision innocentByVote() {
        return new VerdictDecision(IndictmentStatus.INNOCENT, VerdictMethod.VOTE, null);
    }

    /** 표가 한 장도 없다 — 무죄 추정 (요구사항정의서 6.5). */
    public static VerdictDecision innocentByNoVote() {
        return new VerdictDecision(IndictmentStatus.INNOCENT, VerdictMethod.NO_VOTE, null);
    }

    /** 동률이라 판사 탕이에게 넘긴다. 이 값 그대로는 저장할 수 없다. */
    public static VerdictDecision delegateToAi() {
        return AI_DELEGATION;
    }

    /**
     * 판사 탕이의 판결.
     *
     * <p><b>사유를 여기서 자른다.</b> 클라이언트마다 자르게 두면 실패 대체 문구처럼 클라이언트를
     * 거치지 않는 경로가 검사를 빠져나간다. 자를 곳은 한 군데면 된다.
     */
    public static VerdictDecision byAi(IndictmentStatus status, String reason) {
        if (status != IndictmentStatus.GUILTY && status != IndictmentStatus.INNOCENT) {
            throw new IllegalArgumentException("판결은 GUILTY 또는 INNOCENT 여야 합니다: " + status);
        }
        return new VerdictDecision(status, VerdictMethod.AI_JUDGMENT, truncate(reason));
    }

    private static String truncate(String reason) {
        if (reason == null) {
            return null;
        }
        String trimmed = reason.trim();
        return trimmed.length() <= AI_REASON_MAX_LENGTH
                ? trimmed
                : trimmed.substring(0, AI_REASON_MAX_LENGTH);
    }

    /** 아직 결론이 없다 — LLM 을 부를 차례다. */
    public boolean needsAi() {
        return status == null;
    }

    public IndictmentStatus getStatus() {
        return status;
    }

    public VerdictMethod getMethod() {
        return method;
    }

    public String getAiReason() {
        return aiReason;
    }

    /** {@code tbl_indictment.result}. 유죄 1 · 무죄 0 ({@code ck_ind_result}). */
    public int getResult() {
        return status == IndictmentStatus.GUILTY ? 1 : 0;
    }

    public boolean isGuilty() {
        return status == IndictmentStatus.GUILTY;
    }
}
