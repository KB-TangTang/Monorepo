package com.kb.tangtang.mission.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.mission.domain.MissionCertificateStatsRow;
import com.kb.tangtang.mission.domain.MissionCertificateTitle;
import com.kb.tangtang.mission.domain.MissionCertificateTitleInput;
import com.kb.tangtang.mission.domain.MissionRankingRow;
import com.kb.tangtang.mission.dto.MissionCertificateTitlesDto;
import com.kb.tangtang.mission.mapper.MissionCertificateTitleMapper;
import com.kb.tangtang.mission.mapper.MissionScoreMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.time.Clock;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.HexFormat;
import java.util.List;

@Service
public class MissionCertificateTitleService {

    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String PROVIDER_OPENAI = "OPENAI";
    private static final String PROMPT_VERSION = "mission-certificate-title-v1";

    private final MissionCertificateTitleMapper titleMapper;
    private final MissionScoreMapper scoreMapper;
    private final MissionCertificateTitleProvider provider;
    private final MissionCertificateTitleStateService stateService;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final String model;

    @Autowired
    public MissionCertificateTitleService(MissionCertificateTitleMapper titleMapper,
                                          MissionScoreMapper scoreMapper,
                                          MissionCertificateTitleProvider provider,
                                          MissionCertificateTitleStateService stateService,
                                          ObjectMapper objectMapper,
                                          @Value("${openai.model:gpt-5-nano}") String model) {
        this(titleMapper, scoreMapper, provider, stateService, objectMapper,
                Clock.system(SEOUL_ZONE), model);
    }

    MissionCertificateTitleService(MissionCertificateTitleMapper titleMapper,
                                   MissionScoreMapper scoreMapper,
                                   MissionCertificateTitleProvider provider,
                                   MissionCertificateTitleStateService stateService,
                                   ObjectMapper objectMapper,
                                   Clock clock,
                                   String model) {
        this.titleMapper = titleMapper;
        this.scoreMapper = scoreMapper;
        this.provider = provider;
        this.stateService = stateService;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.model = model;
    }

    @Transactional(readOnly = true)
    public MissionCertificateTitlesDto getTitles(long userId, String rawYearMonth) {
        YearMonth yearMonth = validateFinalizedMonth(rawYearMonth);
        MissionRankingRow ranking = scoreMapper.findUserRanking(userId, yearMonth.toString());
        if (ranking == null) {
            throw new BusinessException("CERTIFICATE_NOT_FOUND", "해당 월의 인증서 데이터를 찾을 수 없습니다.");
        }

        MissionCertificateTitle title = titleMapper.findByUserIdAndYearMonth(userId, yearMonth.toString());
        if (title != null && STATUS_COMPLETED.equals(title.getStatus())) {
            return new MissionCertificateTitlesDto(yearMonth.toString(),
                    List.of(title.getTitle1(), title.getTitle2(), title.getTitle3()), title.getProvider());
        }
        return new MissionCertificateTitlesDto(yearMonth.toString(), fallbackTitles(ranking, yearMonth), "FALLBACK");
    }

    public void generate(long userId, YearMonth yearMonth) {
        String period = yearMonth.toString();
        titleMapper.insertIfAbsent(userId, period);
        MissionCertificateTitle stored = titleMapper.findByUserIdAndYearMonth(userId, period);
        if (stored != null && STATUS_COMPLETED.equals(stored.getStatus())) {
            return;
        }

        MissionCertificateTitleInput input = buildInput(userId, yearMonth);
        int claimed = stateService.claim(userId, period, PROVIDER_OPENAI, model, PROMPT_VERSION, inputHash(input));
        if (claimed == 0) {
            return;
        }
        try {
            List<String> titles = provider.generate(input);
            if (stateService.complete(userId, period, titles) != 1) {
                throw new BusinessException("AI_PROVIDER_UNAVAILABLE", "AI 명예 타이틀을 저장하지 못했어요.");
            }
        } catch (BusinessException exception) {
            stateService.fail(userId, period, exception.getCode());
        } catch (RuntimeException exception) {
            stateService.fail(userId, period, "AI_PROVIDER_UNAVAILABLE");
        }
    }

    private MissionCertificateTitleInput buildInput(long userId, YearMonth yearMonth) {
        String period = yearMonth.toString();
        MissionRankingRow ranking = scoreMapper.findUserRanking(userId, period);
        if (ranking == null) {
            throw new BusinessException("CERTIFICATE_NOT_FOUND", "해당 월의 인증서 데이터를 찾을 수 없습니다.");
        }
        int totalUsers = scoreMapper.countRankingUsers(period);
        MissionCertificateStatsRow stats = scoreMapper.findCertificateStats(
                userId, yearMonth.atDay(1), yearMonth.atEndOfMonth());
        if (stats == null) {
            stats = new MissionCertificateStatsRow();
        }
        return MissionCertificateTitleInput.builder()
                .yearMonth(period)
                .totalScore(ranking.getTotalScore())
                .topPercent(calculateTopPercent(ranking.getRank(), totalUsers))
                .successMissionCount(stats.getSuccessMissionCount())
                .completedMissionCount(stats.getCompletedMissionCount())
                .streakDays(stats.getStreakDays())
                .bestStreakDays(stats.getBestStreakDays())
                .build();
    }

    private YearMonth validateFinalizedMonth(String rawYearMonth) {
        try {
            YearMonth yearMonth = YearMonth.parse(rawYearMonth);
            if (!yearMonth.isBefore(YearMonth.now(clock.withZone(SEOUL_ZONE)))) {
                throw new BusinessException("CERTIFICATE_NOT_FINALIZED", "인증서는 랭킹이 확정된 전월까지 발급할 수 있습니다.");
            }
            return yearMonth;
        } catch (BusinessException exception) {
            throw exception;
        } catch (DateTimeParseException | NullPointerException exception) {
            throw new BusinessException("INVALID_YEAR_MONTH", "yearMonth는 YYYY-MM 형식이어야 합니다.");
        }
    }

    private List<String> fallbackTitles(MissionRankingRow ranking, YearMonth yearMonth) {
        int totalUsers = scoreMapper.countRankingUsers(yearMonth.toString());
        int topPercent = calculateTopPercent(ranking.getRank(), totalUsers);
        return List.of(
                "상위 " + topPercent + "%의 판결력",
                "이번 달 진짜 무죄",
                "절약 판결 갱신"
        );
    }

    private int calculateTopPercent(int rank, int totalUsers) {
        return totalUsers == 0 ? 0 : (int) Math.ceil(rank * 100.0 / totalUsers);
    }

    private String inputHash(MissionCertificateTitleInput input) {
        try {
            byte[] source = objectMapper.writeValueAsBytes(input);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(source));
        } catch (Exception exception) {
            throw new BusinessException("AI_PROVIDER_UNAVAILABLE", "AI 명예 타이틀 요청을 준비하지 못했어요.");
        }
    }
}
