package com.kb.tangtang.report.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MonthlyReportMonthDto {

    private String value;
    private int year;
    private int month;
    private boolean available;
    private boolean hasReport;
    private String status;
}
