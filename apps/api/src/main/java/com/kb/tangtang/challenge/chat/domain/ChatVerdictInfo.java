package com.kb.tangtang.challenge.chat.domain;

/**
 * 판결 확정 카드가 도장과 수치를 그리는 데 필요한 값 (이슈 #304).
 *
 * <p><b>{@link ChatSystemType#VERDICT_CONFIRMED} 메시지에만 있다.</b> 나머지 시스템 메시지와
 * 참여자 메시지(TEXT)는 null 이다. 이 필드가 생기기 전에 Redis 에 쌓인 판결 메시지도 null 로
 * 복원되므로 <b>화면은 값이 없을 때도 그려져야 한다</b>(그때는 예전처럼 중립 도장을 찍는다).
 *
 * <p>문구({@code content})와 따로 두는 이유는 화면이 문자열을 파싱하지 않게 하기 위해서다.
 * {@link ChatSystemType} 을 도입한 것과 같은 이유다 — 문구를 한 글자만 고쳐도 도장이 조용히
 * 뒤집히는 구조를 만들지 않는다.
 *
 * @param outcome      유죄/무죄. 화면이 도장 자산을 고르는 기준이다
 * @param guiltyVotes  유죄 표. <b>투표를 거치지 않은 판결은 null</b> — 혐의 인정(CONFESSION)이 그렇다
 * @param innocentVotes 무죄 표. null 조건은 위와 같다
 * @param livesLost    이 판결로 깎인 목숨. 무죄면 0, 유죄여도 남은 목숨이 없었으면 0 이다
 */
public record ChatVerdictInfo(Outcome outcome,
                              Integer guiltyVotes,
                              Integer innocentVotes,
                              int livesLost) {

    /** 화면 계약은 두 값뿐이다. 기소 상태 전체({@code IndictmentStatus})를 채팅 저장소에 흘리지 않는다. */
    public enum Outcome {
        GUILTY,
        INNOCENT
    }

    /** 투표로 갈린 판결 — 표 분포가 있다. */
    public static ChatVerdictInfo byVote(boolean guilty, int guiltyVotes, int innocentVotes, int livesLost) {
        return new ChatVerdictInfo(guilty ? Outcome.GUILTY : Outcome.INNOCENT,
                guiltyVotes, innocentVotes, livesLost);
    }

    /** 혐의 인정 — 투표가 없다. 표 분포를 0:0 으로 두지 않고 비운다. 0:0 은 「아무도 안 던졌다」와 구별되지 않는다. */
    public static ChatVerdictInfo byConfession(int livesLost) {
        return new ChatVerdictInfo(Outcome.GUILTY, null, null, livesLost);
    }
}
