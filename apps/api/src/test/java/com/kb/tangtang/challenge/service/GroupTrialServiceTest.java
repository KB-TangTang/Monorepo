package com.kb.tangtang.challenge.service;

import com.kb.tangtang.challenge.domain.GroupClosedTrialRow;
import com.kb.tangtang.challenge.domain.GroupIndictmentRow;
import com.kb.tangtang.challenge.domain.GroupTrialDetailRow;
import com.kb.tangtang.challenge.domain.IndictmentStatus;
import com.kb.tangtang.challenge.domain.TrialTodoRow;
import com.kb.tangtang.challenge.domain.VerdictMethod;
import com.kb.tangtang.challenge.dto.GroupClosedTrialDto;
import com.kb.tangtang.challenge.dto.GroupIndictmentDto;
import com.kb.tangtang.challenge.dto.GroupTrialDetailDto;
import com.kb.tangtang.challenge.dto.MyTrialDto;
import com.kb.tangtang.challenge.mapper.DefenseMapper;
import com.kb.tangtang.challenge.mapper.GroupChallengeResultMapper;
import com.kb.tangtang.challenge.mapper.IndictmentMapper;
import com.kb.tangtang.challenge.mapper.VoteMapper;
import com.kb.tangtang.common.storage.ImageStorage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * 홈 「오늘의 할 일」 조립 (이슈 #169).
 *
 * <p>조회 조건 자체는 SQL 이 하고 {@code ChallengeMapperXmlTest} 가 모양을 지킨다.
 * 여기서는 <b>SQL 이 만들 수 없는 것</b> — 마감 시각 계산, 두 목록의 병합 순서,
 * {@code type} 문자열 — 만 본다.
 */
@ExtendWith(MockitoExtension.class)
class GroupTrialServiceTest {

    private static final int DEFENSE_HOURS = 6;
    private static final int VOTE_HOURS = 24;
    private static final long USER_ID = 7L;

    /**
     * 테스트가 보는 「지금」. 아래 픽스처의 기소 시각과 같은 값이라 마감은 전부 미래다.
     *
     * <p>고정하지 않으면 마감이 지난 건을 거르는 규칙 때문에 <b>픽스처가 하루 만에 썩는다</b>.
     */
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 16, 9, 0);

    private static final Clock CLOCK = Clock.fixed(
            NOW.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());

    @Mock private IndictmentMapper indictmentMapper;
    @Mock private DefenseMapper defenseMapper;
    @Mock private VoteMapper voteMapper;
    @Mock private GroupChallengeResultMapper resultMapper;
    @Mock private ImageStorage imageStorage;

    private GroupTrialService service() {
        return new GroupTrialService(indictmentMapper, defenseMapper, voteMapper, resultMapper,
                imageStorage, DEFENSE_HOURS, VOTE_HOURS, CLOCK);
    }

    private TrialTodoRow row(long indictmentId, LocalDateTime createdAt) {
        TrialTodoRow row = new TrialTodoRow();
        row.setIndictmentId(indictmentId);
        row.setChallengeId(1L);
        row.setChallengeName("배달 소비 줄이기");
        row.setCreatedAt(createdAt);
        return row;
    }

    /**
     * 변론 마감은 {@code created_at + defense-hours}.
     *
     * <p>기준점을 「지금」으로 잡으면 화면을 열 때마다 마감이 뒤로 밀려 재판이 영원히 안 끝난다.
     */
    @Test
    @DisplayName("변론 마감은 기소 시각에 변론 시간을 더한 값이다")
    void defenseDeadlineIsCreatedAtPlusDefenseHours() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 16, 9, 0);
        TrialTodoRow row = row(11L, createdAt);
        row.setAmount(new BigDecimal("6800"));
        when(indictmentMapper.findDefenseTodos(USER_ID)).thenReturn(List.of(row));
        when(indictmentMapper.findVoteTodos(USER_ID)).thenReturn(List.of());

        MyTrialDto trial = service().findMyTrials(USER_ID).get(0);

        assertEquals("accuse", trial.getType());
        assertEquals(createdAt.plusHours(DEFENSE_HOURS), trial.getDeadline());
        assertEquals(new BigDecimal("6800"), trial.getAmount());
    }

    /**
     * 투표 마감은 <b>변론 시간까지 더한 뒤</b> 투표 시간을 더한다.
     *
     * <p>투표 시간만 더하면 변론 마감과 투표 마감이 겹쳐, 변론을 낸 직후 투표가 이미 끝난 것으로 뜬다.
     */
    @Test
    @DisplayName("투표 마감은 변론 시간과 투표 시간을 모두 더한 값이다")
    void voteDeadlineIncludesDefenseWindow() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 16, 9, 0);
        TrialTodoRow row = row(21L, createdAt);
        row.setDefendantNickname("지판");
        row.setVoteCount(3);
        row.setTotalVoters(5);
        when(indictmentMapper.findDefenseTodos(USER_ID)).thenReturn(List.of());
        when(indictmentMapper.findVoteTodos(USER_ID)).thenReturn(List.of(row));

        MyTrialDto trial = service().findMyTrials(USER_ID).get(0);

        assertEquals("vote", trial.getType());
        assertEquals(createdAt.plusHours(DEFENSE_HOURS + VOTE_HOURS), trial.getDeadline());
        assertEquals("지판", trial.getDefendantNickname());
        assertEquals(3, trial.getVoteCount());
        assertEquals(5, trial.getTotalVoters());
    }

    /**
     * 마감을 넘긴 재판은 TO-DO 에서 뺀다.
     *
     * <p>남겨 두면 카운트다운이 {@code 00:00:00} 인 줄을 눌러 화면까지 들어간 뒤 제출에서야 튕긴다.
     * 특히 투표는 {@code VOTING} 을 풀어 줄 개표 배치가 없어(#172) 영원히 남는다.
     */
    @Test
    @DisplayName("마감이 지난 변론·투표는 할 일 목록에서 빠진다")
    void dropsExpiredTodos() {
        /* 변론 마감 = -1h, 투표 마감 = -1h. 둘 다 이미 지났다 */
        when(indictmentMapper.findDefenseTodos(USER_ID))
                .thenReturn(List.of(row(11L, NOW.minusHours(DEFENSE_HOURS + 1))));
        when(indictmentMapper.findVoteTodos(USER_ID))
                .thenReturn(List.of(row(21L, NOW.minusHours(DEFENSE_HOURS + VOTE_HOURS + 1))));

        assertEquals(List.of(), service().findMyTrials(USER_ID));
    }

    /**
     * 경계는 「지났다」가 아니다. 마감 시각 정각에 낸 표를 {@code VoteService} 는 받는다 —
     * 목록에서 먼저 지우면 화면은 막는데 서버는 받는 구간이 생긴다.
     */
    @Test
    @DisplayName("마감 시각 정각은 아직 남아 있는 것으로 본다")
    void keepsTodoExactlyAtDeadline() {
        when(indictmentMapper.findDefenseTodos(USER_ID))
                .thenReturn(List.of(row(11L, NOW.minusHours(DEFENSE_HOURS))));
        when(indictmentMapper.findVoteTodos(USER_ID)).thenReturn(List.of());

        assertEquals(1, service().findMyTrials(USER_ID).size());
    }

    /**
     * 두 목록을 이어 붙이기만 하면 변론 5건이 앞에 깔리고 마감 10분 남은 투표가 맨 뒤로 간다.
     * 화면은 상위 2건만 카드에 보여준다 — 정렬이 곧 「무엇을 먼저 하라고 안내할지」다.
     */
    @Test
    @DisplayName("변론과 투표를 섞어 마감 임박순으로 정렬한다")
    void mergesBothListsSortedByDeadline() {
        LocalDateTime base = LocalDateTime.of(2026, 8, 16, 9, 0);
        /* 변론 마감 = base+6h. 투표 마감 = (base-30h)+30h = base — 투표가 먼저 와야 한다 */
        when(indictmentMapper.findDefenseTodos(USER_ID)).thenReturn(List.of(row(11L, base)));
        when(indictmentMapper.findVoteTodos(USER_ID)).thenReturn(List.of(row(21L, base.minusHours(30))));

        List<MyTrialDto> trials = service().findMyTrials(USER_ID);

        assertEquals(List.of(21L, 11L),
                trials.stream().map(MyTrialDto::getIndictmentId).toList());
    }

    /**
     * 마감이 같으면 id 순. 없으면 새로고침마다 순서가 흔들려 화면이 이유 없이 재정렬된다.
     */
    @Test
    @DisplayName("마감이 같으면 기소 id 순으로 고정된다")
    void tiesBreakByIndictmentId() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 16, 9, 0);
        when(indictmentMapper.findDefenseTodos(USER_ID))
                .thenReturn(List.of(row(30L, createdAt), row(12L, createdAt)));
        when(indictmentMapper.findVoteTodos(USER_ID)).thenReturn(List.of());

        List<MyTrialDto> trials = service().findMyTrials(USER_ID);

        assertEquals(List.of(12L, 30L),
                trials.stream().map(MyTrialDto::getIndictmentId).toList());
    }

    /**
     * 종류에 따라 비는 필드는 0 이 아니라 <b>null</b> 이어야 한다.
     * {@code voteCount} 가 0 으로 내려가면 변론 줄에 「0/0 투표」가 찍힌다.
     */
    @Test
    @DisplayName("변론 줄에는 투표 관련 필드를 채우지 않는다")
    void defenseRowLeavesVoteFieldsNull() {
        TrialTodoRow row = row(11L, LocalDateTime.of(2026, 8, 16, 9, 0));
        row.setVoteCount(4);
        row.setTotalVoters(5);
        row.setDefendantNickname("나");
        when(indictmentMapper.findDefenseTodos(USER_ID)).thenReturn(List.of(row));
        when(indictmentMapper.findVoteTodos(USER_ID)).thenReturn(List.of());

        MyTrialDto trial = service().findMyTrials(USER_ID).get(0);

        assertNull(trial.getVoteCount());
        assertNull(trial.getTotalVoters());
        assertNull(trial.getDefendantNickname());
    }

    /* ══ 그룹 상세 재판 카드 ═══════════════════════════════ */

    private GroupIndictmentRow indictmentRow(long defendantId, LocalDateTime createdAt) {
        GroupIndictmentRow row = new GroupIndictmentRow();
        row.setId(11L);
        row.setUserId(defendantId);
        row.setNickname("지판");
        row.setStatus("DEFENSE_WAIT");
        row.setChallengeDate(LocalDate.of(2026, 8, 5));
        row.setExceededAmount(new BigDecimal("6800"));
        row.setCreatedAt(createdAt);
        return row;
    }

    /**
     * 마감 두 개를 <b>둘 다</b> 채운다. 상태에 맞는 쪽만 채우면 상태 전이 배치가 도는 순간
     * 화면에서 마감이 잠깐 사라진다 — 어느 쪽을 쓸지는 화면이 정한다.
     */
    @Test
    @DisplayName("재판 카드는 변론·투표 마감을 모두 채운다")
    void groupCardFillsBothDeadlines() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 16, 9, 0);
        when(indictmentMapper.findOpenByGroupId(3L, USER_ID))
                .thenReturn(List.of(indictmentRow(9L, createdAt)));

        GroupIndictmentDto card = service().findGroupIndictments(USER_ID, 3L).get(0);

        assertEquals(createdAt.plusHours(DEFENSE_HOURS), card.getDefenseDeadline());
        assertEquals(createdAt.plusHours(DEFENSE_HOURS + VOTE_HOURS), card.getVoteDeadline());
        assertEquals(LocalDate.of(2026, 8, 5), card.getSettlementDate());
    }

    /**
     * {@code mine} 이 뒤집히면 남의 재판에 「내 변론이 필요해요」가 뜨고, 변론 화면으로 들어가
     * 남을 대신해 변론을 쓰게 된다.
     */
    @Test
    @DisplayName("피고가 나면 mine 이 참이다")
    void markesMyOwnIndictment() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 16, 9, 0);
        when(indictmentMapper.findOpenByGroupId(3L, USER_ID))
                .thenReturn(List.of(indictmentRow(USER_ID, createdAt), indictmentRow(9L, createdAt)));

        List<GroupIndictmentDto> cards = service().findGroupIndictments(USER_ID, 3L);

        assertEquals(List.of(true, false), cards.stream().map(GroupIndictmentDto::isMine).toList());
    }

    /** 저장된 값은 키다. 그대로 내보내면 화면이 {@code profile/9/a.jpg} 를 이미지 주소로 쓴다. */
    @Test
    @DisplayName("프로필 이미지 키는 URL 로 바꿔 내려간다")
    void resolvesProfileImageUrl() {
        GroupIndictmentRow row = indictmentRow(9L, LocalDateTime.of(2026, 8, 16, 9, 0));
        row.setProfileImageKey("profile/9/a.jpg");
        when(indictmentMapper.findOpenByGroupId(3L, USER_ID)).thenReturn(List.of(row));
        when(imageStorage.urlOf("profile/9/a.jpg")).thenReturn("http://localhost/images/profile/9/a.jpg");

        GroupIndictmentDto card = service().findGroupIndictments(USER_ID, 3L).get(0);

        assertEquals("http://localhost/images/profile/9/a.jpg", card.getProfileImageUrl());
    }

    /* ══ 재판 상세 — 개표 공개 조건 (이슈 #171·#172) ══════════════ */

    private GroupTrialDetailRow detailRow(String status) {
        GroupTrialDetailRow row = new GroupTrialDetailRow();
        row.setIndictmentId(11L);
        row.setGroupId(3L);
        row.setUserId(9L);
        row.setStatus(status);
        row.setVerdictMethod(IndictmentStatus.VOTING.name().equals(status)
                ? null : VerdictMethod.AI_JUDGMENT.name());
        row.setAiVerdictReason("변론이 사실과 다르다");
        row.setGuiltyCount(2);
        row.setInnocentCount(2);
        row.setCreatedAt(NOW);
        return row;
    }

    /**
     * <b>AI 판결은 표가 동률일 때만 나온다.</b> 그래서 사유가 내려가는 순간 「지금 2:2 다」가
     * 드러나 {@code guiltyCount} 를 가린 의미가 없어진다 — 같은 분기에서 함께 가려야 한다.
     */
    @Test
    @DisplayName("개표 전에는 판사 탕이의 사유도 표수와 함께 가린다")
    void masksAiReasonBeforeTally() {
        when(indictmentMapper.findTrialDetail(11L, USER_ID))
                .thenReturn(detailRow(IndictmentStatus.VOTING.name()));

        GroupTrialDetailDto detail = service().findTrialDetail(USER_ID, 11L);

        assertNull(detail.getGuiltyCount());
        assertNull(detail.getAiVerdictReason());
    }

    /** 확정된 뒤에는 원문 그대로 내려간다. 화면이 이 문장을 판결문에 띄운다. */
    @Test
    @DisplayName("확정된 재판은 판결 방식과 사유를 그대로 내려보낸다")
    void exposesAiReasonAfterTally() {
        when(indictmentMapper.findTrialDetail(11L, USER_ID))
                .thenReturn(detailRow(IndictmentStatus.GUILTY.name()));

        GroupTrialDetailDto detail = service().findTrialDetail(USER_ID, 11L);

        assertEquals(VerdictMethod.AI_JUDGMENT.name(), detail.getVerdictMethod());
        assertEquals("변론이 사실과 다르다", detail.getAiVerdictReason());
        assertEquals(2, detail.getGuiltyCount());
    }

    /* ══ 지방법원 홈 「재판 현황」 (이슈 #432) ═══════════════════ */

    /**
     * 6가지 입장 중 하나를 만든다. 조합을 만드는 축이 네 개(피고 · 상태 · 변론 · 내 표)라
     * 픽스처를 상태별로 두면 여섯 벌이 된다 — 하나만 고치고 나머지를 잊는다.
     */
    private GroupIndictmentRow trialRow(long id, long defendantId, String status,
                                        boolean defended, String myVerdict,
                                        LocalDateTime createdAt) {
        GroupIndictmentRow row = new GroupIndictmentRow();
        row.setId(id);
        row.setGroupId(3L);
        row.setGroupName("배달 소비 줄이기");
        row.setUserId(defendantId);
        row.setNickname("지판");
        row.setStatus(status);
        row.setChallengeDate(LocalDate.of(2026, 8, 5));
        row.setExceededAmount(new BigDecimal("6800"));
        row.setDefended(defended);
        row.setMyVerdict(myVerdict);
        row.setCreatedAt(createdAt);
        return row;
    }

    /**
     * 여섯 가지가 <b>모두</b> 내려가야 한다. 예전 {@code findMyTrials} 는 1·5번(내가 지금
     * 행동할 수 있는 것)만 줬고, 그래서 <b>내가 기소당해 그룹원들이 나를 심판하는 중인
     * 상태가 화면에서 사라졌다</b> — 목숨이 걸려 있어 가장 궁금한 상태다.
     *
     * <p>거르는 축이 상태 하나가 아니라 넷이라, 한 축만 잘못 걸어도 특정 상태가 통째로
     * 사라지면서 화면은 「재판이 없네」로 멀쩡히 보인다.
     */
    @Test
    @DisplayName("재판 현황은 내 입장 6가지를 모두 내려보낸다")
    void allMyTrialsCoversEverySixStates() {
        when(indictmentMapper.findOpenByUserId(USER_ID)).thenReturn(List.of(
                trialRow(1L, USER_ID, "DEFENSE_WAIT", false, null, NOW),
                trialRow(2L, USER_ID, "DEFENSE_WAIT", true, null, NOW),
                trialRow(3L, USER_ID, "VOTING", true, null, NOW),
                trialRow(4L, 9L, "DEFENSE_WAIT", false, null, NOW),
                trialRow(5L, 9L, "VOTING", true, null, NOW),
                trialRow(6L, 9L, "VOTING", true, "GUILTY", NOW)));

        List<GroupIndictmentDto> cards = service().findAllMyTrials(USER_ID);

        assertEquals(6, cards.size());
        assertEquals(Set.of(1L, 2L, 3L, 4L, 5L, 6L),
                cards.stream().map(GroupIndictmentDto::getId).collect(Collectors.toSet()));
        /*
         * 순서는 아래 sortsActionableFirstThenByDeadline 이 본다. 여기서는 할 일 두 건이
         * 앞으로 나왔다는 것까지만 — 마감이 전부 같아도 상태에 따라 6h·30h 로 갈려서
         * 뒤쪽 네 건의 순서까지 못박으면 이 테스트가 정렬 규칙 테스트와 겹친다.
         */
        assertEquals(List.of(1L, 5L),
                cards.stream().limit(2).map(GroupIndictmentDto::getId).toList());
    }

    /**
     * 할 일이 있는 것(내 변론 필요 · 내 투표 필요)이 먼저 오고, 그 안에서 마감 임박순이다.
     *
     * <p>정렬을 화면에 맡기면 아코디언을 펼칠 때마다 카드가 다시 튄다. 위 6가지 테스트가
     * 순서까지 보지만 그건 마감이 전부 같을 때의 순서라 — 여기서 마감을 벌려 확인한다.
     */
    @Test
    @DisplayName("할 일이 있는 재판이 먼저 오고 그 안에서 마감 임박순이다")
    void sortsActionableFirstThenByDeadline() {
        when(indictmentMapper.findOpenByUserId(USER_ID)).thenReturn(List.of(
                // 할 일 없음 · 마감이 가장 급하다 (그래도 뒤로 간다)
                trialRow(1L, 9L, "VOTING", true, "GUILTY", NOW.minusHours(25)),
                // 할 일 있음 · 마감이 가장 멀다
                trialRow(2L, 9L, "VOTING", true, null, NOW),
                // 할 일 있음 · 마감이 더 급하다
                trialRow(3L, USER_ID, "DEFENSE_WAIT", false, null, NOW.minusHours(1))));

        List<GroupIndictmentDto> cards = service().findAllMyTrials(USER_ID);

        assertEquals(List.of(3L, 2L, 1L),
                cards.stream().map(GroupIndictmentDto::getId).toList());
    }

    /**
     * 마감 판단 기준이 <b>상태별로 다르다.</b> 변론 대기는 변론 마감(6h), 투표 중은 투표 마감(30h)이다.
     *
     * <p>한쪽 기준만 쓰면 둘 중 하나가 통째로 어긋난다 — 투표 마감으로만 재면 변론 마감이
     * 24시간 지난 재판이 「변론 필요」로 남아 눌러 들어간 뒤 제출에서야 튕기고,
     * 변론 마감으로만 재면 투표가 열린 재판이 열리자마자 목록에서 사라진다.
     */
    @Test
    @DisplayName("마감이 지난 재판은 상태에 맞는 마감으로 걸러진다")
    void dropsExpiredByStatusSpecificDeadline() {
        when(indictmentMapper.findOpenByUserId(USER_ID)).thenReturn(List.of(
                // 변론 마감(6h)이 지났다 → 빠진다
                trialRow(1L, USER_ID, "DEFENSE_WAIT", false, null, NOW.minusHours(7)),
                // 변론 마감은 지났지만 투표 마감(30h)은 남았다 → 남는다
                trialRow(2L, 9L, "VOTING", true, null, NOW.minusHours(7)),
                // 투표 마감도 지났다 → 빠진다
                trialRow(3L, 9L, "VOTING", true, null, NOW.minusHours(31))));

        List<GroupIndictmentDto> cards = service().findAllMyTrials(USER_ID);

        assertEquals(List.of(2L), cards.stream().map(GroupIndictmentDto::getId).toList());
    }

    /**
     * 카드 제목의 카테고리·한도는 화면이 참여 그룹 목록과 조인해 채운다. 그 열쇠가 {@code groupId} 다.
     * 빠지면 여러 그룹이 섞인 목록에서 어느 그룹 것인지 알 수 없어 조인 자체가 불가능하다.
     */
    @Test
    @DisplayName("재판 카드는 소속 그룹을 함께 내려보낸다")
    void allMyTrialsCarriesGroup() {
        when(indictmentMapper.findOpenByUserId(USER_ID))
                .thenReturn(List.of(trialRow(1L, 9L, "VOTING", true, null, NOW)));

        GroupIndictmentDto card = service().findAllMyTrials(USER_ID).get(0);

        assertEquals(3L, card.getGroupId());
        assertEquals("배달 소비 줄이기", card.getGroupName());
    }

    /* ══ 확정된 재판 기록 ═══════════════════════════════════ */

    private GroupClosedTrialRow closedRow(long id, long defendantId, String status,
                                          String verdictMethod, LocalDateTime confirmedAt) {
        GroupClosedTrialRow row = new GroupClosedTrialRow();
        row.setId(id);
        row.setGroupId(3L);
        row.setGroupName("배달 소비 줄이기");
        row.setUserId(defendantId);
        row.setNickname("지판");
        row.setStatus(status);
        row.setVerdictMethod(verdictMethod);
        row.setChallengeDate(LocalDate.of(2026, 8, 5));
        row.setExceededAmount(new BigDecimal("6800"));
        row.setGuiltyCount(3);
        row.setInnocentCount(1);
        row.setTotalVoters(4);
        row.setCreatedAt(confirmedAt.minusHours(30));
        row.setConfirmedAt(confirmedAt);
        return row;
    }

    /**
     * 매퍼가 준 순서를 그대로 둔다. SQL 이 {@code updated_at DESC} 로 정렬을 끝냈다.
     *
     * <p>진행 중 목록({@code findAllMyTrials})은 자바에서 다시 정렬하는데, 그 습관으로 여기에도
     * {@code sort} 를 넣으면 <b>기준이 두 곳으로 갈린다.</b> 확정 재판에는 마감이 없어
     * 저쪽의 비교자를 그대로 쓸 수도 없다 — 마감 필드가 통째로 null 이라 NPE 가 난다.
     */
    @Test
    @DisplayName("재판 기록은 매퍼가 준 순서를 그대로 유지한다")
    void trialRecordsKeepMapperOrder() {
        when(indictmentMapper.findClosedByUserId(USER_ID)).thenReturn(List.of(
                closedRow(30L, 9L, "GUILTY", "VOTE", NOW),
                closedRow(12L, USER_ID, "INNOCENT", "NO_VOTE", NOW.minusDays(1)),
                closedRow(41L, 9L, "GUILTY", "CONFESSION", NOW.minusDays(2))));

        List<GroupClosedTrialDto> records = service().findAllMyTrialRecords(USER_ID);

        assertEquals(List.of(30L, 12L, 41L),
                records.stream().map(GroupClosedTrialDto::getId).toList());
    }

    /**
     * <b>개표를 가리지 않는다.</b> {@code findTrialDetail} 이 투표 중에 {@code guiltyCount} 를
     * null 로 덮는 것(이슈 #171)은 편승 투표를 막기 위함인데, 그 이유는 투표가 열려 있을 때만
     * 성립한다. 여기 오는 행은 전부 확정된 재판이라 마스킹을 따라 하면 기록 화면의
     * 개표 표시가 통째로 0 이 된다.
     */
    @Test
    @DisplayName("재판 기록은 개표 결과를 가리지 않는다")
    void trialRecordsExposeTally() {
        when(indictmentMapper.findClosedByGroupId(3L, USER_ID))
                .thenReturn(List.of(closedRow(30L, 9L, "GUILTY", "VOTE", NOW)));

        GroupClosedTrialDto record = service().findGroupTrialRecords(USER_ID, 3L).get(0);

        assertEquals(3, record.getGuiltyCount());
        assertEquals(1, record.getInnocentCount());
        assertEquals(4, record.getTotalVoters());
    }

    /**
     * 판사 탕이의 사유도 그대로 내려보낸다. 같은 이유다 — 확정 후에는 감출 것이 없다.
     *
     * <p>{@code verdictMethod} 를 함께 못박는다. 화면이 이 값으로 이동할 상세를 가르기 때문에
     * 빠지면 AI 가 판결한 재판이 일반 판결문 화면으로 열린다.
     */
    @Test
    @DisplayName("AI 판결 기록은 판결 방식과 사유를 그대로 담는다")
    void trialRecordsKeepAiVerdictReason() {
        GroupClosedTrialRow row = closedRow(30L, 9L, "GUILTY",
                VerdictMethod.AI_JUDGMENT.name(), NOW);
        row.setAiVerdictReason("변론에 증빙이 없어 유죄로 판단했다.");
        when(indictmentMapper.findClosedByUserId(USER_ID)).thenReturn(List.of(row));

        GroupClosedTrialDto record = service().findAllMyTrialRecords(USER_ID).get(0);

        assertEquals(VerdictMethod.AI_JUDGMENT.name(), record.getVerdictMethod());
        assertEquals("변론에 증빙이 없어 유죄로 판단했다.", record.getAiVerdictReason());
    }

    /** 내가 피고인 기록에만 {@code mine} 이 선다. 화면이 이걸로 강조선을 그린다. */
    @Test
    @DisplayName("내가 피고인 기록만 mine 이 true 다")
    void trialRecordsMarkMine() {
        when(indictmentMapper.findClosedByUserId(USER_ID)).thenReturn(List.of(
                closedRow(30L, USER_ID, "GUILTY", "CONFESSION", NOW),
                closedRow(31L, 9L, "INNOCENT", "VOTE", NOW.minusDays(1))));

        List<GroupClosedTrialDto> records = service().findAllMyTrialRecords(USER_ID);

        assertTrue(records.get(0).isMine());
        assertFalse(records.get(1).isMine());
    }

    /** 프로필은 저장된 키가 아니라 조립된 URL 로 내려간다. 진행 중 카드와 같은 규칙이다. */
    @Test
    @DisplayName("재판 기록의 프로필은 URL 로 변환된다")
    void trialRecordsConvertProfileKeyToUrl() {
        GroupClosedTrialRow row = closedRow(30L, 9L, "GUILTY", "VOTE", NOW);
        row.setProfileImageKey("profile/9.png");
        when(indictmentMapper.findClosedByUserId(USER_ID)).thenReturn(List.of(row));
        when(imageStorage.urlOf("profile/9.png")).thenReturn("https://cdn/profile/9.png");

        GroupClosedTrialDto record = service().findAllMyTrialRecords(USER_ID).get(0);

        assertEquals("https://cdn/profile/9.png", record.getProfileImageUrl());
    }

    /**
     * 확정 재판이 없으면 빈 목록이다. 그룹원이 아닐 때도 매퍼가 빈 목록을 주므로 같은 경로다 —
     * 예외를 던지면 「기록이 없는 그룹」과 「남의 그룹」이 화면에서 다르게 보여 존재가 새어 나간다.
     */
    @Test
    @DisplayName("확정된 재판이 없으면 빈 목록이다")
    void trialRecordsAreEmptyWhenNothingConfirmed() {
        when(indictmentMapper.findClosedByGroupId(3L, USER_ID)).thenReturn(List.of());

        assertTrue(service().findGroupTrialRecords(USER_ID, 3L).isEmpty());
    }
}
