package com.kb.tangtang.report.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kb.tangtang.challenge.domain.GroupChallengeEvents;
import com.kb.tangtang.report.domain.ChallengeMonthlyDifficultyPolicy;
import com.kb.tangtang.report.domain.ChallengeMonthlyMissionRow;
import com.kb.tangtang.report.domain.ChallengeMonthlyReportSnapshot;
import com.kb.tangtang.report.dto.GroupRecordDto;
import com.kb.tangtang.report.mapper.ChallengeReportMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 월말 종료 그룹 챌린지의 <b>재판 진행 여부별</b> 결과 저장 시나리오 (이슈 #267).
 *
 * <p>개별 단위 테스트는 이미 각 클래스에 있다. 여기서 고정하는 것은 <b>클래스를 가로지르는 이음매</b>다 —
 * 「1일 배치에서는 재판 중이라 그룹 전적이 비어 저장됐다가, 나중에 확정되면 <b>개인 성과를 건드리지 않고</b>
 * 그룹 전적만 채워지는가」. 이슈 본문이 지적한 「개인 챌린지 및 그룹 챌린지 결과 저장 흐름이 분리되어
 * 있지 않아」가 바로 이 지점이라, 매퍼만 스텁하고 서비스·리스너는 <b>실제 객체를 이어붙여</b> 검증한다.
 *
 * <p>재판 진행 여부는 매퍼가 SQL 로 가른다({@code findGroupRecord} 의 {@code g.status = 'CLOSED'}).
 * 진행 중이면 집계에 잡히지 않아 {@code participatingGroups = 0} 인 결과가 돌아온다.
 */
@ExtendWith(MockitoExtension.class)
class MonthEndGroupRecordScenarioTest {

    private static final long USER_ID = 7L;
    private static final long GROUP_ID = 100L;
    private static final YearMonth TARGET_MONTH = YearMonth.of(2026, 8);
    private static final LocalDate MONTH_START = LocalDate.of(2026, 8, 1);
    private static final LocalDate MONTH_END = LocalDate.of(2026, 8, 31);
    /** 익월 1일 00:20 월 확정 배치 시각. */
    private static final LocalDateTime FINALIZED_AT = LocalDateTime.of(2026, 9, 1, 0, 20);

    @Mock private ChallengeReportMapper mapper;

    private ChallengeMonthlyReportSnapshotService snapshotService;
    private GroupChallengeClosedListener listener;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        snapshotService = new ChallengeMonthlyReportSnapshotService(mapper, objectMapper);
        listener = new GroupChallengeClosedListener(snapshotService);
    }

    /**
     * A. 말일 종료 + 재판 진행 중.
     *
     * <p>{@code findGroupRecord} 가 {@code CLOSED} 만 세므로 진행 중인 그룹은 잡히지 않는다.
     * <b>개인 성과는 정상 저장되고 그룹 전적만 {@code null}</b> 이어야 한다 — 여기서 개인 스냅샷까지
     * 밀리면 재판이 늦게 끝난 사람만 판결문이 통째로 비게 된다.
     */
    @Test
    @DisplayName("A. 재판 진행 중이면 개인 성과만 저장되고 그룹 전적은 null 이다")
    void storesPersonalSnapshotWithNullGroupRecordWhileTrialIsOngoing() {
        givenPersonalMissions();
        when(mapper.findGroupRecord(USER_ID, MONTH_START, MONTH_END)).thenReturn(emptyRecord());

        snapshotService.finalizeUserMonth(USER_ID, TARGET_MONTH, FINALIZED_AT);

        ChallengeMonthlyReportSnapshot snapshot = capturedSnapshot();
        assertNull(snapshot.getGroupRecordJson(), "재판 진행 중에는 그룹 전적이 비어야 한다");
        assertEquals(1, snapshot.getTotalDays(), "개인 성과는 그대로 저장돼야 한다");
        assertNotNull(snapshot.getWeeklyResultsJson());
    }

    /**
     * B. 말일 종료 + 재판 전부 확정.
     *
     * <p>1일 배치 한 번으로 개인 성과와 그룹 전적이 <b>함께</b> 저장된다. 뒤의 보강 경로가 필요 없는
     * 정상 경로다.
     */
    @Test
    @DisplayName("B. 재판이 전부 확정됐으면 1일 배치에서 개인 성과와 함께 저장된다")
    void storesGroupRecordTogetherWithPersonalSnapshotWhenAllTrialsAreClosed() throws Exception {
        givenPersonalMissions();
        when(mapper.findGroupRecord(USER_ID, MONTH_START, MONTH_END)).thenReturn(closedRecord());

        snapshotService.finalizeUserMonth(USER_ID, TARGET_MONTH, FINALIZED_AT);

        JsonNode groupRecord = objectMapper.readTree(capturedSnapshot().getGroupRecordJson());
        assertEquals(1, groupRecord.get("participatingGroups").asInt());
        assertEquals(1, groupRecord.get("survivedCount").asInt());
        assertEquals(2, groupRecord.get("acquittedCount").asInt());
    }

    /**
     * C. A 였다가 나중에 확정 — <b>이 이슈의 본체</b>.
     *
     * <p>확정 이벤트를 받아 그룹 전적만 채운다. {@code upsertMonthlyReport} 를 다시 부르면 개인 성과가
     * 재계산돼 덮이므로, 반드시 {@code updateMonthlyGroupRecord} 만 나가야 한다.
     */
    @Test
    @DisplayName("C. 나중에 확정되면 개인 스냅샷을 건드리지 않고 그룹 전적만 채운다")
    void fillsOnlyGroupRecordWhenTrialIsClosedLater() {
        givenPersonalMissions();
        when(mapper.findGroupRecord(USER_ID, MONTH_START, MONTH_END))
                .thenReturn(emptyRecord())      // 1일 배치 시점 — 아직 재판 중
                .thenReturn(closedRecord());    // 확정 이벤트 시점

        snapshotService.finalizeUserMonth(USER_ID, TARGET_MONTH, FINALIZED_AT);
        assertNull(capturedSnapshot().getGroupRecordJson());

        listener.onGroupChallengeClosed(
                new GroupChallengeEvents.GroupChallengeClosed(GROUP_ID, MONTH_END, List.of(USER_ID)));

        verify(mapper).updateMonthlyGroupRecord(eq(USER_ID), eq("2026-08"),
                org.mockito.ArgumentMatchers.contains("\"participatingGroups\":1"));
        // 개인 스냅샷 저장도 재계산도 1일 배치의 1회뿐 — 확정 이벤트는 여기에 손대지 않는다
        verify(mapper, times(1)).upsertMonthlyReport(any());
        verify(mapper, times(1)).findFinalizedMissionRows(anyLong(), eq(MONTH_START), eq(MONTH_END));
    }

    /**
     * D. C 를 여러 번 — 확정 이벤트와 보강 cron 이 겹쳐 도는 것이 정상이다.
     *
     * <p>{@code updateMonthlyGroupRecord} 는 {@code user_id + year_month} WHERE 의 단순 UPDATE 라
     * 몇 번 돌아도 같은 값이 된다. 인자가 매번 같은지를 못박아 멱등을 고정한다.
     */
    @Test
    @DisplayName("D. 확정 이벤트와 보강 cron 이 겹쳐 돌아도 결과가 같다 (멱등)")
    void isIdempotentWhenEventAndRecoveryCronOverlap() {
        when(mapper.findGroupRecord(USER_ID, MONTH_START, MONTH_END)).thenReturn(closedRecord());
        when(mapper.findMonthEndClosedGroupReportUserIds(MONTH_END, "2026-08"))
                .thenReturn(List.of(USER_ID));
        // 보강 cron 은 대상 월을 인자로 받으므로 Clock 은 이 경로에 관여하지 않는다
        ChallengeMonthlyReportBatchService batchService = new ChallengeMonthlyReportBatchService(
                mapper, snapshotService, Clock.systemUTC());

        listener.onGroupChallengeClosed(
                new GroupChallengeEvents.GroupChallengeClosed(GROUP_ID, MONTH_END, List.of(USER_ID)));
        batchService.refreshMonthEndGroupRecords(TARGET_MONTH);

        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
        verify(mapper, times(2)).updateMonthlyGroupRecord(
                eq(USER_ID), eq("2026-08"), jsonCaptor.capture());
        List<String> stored = jsonCaptor.getAllValues();
        assertEquals(stored.get(0), stored.get(1), "두 경로가 같은 값을 써야 한다");
        verify(mapper, never()).upsertMonthlyReport(any());
    }

    /**
     * 집계가 다시 비면 기존 값을 {@code null} 로 되돌린다.
     *
     * <p>「비었으면 건너뛴다」로 보이기 쉬운 자리라 명시한다. 그룹이 삭제되거나 전적 조건이 깨졌을 때
     * 낡은 JSON 이 화면에 남는 편보다 비우는 쪽이 맞다는 판단이다.
     */
    @Test
    @DisplayName("보강 시 집계가 비면 저장된 그룹 전적을 null 로 되돌린다")
    void clearsStoredGroupRecordWhenAggregationBecomesEmpty() {
        when(mapper.findGroupRecord(USER_ID, MONTH_START, MONTH_END)).thenReturn(emptyRecord());

        snapshotService.refreshUserMonthGroupRecord(USER_ID, TARGET_MONTH);

        verify(mapper).updateMonthlyGroupRecord(USER_ID, "2026-08", null);
    }

    private void givenPersonalMissions() {
        ChallengeMonthlyMissionRow row = new ChallengeMonthlyMissionRow();
        row.setAssignDate(LocalDate.of(2026, 8, 31));
        row.setResult("SUCCESS");
        row.setMissionType("ABSOLUTE");
        row.setDifficultyName("EASY");
        row.setDifficultyScore(10);
        row.setCategoryId(1L);
        row.setCategoryName("카페");
        row.setBaseAmount(BigDecimal.valueOf(10000));
        row.setActualAmount(BigDecimal.valueOf(7000));

        ChallengeMonthlyDifficultyPolicy policy = new ChallengeMonthlyDifficultyPolicy();
        policy.setDifficultyName("EASY");

        when(mapper.findFinalizedMissionRows(USER_ID, MONTH_START, MONTH_END)).thenReturn(List.of(row));
        when(mapper.findDifficultyPolicies()).thenReturn(List.of(policy));
    }

    /** 재판 진행 중 — {@code g.status = 'CLOSED'} 에 걸려 한 건도 잡히지 않은 결과. */
    private GroupRecordDto emptyRecord() {
        return GroupRecordDto.builder().participatingGroups(0).build();
    }

    /** 기소 3건 중 무죄 2 · 유죄 1 로 확정되고 생존한 그룹. */
    private GroupRecordDto closedRecord() {
        return GroupRecordDto.builder()
                .participatingGroups(1)
                .survivedCount(1)
                .eliminatedCount(0)
                .indictedCount(3)
                .acquittedCount(2)
                .convictedCount(1)
                .build();
    }

    private ChallengeMonthlyReportSnapshot capturedSnapshot() {
        ArgumentCaptor<ChallengeMonthlyReportSnapshot> captor =
                ArgumentCaptor.forClass(ChallengeMonthlyReportSnapshot.class);
        verify(mapper).upsertMonthlyReport(captor.capture());
        return captor.getValue();
    }
}
