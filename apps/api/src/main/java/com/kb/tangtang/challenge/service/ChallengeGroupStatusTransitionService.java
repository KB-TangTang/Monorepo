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
 *   참여자 2명 이상 → ACTIVE + 참여자 전원에게 시작 알림
 *   참여자 1명      → 삭제 + 방장에게 미성립 알림
 * </pre>
 *
 * <p><b>미성립 그룹을 CLOSED 로 두지 않고 지우는 이유</b>는 {@code CLOSED} 가 「정상 종료 +
 * 최종 결과 확정 완료」를 뜻하는 상태이기 때문이다(이슈 #261). 시작조차 못 한 그룹은
 * {@code final_*} 가 영원히 NULL 이고 결산 행도 없으며 무엇보다 {@code end_date} 가 미래라,
 * 종료 연월로 월 귀속을 잡는 그룹 전적·월간 리포트가 이 그룹을 미래 달에 끌어다 놓는다.
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

    /**
     * 그룹 챌린지 홈. 미성립 알림의 딥링크다 — 그룹이 삭제돼 상세 화면으로 보내면 404 가 된다
     * (프론트 라우트 {@code /group-challenges/:id}). 시작 알림은 뒤에 groupId 를 붙여 상세로 보낸다.
     */
    private static final String GROUP_CHALLENGE_HOME = "/group-challenges";

    private final ChallengeGroupMapper challengeGroupMapper;
    private final GroupMemberMapper groupMemberMapper;
    private final ChallengeGroupDeleter challengeGroupDeleter;
    private final ApplicationEventPublisher events;

    public ChallengeGroupStatusTransitionService(ChallengeGroupMapper challengeGroupMapper,
                                                 GroupMemberMapper groupMemberMapper,
                                                 ChallengeGroupDeleter challengeGroupDeleter,
                                                 ApplicationEventPublisher events) {
        this.challengeGroupMapper = challengeGroupMapper;
        this.groupMemberMapper = groupMemberMapper;
        this.challengeGroupDeleter = challengeGroupDeleter;
        this.events = events;
    }

    /**
     * 모집 중인 그룹 하나를 시작시키거나 미성립 처리한다.
     *
     * <p><b>「시작일 23:59까지 초대」(이슈 #350)는 이미 성립한 그룹에만 적용된다.</b>
     * 2명 이상이면 여기서 시작시키고, 그 뒤 참여는 {@code joinBlockReason} 이
     * ACTIVE + {@code today <= startDate} 를 열어 둬 그 날 23:59 까지 계속 받는다.
     *
     * <p>반대로 <b>혼자인 그룹은 시작일 00:01 에 그대로 지운다.</b> 한때 이것도 하루 미뤄
     * 「시작일 23:59까지」를 맞추려 했지만 되돌렸다 — 미루면 시작일 하루가 RECRUITING 인 채로
     * 흘러가고, 그 날 늦게 누가 들어오면 다음 날 00:01 에 ACTIVE 가 되면서
     * {@code findGroupsToEvaluate} 가 {@code start_date <= today} 로 집어 <b>이미 지나간 시작일이
     * 소급 기소</b>된다. 성립하지 못한 그룹을 살려 두는 값보다 이 부작용이 크다.
     *
     * @return 실제로 처리했으면 {@code true}. 이미 다른 실행이 처리했으면 {@code false}
     */
    @Transactional
    public boolean startOrCancel(ChallengeGroup group) {
        List<GroupMember> members = groupMemberMapper.findByGroupIds(List.of(group.getId()));
        return members.size() >= MIN_MEMBERS ? start(group, members) : cancel(group);
    }

    private boolean start(ChallengeGroup group, List<GroupMember> members) {
        int changed = challengeGroupMapper.updateStatusIfCurrent(
                group.getId(),
                ChallengeGroupStatus.RECRUITING.name(),
                ChallengeGroupStatus.ACTIVE.name());
        if (changed == 0) {
            /* 이 배치가 조회한 뒤 다른 실행이 먼저 바꿨다. 알림까지 가면 중복 발송이 된다. */
            log.info("그룹 챌린지 시작 건너뜀 groupId={} — 이미 RECRUITING 이 아니다", group.getId());
            return false;
        }
        publishStarted(group, members);
        log.info("그룹 챌린지 시작 groupId={} RECRUITING → ACTIVE memberCount={}",
                group.getId(), members.size());
        return true;
    }

    /**
     * 모집 미달 그룹을 지운다.
     *
     * <p>알림을 삭제 뒤에 발행하는 것은 의도적이다. {@code deleteIfRecruiting} 이 {@code false}
     * 를 내면 다른 실행이 이미 처리했다는 뜻이라 알림을 보내면 안 된다. 그룹 정보는 파라미터로
     * 이미 손에 있으므로 행이 사라진 뒤에도 문구를 만들 수 있다.
     *
     * <p>삭제 절차 자체는 {@link ChallengeGroupDeleter} 가 갖는다 — 방장의 삭제(이슈 #352)와
     * 같은 절차라 한 곳에 뒀다.
     */
    private boolean cancel(ChallengeGroup group) {
        if (!challengeGroupDeleter.deleteIfRecruiting(group.getId())) {
            log.info("그룹 챌린지 미성립 처리 건너뜀 groupId={} — 이미 RECRUITING 이 아니다", group.getId());
            return false;
        }
        publishCanceled(group);
        log.info("그룹 챌린지 미성립 삭제 groupId={} groupName={}", group.getId(), group.getGroupName());
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
                GROUP_CHALLENGE_HOME));
    }

    private String deepLink(ChallengeGroup group) {
        return GROUP_CHALLENGE_HOME + "/" + group.getId();
    }

    /** 시작일·종료일을 모두 포함한 일수. 같은 날이면 1. */
    private int totalDays(ChallengeGroup group) {
        return (int) ChronoUnit.DAYS.between(group.getStartDate(), group.getEndDate()) + 1;
    }
}
