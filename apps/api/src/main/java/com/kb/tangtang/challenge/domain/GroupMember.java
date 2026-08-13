package com.kb.tangtang.challenge.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * {@code tbl_group_member} 한 행.
 *
 * {@code nickname} 은 테이블 컬럼이 아니라 {@code tbl_user} 조인 결과다.
 * 표시용 이니셜은 서버가 만들지 않는다 — 프론트가 닉네임에서 파생한다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupMember {

    private Long groupId;
    private Long userId;
    private Integer livesCount;

    /** SURVIVED / ELIMINATED. 종료 다음 날 확정 배치가 기록한다. NULL = 미확정. */
    private String finalOutcome;
    private Integer finalRank;
    private BigDecimal finalChargeAmount;

    /** 조인 파생 — tbl_user.nickname. NULL 허용 컬럼이라 값이 없을 수 있다. */
    private String nickname;
}
