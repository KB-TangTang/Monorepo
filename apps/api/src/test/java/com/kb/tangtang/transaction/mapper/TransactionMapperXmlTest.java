package com.kb.tangtang.transaction.mapper;

import com.kb.tangtang.transaction.domain.Transaction;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionMapperXmlTest {

    private static final String RESOURCE = "mapper/transaction/TransactionMapper.xml";

    @Test
    @DisplayName("거래 Mapper XML이 파싱되고 update/insert/linkByCorrelation문이 등록된다")
    void parsesMapperXml() throws Exception {
        Configuration configuration = new Configuration();

        try (InputStream inputStream = Resources.getResourceAsStream(RESOURCE)) {
            new XMLMapperBuilder(inputStream, configuration, RESOURCE,
                    configuration.getSqlFragments()).parse();
        }

        String namespace = TransactionMapper.class.getName();
        assertTrue(configuration.hasStatement(namespace + ".update"));
        assertTrue(configuration.hasStatement(namespace + ".insert"));
        assertTrue(configuration.hasStatement(namespace + ".linkByCorrelation"));
        assertTrue(configuration.hasStatement(namespace + ".findByIdAndUser"));
        assertTrue(configuration.hasStatement(namespace + ".updateCategoryByUser"));
        assertTrue(configuration.hasStatement(namespace + ".findDistinctDataMonths"));
        assertTrue(configuration.hasStatement(namespace + ".sumNetConsumption"));
        assertTrue(configuration.hasStatement(namespace + ".sumIncome"));
        assertTrue(configuration.hasStatement(namespace + ".findTransactionRows"));
    }

    @Test
    @DisplayName("findTransactionRows는 startDate/endDate가 없으면 기간 조건 없이 전체를 조회한다(검색 화면용)")
    void findTransactionRowsSkipsDateFilterWhenBothDatesAreNull() throws Exception {
        Configuration configuration = new Configuration();

        try (InputStream inputStream = Resources.getResourceAsStream(RESOURCE)) {
            new XMLMapperBuilder(inputStream, configuration, RESOURCE,
                    configuration.getSqlFragments()).parse();
        }

        String namespace = TransactionMapper.class.getName();
        MappedStatement statement = configuration.getMappedStatement(namespace + ".findTransactionRows");
        BoundSql boundSql = statement.getBoundSql(
                new java.util.HashMap<String, Object>() {{
                    put("userId", 1L);
                    put("startDate", null);
                    put("endDate", null);
                }});
        String sql = boundSql.getSql().replaceAll("\\s+", " ");

        assertFalse(sql.contains("tr_date >="), "startDate/endDate가 null인데 기간 조건이 붙었다: " + sql);
    }

    @Test
    @DisplayName("update문의 SET절에 category_id·category_source가 없다 — 재동기화 때마다 카테고리가 초기화되는 버그(#147) 재발 방지")
    void updateStatementDoesNotTouchCategoryColumns() throws Exception {
        /*
         * 예전에는 이 update(재동기화 upsert) 문의 SET 절에 category_id·category_source 가 있었다.
         * FinancialSyncServiceImpl.upsertTransaction() 이 넘기는 Transaction 객체는 동기화 API 데이터로만
         * 조립돼 categoryId/categorySource 가 항상 null 이라, 재동기화할 때마다 카테고리화 파이프라인이
         * 매긴 값(사용자가 지정한 category_source='USER' 포함)을 통째로 지워버렸다. 카테고리 갱신은
         * updateCategory 문 하나로만 한다 — 이 문에는 다시 넣지 않는다.
         */
        Configuration configuration = new Configuration();

        try (InputStream inputStream = Resources.getResourceAsStream(RESOURCE)) {
            new XMLMapperBuilder(inputStream, configuration, RESOURCE,
                    configuration.getSqlFragments()).parse();
        }

        String namespace = TransactionMapper.class.getName();
        MappedStatement updateStatement = configuration.getMappedStatement(namespace + ".update");
        BoundSql boundSql = updateStatement.getBoundSql(Transaction.builder().build());
        String sql = boundSql.getSql().replaceAll("\\s+", " ");

        assertFalse(sql.contains("category_id = ?"), "update문 SET절에 category_id 가 남아있다: " + sql);
        assertFalse(sql.contains("category_source = ?"), "update문 SET절에 category_source 가 남아있다: " + sql);
    }

    @Test
    @DisplayName("updateCategoryByUser는 user_id까지 WHERE에 넣어 소유권을 DB 레벨에서 강제한다")
    void updateCategoryByUserEnforcesOwnership() throws Exception {
        Configuration configuration = new Configuration();

        try (InputStream inputStream = Resources.getResourceAsStream(RESOURCE)) {
            new XMLMapperBuilder(inputStream, configuration, RESOURCE,
                    configuration.getSqlFragments()).parse();
        }

        String namespace = TransactionMapper.class.getName();
        MappedStatement statement = configuration.getMappedStatement(namespace + ".updateCategoryByUser");
        BoundSql boundSql = statement.getBoundSql(
                new java.util.HashMap<String, Object>() {{
                    put("id", 1L);
                    put("userId", 1L);
                    put("categoryId", 1L);
                }});
        String sql = boundSql.getSql().replaceAll("\\s+", " ");

        assertTrue(sql.contains("WHERE id = ?"), "id 조건이 없다: " + sql);
        assertTrue(sql.contains("AND user_id = ?"), "user_id 조건이 없다(소유권 미검증): " + sql);
    }
}
