package com.kb.tangtang.mission.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MissionRankingEntryDto {
    private int rank;
    private long userId;
    private String nickname;
    private String profileImageUrl;
    private int totalScore;
}
