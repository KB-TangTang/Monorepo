package com.kb.tangtang.challenge.chat.domain;

import com.kb.tangtang.notification.domain.NotificationType;

/**
 * 시스템 메시지 한 건을 남기는 데 필요한 값 묶음 (이슈 #174).
 *
 * <p>문자열 파라미터가 넷이라 순서를 바꿔 넣어도 컴파일이 통과한다. 묶어서 이름으로 부르게 한다.
 *
 * @param content          화면에 그대로 보여줄 문구. 문구 조립은 발행 지점(리스너)이 끝낸다
 * @param systemType       화면이 카드 모양을 고르는 기준
 * @param deepLink         "재판 보러가기" 가 여는 라우터 경로. 없으면 화면이 버튼을 숨긴다
 * @param caseNo           사건번호. 표시 전용이라 서버는 형식만 정하고 파싱하지 않는다
 * @param notificationType 방 밖 참여자에게 나가는 알림 종류
 * @param verdict          판결 결과. {@link ChatSystemType#VERDICT_CONFIRMED} 에만 있고 나머지는 null
 */
public record ChatSystemMessageSpec(String content,
                                    ChatSystemType systemType,
                                    String deepLink,
                                    String caseNo,
                                    NotificationType notificationType,
                                    ChatVerdictInfo verdict) {

    /** 판결이 아닌 시스템 메시지 — 결과 값이 없다. */
    public ChatSystemMessageSpec(String content, ChatSystemType systemType, String deepLink,
                                 String caseNo, NotificationType notificationType) {
        this(content, systemType, deepLink, caseNo, notificationType, null);
    }
}
