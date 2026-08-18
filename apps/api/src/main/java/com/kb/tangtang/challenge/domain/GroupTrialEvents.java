package com.kb.tangtang.challenge.domain;

/**
 * 재판 진행 이벤트. 채팅방에 시스템 메시지를 띄우는 신호다 (이슈 #174).
 *
 * <p><b>발행은 #169~#172 담당자가 한다.</b> 각자 로직 끝에서 한 줄만 부르면 된다.
 * <pre>
 *     events.publishEvent(new GroupTrialEvents.TrialOpened(groupId, indictmentId, nickname));
 * </pre>
 *
 * <p>수신은 ChatSystemMessageListener 가 이미 하고 있다. 채팅 쪽에서 더 할 일은 없다.
 */
public final class GroupTrialEvents {

    private GroupTrialEvents() {
    }

    /** 소비 위반이 감지돼 기소 후보가 생겼다 */
    public static final class ViolationDetected {
        private final long groupId;
        private final long indictmentId;
        private final String targetNickname;

        public ViolationDetected(long groupId, long indictmentId, String targetNickname) {
            this.groupId = groupId;
            this.indictmentId = indictmentId;
            this.targetNickname = targetNickname;
        }

        public long getGroupId() { return groupId; }
        public long getIndictmentId() { return indictmentId; }
        public String getTargetNickname() { return targetNickname; }
    }

    /** 재판이 열렸다 */
    public static final class TrialOpened {
        private final long groupId;
        private final long indictmentId;
        private final String targetNickname;

        public TrialOpened(long groupId, long indictmentId, String targetNickname) {
            this.groupId = groupId;
            this.indictmentId = indictmentId;
            this.targetNickname = targetNickname;
        }

        public long getGroupId() { return groupId; }
        public long getIndictmentId() { return indictmentId; }
        public String getTargetNickname() { return targetNickname; }
    }

    /** 피고인이 변론을 등록했다 */
    public static final class DefenseRegistered {
        private final long groupId;
        private final long indictmentId;
        private final String targetNickname;

        public DefenseRegistered(long groupId, long indictmentId, String targetNickname) {
            this.groupId = groupId;
            this.indictmentId = indictmentId;
            this.targetNickname = targetNickname;
        }

        public long getGroupId() { return groupId; }
        public long getIndictmentId() { return indictmentId; }
        public String getTargetNickname() { return targetNickname; }
    }

    /** 판결이 확정됐다 */
    public static final class VerdictConfirmed {
        private final long groupId;
        private final long indictmentId;
        private final String summary;

        public VerdictConfirmed(long groupId, long indictmentId, String summary) {
            this.groupId = groupId;
            this.indictmentId = indictmentId;
            this.summary = summary;
        }

        public long getGroupId() { return groupId; }
        public long getIndictmentId() { return indictmentId; }
        public String getSummary() { return summary; }
    }
}
