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

    /**
     * 알림 1건. 남의 알림이면 null 이다.
     * DLQ 재처리가 "이미 저장된 알림을 다시 푸시" 할 때 쓴다 — createdAt·isRead 를 지어내지 않기 위해 DB 에서 읽는다.
     */
    Notification findById(@Param("id") long id, @Param("userId") long userId);

    int countUnread(@Param("userId") long userId);

    /**
     * 같은 종류·같은 딥링크의 **안 읽은** 알림 수. 중복 알림 억제에 쓴다 (이슈 #70).
     *
     * 딥링크까지 보는 이유는 계좌마다 재연동 알림이 따로 가야 하기 때문이다 —
     * 종류만 보면 국민은행 알림이 떠 있는 동안 신한은행 알림이 막힌다.
     */
    int countUnreadSame(@Param("userId") long userId,
                        @Param("type") String type,
                        @Param("deepLinkUrl") String deepLinkUrl);

    /** 남의 알림이면 0 을 돌려준다 — 서비스가 이것으로 NOT_FOUND 를 판정한다 */
    int markRead(@Param("id") long id, @Param("userId") long userId);

    int markAllRead(@Param("userId") long userId);
}
