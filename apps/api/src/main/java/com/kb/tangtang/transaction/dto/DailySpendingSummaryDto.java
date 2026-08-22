package com.kb.tangtang.transaction.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 홈 「오늘 쓴 돈」 카드 전용 요약. 필드명을 프론트 HomeSpendingCard 의 props 와 1:1 로 맞춘다.
 * 집계 규칙은 장부(월별 거래내역)와 같다 — 카드에 「거래내역」 입구가 붙어 있어 한 탭 거리에서 대조된다.
 */
@Getter
@Builder
public class DailySpendingSummaryDto {

    /** 서버가 판단한 "오늘"(KST, yyyy-MM-dd). 자정 넘김·타임존 사고를 프론트에서 확인할 유일한 단서다. */
    private String date;
    /** 오늘 순소비. 환불이 소비보다 크면 음수다. 데이터가 없어도 null 이 아니라 0. */
    private BigDecimal todayAmount;
    /** 이번 달(1일~말일) 순소비. 장부 요약의 totalSpent 와 같은 값이다. 데이터가 없어도 0. */
    private BigDecimal monthAmount;
    /**
     * 어제 대비 증감률(%). 음수면 덜 쓴 것이다.
     * 어제 순지출이 0 이하면 비율을 낼 수 없어 null 이다 — 프론트가 null 을 「문구 생략」으로 읽는다.
     */
    private BigDecimal changeRate;
}
