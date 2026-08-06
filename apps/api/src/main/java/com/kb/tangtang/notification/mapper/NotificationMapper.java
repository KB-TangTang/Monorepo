package com.kb.tangtang.notification.mapper;

import com.kb.tangtang.notification.domain.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * tbl_notification 접근.
 *
 * ⚠ @Mapper 가 없으면 등록되지 않는다 (@MapperScan(annotationClass = Mapper.class)).
 * SQL 은 mapper/notification/NotificationMapper.xml 에 있다.
 */
@Mapper
public interface NotificationMapper {

    /** 저장. useGeneratedKeys 로 id 가 채워진다 */
    int insert(Notification notification);

    /**
     * 최신순 페이지. cursor 가 null 이면 처음부터, 있으면 그 id 보다 작은 것부터.
     * size 는 서비스에서 1~50 으로 자른 값이 들어온다.
     */
    List<Notification> findPage(@Param("userId") long userId,
                                @Param("cursor") Long cursor,
                                @Param("size") int size);

    int countUnread(@Param("userId") long userId);

    /** 남의 알림이면 0 을 돌려준다 — 서비스가 이것으로 NOT_FOUND 를 판정한다 */
    int markRead(@Param("id") long id, @Param("userId") long userId);

    int markAllRead(@Param("userId") long userId);
}
