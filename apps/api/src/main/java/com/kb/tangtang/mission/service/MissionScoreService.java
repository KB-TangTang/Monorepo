package com.kb.tangtang.mission.service;

import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.common.storage.ImageStorage;
import com.kb.tangtang.mission.domain.MissionRankingRow;
import com.kb.tangtang.mission.dto.MissionMonthlyRankingDto;
import com.kb.tangtang.mission.dto.MissionMonthlyScoreDto;
import com.kb.tangtang.mission.dto.MissionMyRankingDto;
import com.kb.tangtang.mission.dto.MissionRankingEntryDto;
import com.kb.tangtang.mission.dto.MissionRankingMonthsDto;
import com.kb.tangtang.mission.mapper.MissionScoreMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MissionScoreService {

    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");
    private static final int TOP_RANKING_LIMIT = 10;

    private final MissionScoreMapper missionScoreMapper;
    private final ImageStorage imageStorage;
    private final Clock clock;

    @Autowired
    public MissionScoreService(MissionScoreMapper missionScoreMapper, ImageStorage imageStorage) {
        this(missionScoreMapper, imageStorage, Clock.system(SEOUL_ZONE));
    }

    public MissionScoreService(MissionScoreMapper missionScoreMapper) {
        this(missionScoreMapper, null, Clock.system(SEOUL_ZONE));
    }

    MissionScoreService(MissionScoreMapper missionScoreMapper, Clock clock) {
        this(missionScoreMapper, null, clock);
    }

    MissionScoreService(MissionScoreMapper missionScoreMapper, ImageStorage imageStorage, Clock clock) {
        this.missionScoreMapper = missionScoreMapper;
        this.imageStorage = imageStorage;
        this.clock = clock;
    }

    @Transactional
    public void recalculate(long userId, YearMonth yearMonth) {
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();
        int totalScore = missionScoreMapper.calculateMonthlyScore(userId, startDate, endDate);
        missionScoreMapper.upsertMonthlyScore(userId, yearMonth.toString(), totalScore);
    }

    @Transactional(readOnly = true)
    public MissionMonthlyScoreDto getCurrentScore(long userId) {
        YearMonth yearMonth = YearMonth.now(clock.withZone(SEOUL_ZONE));
        Integer totalScore = missionScoreMapper.findMonthlyScore(userId, yearMonth.toString());
        return new MissionMonthlyScoreDto(yearMonth.toString(), totalScore == null ? 0 : totalScore);
    }

    @Transactional(readOnly = true)
    public MissionMonthlyRankingDto getMonthlyRanking(long userId, String rawYearMonth) {
        String yearMonth = parseYearMonth(rawYearMonth).toString();
        MissionRankingRow myRankingRow = missionScoreMapper.findUserRanking(userId, yearMonth);
        if (myRankingRow == null) {
            return null;
        }

        int totalUsers = missionScoreMapper.countRankingUsers(yearMonth);
        List<MissionRankingEntryDto> topRankings = missionScoreMapper
                .findTopRankings(yearMonth, TOP_RANKING_LIMIT)
                .stream()
                .map(this::toRankingEntry)
                .collect(Collectors.toList());

        return new MissionMonthlyRankingDto(
                yearMonth,
                totalUsers,
                topRankings,
                toMyRanking(myRankingRow, totalUsers)
        );
    }

    @Transactional(readOnly = true)
    public MissionRankingMonthsDto getRankingMonths() {
        return new MissionRankingMonthsDto(missionScoreMapper.findRankingMonths());
    }

    private YearMonth parseYearMonth(String rawYearMonth) {
        if (rawYearMonth == null || rawYearMonth.trim().isEmpty()) {
            return YearMonth.now(clock.withZone(SEOUL_ZONE));
        }
        try {
            return YearMonth.parse(rawYearMonth);
        } catch (DateTimeParseException ex) {
            throw new BusinessException("INVALID_YEAR_MONTH", "yearMonth는 YYYY-MM 형식이어야 합니다.");
        }
    }

    private MissionRankingEntryDto toRankingEntry(MissionRankingRow row) {
        return new MissionRankingEntryDto(
                row.getRank(),
                row.getUserId(),
                row.getNickname(),
                resolveProfileImageUrl(row.getProfileImageKey()),
                row.getTotalScore()
        );
    }

    private MissionMyRankingDto toMyRanking(MissionRankingRow row, int totalUsers) {
        if (row == null) {
            return null;
        }
        int topPercent = totalUsers == 0
                ? 0
                : (int) Math.ceil(row.getRank() * 100.0 / totalUsers);
        return new MissionMyRankingDto(
                row.getRank(),
                row.getNickname(),
                resolveProfileImageUrl(row.getProfileImageKey()),
                row.getTotalScore(),
                topPercent
        );
    }

    private String resolveProfileImageUrl(String profileImageKey) {
        return imageStorage == null ? null : imageStorage.urlOf(profileImageKey);
    }
}
