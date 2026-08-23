/*
 * 그룹 채팅 말풍선 묶기 규칙. GroupChatView 가 목록을 만들 때 쓴다.
 *
 * 규칙이 두 개인데 기준이 서로 다르다. 한 곳에 모아 둔 이유가 그것이다.
 *   - 이름·아바타 숨김: 같은 사람이 5분 안에 이어 보내면 반복하지 않는다 (isGroupedMessage)
 *   - 시간 숨김:        같은 쪽이 같은 "분"에 이어 보내면 마지막 줄에만 남긴다 (shouldShowTime)
 *                       — 참여자는 사람 단위, 재판 알림 필은 알림끼리 한 묶음이다
 *
 * 시간까지 5분 창으로 묶으면 10:01 에 보낸 줄의 시간이 사라지고 10:05 만 남아 정보가 준다.
 * 분 단위로 끊으면 연달아 친 줄만 합쳐지고 분이 바뀌면 다시 보인다 — 카카오톡과 같은 방식이다.
 *
 * 메시지는 api/groupChatAdapter.js 가 정규화한 모양이다
 * ({ messageId, type, isSystem, senderId, senderName, content, sentAt: Date|null }).
 */

/** 이름·아바타를 반복하지 않을 연속 발화 판정 기준 */
export const GROUPING_WINDOW_MS = 5 * 60 * 1000;

/** 같은 사람이 5분 안에 잇달아 보낸 메시지면 이름·아바타를 반복하지 않는다 */
export function isGroupedMessage(prev, msg) {
    if (!prev || prev.isSystem || msg.isSystem) return false;
    if (Number(prev.senderId) !== Number(msg.senderId)) return false;
    if (!prev.sentAt || !msg.sentAt) return false;
    return msg.sentAt - prev.sentAt < GROUPING_WINDOW_MS;
}

/**
 * 이 메시지에 시간을 찍을지 정한다.
 *
 * `next` 는 화면에서 바로 아래에 오는 메시지다. 사이에 날짜 구분선이나
 * "여기부터 새 메시지" 경계가 끼면 호출부가 null 을 넘긴다 — 구분선 위 줄에는 시간을 남긴다.
 *
 * 재판 알림 필도 같은 규칙을 탄다. 기소 한 건이 적발·개시로 <b>같은 초에 두 줄</b> 떨어지는데
 * (ChatSystemMessageListener 가 두 이벤트를 잇달아 쏜다) 줄마다 같은 시각이 찍히면
 * 시각이 정보가 아니라 반복되는 얼룩이 된다.
 */
export function shouldShowTime(msg, next) {
    /* 서버가 시각을 안 줬거나 해석에 실패한 메시지다. 빈 자리를 만들지 않는다 */
    if (!msg?.sentAt) return false;
    if (!next) return true;

    /* 필과 말풍선은 서로 묶지 않는다 — 가운데·좌우로 정렬이 달라 한 덩어리로 읽히지 않는다 */
    if (Boolean(msg.isSystem) !== Boolean(next.isSystem)) return true;

    /* 필에는 보낸 사람이 없다. 재판 알림끼리는 종류가 달라도 「법정」 하나가 말한 것으로 본다 */
    if (!msg.isSystem && Number(next.senderId) !== Number(msg.senderId)) return true;

    if (!next.sentAt) return true;
    return !isSameMinute(msg.sentAt, next.sentAt);
}

/**
 * 말풍선 옆·재판 알림 필 아래에 찍는 시각. 24시간제 `HH:MM` 이다.
 *
 * 서버가 시각을 안 줬거나 파싱에 실패하면(`sentAt: null`) 빈 문자열을 돌려준다 —
 * 화면이 빈 자리를 만들거나 `NaN:NaN` 을 그리지 않는다.
 */
export function clockLabel(date) {
    if (!date) return '';

    const hh = String(date.getHours()).padStart(2, '0');
    const mm = String(date.getMinutes()).padStart(2, '0');
    return `${hh}:${mm}`;
}

