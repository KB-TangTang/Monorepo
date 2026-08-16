package com.kb.tangtang.challenge.domain;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 홈 「오늘의 할 일」 한 줄의 원재료 (이슈 #169).
 *
 * <p>변론 대기(내가 피고)와 투표 대기(남이 피고) 두 조회가 같은 모양을 쓴다. 어느 쪽인지는
 * <b>어느 쿼리가 만들었는지</b>로만 갈린다 — 행에 종류 컬럼을 두면 두 쿼리가 각자 상수를
 * SELECT 하게 되고, 그 상수가 DTO 의 값과 어긋나도 컴파일러가 잡아주지 못한다.
 *
 * <p>마감 시각이 없는 것은 {@code tbl_indictment} 에 마감 컬럼이 없기 때문이다
 * ({@code schema.sql} 의 {@code [0803]} 주석). {@code createdAt} 에
 * {@code challenge.trial.*} 시간을 더해 서비스가 계산한다.
 */
@Getter
@Setter
public class TrialTodoRow {

    private Long indictmentId;

    private Long challengeId;

    private String challengeName;

    /** 피고 닉네임. 투표 대기일 때만 의미가 있다 — 변론 대기는 피고가 본인이다. */
    private String defendantNickname;

    /** 한도 초과액. 변론 대기 화면이 「청구 N원」으로 쓴다. */
    private BigDecimal amount;

    /** 지금까지 들어온 표. 투표 대기일 때만. */
    private int voteCount;

    /** 투표할 수 있는 사람 수 = 참여자 - 피고 1명. 투표 대기일 때만. */
    private int totalVoters;

    /** 기소 생성 시각. 마감 계산의 기준점이다. */
    private LocalDateTime createdAt;
}
