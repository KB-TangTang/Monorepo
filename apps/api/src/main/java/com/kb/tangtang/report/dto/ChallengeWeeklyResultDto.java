package com.kb.tangtang.report.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ChallengeWeeklyResultDto {

    private int week;
    private int successDays;
    private int totalDays;
    private BigDecimal successRate;
}
