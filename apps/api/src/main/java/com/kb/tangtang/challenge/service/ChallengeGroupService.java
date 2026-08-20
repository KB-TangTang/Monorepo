package com.kb.tangtang.challenge.service;

import com.kb.tangtang.challenge.chat.domain.ChatMessage;
import com.kb.tangtang.challenge.chat.store.ChatMessageStore;
import com.kb.tangtang.challenge.domain.ChallengeGroup;
import com.kb.tangtang.challenge.domain.ChallengeGroupStatus;
import com.kb.tangtang.challenge.domain.EvalType;
import com.kb.tangtang.challenge.domain.GroupMember;
import com.kb.tangtang.challenge.domain.GroupTrialSummaryRow;
import com.kb.tangtang.challenge.dto.ChallengeGroupCreateRequestDto;
import com.kb.tangtang.challenge.dto.ChallengeGroupCreatedDto;
import com.kb.tangtang.challenge.dto.ChallengeGroupDto;
import com.kb.tangtang.challenge.dto.GroupMemberDto;
import com.kb.tangtang.challenge.dto.InviteCodePreviewDto;
import com.kb.tangtang.challenge.mapper.ChallengeGroupMapper;
import com.kb.tangtang.challenge.mapper.GroupMemberMapper;
import com.kb.tangtang.challenge.mapper.IndictmentMapper;
import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.notification.domain.NotificationRequestedEvent;
import com.kb.tangtang.notification.domain.NotificationType;
import com.kb.tangtang.transaction.domain.Category;
import com.kb.tangtang.transaction.mapper.CategoryMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 그룹 챌린지 생성 · 초대 · 참여 · 조회 (GC_01_01 ~ GC_01_06).
 *
 * <p>팀 결정 사항 (이슈 #151)
 * <ul>
 *   <li>정원은 6명 고정이다. 생성 화면에 입력 UI 가 없어 요청으로 받지 않는다.</li>
 *   <li>목숨은 <b>참여 시점에 계산해 INSERT</b> 한다. {@code lives_count} 가 NOT NULL 인데
 *       기본값이 없어서다. 뒤늦게 참여해도 목숨은 같다 — 의도된 정책이다.</li>
 *   <li>초대 코드에는 만료 컬럼이 없다. <b>시작일 당일 23:59 까지 모집</b>하며, 만료 판정은
 *       상태 전이 배치가 바꿔 놓는 {@code status} 로 한다.</li>
 *   <li>제한 금액 0원은 유효하다. 무지출 챌린지 컨셉이 그 값을 쓴다.</li>
 * </ul>
 *
 * <p>상태 전이(RECRUITING → ACTIVE/CLOSED)와 시작 알림은 이 서비스가 하지 않는다.
 * {@link ChallengeGroupStatusBatchService} 가 담당한다(이슈 #152).
 * 참여 가능 판정은 그 배치가 남긴 <b>status</b> 를 기준으로 한다 — 근거는
 * {@code joinBlockReason} 주석에 있다.
 */
@Service
@Log4j2
public class ChallengeGroupService {

    /** 정원 고정값. DDL 의 {@code ck_cg_members} 는 2~6 을 허용하지만 화면에 선택 UI 가 없다. */
    private static final int FIXED_MAX_MEMBERS = 6;

    private static final int MAX_GROUP_NAME_LENGTH = 100;
    private static final int MAX_MEMO_LENGTH = 300;

    /** {@code ck_cg_period} 가 강제하는 최대 기간(시작일 포함 7일). */
    private static final int MAX_PERIOD_DAYS = 7;

    /**
     * 그룹 챌린지 홈. 삭제 알림의 딥링크다 — 그룹이 사라진 뒤라 상세({@code /group-challenges/:id})로
     * 보내면 404 가 된다. {@code ChallengeGroupStatusTransitionService} 의 미성립 알림과 같은 이유다.
     */
    private static final String GROUP_CHALLENGE_HOME = "/group-challenges";

    private final ChallengeGroupMapper challengeGroupMapper;
    private final GroupMemberMapper groupMemberMapper;
    private final IndictmentMapper indictmentMapper;
    private final CategoryMapper categoryMapper;
    private final InviteCodeGenerator inviteCodeGenerator;
    private final ChatMessageStore chatMessageStore;
    private final ChallengeGroupDeleter challengeGroupDeleter;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Autowired
    public ChallengeGroupService(ChallengeGroupMapper challengeGroupMapper,
                                 GroupMemberMapper groupMemberMapper,
                                 IndictmentMapper indictmentMapper,
                                 CategoryMapper categoryMapper,
                                 InviteCodeGenerator inviteCodeGenerator,
                                 ChatMessageStore chatMessageStore,
                                 ChallengeGroupDeleter challengeGroupDeleter,
                                 ApplicationEventPublisher events) {
        this(challengeGroupMapper, groupMemberMapper, indictmentMapper, categoryMapper,
                inviteCodeGenerator, chatMessageStore, challengeGroupDeleter, events,
                Clock.systemDefaultZone());
    }

    ChallengeGroupService(ChallengeGroupMapper challengeGroupMapper,
                          GroupMemberMapper groupMemberMapper,
                          IndictmentMapper indictmentMapper,
                          CategoryMapper categoryMapper,
                          InviteCodeGenerator inviteCodeGenerator,
                          ChatMessageStore chatMessageStore,
                          ChallengeGroupDeleter challengeGroupDeleter,
                          ApplicationEventPublisher events,
                          Clock clock) {
        this.challengeGroupMapper = challengeGroupMapper;
        this.groupMemberMapper = groupMemberMapper;
        this.indictmentMapper = indictmentMapper;
        this.categoryMapper = categoryMapper;
        this.inviteCodeGenerator = inviteCodeGenerator;
        this.chatMessageStore = chatMessageStore;
        this.challengeGroupDeleter = challengeGroupDeleter;
        this.events = events;
        this.clock = clock;
    }

    /* ══ 생성 ══════════════════════════════════════════════ */

    /**
     * 그룹 챌린지를 만들고 방장을 첫 참여자로 넣는다.
     *
     * 방장을 따로 취급하지 않는 이유는 정원·목숨·랭킹이 모두 {@code tbl_group_member} 를
     * 세기 때문이다. 방장만 빠져 있으면 정원 계산이 한 자리씩 어긋난다.
     */
    @Transactional
    public ChallengeGroupCreatedDto create(long userId, ChallengeGroupCreateRequestDto request) {
        LocalDate today = LocalDate.now(clock);
        EvalType evalType = validateCreate(request, today);

        ChallengeGroup group = ChallengeGroup.builder()
                .adminId(userId)
                .groupName(request.getGroupName().trim())
                .categoryId(request.getCategoryId())
                .limitAmount(request.getLimitAmount())
                .evalType(evalType.name())
                .maxMembers(FIXED_MAX_MEMBERS)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .inviteCode(inviteCodeGenerator.generate())
                .status(ChallengeGroupStatus.RECRUITING.name())
                .memo(trimToNull(request.getMemo()))
                .build();
        challengeGroupMapper.insertGroup(group);

        groupMemberMapper.insertMember(newMember(group, userId));
        log.info("그룹 챌린지 생성 userId={} groupId={} inviteCode={} startDate={}",
                userId, group.getId(), group.getInviteCode(), group.getStartDate());

        // 채팅방은 챌린지 생성과 동시에 열린다. 개설·삭제 UI 는 없다(이슈 #174)
        // Redis 장애로 채팅방을 못 열어도 챌린지 생성이라는 본업은 성공해야 한다 — 조용히 삼키지 않고 로그만 남긴다.
        try {
            chatMessageStore.initRoom(group.getId(), Set.of(group.getAdminId()), group.getEndDate());
        } catch (Exception e) {
            log.error("채팅방 개설 실패 groupId={} — 챌린지 생성은 계속 진행한다", group.getId(), e);
        }

        return ChallengeGroupCreatedDto.builder()
                .groupId(group.getId())
                .inviteCode(group.getInviteCode())
                .build();
    }

    /* ══ 초대 코드 · 참여 ═══════════════════════════════════ */

    /**
     * 초대 코드 미리보기 (GC_01_05).
     *
     * 코드 자체가 없으면 예외지만, 코드가 유효한데 참여만 못 하는 경우는 200 으로 내려준다.
     * 참여 확인 화면이 그룹 정보를 먼저 보여주고 나서 사유를 안내해야 하기 때문이다.
     */
    @Transactional(readOnly = true)
    public InviteCodePreviewDto previewInviteCode(long userId, String inviteCode) {
        ChallengeGroup group = findGroupByInviteCodeOrThrow(userId, inviteCode);

        LocalDate today = LocalDate.now(clock);
        List<GroupMember> members = findMembers(group.getId());
        String reason = joinBlockReason(group, members, userId, today);
        log.info("초대 코드 조회 userId={} inviteCode={} groupId={} status={} startDate={} memberCount={} joinable={} reason={}",
                userId, group.getInviteCode(), group.getId(), group.getStatus(), group.getStartDate(),
                members.size(), reason == null, reason);

        return InviteCodePreviewDto.builder()
                .challenge(toDto(userId, group, members, today))
                .joinable(reason == null)
                .reason(reason)
                .build();
    }

    /**
     * 참여 (GC_01_06). 성공하면 참여한 그룹을 그대로 돌려준다 — 화면이 바로 상세로 넘어간다.
     *
     * <p><b>groupId 가 아니라 초대 코드로 그룹을 찾는다.</b> 예전에는 groupId 를 받았는데, 코드 검증이
     * {@link #previewInviteCode} 에만 있고 여기에는 없어 <b>코드 없이 참여할 수 있었다</b>(이슈 #346).
     * 코드를 「그룹을 찾는 유일한 수단」으로 만들면 검증을 빠뜨릴 자리 자체가 없어진다.
     *
     * <p><b>판정이 두 번인 것은 의도한 것이다</b>(이슈 #354). 앞의 것은 잠금 없는 빠른 실패고,
     * 참여를 실제로 허락하는 것은 <b>그룹 행을 잠근 뒤의 두 번째 판정</b>이다. 같은
     * {@link #joinBlockReason} 을 그대로 다시 부르므로 경합에서 밀린 요청도 기존과
     * <b>똑같은 오류 코드</b>({@code GROUP_FULL} 등)를 받는다 — 프론트가 모르는 코드가 생기지 않는다.
     */
    @Transactional
    public ChallengeGroupDto join(long userId, String inviteCode) {
        log.info("그룹 참여 요청 userId={} inviteCode={}", userId, inviteCode);

        ChallengeGroup group = findGroupByInviteCodeOrThrow(userId, inviteCode);
        long groupId = group.getId();
        LocalDate today = LocalDate.now(clock);
        List<GroupMember> members = findMembers(groupId);

        String reason = joinBlockReason(group, members, userId, today);
        if (reason != null) {
            log.warn("그룹 참여 차단 userId={} groupId={} reason={} status={} startDate={} memberCount={}",
                    userId, groupId, reason, group.getStatus(), group.getStartDate(), members.size());
            throw joinBlocked(reason);
        }

        assertJoinableUnderLock(userId, groupId, today);

        groupMemberMapper.insertMember(newMember(group, userId));
        // ⚠ REPEATABLE READ 스냅샷이 위 findMembers 시점에 고정돼 있어, 잠금 직전에 커밋된 다른
        //   참여가 이 목록에는 안 보일 수 있다(내가 넣은 행은 보인다). 정원은 잠금이 지켰고
        //   화면은 새로고침하면 맞는다 — 표시용 조회까지 잠금 읽기로 바꾸면 tbl_user 까지 잠긴다.
        List<GroupMember> joined = findMembers(groupId);
        log.info("그룹 참여 완료 userId={} groupId={} memberCount={}", userId, groupId, joined.size());

        // 참여자 캐시를 갱신하지 않으면 새 멤버에게 안 읽은 수가 쌓이지 않는다
        // Redis 장애로 캐시 갱신이 실패해도 참여라는 본업은 성공해야 한다 — 조용히 삼키지 않고 로그만 남긴다.
        try {
            chatMessageStore.cacheMembers(groupId, Set.of(userId), group.getEndDate());
        } catch (Exception e) {
            log.error("채팅방 참여자 캐시 갱신 실패 groupId={} userId={} — 참여 처리는 계속 진행한다",
                    groupId, userId, e);
        }

        return toDto(userId, group, joined, today);
    }

    /* ══ 삭제 ══════════════════════════════════════════════ */

    /**
     * 방장이 모집 중인 그룹을 없앤다 (이슈 #352).
     *
     * <p><b>모집 중(RECRUITING)만 지울 수 있다.</b> 시작한 뒤에는 참여자들의 소비 집계·기소·투표가
     * 쌓여 있어, 한 사람의 결정으로 지우면 남의 기록까지 CASCADE 로 함께 사라진다.
     * ACTIVE 이후를 열어 줄지는 팀 논의 대기 중이다.
     *
     * <p>참여자를 <b>지우기 전에</b> 읽는다. {@code tbl_group_member} 는 FK CASCADE 로 함께
     * 사라지므로 삭제 뒤에 조회하면 알릴 대상이 빈 목록이 된다.
     *
     * <p>「나가기(멤버 탈퇴)」는 만들지 않는다. 목숨·순위·기소가 참여자 수를 전제로 계산돼
     * 중간 이탈을 넣으려면 그 계산을 전부 다시 정의해야 한다.
     */
    @Transactional
    public void deleteGroup(long userId, long groupId) {
        ChallengeGroup group = findGroupOrThrow(groupId);
        if (group.getAdminId() == null || group.getAdminId() != userId) {
            log.warn("그룹 삭제 차단 userId={} groupId={} adminId={} — 방장이 아니다",
                    userId, groupId, group.getAdminId());
            throw new BusinessException("GROUP_NOT_OWNER", "방장만 챌린지를 삭제할 수 있습니다.");
        }

        List<GroupMember> members = findMembers(groupId);
        if (!challengeGroupDeleter.deleteIfRecruiting(groupId)) {
            // 조회 시점엔 RECRUITING 이었어도 그 사이 시작 배치가 ACTIVE 로 바꿨을 수 있다.
            log.warn("그룹 삭제 차단 userId={} groupId={} status={} — 모집 중이 아니다",
                    userId, groupId, group.getStatus());
            throw new BusinessException("GROUP_NOT_DELETABLE", "이미 시작된 챌린지는 삭제할 수 없습니다.");
        }

        publishDeleted(group, members);
        log.info("그룹 챌린지 삭제 userId={} groupId={} groupName={} memberCount={}",
                userId, groupId, group.getGroupName(), members.size());
    }

    /**
     * 방장을 뺀 남은 참여자에게만 알린다. 방장은 자기가 누른 결과라 알림이 필요 없다.
     *
     * <p>모집 중이라도 이미 최대 5명이 들어와 있을 수 있다 — 「혼자였을 테니 알림이 필요 없다」는
     * 가정은 틀렸다. 딥링크는 그룹 홈이다. 상세로 보내면 방금 지운 그룹이라 404 가 된다.
     */
    private void publishDeleted(ChallengeGroup group, List<GroupMember> members) {
        Map<String, String> params = Map.of("groupName", group.getGroupName());
        for (GroupMember member : members) {
            if (member.getUserId() == null || member.getUserId().equals(group.getAdminId())) {
                continue;
            }
            events.publishEvent(new NotificationRequestedEvent(
                    member.getUserId(),
                    NotificationType.GROUP_CHALLENGE_DELETED,
                    params,
                    GROUP_CHALLENGE_HOME));
        }
    }

    /* ══ 조회 ══════════════════════════════════════════════ */

    /**
     * 내가 참여 중인 그룹 목록 (GC_01_01).
     *
     * @param statuses NULL·빈 목록이면 전체
     */
    @Transactional(readOnly = true)
    public List<ChallengeGroupDto> findMyGroups(long userId, List<String> statuses) {
        List<ChallengeGroup> groups = challengeGroupMapper.findMyGroups(userId, validateStatuses(statuses));
        if (groups.isEmpty()) {
            return List.of();
        }

        // 그룹마다 참여자를 따로 읽으면 N+1 이 된다. 한 번에 읽어 groupId 로 나눈다.
        List<Long> groupIds = groups.stream().map(ChallengeGroup::getId).toList();
        Map<Long, List<GroupMember>> membersByGroup = groupMemberMapper.findByGroupIds(groupIds).stream()
                .collect(Collectors.groupingBy(GroupMember::getGroupId));

        // 재판 배지도 같은 이유로 한 번에 센다. 재판이 없는 그룹은 행 자체가 안 나오므로 없으면 null 이다.
        Map<Long, GroupTrialSummaryRow> trialByGroup =
                indictmentMapper.findTrialSummaryByGroupIds(userId, groupIds).stream()
                        .collect(Collectors.toMap(GroupTrialSummaryRow::getGroupId, row -> row));

        LocalDate today = LocalDate.now(clock);
        List<ChallengeGroupDto> result = new ArrayList<>(groups.size());
        for (ChallengeGroup group : groups) {
            List<GroupMember> members = membersByGroup.getOrDefault(group.getId(), List.of());
            result.add(toDto(userId, group, members, today, trialByGroup.get(group.getId())));
        }
        return result;
    }

    /** 그룹 상세 (GC_01_09). 참여자만 볼 수 있다 — 비참여자는 초대 코드 미리보기를 쓴다. */
    @Transactional(readOnly = true)
    public ChallengeGroupDto findDetail(long userId, long groupId) {
        ChallengeGroup group = findGroupOrThrow(groupId);
        List<GroupMember> members = findMembers(groupId);
        if (!isMember(members, userId)) {
            throw new BusinessException("GROUP_NOT_MEMBER", "참여 중인 챌린지가 아닙니다.");
        }
        return toDto(userId, group, members, LocalDate.now(clock));
    }

    /* ══ 검증 ══════════════════════════════════════════════ */

    private EvalType validateCreate(ChallengeGroupCreateRequestDto request, LocalDate today) {
        String groupName = trimToNull(request.getGroupName());
        if (groupName == null) {
            throw new BusinessException("GROUP_NAME_REQUIRED", "챌린지 이름을 입력해 주세요.");
        }
        if (groupName.length() > MAX_GROUP_NAME_LENGTH) {
            throw new BusinessException("GROUP_NAME_TOO_LONG",
                    "챌린지 이름은 " + MAX_GROUP_NAME_LENGTH + "자를 넘을 수 없습니다.");
        }

        // 0원은 무지출 챌린지의 정상 입력값이다. 음수만 막는다.
        if (request.getLimitAmount() == null || request.getLimitAmount() < 0) {
            throw new BusinessException("GROUP_LIMIT_AMOUNT_INVALID", "제한 금액은 0원 이상이어야 합니다.");
        }

        EvalType evalType = parseEvalType(request.getEvalType());

        LocalDate startDate = request.getStartDate();
        LocalDate endDate = request.getEndDate();
        if (startDate == null || endDate == null) {
            throw new BusinessException("GROUP_PERIOD_INVALID", "챌린지 기간을 입력해 주세요.");
        }
        // 오늘 시작을 막는다. 모집이 시작일 23:59 에 닫히므로 오늘 시작하면 모집 기간이
        // 만든 시각부터 자정까지로 쪼그라든다. 프론트 달력도 내일부터만 고를 수 있다.
        if (!startDate.isAfter(today)) {
            throw new BusinessException("GROUP_START_DATE_INVALID", "시작일은 내일 이후여야 합니다.");
        }
        if (endDate.isBefore(startDate)) {
            throw new BusinessException("GROUP_PERIOD_INVALID", "종료일은 시작일 이후여야 합니다.");
        }
        if (daysBetweenInclusive(startDate, endDate) > MAX_PERIOD_DAYS) {
            throw new BusinessException("GROUP_PERIOD_INVALID",
                    "챌린지 기간은 최대 " + MAX_PERIOD_DAYS + "일입니다.");
        }

        String memo = trimToNull(request.getMemo());
        if (memo != null && memo.length() > MAX_MEMO_LENGTH) {
            throw new BusinessException("GROUP_MEMO_TOO_LONG",
                    "리워드·벌칙 메모는 " + MAX_MEMO_LENGTH + "자를 넘을 수 없습니다.");
        }

        validateCategory(request.getCategoryId());

        return evalType;
    }

    /**
     * 카테고리는 <b>소분류만</b> 받는다 (이슈 #352). {@code null} 은 「총 소비」라 그대로 통과시킨다.
     *
     * <p>대분류를 막는 이유는 집계가 조용히 0원이 되기 때문이다. {@code tbl_transaction.category_id}
     * 에는 소분류만 들어가는데 {@code GroupChallengeResultMapper} 는 {@code t.category_id =
     * g.category_id} 로 정확히 맞춰 본다 — 대분류 id 로 그룹을 만들면 <b>매칭되는 거래가 영원히 0건</b>
     * 이라 아무도 기소되지 않고, 화면에는 오류 없이 「0원」만 뜬다. 실제로 화면이 대분류 id 를 하드코딩해
     * 두고 있었다.
     *
     * <p>허용 목록을 15개 id 로 박아 두지 않은 이유는 {@code tbl_category.id} 가 AUTO_INCREMENT 라
     * 환경마다 값이 다를 수 있어서다. 「소분류인가」만 본다 — 화면이 보여줄 목록을 좁히는 일은 화면이 한다.
     */
    private void validateCategory(Long categoryId) {
        if (categoryId == null) {
            return;
        }
        Category category = categoryMapper.findById(categoryId);
        if (category == null || category.getParentId() == null) {
            throw new BusinessException("GROUP_CATEGORY_INVALID", "선택할 수 없는 카테고리입니다.");
        }
    }

    private EvalType parseEvalType(String raw) {
        if (raw != null) {
            for (EvalType type : EvalType.values()) {
                if (type.name().equalsIgnoreCase(raw.trim())) {
                    return type;
                }
            }
        }
        throw new BusinessException("GROUP_EVAL_TYPE_INVALID", "평가 방식을 선택해 주세요.");
    }

    /** 알 수 없는 상태값이 그대로 SQL 로 내려가면 조용히 빈 목록이 된다. 미리 걸러 400 으로 끝낸다. */
    private List<String> validateStatuses(List<String> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return null;
        }
        List<String> normalized = new ArrayList<>(statuses.size());
        for (String raw : statuses) {
            String candidate = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
            try {
                normalized.add(ChallengeGroupStatus.valueOf(candidate).name());
            } catch (IllegalArgumentException e) {
                throw new BusinessException("GROUP_STATUS_INVALID", "알 수 없는 상태값입니다: " + raw);
            }
        }
        return normalized;
    }

    private String normalizeInviteCode(String inviteCode) {
        String normalized = trimToNull(inviteCode);
        if (normalized == null) {
            throw new BusinessException("GROUP_INVITE_CODE_NOT_FOUND", "유효하지 않은 초대 코드입니다.");
        }
        // 사용자가 옮겨 적는 값이라 대소문자는 가리지 않는다. 저장은 항상 대문자다.
        return normalized.toUpperCase(Locale.ROOT);
    }

    /**
     * 초대 코드로 그룹을 찾는다. 없으면 예외.
     *
     * <p>미리보기와 참여가 <b>같은 조회</b>를 하므로 한 곳에 둔다. 한쪽만 정규화나 존재 검사를
     * 빠뜨리면 「미리보기는 되는데 참여는 안 되는」 코드가 생긴다 — 이슈 #346 이 정확히 그
     * 어긋남(참여 쪽에 코드 검증이 아예 없었다)이었다.
     *
     * <p>코드가 없을 때와 틀렸을 때를 같은 오류로 묶는 것은 의도적이다. 구분해서 알려주면
     * 「존재하는 코드」를 가려내는 신호가 된다.
     */
    private ChallengeGroup findGroupByInviteCodeOrThrow(long userId, String inviteCode) {
        String normalized = normalizeInviteCode(inviteCode);
        ChallengeGroup group = challengeGroupMapper.findByInviteCode(normalized);
        if (group == null) {
            log.warn("초대 코드 조회 실패 userId={} inviteCode={} — 일치하는 그룹 없음", userId, normalized);
            throw new BusinessException("GROUP_INVITE_CODE_NOT_FOUND", "유효하지 않은 초대 코드입니다.");
        }
        return group;
    }

    /**
     * 정원 판정을 원자적으로 다시 한다 (이슈 #354). 통과하면 INSERT 해도 되는 상태다.
     *
     * <p><b>「읽고 → 세고 → 넣는」 사이가 비어 있던 것이 이 이슈의 버그다.</b> 같은 초대 링크를
     * 여러 명이 동시에 열면 둘 다 「자리 있음」을 보고 둘 다 들어갔다. PK {@code (group_id,user_id)}
     * 는 같은 사람의 중복만 막고, {@code @Transactional} 만으로는 팬텀 삽입을 못 막는다.
     * 그룹 행을 잠가 이 구간을 한 줄로 세운다.
     *
     * <p><b>두 조회 모두 잠금 읽기다.</b> 그룹 행만 잠그고 참여자를 일반 SELECT 로 세면,
     * REPEATABLE READ 스냅샷이 트랜잭션 첫 읽기 시점에 고정돼 있어 <b>그 사이 커밋된 참여가
     * 안 보인다</b> — 잠금을 걸고도 예전 숫자를 읽어 방어가 무력해진다.
     * 근거는 {@code GroupMemberMapper.findByGroupIdForUpdate} 주석에 있다.
     *
     * <p>잠금 순서는 <b>{@code tbl_challenge_group} → {@code tbl_group_member}</b> 다.
     * 그룹 챌린지의 다른 쓰기 경로(상태 전이 · 미성립 삭제 · 방장 삭제)도 그룹 행부터 잡는다.
     * 반대로 잡는 경로를 새로 만들면 데드락이 된다.
     */
    private void assertJoinableUnderLock(long userId, long groupId, LocalDate today) {
        ChallengeGroup locked = challengeGroupMapper.lockGroupForJoin(groupId);
        if (locked == null) {
            /* 잠금을 기다리는 사이 미성립 배치나 방장 삭제가 그룹을 지웠다.
               초대 코드로 들어온 경로라 「없는 코드」와 같은 오류로 돌려준다 — 프론트가 이미 아는 코드다. */
            log.warn("그룹 참여 차단 userId={} groupId={} — 잠금 시점에 그룹이 사라졌다", userId, groupId);
            throw new BusinessException("GROUP_INVITE_CODE_NOT_FOUND", "유효하지 않은 초대 코드입니다.");
        }

        List<GroupMember> lockedMembers = groupMemberMapper.findByGroupIdForUpdate(groupId);
        String reason = joinBlockReason(locked, lockedMembers, userId, today);
        if (reason != null) {
            log.warn("그룹 참여 차단(잠금 후 재판정) userId={} groupId={} reason={} status={} memberCount={}",
                    userId, groupId, reason, locked.getStatus(), lockedMembers.size());
            throw joinBlocked(reason);
        }
    }

    /**
     * 참여를 막는 사유. 참여할 수 있으면 NULL.
     *
     * <p>순서에 의미가 있다. 이미 참여 중이면 종료·만료 여부와 무관하게 그룹으로 보내야 하므로
     * ALREADY_JOINED 를 먼저 본다.
     *
     * <p><b>기준은 status 다</b> (2026-08-13, 이슈 #152 에서 결정). 예전에는 {@code start_date} 와
     * 오늘을 비교해 EXPIRED 를 판정했는데, #152 배치가 status 를 RECRUITING → ACTIVE 로 바꾸기
     * 시작하면서 근거가 둘이 됐다. 배치가 지연되거나 수동 트리거로 돌면 두 판정이 어긋나
     * "화면엔 진행 중인데 참여가 되는" 상태가 생긴다.
     *
     * <p><b>단 ACTIVE 한 갈래에서만 시작일을 함께 본다</b> (2026-08-20, 이슈 #350).
     * 모집 마감은 <b>시작일 23:59</b> 인데(클래스 주석 · 초대 화면 문구) 상태 전이 배치는 시작일
     * 00:01 에 돌아 ACTIVE 로 바꿔 버린다. status 만 보면 시작일 하루가 통째로 막혀
     * 시작일 참여 창이 0초가 된다. {@code today <= startDate} 인 동안에는 ACTIVE 라도 열어 둔다.
     *
     * <p>여기서만 날짜를 보는 것은 #152 결정과 충돌하지 않는다. 그 결정이 막으려던 것은
     * <b>「배치가 밀려 status 와 날짜가 어긋나는」</b> 경우인데, 이 조건은 배치가 밀리면
     * RECRUITING 이라 애초에 닿지 않고, 배치가 제때 돌았을 때만 평가된다.
     *
     * <p>늦게 들어온 사람도 목숨은 같게 받는다(create 주석 참고). 시작일에 들어오면 그 날
     * 0시부터의 소비가 집계된다 — 참여 화면이 미리 알린다.
     *
     * <p><b>참여 시에는 두 번 불린다</b> — 잠금 없는 사전 판정과 {@link #assertJoinableUnderLock}
     * 의 최종 판정(이슈 #354). 여기에 조건을 더하면 양쪽에 그대로 적용된다.
     */
    private String joinBlockReason(ChallengeGroup group, List<GroupMember> members,
                                   long userId, LocalDate today) {
        if (isMember(members, userId)) {
            return "ALREADY_JOINED";
        }
        ChallengeGroupStatus status = ChallengeGroupStatus.valueOf(group.getStatus());
        if (status == ChallengeGroupStatus.JUDGING || status == ChallengeGroupStatus.CLOSED) {
            return "CLOSED";
        }
        if (status == ChallengeGroupStatus.ACTIVE && today.isAfter(group.getStartDate())) {
            return "EXPIRED";
        }
        if (members.size() >= group.getMaxMembers()) {
            return "FULL";
        }
        return null;
    }

    private BusinessException joinBlocked(String reason) {
        return switch (reason) {
            case "ALREADY_JOINED" -> new BusinessException("GROUP_ALREADY_JOINED", "이미 참여 중인 챌린지입니다.");
            case "CLOSED" -> new BusinessException("GROUP_CLOSED", "이미 종료된 챌린지입니다.");
            case "EXPIRED" -> new BusinessException("GROUP_INVITE_CODE_EXPIRED", "모집이 마감된 챌린지입니다.");
            case "FULL" -> new BusinessException("GROUP_FULL", "정원이 가득 찼습니다.");
            default -> new BusinessException("GROUP_JOIN_BLOCKED", "지금은 참여할 수 없습니다.");
        };
    }

    /* ══ 조립 ══════════════════════════════════════════════ */

    /** 재판 배지가 없는 자리(상세 · 참여 · 초대 미리보기)용. */
    private ChallengeGroupDto toDto(long userId,
                                    ChallengeGroup group,
                                    List<GroupMember> members,
                                    LocalDate today) {
        return toDto(userId, group, members, today, null);
    }

    /** @param trial 재판 배지 집계. 목록에서만 채워지고 그 외에는 NULL 이다 */
    private ChallengeGroupDto toDto(long userId,
                                    ChallengeGroup group,
                                    List<GroupMember> members,
                                    LocalDate today,
                                    GroupTrialSummaryRow trial) {
        int totalDays = daysBetweenInclusive(group.getStartDate(), group.getEndDate());
        EvalType evalType = EvalType.valueOf(group.getEvalType());
        GroupMember me = findMember(members, userId);
        ChatSummary chat = me == null ? ChatSummary.EMPTY : chatSummary(group.getId(), userId);

        return ChallengeGroupDto.builder()
                .id(group.getId())
                .adminId(group.getAdminId())
                .groupName(group.getGroupName())
                .categoryId(group.getCategoryId())
                .categoryName(group.getCategoryName())
                .limitAmount(group.getLimitAmount())
                .evalType(group.getEvalType())
                .maxMembers(group.getMaxMembers())
                .startDate(group.getStartDate())
                .endDate(group.getEndDate())
                .inviteCode(group.getInviteCode())
                .status(group.getStatus())
                .memo(group.getMemo())
                .createdAt(group.getCreatedAt())
                .livesCount(me == null ? null : me.getLivesCount())
                .finalOutcome(me == null ? null : me.getFinalOutcome())
                .finalRank(me == null ? null : me.getFinalRank())
                .finalChargeAmount(me == null ? null : me.getFinalChargeAmount())
                .totalDays(totalDays)
                .currentDay(currentDay(group, today, totalDays))
                .daysUntilStart(daysUntilStart(group, today))
                .maxLives(evalType.livesFor(totalDays))
                .memberCount(members.size())
                .owner(group.getAdminId() != null && group.getAdminId() == userId)
                .member(me != null)
                .joinable(joinBlockReason(group, members, userId, today) == null)
                .members(members.stream()
                        .map(m -> toMemberDto(m, group.getAdminId()))
                        .toList())
                .unreadChatCount(chat.unreadCount())
                .lastChatMessage(chat.lastMessage())
                .lastChatTime(chat.lastTime())
                .pendingTrialCount(trial == null ? 0 : trial.getPendingVoteCount())
                .defendant(trial != null && trial.getMyDefenseNeededCount() > 0)
                .myVoteStatus(myVoteStatus(trial))
                .build();
    }

    /**
     * 「투표중」 · 「투표완료」 · 배지 없음을 가른다.
     *
     * <p>안 던진 표가 하나라도 남아 있으면 이미 던진 표가 있어도 {@code PENDING} 이다 —
     * 배지는 남은 할 일을 가리키므로 완료 쪽이 이기면 할 일이 숨는다.
     */
    private String myVoteStatus(GroupTrialSummaryRow trial) {
        if (trial == null) {
            return null;
        }
        if (trial.getPendingVoteCount() > 0) {
            return "PENDING";
        }
        return trial.getCastVoteCount() > 0 ? "DONE" : null;
    }

    /**
     * 목록·상세에 함께 실을 채팅 요약. 값은 Redis 에만 있다(채팅은 MySQL 에 저장하지 않는다).
     *
     * <p>새 Redis 코드는 필요 없다. 안 읽은 수는 메시지 발행 때 이미 쌓이고 있고,
     * {@code findRecent(groupId, 1)} 은 내부가 {@code range(-1, -1)} 이라 가장 최근 1건을 준다.
     *
     * <p><b>Redis 가 죽어도 그룹 목록은 떠야 한다.</b> 채팅 요약은 부가 정보라 실패를 삼키고
     * 빈 값을 돌려준다. 여기서 예외를 올리면 채팅과 무관한 재판 목록 화면 전체가 함께 죽는다.
     */
    private ChatSummary chatSummary(long groupId, long userId) {
        try {
            List<ChatMessage> recent = chatMessageStore.findRecent(groupId, 1);
            if (recent.isEmpty()) {
                // 방은 있는데 대화만 없는 경우다. 안 읽은 수는 어차피 0 이라 조회를 아낀다.
                return ChatSummary.EMPTY;
            }
            ChatMessage last = recent.get(recent.size() - 1);
            return new ChatSummary(chatMessageStore.unreadOf(groupId, userId),
                    last.getContent(), last.getSentAt());
        } catch (RuntimeException e) {
            log.warn("채팅 요약을 읽지 못해 빈 값으로 내린다. groupId={} userId={}", groupId, userId, e);
            return ChatSummary.EMPTY;
        }
    }

    /** {@link #chatSummary} 의 반환 묶음. 세 값이 항상 함께 결정돼서 따로 나르지 않는다. */
    private record ChatSummary(int unreadCount, String lastMessage, LocalDateTime lastTime) {
        private static final ChatSummary EMPTY = new ChatSummary(0, null, null);
    }

    private GroupMemberDto toMemberDto(GroupMember member, Long adminId) {
        return GroupMemberDto.builder()
                .userId(member.getUserId())
                .nickname(member.getNickname())
                .owner(member.getUserId().equals(adminId))
                .build();
    }

    /** 진행 중일 때의 경과 일차(1부터). 시작 전·종료 후에는 NULL. */
    private Integer currentDay(ChallengeGroup group, LocalDate today, int totalDays) {
        if (today.isBefore(group.getStartDate()) || today.isAfter(group.getEndDate())) {
            return null;
        }
        return Math.min(daysBetweenInclusive(group.getStartDate(), today), totalDays);
    }

    /** 시작까지 남은 일수. 시작일 도래 후에는 NULL. */
    private Integer daysUntilStart(ChallengeGroup group, LocalDate today) {
        if (!today.isBefore(group.getStartDate())) {
            return null;
        }
        return (int) ChronoUnit.DAYS.between(today, group.getStartDate());
    }

    /* ══ 보조 ══════════════════════════════════════════════ */

    private GroupMember newMember(ChallengeGroup group, long userId) {
        int totalDays = daysBetweenInclusive(group.getStartDate(), group.getEndDate());
        return GroupMember.builder()
                .groupId(group.getId())
                .userId(userId)
                .livesCount(EvalType.valueOf(group.getEvalType()).livesFor(totalDays))
                .build();
    }

    private ChallengeGroup findGroupOrThrow(long groupId) {
        ChallengeGroup group = challengeGroupMapper.findById(groupId);
        if (group == null) {
            throw new BusinessException("GROUP_NOT_FOUND", "챌린지를 찾을 수 없습니다.");
        }
        return group;
    }

    private List<GroupMember> findMembers(long groupId) {
        return groupMemberMapper.findByGroupIds(List.of(groupId));
    }

    private boolean isMember(List<GroupMember> members, long userId) {
        return findMember(members, userId) != null;
    }

    private GroupMember findMember(List<GroupMember> members, long userId) {
        for (GroupMember member : members) {
            if (member.getUserId() != null && member.getUserId() == userId) {
                return member;
            }
        }
        return null;
    }

    /** 시작일·종료일을 모두 포함한 일수. 같은 날이면 1. */
    private int daysBetweenInclusive(LocalDate from, LocalDate to) {
        return (int) ChronoUnit.DAYS.between(from, to) + 1;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
