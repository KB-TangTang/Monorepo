package com.kb.tangtang.fixedexpense.mapper;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FixedExpenseLifecycleMapperXmlTest {

    private static final String RESOURCE = "mapper/fixedexpense/FixedExpenseLifecycleMapper.xml";

    @Test
    @DisplayName("고정지출 수명주기 Mapper XML이 파싱되고 조건부 상태 전이 쿼리가 등록된다")
    void parsesMapperXml() throws Exception {
        Configuration configuration = new Configuration();

        try (InputStream inputStream = Resources.getResourceAsStream(RESOURCE)) {
            new XMLMapperBuilder(inputStream, configuration, RESOURCE,
                    configuration.getSqlFragments()).parse();
        }

        String namespace = FixedExpenseLifecycleMapper.class.getName();
        assertTrue(configuration.hasStatement(namespace + ".findLifecycleCandidates"));
        assertTrue(configuration.hasStatement(namespace + ".findEligiblePayments"));
        assertTrue(configuration.hasStatement(namespace + ".advanceConfirmedCandidate"));
        assertTrue(configuration.hasStatement(namespace + ".moveConfirmedCandidateToBuffer"));
        assertTrue(configuration.hasStatement(namespace + ".reactivateBufferedCandidate"));
        assertTrue(configuration.hasStatement(namespace + ".verifyBufferedCandidate"));
        assertTrue(configuration.hasStatement(namespace + ".linkPaymentToCandidate"));

        String xml;
        try (InputStream inputStream = Resources.getResourceAsStream(RESOURCE)) {
            xml = new String(inputStream.readAllBytes());
        }
        assertFalse(xml.contains("${"));
        assertTrue(xml.contains("status = 'ACTIVE' AND confirmed_at IS NOT NULL"));
        assertTrue(xml.contains("status = 'BUFFER'"));
        assertTrue(xml.contains("status = 'VERIFIED_CANCELLED'"));
        assertTrue(xml.contains("confirmed_at = NULL"));
        assertTrue(xml.contains("relapse_detected_at = #{relapseDetectedAt}"));
        assertTrue(xml.contains("verified_at = #{verifiedAt}"));
        assertTrue(xml.contains("next_expected_date = #{expectedDate}"));
        assertTrue(xml.contains("t.classification = 'CONSUMPTION'"));
        assertTrue(xml.contains("COALESCE(t.cancel_yn, 'N')"));
        assertTrue(xml.contains("t.is_refund = 0"));
        assertTrue(xml.contains("t.is_excluded_from_summary = 0"));
    }
}
