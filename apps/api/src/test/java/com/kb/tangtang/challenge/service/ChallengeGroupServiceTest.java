package com.kb.tangtang.challenge.service;

import com.kb.tangtang.challenge.domain.ChallengeGroup;
import com.kb.tangtang.challenge.domain.GroupMember;
import com.kb.tangtang.challenge.dto.ChallengeGroupCreateRequestDto;
import com.kb.tangtang.challenge.dto.ChallengeGroupCreatedDto;
import com.kb.tangtang.challenge.dto.ChallengeGroupDto;
import com.kb.tangtang.challenge.dto.InviteCodePreviewDto;
import com.kb.tangtang.challenge.mapper.ChallengeGroupMapper;
import com.kb.tangtang.challenge.mapper.GroupMemberMapper;
import com.kb.tangtang.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChallengeGroupServiceTest {

    private static final long OWNER_ID = 1L;
    private static final long GUEST_ID = 2L;
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 12);

    private FakeGroupMapper groupMapper;
    private FakeMemberMapper memberMapper;
    private ChallengeGroupService service;

    @BeforeEach
    void setUp() {
        groupMapper = new FakeGroupMapper();
        memberMapper = new FakeMemberMapper();
        service = newService(TODAY);
    }

    private ChallengeGroupService newService(LocalDate today) {
        ZoneId zone = ZoneId.of("Asia/Seoul");
        Clock clock = Clock.fixed(today.atStartOfDay(zone).toInstant(), zone);
        return new ChallengeGroupService(groupMapper, memberMapper,
                new InviteCodeGenerator(groupMapper), clock);
    }

    /* ══ 생성 ══════════════════════════════════════════════ */

    @Test
    @DisplayName("생성하면 방장이 첫 참여자로 들어가고 일일평가 목숨은 기간 일수와 같다")
    void createRegistersOwnerAsMember() {
        ChallengeGroupCreatedDto created = service.create(OWNER_ID, request(r -> {
            r.setEvalType("DAILY");
            r.setEndDate(TODAY.plusDays(6));   // 7일
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
    }

    @Test
    @DisplayName("기간평가는 기간이 며칠이든 목숨이 1이다")
    void periodEvalGivesSingleLife() {
        ChallengeGroupCreatedDto created = service.create(OWNER_ID, request(r -> {
            r.setEvalType("PERIOD");
            r.setEndDate(TODAY.plusDays(6));
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
                () -> service.create(OWNER_ID, request(r -> r.setEndDate(TODAY.plusDays(7)))));

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

    @Test
    @DisplayName("시작일 다음 날부터는 만료다 — 만료 컬럼 없이 날짜로 판정한다")
    void expiresDayAfterStartDate() {
        String code = service.create(OWNER_ID, request(r -> {
            r.setStartDate(TODAY.plusDays(1));
            r.setEndDate(TODAY.plusDays(3));
        })).getInviteCode();

        InviteCodePreviewDto preview = newService(TODAY.plusDays(2)).previewInviteCode(GUEST_ID, code);

        assertFalse(preview.isJoinable());
        assertEquals("EXPIRED", preview.getReason());
        assertNotNull(preview.getChallenge(), "참여 불가여도 그룹 정보는 보여줘야 한다");
    }

    @Test
    @DisplayName("정원이 차면 FULL 사유로 내려간다 — 예외가 아니라 200 이다")
    void previewReportsFull() {
        ChallengeGroupCreatedDto created = service.create(OWNER_ID, request(r -> { }));
        for (long userId = 10L; userId < 15L; userId++) {
            service.join(userId, created.getGroupId());
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
            r.setEndDate(TODAY.plusDays(2));   // 3일
        }));

        ChallengeGroupDto detail = service.join(GUEST_ID, created.getGroupId());

        assertTrue(detail.isMember());
        assertEquals(3, detail.getLivesCount());
        assertEquals(2, detail.getMemberCount());
        assertFalse(detail.isOwner());
    }

    @Test
    @DisplayName("정원이 찬 뒤의 참여는 GROUP_FULL 로 거절한다")
    void joinRejectedWhenFull() {
        ChallengeGroupCreatedDto created = service.create(OWNER_ID, request(r -> { }));
        for (long userId = 10L; userId < 15L; userId++) {
            service.join(userId, created.getGroupId());
        }

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.join(GUEST_ID, created.getGroupId()));

        assertEquals("GROUP_FULL", e.getCode());
    }

    @Test
    @DisplayName("두 번 참여하면 GROUP_ALREADY_JOINED 로 거절한다")
    void joinRejectedWhenAlreadyMember() {
        ChallengeGroupCreatedDto created = service.create(OWNER_ID, request(r -> { }));

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.join(OWNER_ID, created.getGroupId()));

        assertEquals("GROUP_ALREADY_JOINED", e.getCode());
    }

    @Test
    @DisplayName("없는 그룹에 참여하면 GROUP_NOT_FOUND 다")
    void joinUnknownGroup() {
        BusinessException e = assertThrows(BusinessException.class, () -> service.join(GUEST_ID, 999L));

        assertEquals("GROUP_NOT_FOUND", e.getCode());
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

    /* ══ 픽스처 ════════════════════════════════════════════ */

    private interface RequestTweak {
        void apply(ChallengeGroupCreateRequestDto request);
    }

    private ChallengeGroupCreateRequestDto request(RequestTweak tweak) {
        ChallengeGroupCreateRequestDto request = new ChallengeGroupCreateRequestDto();
        request.setGroupName("커피값 줄이기");
        request.setLimitAmount(5000);
        request.setEvalType("DAILY");
        request.setStartDate(TODAY);
        request.setEndDate(TODAY.plusDays(2));
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
    }
}
