package com.kb.tangtang.fixedexpense;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FixedExpensePaymentReminderSchemaContractTest {

    @Test
    void paymentReminderMigrationKeepsSchemaSqlUntouchedAndUsesCycleUniqueKey() throws IOException {
        Path root = repositoryRoot();
        String schema = Files.readString(root.resolve("db/schema.sql"));
        String migration = Files.readString(root.resolve(
                "db/migration/20260814_add_fixed_expense_payment_reminder.sql"));

        assertFalse(schema.contains("tbl_fixed_expense_payment_reminder"));
        assertTrue(migration.contains("[업그레이드/신규 설치 공통]"));
        assertTrue(migration.contains("CREATE TABLE tbl_fixed_expense_payment_reminder"));
        assertTrue(migration.contains("expected_payment_date"));
        assertTrue(migration.contains("notification_type"));
        assertTrue(migration.contains("uk_fepr_candidate_due_type"));
        assertTrue(migration.contains("fixed_expense_candidate_id, expected_payment_date, notification_type"));
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
