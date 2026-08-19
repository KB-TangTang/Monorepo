package com.kb.tangtang.challenge.service;

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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 그룹 상세 조립 (이슈 #169).
 *
 * <p>그룹 정보는 {@code ChallengeGroupService}, 재판 카드는 {@code GroupTrialService} 가 이미
 * 각자 테스트로 지키고 있다. 여기서는 <b>둘을 합치면서 생기는 것</b> — 나를 빼는 규칙,
 * 사용률 계산, 재판 중 배지 — 만 본다.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChallengeGroupDetailServiceTest {

    private static final long USER_ID = 7L;
    private static final long GROUP_ID = 3L;
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 16);

    @Mock private ChallengeGroupService challengeGroupService;
    @Mock private GroupTrialService groupTrialService;
    @Mock private GroupChallengeResultMapper resultMapper;
    @Mock private GroupMemberMapper groupMemberMapper;
    @Mock private IndictmentMapper indictmentMapper;
    @Mock private ImageStorage imageStorage;

    private ChallengeGroupDetailService service() {
        return new ChallengeGroupDetailService(challengeGroupService, groupTrialService, resultMapper,
                groupMemberMapper, indictmentMapper, imageStorage,
                Clock.fixed(TODAY.atStartOfDay(ZoneId.systemDefault()).toInstant(),
                ZoneId.systemDefault()));
    }

    private void givenGroup(int limitAmount) {
        givenGroup(limitAmount, "ACTIVE");
    }

    private void givenGroup(int limitAmount, String status) {
        when(challengeGroupService.findDetail(USER_ID, GROUP_ID)).thenReturn(
                ChallengeGroupDto.builder().id(GROUP_ID).groupName("커피값 줄이기")
                        .limitAmount(limitAmount).evalType("DAILY").status(status).build());
    }

    private void givenIndictments(GroupIndictmentDto... cards) {
        when(groupTrialService.findGroupIndictments(USER_ID, GROUP_ID)).thenReturn(List.of(cards));
    }

    private void givenConsumption(GroupMemberConsumptionRow... rows) {
        when(resultMapper.findMemberConsumption(GROUP_ID, TODAY)).thenReturn(List.of(rows));
    }

    private GroupMemberConsumptionRow consumption(long userId, String amount) {
        GroupMemberConsumptionRow row = new GroupMemberConsumptionRow();
        row.setUserId(userId);
        row.setNickname("절약왕" + userId);
        row.setAmount(new BigDecimal(amount));
        return row;
    }

    private GroupIndictmentDto card(long defendantId, String status) {
        return GroupIndictmentDto.builder()
                .id(11L)
                .userId(defendantId)
                .status(status)
                .defenseDeadline(LocalDateTime.of(2026, 8, 16, 15, 0))
                .build();
    }

    /**
     * 상단 먹지 카드와 아래 멤버 그리드가 <b>같은 조회</b>를 나눠 쓴다.
     * 내가 멤버 그리드에 남으면 화면에 내 카드가 두 번 나온다.
     */
    @Test
    @DisplayName("내 소비는 my* 로 빠지고 멤버 목록에는 남지 않는다")
    void splitsMyselfOutOfMemberList() {
        givenGroup(10_000);
        givenIndictments();
        givenConsumption(consumption(USER_ID, "6000"), consumption(9L, "3000"));

        ChallengeGroupDetailDto detail = service().findDetail(USER_ID, GROUP_ID);

        assertEquals(new BigDecimal("6000"), detail.getMyDailyAmount());
        assertEquals(60, detail.getMyUsagePercent());
        assertEquals(List.of(9L),
                detail.getDailyMembers().stream().map(GroupDailyMemberDto::getUserId).toList());
    }

    /** 초과분을 0 으로 깎으면 화면이 「얼마나 넘겼는지」를 표시할 수 없다. */
    @Test
    @DisplayName("한도를 넘기면 남은 금액이 음수로 내려간다")
    void remainingGoesNegativeWhenExceeded() {
        givenGroup(10_000);
        givenIndictments();
        givenConsumption(consumption(USER_ID, "12500"));

        ChallengeGroupDetailDto detail = service().findDetail(USER_ID, GROUP_ID);

        assertEquals(0, new BigDecimal("-2500").compareTo(detail.getMyRemainingAmount()));
        assertEquals(125, detail.getMyUsagePercent(), "막대를 100 에서 자르는 일은 화면이 한다");
    }

    /**
     * 한도 0원(무지출 챌린지)은 나눗셈이 되지 않는다. 0% 로 두면 전액 초과인 참여자의 막대가
     * 텅 비어 정상으로 보인다.
     */
    @Test
    @DisplayName("한도 0원 무지출 챌린지는 한 푼만 써도 100% 다")
    void zeroLimitCountsAnySpendAsFull() {
        givenGroup(0);
        givenIndictments();
        givenConsumption(consumption(USER_ID, "0"), consumption(9L, "500"));

        ChallengeGroupDetailDto detail = service().findDetail(USER_ID, GROUP_ID);

        assertEquals(0, detail.getMyUsagePercent());
        assertEquals(100, detail.getDailyMembers().get(0).getUsagePercent());
        assertTrue(detail.getDailyMembers().get(0).isExceeded());
    }

    /**
     * 초과 판정은 반올림한 퍼센트가 아니라 금액으로 한다.
     * 9,996원/10,000원은 100% 로 표시되지만 초과는 아니다 — 사이렌이 잘못 켜지면 안 된다.
     */
    @Test
    @DisplayName("100% 로 표시돼도 금액이 한도 이하면 초과가 아니다")
    void exceededIsDecidedByAmountNotRoundedPercent() {
        givenGroup(10_000);
        givenIndictments();
        givenConsumption(consumption(USER_ID, "0"), consumption(9L, "9996"));

        GroupDailyMemberDto member = service().findDetail(USER_ID, GROUP_ID).getDailyMembers().get(0);

        assertEquals(100, member.getUsagePercent());
        assertFalse(member.isExceeded());
    }

    /**
     * 배지는 재판 카드에서 끌어온다. 같은 것을 세는 쿼리를 하나 더 두면
     * 「카드에는 재판이 있는데 배지는 없는」 상태가 생긴다.
     */
    @Test
    @DisplayName("재판 중인 멤버에게만 재판 상태가 붙는다")
    void tagsTrialStatusFromIndictments() {
        givenGroup(10_000);
        givenIndictments(card(9L, "VOTING"));
        givenConsumption(consumption(USER_ID, "0"), consumption(9L, "12000"), consumption(10L, "3000"));

        List<GroupDailyMemberDto> members = service().findDetail(USER_ID, GROUP_ID).getDailyMembers();

        assertEquals("VOTING", members.get(0).getTrialStatus());
        assertNull(members.get(1).getTrialStatus());
    }

    /**
     * 집계 행이 아직 없는 참여자(첫날 오전 등)를 매퍼가 0원으로 채워 준다.
     * 그래도 NULL 이 오면 사용률 계산에서 NPE 로 상세 화면 전체가 죽는다.
     */
    @Test
    @DisplayName("소비 집계가 비어 있어도 0원으로 내려간다")
    void treatsMissingConsumptionAsZero() {
        GroupMemberConsumptionRow me = consumption(USER_ID, "0");
        me.setAmount(null);
        GroupMemberConsumptionRow other = consumption(9L, "0");
        other.setAmount(null);
        givenGroup(10_000);
        givenIndictments();
        givenConsumption(me, other);

        ChallengeGroupDetailDto detail = service().findDetail(USER_ID, GROUP_ID);

        assertEquals(0, detail.getMyDailyAmount().signum());
        assertEquals(0, detail.getMyUsagePercent());
        assertEquals(0, detail.getDailyMembers().get(0).getDailyAmount().signum());
    }

    /* ══ 종료 화면 필드 (이슈 #173) ═══════════════════════ */

    private GroupMember finalMember(long userId, int finalRank, String outcome, String imageKey) {
        return GroupMember.builder()
                .groupId(GROUP_ID)
                .userId(userId)
                .livesCount(2)
                .finalRank(finalRank)
                .finalOutcome(outcome)
                .nickname("절약왕" + userId)
                .profileImageKey(imageKey)
                .build();
    }

    /**
     * 확정값을 읽기만 하고({@code final_rank} 그대로), 정렬만 서비스가 한다 —
     * {@code findByGroupIds} 는 다른 화면 때문에 {@code user_id} 순으로 온다.
     */
    @Test
    @DisplayName("종료 그룹은 최종 결과를 final_rank 순으로 채운다")
    void closedGroupFillsFinalMembersOrderedByRank() {
        givenGroup(10_000, "CLOSED");
        givenIndictments();
        givenConsumption();
        when(groupMemberMapper.findByGroupIds(List.of(GROUP_ID))).thenReturn(List.of(
                finalMember(7L, 2, "SURVIVED", null),
                finalMember(9L, 1, "SURVIVED", "profile/9.png"),
                finalMember(10L, 3, "ELIMINATED", null)));
        when(imageStorage.urlOf("profile/9.png")).thenReturn("https://img/profile/9.png");
        GroupTrialStatsRow stats = new GroupTrialStatsRow();
        stats.setTotalTrials(3);
        stats.setGuiltyCount(2);
        stats.setInnocentCount(1);
        when(indictmentMapper.findClosedTrialStats(GROUP_ID)).thenReturn(stats);

        ChallengeGroupDetailDto detail = service().findDetail(USER_ID, GROUP_ID);

        assertEquals(List.of(9L, 7L, 10L), detail.getFinalMembers().stream()
                .map(ChallengeGroupDetailDto.FinalMember::getUserId).toList());
        assertEquals("ELIMINATED", detail.getFinalMembers().get(2).getFinalOutcome());
        assertEquals("https://img/profile/9.png", detail.getFinalMembers().get(0).getProfileImageUrl());
        assertEquals(3, detail.getTrialStats().getTotalTrials());
        assertEquals(2, detail.getTrialStats().getGuiltyCount());
        assertEquals(1, detail.getTrialStats().getInnocentCount());
    }

    /**
     * CLOSED 전에는 {@code final_*} 가 NULL 이라 만들 수 있는 값이 없다.
     * 진행 중 화면이 빈 포디움을 그리면 안 되므로 필드째 NULL 로 내린다.
     */
    @Test
    @DisplayName("진행 중 그룹은 종료 화면 필드를 채우지 않는다")
    void activeGroupLeavesClosedFieldsNull() {
        givenGroup(10_000);
        givenIndictments();
        givenConsumption(consumption(USER_ID, "6000"));

        ChallengeGroupDetailDto detail = service().findDetail(USER_ID, GROUP_ID);

        assertNull(detail.getFinalMembers());
        assertNull(detail.getTrialStats());
        verify(groupMemberMapper, never()).findByGroupIds(List.of(GROUP_ID));
        verify(indictmentMapper, never()).findClosedTrialStats(GROUP_ID);
    }
}
