package com.kb.tangtang.challenge.service;

import com.kb.tangtang.challenge.domain.ChallengeGroup;
import com.kb.tangtang.challenge.domain.ChallengeGroupStatus;
import com.kb.tangtang.challenge.domain.EvalType;
import com.kb.tangtang.challenge.domain.GroupRankingRow;
import com.kb.tangtang.challenge.dto.GroupRankingDto;
import com.kb.tangtang.challenge.mapper.ChallengeGroupMapper;
import com.kb.tangtang.challenge.mapper.GroupChallengeResultMapper;
import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.common.storage.ImageStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * 명예 법정(생존자 랭킹) 조회 (이슈 #173).
 *
 * <p>상태로 조회 경로가 갈린다 — <b>종료(CLOSED)는 확정값을 읽기만 하고, 그 외에는 지금 시점의
 * 순위를 계산한다.</b> 종료 후 재계산 금지는 #172 와의 계약이다: {@code final_*} 는 write-once 고,
 * 종료 후 환불·재분류로 소비액이 바뀌어도 이미 사용자에게 보여 준 순위는 뒤집히지 않아야 한다.
 * 개표 중(JUDGING)은 아직 확정 전이라 진행 중과 같은 경로다.
 */
@Service
public class GroupRankingService {

    private final ChallengeGroupMapper challengeGroupMapper;
    private final GroupChallengeResultMapper resultMapper;
    private final ImageStorage imageStorage;
    private final Clock clock;

    @Autowired
    public GroupRankingService(ChallengeGroupMapper challengeGroupMapper,
                               GroupChallengeResultMapper resultMapper,
                               ImageStorage imageStorage) {
        this(challengeGroupMapper, resultMapper, imageStorage, Clock.systemDefaultZone());
    }

    GroupRankingService(ChallengeGroupMapper challengeGroupMapper,
                        GroupChallengeResultMapper resultMapper,
                        ImageStorage imageStorage,
                        Clock clock) {
        this.challengeGroupMapper = challengeGroupMapper;
        this.resultMapper = resultMapper;
        this.imageStorage = imageStorage;
        this.clock = clock;
    }

    /**
     * 명예 법정 한 벌. 참여자가 아니면 {@code GROUP_NOT_MEMBER} 로 거절된다.
     *
     * <p>참여 여부는 랭킹 행에서 확인한다 — 랭킹은 어차피 참여자 전원을 담으므로 참여자 목록을
     * 따로 읽으면 같은 것을 두 번 읽는 셈이다. 거절이 확정되면 읽은 행은 응답에 실리지 않는다.
     */
    @Transactional(readOnly = true)
    public GroupRankingDto findRanking(long userId, long groupId) {
        ChallengeGroup group = challengeGroupMapper.findById(groupId);
        if (group == null) {
            throw new BusinessException("GROUP_NOT_FOUND", "챌린지를 찾을 수 없습니다.");
        }

        boolean closed = ChallengeGroupStatus.CLOSED.name().equals(group.getStatus());
        List<GroupRankingRow> rows = closed
                ? resultMapper.findRankingClosed(groupId)
                : resultMapper.findRankingActive(groupId, group.getEvalType());

        boolean member = false;
        List<GroupRankingDto.Member> members = new ArrayList<>(rows.size());
        for (GroupRankingRow row : rows) {
            boolean mine = row.getUserId() != null && row.getUserId() == userId;
            member |= mine;
            members.add(GroupRankingDto.Member.builder()
                    .rank(row.getRank())
                    .userId(row.getUserId())
                    .nickname(row.getNickname())
                    .profileImageUrl(imageStorage.urlOf(row.getProfileImageKey()))
                    .mine(mine)
                    .livesCount(row.getLivesCount())
                    .totalConsumption(row.getTotalConsumption())
                    .finalOutcome(row.getFinalOutcome())
                    .finalChargeAmount(row.getFinalChargeAmount())
                    .build());
        }
        if (!member) {
            throw new BusinessException("GROUP_NOT_MEMBER", "참여 중인 챌린지가 아닙니다.");
        }

        return GroupRankingDto.builder()
                .evalType(group.getEvalType())
                .status(group.getStatus())
                .limitAmount(group.getLimitAmount())
                .maxLives(maxLives(group))
                .memo(group.getMemo())
                .lastSettlementDate(lastSettlementDate(group, closed))
                .members(members)
                .build();
    }

    /**
     * 시작 목숨 수. 참여 INSERT 가 쓰는 {@link EvalType#livesFor} 를 그대로 다시 계산한다 —
     * 그룹 테이블에 저장된 값이 아니라서 계산식이 두 벌이 되면 안 된다.
     */
    private int maxLives(ChallengeGroup group) {
        int totalDays = (int) (ChronoUnit.DAYS.between(group.getStartDate(), group.getEndDate()) + 1);
        return EvalType.valueOf(group.getEvalType()).livesFor(totalDays);
    }

    /**
     * 종료 후에는 {@code end_date} 가 곧 마지막 결산일이다 — 조회할 필요가 없고,
     * 진행 중에만 「오늘 제외 마지막 결산 행」을 읽는다.
     */
    private LocalDate lastSettlementDate(ChallengeGroup group, boolean closed) {
        if (closed) {
            return group.getEndDate();
        }
        return resultMapper.findLastSettlementDate(group.getId(), LocalDate.now(clock));
    }
}
