package com.kb.tangtang.challenge.service;

import com.kb.tangtang.challenge.domain.Defense;
import com.kb.tangtang.challenge.mapper.DefenseMapper;
import com.kb.tangtang.challenge.mapper.IndictmentMapper;
import com.kb.tangtang.common.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link DefenseWriter} 가 <b>실제로 트랜잭션 프록시를 타는지</b> 본다 (이슈 #318).
 *
 * <p>{@code @Transactional} 애노테이션이 붙어 있는지만 리플렉션으로 확인하는 검사는
 * <b>자기호출(self-invocation) 함정을 못 잡는다</b> — 애노테이션은 그대로 있는데 같은 클래스 안에서
 * {@code this.save(...)} 로 부르면 프록시를 거치지 않아 트랜잭션이 아예 열리지 않는다. 그러면
 * {@code moveToVoting} 이 0행이어도 앞서 넣은 변론·이미지가 되돌아가지 않는다.
 *
 * <p>그래서 애노테이션이 아니라 <b>트랜잭션 매니저가 불렸는지</b>를 단언한다.
 * 트랜잭션 매니저를 mock 으로 두어 <b>DB 없이</b> 돈다.
 */
class DefenseWriterTest {

    private static final long INDICTMENT_ID = 11L;

    private AnnotationConfigApplicationContext context;
    private DefenseWriter writer;
    private PlatformTransactionManager tx;
    private DefenseMapper defenseMapper;
    private IndictmentMapper indictmentMapper;

    @Configuration
    @EnableTransactionManagement
    static class Ctx {

        @Bean
        PlatformTransactionManager tx() {
            return mock(PlatformTransactionManager.class);
        }

        @Bean
        DefenseMapper defenseMapper() {
            return mock(DefenseMapper.class);
        }

        @Bean
        IndictmentMapper indictmentMapper() {
            return mock(IndictmentMapper.class);
        }

        @Bean
        DefenseWriter writer(DefenseMapper defenseMapper, IndictmentMapper indictmentMapper) {
            return new DefenseWriter(defenseMapper, indictmentMapper);
        }
    }

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigApplicationContext(Ctx.class);
        writer = context.getBean(DefenseWriter.class);
        tx = context.getBean(PlatformTransactionManager.class);
        defenseMapper = context.getBean(DefenseMapper.class);
        indictmentMapper = context.getBean(IndictmentMapper.class);

        when(tx.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    private static Defense defense() {
        Defense defense = new Defense();
        defense.setId(5L);
        defense.setIndictmentId(INDICTMENT_ID);
        return defense;
    }

    /**
     * 프록시가 벗겨지면 {@code @Transactional} 이 조용히 무시된다 — 여기서 먼저 빨개진다.
     */
    @Test
    @DisplayName("빈이 AOP 프록시로 감싸져 있다")
    void isWrappedInProxy() {
        assertTrue(AopUtils.isAopProxy(writer),
                "프록시가 아니면 @Transactional 이 붙어 있어도 트랜잭션이 열리지 않는다");
    }

    @Test
    @DisplayName("save 는 트랜잭션을 열고 커밋한다 — 세 문장이 한 트랜잭션에 묶여야 한다")
    void saveOpensAndCommitsTransaction() {
        when(indictmentMapper.moveToVoting(INDICTMENT_ID)).thenReturn(1);

        writer.save(defense(), List.of("defense/11/a.jpg"));

        verify(tx).getTransaction(any());
        verify(tx).commit(any());
        verify(tx, never()).rollback(any());
        verify(defenseMapper).insertDefense(any());
        verify(defenseMapper).insertDefenseImages(5L, List.of("defense/11/a.jpg"));
    }

    /**
     * 0행이면 변론 INSERT 까지 되돌아가야 한다. 프록시가 없으면 rollback 이 아예 불리지 않는다.
     */
    @Test
    @DisplayName("상태 전이가 0행이면 롤백한다")
    void rollsBackWhenStatusAlreadyMoved() {
        when(indictmentMapper.moveToVoting(INDICTMENT_ID)).thenReturn(0);

        BusinessException e = assertThrows(BusinessException.class,
                () -> writer.save(defense(), List.of()));

        assertEquals("DEFENSE_NOT_ALLOWED", e.getCode());
        verify(tx).rollback(any());
        verify(tx, never()).commit(any());
    }

    /** 빈 목록으로 {@code foreach} 가 돌면 {@code VALUES} 가 비어 SQL 문법 오류가 난다. */
    @Test
    @DisplayName("증빙 사진이 없으면 이미지 INSERT 를 부르지 않는다")
    void skipsImageInsertWhenNoKeys() {
        when(indictmentMapper.moveToVoting(INDICTMENT_ID)).thenReturn(1);

        writer.save(defense(), List.of());

        verify(defenseMapper, never()).insertDefenseImages(any(), any());
    }
}
