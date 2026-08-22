package com.kb.tangtang.fixedexpense.mapper;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FixedExpensePaymentReminderMapperXmlTest {

    private static final String RESOURCE = "mapper/fixedexpense/FixedExpensePaymentReminderMapper.xml";

    @Test
    @DisplayName("결제 예정 알림 Mapper XML이 파싱되고 대상·주기 등록 쿼리가 등록된다")
    void parsesMapperXml() throws Exception {
        Configuration configuration = new Configuration();

        try (InputStream inputStream = Resources.getResourceAsStream(RESOURCE)) {
            new XMLMapperBuilder(inputStream, configuration, RESOURCE,
                    configuration.getSqlFragments()).parse();
        }

        String namespace = FixedExpensePaymentReminderMapper.class.getName();
        assertTrue(configuration.hasStatement(namespace + ".findPaymentReminderCandidates"));
        assertTrue(configuration.hasStatement(namespace + ".registerPaymentReminder"));
        assertTrue(configuration.hasStatement(namespace + ".deletePaymentRemindersByUser"));

        String xml;
        try (InputStream inputStream = Resources.getResourceAsStream(RESOURCE)) {
            xml = new String(inputStream.readAllBytes());
        }
        assertFalse(xml.contains("${"));
        assertTrue(xml.contains("status = 'ACTIVE'"));
        assertTrue(xml.contains("confirmed_at IS NOT NULL"));
        assertTrue(xml.contains("is_excluded = 0"));
        assertTrue(xml.contains("next_expected_date IS NOT NULL"));
        assertTrue(xml.contains("next_expected_date &gt;= #{startDate}"));
        assertTrue(xml.contains("next_expected_date &lt;= #{endDate}"));
        assertTrue(xml.contains("INSERT IGNORE INTO tbl_fixed_expense_payment_reminder"));
        assertTrue(xml.contains("DELETE reminder"));
        assertTrue(xml.contains("candidate.user_id = #{userId}"));
    }
}
