package com.kb.tangtang.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.client.RestTemplate;

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
@EnableScheduling   // NotificationDlqRetryScheduler (NT_01_04)
@EnableAsync        // NotificationRequestedListener 의 @Async
/*
 * 환경별 설정은 한 번에 하나만 로드한다.
 *   로컬 : APP_ENV 없음 → 기본값 local → application-local.properties (git 제외, 개인 시크릿)
 *   도커 : docker-compose 가 APP_ENV=docker 주입 → application-docker.properties
 *
 * 과거에 두 파일을 동시에 나열했다가, 로컬에서도 docker 파일이 로드돼
 * jdbc.driver 가 ${JDBC_DRIVER} 로 덮이면서 컨텍스트 로딩이 실패했다.
 * (실측: @PropertySource 는 뒤에 선언한 파일이 앞을 덮어쓴다)
 *
 * @PropertySource 의 경로에는 플레이스홀더를 쓸 수 있고, 이 시점에는
 * 시스템 프로퍼티·환경변수가 이미 해석 가능하다.
 */
@PropertySource(
        value = {
                "classpath:/application.properties",
                "classpath:/application-${app.env:local}.properties"
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

    /**
     * 프로그래밍 방식 트랜잭션 경계.
     *
     * ⚠ @Transactional 은 **프록시를 통해 들어온 호출에만** 걸린다. 한 클래스 안에서 자기 메서드를
     *   부르는 구조(FinancialSyncService: 외부 API 수집은 트랜잭션 밖, 저장만 트랜잭션 안)에서는
     *   애너테이션이 조용히 무시되고 문장 단위 auto-commit 으로 떨어진다.
     *   그런 구간은 이 템플릿으로 경계를 명시한다.
     */
    @Bean
    public TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);
    }

    /** 외부 API 호출용. 지금은 구글 OAuth 만 쓴다. */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /**
     * @EnableScheduling 전용 스케줄러.
     *
     * ⚠ 이 빈이 없으면 스프링이 **단일 스레드** 스케줄러로 폴백한다. SseHeartbeat.ping(15초)과
     *   NotificationDlqRetryScheduler.retryDue(60초)가 그 한 스레드를 나눠 쓰게 되고,
     *   응답 없는 클라이언트 하나가 SseEmitter.send() 에서 막히면 **모든 사용자의 하트비트**가 멈춘다.
     *   하트비트가 막으려던 바로 그 상황이다.
     */
    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(3);
        scheduler.setThreadNamePrefix("tt-sched-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(10);
        return scheduler;
    }

    /**
     * @EnableAsync 전용 실행기. 빈 이름이 반드시 taskExecutor 여야 @Async 가 집어 간다.
     *
     * ⚠ 이 빈이 없으면 SimpleAsyncTaskExecutor 로 폴백해 **이벤트 하나당 새 스레드**를 무제한 만든다.
     *   큐 용량을 두어 상한을 건다.
     */
    @Bean
    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("tt-async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        return executor;
    }

    /**
     * 기본 ObjectMapper 는 java.time(LocalDateTime 등)을 직렬화하지 못한다
     * (JavaTimeModule 이 없으면 InvalidDefinitionException 으로 500이 난다).
     * 이 빈은 JwtAuthInterceptor · GoogleOAuthClient 가 직접 주입받아 쓰므로,
     * MVC 응답 변환기(Jackson2ObjectMapperBuilder)와 동일하게 JavaTimeModule 을 등록하고
     * 타임스탬프(숫자 배열) 대신 ISO-8601 문자열로 쓰도록 맞춘다.
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return objectMapper;
    }
}
