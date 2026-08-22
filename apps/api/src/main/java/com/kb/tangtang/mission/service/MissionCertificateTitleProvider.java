package com.kb.tangtang.mission.service;

import com.kb.tangtang.mission.domain.MissionCertificateTitleInput;

import java.util.List;

public interface MissionCertificateTitleProvider {
    List<String> generate(MissionCertificateTitleInput input);
}
