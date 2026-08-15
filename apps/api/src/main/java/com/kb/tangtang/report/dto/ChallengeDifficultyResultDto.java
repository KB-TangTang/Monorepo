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
public class ChallengeDifficultyResultDto {

    private String difficultyName;
    private int attempts;
    private int successDays;
    private BigDecimal successRate;
}