function isSameMinute(a, b) {
    return (
        a.getFullYear() === b.getFullYear() &&
        a.getMonth() === b.getMonth() &&
        a.getDate() === b.getDate() &&
        a.getHours() === b.getHours() &&
        a.getMinutes() === b.getMinutes()
    );
}

/*
 * ── 재판 알림 필 표시 규칙 (이슈 #452) ─────────────────────────────────
 *
 * 카드 세 종(기록·판결·폴백 pill)을 한 줄짜리 필 하나로 합쳤다. 규칙을 컴포넌트가 아니라
 * 여기에 두는 이유는 이 저장소에 SFC 렌더링 하네스가 없기 때문이다 — 컴포넌트 안의 computed 는
 * 소스를 정규식으로 훑는 방식으로밖에 검사하지 못해 「무죄에 붉은 색을 쓰지 않는다」 같은
 * 규칙이 그 방식으로는 지켜지지 않는다.
 *
 * 입력은 groupChatAdapter 가 정규화한 시스템 메시지다
 * ({ content, systemType, deepLink, verdict }).
 */

/** 판결이 아닌 세 종의 색. 판결은 verdict.outcome 이 정한다 */
const TONE_BY_TYPE = {
    VIOLATION_DETECTED: 'ink',
    TRIAL_OPENED: 'ink',
    DEFENSE_REGISTERED: 'gold',
};

/*
 * 버튼 문구.
 *
 * TRIAL_OPENED 에는 버튼이 없다. 이 시점의 기소 상태는 DEFENSE_WAIT 이라
 * VoteService 가 투표를 VOTE_NOT_ALLOWED 로 거절하고, 변론은 아직 존재하지 않는다.
 * 바로 앞줄에 VIOLATION_DETECTED 의 「사건 보기」가 이미 떠 있어 갈 길도 막히지 않는다.
 *
 * 「투표하기」가 DEFENSE_REGISTERED 에 붙는 것은 그 순간이 곧 투표 개시라서다 —
 * DefenseService 가 변론을 저장하면서 상태를 VOTING 으로 넘기고 이 이벤트를 쏜다.
 */
const CTA_BY_TYPE = {
    VIOLATION_DETECTED: '사건 보기',
    DEFENSE_REGISTERED: '투표하기',
};

/*
 * 배경 없는 텍스트로 그릴 버튼.
 *
 * 판결은 <b>이미 끝난 일</b>이다. 적발·투표는 지금 눌러야 하는 것이고, 판결문은 궁금하면 보는 것이다.
 * 채운 뱃지를 네 번 다 쓰면 기소 한 건이 흘러갈 때마다 방이 뱃지로 덮여, 정작 마감이 있는
 * 「투표하기」가 나머지 셋에 묻힌다. 시안도 판결 확정 필만 `background:none` 인 텍스트 버튼이다.
 */
const QUIET_CTA = new Set(['판결문']);

/*
 * 도장 그림이 있는 결과. verdictTone 과 값은 같지만 정하는 것이 다르다 — 저쪽은 색이고
 * 이쪽은 <b>아바타 자리에 무엇을 띄울지</b>다.
 *
 * 판결 필의 아바타(49_verdict.png)는 유죄와 무죄에 같은 그림을 쓴다. 「판결이 났다」까지만
 * 말하고 결과는 말하지 않는다는 뜻이다. 도장으로 바꾸면 색 말고도 결과를 읽을 방법이 하나 생긴다.
 *
 * <b>버튼과는 무관하다.</b> 딥링크가 없어 「판결문」을 접는 경우에도 도장은 그대로 뜬다 —
 * 결과를 알리는 일과 판결문으로 보내는 일은 서로 다른 일이다.
 *
 * 결과를 모르는 옛 판결 메시지(#304 이전)는 띄울 도장이 없어 탕이 얼굴로 남는다.
 */
const STAMPED_OUTCOMES = new Set(['GUILTY', 'INNOCENT']);

