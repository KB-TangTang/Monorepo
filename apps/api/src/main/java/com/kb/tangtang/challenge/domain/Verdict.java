package com.kb.tangtang.challenge.domain;

/**
 * 배심원이 던진 표 ({@code tbl_vote.verdict}, 이슈 #171).
 * DB CHECK({@code ck_vote_verdict})가 이 두 값만 허용한다.
 *
 * <p>⚠ {@link VerdictMethod} 와 헷갈리기 쉽다. 그쪽은 판결이 <b>어떤 경로로</b> 확정됐는지
 * ({@code VOTE} · {@code NO_VOTE} · {@code AI_JUDGMENT} · {@code CONFESSION})이고,
 * 이쪽은 표 한 장의 <b>내용</b>이다. 한 재판에 둘 다 등장한다 —
 * 표를 모아 개표하면 {@code verdictMethod = VOTE} 로 결과가 확정된다(이슈 #172).
 */
public enum Verdict {

    /** 유죄. */
    GUILTY,

    /** 무죄. */
    INNOCENT;

    /** 화면·요청 본문에서 올라온 문자열이 이 enum 인지. NULL 도 false 다. */
    public static boolean isValid(String name) {
        for (Verdict verdict : values()) {
            if (verdict.name().equals(name)) {
                return true;
            }
        }
        return false;
    }
}
