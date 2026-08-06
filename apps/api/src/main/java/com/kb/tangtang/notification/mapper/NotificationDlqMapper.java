package com.kb.tangtang.notification.mapper;

import com.kb.tangtang.notification.domain.NotificationDlqRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** tbl_notification_dlq 접근. SQL 은 mapper/notification/NotificationDlqMapper.xml 에 있다. */
@Mapper
public interface NotificationDlqMapper {

    int insert(@Param("eventType") String eventType,
               @Param("payloadJson") String payloadJson,
               @Param("errorMessage") String errorMessage);

    /** 재시도 횟수가 남은 행. 다음 시각 도래 여부는 스케줄러가 판단한다 */
    List<NotificationDlqRow> findRetryable(@Param("maxRetry") int maxRetry);

    int increaseRetry(@Param("dlqId") long dlqId);

    int delete(@Param("dlqId") long dlqId);
}
