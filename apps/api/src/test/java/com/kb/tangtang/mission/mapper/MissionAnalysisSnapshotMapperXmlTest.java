package com.kb.tangtang.mission.mapper;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MissionAnalysisSnapshotMapperXmlTest {

    private static final String RESOURCE = "mapper/mission/MissionAnalysisSnapshotMapper.xml";

    @Test
    @DisplayName("분석 스냅샷 Mapper XML이 파싱되고 조회·저장문이 등록된다")
    void parsesMapperXml() throws Exception {
        Configuration configuration = new Configuration();

        try (InputStream inputStream = Resources.getResourceAsStream(RESOURCE)) {
            new XMLMapperBuilder(inputStream, configuration, RESOURCE,
                    configuration.getSqlFragments()).parse();
        }

        String namespace = MissionAnalysisSnapshotMapper.class.getName();
        assertTrue(configuration.hasStatement(namespace + ".findPendingSnapshots"));
        assertTrue(configuration.hasStatement(namespace + ".insertSnapshots"));
    }
}
