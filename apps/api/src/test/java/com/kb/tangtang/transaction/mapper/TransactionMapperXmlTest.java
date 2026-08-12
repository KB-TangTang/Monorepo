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
    }

    @Test
    @DisplayName("update문의 SET절에 category_id·category_source가 있다 — 재동기화 시 카테고리 판정이 유실되지 않아야 한다")
    void updateStatementSetsCategoryIdAndCategorySource() throws Exception {
        Configuration configuration = new Configuration();

        try (InputStream inputStream = Resources.getResourceAsStream(RESOURCE)) {
            new XMLMapperBuilder(inputStream, configuration, RESOURCE,
                    configuration.getSqlFragments()).parse();
        }

        String namespace = TransactionMapper.class.getName();
        MappedStatement updateStatement = configuration.getMappedStatement(namespace + ".update");
        BoundSql boundSql = updateStatement.getBoundSql(Transaction.builder().build());
        String sql = boundSql.getSql().replaceAll("\\s+", " ");

        assertTrue(sql.contains("category_id = ?"), "update문 SET절에 category_id 가 없다: " + sql);
        assertTrue(sql.contains("category_source = ?"), "update문 SET절에 category_source 가 없다: " + sql);
    }
}
