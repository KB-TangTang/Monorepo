package com.kb.tangtang.challenge.service;

import com.kb.tangtang.challenge.domain.GroupTrialDetailRow;
import com.kb.tangtang.challenge.domain.Vote;
import com.kb.tangtang.challenge.mapper.IndictmentMapper;
import com.kb.tangtang.challenge.mapper.VoteMapper;
import com.kb.tangtang.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 소비 재판 투표 (이슈 #171).
 *
 * <p>SQL 의 모양은 {@code ChallengeMapperXmlTest} 가 지킨다. 여기서는 <b>SQL 이 만들 수 없는 것</b>
 * — 검증 순서, 마감 시각 계산, 코멘트 정규화, 저장 행에 들어가는 값 — 만 본다.
 *
 * <p>{@link Clock} 을 주입해 마감을 고정한다. {@code LocalDateTime.now()} 를 쓰면
 * 「마감 30시간」 테스트가 실행 시각에 따라 통과·실패를 오간다.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VoteServiceTest {

    private static final int DEFENSE_HOURS = 6;
    private static final int VOTE_HOURS = 24;
    private static final long USER_ID = 7L;
    private static final long DEFENDANT_ID = 9L;
    private static final long INDICTMENT_ID = 11L;
    private static final long GROUP_ID = 3L;

    /** 기소 10:00 → 변론 마감 16:00 → 투표 마감 다음 날 16:00. 「지금」은 그 사이. */
    private static final LocalDateTime INDICTED_AT = LocalDateTime.of(2026, 8, 18, 10, 0);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 18, 20, 0);
    private static final LocalDateTime VOTE_DEADLINE = LocalDateTime.of(2026, 8, 19, 16, 0);

    @Mock private IndictmentMapper indictmentMapper;
    @Mock private VoteMapper voteMapper;

    private VoteService service() {
        return service(NOW);
    }

    private VoteService service(LocalDateTime now) {
        Clock clock = Clock.fixed(now.atZone(ZoneId.systemDefault()).toInstant(),
                ZoneId.systemDefault());
        return new VoteService(indictmentMapper, voteMapper, DEFENSE_HOURS, VOTE_HOURS, clock);
    }

    /** 검증을 모두 통과하는 기본 상태 — 각 테스트가 필요한 필드만 어긋나게 바꾼다. */
    private GroupTrialDetailRow votableRow() {
        GroupTrialDetailRow row = new GroupTrialDetailRow();
        row.setIndictmentId(INDICTMENT_ID);
        row.setGroupId(GROUP_ID);
        /* 피고는 내가 아닌 사람이다 — 배심원 입장이 기본이다 */
        row.setUserId(DEFENDANT_ID);
        row.setNickname("지판");
        row.setStatus("VOTING");
        row.setCreatedAt(INDICTED_AT);
        row.setLimitAmount(new BigDecimal("50000"));
        row.setCurrentAmount(new BigDecimal("55000"));
        return row;
    }

    private void given(GroupTrialDetailRow row) {
        when(indictmentMapper.findTrialDetail(INDICTMENT_ID, USER_ID)).thenReturn(row);
    }

    private BusinessException callVote(String verdict, String comment) {
        return assertThrows(BusinessException.class,
                () -> service().vote(USER_ID, INDICTMENT_ID, verdict, comment));
    }

    private Vote savedVote() {
        ArgumentCaptor<Vote> captor = ArgumentCaptor.forClass(Vote.class);
        verify(voteMapper).insert(captor.capture());
        return captor.getValue();
    }

    // ── 검증 순서 ──────────────────────────────────────────────────────────

    /**
     * <b>없는 재판과 남의 그룹 재판을 다른 코드로 알려 주면 안 된다.</b> 조회 SQL 이 참여자 조인으로
     * 걸러 NULL 을 주므로, 그룹 밖 사람에게는 존재 자체가 보이지 않는다.
     */
    @Test
    @DisplayName("재판이 없거나 내 그룹이 아니면 TRIAL_NOT_FOUND")
    void rejectsMissingTrial() {
        when(indictmentMapper.findTrialDetail(INDICTMENT_ID, USER_ID)).thenReturn(null);

        assertEquals("TRIAL_NOT_FOUND", callVote("GUILTY", null).getCode());
        verify(voteMapper, never()).insert(any());
    }

    /** 변론을 기다리는 중에는 아직 표를 받지 않는다 — 변론을 못 본 표가 섞인다. */
    @Test
    @DisplayName("변론 대기 중이면 VOTE_NOT_ALLOWED")
    void rejectsBeforeVotingStarts() {
        GroupTrialDetailRow row = votableRow();
        row.setStatus("DEFENSE_WAIT");
        given(row);

        assertEquals("VOTE_NOT_ALLOWED", callVote("GUILTY", null).getCode());
    }

    /** 이미 확정된 재판에 표를 더하면 개표 결과와 표수가 어긋난다. */
    @Test
    @DisplayName("판결이 확정된 뒤에는 VOTE_NOT_ALLOWED")
    void rejectsAfterVerdict() {
        GroupTrialDetailRow row = votableRow();
        row.setStatus("GUILTY");
        given(row);

        assertEquals("VOTE_NOT_ALLOWED", callVote("INNOCENT", null).getCode());
    }

    /**
     * 개표 배치(#172)가 아직 없어 <b>상태는 VOTING 인 채로 마감만 지난</b> 재판이 그대로 남는다.
     * 상태만 보면 마감 며칠 뒤에도 표가 들어온다.
     */
    @Test
    @DisplayName("상태가 VOTING 이어도 마감이 지났으면 VOTE_NOT_ALLOWED")
    void rejectsExpiredDeadlineEvenWhenStatusStillVoting() {
        given(votableRow());

        BusinessException e = assertThrows(BusinessException.class, () ->
                service(VOTE_DEADLINE.plusMinutes(1))
                        .vote(USER_ID, INDICTMENT_ID, "GUILTY", null));

        assertEquals("VOTE_NOT_ALLOWED", e.getCode());
        verify(voteMapper, never()).insert(any());
    }

    /**
     * 마감 시각 정각은 아직 마감이 아니다({@code isBefore} 라 경계가 포함된다).
     *
     * <p>투표 마감은 <b>변론 시간까지 더한 뒤</b> 투표 시간을 더한 값이다 — 투표 시간만 더하면
     * 변론이 끝나는 순간 투표도 끝난 것이 된다.
     */
    @Test
    @DisplayName("마감 정각은 아직 투표할 수 있다")
    void allowsVoteAtExactDeadline() {
        given(votableRow());

        service(VOTE_DEADLINE).vote(USER_ID, INDICTMENT_ID, "GUILTY", null);

        verify(voteMapper).insert(any());
    }

    /** 자기 재판에 표를 던지면 자기 무죄를 자기가 만든다. */
    @Test
    @DisplayName("피고 본인이면 CANNOT_VOTE_OWN_TRIAL")
    void rejectsDefendantVotingOnOwnTrial() {
        GroupTrialDetailRow row = votableRow();
        row.setUserId(USER_ID);
        given(row);

        assertEquals("CANNOT_VOTE_OWN_TRIAL", callVote("INNOCENT", null).getCode());
        verify(voteMapper, never()).insert(any());
    }

    /**
     * 복합 PK {@code (group_id, user_id, indictment_id)} 가 최후 방어지만, 유니크 위반은
     * 500 으로 나가 코드를 줄 수 없다. 화면이 안내 문구를 고를 수 있도록 여기서 먼저 막는다.
     */
    @Test
    @DisplayName("이미 투표했으면 VOTE_ALREADY_EXISTS")
    void rejectsDuplicateVote() {
        given(votableRow());
        when(voteMapper.existsByIndictmentIdAndUserId(INDICTMENT_ID, USER_ID)).thenReturn(true);

        assertEquals("VOTE_ALREADY_EXISTS", callVote("GUILTY", null).getCode());
        verify(voteMapper, never()).insert(any());
    }

    /**
     * {@code ck_vote_verdict} CHECK 도 막지만 그때는 500 이다. 요청 DTO 가 문자열인 이유도
     * 이것이다 — enum 으로 받으면 Jackson 파싱 오류가 우리 코드를 대신한다.
     */
    @Test
    @DisplayName("유죄·무죄가 아니면 INVALID_VERDICT")
    void rejectsUnknownVerdict() {
        given(votableRow());

        assertEquals("INVALID_VERDICT", callVote("ABSTAIN", null).getCode());
        assertEquals("INVALID_VERDICT", callVote("guilty", null).getCode());
        assertEquals("INVALID_VERDICT", callVote(null, null).getCode());
    }

    /**
     * 화면의 {@code maxlength} 는 우회할 수 있다. {@code comment VARCHAR(40)} 이라
     * 넘치면 INSERT 가 잘리거나 죽는다.
     */
    @Test
    @DisplayName("코멘트가 40자를 넘으면 COMMENT_TOO_LONG")
    void rejectsTooLongComment() {
        given(votableRow());

        assertEquals("COMMENT_TOO_LONG", callVote("GUILTY", "가".repeat(41)).getCode());

        /* 40자는 통과한다 — 경계에서 한 글자 어긋나면 화면이 못 내는 값이 된다 */
        service().vote(USER_ID, INDICTMENT_ID, "GUILTY", "가".repeat(40));
        assertEquals("가".repeat(40), savedVote().getComment());
    }

    /** 앞뒤 공백은 길이에 넣지 않는다 — 스페이스 두 개로 40자 제한에 걸리면 이유를 알 수 없다. */
    @Test
    @DisplayName("공백을 다듬은 뒤 길이를 잰다")
    void trimsBeforeMeasuringLength() {
        given(votableRow());

        service().vote(USER_ID, INDICTMENT_ID, "GUILTY", "  " + "가".repeat(40) + "  ");

        assertEquals("가".repeat(40), savedVote().getComment());
    }

    // ── 저장되는 값 ───────────────────────────────────────────────────────

    /** 표에 담기는 값 전부. {@code groupId} 를 빠뜨리면 복합 PK 라 INSERT 가 죽는다. */
    @Test
    @DisplayName("코멘트를 적은 표는 그대로 저장된다")
    void savesVoteWithComment() {
        given(votableRow());

        service().vote(USER_ID, INDICTMENT_ID, "INNOCENT", "사정이 있어 보여요");

        Vote saved = savedVote();
        assertEquals(GROUP_ID, saved.getGroupId().longValue());
        assertEquals(USER_ID, saved.getUserId().longValue());
        assertEquals(INDICTMENT_ID, saved.getIndictmentId().longValue());
        assertEquals("INNOCENT", saved.getVerdict());
        assertEquals("사정이 있어 보여요", saved.getComment());
    }

    /**
     * 코멘트는 선택이다. <b>빈 문자열이 아니라 NULL</b> 로 저장해야 코멘트 목록 조회가
     * 「있는데 비어 있는」 행을 걸러내지 않아도 된다.
     */
    @Test
    @DisplayName("코멘트가 없거나 공백만이면 NULL 로 저장한다")
    void savesNullCommentWhenBlank() {
        given(votableRow());

        service().vote(USER_ID, INDICTMENT_ID, "GUILTY", null);
        service().vote(USER_ID, INDICTMENT_ID, "GUILTY", "   ");

        ArgumentCaptor<Vote> captor = ArgumentCaptor.forClass(Vote.class);
        verify(voteMapper, times(2)).insert(captor.capture());
        captor.getAllValues().forEach(vote -> assertNull(vote.getComment()));
    }

    /**
     * <b>개표하지 않는다.</b> 마지막 한 표가 들어와도 상태는 VOTING 그대로다 —
     * 표가 다 모였다고 즉시 확정하면 마감 전에 결과가 나온다. 확정은 이슈 #172 가 한다.
     */
    @Test
    @DisplayName("투표는 기소 상태를 건드리지 않는다")
    void voteNeverChangesIndictmentStatus() {
        given(votableRow());

        service().vote(USER_ID, INDICTMENT_ID, "GUILTY", null);

        verify(indictmentMapper, never()).moveToVoting(INDICTMENT_ID);
        verify(indictmentMapper, never()).confirmConfession(INDICTMENT_ID);
    }
}
