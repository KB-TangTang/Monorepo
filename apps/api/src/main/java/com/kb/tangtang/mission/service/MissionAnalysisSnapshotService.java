package com.kb.tangtang.mission.service;

import com.kb.tangtang.mission.domain.MissionAnalysisSnapshot;
import com.kb.tangtang.mission.dto.MissionAnalysisSnapshotDto;
import com.kb.tangtang.mission.dto.MissionAnalysisSnapshotItemDto;
import com.kb.tangtang.mission.dto.MissionCategoryAnalysisDto;
import com.kb.tangtang.mission.dto.MissionCategoryRankDto;
import com.kb.tangtang.mission.mapper.MissionAnalysisSnapshotMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class MissionAnalysisSnapshotService {

    private final MissionAnalysisSnapshotMapper missionAnalysisSnapshotMapper;
    private final MissionCategoryAnalysisService missionCategoryAnalysisService;
    private final Clock clock;

    @Autowired
    public MissionAnalysisSnapshotService(MissionAnalysisSnapshotMapper missionAnalysisSnapshotMapper,
                                          MissionCategoryAnalysisService missionCategoryAnalysisService) {
        this(missionAnalysisSnapshotMapper, missionCategoryAnalysisService, Clock.systemDefaultZone());
    }

    MissionAnalysisSnapshotService(MissionAnalysisSnapshotMapper missionAnalysisSnapshotMapper,
                                   MissionCategoryAnalysisService missionCategoryAnalysisService,
                                   Clock clock) {
        this.missionAnalysisSnapshotMapper = missionAnalysisSnapshotMapper;
        this.missionCategoryAnalysisService = missionCategoryAnalysisService;
        this.clock = clock;
    }

    @Transactional
    public MissionAnalysisSnapshotDto getOrCreateSnapshot(long userId) {
        List<MissionAnalysisSnapshot> pendingSnapshots =
                missionAnalysisSnapshotMapper.findPendingSnapshots(userId);
        if (!pendingSnapshots.isEmpty()) {
            return toDto(pendingSnapshots);
        }

        boolean alreadyQualified = missionAnalysisSnapshotMapper.findQualifiedAt(userId) != null;
        if (!alreadyQualified && missionCategoryAnalysisService.hasInitialQualification(userId)) {
            missionAnalysisSnapshotMapper.markQualified(userId, LocalDateTime.now(clock));
            alreadyQualified = true;
        }
        MissionCategoryAnalysisDto analysis = alreadyQualified
                ? missionCategoryAnalysisService.getCategoryAnalysisForQualifiedUser(userId)
                : missionCategoryAnalysisService.getCategoryAnalysis(userId);
        if (analysis.getTopCategories().isEmpty()) {
            return MissionAnalysisSnapshotDto.empty();
        }

        LocalDate cycleStartDate = LocalDate.now(clock);
        List<MissionAnalysisSnapshot> newSnapshots = analysis.getTopCategories().stream()
                .map(category -> toSnapshot(userId, cycleStartDate, category))
                .toList();

        missionAnalysisSnapshotMapper.insertSnapshots(newSnapshots);
        return toDto(newSnapshots);
    }

    @Transactional
    public MissionCategoryAnalysisDto getCurrentRotationAnalysis(long userId) {
        List<MissionAnalysisSnapshot> snapshots =
                missionAnalysisSnapshotMapper.findLatestCycleSnapshots(userId);
        if (snapshots.isEmpty()) {
            getOrCreateSnapshot(userId);
            snapshots = missionAnalysisSnapshotMapper.findLatestCycleSnapshots(userId);
        }
        if (snapshots.isEmpty()) {
            LocalDate today = LocalDate.now(clock);
            return MissionCategoryAnalysisDto.builder()
                    .analysisStartDate(today.minusDays(28))
                    .analysisEndDate(today.minusDays(1))
                    .transactionCount(0)
                    .cumulativeTransactionCount(
                            missionCategoryAnalysisService.getCumulativeTransactionCount(userId))
                    .requiredCumulativeTransactionCount(
                            missionCategoryAnalysisService.getRequiredCumulativeTransactionCount())
                    .topCategories(List.of())
                    .build();
        }

        LocalDate cycleStartDate = snapshots.get(0).getCycleStartDate();
        List<MissionCategoryRankDto> categories = snapshots.stream()
                .map(snapshot -> MissionCategoryRankDto.builder()
                        .rank(snapshot.getCategoryRank())
                        .categoryId(snapshot.getCategoryId())
                        .parentCategoryName(snapshot.getParentCategoryName())
                        .categoryName(snapshot.getCategoryName())
                        .totalAmount(snapshot.getSpendingAmount())
                        .transactionCount(snapshot.getTransactionCount())
                        .spendingRatio(snapshot.getSpendingRatio())
                        .rotationAssignDate(snapshot.getAssignedDate())
                        .rotationResult(snapshot.getRotationResult())
                        .missionRound(snapshot.getMissionRound())
                        .build())
                .toList();
        int transactionCount = snapshots.stream()
                .mapToInt(MissionAnalysisSnapshot::getTransactionCount)
                .sum();

        return MissionCategoryAnalysisDto.builder()
                .analysisStartDate(cycleStartDate.minusDays(28))
                .analysisEndDate(cycleStartDate.minusDays(1))
                .transactionCount(transactionCount)
                .cumulativeTransactionCount(
                        missionCategoryAnalysisService.getCumulativeTransactionCount(userId))
                .requiredCumulativeTransactionCount(
                        missionCategoryAnalysisService.getRequiredCumulativeTransactionCount())
                .topCategories(categories)
                .build();
    }

    private MissionAnalysisSnapshot toSnapshot(long userId,
                                               LocalDate cycleStartDate,
                                               MissionCategoryRankDto category) {
        return MissionAnalysisSnapshot.builder()
                .userId(userId)
                .cycleStartDate(cycleStartDate)
                .categoryId(category.getCategoryId())
                .parentCategoryName(category.getParentCategoryName())
                .categoryName(category.getCategoryName())
                .categoryRank(category.getRank())
                .spendingAmount(category.getTotalAmount())
                .spendingRatio(category.getSpendingRatio())
                .transactionCount(category.getTransactionCount())
                .build();
    }

    private MissionAnalysisSnapshotDto toDto(List<MissionAnalysisSnapshot> snapshots) {
        if (snapshots.isEmpty()) {
            return MissionAnalysisSnapshotDto.empty();
        }

        List<MissionAnalysisSnapshotItemDto> items = snapshots.stream()
                .map(snapshot -> MissionAnalysisSnapshotItemDto.builder()
                        .categoryId(snapshot.getCategoryId())
                        .parentCategoryName(snapshot.getParentCategoryName())
                        .categoryName(snapshot.getCategoryName())
                        .categoryRank(snapshot.getCategoryRank())
                        .spendingAmount(snapshot.getSpendingAmount())
                        .spendingRatio(snapshot.getSpendingRatio())
                        .transactionCount(snapshot.getTransactionCount())
                        .assignedDate(snapshot.getAssignedDate())
                        .build())
                .toList();

        return MissionAnalysisSnapshotDto.builder()
                .available(true)
                .cycleStartDate(snapshots.get(0).getCycleStartDate())
                .items(items)
                .build();
    }
}
