package com.kb.tangtang.challenge.service;

import com.kb.tangtang.challenge.domain.ChallengeGroup;
import com.kb.tangtang.challenge.domain.ChallengeGroupStatus;
import com.kb.tangtang.challenge.mapper.ChallengeGroupMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * 그룹 챌린지 상태 전이 배치. 하루에 한 번, 두 전이를 순서대로 돌린다.
 *
 * <pre>
 *   RECRUITING → ACTIVE / 삭제   {@link #startDueGroups}  (이슈 #152 · #261)
 *   ACTIVE     → JUDGING         {@link #judgeEndedGroups} (이슈 #169)
 * </pre>
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
     * <b>혼자인 그룹의 삭제를 하루 미루는 것(이슈 #350)도 이 부등호에 기대고 있다</b> —
     * 시작일에 유예된 그룹을 다음 날 실행이 그대로 다시 집어 간다.
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
                if (transitionService.startOrCancel(group, today)) {
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

    /**
     * 평가가 끝난 그룹을 재판 상태로 넘긴다 (이슈 #169). {@code ACTIVE → JUDGING}.
     *
     * <p><b>왜 이 이슈에서 하는가.</b> {@code JUDGING} 을 쓰는 코드가 저장소에 한 곳도 없었다.
     * 그래서 종료일이 지난 그룹이 영영 {@code ACTIVE} 로 남아 「진행 중」 탭에 쌓이고
     * 「종료됨」 탭({@code JUDGING,CLOSED})은 비어 있었다. 시드 SQL 이 {@code JUDGING} 행을
     * 직접 넣고 있어 화면상으로는 가려져 있었을 뿐이다.
     *
     * <p><b>기준일이 {@code today} 가 아니라 {@code today - 1일} 인 이유</b>는
     * {@link com.kb.tangtang.challenge.mapper.ChallengeGroupMapper#findGroupsToJudge} 주석 참고 —
     * 평가·기소 배치가 종료 다음 날까지 {@code ACTIVE} 그룹을 봐야 한다.
     *
     * <p>전이 서비스를 거치지 않고 매퍼를 직접 부른다. 발행할 알림도, 함께 묶을 다른 쓰기도 없어
     * UPDATE 한 문장이면 끝이고 그 자체가 원자적이다. 트랜잭션 경계를 만들 이유가 없다.
     * 멱등성은 {@code updateStatusIfCurrent} 의 {@code WHERE status} 비교가 그대로 보장한다.
     *
     * @param today 배치 기준일
     * @return 실제로 상태가 바뀐 그룹 수
     */
    public int judgeEndedGroups(LocalDate today) {
        List<ChallengeGroup> ended = challengeGroupMapper.findGroupsToJudge(
                ChallengeGroupStatus.ACTIVE.name(), today.minusDays(1));
        if (ended.isEmpty()) {
            return 0;
        }

        int transitioned = 0;
        for (ChallengeGroup group : ended) {
            try {
                int changed = challengeGroupMapper.updateStatusIfCurrent(
                        group.getId(),
                        ChallengeGroupStatus.ACTIVE.name(),
                        ChallengeGroupStatus.JUDGING.name());
                if (changed > 0) {
                    transitioned++;
                    log.info("그룹 챌린지 재판 전이 groupId={} ACTIVE → JUDGING endDate={}",
                            group.getId(), group.getEndDate());
                }
            } catch (RuntimeException e) {
                /* 한 그룹의 실패가 나머지를 막지 않는다. 다음 실행이 다시 집어 간다 */
                log.error("그룹 챌린지 재판 전이 실패 groupId={}", group.getId(), e);
            }
        }
        log.info("그룹 챌린지 재판 전이 배치 완료 today={} 대상={} 전이={}", today, ended.size(), transitioned);
        return transitioned;
    }
}
