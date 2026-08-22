package com.kb.tangtang.mission.service;

import com.kb.tangtang.config.RootConfig;
import com.kb.tangtang.mission.dto.MissionAnalysisSnapshotDto;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 로컬 MySQL에서 스냅샷 INSERT와 재사용을 확인하는 통합 테스트.
 * 테스트 트랜잭션을 롤백하므로 생성한 스냅샷은 DB에 남지 않는다.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = RootConfig.class)
@Transactional
@Rollback
@Disabled("로컬 MySQL 스냅샷 저장 검증이 필요할 때만 임시로 해제")
class MissionAnalysisSnapshotIntegrationTest {

    private static final long USER_ID = 1L;

    @Autowired
    private MissionAnalysisSnapshotService missionAnalysisSnapshotService;

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("실제 DB에 스냅샷을 저장하고 두 번째 호출에서는 같은 주기를 재사용한다")
    void insertsAndReusesSnapshot() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        int beforeCount = countSnapshots(jdbcTemplate);

        MissionAnalysisSnapshotDto first =
                missionAnalysisSnapshotService.getOrCreateSnapshot(USER_ID);
        MissionAnalysisSnapshotDto second =
                missionAnalysisSnapshotService.getOrCreateSnapshot(USER_ID);

        assertTrue(first.isAvailable());
        assertEquals(3, first.getItems().size());
        assertEquals(first.getCycleStartDate(), second.getCycleStartDate());
        assertEquals(first.getItems().size(), second.getItems().size());
        assertEquals(beforeCount + first.getItems().size(), countSnapshots(jdbcTemplate));
    }

    private int countSnapshots(JdbcTemplate jdbcTemplate) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tbl_user_mission_analysis WHERE user_id = ?",
                Integer.class,
                USER_ID);
        return count == null ? 0 : count;
    }
}
