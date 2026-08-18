package com.kb.tangtang.challenge.docs;

import com.kb.tangtang.challenge.chat.dto.ChatMessagePageDto;
import com.kb.tangtang.challenge.chat.dto.ChatRoomDto;
import com.kb.tangtang.common.auth.LoginUser;
import com.kb.tangtang.common.docs.SwaggerTags;
import com.kb.tangtang.common.dto.ApiResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import springfox.documentation.annotations.ApiIgnore;

/** {@code ChatController} 의 Swagger 문서. */
@Api(tags = SwaggerTags.GROUP_CHALLENGE)
public interface ChatControllerDocs {

    @ApiOperation(value = "채팅방 정보 조회",
            notes = "오류 코드: NOT_FOUND(없는 챌린지) · CHAT_NOT_MEMBER(참여자 아님) "
                    + "· CHAT_ROOM_CLOSED(종료된 챌린지)")
    ApiResponse<ChatRoomDto> room(@PathVariable long groupId, @ApiIgnore @LoginUser Long userId);

    @ApiOperation(value = "메시지 목록 조회",
            notes = "before 는 위로 스크롤, after 는 재연결 후 놓친 구간 보충용이다. "
                    + "둘을 함께 주면 INVALID_REQUEST 다. 둘 다 없으면 최근 limit 건을 준다. "
                    + "limit 은 1~100 사이만 허용하며 범위를 벗어나면 INVALID_REQUEST 다.")
    ApiResponse<ChatMessagePageDto> messages(
            @PathVariable long groupId,
            @ApiParam(value = "이 messageId 보다 앞 구간") @RequestParam(required = false) Long before,
            @ApiParam(value = "이 messageId 보다 뒤 구간") @RequestParam(required = false) Long after,
            @ApiParam(value = "기본 50") @RequestParam(defaultValue = "50") int limit,
            @ApiIgnore @LoginUser Long userId);

    @ApiOperation(value = "안 읽은 수 초기화", notes = "채팅방에 들어가거나 복귀할 때 호출한다.")
    ApiResponse<Void> markRead(@PathVariable long groupId, @ApiIgnore @LoginUser Long userId);
}