/** 서버 deepLink 는 네 종류 모두 trialProgress 를 가리킨다 (ChatSystemMessageListener#trialLink) */
const TRIAL_LINK = /^\/group-challenges\/(\d+)\/trial\/(\d+)$/;

/**
 * 「투표하기」가 갈 곳.
 *
 * 투표 화면(VoteVerdictView)이 변론서·증거를 먼저 펼친 뒤 유·무죄를 받는다. 그래서 버튼 하나로
 * 「변론을 보고 투표한다」가 끝난다. 투표할 수 없는 상태면 그 화면이 알아서 판결 화면이나
 * 진행 현황으로 되돌리므로(VoteVerdictView.vue:36-52) 막다른 길이 생기지 않는다.
 *
 * 딥링크 모양이 달라지면 서버가 준 값을 그대로 쓴다 — 최악이 「진행 현황으로 간다」이지
 * 빈 화면이 아니다.
 */
function voteLink(deepLink) {
    const matched = TRIAL_LINK.exec(deepLink ?? '');
    return matched ? `/group-challenges/${matched[1]}/vote/${matched[2]}` : deepLink;
}

/**
 * 「판결문」이 갈 곳.
 *
 * 서버 deepLink 는 재판 진행 현황(타임라인)이라 <b>버튼 문구와 도착지가 어긋나 있었다</b> —
 * 판결문을 눌러도 타임라인에 떨어져 거기서 「판결 보기」를 한 번 더 눌러야 했다.
 * 최종 판결 화면(router/index.js:238)으로 바로 보낸다.
 *
 * VerdictResultView 가 확정 여부를 스스로 확인하고 아니면 진행 현황으로 되돌리므로
 * (VerdictResultView.vue:33-38) 막다른 길이 생기지 않는다 — voteLink 와 같은 안전장치다.
 */
function verdictLink(deepLink) {
    return TRIAL_LINK.test(deepLink ?? '') ? `${deepLink}/verdict` : deepLink;
}

/** 버튼별로 딥링크를 다시 쓰는 규칙. 없으면 서버가 준 값을 그대로 쓴다 */
const LINK_BY_CTA = {
    투표하기: voteLink,
    판결문: verdictLink,
};

/**
 * 필에 적을 문구. content 의 첫 문장 하나만 쓴다.
 *
 * 마침표를 못 찾으면 통째로 쓴다. 금액은 「12,400원」이라 마침표가 없어 이 규칙에 걸리지 않는다.
 */
function firstSentence(content) {
    const end = content.indexOf('.');
    return end === -1 ? content : content.slice(0, end + 1);
}

/**
 * 문구를 「닉네임」과 「나머지」로 가른다.
 *
 * 닉네임이 1~50자라(utils/user.js) 넘침은 예외가 아니라 정상 경로다. 그래서 이 함수가 정하는
 * 것은 <b>무엇을 먼저 버릴지</b>다. 한 덩어리로 두면 뒤가 잘려 「가나다라마바사아…」만 남고
 * 무슨 일이 일어났는지가 통째로 사라진다. 닉네임 쪽만 줄이면
 * 「가나다라마…님의 소비가 한도를 넘었어요.」로 <b>문장이 끝까지 살아남는다.</b>
 *
 * ⚠ 처음에는 「한 줄 + 통째로 ellipsis」로 뒀었다. 닉네임이 50자까지 가능하니 어떤 배치로도
 * 한 줄에 못 담는다는 이유였는데, 50자라는 극단값에 맞추느라 <b>실제 대부분인 2~10자까지</b>
 * 같이 잘리게 만든 판단이었다. 360px 에서 텍스트 폭이 약 14자인데 적발 문구는 닉네임 2자에서도
 * 17자다 — 브라우저에서 확인하고 뒤집었다.
 *
 * 서버 문구 네 종이 모두 `{닉네임}님` 으로 시작한다(ChatSystemMessageListener ·
 * GroupVerdictTransitionService). 첫 「님」 앞까지를 닉네임으로 본다.
 *
 * ⚠ 결정 ⑤(문구 파싱 금지)에서 한 발 나가는 지점이다. 다만 이것은 <b>색·결과 판정이 아니라
 * 어디서 줄일지를 고르는 표시 규칙</b>이고, 못 찾으면 통째로 그린다 — 최악이 「예전처럼 뒤가
 * 잘린다」이지 화면이 깨지거나 색이 뒤집히지 않는다.
 *
 * 닉네임 자체가 「님」으로 시작하면(「님님」) 앞이 비므로 가르지 않는다.
 */
