package com.kb.tangtang.mission.domain;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MissionRankingRow {
    private long userId;
    private String nickname;
    private String profileImageKey;
    private int totalScore;
    private int rank;
}
