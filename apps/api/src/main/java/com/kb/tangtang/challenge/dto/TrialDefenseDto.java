package com.kb.tangtang.challenge.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 제출된 변론 (이슈 #170).
 *
 * <p>{@link GroupTrialDetailDto#getDefense()} 가 NULL 이면 아직 변론이 없다는 뜻이다.
 * 빈 객체로 채우지 않는다 — 화면이 "변론 대기" 와 "내용 없는 변론" 을 구분할 수 없게 된다.
 */
@Getter
@Builder
public class TrialDefenseDto {

    private String content;

    /** 피고가 신고한 실제 개인 부담금(건별 입력의 합계). */
    private BigDecimal actualBurdenAmount;

    /** 무죄 시 소비액에서 빠질 금액. 서버가 {@code 소비액 - 부담금} 으로 계산해 저장한 값이다. */
    private BigDecimal deductionAmount;

    /** 증빙 이미지 URL. 최대 3장, 없으면 빈 배열이다. */
    private List<String> imageUrls;

    private LocalDateTime createdAt;
}
