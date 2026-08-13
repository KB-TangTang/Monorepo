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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LlmCategorizationJobStateServiceTest {

    private LlmCategorizationJobMapper jobMapper;
    private LlmCategorizationJobStateService service;

    @BeforeEach
    void setUp() {
        jobMapper = mock(LlmCategorizationJobMapper.class);
        service = new LlmCategorizationJobStateService(jobMapper);
    }

    @Test
    @DisplayName("작업을 FAILED 상태와 종료시각으로 마감한다")
    void marksJobFailed() {
        LocalDateTime finishedAt = LocalDateTime.of(2026, 8, 13, 9, 0);

        service.markFailed(1L, finishedAt);

        verify(jobMapper).markFinished(1L, "FAILED", finishedAt);
    }

    @Test
    @DisplayName("선점은 markProcessing 을 호출하고 갱신된 행 수를 그대로 돌려준다")
    void claimProcessingReturnsAffectedRowCount() {
        LocalDateTime startedAt = LocalDateTime.of(2026, 8, 13, 9, 0);
        when(jobMapper.markProcessing(1L, startedAt)).thenReturn(1);

        assertEquals(1, service.claimProcessing(1L, startedAt));

        verify(jobMapper).markProcessing(1L, startedAt);
    }

    @Test
    @DisplayName("이미 다른 tick 이 가져간 작업이면 0 을 그대로 돌려준다")
    void claimProcessingReturnsZeroWhenAlreadyClaimed() {
        LocalDateTime startedAt = LocalDateTime.of(2026, 8, 13, 9, 0);
        when(jobMapper.markProcessing(anyLong(), any())).thenReturn(0);

        assertEquals(0, service.claimProcessing(2L, startedAt));
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
        Method method = LlmCategorizationJobStateService.class
                .getDeclaredMethod("markFailed", long.class, LocalDateTime.class);

        Transactional transactional = method.getAnnotation(Transactional.class);

        assertNotNull(transactional, "markFailed 에 @Transactional 이 없다");
        assertEquals(Propagation.REQUIRES_NEW, transactional.propagation(),
                "markFailed 는 REQUIRES_NEW 여야 바깥 롤백에도 FAILED 마감이 살아남는다");
    }

    /**
     * 선점(PROCESSING) 도 REQUIRES_NEW 여야 한다.
     *
     * markProcessing 은 그 작업 행에 UPDATE 를 걸어 커밋 전까지 InnoDB 행 잠금을 쥔다.
     * 이 UPDATE 가 processJob 자신의 트랜잭션 안에서 실행되면, 실패 시 markFailed 가
     * REQUIRES_NEW(= 별도 커넥션)로 같은 행을 UPDATE 하려다 아직 열려 있는 바깥 트랜잭션이 쥔
     * 잠금을 기다린다 → innodb_lock_wait_timeout(기본 50초) 뒤 CannotAcquireLockException 이
     * 원래 예외를 덮어쓰고 바깥이 롤백되며 작업이 PENDING 으로 부활한다.
     * 선점을 독립 트랜잭션으로 먼저 커밋해 잠금을 놓아야 그 교착이 사라진다.
     *
     * 실제 잠금 대기 재현은 진짜 MySQL 커넥션 두 개가 필요해 이 스위트의 범위 밖이다
     * (계획 문서 Global Constraints: 실제 DB 연결이 필요한 테스트는 만들지 않는다).
     * 여기서는 구조(별도 빈 + REQUIRES_NEW)만 못 박는다.
     */
    @Test
    @DisplayName("claimProcessing 은 REQUIRES_NEW 독립 트랜잭션이다")
    void claimProcessingRunsInItsOwnTransaction() throws Exception {
        Method method = LlmCategorizationJobStateService.class
                .getDeclaredMethod("claimProcessing", long.class, LocalDateTime.class);

        Transactional transactional = method.getAnnotation(Transactional.class);

        assertNotNull(transactional, "claimProcessing 에 @Transactional 이 없다");
        assertEquals(Propagation.REQUIRES_NEW, transactional.propagation(),
                "claimProcessing 이 REQUIRES_NEW 가 아니면 선점 잠금을 붙든 채 작업을 돌게 되어 "
                        + "FAILED 마감이 자기 잠금을 기다리다 lock wait timeout 에 걸린다");
    }

    /**
     * self-invocation 함정 방지 — 이 로직은 반드시 processJob 을 가진 클래스 '밖'에 있어야 한다.
     * 같은 클래스의 메서드로 옮기면 프록시를 타지 않아 @Transactional 이 통째로 무시된다.
     */
    @Test
    @DisplayName("상태 전이 로직은 처리 서비스와 별개의 빈에 있다")
    void isSeparateBeanFromProcessingService() {
        assertNotNull(LlmCategorizationJobStateService.class.getAnnotation(Service.class),
                "스프링 빈이 아니면 프록시를 타지 않아 REQUIRES_NEW 가 적용되지 않는다");

        /* 처리 서비스가 자기 메서드가 아니라 이 별도 빈에 위임하고 있어야 한다. */
        boolean delegatesToThisBean = Arrays.stream(
                        LlmCategorizationProcessingServiceImpl.class.getDeclaredFields())
                .anyMatch(field -> field.getType() == LlmCategorizationJobStateService.class);
        assertTrue(delegatesToThisBean,
                "processJob 이 같은 클래스 안에서 선점·FAILED 마감을 하면 self-invocation 이라 "
                        + "@Transactional 이 무시된다 — 별도 빈에 위임해야 한다");
    }
}
