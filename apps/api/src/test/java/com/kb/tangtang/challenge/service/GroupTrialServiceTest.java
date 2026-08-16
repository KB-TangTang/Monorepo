package com.kb.tangtang.challenge.service;

import com.kb.tangtang.challenge.domain.GroupIndictmentRow;
import com.kb.tangtang.challenge.domain.TrialTodoRow;
import com.kb.tangtang.challenge.dto.GroupIndictmentDto;
import com.kb.tangtang.challenge.dto.MyTrialDto;
import com.kb.tangtang.challenge.mapper.IndictmentMapper;
import com.kb.tangtang.common.storage.ImageStorage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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

    @Mock private IndictmentMapper indictmentMapper;
    @Mock private ImageStorage imageStorage;

    private GroupTrialService service() {
        return new GroupTrialService(indictmentMapper, imageStorage, DEFENSE_HOURS, VOTE_HOURS);
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
}
