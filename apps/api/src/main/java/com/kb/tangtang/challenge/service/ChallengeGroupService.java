package com.kb.tangtang.challenge.service;

import com.kb.tangtang.challenge.domain.ChallengeGroup;
import com.kb.tangtang.challenge.domain.ChallengeGroupStatus;
import com.kb.tangtang.challenge.domain.EvalType;
import com.kb.tangtang.challenge.domain.GroupMember;
import com.kb.tangtang.challenge.dto.ChallengeGroupCreateRequestDto;
import com.kb.tangtang.challenge.dto.ChallengeGroupCreatedDto;
import com.kb.tangtang.challenge.dto.ChallengeGroupDetailDto;
import com.kb.tangtang.challenge.dto.ChallengeGroupSummaryDto;
import com.kb.tangtang.challenge.dto.GroupMemberDto;
import com.kb.tangtang.challenge.dto.InviteCodePreviewDto;
import com.kb.tangtang.challenge.mapper.ChallengeGroupMapper;
import com.kb.tangtang.challenge.mapper.GroupMemberMapper;
import com.kb.tangtang.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 그룹 챌린지 생성 · 초대 · 참여 · 조회 (GC_01_01 ~ GC_01_06).
 *
 * <p>팀 결정 사항 (이슈 #151)
 * <ul>
 *   <li>정원은 6명 고정이다. 생성 화면에 입력 UI 가 없어 요청으로 받지 않는다.</li>
 *   <li>목숨은 <b>참여 시점에 계산해 INSERT</b> 한다. {@code lives_count} 가 NOT NULL 인데
 *       기본값이 없어서다. 뒤늦게 참여해도 목숨은 같다 — 의도된 정책이다.</li>
 *   <li>초대 코드에는 만료 컬럼이 없다. {@code start_date} 에서 파생한다 —
 *       <b>시작일 당일 23:59 까지 모집</b>, 그 다음 날부터 만료.</li>
 *   <li>제한 금액 0원은 유효하다. 무지출 챌린지 컨셉이 그 값을 쓴다.</li>
 * </ul>
 *
 * <p>상태 전이(RECRUITING → ACTIVE)와 시작 알림은 이 서비스가 하지 않는다. 별도 배치(이슈 #152).
 * 그래서 시작일이 지났어도 status 는 한동안 RECRUITING 일 수 있고, 참여 가능 판정은
 * status 가 아니라 <b>날짜</b>를 기준으로 한다.
 */
@Service
public class ChallengeGroupService {

    /** 정원 고정값. DDL 의 {@code ck_cg_members} 는 2~6 을 허용하지만 화면에 선택 UI 가 없다. */
    private static final int FIXED_MAX_MEMBERS = 6;

    private static final int MAX_GROUP_NAME_LENGTH = 100;
    private static final int MAX_MEMO_LENGTH = 300;

    /** {@code ck_cg_period} 가 강제하는 최대 기간(시작일 포함 7일). */
    private static final int MAX_PERIOD_DAYS = 7;

    private final ChallengeGroupMapper challengeGroupMapper;
    private final GroupMemberMapper groupMemberMapper;
    private final InviteCodeGenerator inviteCodeGenerator;
    private final Clock clock;

    @Autowired
    public ChallengeGroupService(ChallengeGroupMapper challengeGroupMapper,
                                 GroupMemberMapper groupMemberMapper,
                                 InviteCodeGenerator inviteCodeGenerator) {
        this(challengeGroupMapper, groupMemberMapper, inviteCodeGenerator, Clock.systemDefaultZone());
    }

    ChallengeGroupService(ChallengeGroupMapper challengeGroupMapper,
                          GroupMemberMapper groupMemberMapper,
                          InviteCodeGenerator inviteCodeGenerator,
                          Clock clock) {
        this.challengeGroupMapper = challengeGroupMapper;
        this.groupMemberMapper = groupMemberMapper;
        this.inviteCodeGenerator = inviteCodeGenerator;
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
        String normalized = normalizeInviteCode(inviteCode);
        ChallengeGroup group = challengeGroupMapper.findByInviteCode(normalized);
        if (group == null) {
            throw new BusinessException("GROUP_INVITE_CODE_NOT_FOUND", "유효하지 않은 초대 코드입니다.");
        }

        List<GroupMember> members = findMembers(group.getId());
        ChallengeGroupDetailDto detail = toDetail(userId, group, members);
        String reason = joinBlockReason(group, members, userId);

        return InviteCodePreviewDto.builder()
                .challenge(detail)
                .joinable(reason == null)
                .reason(reason)
                .build();
    }

    /** 참여 (GC_01_06). 성공하면 참여한 그룹의 상세를 그대로 돌려준다 — 화면이 바로 상세로 넘어간다. */
    @Transactional
    public ChallengeGroupDetailDto join(long userId, long groupId) {
        ChallengeGroup group = findGroupOrThrow(groupId);
        List<GroupMember> members = findMembers(groupId);

        String reason = joinBlockReason(group, members, userId);
        if (reason != null) {
            throw joinBlocked(reason);
        }

        groupMemberMapper.insertMember(newMember(group, userId));
        return toDetail(userId, group, findMembers(groupId));
    }

    /* ══ 조회 ══════════════════════════════════════════════ */

    /**
     * 내가 참여 중인 그룹 목록 (GC_01_01).
     *
     * @param statuses NULL·빈 목록이면 전체
     */
    @Transactional(readOnly = true)
    public List<ChallengeGroupSummaryDto> findMyGroups(long userId, List<String> statuses) {
        List<ChallengeGroup> groups = challengeGroupMapper.findMyGroups(userId, validateStatuses(statuses));
        if (groups.isEmpty()) {
            return List.of();
        }

        // 그룹마다 참여자를 따로 읽으면 N+1 이 된다. 한 번에 읽어 groupId 로 나눈다.
        List<Long> groupIds = groups.stream().map(ChallengeGroup::getId).toList();
        Map<Long, List<GroupMember>> membersByGroup = groupMemberMapper.findByGroupIds(groupIds).stream()
                .collect(Collectors.groupingBy(GroupMember::getGroupId));

        LocalDate today = LocalDate.now(clock);
        List<ChallengeGroupSummaryDto> result = new ArrayList<>(groups.size());
        for (ChallengeGroup group : groups) {
            List<GroupMember> members = membersByGroup.getOrDefault(group.getId(), List.of());
            result.add(toSummary(userId, group, members, today));
        }
        return result;
    }

    /** 그룹 상세 (GC_01_09). 참여자만 볼 수 있다 — 비참여자는 초대 코드 미리보기를 쓴다. */
    @Transactional(readOnly = true)
    public ChallengeGroupDetailDto findDetail(long userId, long groupId) {
        ChallengeGroup group = findGroupOrThrow(groupId);
        List<GroupMember> members = findMembers(groupId);
        if (!isMember(members, userId)) {
            throw new BusinessException("GROUP_NOT_MEMBER", "참여 중인 챌린지가 아닙니다.");
        }
        return toDetail(userId, group, members);
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
        if (startDate.isBefore(today)) {
            throw new BusinessException("GROUP_START_DATE_INVALID", "시작일은 오늘 이후여야 합니다.");
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

        return evalType;
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
     * 참여를 막는 사유. 참여할 수 있으면 NULL.
     *
     * 순서에 의미가 있다. 이미 참여 중이면 종료·만료 여부와 무관하게 그룹으로 보내야 하므로
     * ALREADY_JOINED 를 먼저 본다.
     */
    private String joinBlockReason(ChallengeGroup group, List<GroupMember> members, long userId) {
        if (isMember(members, userId)) {
            return "ALREADY_JOINED";
        }
        ChallengeGroupStatus status = ChallengeGroupStatus.valueOf(group.getStatus());
        if (status == ChallengeGroupStatus.JUDGING || status == ChallengeGroupStatus.CLOSED) {
            return "CLOSED";
        }
        // 만료 컬럼이 없어 날짜로 판정한다. 시작일 당일까지는 모집한다.
        if (LocalDate.now(clock).isAfter(group.getStartDate())) {
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

    private ChallengeGroupDetailDto toDetail(long userId, ChallengeGroup group, List<GroupMember> members) {
        ChallengeGroupSummaryDto summary = toSummary(userId, group, members, LocalDate.now(clock));
        return ChallengeGroupDetailDto.builder()
                .challenge(summary)
                .memo(group.getMemo())
                .member(isMember(members, userId))
                .joinable(joinBlockReason(group, members, userId) == null)
                .build();
    }

    private ChallengeGroupSummaryDto toSummary(long userId,
                                               ChallengeGroup group,
                                               List<GroupMember> members,
                                               LocalDate today) {
        int totalDays = daysBetweenInclusive(group.getStartDate(), group.getEndDate());
        EvalType evalType = EvalType.valueOf(group.getEvalType());
        GroupMember me = findMember(members, userId);

        return ChallengeGroupSummaryDto.builder()
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
                .members(members.stream()
                        .map(m -> toMemberDto(m, group.getAdminId()))
                        .toList())
                .build();
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
