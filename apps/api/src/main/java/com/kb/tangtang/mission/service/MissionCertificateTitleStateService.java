package com.kb.tangtang.mission.service;

import com.kb.tangtang.mission.mapper.MissionCertificateTitleMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MissionCertificateTitleStateService {

    private final MissionCertificateTitleMapper mapper;

    public MissionCertificateTitleStateService(MissionCertificateTitleMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int claim(long userId, String yearMonth, String provider, String model,
                     String promptVersion, String inputHash) {
        return mapper.claimGeneration(userId, yearMonth, provider, model, promptVersion, inputHash);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int complete(long userId, String yearMonth, List<String> titles) {
        return mapper.completeGeneration(userId, yearMonth, titles.get(0), titles.get(1), titles.get(2));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int fail(long userId, String yearMonth, String failureCode) {
        return mapper.failGeneration(userId, yearMonth, failureCode);
    }
}
