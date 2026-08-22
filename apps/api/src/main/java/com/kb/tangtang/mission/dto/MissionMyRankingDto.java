package com.kb.tangtang.mission.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MissionMyRankingDto {
    private int rank;
    private String nickname;
    private String profileImageUrl;
    private int totalScore;
    private int topPercent;
}
