package com.kb.tangtang.challenge.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * {@code tbl_challenge_group} 한 행.
 *
 * {@code categoryName} 은 테이블 컬럼이 아니라 {@code tbl_category} 조인 결과다.
 * {@code categoryId} 가 NULL 이면 총소비 챌린지라 조인 결과도 NULL 이다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeGroup {

    /**
     * INSERT 직후 {@code useGeneratedKeys} 가 채워 넣는 자리다.
     * 채번된 id 를 곧바로 {@code tbl_group_member} INSERT 에 써야 해서 쓰기가 열려 있다.
     */
    @Setter
    private Long id;
    private Long adminId;
    private String groupName;
    private Long categoryId;
    private Integer limitAmount;
    private String evalType;
    private Integer maxMembers;
    private LocalDate startDate;
    private LocalDate endDate;
    private String inviteCode;
    private String status;
    private String memo;

    /** 조인 파생 — tbl_category.category_name. categoryId 가 NULL 이면 NULL. */
    private String categoryName;
}
