package com.kb.tangtang.mission.mapper;

import com.kb.tangtang.mission.domain.MissionCertificateTitle;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface MissionCertificateTitleMapper {

    List<Long> findGenerationCandidates(@Param("yearMonth") String yearMonth,
                                        @Param("maxAttempts") int maxAttempts,
                                        @Param("retryNotBefore") LocalDateTime retryNotBefore);

    int insertIfAbsent(@Param("userId") long userId, @Param("yearMonth") String yearMonth);

    MissionCertificateTitle findByUserIdAndYearMonth(@Param("userId") long userId,
                                                      @Param("yearMonth") String yearMonth);

    int claimGeneration(@Param("userId") long userId,
                        @Param("yearMonth") String yearMonth,
                        @Param("provider") String provider,
                        @Param("model") String model,
                        @Param("promptVersion") String promptVersion,
                        @Param("inputHash") String inputHash);

    int completeGeneration(@Param("userId") long userId,
                           @Param("yearMonth") String yearMonth,
                           @Param("title1") String title1,
                           @Param("title2") String title2,
                           @Param("title3") String title3);

    int failGeneration(@Param("userId") long userId,
                       @Param("yearMonth") String yearMonth,
                       @Param("failureCode") String failureCode);
}
