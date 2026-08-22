package com.kb.tangtang.report.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChallengeReportMonthDto {

    private String value;
    private int year;
    private int month;
    private boolean available;
    private boolean hasReport;
    private boolean firstReport;
    private String status;
}
