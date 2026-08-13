package com.kb.tangtang.report.mapper;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MonthlyAiAnalysisSchemaContractTest {

    @Test
    void assetSnapshotKeepsUserMonthUniquenessAndMigrationAddsAiColumns() throws IOException {
        Path root = repositoryRoot();
        String schema = Files.readString(root.resolve("db/schema.sql"));
        String migration = Files.readString(root.resolve(
                "db/migration/20260813_add_monthly_report_ai_analysis_to_asset_snapshot.sql"));

        assertTrue(schema.contains("CREATE TABLE tbl_asset_snapshot"));
        assertTrue(schema.contains("UNIQUE KEY uk_as_user_month (user_id, `year_month`)"));
        assertTrue(migration.contains("ALTER TABLE tbl_asset_snapshot"));
        assertTrue(migration.contains("MODIFY COLUMN ai_comment JSON NULL"));
        assertTrue(migration.contains("MODIFY COLUMN compare_comment TEXT NULL"));
        assertTrue(migration.contains("ADD COLUMN ai_analysis_status"));
        assertTrue(migration.contains("신규 설치 순서"));
        assertTrue(migration.contains("롤백은 수동으로 수행한다"));
        assertFalse(migration.contains("CREATE TABLE"));
    }

    private Path repositoryRoot() {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (current != null) {
            if (Files.exists(current.resolve("db/schema.sql"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Repository root could not be located");
    }
}
