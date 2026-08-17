package com.kb.tangtang.mission.domain;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MissionCertificateTitle {
    private long userId;
    private String yearMonth;
    private String title1;
    private String title2;
    private String title3;
    private String status;
    private String provider;
    private String failureCode;
    private int attemptCount;
}
