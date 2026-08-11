package com.kb.tangtang.mission.mapper;

import com.kb.tangtang.mission.domain.MissionAnalysisSnapshot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MissionAnalysisSnapshotMapper {

    List<MissionAnalysisSnapshot> findPendingSnapshots(@Param("userId") long userId);

    int insertSnapshots(@Param("snapshots") List<MissionAnalysisSnapshot> snapshots);
}
