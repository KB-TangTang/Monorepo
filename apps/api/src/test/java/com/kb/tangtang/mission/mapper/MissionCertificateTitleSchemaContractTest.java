package com.kb.tangtang.mission.mapper;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MissionCertificateTitleSchemaContractTest {

    @Test
    void alignsMonthlyRankingAndCertificateTitleMonthCollations() throws IOException {
        String migration = Files.readString(projectRoot().resolve(
                "db/migration/20260820_align_mission_certificate_title_collation.sql"));

        assertTrue(migration.contains("ALTER TABLE tbl_monthly_ranking"));
        assertTrue(migration.contains("ALTER TABLE tbl_mission_certificate_title"));
        assertTrue(migration.split("COLLATE utf8mb4_general_ci", -1).length - 1 >= 2);
    }

    private Path projectRoot() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null && !Files.exists(current.resolve("db/schema.sql"))) {
            current = current.getParent();
        }
        if (current == null) {
            throw new IllegalStateException("프로젝트 루트를 찾을 수 없습니다.");
        }
        return current;
    }
}
