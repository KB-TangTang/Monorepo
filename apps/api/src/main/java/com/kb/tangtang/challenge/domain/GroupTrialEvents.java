package com.kb.tangtang.challenge.domain;

/**
 * 재판 진행 이벤트. 채팅방에 시스템 메시지를 띄우는 신호다 (이슈 #174).
 *
 * <p>발행은 재판 흐름을 쥔 쪽(#169~#172)이 하고, 수신은 ChatSystemMessageListener 가 한다.
 * <pre>
 *     events.publishEvent(new GroupTrialEvents.TrialOpened(groupId, indictmentId, nickname));
 * </pre>
 *
 * <p><b>여기서 채팅 패키지의 타입을 참조하지 않는다.</b> 의존은 chat → challenge 한 방향이다.
 * 판결 결과도 값(boolean · int)으로만 싣고, 화면이 쓰는 모양으로 묶는 일은 수신부가 한다.
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

    /**
     * 판결이 확정됐다.
     *
     * <p>요약 문구뿐 아니라 <b>결과 자체</b>를 싣는다(이슈 #304). 채팅 카드가 유죄·무죄 도장을
     * 고르고 「투표 4:2 · 목숨 1 차감」을 적으려면 그 값이 밖으로 나와야 하는데, 예전에는
     * 확정 서비스 안에서만 갈리고 문구 한 줄로 뭉개져 나갔다. 문구를 파싱해 도장을 고르는 방식은
     * 쓰지 않는다 — 문구를 한 글자만 고쳐도 무죄에 붉은 도장이 찍힌다.
     *
     * <p>생성자를 감추고 {@link #byVote} · {@link #byConfession} 으로만 만들게 한 것은 인자가
     * 일곱 개라 순서를 바꿔 넣어도 컴파일이 통과하기 때문이다.
     */
    public static final class VerdictConfirmed {
        private final long groupId;
        private final long indictmentId;
        private final String summary;
        private final boolean guilty;
        private final Integer guiltyVotes;
        private final Integer innocentVotes;
        private final int livesLost;

        private VerdictConfirmed(long groupId, long indictmentId, String summary, boolean guilty,
                                 Integer guiltyVotes, Integer innocentVotes, int livesLost) {
            this.groupId = groupId;
            this.indictmentId = indictmentId;
            this.summary = summary;
            this.guilty = guilty;
            this.guiltyVotes = guiltyVotes;
            this.innocentVotes = innocentVotes;
            this.livesLost = livesLost;
        }

        /**
         * 개표를 거친 판결. 표가 한 장도 없어 무죄 추정으로 끝난 건도 여기다(0:0 으로 실린다).
         *
         * @param livesLost 실제로 깎인 목숨. 유죄여도 남은 목숨이 없었으면 0 이다
         */
        public static VerdictConfirmed byVote(long groupId, long indictmentId, String summary,
                                              boolean guilty, int guiltyVotes, int innocentVotes,
                                              int livesLost) {
            return new VerdictConfirmed(groupId, indictmentId, summary, guilty,
                    guiltyVotes, innocentVotes, livesLost);
        }

        /** 혐의 인정. 투표가 없어 표 분포가 비고, 결과는 항상 유죄다. */
        public static VerdictConfirmed byConfession(long groupId, long indictmentId, String summary,
                                                    int livesLost) {
            return new VerdictConfirmed(groupId, indictmentId, summary, true, null, null, livesLost);
        }

        public long getGroupId() { return groupId; }
        public long getIndictmentId() { return indictmentId; }
        public String getSummary() { return summary; }
        public boolean isGuilty() { return guilty; }

        /** 투표를 거치지 않은 판결(혐의 인정)이면 null 이다. */
        public Integer getGuiltyVotes() { return guiltyVotes; }

        /** 투표를 거치지 않은 판결(혐의 인정)이면 null 이다. */
        public Integer getInnocentVotes() { return innocentVotes; }

        public int getLivesLost() { return livesLost; }
    }
}
