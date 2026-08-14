package com.kb.tangtang.mission.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MissionPoolItem {
    private Long id;
    private String missionTitle;
    private String missionContent;
}
