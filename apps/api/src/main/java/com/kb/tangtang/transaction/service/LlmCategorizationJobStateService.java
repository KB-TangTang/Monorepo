package com.kb.tangtang.transaction.service;

import com.kb.tangtang.transaction.domain.LlmJobStatus;
import com.kb.tangtang.transaction.mapper.LlmCategorizationJobMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 이슈 #147 후속 — tbl_llm_categorization_job 의 상태 전이 중
 * **호출자(processJob)의 트랜잭션과 무관하게 반드시 커밋돼야 하는 것**만 담당한다.
 *
 * 담당하는 두 전이:
 * 1. claimProcessing — PENDING → PROCESSING (작업 선점)
 * 2. markFailed      — → FAILED (실패 마감)
 *
 * 왜 둘 다 별도 빈의 REQUIRES_NEW 인가:
 *
 * (1) FAILED 마감이 살아남아야 한다.
 * LlmCategorizationProcessingServiceImpl.processJob 은 @Transactional 이고, 실패 시
 * FAILED 로 마감한 뒤 예외를 다시 던진다. 같은 트랜잭션에 있으면 스프링 기본 롤백 정책이
 * 그 마감을 되돌려 버려서 작업이 PENDING 으로 돌아가고, 스케줄러가 매 tick 마다 다시 집어
 * 유료 OpenAI 호출을 무한히 태운다(FAILED 자동 재시도는 이번 범위 밖이다).
 *
 * (2) PROCESSING 선점이 먼저 커밋돼야 (1)이 실제로 동작한다.
 * markProcessing 은 그 작업 행에 UPDATE 를 걸어 커밋 전까지 InnoDB 행 잠금을 쥔다.
 * 이 UPDATE 가 processJob 자신의 트랜잭션 안에 있으면, 그 트랜잭션이 아직 열려 있는 채로
 * markFailed 가 REQUIRES_NEW(= 다른 커넥션)로 같은 행을 UPDATE 하려다 자기 자신이 쥔 잠금을
 * 기다리게 된다 → innodb_lock_wait_timeout(기본 50초) 만큼 스케줄러 스레드가 멈춘 뒤
 * CannotAcquireLockException 이 원래 예외를 덮어쓰고, 바깥 트랜잭션이 롤백되며 PROCESSING 선점까지
 * 되돌아가 작업이 PENDING 으로 부활한다. 선점을 독립 트랜잭션으로 먼저 커밋해 잠금을 놓아야
 * 이 교착이 사라진다.
 *
 * LlmCategorizationProcessingServiceImpl 안의 메서드로 두면 self-invocation 이라 프록시를
 * 타지 않아 @Transactional 이 통째로 무시된다. 반드시 다른 빈이어야 한다.
 * (같은 함정·같은 해법: user.service.RefreshTokenSecurityService)
 */
@Service
public class LlmCategorizationJobStateService {

    private final LlmCategorizationJobMapper jobMapper;

    public LlmCategorizationJobStateService(LlmCategorizationJobMapper jobMapper) {
        this.jobMapper = jobMapper;
    }

    /**
     * PENDING → PROCESSING 선점. 매퍼 SQL 의 {@code AND status = 'PENDING'} 가드 덕분에
     * 이미 다른 tick 이 가져간 작업이면 0 행이 갱신된다.
     *
     * @return 갱신된 행 수. 0 이면 이 실행 주체는 이 작업을 처리하면 안 된다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int claimProcessing(long jobId, LocalDateTime startedAt) {
        return jobMapper.markProcessing(jobId, startedAt);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(long jobId, LocalDateTime finishedAt) {
        jobMapper.markFinished(jobId, LlmJobStatus.FAILED, finishedAt);
    }
}
