package com.kb.tangtang.mission.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

/** 미션 배정이 사용할 최신 미소진 분석 스냅샷. */
@Getter
@Builder
public class MissionAnalysisSnapshotDto {

    private boolean available;
    private LocalDate cycleStartDate;
    private List<MissionAnalysisSnapshotItemDto> items;

    public static MissionAnalysisSnapshotDto empty() {
        return MissionAnalysisSnapshotDto.builder()
                .available(false)
                .items(List.of())
                .build();
    }
}
