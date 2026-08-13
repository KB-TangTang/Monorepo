package com.kb.tangtang.transaction.service;

import com.kb.tangtang.transaction.mapper.LlmCategorizationJobMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class LlmCategorizationFailureServiceTest {

    private LlmCategorizationJobMapper jobMapper;
    private LlmCategorizationFailureService service;

    @BeforeEach
    void setUp() {
        jobMapper = mock(LlmCategorizationJobMapper.class);
        service = new LlmCategorizationFailureService(jobMapper);
    }

    @Test
    @DisplayName("작업을 FAILED 상태와 종료시각으로 마감한다")
    void marksJobFailed() {
        LocalDateTime finishedAt = LocalDateTime.of(2026, 8, 13, 9, 0);

        service.markFailed(1L, finishedAt);

        verify(jobMapper).markFinished(1L, "FAILED", finishedAt);
    }

    /**
     * 이 클래스가 존재하는 유일한 이유가 REQUIRES_NEW 다.
     *
     * processJob 은 FAILED 마감 직후 예외를 다시 던지므로, 이 메서드가 호출자와 같은 트랜잭션에
     * 묶이면 스프링 기본 롤백 정책이 마감을 되돌려 작업이 PENDING 으로 부활하고 스케줄러가
     * 매 tick 마다 유료 OpenAI 호출을 반복한다. 애너테이션이 지워지거나 REQUIRED 로 바뀌면
     * 그 사고가 조용히 되살아나므로 여기서 못 박아 둔다.
     *
     * 실제 롤백 이후에도 FAILED 가 살아남는지는 진짜 DB 커넥션이 필요해 이 스위트의 범위 밖이다
     * (계획 문서 Global Constraints: 실제 DB 연결이 필요한 테스트는 만들지 않는다).
     */
    @Test
    @DisplayName("markFailed 는 REQUIRES_NEW 독립 트랜잭션이다")
    void markFailedRunsInItsOwnTransaction() throws Exception {
        Method method = LlmCategorizationFailureService.class
                .getDeclaredMethod("markFailed", long.class, LocalDateTime.class);

        Transactional transactional = method.getAnnotation(Transactional.class);

        assertNotNull(transactional, "markFailed 에 @Transactional 이 없다");
        assertEquals(Propagation.REQUIRES_NEW, transactional.propagation(),
                "markFailed 는 REQUIRES_NEW 여야 바깥 롤백에도 FAILED 마감이 살아남는다");
    }

    /**
     * self-invocation 함정 방지 — 이 로직은 반드시 processJob 을 가진 클래스 '밖'에 있어야 한다.
     * 같은 클래스의 메서드로 옮기면 프록시를 타지 않아 @Transactional 이 통째로 무시된다.
     */
    @Test
    @DisplayName("FAILED 마감 로직은 처리 서비스와 별개의 빈에 있다")
    void isSeparateBeanFromProcessingService() {
        assertNotNull(LlmCategorizationFailureService.class.getAnnotation(Service.class),
                "스프링 빈이 아니면 프록시를 타지 않아 REQUIRES_NEW 가 적용되지 않는다");

        /* 처리 서비스가 자기 메서드가 아니라 이 별도 빈에 위임하고 있어야 한다. */
        boolean delegatesToThisBean = Arrays.stream(
                        LlmCategorizationProcessingServiceImpl.class.getDeclaredFields())
                .anyMatch(field -> field.getType() == LlmCategorizationFailureService.class);
        assertTrue(delegatesToThisBean,
                "processJob 이 같은 클래스 안에서 FAILED 마감을 하면 self-invocation 이라 "
                        + "@Transactional 이 무시된다 — 별도 빈에 위임해야 한다");
    }
}
