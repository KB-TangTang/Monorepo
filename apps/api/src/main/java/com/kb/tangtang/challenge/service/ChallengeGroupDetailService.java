package com.kb.tangtang.challenge.service;

import com.kb.tangtang.challenge.domain.ChallengeGroupStatus;
import com.kb.tangtang.challenge.domain.GroupMember;
import com.kb.tangtang.challenge.domain.GroupMemberConsumptionRow;
import com.kb.tangtang.challenge.domain.GroupTrialStatsRow;
import com.kb.tangtang.challenge.dto.ChallengeGroupDetailDto;
import com.kb.tangtang.challenge.dto.ChallengeGroupDto;
import com.kb.tangtang.challenge.dto.GroupDailyMemberDto;
import com.kb.tangtang.challenge.dto.GroupIndictmentDto;
import com.kb.tangtang.challenge.mapper.GroupChallengeResultMapper;
import com.kb.tangtang.challenge.mapper.GroupMemberMapper;
import com.kb.tangtang.challenge.mapper.IndictmentMapper;
import com.kb.tangtang.common.storage.ImageStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 그룹 챌린지 상세 화면 조립 (이슈 #169).
 *
 * <p>스스로 읽는 것은 참여자별 소비액 하나뿐이다. 나머지는 이미 있는 것을 모은다 —
 * 그룹·참여자는 {@link ChallengeGroupService}, 재판 카드는 {@link GroupTrialService}.
 *
 * <p>세 조각을 {@code ChallengeGroupService} 안에 몰지 않았다. 그쪽은 이미 470줄이고
 * 6명이 동시에 손대는 파일이라 화면 조립까지 들어가면 충돌이 늘어난다. 무엇보다
 * <b>재판 시간 프로퍼티({@code challenge.trial.*})가 두 클래스로 갈린다</b> — 마감 계산이
 * 두 벌이 되면 홈 화면과 상세 화면의 마감이 서로 달라진다.
 *
 * <p>접근 권한은 {@link ChallengeGroupService#findDetail} 이 이미 확인한다. 여기서 다시
 * 확인하지 않는 이유는 규칙이 바뀔 때 한쪽만 고쳐지는 사고를 막기 위해서다.
 */
@Service
public class ChallengeGroupDetailService {

    private final ChallengeGroupService challengeGroupService;
    private final GroupTrialService groupTrialService;
    private final GroupChallengeResultMapper resultMapper;
    private final GroupMemberMapper groupMemberMapper;
    private final IndictmentMapper indictmentMapper;
    private final ImageStorage imageStorage;
    private final Clock clock;

    @Autowired
    public ChallengeGroupDetailService(ChallengeGroupService challengeGroupService,
                                       GroupTrialService groupTrialService,
                                       GroupChallengeResultMapper resultMapper,
                                       GroupMemberMapper groupMemberMapper,
                                       IndictmentMapper indictmentMapper,
                                       ImageStorage imageStorage) {
        this(challengeGroupService, groupTrialService, resultMapper, groupMemberMapper,
                indictmentMapper, imageStorage, Clock.systemDefaultZone());
    }

    ChallengeGroupDetailService(ChallengeGroupService challengeGroupService,
                                GroupTrialService groupTrialService,
                                GroupChallengeResultMapper resultMapper,
                                GroupMemberMapper groupMemberMapper,
                                IndictmentMapper indictmentMapper,
                                ImageStorage imageStorage,
                                Clock clock) {
        this.challengeGroupService = challengeGroupService;
        this.groupTrialService = groupTrialService;
        this.resultMapper = resultMapper;
        this.groupMemberMapper = groupMemberMapper;
        this.indictmentMapper = indictmentMapper;
        this.imageStorage = imageStorage;
        this.clock = clock;
    }

    /**
     * 상세 한 벌. 참여자가 아니면 {@code GROUP_NOT_MEMBER} 로 거절된다.
     *
     * <p>재판 카드와 소비 상태는 <b>진행 중(ACTIVE)이 아니어도 그대로 조립한다.</b> 모집 중이면
     * 기소가 없어 빈 배열이 되고 소비액은 0원이 된다. 상태로 분기해 필드를 비우면, 상태 전이 배치가
     * 도는 그 순간 화면이 무엇을 보여줄지가 갈려 재현하기 어려운 버그가 된다.
     * 무엇을 그릴지는 화면이 {@code status} 를 보고 정한다.
     *
     * <p><b>종료 화면 필드({@code finalMembers}·{@code trialStats})만은 CLOSED 로 분기한다</b>
     * (이슈 #173). 위 원칙의 예외가 아니라 데이터 제약이다 — {@code final_*} 컬럼은 확정 배치(#172)가
     * CLOSED 전이와 함께 채우므로 그 전에는 NULL 뿐이라 만들 수 있는 값이 없다.
     */
    @Transactional(readOnly = true)
    public ChallengeGroupDetailDto findDetail(long userId, long groupId) {
        ChallengeGroupDto challenge = challengeGroupService.findDetail(userId, groupId);
        List<GroupIndictmentDto> indictments = groupTrialService.findGroupIndictments(userId, groupId);

        // 재판 중 배지는 진행 중인 기소에서 그대로 끌어온다. 같은 것을 세는 쿼리를 하나 더 두면
        // "카드에는 재판이 있는데 배지는 없는" 상태가 생긴다.
        Map<Long, String> trialStatusByUser = new HashMap<>();
        for (GroupIndictmentDto indictment : indictments) {
            trialStatusByUser.putIfAbsent(indictment.getUserId(), indictment.getStatus());
        }

        int limitAmount = challenge.getLimitAmount() == null ? 0 : challenge.getLimitAmount();
        BigDecimal myAmount = BigDecimal.ZERO;
        List<GroupDailyMemberDto> others = new ArrayList<>();

        for (GroupMemberConsumptionRow row : resultMapper.findMemberConsumption(groupId, LocalDate.now(clock))) {
            BigDecimal amount = row.getAmount() == null ? BigDecimal.ZERO : row.getAmount();
            if (row.getUserId() != null && row.getUserId() == userId) {
                myAmount = amount;
                continue;
            }
            others.add(GroupDailyMemberDto.builder()
                    .userId(row.getUserId())
                    .nickname(row.getNickname())
                    .profileImageUrl(imageStorage.urlOf(row.getProfileImageKey()))
                    .dailyAmount(amount)
                    .usagePercent(usagePercent(amount, limitAmount))
                    .exceeded(isExceeded(amount, limitAmount))
                    .trialStatus(trialStatusByUser.get(row.getUserId()))
                    .build());
        }

        boolean closed = ChallengeGroupStatus.CLOSED.name().equals(challenge.getStatus());
        return ChallengeGroupDetailDto.builder()
                .challenge(challenge)
                .myDailyAmount(myAmount)
                .myUsagePercent(usagePercent(myAmount, limitAmount))
                .myRemainingAmount(BigDecimal.valueOf(limitAmount).subtract(myAmount))
                .indictments(indictments)
                .dailyMembers(others)
                .finalMembers(closed ? finalMembers(groupId) : null)
                .trialStats(closed ? trialStats(groupId) : null)
                .build();
    }

    /**
     * 종료 화면 참여자 — 확정값({@code final_*})을 <b>읽기만 한다</b>(재계산 금지, #172 와의 계약).
     *
     * <p>정렬은 여기서 한다. {@code findByGroupIds} 는 목록 순서 고정을 위해 {@code user_id} 순으로
     * 읽는데, 종료 화면은 {@code final_rank} 순이어야 포디움·테이블이 맞다. 다른 호출부가 있어
     * XML 의 ORDER BY 를 바꾸지 않는다.
     */
    private List<ChallengeGroupDetailDto.FinalMember> finalMembers(long groupId) {
        List<GroupMember> members = new ArrayList<>(groupMemberMapper.findByGroupIds(List.of(groupId)));
        members.sort(Comparator
                .comparing(GroupMember::getFinalRank, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(GroupMember::getUserId));

        List<ChallengeGroupDetailDto.FinalMember> result = new ArrayList<>();
        for (GroupMember member : members) {
            result.add(ChallengeGroupDetailDto.FinalMember.builder()
                    .userId(member.getUserId())
                    .nickname(member.getNickname())
                    .profileImageUrl(imageStorage.urlOf(member.getProfileImageKey()))
                    .finalRank(member.getFinalRank())
                    .finalOutcome(member.getFinalOutcome())
                    .livesCount(member.getLivesCount())
                    .build());
        }
        return result;
    }

    /** 확정 재판 전적. 재판이 없어도 매퍼가 0 으로 채운 한 행을 준다 — NULL 방어 불필요. */
    private ChallengeGroupDetailDto.TrialStats trialStats(long groupId) {
        GroupTrialStatsRow row = indictmentMapper.findClosedTrialStats(groupId);
        return ChallengeGroupDetailDto.TrialStats.builder()
                .totalTrials(row.getTotalTrials())
                .guiltyCount(row.getGuiltyCount())
                .innocentCount(row.getInnocentCount())
                .build();
    }

    /**
     * 한도 대비 사용률(%). 100 을 넘겨서 돌려준다 — 막대를 자르는 일은 화면이 한다.
     *
     * <p>한도 0원(무지출 챌린지)은 나눗셈이 되지 않는다. 한 푼이라도 썼으면 100 으로 본다.
     * 0 으로 두면 전액 초과인 참여자의 막대가 텅 비어 정상으로 보인다.
     */
    private int usagePercent(BigDecimal amount, int limitAmount) {
        if (limitAmount <= 0) {
            return amount.signum() > 0 ? 100 : 0;
        }
        return amount.multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(limitAmount), 0, RoundingMode.HALF_UP)
                .intValue();
    }

    /**
     * 한도 초과 여부. 사용률이 아니라 <b>금액</b>으로 판정한다 — 반올림한 퍼센트로 보면
     * 한도의 99.6% 를 쓴 참여자가 100% 로 표시되면서 초과로 잘못 걸린다.
     */
    private boolean isExceeded(BigDecimal amount, int limitAmount) {
        return amount.compareTo(BigDecimal.valueOf(limitAmount)) > 0;
    }
}
