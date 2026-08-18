package com.kb.tangtang.mission.service;

import com.kb.tangtang.mission.mapper.MissionCertificateTitleMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;

@Service
@Log4j2
public class MissionCertificateTitleBatchService {

    private final MissionCertificateTitleMapper mapper;
    private final MissionCertificateTitleService titleService;
    private final Clock clock;
    private final int maxAttempts;
    private final int retryDelayMinutes;

    @Autowired
    public MissionCertificateTitleBatchService(MissionCertificateTitleMapper mapper,
                                               MissionCertificateTitleService titleService,
                                               @Value("${mission.certificate.title.batch.max-attempts:2}") int maxAttempts,
                                               @Value("${mission.certificate.title.batch.retry-delay-minutes:20}") int retryDelayMinutes,
                                               @Value("${mission.assignment.zone:Asia/Seoul}") String zoneId) {
        this(mapper, titleService, Clock.system(ZoneId.of(zoneId)), maxAttempts, retryDelayMinutes);
    }

    MissionCertificateTitleBatchService(MissionCertificateTitleMapper mapper,
                                        MissionCertificateTitleService titleService,
                                        Clock clock, int maxAttempts, int retryDelayMinutes) {
        this.mapper = mapper;
        this.titleService = titleService;
        this.clock = clock;
        this.maxAttempts = maxAttempts;
        this.retryDelayMinutes = retryDelayMinutes;
    }

    public void generatePreviousMonthTitles() {
        LocalDateTime now = LocalDateTime.now(clock);
        YearMonth targetMonth = YearMonth.from(now).minusMonths(1);
        int generated = 0;
        for (Long userId : mapper.findGenerationCandidates(
                targetMonth.toString(), maxAttempts, now.minusMinutes(retryDelayMinutes))) {
            try {
                titleService.generate(userId, targetMonth);
                generated++;
            } catch (RuntimeException exception) {
                log.error("인증서 AI 타이틀 생성 실패. userId={}, yearMonth={}", userId, targetMonth, exception);
            }
        }
        log.info("인증서 AI 타이틀 배치 완료. yearMonth={}, generated={}", targetMonth, generated);
    }
}
