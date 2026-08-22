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
import com.kb.tangtang.notification.domain.NotificationRequestedEvent;
import com.kb.tangtang.challenge.dto.GroupMemberDto;
import com.kb.tangtang.common.storage.ImageStorage;
import com.kb.tangtang.notification.domain.NotificationType;
import com.kb.tangtang.transaction.domain.Category;
import com.kb.tangtang.transaction.mapper.CategoryMapper;
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

    /** 소분류 id. {@code tbl_category} 는 AUTO_INCREMENT 라 실제 값은 환경마다 다르다 — 여기선 관계 구조만 흉내낸다. */
    private static final long SUB_CATEGORY_ID = 11L;

    /** 대분류 id. 그룹 챌린지가 이 값을 쓰면 거래가 하나도 매칭되지 않아 집계가 영원히 0원이다(이슈 #352). */
    private static final long PARENT_CATEGORY_ID = 1L;

    private FakeGroupMapper groupMapper;
    private FakeMemberMapper memberMapper;
    private IndictmentMapper indictmentMapper;
    private FakeCategoryMapper categoryMapper;
    private ChatMessageStore chatMessageStore;
    private final List<NotificationRequestedEvent> published = new ArrayList<>();
    private ChallengeGroupService service;

    @BeforeEach
    void setUp() {
        groupMapper = new FakeGroupMapper();
        memberMapper = new FakeMemberMapper();
        // 배지 집계는 목록 테스트에서만 쓴다. 기본값(빈 목록)이면 재판이 하나도 없는 평시와 같다.
        indictmentMapper = mock(IndictmentMapper.class);
        categoryMapper = new FakeCategoryMapper();
        chatMessageStore = mock(ChatMessageStore.class);
        published.clear();
        service = newService(TODAY);
    }

    /**
     * 프로필 이미지 저장소 대역. 키를 그대로 URL 자리에 흘려보내, 서비스가 **키가 아니라
     * 변환된 값을 내려주는지**만 본다. 실제 URL 규칙은 ImageStorage 구현의 몫이다.
     */
    private static final ImageStorage IMAGE_STORAGE = new ImageStorage() {
        @Override
        public String store(byte[] content, String key) {
            return key;
        }

        @Override
        public void delete(String key) {
        }

        @Override
        public String urlOf(String key) {
            return key == null ? null : "https://img.test/" + key;
        }
    };

    private ChallengeGroupService newService(LocalDate today) {
        ZoneId zone = ZoneId.of("Asia/Seoul");
        Clock clock = Clock.fixed(today.atStartOfDay(zone).toInstant(), zone);
        return new ChallengeGroupService(groupMapper, memberMapper, indictmentMapper, categoryMapper,
                new InviteCodeGenerator(groupMapper), chatMessageStore,
                new ChallengeGroupDeleter(groupMapper, chatMessageStore),
                event -> published.add((NotificationRequestedEvent) event), IMAGE_STORAGE, clock);
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

    /* ══ 카테고리 검증 (이슈 #352) ═══════════════════════════ */

    @Test
    @DisplayName("카테고리 없음(총 소비)은 그대로 통과한다")
    void allowsNullCategory() {
        ChallengeGroupCreatedDto created = service.create(OWNER_ID, request(r -> r.setCategoryId(null)));

        assertNull(groupMapper.findById(created.getGroupId()).getCategoryId());
    }

    @Test
    @DisplayName("소분류는 허용한다")
    void allowsSubCategory() {
        ChallengeGroupCreatedDto created =
                service.create(OWNER_ID, request(r -> r.setCategoryId(SUB_CATEGORY_ID)));

        assertEquals(SUB_CATEGORY_ID, groupMapper.findById(created.getGroupId()).getCategoryId());
    }

    /**
     * <b>#352 회귀 방지.</b> 생성 화면이 대분류 id 를 하드코딩해 두고 있었다.
     * tbl_transaction.category_id 에는 소분류만 들어가는데 결과 집계는 정확히 일치하는 값만 세므로,
     * 대분류로 만든 그룹은 매칭 거래가 영원히 0건 — 오류 하나 없이 「0원」만 뜨고 아무도 기소되지 않는다.
     */
    @Test
    @DisplayName("대분류는 거절한다 — 집계가 조용히 0원이 되는 것을 생성 시점에 막는다")
    void rejectsParentCategory() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> service.create(OWNER_ID, request(r -> r.setCategoryId(PARENT_CATEGORY_ID))));

        assertEquals("GROUP_CATEGORY_INVALID", e.getCode());
    }

    @Test
    @DisplayName("없는 카테고리 id 는 거절한다")
    void rejectsUnknownCategory() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> service.create(OWNER_ID, request(r -> r.setCategoryId(9999L))));

        assertEquals("GROUP_CATEGORY_INVALID", e.getCode());
    }

    /* ══ 삭제 (이슈 #352) ══════════════════════════════════ */

    @Test
    @DisplayName("방장이 모집 중인 그룹을 지우면 그룹과 채팅방이 함께 사라진다")
    void ownerDeletesRecruitingGroup() {
        long groupId = service.create(OWNER_ID, request(r -> { })).getGroupId();

        service.deleteGroup(OWNER_ID, groupId);

        assertNull(groupMapper.findById(groupId));
        verify(chatMessageStore).deleteRoom(groupId);
    }

    @Test
    @DisplayName("삭제하면 방장을 뺀 남은 참여자에게만 알림이 간다")
    void deleteNotifiesRemainingMembersOnly() {
        String code = service.create(OWNER_ID, request(r -> { })).getInviteCode();
        service.join(GUEST_ID, code);
        long groupId = groupMapper.findByInviteCode(code).getId();
        published.clear();

        service.deleteGroup(OWNER_ID, groupId);

        assertEquals(1, published.size(), "방장은 자기가 누른 결과라 알림이 필요 없다");
        assertEquals(GUEST_ID, published.get(0).userId());
        assertEquals(NotificationType.GROUP_CHALLENGE_DELETED, published.get(0).type());
        assertEquals("커피값 줄이기", published.get(0).params().get("groupName"));
        assertEquals("/group-challenges", published.get(0).deepLinkUrl(),
                "그룹이 사라졌으므로 상세가 아니라 홈으로 보낸다");
    }

    @Test
    @DisplayName("방장이 아니면 삭제할 수 없다")
    void nonOwnerCannotDelete() {
        String code = service.create(OWNER_ID, request(r -> { })).getInviteCode();
        service.join(GUEST_ID, code);
        long groupId = groupMapper.findByInviteCode(code).getId();

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.deleteGroup(GUEST_ID, groupId));

        assertEquals("GROUP_NOT_OWNER", e.getCode());
        assertNotNull(groupMapper.findById(groupId), "차단됐으면 그룹이 남아 있어야 한다");
        assertTrue(published.isEmpty());
    }

    /**
     * 참여자들의 소비 집계·기소·투표가 이미 쌓여 있어, 한 사람의 결정으로 남의 기록까지
     * CASCADE 로 지우게 된다. ACTIVE 이후를 열어 줄지 검토했으나 2026-08-20 팀 논의에서
     * 열지 않기로 확정했다 - 챌린지가 최대 7일이라 오래 매여 있지 않고, 진행 중인 재판과
     * 집계가 갑자기 사라지는 쪽이 더 나쁘다.
     */
    @Test
    @DisplayName("이미 시작된 그룹은 방장도 삭제할 수 없다")
    void cannotDeleteStartedGroup() {
        long groupId = service.create(OWNER_ID, request(r -> { })).getGroupId();
        groupMapper.updateStatusIfCurrent(groupId, "RECRUITING", "ACTIVE");

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.deleteGroup(OWNER_ID, groupId));

        assertEquals("GROUP_NOT_DELETABLE", e.getCode());
        assertNotNull(groupMapper.findById(groupId));
        verify(chatMessageStore, never()).deleteRoom(groupId);
    }

    @Test
    @DisplayName("없는 그룹을 지우려 하면 GROUP_NOT_FOUND 다")
    void deleteUnknownGroupThrows() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> service.deleteGroup(OWNER_ID, 9999L));

        assertEquals("GROUP_NOT_FOUND", e.getCode());
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

    /**
     * 사전 판정을 통과한 뒤 잠금 안에서 정원이 차는 경로 (이슈 #354).
     * 예전에는 사전 판정이 곧 최종 판정이라 이 상황에서 <b>7번째 참여자가 그대로 들어갔다.</b>
     */
    @Test
    @DisplayName("사전 판정을 통과해도 잠금 안에서 마지막 자리가 차 있으면 GROUP_FULL 이다 (#354)")
    void joinLosesRaceForLastSeat() {
        ChallengeGroupCreatedDto created = service.create(OWNER_ID, request(r -> { }));
        for (long userId = 10L; userId < 14L; userId++) {
            service.join(userId, created.getInviteCode());   // 방장 포함 5명 — 한 자리 남는다
        }
        // 잠금을 잡는 순간 경쟁자가 마지막 자리를 채운다
        groupMapper.onLockGroup(() -> memberMapper.insertMember(GroupMember.builder()
                .groupId(created.getGroupId())
                .userId(99L)
                .livesCount(1)
                .build()));

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.join(GUEST_ID, created.getInviteCode()));

        assertEquals("GROUP_FULL", e.getCode(), "새 오류 코드를 만들면 프론트가 모른다");
        assertEquals(6, memberMapper.findByGroupIds(List.of(created.getGroupId())).size(),
                "정원 6명을 넘지 않는다");
    }

    /**
     * 잠금을 기다리는 사이 미성립 배치나 방장 삭제가 그룹을 지울 수 있다. 그대로 INSERT 하면
     * FK 위반으로 500 이 된다 — 초대 코드 경로라 「없는 코드」와 같은 오류로 돌려준다.
     */
    @Test
    @DisplayName("잠금 시점에 그룹이 사라졌으면 GROUP_INVITE_CODE_NOT_FOUND 다 (#354)")
    void joinWhenGroupVanishesUnderLock() {
        ChallengeGroupCreatedDto created = service.create(OWNER_ID, request(r -> { }));
        groupMapper.onLockGroup(() -> groupMapper.deleteIfCurrent(created.getGroupId(), "RECRUITING"));

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.join(GUEST_ID, created.getInviteCode()));

        assertEquals("GROUP_INVITE_CODE_NOT_FOUND", e.getCode());
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

    @Test
    @DisplayName("상세의 참여자 목록에 프로필 이미지 URL 이 담긴다 — 키가 아니라 URL 이다")
    void detailMembersCarryProfileImageUrl() {
        /*
         * 이슈 #407. 이 목록을 쓰는 화면(그룹 상세 멤버 그리드 · 채팅 아바타)이 프로필 사진을
         * 그린다. 이 필드가 없던 동안 두 화면 모두 프로필을 바꿔도 이니셜만 보여줬다.
         *
         * 저장소 키를 그대로 내려주면 안 된다 — URL 규칙이 바뀔 때 프론트까지 고쳐야 한다.
         */
        ChallengeGroupCreatedDto created = service.create(OWNER_ID, request(r -> { }));
        memberMapper.insertMember(GroupMember.builder()
                .groupId(created.getGroupId())
                .userId(99L)
                .nickname("탕이")
                .profileImageKey("profile/99.png")
                .livesCount(1)
                .build());

        ChallengeGroupDto detail = service.findDetail(OWNER_ID, created.getGroupId());

        GroupMemberDto joined = detail.getMembers().stream()
                .filter(m -> m.getUserId() == 99L)
                .findFirst()
                .orElseThrow();
        assertEquals("https://img.test/profile/99.png", joined.getProfileImageUrl());
    }

    @Test
    @DisplayName("프로필 이미지가 없는 참여자는 URL 이 NULL 이다 — 화면이 이니셜로 그린다")
    void detailMemberWithoutImageHasNullUrl() {
        ChallengeGroupCreatedDto created = service.create(OWNER_ID, request(r -> { }));

        ChallengeGroupDto detail = service.findDetail(OWNER_ID, created.getGroupId());

        assertNull(detail.getMembers().get(0).getProfileImageUrl());
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
        assertEquals("요롱이: 나 오늘 진짜 참았다", listed.getLastChatMessage(),
                "프론트의 실시간 경로(groupChat.receiveChatAlert)와 같은 모양이어야 한다");
        assertEquals(sentAt, listed.getLastChatTime());

        ChallengeGroupDto detail = service.findDetail(OWNER_ID, created.getGroupId());
        assertEquals(3, detail.getUnreadChatCount(), "상세 FAB 배지도 같은 필드를 본다");
    }

    @Test
    @DisplayName("보낸 사람이 없는 메시지에는 접두를 붙이지 않는다 — SYSTEM 메시지가 여기 해당한다")
    void systemMessagePreviewHasNoNicknamePrefix() {
        ChallengeGroupCreatedDto created = service.create(OWNER_ID, request(r -> { }));
        when(chatMessageStore.findRecent(created.getGroupId(), 1))
                .thenReturn(List.of(ChatMessage.of(1L, ChatMessageType.SYSTEM, null, null,
                        "재판이 열렸어요", LocalDateTime.of(2026, 8, 12, 14, 3))));

        ChallengeGroupDto listed = service.findMyGroups(OWNER_ID, null).get(0);

        assertEquals("재판이 열렸어요", listed.getLastChatMessage(),
                "\": 재판이 열렸어요\" 처럼 빈 이름이 앞에 붙으면 안 된다");
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

    /** 대분류 1개 · 그 아래 소분류 1개. 「소분류인가」만 보므로 이 둘이면 분기가 다 덮인다. */
    private static class FakeCategoryMapper implements CategoryMapper {
        @Override
        public List<Category> findAll() {
            return List.of(category(PARENT_CATEGORY_ID, "식비", null),
                    category(SUB_CATEGORY_ID, "카페/간식", PARENT_CATEGORY_ID));
        }

        @Override
        public Category findById(Long id) {
            return findAll().stream().filter(c -> c.getId().equals(id)).findFirst().orElse(null);
        }

        private static Category category(long id, String name, Long parentId) {
            return Category.builder().id(id).categoryName(name).parentId(parentId).build();
        }
    }

    private static class FakeGroupMapper implements ChallengeGroupMapper {
        private final List<ChallengeGroup> groups = new ArrayList<>();
        private long sequence;
        private List<String> lastStatuses;

        /**
         * 잠금을 기다리는 사이 다른 트랜잭션이 커밋한 상황을 흉내내는 훅 (이슈 #354).
         *
         * <p>실제 {@code FOR UPDATE} 는 앞선 트랜잭션이 끝날 때까지 그 자리에서 멈춰 서고,
         * 깨어났을 때는 그 트랜잭션의 결과가 이미 반영돼 있다. 「깨어나는 순간」이 여기다 —
         * 스레드를 쓰지 않고도 <b>사전 판정을 통과한 뒤 잠금 안에서 상황이 뒤집히는</b> 경로를
         * 그대로 태울 수 있다. (실제 잠금 동작 자체는 {@code ChallengeGroupJoinRaceIntegrationTest})
         */
        private Runnable onLockGroup = () -> { };

        void onLockGroup(Runnable action) {
            this.onLockGroup = action;
        }

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

        /** 실제 구현은 판정용 4개 컬럼만 채운다. 페이크는 구분할 이유가 없어 행 그대로 준다. */
        @Override
        public ChallengeGroup lockGroupForJoin(Long groupId) {
            onLockGroup.run();
            return groups.stream()
                    .filter(g -> g.getId().equals(groupId))
                    .findFirst()
                    .orElse(null);
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

        /** 실제 구현은 group_id · user_id 만 채우는 잠금 읽기다. 페이크에서는 결과가 같다. */
        @Override
        public List<GroupMember> findByGroupIdForUpdate(long groupId) {
            return findByGroupIds(List.of(groupId));
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
