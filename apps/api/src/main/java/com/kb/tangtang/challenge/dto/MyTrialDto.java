package com.kb.tangtang.challenge.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 홈 「오늘의 할 일」 한 줄 (이슈 #169).
 *
 * <p>변론 대기와 투표 대기를 <b>한 배열로</b> 내려보낸다. 화면이 마감 임박순 하나로 섞어 보여주고
 * 필터 칩으로만 나누기 때문이다({@code GroupTodoSheet.vue}). 두 배열로 나눠 주면 프론트가
 * 다시 합쳐 정렬해야 하는데, 그 정렬 기준이 서버와 어긋나면 시트와 카드의 순서가 달라진다.
 *
 * <p><b>제목·「3/5 투표」 같은 표시 문구는 담지 않는다.</b> 문구를 서버가 만들면 디자인을 고칠 때마다
 * 백엔드를 배포해야 한다. 서버는 재료만 주고 {@code GroupTodoItem.vue} 가 조립한다.
 */
@Getter
@Builder
public class MyTrialDto {

    /** 프론트의 목록 key 이자 라우팅 파라미터. */
    private Long indictmentId;

    /**
     * {@code accuse}(내가 변론) 또는 {@code vote}(내가 투표).
     *
     * <p><b>소문자 고정이다.</b> {@code GroupTodoItem.vue:14} 와 {@code GroupTodoSheet.vue:31} 이
     * 소문자 리터럴로 비교한다. 대문자로 바꾸면 아이콘·버튼·필터가 전부 투표 쪽으로 넘어간다.
     */
    private String type;

    /** 라우팅 파라미터({@code defenseViolation} · {@code voteVerdict})의 {@code id}. */
    private Long challengeId;

    private String challengeName;

    /** 피고 닉네임. {@code vote} 일 때만 채워진다 — {@code accuse} 의 피고는 보는 사람 본인이다. */
    private String defendantNickname;

    /** 한도 초과액. {@code accuse} 일 때만 채워진다. 화면의 「청구 N원」. */
    private BigDecimal amount;

    /** {@code vote} 일 때만. 지금까지 들어온 표. */
    private Integer voteCount;

    /** {@code vote} 일 때만. 투표할 수 있는 사람 수(참여자 - 피고 1명). */
    private Integer totalVoters;

    /**
     * 마감 시각(ISO-8601 절대시각).
     *
     * <p>남은 시간(분)이 아니라 절대시각을 준다. 분으로 주면 화면을 열어 둔 채 몇 시간이 지났을 때
     * 카운트다운이 응답을 받은 시점 기준으로 굳는다. 절대시각이면 브라우저가 매초 다시 계산한다.
     *
     * <p>저장된 값이 아니라 {@code created_at + challenge.trial.*} 계산값이다 —
     * {@code tbl_indictment} 에 마감 컬럼이 없다({@code schema.sql} 의 {@code [0803]} 주석).
     */
    private LocalDateTime deadline;
}
