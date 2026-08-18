package com.kb.tangtang.challenge.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * {@code tbl_indictment} 한 행. 한도를 넘긴 참여자를 재판에 세우는 기소다.
 *
 * <p>평가 배치(#168)가 만드는 기소는 항상 {@link IndictmentStatus#DEFENSE_WAIT} 이고
 * {@code result} · {@code verdictMethod} · {@code aiVerdictReason} 은 전부 NULL 이다.
 * 그 셋은 판결이 확정될 때(#171 · #172) 채워진다.
 *
 * <p>{@code resultId} 에 UNIQUE 가 걸려 있어(uk_ind_result) 일별 결과 1행당 기소는 최대 1건이다.
 * 배치가 같은 위반을 5분마다 다시 기소하는 것을 DB 가 막는다.
 * ({@code db/migration/20260815_add_indictment_result_unique.sql})
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Indictment {

    @Setter
    private Long id;

    /**
     * 기소 대상 일별 결과({@code tbl_group_challenge_daily_result.id}).
     *
     * <p>일일평가는 위반한 그 날의 행, 기간평가는 {@code end_date} 행을 가리킨다.
     * 기간평가에서 이 행의 날짜는 "위반 날짜" 가 아니다 — 기간 전체가 대상이므로
     * 변론 화면은 이 날짜가 아니라 그룹의 {@code start_date ~ end_date} 로 거래를 조회한다.
     */
    private Long resultId;

    private Long groupId;

    /** 피기소자. */
    private Long userId;

    /** {@link IndictmentStatus} 의 이름. */
    private String status;

    /** 기소 사유. AI 판결 사유({@code aiVerdictReason})와 다른 값이다. */
    private String message;

    /** 판결 요약 플래그. 1=유죄 / 0=무죄 / NULL=미확정. {@code status} 와 짝이 맞아야 한다. */
    private Boolean result;

    /** {@link VerdictMethod} 의 이름. 확정 시 기록된다. */
    private String verdictMethod;

    /** 판사 탕이의 판결 사유 원문. {@code verdictMethod=AI_JUDGMENT} 일 때만 값을 가진다. */
    private String aiVerdictReason;

    /**
     * 조인 파생 — 기소 대상 일별 결과의 {@code challenge_date}.
     *
     * <p>화면·알림은 기소가 만들어진 시각({@code created_at})이 아니라 <b>위반한 날짜</b>를 보여야 한다.
     * 심야 거래는 다음 날 배치에서 잡히므로 둘이 하루 어긋난다.
     */
    private LocalDate challengeDate;
}
