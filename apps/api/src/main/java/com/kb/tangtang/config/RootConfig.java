package com.kb.tangtang.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.*;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.bind.annotation.ControllerAdvice;

import javax.sql.DataSource;

/**
 * 루트 컨텍스트 설정.
 * DataSource / MyBatis / 트랜잭션 등 웹 계층을 제외한 모든 빈을 담당한다.
 * Controller · ControllerAdvice 는 ServletConfig 가 담당하므로 여기서는 제외한다.
 *
 * 설정 값은 application.properties(공통) 위에 application-local.properties(개인)가 덮어쓴다.
 * 접속 계정은 반드시 application-local.properties 에 둔다. (커밋 금지)
 */
@Configuration
@EnableTransactionManagement
// Spring @PropertySource 는 뒤에 오는 것이 앞의 값을 덮어쓴다.
// application-local.properties(개인) 를 가장 마지막에 둬야 실제로 우선 적용된다.
// 도커 전용 프로퍼티 파일은 두지 않는다. OS 환경변수(systemEnvironment)는 @PropertySource 로
// 등록한 파일들보다 우선순위가 높고, docker-compose.yml 이 컨테이너에 JDBC_DRIVER/JDBC_URL/
// JDBC_USERNAME/JDBC_PASSWORD 를 직접 주입하므로 도커에서는 그 값이 항상 이긴다.
// 로컬에는 그 환경변수가 없으니 application.properties(driver·url) + application-local.properties
// (username·password) 조합으로 정상 해석된다.
@PropertySource(
        value = {
                "classpath:/application.properties",
                "classpath:/application-local.properties"
        },
        ignoreResourceNotFound = true)
@ComponentScan(
        basePackages = "com.kb.tangtang",
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ANNOTATION,
                classes = {Controller.class, ControllerAdvice.class}))
// 매퍼 인터페이스는 반드시 @Mapper 를 붙일 것 (일반 인터페이스가 매퍼로 등록되는 사고 방지)
@MapperScan(basePackages = "com.kb.tangtang", annotationClass = Mapper.class)
public class RootConfig {

    /**
     * ${...} 치환을 엄격하게 수행한다.
     * 이 빈이 없으면 값을 못 찾았을 때 예외 대신 "${jdbc.url}" 문자열이 그대로 주입되어
     * 원인을 알기 어려운 에러가 난다. 없으면 "Could not resolve placeholder 'jdbc.url'" 로 즉시 실패한다.
     */
    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Value("${jdbc.driver}") String driver;
    @Value("${jdbc.url}") String url;
    @Value("${jdbc.username}") String username;
    @Value("${jdbc.password}") String password;

    @Autowired
    ApplicationContext applicationContext;

    @Bean
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName(driver);
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        return new HikariDataSource(config);
    }

    @Bean
    public SqlSessionFactory sqlSessionFactory() throws Exception {
        SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
        factory.setConfigLocation(applicationContext.getResource("classpath:/mybatis-config.xml"));
        factory.setDataSource(dataSource());
        // 매퍼 XML 위치: src/main/resources/mapper/<모듈>/*.xml
        factory.setMapperLocations(applicationContext.getResources("classpath:/mapper/**/*.xml"));
        return factory.getObject();
    }

    @Bean
    public DataSourceTransactionManager transactionManager() {
        return new DataSourceTransactionManager(dataSource());
    }
}
