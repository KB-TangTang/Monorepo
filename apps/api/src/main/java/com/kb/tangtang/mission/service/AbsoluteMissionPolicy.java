package com.kb.tangtang.mission.service;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.YearMonth;

@Component
public class AbsoluteMissionPolicy {

    public boolean isMonthlyInspectionDate(LocalDate date) {
        return date.getDayOfMonth() == monthlyInspectionDay(YearMonth.from(date));
    }

    public int monthlyInspectionDay(YearMonth yearMonth) {
        return stableIndex("MONTHLY_INSPECTION_DATE:" + yearMonth, yearMonth.lengthOfMonth()) + 1;
    }

    public int monthlyMissionIndex(YearMonth yearMonth, int candidateCount) {
        return stableIndex("MONTHLY_INSPECTION_MISSION:" + yearMonth, candidateCount);
    }

    public int coldStartMissionIndex(LocalDate date, int candidateCount) {
        return stableIndex("COLD_START_MISSION:" + date, candidateCount);
    }

    private int stableIndex(String seed, int bound) {
        if (bound <= 0) {
            throw new IllegalArgumentException("후보 개수는 1개 이상이어야 합니다.");
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(seed.getBytes(StandardCharsets.UTF_8));
            int value = ((digest[0] & 0xff) << 24)
                    | ((digest[1] & 0xff) << 16)
                    | ((digest[2] & 0xff) << 8)
                    | (digest[3] & 0xff);
            return Math.floorMod(value, bound);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", exception);
        }
    }
}
