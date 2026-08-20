package com.kb.tangtang.challenge.service;

import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.config.RootConfig;
import org.apache.ibatis.annotations.Mapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.mybatis.spring.mapper.ClassPathMapperScanner;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * 마지막 한 자리에 두 명이 <b>동시에</b> 참여할 때 한 명만 들어가는지 본다 (이슈 #354).
 *
 * <p><b>단위 테스트로는 이걸 증명할 수 없다.</b> {@code ChallengeGroupServiceTest} 의 페이크는
 * 잠금이 없는 자바 컬렉션이라, 서비스가 {@code FOR UPDATE} 를 부르는지까지만 확인해 준다.
 * 「진짜로 직렬화되는가」는 InnoDB 가 하는 일이라 실제 MySQL 이 있어야 한다.
 *
 * <p><b>{@code @Transactional} 을 붙이지 않는다.</b> 붙이면 두 스레드가 테스트 트랜잭션을
 * 공유하지 못해 경합 자체가 재현되지 않고, 롤백도 스레드 밖에서는 안 먹는다.
 * 그래서 뒷정리를 {@link #cleanUp()} 이 직접 한다 — 초대 코드로 그룹을 지우면
 * {@code tbl_group_member} 는 FK CASCADE 로 함께 사라진다.
 *
 * <p>돌리는 법: {@code @Disabled} 를 잠시 지우고
 * {@code ./gradlew :apps:api:test --tests '*ChallengeGroupJoinRaceIntegrationTest'}.
 * 로컬 MySQL 에 {@code status='ACTIVE'} 인 사용자가 3명 이상 있어야 한다(없으면 스킵된다).
 * Redis 가 없어도 된다 — 채팅 캐시 갱신 실패는 서비스가 로그만 남기고 넘어간다.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {RootConfig.class, ChallengeGroupJoinRaceIntegrationTest.DropTestOnlyBeans.class})
@Disabled("실 MySQL 이 필요한 동시성 검증. 정원 경합 방어를 손볼 때만 임시로 해제한다")
class ChallengeGroupJoinRaceIntegrationTest {

    /**
     * {@code RootConfig} 가 {@code com.kb.tangtang} 전체를 스캔하는데, 그 범위에는
     * <b>테스트 클래스패스</b>도 들어간다. 다른 테스트가 자기 컨텍스트용으로 선언해 둔
     * {@code @Configuration static class Ctx}(예: {@code DefenseWriterTest.Ctx} ·
     * {@code ProfileImageWriterTest.Ctx})가 함께 스캔돼 목(mock) 빈이 끼어든다.
     * 그대로 두면 {@code PlatformTransactionManager} 가 둘(진짜 {@code transactionManager} +
     * 목 {@code tx})이 되어 컨텍스트가 {@code NoUniqueBeanDefinitionException} 으로 죽는다.
     *
     * <p>목이 끼어드는 것만이 문제가 아니다. 테스트 설정의 {@code @Bean indictmentMapper()} 는
     * <b>이름이 같아서</b> {@code @MapperScan} 이 등록해 둔 진짜 매퍼 정의를 덮어쓴다.
     * 그래서 지운 뒤에 매퍼 스캔을 한 번 더 돌려 진짜 정의를 되돌려 놓는다.
     *
     * <p>남의 테스트 파일을 고치는 대신 <b>이 컨텍스트에서만</b> 정리한다.
     * {@code @Configuration} 을 일부러 붙이지 않았다 — 붙이면 이 클래스 자신이
     * 같은 방식으로 남의 컨텍스트를 오염시킨다.
     */
    static class DropTestOnlyBeans {

        @Bean
        static BeanFactoryPostProcessor dropTestOnlyBeans() {
            return beanFactory -> {
                if (!(beanFactory instanceof BeanDefinitionRegistry registry)) {
                    return;
                }
                // 1차: 테스트 클래스 안에 중첩된 설정 클래스를 찾는다 (예: ...DefenseWriterTest$Ctx)
                Set<String> testConfigs = new LinkedHashSet<>();
                for (String name : registry.getBeanDefinitionNames()) {
                    String className = registry.getBeanDefinition(name).getBeanClassName();
                    if (className != null && className.contains("Test$")) {
                        testConfigs.add(name);
                    }
                }
                // 2차: 그 설정 클래스의 @Bean 메서드가 만든 빈까지 함께 지운다.
                //      설정 클래스를 먼저 지우면 factoryBeanName 을 잃은 고아 정의가 남는다.
                for (String name : registry.getBeanDefinitionNames()) {
                    BeanDefinition definition = registry.getBeanDefinition(name);
                    if (testConfigs.contains(definition.getFactoryBeanName())) {
                        registry.removeBeanDefinition(name);
                    }
                }
                testConfigs.forEach(registry::removeBeanDefinition);

                // 3차: 덮어써진 진짜 매퍼를 되살린다. RootConfig 의 @MapperScan 과 같은 조건이다.
                ClassPathMapperScanner scanner = new ClassPathMapperScanner(registry);
                scanner.setAnnotationClass(Mapper.class);
                scanner.registerFilters();
                scanner.doScan("com.kb.tangtang");
            };
        }
    }

    /** 이 테스트가 만든 것만 지우기 위한 표식. 알파벳은 InviteCodeGenerator 규칙(0/1/I/L/O 제외)을 따른다. */
    private static final String INVITE_CODE = "RACE9";

    @Autowired
    private ChallengeGroupService service;

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbc;
    private List<Long> userIds;

    @BeforeEach
    void setUp() {
        jdbc = new JdbcTemplate(dataSource);
        userIds = jdbc.queryForList(
                "SELECT id FROM tbl_user WHERE status = 'ACTIVE' ORDER BY id LIMIT 3", Long.class);
        assumeTrue(userIds.size() == 3, "ACTIVE 사용자가 3명 이상 있어야 경합을 만들 수 있다");

        cleanUp();
        LocalDate start = LocalDate.now().plusDays(1);
        // 정원 2명 · 방장 1명 → 남은 자리는 정확히 하나다. ck_cg_members 가 허용하는 최솟값이라
        // 참여자를 미리 5명 채워 넣지 않아도 같은 경계를 만들 수 있다.
        jdbc.update("""
                INSERT INTO tbl_challenge_group
                    (admin_id, group_name, category_id, limit_amount, eval_type,
                     max_members, start_date, end_date, invite_code, status)
                VALUES (?, '정원 경합 검증', NULL, 10000, 'PERIOD', 2, ?, ?, ?, 'RECRUITING')
                """, userIds.get(0), start, start.plusDays(3), INVITE_CODE);
        jdbc.update("""
                INSERT INTO tbl_group_member (group_id, user_id, lives_count)
                VALUES (?, ?, 1)
                """, groupId(), userIds.get(0));
    }

    @AfterEach
    void tearDown() {
        cleanUp();
    }

    @Test
    @DisplayName("마지막 한 자리에 둘이 동시에 들어오면 한 명만 성공하고 나머지는 GROUP_FULL 이다")
    void onlyOneWinsTheLastSeat() throws Exception {
        CyclicBarrier startLine = new CyclicBarrier(2);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        List<Future<String>> futures = new ArrayList<>();
        for (int i = 1; i <= 2; i++) {
            long userId = userIds.get(i);
            futures.add(pool.submit(() -> {
                startLine.await(10, TimeUnit.SECONDS);   // 두 스레드를 최대한 같은 순간에 출발시킨다
                try {
                    service.join(userId, INVITE_CODE);
                    return "OK";
                } catch (BusinessException e) {
                    return e.getCode();
                }
            }));
        }
        pool.shutdown();

        List<String> results = new ArrayList<>();
        for (Future<String> future : futures) {
            // BusinessException 이 아닌 예외(데드락·잠금 타임아웃)는 여기서 그대로 터진다.
            // 밀린 요청은 500 이 아니라 기존과 같은 GROUP_FULL 을 받아야 한다.
            results.add(future.get(30, TimeUnit.SECONDS));
        }

        assertEquals(1, results.stream().filter("OK"::equals).count(),
                "한 명만 들어가야 한다. 둘 다 OK 면 정원이 넘은 것이다: " + results);
        assertEquals(1, results.stream().filter("GROUP_FULL"::equals).count(),
                "밀린 쪽은 새 코드가 아니라 기존 GROUP_FULL 을 받아야 한다: " + results);
        assertEquals(2, memberCount(), "참여자 수가 max_members(2) 를 넘지 않는다");
    }

    private long groupId() {
        return jdbc.queryForObject(
                "SELECT id FROM tbl_challenge_group WHERE invite_code = ?", Long.class, INVITE_CODE);
    }

    private int memberCount() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM tbl_group_member WHERE group_id = ?", Integer.class, groupId());
        return count == null ? 0 : count;
    }

    /** 참여자·기소·투표는 FK CASCADE 가 함께 지운다. 이 코드로 만든 그룹만 지운다. */
    private void cleanUp() {
        jdbc.update("DELETE FROM tbl_challenge_group WHERE invite_code = ?", INVITE_CODE);
    }
}
