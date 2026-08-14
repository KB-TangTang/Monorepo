package com.kb.tangtang.fixedexpense.service;

import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.fixedexpense.domain.FixedExpenseCandidate;
import com.kb.tangtang.fixedexpense.dto.FixedExpenseCandidateActionResponseDto;
import com.kb.tangtang.fixedexpense.mapper.FixedExpenseCandidateActionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

/** 고정지출 탐지 후보의 명시적 확정·제외 결정을 처리한다. */
@Service
public class FixedExpenseCandidateActionService {

    private static final String ACTIVE = "ACTIVE";
    private static final String CONFIRM = "CONFIRM";
    private static final String EXCLUDE = "EXCLUDE";

    private final FixedExpenseCandidateActionMapper mapper;
    private final Clock clock;

    @Autowired
    public FixedExpenseCandidateActionService(
            FixedExpenseCandidateActionMapper mapper,
            @Value("${fixed.expense.detection.zone:Asia/Seoul}") String zoneId) {
        this(mapper, Clock.system(ZoneId.of(zoneId)));
    }

    FixedExpenseCandidateActionService(FixedExpenseCandidateActionMapper mapper, Clock clock) {
        this.mapper = mapper;
        this.clock = clock;
    }

    /**
     * 조건부 갱신으로 결정 자체를 원자적으로 적용한다.
     *
     * <p>0건 갱신은 없는/타인 후보 또는 이미 바뀐 상태를 뜻한다. 이때 잠금 조회로 최신 상태를
     * 확인해 같은 action 재전송은 성공으로, 반대 action이나 비활성 상태는 충돌로 판정한다.
     */
    @Transactional
    public FixedExpenseCandidateActionResponseDto decide(long userId, long candidateId, String action) {
        String normalizedAction = validateAction(action);
        LocalDateTime decidedAt = LocalDateTime.now(clock).withNano(0);

        int updated = CONFIRM.equals(normalizedAction)
                ? mapper.confirmCandidate(candidateId, userId, decidedAt)
                : mapper.excludeCandidate(candidateId, userId);
        if (updated == 1) {
            return FixedExpenseCandidateActionResponseDto.builder()
                    .candidateId(candidateId)
                    .status(ACTIVE)
                    .isExcluded(EXCLUDE.equals(normalizedAction))
                    .confirmedAt(CONFIRM.equals(normalizedAction) ? decidedAt : null)
                    .build();
        }

        FixedExpenseCandidate current = mapper.findOwnedCandidateForUpdate(candidateId, userId);
        if (current == null) {
            throw new BusinessException("NOT_FOUND", "고정지출 후보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND);
        }
        if (isSameActionResult(current, normalizedAction)) {
            return toResponse(current);
        }
        throw new BusinessException("INVALID_REQUEST", "현재 후보 상태에서는 요청한 처리를 할 수 없습니다.",
                HttpStatus.CONFLICT);
    }

    private String validateAction(String action) {
        if (CONFIRM.equals(action) || EXCLUDE.equals(action)) {
            return action;
        }
        throw new BusinessException("INVALID_REQUEST", "action은 CONFIRM 또는 EXCLUDE여야 합니다.");
    }

    private boolean isSameActionResult(FixedExpenseCandidate candidate, String action) {
        if (CONFIRM.equals(action)) {
            return ACTIVE.equals(candidate.getStatus())
                    && !candidate.isExcluded()
                    && candidate.getConfirmedAt() != null;
        }
        return candidate.isExcluded();
    }

    private FixedExpenseCandidateActionResponseDto toResponse(FixedExpenseCandidate candidate) {
        return FixedExpenseCandidateActionResponseDto.builder()
                .candidateId(candidate.getId())
                .status(candidate.getStatus())
                .isExcluded(candidate.isExcluded())
                .confirmedAt(candidate.getConfirmedAt())
                .build();
    }
}
