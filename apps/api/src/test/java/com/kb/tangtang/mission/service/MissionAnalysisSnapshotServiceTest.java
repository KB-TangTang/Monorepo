package com.kb.tangtang.mission.service;

import com.kb.tangtang.mission.domain.MissionAnalysisSnapshot;
import com.kb.tangtang.mission.dto.MissionAnalysisSnapshotDto;
import com.kb.tangtang.mission.dto.MissionCategoryAnalysisDto;
import com.kb.tangtang.mission.dto.MissionCategoryRankDto;
import com.kb.tangtang.mission.mapper.MissionAnalysisSnapshotMapper;
import com.kb.tangtang.mission.mapper.MissionCategoryAnalysisMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MissionAnalysisSnapshotServiceTest {

    private static final long USER_ID = 1L;
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 11);

    private static class FakeSnapshotMapper implements MissionAnalysisSnapshotMapper {
        List<MissionAnalysisSnapshot> pendingSnapshots = List.of();
        final List<MissionAnalysisSnapshot> insertedSnapshots = new ArrayList<>();
        LocalDateTime qualifiedAt;
        LocalDateTime markedQualifiedAt;

        @Override
        public List<MissionAnalysisSnapshot> findPendingSnapshots(long userId) {
            return pendingSnapshots;
        }

        @Override
        public MissionAnalysisSnapshot findNextPendingSnapshotForUpdate(long userId) {
            return pendingSnapshots.isEmpty() ? null : pendingSnapshots.get(0);
        }

        @Override
        public LocalDateTime findQualifiedAt(long userId) {
            return qualifiedAt;
        }

        @Override
        public int markQualified(long userId, LocalDateTime qualifiedAt) {
            markedQualifiedAt = qualifiedAt;
            this.qualifiedAt = qualifiedAt;
            return 1;
        }

        @Override
        public int insertSnapshots(List<MissionAnalysisSnapshot> snapshots) {
            insertedSnapshots.addAll(snapshots);
            return snapshots.size();
        }

        @Override
        public int markAssigned(long snapshotId, LocalDate assignedDate) {
            return 1;
        }
    }

    private static class StubCategoryAnalysisService extends MissionCategoryAnalysisService {
        MissionCategoryAnalysisDto result;
        int callCount;
        int qualifiedCallCount;

        StubCategoryAnalysisService() {
            super((MissionCategoryAnalysisMapper) null);
        }

        @Override
        public MissionCategoryAnalysisDto getCategoryAnalysis(long userId) {
            callCount++;
            return result;
        }

        @Override
        public MissionCategoryAnalysisDto getCategoryAnalysisForQualifiedUser(long userId) {
            qualifiedCallCount++;
            return result;
        }
    }

    private MissionAnalysisSnapshotService service(FakeSnapshotMapper mapper,
                                                    StubCategoryAnalysisService analysisService) {
        ZoneId zoneId = ZoneId.of("Asia/Seoul");
        Clock clock = Clock.fixed(TODAY.atStartOfDay(zoneId).toInstant(), zoneId);
        return new MissionAnalysisSnapshotService(mapper, analysisService, clock);
    }

    @Test
    @DisplayName("최신 주기에 미소진 스냅샷이 있으면 재분석하거나 저장하지 않는다")
    void reusesPendingSnapshots() {
        FakeSnapshotMapper mapper = new FakeSnapshotMapper();
        mapper.pendingSnapshots = List.of(
                snapshot(2, "취미", "227436", "12.50"),
                snapshot(3, "온라인쇼핑", "148796", "8.18"));
        StubCategoryAnalysisService analysisService = new StubCategoryAnalysisService();

        MissionAnalysisSnapshotDto result = service(mapper, analysisService)
                .getOrCreateSnapshot(USER_ID);

        assertTrue(result.isAvailable());
        assertEquals(TODAY, result.getCycleStartDate());
        assertEquals(2, result.getItems().size());
        assertEquals(2, result.getItems().get(0).getCategoryRank());
        assertEquals(0, analysisService.callCount);
        assertTrue(mapper.insertedSnapshots.isEmpty());
    }

    @Test
    @DisplayName("미소진 스냅샷이 없으면 상위 카테고리를 새 주기로 일괄 저장한다")
    void createsSnapshotsFromCategoryAnalysis() {
        FakeSnapshotMapper mapper = new FakeSnapshotMapper();
        StubCategoryAnalysisService analysisService = new StubCategoryAnalysisService();
        analysisService.result = eligibleAnalysis(List.of(
                category(1, 18L, "쇼핑", "패션", "325429", 4, "17.89"),
                category(2, 40L, "문화/여가", "취미", "227436", 5, "12.50"),
                category(3, 17L, "쇼핑", "온라인쇼핑", "148796", 3, "8.18")));

        MissionAnalysisSnapshotDto result = service(mapper, analysisService)
                .getOrCreateSnapshot(USER_ID);

        assertTrue(result.isAvailable());
        assertEquals(TODAY, result.getCycleStartDate());
        assertEquals(3, mapper.insertedSnapshots.size());
        assertEquals(USER_ID, mapper.insertedSnapshots.get(0).getUserId());
        assertEquals(18L, mapper.insertedSnapshots.get(0).getCategoryId());
        assertEquals(1, mapper.insertedSnapshots.get(0).getCategoryRank());
        assertEquals(new BigDecimal("325429"), mapper.insertedSnapshots.get(0).getSpendingAmount());
        assertEquals(new BigDecimal("17.89"), mapper.insertedSnapshots.get(0).getSpendingRatio());
        assertEquals(4, mapper.insertedSnapshots.get(0).getTransactionCount());
        assertEquals(1, analysisService.callCount);
        assertEquals(0, analysisService.qualifiedCallCount);
        assertEquals(TODAY.atStartOfDay(), mapper.markedQualifiedAt);
    }

    @Test
    @DisplayName("자격 획득 시각이 있으면 최초 50건 조건 없이 재분석한다")
    void qualifiedUserUsesAnalysisWithoutInitialRequirement() {
        FakeSnapshotMapper mapper = new FakeSnapshotMapper();
        mapper.qualifiedAt = LocalDateTime.of(2026, 7, 1, 10, 0);
        StubCategoryAnalysisService analysisService = new StubCategoryAnalysisService();
        analysisService.result = eligibleAnalysis(List.of(
                category(1, 18L, "쇼핑", "패션", "120000", 12, "40.00")));

        MissionAnalysisSnapshotDto result = service(mapper, analysisService)
                .getOrCreateSnapshot(USER_ID);

        assertTrue(result.isAvailable());
        assertEquals(0, analysisService.callCount);
        assertEquals(1, analysisService.qualifiedCallCount);
        assertEquals(1, mapper.insertedSnapshots.size());
        assertNull(mapper.markedQualifiedAt);
    }

    @Test
    @DisplayName("상대형 조건을 충족하지 못하면 스냅샷을 저장하지 않는다")
    void skipsInsertWhenRelativeMissionIsIneligible() {
        FakeSnapshotMapper mapper = new FakeSnapshotMapper();
        StubCategoryAnalysisService analysisService = new StubCategoryAnalysisService();
        analysisService.result = MissionCategoryAnalysisDto.builder()
                .relativeEligible(false)
                .topCategories(List.of())
                .build();

        MissionAnalysisSnapshotDto result = service(mapper, analysisService)
                .getOrCreateSnapshot(USER_ID);

        assertFalse(result.isAvailable());
        assertTrue(result.getItems().isEmpty());
        assertTrue(mapper.insertedSnapshots.isEmpty());
    }

    @Test
    @DisplayName("분석된 카테고리가 두 개면 존재하는 두 행만 저장한다")
    void insertsOnlyExistingCategories() {
        FakeSnapshotMapper mapper = new FakeSnapshotMapper();
        StubCategoryAnalysisService analysisService = new StubCategoryAnalysisService();
        analysisService.result = eligibleAnalysis(List.of(
                category(1, 18L, "쇼핑", "패션", "325429", 4, "17.89"),
                category(2, 40L, "문화/여가", "취미", "227436", 5, "12.50")));

        MissionAnalysisSnapshotDto result = service(mapper, analysisService)
                .getOrCreateSnapshot(USER_ID);

        assertEquals(2, mapper.insertedSnapshots.size());
        assertEquals(2, result.getItems().size());
    }

    private MissionCategoryAnalysisDto eligibleAnalysis(List<MissionCategoryRankDto> categories) {
        return MissionCategoryAnalysisDto.builder()
                .relativeEligible(true)
                .topCategories(categories)
                .build();
    }

    private MissionCategoryRankDto category(int rank, long categoryId, String parentCategoryName,
                                            String categoryName, String totalAmount, int transactionCount,
                                            String spendingRatio) {
        return MissionCategoryRankDto.builder()
                .rank(rank)
                .categoryId(categoryId)
                .parentCategoryName(parentCategoryName)
                .categoryName(categoryName)
                .totalAmount(new BigDecimal(totalAmount))
                .transactionCount(transactionCount)
                .spendingRatio(new BigDecimal(spendingRatio))
                .build();
    }

    private MissionAnalysisSnapshot snapshot(int rank, String categoryName,
                                             String spendingAmount, String spendingRatio) {
        return MissionAnalysisSnapshot.builder()
                .userId(USER_ID)
                .cycleStartDate(TODAY)
                .categoryId((long) rank)
                .parentCategoryName("테스트")
                .categoryName(categoryName)
                .categoryRank(rank)
                .spendingAmount(new BigDecimal(spendingAmount))
                .spendingRatio(new BigDecimal(spendingRatio))
                .transactionCount(rank + 2)
                .build();
    }
}
