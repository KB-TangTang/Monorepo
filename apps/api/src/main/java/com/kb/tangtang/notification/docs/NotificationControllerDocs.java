package com.kb.tangtang.notification.docs;

import com.kb.tangtang.common.dto.ApiResponse;
import com.kb.tangtang.notification.dto.NotificationListDto;
import com.kb.tangtang.notification.dto.UnreadCountDto;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import springfox.documentation.annotations.ApiIgnore;

/** {@code NotificationController} 의 Swagger 문서. */
@Api(tags = "09. 알림 - 목록 · 읽음 처리 · SSE 실시간 스트림")
public interface NotificationControllerDocs {

    @ApiOperation(value = "알림 목록 조회",
            notes = "**커서 기반 페이지네이션**이다. 첫 페이지는 `cursor` 없이 호출하고, "
                    + "응답의 마지막 알림 ID 를 다음 요청의 `cursor` 로 넘긴다.")
    ApiResponse<NotificationListDto> list(@ApiIgnore Long userId,
                                          @ApiParam(value = "이 ID 보다 과거의 알림을 가져온다. 첫 페이지는 생략") Long cursor,
                                          @ApiParam(value = "한 번에 가져올 개수. 생략 시 서버 기본값") Integer size);

    @ApiOperation(value = "안 읽은 알림 개수", notes = "탭바 배지에 쓴다.")
    ApiResponse<UnreadCountDto> unreadCount(@ApiIgnore Long userId);

    @ApiOperation(value = "알림 1건 읽음 처리",
            notes = "처리 후 **갱신된 안 읽은 개수**를 돌려준다. 배지를 즉시 고칠 수 있게 하기 위함이다.")
    ApiResponse<UnreadCountDto> read(@ApiIgnore Long userId,
                                     @ApiParam(value = "알림 ID", required = true) long id);

    @ApiOperation(value = "전체 읽음 처리", notes = "처리 후 갱신된 안 읽은 개수(0)를 돌려준다.")
    ApiResponse<UnreadCountDto> readAll(@ApiIgnore Long userId);

    @ApiOperation(value = "실시간 알림 SSE 스트림",
            notes = "**스트림이라 `ApiResponse` 로 감싸지 않는다.** `text/event-stream` 을 그대로 반환한다.\n\n"
                    + "Swagger 의 Try it out 으로는 확인할 수 없다. 연결이 끊기지 않아 응답이 끝나지 않는다.\n\n"
                    + "연결 직후 `connected` 이벤트를 한 번 보낸다. 프록시 버퍼링을 깨고 프론트가 "
                    + "「연결됨」 을 판정할 근거를 주기 위함이며, 이걸 못 받으면 프론트가 폴링으로 강등한다.")
    SseEmitter stream(@ApiIgnore Long userId);
}
