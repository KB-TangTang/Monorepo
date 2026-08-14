package com.kb.tangtang.challenge.service;

import com.kb.tangtang.challenge.domain.ChallengeGroup;
import com.kb.tangtang.challenge.domain.ChallengeGroupStatus;
import com.kb.tangtang.challenge.mapper.ChallengeGroupMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * 그룹 챌린지 시작 상태 전이 배치 (이슈 #152).
 *
 * <p>조회 시점 파생이 아니라 배치로 처리하는 이유는 <b>알림을 보내려면 "전이하는 순간"이
 * 명시적으로 있어야</b> 하기 때문이다. status 를 화면에서 날짜로 계산해 보여주면 그 순간이 없다.
 *
 * <p>여기에 {@code @Transactional} 을 붙이지 않는다. 그룹 하나가 실패해도 나머지는 처리돼야 한다 —
 * 트랜잭션 경계는 {@link ChallengeGroupStatusTransitionService} 가 그룹 단위로 잡는다.
 */
@Service
@Log4j2
public class ChallengeGroupStatusBatchService {

    private final ChallengeGroupMapper challengeGroupMapper;
    private final ChallengeGroupStatusTransitionService transitionService;

    public ChallengeGroupStatusBatchService(ChallengeGroupMapper challengeGroupMapper,
                                            ChallengeGroupStatusTransitionService transitionService) {
        this.challengeGroupMapper = challengeGroupMapper;
        this.transitionService = transitionService;
    }

    /**
     * 시작일이 도래한 모집 중 그룹을 전부 전이시킨다.
     *
     * <p>{@code start_date < today} 인 그룹도 함께 집는다(등호가 아니라 부등호다). 배치가 하루
     * 걸러 뛰거나 서버가 내려가 있었어도 다음 실행이 밀린 그룹을 주워 담아야 하기 때문이다.
     *
     * @param today 배치 기준일
     * @return 실제로 상태가 바뀐 그룹 수
     */
    public int startDueGroups(LocalDate today) {
        List<ChallengeGroup> due = challengeGroupMapper.findGroupsToStart(
                ChallengeGroupStatus.RECRUITING.name(), today);
        if (due.isEmpty()) {
            return 0;
        }

        int transitioned = 0;
        for (ChallengeGroup group : due) {
            try {
                if (transitionService.startOrCancel(group)) {
                    transitioned++;
                }
            } catch (RuntimeException e) {
                /* 한 그룹의 실패가 나머지를 막지 않는다. 다음 실행이 다시 집어 간다 */
                log.error("그룹 챌린지 상태 전이 실패 groupId={}", group.getId(), e);
            }
        }
        log.info("그룹 챌린지 상태 전이 배치 완료 today={} 대상={} 전이={}", today, due.size(), transitioned);
        return transitioned;
    }
}
