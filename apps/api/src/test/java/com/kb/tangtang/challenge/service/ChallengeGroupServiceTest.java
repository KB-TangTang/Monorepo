package com.kb.tangtang.challenge.service;

import com.kb.tangtang.challenge.chat.domain.ChatMessage;
import com.kb.tangtang.challenge.chat.domain.ChatMessageType;
import com.kb.tangtang.challenge.chat.store.ChatMessageStore;
import com.kb.tangtang.challenge.domain.ChallengeGroup;
import com.kb.tangtang.challenge.domain.GroupMember;
import com.kb.tangtang.challenge.domain.GroupTrialSummaryRow;
import com.kb.tangtang.challenge.dto.ChallengeGroupCreateRequestDto;
import com.kb.tangtang.challenge.dto.ChallengeGroupCreatedDto;
import com.kb.tangtang.challenge.dto.ChallengeGroupDto;
import com.kb.tangtang.challenge.dto.InviteCodePreviewDto;
import com.kb.tangtang.challenge.mapper.ChallengeGroupMapper;
import com.kb.tangtang.challenge.mapper.GroupMemberMapper;
import com.kb.tangtang.challenge.mapper.IndictmentMapper;
import com.kb.tangtang.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ChallengeGroupServiceTest {

    private static final long OWNER_ID = 1L;
    private static final long GUEST_ID = 2L;
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 12);

    /**
     * 기본 시작일. 생성은 <b>내일 이후</b>만 받는다(이슈 #350) — 모집이 시작일 23:59 에 닫히므로
     * 오늘 시작하면 모집 기간이 없어진다. 기간을 세는 테스트는 TODAY 가 아니라 이 날짜를 기준으로 잡는다.
     */
    private static final LocalDate START = TODAY.plusDays(1);

    private FakeGroupMapper groupMapper;
    private FakeMemberMapper memberMapper;
    private IndictmentMapper indictmentMapper;
    private ChatMessageStore chatMessageStore;
    private ChallengeGroupService service;

    @BeforeEach
    void setUp() {
        groupMapper = new FakeGroupMapper();
        memberMapper = new FakeMemberMapper();
        // 배지 집계는 목록 테스트에서만 쓴다. 기본값(빈 목록)이면 재판이 하나도 없는 평시와 같다.
        indictmentMapper = mock(IndictmentMapper.class);
        chatMessageStore = mock(ChatMessageStore.class);
        service = newService(TODAY);
    }

    private ChallengeGroupService newService(LocalDate today) {
        ZoneId zone = ZoneId.of("Asia/Seoul");
        Clock clock = Clock.fixed(today.atStartOfDay(zone).toInstant(), zone);
        return new ChallengeGroupService(groupMapper, memberMapper, indictmentMapper,
                new InviteCodeGenerator(groupMapper), chatMessageStore, clock);
    }

    /* ══ 생성 ══════════════════════════════════════════════ */

    @Test
    @DisplayName("생성하면 방장이 첫 참여자로 들어가고 일일평가 목숨은 기간 일수와 같다")
    void createRegistersOwnerAsMember() {
        ChallengeGroupCreatedDto created = service.create(OWNER_ID, request(r -> {
            r.setEvalType("DAILY");
            r.setEndDate(START.plusDays(6));   // 7일
        }));

        assertNotNull(created.getGroupId());
        assertEquals(5, created.getInviteCode().length(), "참여 코드 입력 UI 가 5칸이다");

        List<GroupMember> members = memberMapper.findByGroupIds(List.of(created.getGroupId()));
        assertEquals(1, members.size());
        assertEquals(OWNER_ID, members.get(0).getUserId());
        assertEquals(7, members.get(0).getLivesCount());

        ChallengeGroup saved = groupMapper.findById(created.getGroupId());
        assertEquals("RECRUITING", saved.getStatus());
        assertEquals(6, saved.getMaxMembers(), "정원은 6명 고정이다");

        verify(chatMessageStore).initRoom(created.getGroupId(), Set.of(OWNER_ID), saved.getEndDate());
    }

    @Test
    @DisplayName("채팅방 개설이 실패해도 챌린지 생성 자체는 성공한다 — Redis 장애가 본업을 막지 않는다")
    void createSucceedsEvenWhenChatRoomInitFails() {
        doThrow(new RuntimeException("Redis 연결 실패"))
                .when(chatMessageStore).initRoom(anyLong(), anySet(), any());

        ChallengeGroupCreatedDto created = service.create(OWNER_ID, request(r -> { }));

        assertNotNull(created.getGroupId());
        assertEquals("RECRUITING", groupMapper.findById(created.getGroupId()).getStatus());
    }

    @Test
    @DisplayName("기간평가는 기간이 며칠이든 목숨이 1이다")
    void periodEvalGivesSingleLife() {
        ChallengeGroupCreatedDto created = service.create(OWNER_ID, request(r -> {
            r.setEvalType("PERIOD");
            r.setEndDate(START.plusDays(6));
        }));

        assertEquals(1, memberMapper.findByGroupIds(List.of(created.getGroupId())).get(0).getLivesCount());
    }

    @Test
    @DisplayName("제한 금액 0원은 무지출 챌린지로 허용된다")
    void allowsZeroLimitAmount() {
        ChallengeGroupCreatedDto created = service.create(OWNER_ID, request(r -> r.setLimitAmount(0)));

        assertEquals(0, groupMapper.findById(created.getGroupId()).getLimitAmount());
    }

    @Test
    @DisplayName("제한 금액이 음수면 거절한다")
    void rejectsNegativeLimitAmount() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> service.create(OWNER_ID, request(r -> r.setLimitAmount(-1))));

        assertEquals("GROUP_LIMIT_AMOUNT_INVALID", e.getCode());
    }

    @Test
    @DisplayName("기간이 7일을 넘으면 거절한다 — ck_cg_period 위반을 DB 까지 보내지 않는다")
    void rejectsPeriodLongerThanSevenDays() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> service.create(OWNER_ID, request(r -> r.setEndDate(START.plusDays(7)))));

        assertEquals("GROUP_PERIOD_INVALID", e.getCode());
    }

    @Test
    @DisplayName("시작일이 어제면 거절한다")
    void rejectsPastStartDate() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> service.create(OWNER_ID, request(r -> {
                    r.setStartDate(TODAY.minusDays(1));
                    r.setEndDate(TODAY.plusDays(1));
                })));

        assertEquals("GROUP_START_DATE_INVALID", e.getCode());
    }

    /**
     * 이슈 #350. 모집은 시작일 23:59 에 닫힌다 — 오늘 시작을 허용하면 만든 시각부터 자정까지가
     * 모집 기간의 전부가 된다. 프론트 달력은 이미 내일부터만 고를 수 있어 API 직접 호출만 막으면 된다.
     */
    @Test
    @DisplayName("오늘 시작은 거절한다 — 모집 기간이 없어진다")
    void rejectsTodayAsStartDate() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> service.create(OWNER_ID, request(r -> {
                    r.setStartDate(TODAY);
                    r.setEndDate(TODAY.plusDays(2));
                })));

        assertEquals("GROUP_START_DATE_INVALID", e.getCode());
    }

    @Test
    @DisplayName("내일 시작은 허용한다")
    void allowsTomorrowAsStartDate() {
        ChallengeGroupCreatedDto created = service.create(OWNER_ID, request(r -> {
            r.setStartDate(TODAY.plusDays(1));
            r.setEndDate(TODAY.plusDays(3));
        }));

        assertEquals(TODAY.plusDays(1), groupMapper.findById(created.getGroupId()).getStartDate());
    }

    @Test
    @DisplayName("이름이 공백뿐이면 거절한다")
    void rejectsBlankName() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> service.create(OWNER_ID, request(r -> r.setGroupName("   "))));

        assertEquals("GROUP_NAME_REQUIRED", e.getCode());
    }

    @Test
    @DisplayName("알 수 없는 평가 방식은 거절한다")
    void rejectsUnknownEvalType() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> service.create(OWNER_ID, request(r -> r.setEvalType("WEEKLY"))));

        assertEquals("GROUP_EVAL_TYPE_INVALID", e.getCode());
    }

    /* ══ 초대 코드 미리보기 ═════════════════════════════════ */

    @Test
    @DisplayName("없는 초대 코드는 예외다")
    void unknownInviteCodeThrows() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> service.previewInviteCode(GUEST_ID, "ZZZZZ"));

        assertEquals("GROUP_INVITE_CODE_NOT_FOUND", e.getCode());
    }

    @Test
    @DisplayName("초대 코드는 대소문자를 가리지 않는다")
    void inviteCodeIsCaseInsensitive() {
        String code = service.create(OWNER_ID, request(r -> { })).getInviteCode();

        InviteCodePreviewDto preview = service.previewInviteCode(GUEST_ID, code.toLowerCase());

        assertTrue(preview.isJoinable());
    }

    @Test
    @DisplayName("시작일 당일까지는 모집한다")
    void joinableOnStartDate() {
        String code = service.create(OWNER_ID, request(r -> {
            r.setStartDate(TODAY.plusDays(1));
            r.setEndDate(TODAY.plusDays(3));
        })).getInviteCode();

        InviteCodePreviewDto preview = newService(TODAY.plusDays(1)).previewInviteCode(GUEST_ID, code);

        assertTrue(preview.isJoinable());
        assertNull(preview.getReason());
    }

    /**
     * 이슈 #350. 상태 전이 배치는 시작일 00:01 에 돌아 ACTIVE 로 바꾼다. status 만 보면 시작일
     * 하루가 통째로 막혀 「시작일 23:59까지 초대」가 거짓말이 된다.
     */
    @Test
    @DisplayName("ACTIVE 라도 오늘이 시작일이면 참여할 수 있다 — 시작일 23:59 까지 모집")
    void joinableOnStartDateEvenWhenActive() {
        ChallengeGroupCreatedDto created = service.create(OWNER_ID, request(r -> {
            r.setStartDate(TODAY.plusDays(1));
            r.setEndDate(TODAY.plusDays(3));
        }));
        groupMapper.updateStatusIfCurrent(created.getGroupId(), "RECRUITING", "ACTIVE");

        ChallengeGroupService onStartDate = newService(TODAY.plusDays(1));
        InviteCodePreviewDto preview = onStartDate.previewInviteCode(GUEST_ID, created.getInviteCode());

        assertTrue(preview.isJoinable());
        assertNull(preview.getReason());

        ChallengeGroupDto joined = onStartDate.join(GUEST_ID, created.getInviteCode());
        assertTrue(joined.isMember());
        assertEquals(2, joined.getMemberCount());
    }

    @Test
    @DisplayName("ACTIVE 이고 시작일이 지났으면 만료다 (#350 회귀 방지)")
    void expiresOnceBatchActivates() {
        ChallengeGroupCreatedDto created = service.create(OWNER_ID, request(r -> {
            r.setStartDate(TODAY.plusDays(1));
            r.setEndDate(TODAY.plusDays(3));
        }));
        groupMapper.updateStatusIfCurrent(created.getGroupId(), "RECRUITING", "ACTIVE");

        InviteCodePreviewDto preview = newService(TODAY.plusDays(2))
                .previewInviteCode(GUEST_ID, created.getInviteCode());

        assertFalse(preview.isJoinable());
        assertEquals("EXPIRED", preview.getReason());
        assertNotNull(preview.getChallenge(), "참여 불가여도 그룹 정보는 보여줘야 한다");
    }

    @Test
    @DisplayName("배치가 아직 안 돌았으면 시작일이 지나도 모집 중이다 — status 단일 기준의 대가(#152)")
    void stillJoinableUntilBatchRuns() {
        String code = service.create(OWNER_ID, request(r -> {
            r.setStartDate(TODAY.plusDays(1));
            r.setEndDate(TODAY.plusDays(3));
        })).getInviteCode();

        InviteCodePreviewDto preview = newService(TODAY.plusDays(2)).previewInviteCode(GUEST_ID, code);

        assertTrue(preview.isJoinable());
    }

    @Test
    @DisplayName("종료된 챌린지는 CLOSED 사유다")
    void previewReportsClosed() {
        ChallengeGroupCreatedDto created = service.create(OWNER_ID, request(r -> { }));
        groupMapper.updateStatusIfCurrent(created.getGroupId(), "RECRUITING", "CLOSED");

        InviteCodePreviewDto preview = service.previewInviteCode(GUEST_ID, created.getInviteCode());

        assertFalse(preview.isJoinable());
        assertEquals("CLOSED", preview.getReason());
    }

    @Test
    @DisplayName("정원이 차면 FULL 사유로 내려간다 — 예외가 아니라 200 이다")
    void previewReportsFull() {
        ChallengeGroupCreatedDto created = service.create(OWNER_ID, request(r -> { }));
        for (long userId = 10L; userId < 15L; userId++) {
            service.join(userId, created.getInviteCode());
        }

        InviteCodePreviewDto preview = service.previewInviteCode(GUEST_ID, created.getInviteCode());

        assertFalse(preview.isJoinable());
        assertEquals("FULL", preview.getReason());
    }

    @Test
    @DisplayName("이미 참여 중이면 ALREADY_JOINED 를 먼저 알린다")
    void previewReportsAlreadyJoined() {
        ChallengeGroupCreatedDto created = service.create(OWNER_ID, request(r -> { }));

        InviteCodePreviewDto preview = service.previewInviteCode(OWNER_ID, created.getInviteCode());

        assertFalse(preview.isJoinable());
        assertEquals("ALREADY_JOINED", preview.getReason());
        assertTrue(preview.getChallenge().isMember());
    }

    /* ══ 참여 ══════════════════════════════════════════════ */

    @Test
    @DisplayName("참여하면 목숨이 부여되고 상세가 바로 돌아온다")
    void joinGrantsLives() {
        ChallengeGroupCreatedDto created = service.create(OWNER_ID, request(r -> {
            r.setEvalType("DAILY");
            r.setEndDate(START.plusDays(2));   // 3일
        }));

        ChallengeGroupDto detail = service.join(GUEST_ID, created.getInviteCode());

        assertTrue(detail.isMember());
        assertEquals(3, detail.getLivesCount());
        assertEquals(2, detail.getMemberCount());
        assertFalse(detail.isOwner());

        verify(chatMessageStore).cacheMembers(created.getGroupId(), Set.of(GUEST_ID), detail.getEndDate());
    }

    @Test
    @DisplayName("참여 차단 시에는 채팅방 캐시를 건드리지 않는다")
    void joinBlockedDoesNotTouchChatCache() {
        ChallengeGroupCreatedDto created = service.create(OWNER_ID, request(r -> { }));
        clearInvocations(chatMessageStore);

        assertThrows(BusinessException.class, () -> service.join(OWNER_ID, created.getInviteCode()));

        verifyNoInteractions(chatMessageStore);
    }

    @Test
    @DisplayName("정원이 찬 뒤의 참여는 GROUP_FULL 로 거절한다")
    void joinRejectedWhenFull() {
        ChallengeGroupCreatedDto created = service.create(OWNER_ID, request(r -> { }));
        for (long userId = 10L; userId < 15L; userId++) {
            service.join(userId, created.getInviteCode());
        }

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.join(GUEST_ID, created.getInviteCode()));

        assertEquals("GROUP_FULL", e.getCode());
    }

    @Test
    @DisplayName("두 번 참여하면 GROUP_ALREADY_JOINED 로 거절한다")
    void joinRejectedWhenAlreadyMember() {
        ChallengeGroupCreatedDto created = service.create(OWNER_ID, request(r -> { }));

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.join(OWNER_ID, created.getInviteCode()));

        assertEquals("GROUP_ALREADY_JOINED", e.getCode());
    }

    @Test
    @DisplayName("없는 초대 코드로 참여하면 GROUP_INVITE_CODE_NOT_FOUND 다")
    void joinUnknownInviteCode() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> service.join(GUEST_ID, "NOPE9"));

        assertEquals("GROUP_INVITE_CODE_NOT_FOUND", e.getCode());
    }

    @Test
    @DisplayName("빈 초대 코드도 GROUP_INVITE_CODE_NOT_FOUND 다 — 코드 없이 들어올 길을 남기지 않는다")
    void joinBlankInviteCode() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> service.join(GUEST_ID, "   "));

        assertEquals("GROUP_INVITE_CODE_NOT_FOUND", e.getCode());
    }

    @Test
    @DisplayName("참여도 초대 코드의 대소문자를 가리지 않는다 — 미리보기와 같은 정규화를 탄다")
    void joinIsCaseInsensitive() {
        ChallengeGroupCreatedDto created = service.create(OWNER_ID, request(r -> { }));

        ChallengeGroupDto detail = service.join(GUEST_ID, created.getInviteCode().toLowerCase());

        assertTrue(detail.isMember());
        assertEquals(2, detail.getMemberCount());
    }

    /**
     * 이슈 #346 의 회귀 방지.
     *
     * <p>예전에는 참여가 {@code groupId} 를 받아 <b>초대 코드 없이도 참여할 수 있었다.</b> 코드를
     * 그룹을 찾는 유일한 수단으로 만든 지금은, 남의 그룹 코드를 모르면 그 그룹에 닿을 방법이 없다.
     * groupId 를 문자열로 밀어 넣어도 코드로 해석돼 조회에 실패한다.
     */
    @Test
    @DisplayName("groupId 를 코드 자리에 넣어도 참여되지 않는다 (#346)")
    void joinRejectsGroupIdInPlaceOfInviteCode() {
        ChallengeGroupCreatedDto created = service.create(OWNER_ID, request(r -> { }));

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.join(GUEST_ID, String.valueOf(created.getGroupId())));

        assertEquals("GROUP_INVITE_CODE_NOT_FOUND", e.getCode());
        assertEquals(1, memberMapper.findByGroupIds(List.of(created.getGroupId())).size(),
                "참여자가 늘어나면 안 된다");
    }

    /* ══ 조회 ══════════════════════════════════════════════ */

    @Test
    @DisplayName("시작 전에는 daysUntilStart 만, 진행 중에는 currentDay 만 채운다")
    void derivesDayFields() {
        service.create(OWNER_ID, request(r -> {
            r.setStartDate(TODAY.plusDays(2));
            r.setEndDate(TODAY.plusDays(4));
        }));

        ChallengeGroupDto beforeStart = service.findMyGroups(OWNER_ID, null).get(0);
        assertEquals(2, beforeStart.getDaysUntilStart());
        assertNull(beforeStart.getCurrentDay());
        assertEquals(3, beforeStart.getTotalDays());
        assertEquals(3, beforeStart.getMaxLives());

        ChallengeGroupDto onDayTwo = newService(TODAY.plusDays(3)).findMyGroups(OWNER_ID, null).get(0);
        assertNull(onDayTwo.getDaysUntilStart());
        assertEquals(2, onDayTwo.getCurrentDay());

        ChallengeGroupDto afterEnd = newService(TODAY.plusDays(5)).findMyGroups(OWNER_ID, null).get(0);
        assertNull(afterEnd.getCurrentDay());
        assertNull(afterEnd.getDaysUntilStart());
    }

    @Test
    @DisplayName("알 수 없는 상태 필터는 빈 목록이 아니라 400 이다")
    void rejectsUnknownStatusFilter() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> service.findMyGroups(OWNER_ID, List.of("FINISHED")));

        assertEquals("GROUP_STATUS_INVALID", e.getCode());
    }

    @Test
    @DisplayName("상태 필터는 대소문자를 가리지 않고 매퍼로 넘어간다")
    void normalizesStatusFilter() {
        service.findMyGroups(OWNER_ID, List.of("judging", "closed"));

        assertEquals(List.of("JUDGING", "CLOSED"), groupMapper.lastStatuses);
    }

    /* ══ 목록 카드의 재판 배지 (이슈 #169) ═════════════════ */

    private void givenTrialSummary(long groupId, int defenseNeeded, int pendingVote, int castVote) {
        GroupTrialSummaryRow row = new GroupTrialSummaryRow();
        row.setGroupId(groupId);
        row.setMyDefenseNeededCount(defenseNeeded);
        row.setPendingVoteCount(pendingVote);
        row.setCastVoteCount(castVote);
        when(indictmentMapper.findTrialSummaryByGroupIds(eq(OWNER_ID), anyList()))
                .thenReturn(List.of(row));
    }

    /**
     * 재판이 없는 그룹은 집계 쿼리가 <b>행을 내려주지 않는다</b>. 이 자리에서 NULL 이 새면
     * 재판이 하나도 없는 평시에 목록 API 가 통째로 500 이 된다.
     */
    @Test
    @DisplayName("재판이 없으면 배지 없이 기본값으로 내려간다")
    void listWithoutTrialsFallsBackToDefaults() {
        service.create(OWNER_ID, request(r -> { }));

        ChallengeGroupDto card = service.findMyGroups(OWNER_ID, null).get(0);

        assertEquals(0, card.getPendingTrialCount());
        assertFalse(card.isDefendant());
        assertNull(card.getMyVoteStatus());
    }

    /**
     * 「변론필요」는 <b>내가 아직 변론을 안 낸</b> 기소만 센다 — 세는 일은 SQL 이 한다.
     * 여기서는 0 보다 크면 참으로 접히는지만 본다.
     */
    @Test
    @DisplayName("내 변론이 남아 있으면 defendant 가 참이다")
    void listMarksDefendant() {
        ChallengeGroupCreatedDto created = service.create(OWNER_ID, request(r -> { }));
        givenTrialSummary(created.getGroupId(), 1, 0, 0);

        ChallengeGroupDto card = service.findMyGroups(OWNER_ID, null).get(0);

        assertTrue(card.isDefendant());
    }

    /**
     * 안 던진 표가 남아 있으면 이미 던진 표가 있어도 {@code PENDING} 이다.
     * 완료 쪽이 이기면 「투표완료」 배지 뒤로 남은 할 일이 숨는다.
     */
    @Test
    @DisplayName("던질 표가 남아 있으면 이미 던진 표가 있어도 PENDING 이다")
    void listPrefersPendingOverDone() {
        ChallengeGroupCreatedDto created = service.create(OWNER_ID, request(r -> { }));
        givenTrialSummary(created.getGroupId(), 0, 2, 1);

        ChallengeGroupDto card = service.findMyGroups(OWNER_ID, null).get(0);

        assertEquals("PENDING", card.getMyVoteStatus());
        assertEquals(2, card.getPendingTrialCount());
    }

    /**
     * 「투표완료」와 「순항중」은 둘 다 {@code pendingTrialCount == 0} 이다.
     * 그 하나로 갈음하면 표를 다 던진 그룹이 재판 없는 그룹처럼 보인다.
     */
    @Test
    @DisplayName("표를 다 던졌으면 DONE, 재판 자체가 없으면 NULL 이다")
    void listSeparatesDoneFromNoTrial() {
        ChallengeGroupCreatedDto created = service.create(OWNER_ID, request(r -> { }));
        givenTrialSummary(created.getGroupId(), 0, 0, 3);

        ChallengeGroupDto card = service.findMyGroups(OWNER_ID, null).get(0);

        assertEquals(0, card.getPendingTrialCount());
        assertEquals("DONE", card.getMyVoteStatus());
    }

    /**
     * 상세는 같은 응답의 {@code indictments} 로 배지를 판단한다. 여기서 한 번 더 세면
     * 화면 하나에 같은 것을 두 번 묻는 쿼리가 생긴다.
     */
    @Test
    @DisplayName("상세는 재판 배지를 세지 않는다")
    void detailDoesNotCountTrialBadges() {
        ChallengeGroupCreatedDto created = service.create(OWNER_ID, request(r -> { }));

        ChallengeGroupDto detail = service.findDetail(OWNER_ID, created.getGroupId());

        assertNull(detail.getMyVoteStatus());
        verifyNoInteractions(indictmentMapper);
    }

    @Test
    @DisplayName("참여자가 아니면 상세를 볼 수 없다 — 비참여자는 초대 코드 경로를 쓴다")
    void detailRequiresMembership() {
        ChallengeGroupCreatedDto created = service.create(OWNER_ID, request(r -> { }));

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.findDetail(GUEST_ID, created.getGroupId()));

        assertEquals("GROUP_NOT_MEMBER", e.getCode());
    }

    @Test
    @DisplayName("상세에는 메모와 방장 여부가 담긴다")
    void detailCarriesMemoAndOwnership() {
        ChallengeGroupCreatedDto created = service.create(OWNER_ID, request(r -> r.setMemo("꼴찌가 커피 쏘기")));

        ChallengeGroupDto detail = service.findDetail(OWNER_ID, created.getGroupId());

        assertEquals("꼴찌가 커피 쏘기", detail.getMemo());
        assertTrue(detail.isOwner());
        assertTrue(detail.getMembers().get(0).isOwner());
        assertFalse(detail.isJoinable(), "이미 참여 중이면 다시 참여할 수 없다");
    }

    /* ══ 채팅 요약 (이슈 #271) ══════════════════════════════ */

    @Test
    @DisplayName("목록·상세에 안 읽은 수와 마지막 메시지가 함께 실린다 — 방에 들어가야만 알 수 있던 값이다")
    void carriesChatSummary() {
        ChallengeGroupCreatedDto created = service.create(OWNER_ID, request(r -> { }));
        LocalDateTime sentAt = LocalDateTime.of(2026, 8, 12, 14, 3);
        when(chatMessageStore.findRecent(created.getGroupId(), 1))
                .thenReturn(List.of(chatMessage("나 오늘 진짜 참았다", sentAt)));
        when(chatMessageStore.unreadOf(created.getGroupId(), OWNER_ID)).thenReturn(3);

        ChallengeGroupDto listed = service.findMyGroups(OWNER_ID, null).get(0);
        assertEquals(3, listed.getUnreadChatCount());
        assertEquals("나 오늘 진짜 참았다", listed.getLastChatMessage());
        assertEquals(sentAt, listed.getLastChatTime());

        ChallengeGroupDto detail = service.findDetail(OWNER_ID, created.getGroupId());
        assertEquals(3, detail.getUnreadChatCount(), "상세 FAB 배지도 같은 필드를 본다");
    }

    @Test
    @DisplayName("대화가 없으면 빈 값이고 안 읽은 수는 조회하지도 않는다")
    void emptyChatSummaryWhenNoMessage() {
        service.create(OWNER_ID, request(r -> { }));

        ChallengeGroupDto listed = service.findMyGroups(OWNER_ID, null).get(0);

        assertEquals(0, listed.getUnreadChatCount());
        assertNull(listed.getLastChatMessage());
        assertNull(listed.getLastChatTime());
        verify(chatMessageStore, never()).unreadOf(anyLong(), anyLong());
    }

    @Test
    @DisplayName("Redis 가 죽어도 목록은 뜬다 — 채팅 요약만 비운다")
    void listSurvivesChatStoreFailure() {
        service.create(OWNER_ID, request(r -> { }));
        when(chatMessageStore.findRecent(anyLong(), anyInt()))
                .thenThrow(new RuntimeException("Redis 연결 실패"));

        List<ChallengeGroupDto> groups = service.findMyGroups(OWNER_ID, null);

        assertEquals(1, groups.size(), "채팅은 부가 정보다. 재판 목록 화면 전체가 함께 죽으면 안 된다");
        assertEquals(0, groups.get(0).getUnreadChatCount());
        assertNull(groups.get(0).getLastChatMessage());
    }

    @Test
    @DisplayName("초대 코드 미리보기에는 마지막 대화가 새어 나가지 않는다 — 비참여자다")
    void previewHidesChatSummary() {
        String code = service.create(OWNER_ID, request(r -> { })).getInviteCode();
        when(chatMessageStore.findRecent(anyLong(), anyInt()))
                .thenReturn(List.of(chatMessage("우리끼리 하는 얘기", LocalDateTime.of(2026, 8, 12, 9, 0))));

        InviteCodePreviewDto preview = service.previewInviteCode(GUEST_ID, code);

        assertNull(preview.getChallenge().getLastChatMessage());
        assertEquals(0, preview.getChallenge().getUnreadChatCount());
        verify(chatMessageStore, never()).unreadOf(anyLong(), anyLong());
    }

    /* ══ 픽스처 ════════════════════════════════════════════ */

    private ChatMessage chatMessage(String content, LocalDateTime sentAt) {
        return ChatMessage.of(1L, ChatMessageType.TEXT, OWNER_ID, "요롱이", content, sentAt);
    }

    private interface RequestTweak {
        void apply(ChallengeGroupCreateRequestDto request);
    }

    private ChallengeGroupCreateRequestDto request(RequestTweak tweak) {
        ChallengeGroupCreateRequestDto request = new ChallengeGroupCreateRequestDto();
        request.setGroupName("커피값 줄이기");
        request.setLimitAmount(5000);
        request.setEvalType("DAILY");
        request.setStartDate(START);
        request.setEndDate(START.plusDays(2));
        tweak.apply(request);
        return request;
    }

    private static class FakeGroupMapper implements ChallengeGroupMapper {
        private final List<ChallengeGroup> groups = new ArrayList<>();
        private long sequence;
        private List<String> lastStatuses;

        @Override
        public int insertGroup(ChallengeGroup group) {
            group.setId(++sequence);   // useGeneratedKeys 흉내
            groups.add(group);
            return 1;
        }

        @Override
        public ChallengeGroup findById(Long groupId) {
            return groups.stream()
                    .filter(g -> g.getId().equals(groupId))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public ChallengeGroup findByInviteCode(String inviteCode) {
            return groups.stream()
                    .filter(g -> g.getInviteCode().equals(inviteCode))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public List<ChallengeGroup> findMyGroups(Long userId, List<String> statuses) {
            lastStatuses = statuses;
            return groups.stream()
                    .filter(g -> statuses == null || statuses.contains(g.getStatus()))
                    .toList();
        }

        @Override
        public int countByInviteCode(String inviteCode) {
            return (int) groups.stream().filter(g -> g.getInviteCode().equals(inviteCode)).count();
        }

        @Override
        public List<ChallengeGroup> findGroupsToStart(String status, LocalDate today) {
            return groups.stream()
                    .filter(g -> g.getStatus().equals(status))
                    .filter(g -> !g.getStartDate().isAfter(today))
                    .toList();
        }

        @Override
        public List<ChallengeGroup> findGroupsToJudge(String status, LocalDate endedBefore) {
            return groups.stream()
                    .filter(g -> g.getStatus().equals(status))
                    .filter(g -> g.getEndDate().isBefore(endedBefore))
                    .toList();
        }

        @Override
        public List<ChallengeGroup> findGroupsToEvaluate(String status, LocalDate today, LocalDate yesterday) {
            return groups.stream()
                    .filter(g -> g.getStatus().equals(status))
                    .filter(g -> !g.getStartDate().isAfter(today))
                    .filter(g -> !g.getEndDate().isBefore(yesterday))
                    .toList();
        }

        @Override
        public int updateStatusIfCurrent(Long groupId, String fromStatus, String toStatus) {
            for (int i = 0; i < groups.size(); i++) {
                ChallengeGroup group = groups.get(i);
                if (group.getId().equals(groupId) && group.getStatus().equals(fromStatus)) {
                    // status 에는 세터가 없다(배치 SQL 만 바꾸라는 뜻). 복제해 갈아 끼운다.
                    groups.set(i, ChallengeGroup.builder()
                            .id(group.getId())
                            .adminId(group.getAdminId())
                            .groupName(group.getGroupName())
                            .categoryId(group.getCategoryId())
                            .limitAmount(group.getLimitAmount())
                            .evalType(group.getEvalType())
                            .maxMembers(group.getMaxMembers())
                            .startDate(group.getStartDate())
                            .endDate(group.getEndDate())
                            .inviteCode(group.getInviteCode())
                            .status(toStatus)
                            .memo(group.getMemo())
                            .categoryName(group.getCategoryName())
                            .build());
                    return 1;
                }
            }
            return 0;   // compare-and-set 실패 — 배치 멱등성이 이 0 에 걸려 있다
        }

        @Override
        public int deleteIfCurrent(Long groupId, String status) {
            return groups.removeIf(g -> g.getId().equals(groupId) && g.getStatus().equals(status)) ? 1 : 0;
        }

        /** 최종 확정 배치 전용(#172). 이 테스트가 보는 흐름과 무관하다. */
        @Override
        public List<ChallengeGroup> findGroupsToClose(String status) {
            return List.of();
        }
    }

    private static class FakeMemberMapper implements GroupMemberMapper {
        private final List<GroupMember> members = new ArrayList<>();

        @Override
        public int insertMember(GroupMember member) {
            members.add(member);
            return 1;
        }

        @Override
        public List<GroupMember> findByGroupIds(List<Long> groupIds) {
            return members.stream()
                    .filter(m -> groupIds.contains(m.getGroupId()))
                    .toList();
        }

        @Override
        public List<Long> findUserIdsByGroupId(long groupId) {
            return members.stream()
                    .filter(m -> m.getGroupId() == groupId)
                    .map(GroupMember::getUserId)
                    .toList();
        }

        /** 목숨 차감(이슈 #172)은 이 테스트의 관심사가 아니다. 그룹 생성·참여만 본다. */
        @Override
        public int decreaseLife(Long groupId, Long userId) {
            return 0;
        }

        /** 최종 결과 확정(#172)도 마찬가지다. */
        @Override
        public int finalizeMember(Long groupId, Long userId, String finalOutcome,
                                  Integer finalRank, BigDecimal finalChargeAmount) {
            return 0;
        }
    }
}
