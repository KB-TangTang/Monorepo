package com.kb.tangtang.transaction.service;

import com.kb.tangtang.transaction.domain.LlmJobStatus;
import com.kb.tangtang.transaction.mapper.LlmCategorizationJobMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 이슈 #147 후속 — LLM 분류 작업을 FAILED 로 마감하는 것만 담당한다.
 *
 * 왜 별도 클래스인가:
 * LlmCategorizationProcessingServiceImpl.processJob 은 @Transactional 이고, 실패 시
 * FAILED 로 마감한 뒤 예외를 다시 던진다. 같은 트랜잭션에 있으면 스프링 기본 롤백 정책이
 * 그 마감을 되돌려 버려서 작업이 PENDING 으로 돌아가고, 스케줄러가 매 tick 마다 다시 집어
 * 유료 OpenAI 호출을 무한히 태운다(FAILED 자동 재시도는 이번 범위 밖이다).
 * REQUIRES_NEW 로 독립 트랜잭션에서 커밋해야 바깥이 롤백돼도 FAILED 가 살아남는다.
 *
 * LlmCategorizationProcessingServiceImpl 안의 메서드로 두면 self-invocation 이라 프록시를
 * 타지 않아 @Transactional 이 통째로 무시된다. 반드시 다른 빈이어야 한다.
 * (같은 함정·같은 해법: user.service.RefreshTokenSecurityService)
 */
@Service
public class LlmCategorizationFailureService {

    private final LlmCategorizationJobMapper jobMapper;

    public LlmCategorizationFailureService(LlmCategorizationJobMapper jobMapper) {
        this.jobMapper = jobMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(long jobId, LocalDateTime finishedAt) {
        jobMapper.markFinished(jobId, LlmJobStatus.FAILED, finishedAt);
    }
}
