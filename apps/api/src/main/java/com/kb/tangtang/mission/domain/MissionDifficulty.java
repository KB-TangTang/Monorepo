package com.kb.tangtang.mission.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MissionDifficulty {
    private Long id;
    private String difficultyName;
    private BigDecimal minReductionRate;
    private BigDecimal maxReductionRate;
}
