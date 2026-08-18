package com.kb.tangtang.mission.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbsoluteMissionPolicyTest {

    private final AbsoluteMissionPolicy policy = new AbsoluteMissionPolicy();

    @Test
    void monthlyInspectionDayIsStableAndWithinMonth() {
        YearMonth month = YearMonth.of(2026, 8);

        int first = policy.monthlyInspectionDay(month);
        int second = policy.monthlyInspectionDay(month);

        assertEquals(first, second);
        assertTrue(first >= 1 && first <= month.lengthOfMonth());
        assertTrue(policy.isMonthlyInspectionDate(month.atDay(first)));
    }

    @Test
    void leapYearFebruaryProducesValidDay() {
        YearMonth month = YearMonth.of(2028, 2);

        int day = policy.monthlyInspectionDay(month);

        assertTrue(day >= 1 && day <= 29);
    }

    @Test
    void sameDateAndMonthProduceSameMissionIndex() {
        LocalDate date = LocalDate.of(2026, 8, 18);

        assertEquals(policy.coldStartMissionIndex(date, 15),
                policy.coldStartMissionIndex(date, 15));
        assertEquals(policy.monthlyMissionIndex(YearMonth.from(date), 15),
                policy.monthlyMissionIndex(YearMonth.from(date), 15));
        assertNotEquals(policy.coldStartMissionIndex(date, 15),
                policy.coldStartMissionIndex(date.plusDays(1), 15));
    }
}
