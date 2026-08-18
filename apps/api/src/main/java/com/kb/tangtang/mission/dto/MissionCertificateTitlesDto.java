package com.kb.tangtang.mission.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class MissionCertificateTitlesDto {
    private String yearMonth;
    private List<String> titles;
    private String source;
}
