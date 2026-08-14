package com.kb.tangtang.challenge.service;

import com.kb.tangtang.challenge.domain.ChallengeGroup;
import com.kb.tangtang.challenge.domain.ChallengeGroupStatus;
import com.kb.tangtang.challenge.domain.GroupMember;
import com.kb.tangtang.challenge.mapper.ChallengeGroupMapper;
import com.kb.tangtang.challenge.mapper.GroupMemberMapper;
import com.kb.tangtang.notification.domain.NotificationRequestedEvent;
import com.kb.tangtang.notification.domain.NotificationType;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/**
 * 그룹 챌린지 한 건의 시작 전이 (이슈 #152).
 *
 * <p>시작일이 도래한 그룹을 참여 인원에 따라 갈라 보낸다.
 * <pre>
 *   참여자 2명 이상 → ACTIVE  + 참여자 전원에게 시작 알림
 *   참여자 1명      → CLOSED  + 방장에게만 미성립 알림
 * </pre>
 *
 * <p><b>배치 본체(ChallengeGroupStatusBatchService)와 클래스를 나눈 이유</b>는 트랜잭션을
 * 그룹 하나마다 끊기 위해서다. 한 클래스 안에서 부르면 자기호출이라 프록시를 타지 않아
 * {@code @Transactional} 이 걸리지 않고, 배치 전체가 한 트랜잭션이 되면 마지막 그룹에서 난
 * 오류가 앞서 전이된 그룹까지 되돌린다. 그때 알림은 이미 나간 뒤라 DB 와 알림함이 어긋난다.
 * ({@code RelativeMissionAssignmentBatchService} → {@code RelativeMissionAssignmentService} 와 같은 구조)
 *
 * <p><b>멱등성</b>은 {@code updateStatusIfCurrent} 의 반환값이 보장한다. 이미 전이된 그룹은
 * 0 행이 바뀌고 그 자리에서 멈추므로, 배치를 두 번 돌려도 알림이 두 번 나가지 않는다.
 * 조회 결과를 믿고 무조건 UPDATE 하면 조회와 UPDATE 사이에 다른 인스턴스가 끼어들 수 있다.
 */
@Service
@Log4j2
public class ChallengeGroupStatusTransitionService {

    /**
     * 챌린지가 성립하는 최소 인원. {@code ck_cg_members} 가 정원을 2~6 으로 강제하는 것과 같은 값이다 —
     * 혼자 하는 그룹 챌린지는 재판(투표)이 성립하지 않는다.
     */
    static final int MIN_MEMBERS = 2;

    private final ChallengeGroupMapper challengeGroupMapper;
    private final GroupMemberMapper groupMemberMapper;
    private final ApplicationEventPublisher events;

    public ChallengeGroupStatusTransitionService(ChallengeGroupMapper challengeGroupMapper,
                                                 GroupMemberMapper groupMemberMapper,
                                                 ApplicationEventPublisher events) {
        this.challengeGroupMapper = challengeGroupMapper;
        this.groupMemberMapper = groupMemberMapper;
        this.events = events;
    }

    /**
     * 모집 중인 그룹 하나를 시작시키거나 미성립 처리한다.
     *
     * @return 상태를 실제로 바꿨으면 {@code true}. 이미 다른 실행이 바꿔놨으면 {@code false}
     */
    @Transactional
    public boolean startOrCancel(ChallengeGroup group) {
        List<GroupMember> members = groupMemberMapper.findByGroupIds(List.of(group.getId()));
        boolean established = members.size() >= MIN_MEMBERS;
        ChallengeGroupStatus target = established
                ? ChallengeGroupStatus.ACTIVE
                : ChallengeGroupStatus.CLOSED;

        int changed = challengeGroupMapper.updateStatusIfCurrent(
                group.getId(), ChallengeGroupStatus.RECRUITING.name(), target.name());
        if (changed == 0) {
            /* 이 배치가 조회한 뒤 다른 실행이 먼저 바꿨다. 알림까지 가면 중복 발송이 된다. */
            log.info("그룹 챌린지 상태 전이 건너뜀 groupId={} — 이미 RECRUITING 이 아니다", group.getId());
            return false;
        }

        if (established) {
            publishStarted(group, members);
        } else {
            publishCanceled(group);
        }
        log.info("그룹 챌린지 상태 전이 groupId={} {} → {} memberCount={}",
                group.getId(), ChallengeGroupStatus.RECRUITING, target, members.size());
        return true;
    }

    /** 참여자 전원에게. 방장도 {@code tbl_group_member} 에 들어 있어 따로 챙기지 않는다. */
    private void publishStarted(ChallengeGroup group, List<GroupMember> members) {
        Map<String, String> params = Map.of(
                "groupName", group.getGroupName(),
                "days", String.valueOf(totalDays(group)));
        for (GroupMember member : members) {
            events.publishEvent(new NotificationRequestedEvent(
                    member.getUserId(),
                    NotificationType.GROUP_CHALLENGE_STARTED,
                    params,
                    deepLink(group)));
        }
    }

    /**
     * 방장에게만. 이 시점의 참여자는 방장 한 명뿐이라 전원 발송과 결과가 같지만,
     * 정책이 "방장에게 알린다" 이므로 그대로 적는다.
     */
    private void publishCanceled(ChallengeGroup group) {
        events.publishEvent(new NotificationRequestedEvent(
                group.getAdminId(),
                NotificationType.GROUP_CHALLENGE_CANCELED,
                Map.of("groupName", group.getGroupName()),
                deepLink(group)));
    }

    private String deepLink(ChallengeGroup group) {
        return "/group-challenges/" + group.getId();
    }

    /** 시작일·종료일을 모두 포함한 일수. 같은 날이면 1. */
    private int totalDays(ChallengeGroup group) {
        return (int) ChronoUnit.DAYS.between(group.getStartDate(), group.getEndDate()) + 1;
    }
}
