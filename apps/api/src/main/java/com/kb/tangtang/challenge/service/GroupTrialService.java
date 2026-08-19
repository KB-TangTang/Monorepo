package com.kb.tangtang.challenge.service;

import com.kb.tangtang.challenge.domain.Defense;
import com.kb.tangtang.challenge.domain.EvalType;
import com.kb.tangtang.challenge.domain.GroupIndictmentRow;
import com.kb.tangtang.challenge.domain.GroupTrialDetailRow;
import com.kb.tangtang.challenge.domain.IndictmentStatus;
import com.kb.tangtang.challenge.domain.TrialTodoRow;
import com.kb.tangtang.challenge.domain.TrialTransactionRow;
import com.kb.tangtang.challenge.domain.VoteCommentRow;
import com.kb.tangtang.challenge.dto.GroupIndictmentDto;
import com.kb.tangtang.challenge.dto.GroupTrialDetailDto;
import com.kb.tangtang.challenge.dto.MyTrialDto;
import com.kb.tangtang.challenge.dto.TrialAccusedDto;
import com.kb.tangtang.challenge.dto.TrialDefenseDto;
import com.kb.tangtang.challenge.dto.TrialTransactionDayDto;
import com.kb.tangtang.challenge.dto.TrialTransactionDto;
import com.kb.tangtang.challenge.dto.TrialTransactionsDto;
import com.kb.tangtang.challenge.dto.TrialVoteCommentDto;
import com.kb.tangtang.challenge.mapper.DefenseMapper;
import com.kb.tangtang.challenge.mapper.GroupChallengeResultMapper;
import com.kb.tangtang.challenge.mapper.IndictmentMapper;
import com.kb.tangtang.challenge.mapper.VoteMapper;
import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.common.storage.ImageStorage;
import com.kb.tangtang.common.util.PaymentMethodLabels;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 재판 조회 — 홈 「오늘의 할 일」·그룹 상세의 재판 카드(이슈 #169)와
 * 재판 상세·결산 구간 거래 목록(이슈 #170).
 *
 * <p><b>읽기 전용이다.</b> 변론 등록·혐의 인정은 {@code DefenseService} 가 맡는다 —
 * 상태를 바꾸는 쪽과 읽는 쪽을 한 클래스에 두면 {@code @Transactional(readOnly = true)} 를
 * 메서드마다 확인해야 한다.
 *
 * <p>{@link ChallengeGroupService} 에 넣지 않았다. 그쪽은 그룹 자체(생성·초대·참여)를 다루는데
 * 여기는 {@code tbl_indictment} 만 읽는다. 한 클래스에 묶으면 그룹을 고치는 사람과 재판을 고치는
 * 사람이 같은 파일에서 충돌한다 — 6명이 병렬로 작업하는 프로젝트다.
 *
 * <p><b>마감 시각은 여기서 계산한다.</b> {@code tbl_indictment} 에 마감 컬럼이 없어
 * {@code created_at} 에 {@code challenge.trial.*} 시간을 더한다. 계산을 매퍼(SQL)로 내리지 않은
 * 이유는 프로퍼티를 SQL 에 넣으려면 {@code ${}} 가 필요해서다 — 팀 규칙상 금지다.
 */
@Service
public class GroupTrialService {

    private final IndictmentMapper indictmentMapper;
    private final DefenseMapper defenseMapper;
    private final VoteMapper voteMapper;
    private final GroupChallengeResultMapper resultMapper;
    private final ImageStorage imageStorage;

    /** 기소 후 변론을 낼 수 있는 시간. */
    private final int defenseHours;

    /** 변론 마감 후 투표를 받는 시간. */
    private final int voteHours;

    /** 마감이 지났는지 판단하는 기준. 테스트가 고정 시각을 끼운다. */
    private final Clock clock;

    @Autowired
    public GroupTrialService(IndictmentMapper indictmentMapper,
                             DefenseMapper defenseMapper,
                             VoteMapper voteMapper,
                             GroupChallengeResultMapper resultMapper,
                             ImageStorage imageStorage,
                             @Value("${challenge.trial.defense-hours}") int defenseHours,
                             @Value("${challenge.trial.vote-hours}") int voteHours) {
        this(indictmentMapper, defenseMapper, voteMapper, resultMapper, imageStorage,
                defenseHours, voteHours, Clock.systemDefaultZone());
    }

    /** 테스트용. {@code DefenseService} · {@code VoteService} 와 같은 방식이다. */
    GroupTrialService(IndictmentMapper indictmentMapper,
                      DefenseMapper defenseMapper,
                      VoteMapper voteMapper,
                      GroupChallengeResultMapper resultMapper,
                      ImageStorage imageStorage,
                      int defenseHours,
                      int voteHours,
                      Clock clock) {
        this.indictmentMapper = indictmentMapper;
        this.defenseMapper = defenseMapper;
        this.voteMapper = voteMapper;
        this.resultMapper = resultMapper;
        this.imageStorage = imageStorage;
        this.defenseHours = defenseHours;
        this.voteHours = voteHours;
        this.clock = clock;
    }

    /**
     * 내가 지금 처리해야 하는 재판 전부. 마감 임박순.
     *
     * <p>변론 대기와 투표 대기를 각각 조회해 합친다. 한 쿼리로 UNION 하면 두 조회의 조인·조건이
     * 서로 다른데도 컬럼 수를 억지로 맞춰야 해서, 한쪽만 고칠 때 다른 쪽이 조용히 깨진다.
     * 한 사람이 동시에 지고 있는 재판은 많아야 수십 건이라 쿼리 두 번이 부담되지 않는다.
     *
     * <p><b>마감이 지난 건은 내리지 않는다.</b> TO-DO 는 「지금 처리할 수 있는 일」이고, 마감을 넘긴
     * 재판에 변론·투표를 넣으면 {@code DefenseService} · {@code VoteService} 가 그 자리에서 거절한다.
     * 목록에 남겨 두면 카운트다운이 {@code 00:00:00} 인 줄을 눌러 화면까지 들어간 뒤 제출에서야
     * 튕긴다. 특히 투표는 {@code VOTING} 을 풀어 줄 개표 배치가 아직 없어(#172) <b>영원히</b> 남는다.
     *
     * <p>상태 전이 배치와 판단 주체가 둘이 되는 것은 맞다. 다만 배치는 {@code status} 를 옮기는
     * 일을 하고 여기는 <b>보여줄지</b>를 정한다 — 마감을 넘긴 순간부터 배치가 돌기 전까지의 구간에서
     * 둘의 답이 갈릴 뿐이고, 그 구간에서 옳은 답은 「보여주지 않는다」다.
     */
    @Transactional(readOnly = true)
    public List<MyTrialDto> findMyTrials(long userId) {
        List<MyTrialDto> trials = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now(clock);

        for (TrialTodoRow row : indictmentMapper.findDefenseTodos(userId)) {
            LocalDateTime deadline = row.getCreatedAt().plusHours(defenseHours);
            if (deadline.isBefore(now)) {
                continue;
            }
            trials.add(MyTrialDto.builder()
                    .indictmentId(row.getIndictmentId())
                    .type("accuse")
                    .challengeId(row.getChallengeId())
                    .challengeName(row.getChallengeName())
                    .amount(row.getAmount())
                    .deadline(deadline)
                    .build());
        }

        for (TrialTodoRow row : indictmentMapper.findVoteTodos(userId)) {
            LocalDateTime deadline = row.getCreatedAt().plusHours(defenseHours + voteHours);
            if (deadline.isBefore(now)) {
                continue;
            }
            trials.add(MyTrialDto.builder()
                    .indictmentId(row.getIndictmentId())
                    .type("vote")
                    .challengeId(row.getChallengeId())
                    .challengeName(row.getChallengeName())
                    .defendantNickname(row.getDefendantNickname())
                    .voteCount(row.getVoteCount())
                    .totalVoters(row.getTotalVoters())
                    .deadline(deadline)
                    .build());
        }

        /*
         * 정렬은 서버가 끝낸다. 화면이 카드(상위 2건)와 바텀시트(전체) 두 곳에서 같은 배열을 쓰는데,
         * 프론트가 각자 정렬하면 카드에 보이던 건이 시트에서는 세 번째로 밀리는 일이 생긴다.
         * 같은 마감이면 id 순 — 순서가 매 요청 흔들리면 화면이 이유 없이 재정렬된다.
         */
        trials.sort(Comparator.comparing(MyTrialDto::getDeadline)
                .thenComparing(MyTrialDto::getIndictmentId));
        return trials;
    }

    /**
     * 그룹 상세의 재판 카드. 진행 중인 기소만, 오래된 순(= 마감이 급한 순).
     *
     * <p><b>참여자 검증을 하지 않는다.</b> 호출부({@link ChallengeGroupDetailService})가 먼저
     * 그룹 접근 권한을 확인한 뒤 부른다. 여기서 한 번 더 확인하려면 참여자 목록을 다시 읽어야 하고,
     * 검증이 두 군데로 갈리면 나중에 규칙이 바뀔 때 한쪽만 고쳐진다.
     *
     * <p>마감 두 개를 모두 채운다 — 화면이 {@code status} 로 골라 쓰는 이유는
     * {@link GroupIndictmentDto} 주석에 있다.
     */
    @Transactional(readOnly = true)
    public List<GroupIndictmentDto> findGroupIndictments(long userId, long groupId) {
        List<GroupIndictmentDto> cards = new ArrayList<>();
        for (GroupIndictmentRow row : indictmentMapper.findOpenByGroupId(groupId, userId)) {
            cards.add(GroupIndictmentDto.builder()
                    .id(row.getId())
                    .userId(row.getUserId())
                    .nickname(row.getNickname())
                    .profileImageUrl(imageStorage.urlOf(row.getProfileImageKey()))
                    .status(row.getStatus())
                    .settlementDate(row.getChallengeDate())
                    .exceededAmount(row.getExceededAmount())
                    .mine(row.getUserId() != null && row.getUserId() == userId)
                    .defended(row.isDefended())
                    .myVote(row.getMyVerdict())
                    .voteCount(row.getVoteCount())
                    .totalVoters(row.getTotalVoters())
                    .defenseDeadline(row.getCreatedAt().plusHours(defenseHours))
                    .voteDeadline(row.getCreatedAt().plusHours(defenseHours + voteHours))
                    .build());
        }
        return cards;
    }

    /**
     * 재판 상세 한 벌 (이슈 #170). 기소 안내 · 변론 작성 · 투표(#171) · 판결 상세가 모두 이것을 쓴다.
     *
     * <p>권한 검사는 매퍼가 겸한다 — 참여자 조인이 있어 그룹 밖 사람에게는 행이 없다.
     * 그 NULL 을 그대로 {@code TRIAL_NOT_FOUND} 로 바꾼다. 「권한 없음」과 구분해 주면
     * 남의 그룹에 그 기소가 존재한다는 사실이 새어 나간다.
     *
     * <p>변론을 따로 한 번 더 읽는다. 상세 쿼리에 조인하면 증빙 이미지 개수만큼 행이 늘어
     * 나머지 컬럼이 전부 중복된다.
     *
     * <p><b>표 분포와 코멘트는 개표 후에만 채운다</b>({@link #isCounted}). 매퍼는 항상 세지만
     * 투표 중이면 여기서 NULL 로 덮는다 — 이슈 #171 의 「개표 전 비율 비공개」 정책이다.
     */
    @Transactional(readOnly = true)
    public GroupTrialDetailDto findTrialDetail(long userId, long indictmentId) {
        GroupTrialDetailRow row = indictmentMapper.findTrialDetail(indictmentId, userId);
        if (row == null) {
            throw new BusinessException("TRIAL_NOT_FOUND", "재판을 찾을 수 없습니다.");
        }

        boolean mine = row.getUserId() != null && row.getUserId() == userId;
        boolean counted = isCounted(row.getStatus());
        return GroupTrialDetailDto.builder()
                .indictmentId(row.getIndictmentId())
                .groupId(row.getGroupId())
                .groupName(row.getGroupName())
                .status(row.getStatus())
                .result(row.getResult())
                .verdictMethod(row.getVerdictMethod())
                .message(row.getMessage())
                .accused(TrialAccusedDto.builder()
                        .userId(row.getUserId())
                        .nickname(row.getNickname())
                        .profileImageUrl(imageStorage.urlOf(row.getProfileImageKey()))
                        .mine(mine)
                        .build())
                .evalType(row.getEvalType())
                .challengeDate(row.getChallengeDate())
                .startDate(row.getStartDate())
                .endDate(row.getEndDate())
                .categoryId(row.getCategoryId())
                .categoryName(row.getCategoryName())
                .limitAmount(row.getLimitAmount())
                .currentAmount(row.getCurrentAmount())
                .exceededAmount(row.getExceededAmount())
                .createdAt(row.getCreatedAt())
                .defenseDeadline(row.getCreatedAt().plusHours(defenseHours))
                .voteDeadline(row.getCreatedAt().plusHours(defenseHours + voteHours))
                .defense(findDefense(indictmentId))
                .myVerdict(row.getMyVerdict())
                .voteCount(row.getVoteCount())
                .totalVoters(row.getTotalVoters())
                .guiltyCount(counted ? row.getGuiltyCount() : null)
                .innocentCount(counted ? row.getInnocentCount() : null)
                /*
                 * 판사 탕이의 사유도 같은 분기 안에 둔다(이슈 #172). AI 판결은 표가 동률일 때만
                 * 나오므로 사유가 내려가는 순간 「지금 2:2 다」가 드러난다 — guiltyCount 를 가린
                 * 의미가 없어진다. 확정 전에는 컬럼 자체가 NULL 이지만 조건에 기대지 않는다.
                 */
                .aiVerdictReason(counted ? row.getAiVerdictReason() : null)
                .comments(counted ? findComments(indictmentId) : null)
                .build();
    }

    /**
     * 표 분포와 코멘트를 내려보내도 되는 상태인가 (이슈 #171 결정).
     *
     * <p><b>개표 전에는 안 된다.</b> 투표 중에 유무죄 비율이 보이면 이기는 쪽에 표가 몰리고,
     * 코멘트 문장에는 어느 쪽에 던졌는지가 그대로 드러나 숫자를 가린 의미가 없어진다.
     * 진행 상황은 {@code voteCount / totalVoters}(몇 명이 던졌나)까지만 공개한다.
     *
     * <p><b>판단을 여기 한 곳에만 둔다.</b> SQL 이나 컨트롤러로 흩뿌리면 이슈 #172 가
     * 공개 조건을 풀 때 한쪽을 빠뜨린다. #172 는 이 메서드만 보면 된다.
     */
    private boolean isCounted(String status) {
        return IndictmentStatus.GUILTY.name().equals(status)
                || IndictmentStatus.INNOCENT.name().equals(status);
    }

    /** 익명 코멘트. 비어 있는 표는 매퍼가 이미 걸러 낸다. */
    private List<TrialVoteCommentDto> findComments(long indictmentId) {
        List<TrialVoteCommentDto> comments = new ArrayList<>();
        for (VoteCommentRow row : voteMapper.findCommentsByIndictmentId(indictmentId)) {
            comments.add(TrialVoteCommentDto.builder()
                    .comment(row.getComment())
                    .createdAt(row.getCreatedAt())
                    .build());
        }
        return comments;
    }

    /**
     * 결산 구간의 거래 목록 (이슈 #170). <b>피고 본인만</b> 볼 수 있다.
     *
     * <p>상세와 분리한 이유는 여기에 있다 — 상세는 투표자(#171)도 보는데 거래 목록까지 섞으면
     * 남의 거래내역이 배심원에게 흘러간다.
     *
     * <p>피고가 아니면 {@code TRIAL_NOT_FOUND} 다. 「권한 없음」으로 구분하면 그룹 안에서
     * 남의 재판에 거래 목록이 있다는 것까지는 알려 주는 셈이라, 여기서는 없는 것으로 취급한다.
     *
     * <p>날짜 범위는 {@code evalType} 이 정한다 — 일일결산은 위반한 그 하루, 기간결산은
     * 그룹 기간 전체다. 판단을 SQL 로 내리지 않는 이유는 매퍼 주석에 있다.
     */
    @Transactional(readOnly = true)
    public TrialTransactionsDto findTrialTransactions(long userId, long indictmentId) {
        GroupTrialDetailRow row = indictmentMapper.findTrialDetail(indictmentId, userId);
        if (row == null || row.getUserId() == null || row.getUserId() != userId) {
            throw new BusinessException("TRIAL_NOT_FOUND", "재판을 찾을 수 없습니다.");
        }

        boolean period = EvalType.PERIOD.name().equals(row.getEvalType());
        LocalDate from = period ? row.getStartDate() : row.getChallengeDate();
        LocalDate to = period ? row.getEndDate() : row.getChallengeDate();

        return TrialTransactionsDto.builder()
                .evalType(row.getEvalType())
                .limitAmount(row.getLimitAmount())
                .currentAmount(row.getCurrentAmount())
                .days(groupByDate(resultMapper.findTrialTransactions(
                        row.getGroupId(), userId, from, to)))
                .build();
    }

    /** 변론이 없으면 NULL. 빈 객체로 채우면 화면이 「변론 대기」와 구분할 수 없다. */
    private TrialDefenseDto findDefense(long indictmentId) {
        Defense defense = defenseMapper.findByIndictmentId(indictmentId);
        if (defense == null) {
            return null;
        }

        List<String> imageUrls = new ArrayList<>();
        for (String key : defenseMapper.findImageKeysByDefenseId(defense.getId())) {
            imageUrls.add(imageStorage.urlOf(key));
        }

        return TrialDefenseDto.builder()
                .content(defense.getContent())
                .actualBurdenAmount(defense.getActualBurdenAmount())
                .deductionAmount(defense.getDeductionAmount())
                .imageUrls(imageUrls)
                .createdAt(defense.getCreatedAt())
                .build();
    }

    /**
     * 거래를 날짜별로 묶는다. 매퍼가 날짜순으로 정렬해 주므로 <b>날짜가 바뀌는 지점만</b> 보면 된다.
     *
     * <p>{@code dailyAmount} 를 {@code tbl_group_challenge_daily_result} 에서 다시 읽지 않고
     * 목록을 더해 만든다 — 두 값이 어긋나면 화면에서 「합계와 항목이 다른」 하루가 나온다.
     * 조건도 환불 규칙도 집계 배치와 같은 조각을 쓰므로 결과는 같은 값이다.
     */
    private List<TrialTransactionDayDto> groupByDate(List<TrialTransactionRow> rows) {
        List<TrialTransactionDayDto> days = new ArrayList<>();

        LocalDate currentDate = null;
        List<TrialTransactionDto> items = null;
        BigDecimal dailyAmount = null;

        for (TrialTransactionRow row : rows) {
            if (!row.getTrDate().equals(currentDate)) {
                if (currentDate != null) {
                    days.add(TrialTransactionDayDto.builder()
                            .date(currentDate)
                            .dailyAmount(dailyAmount)
                            .transactions(items)
                            .build());
                }
                currentDate = row.getTrDate();
                items = new ArrayList<>();
                dailyAmount = BigDecimal.ZERO;
            }

            items.add(TrialTransactionDto.builder()
                    .transactionId(row.getTransactionId())
                    .time(row.getTrTime())
                    .merchantName(row.getMerchantName())
                    .amount(row.getAmount())
                    .categoryName(row.getCategoryName())
                    .paymentMethod(PaymentMethodLabels.resolve(row.getSourceType(),
                            row.getDirection(), row.getCardInstitutionName(),
                            row.getAccountBankName()))
                    .isRefund(row.isRefund())
                    .build());
            dailyAmount = dailyAmount.add(row.getAmount());
        }

        if (currentDate != null) {
            days.add(TrialTransactionDayDto.builder()
                    .date(currentDate)
                    .dailyAmount(dailyAmount)
                    .transactions(items)
                    .build());
        }
        return days;
    }
}