function splitName(text) {
    const at = text.indexOf('님');
    if (at <= 0) return { leadName: '', lead: text };
    return { leadName: text.slice(0, at), lead: text.slice(at) };
}

/**
 * 판결 필의 색.
 *
 * 문구에서 "유죄" 를 찾아 고르지 않는다 — 문구를 한 글자만 고쳐도 무죄 판결이 붉게 뜬다.
 * 무죄를 초록으로 두는 것은 한 줄 필에서 <b>색이 유일한 결과 신호</b>이기 때문이다.
 * 시안은 판결 확정을 통째로 붉게 뒀지만, 62px 도장이 사라진 자리에서 무죄에 붉은 색을 쓰면
 * 정보가 뒤집힌다.
 *
 * @returns {'danger'|'success'|'muted'} muted 는 이 필드가 생기기 전(#304)에 Redis 에 쌓인 판결이다
 */
function verdictTone(verdict) {
    if (verdict?.outcome === 'GUILTY') return 'danger';
    if (verdict?.outcome === 'INNOCENT') return 'success';
    return 'muted';
}

/**
 * 시스템 메시지 한 건을 필 한 줄로 옮긴다.
 *
 * systemType 을 모르는 메시지(이 필드가 생기기 전에 저장된 것)는 <b>문구를 자르지도, 가르지도
 * 않는다.</b> 첫 문장 자르기와 닉네임 분리는 둘 다 서버 문구의 모양을 아는 것을 전제로 하는데,
 * 모양을 모르는 문구에 적용하면 뒤쪽을 소리 없이 버리거나 엉뚱한 데를 굵게 만든다.
 *
 * `leadName` 이 빈 문자열이면 화면은 `lead` 를 통째로 그린다.
 *
 * `stamp` 는 아바타 자리에 탕이 얼굴 대신 띄울 판결 도장이다 — 위 STAMPED_OUTCOMES 참고.
 * `ctaQuiet` 는 버튼을 배경 없는 글자로 그릴지다 — 위 QUIET_CTA 참고.
 *
 * @returns {{tone: string, leadName: string, lead: string, stamp: string|null,
 *            ctaLabel: string|null, ctaTo: string|null, ctaQuiet: boolean}}
 */
export function trialPillView(message) {
    const content = message?.content ?? '';
    const systemType = message?.systemType ?? null;
    const isVerdict = systemType === 'VERDICT_CONFIRMED';

    if (!isVerdict && !TONE_BY_TYPE[systemType]) {
        return {
            tone: 'muted',
            leadName: '',
            lead: content,
            stamp: null,
            ctaLabel: null,
            ctaTo: null,
            ctaQuiet: false,
        };
    }

    const label = isVerdict ? '판결문' : (CTA_BY_TYPE[systemType] ?? null);
    const rewrite = LINK_BY_CTA[label];
    const to = rewrite ? rewrite(message.deepLink) : message.deepLink;

    /* 문구가 없거나 갈 곳이 없으면 버튼을 그리지 않는다 — 눌러도 아무 일이 없는 버튼이 된다 */
    const hasCta = Boolean(label) && Boolean(to);
    const outcome = message?.verdict?.outcome;

    return {
        tone: isVerdict ? verdictTone(message.verdict) : TONE_BY_TYPE[systemType],
        ...splitName(firstSentence(content)),
        stamp: isVerdict && STAMPED_OUTCOMES.has(outcome) ? outcome : null,
        ctaLabel: hasCta ? label : null,
        ctaTo: hasCta ? to : null,
        ctaQuiet: hasCta && QUIET_CTA.has(label),
    };
}
