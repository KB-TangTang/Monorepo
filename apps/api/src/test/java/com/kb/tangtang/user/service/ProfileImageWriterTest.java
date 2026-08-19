package com.kb.tangtang.user.service;

import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.common.storage.ImageStorage;
import com.kb.tangtang.user.mapper.UserMapper;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ProfileImageWriter} 가 <b>실제로 트랜잭션 프록시를 타는지</b> 본다 (이슈 #318).
 *
 * <p>이유는 {@code DefenseWriterTest} 와 같다 — 애노테이션 유무만 보는 검사는 자기호출
 * 함정을 못 잡는다. 여기서는 특히 {@code deleteAfterCommit} 이 트랜잭션 동기화에 기대므로,
 * 프록시가 벗겨지면 <b>옛 파일이 커밋 전에 지워지는</b> 경로로 조용히 되돌아간다.
 *
 * <p>트랜잭션 매니저를 mock 으로 두어 <b>DB 없이</b> 돈다.
 */
class ProfileImageWriterTest {

    private static final long USER_ID = 7L;

    private AnnotationConfigApplicationContext context;
    private ProfileImageWriter writer;
    private PlatformTransactionManager tx;
    private UserMapper userMapper;
    private ImageStorage imageStorage;

    @Configuration
    @EnableTransactionManagement
    static class Ctx {

        @Bean
        PlatformTransactionManager tx() {
            return mock(PlatformTransactionManager.class);
        }

        @Bean
        UserMapper userMapper() {
            return mock(UserMapper.class);
        }

        @Bean
        ImageStorage imageStorage() {
            return mock(ImageStorage.class);
        }

        @Bean
        ProfileImageWriter writer(UserMapper userMapper, ImageStorage imageStorage) {
            return new ProfileImageWriter(userMapper, imageStorage);
        }
    }

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigApplicationContext(Ctx.class);
        writer = context.getBean(ProfileImageWriter.class);
        tx = context.getBean(PlatformTransactionManager.class);
        userMapper = context.getBean(UserMapper.class);
        imageStorage = context.getBean(ImageStorage.class);

        when(tx.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    /** 프록시가 벗겨지면 {@code @Transactional} 이 조용히 무시된다 — 여기서 먼저 빨개진다. */
    @Test
    @DisplayName("빈이 AOP 프록시로 감싸져 있다")
    void isWrappedInProxy() {
        assertTrue(AopUtils.isAopProxy(writer),
                "프록시가 아니면 @Transactional 이 붙어 있어도 트랜잭션이 열리지 않는다");
    }

    @Test
    @DisplayName("apply 는 트랜잭션을 열고 커밋한다")
    void applyOpensAndCommitsTransaction() {
        when(userMapper.updateProfileImageKey(eq(USER_ID), anyString())).thenReturn(1);

        writer.apply(USER_ID, "profile/7/new.jpg", null);

        verify(tx).getTransaction(any());
        verify(tx).commit(any());
        verify(tx, never()).rollback(any());
        verify(userMapper).updateProfileImageKey(USER_ID, "profile/7/new.jpg");
    }

    @Test
    @DisplayName("clear 는 트랜잭션을 열고 커밋한다")
    void clearOpensAndCommitsTransaction() {
        when(userMapper.updateProfileImageKey(USER_ID, null)).thenReturn(1);

        writer.clear(USER_ID, null);

        verify(tx).getTransaction(any());
        verify(tx).commit(any());
        verify(userMapper).updateProfileImageKey(USER_ID, null);
    }

    @Test
    @DisplayName("갱신이 0행이면 NOT_FOUND 로 롤백한다")
    void rollsBackWhenNoRowUpdated() {
        when(userMapper.updateProfileImageKey(eq(USER_ID), anyString())).thenReturn(0);

        BusinessException e = assertThrows(BusinessException.class,
                () -> writer.apply(USER_ID, "profile/7/new.jpg", "profile/7/old.jpg"));

        assertEquals("NOT_FOUND", e.getCode());
        verify(tx).rollback(any());
        verify(tx, never()).commit(any());
        /* 롤백된 트랜잭션이라 afterCommit 이 오지 않는다 — 옛 파일은 그대로 살아 있어야 한다 */
        verify(imageStorage, never()).delete(anyString());
    }
}
