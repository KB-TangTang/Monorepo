package com.kb.tangtang.fixedexpense;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FixedExpenseCandidateSchemaContractTest {

    @Test
    void confirmationMigrationKeepsSchemaSqlUntouchedAndDoesNotBackfill() throws IOException {
        Path root = repositoryRoot();
        String schema = Files.readString(root.resolve("db/schema.sql"));
        String candidateTable = schema.substring(
                schema.indexOf("CREATE TABLE tbl_fixed_expense_candidate"),
                schema.indexOf("CREATE TABLE tbl_transaction"));
        String migration = Files.readString(root.resolve(
                "db/migration/20260814_add_fixed_expense_candidate_confirmation.sql"));

        assertFalse(candidateTable.contains("confirmed_at"));
        assertTrue(migration.contains("신규 설치 순서"));
        assertTrue(migration.contains("기존 설치 순서"));
        assertTrue(migration.contains("ADD COLUMN confirmed_at DATETIME NULL"));
        assertTrue(migration.contains("idx_fec_user_candidate_lookup"));
        assertTrue(migration.contains("(user_id, status, is_excluded, confirmed_at)"));
        assertTrue(migration.contains("backfill하지 않는다"));
        assertFalse(migration.contains("UPDATE tbl_fixed_expense_candidate"));
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
