package com.kb.tangtang.challenge.service;

import com.kb.tangtang.challenge.domain.GroupMemberConsumptionRow;
import com.kb.tangtang.challenge.dto.ChallengeGroupDetailDto;
import com.kb.tangtang.challenge.dto.ChallengeGroupDto;
import com.kb.tangtang.challenge.dto.GroupDailyMemberDto;
import com.kb.tangtang.challenge.dto.GroupIndictmentDto;
import com.kb.tangtang.challenge.mapper.GroupChallengeResultMapper;
import com.kb.tangtang.common.storage.ImageStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
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
    private final ImageStorage imageStorage;
    private final Clock clock;

    @Autowired
    public ChallengeGroupDetailService(ChallengeGroupService challengeGroupService,
                                       GroupTrialService groupTrialService,
                                       GroupChallengeResultMapper resultMapper,
                                       ImageStorage imageStorage) {
        this(challengeGroupService, groupTrialService, resultMapper, imageStorage,
                Clock.systemDefaultZone());
    }

    ChallengeGroupDetailService(ChallengeGroupService challengeGroupService,
                                GroupTrialService groupTrialService,
                                GroupChallengeResultMapper resultMapper,
                                ImageStorage imageStorage,
                                Clock clock) {
        this.challengeGroupService = challengeGroupService;
        this.groupTrialService = groupTrialService;
        this.resultMapper = resultMapper;
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

        return ChallengeGroupDetailDto.builder()
                .challenge(challenge)
                .myDailyAmount(myAmount)
                .myUsagePercent(usagePercent(myAmount, limitAmount))
                .myRemainingAmount(BigDecimal.valueOf(limitAmount).subtract(myAmount))
                .indictments(indictments)
                .dailyMembers(others)
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
