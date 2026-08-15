package com.kb.tangtang.notification.domain;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 알림 종류와 **문구**. 값은 tbl_notification.type 컬럼(VARCHAR(30))에 그대로 들어간다.
 *
 * 문구를 여기에 모아 둔 이유는, 발행하는 도메인마다 문구를 들고 있으면 6개 모듈에 흩어져
 * 톤이 제각각이 되기 때문이다. 발행자는 치환값만 넘기고 문장은 이 파일이 소유한다.
 * (이슈 #68 · 팀 결정 2026-08-07)
 *
 * 2026-08-07 기준 실제로 이벤트가 발행되는 것은 ACCOUNT_RECONNECT 하나다.
 * 나머지는 발행 주체(challenge·mission·report·fixedexpense) 백엔드가 아직 없어
 * db/seed_notification_demo.sql 로만 들어간다. (DECISIONS.md 2026-08-06 (3))
 */
public enum NotificationType {

    ACCOUNT_RECONNECT("계좌 재연동이 필요해요", "{bankName} · 인증이 만료됐어요"),

    /*
     * 그룹 챌린지 상태 전이 배치가 발행한다 (이슈 #152).
     *
     * 기존 GROUP_TRIAL_OPENED 로 대신하지 않은 이유는 제목이 NotificationService 에서
     * type.getDefaultTitle() 로 고정돼 발행자가 바꿀 수 없어서다 — 재활용하면 챌린지가
     * 시작됐을 뿐인데 알림함에 "재판이 열렸어요" 가 뜬다.
     */
    GROUP_CHALLENGE_STARTED("챌린지가 시작됐어요", "{groupName} · 오늘부터 {days}일간"),
    GROUP_CHALLENGE_CANCELED("챌린지가 성립되지 않았어요", "{groupName} · 참여자가 모자라 종료됐어요"),

    /*
     * 아래 5종은 문구가 아직 정해지지 않았다. {content} 는 "발행자가 넘긴 문구를 그대로 쓴다" 는 뜻이다.
     * 담당자가 기능을 붙일 때 이 자리에 실제 템플릿을 써 넣으면 문구가 이 파일로 모인다.
     *   예) GROUP_JUDGMENT("판결이 확정됐어요", "{trialName} · 내 사건 {verdict}")
     */
    GROUP_JUDGMENT("판결이 확정됐어요", "{content}"),
    GROUP_DEFENSE_REGISTERED("변론이 등록됐어요", "{content}"),
    GROUP_TRIAL_OPENED("재판이 열렸어요", "{content}"),
    MISSION_DEADLINE("오늘 미션 마감 임박", "{content}"),
    MONTHLY_REPORT("판결문이 도착했어요", "{content}"),
    PAYMENT_DUE("결제 예정 알림", "{content}");

    /** {이름} 형태의 자리표시자. 이름은 영문·숫자만 받는다. */
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([A-Za-z0-9]+)\\}");

    private final String defaultTitle;
    private final String contentTemplate;

    NotificationType(String defaultTitle, String contentTemplate) {
        this.defaultTitle = defaultTitle;
        this.contentTemplate = contentTemplate;
    }

    public String getDefaultTitle() {
        return defaultTitle;
    }

    public String getContentTemplate() {
        return contentTemplate;
    }

    /**
     * 템플릿의 자리표시자를 채워 알림 본문을 만든다.
     *
     * ⚠ 값이 없으면 예외를 던진다. 자리표시자를 그대로 둔 채 내보내면
     * 사용자가 알림함에서 "{bankName} · 인증이 만료됐어요" 를 보게 된다.
     * 반쯤 만들어진 문구를 내보내느니 실패시키고 DLQ 에 남기는 편이 낫다.
     *
     * @param params 자리표시자 이름 → 값
     * @throws IllegalArgumentException 채우지 못한 자리표시자가 있을 때
     */
    public String render(Map<String, String> params) {
        Map<String, String> safe = params == null ? Map.of() : params;
        Matcher matcher = PLACEHOLDER.matcher(contentTemplate);
        StringBuilder rendered = new StringBuilder();
        while (matcher.find()) {
            String key = matcher.group(1);
            String value = safe.get(key);
            if (value == null) {
                throw new IllegalArgumentException(
                        name() + " 알림 문구에 필요한 값이 없습니다: " + key);
            }
            matcher.appendReplacement(rendered, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(rendered);
        return rendered.toString();
    }
}
