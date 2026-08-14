package com.kb.tangtang.mission.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class MissionVerdictAcknowledgeDto {

    private Long assignmentId;
    private LocalDateTime resultCheckedAt;
}
