package com.kb.tangtang.config;

import lombok.extern.log4j.Log4j2;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 로컬 DB 세팅 확인용 테스트.
 *
 * 실제 MySQL 연결이 필요하므로 기본은 비활성화 상태다.
 * 본인 로컬 세팅을 확인할 때만 @Disabled 를 잠시 주석 처리하고 실행할 것.
 * (활성화된 채로 커밋하면 다른 팀원의 빌드가 깨진다)
 *
 * 사전 준비: src/main/resources/application-local.properties 작성
 */
@Disabled("로컬 DB 연결 확인이 필요할 때만 임시로 해제")
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {RootConfig.class})
@Log4j2
class DataSourceConnectionTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private SqlSessionFactory sqlSessionFactory;

    @Test
    @DisplayName("DataSource 로 커넥션을 얻는다")
    void dataSource() throws SQLException {
        try (Connection con = dataSource.getConnection()) {
            assertNotNull(con);
            log.info("DataSource 연결 성공: {}", con);
        }
    }

    @Test
    @DisplayName("SqlSessionFactory 로 세션을 연다")
    void sqlSessionFactory() throws SQLException {
        try (SqlSession session = sqlSessionFactory.openSession();
             Connection con = session.getConnection()) {
            assertNotNull(con);
            log.info("MyBatis 세션 연결 성공: {}", session);
        }
    }
}
