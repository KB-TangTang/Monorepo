/**
 * 알림 표시용 순수 함수 모음.
 *
 * 화면 로직을 여기에 두는 이유는 이 프로젝트의 프론트 테스트 하네스가
 * node --test 기반이라 컴포넌트를 띄우지 못하기 때문이다. 판정은 전부 여기서 한다.
 */

const VISUALS = {
    ACCOUNT_RECONNECT: { icon: 'refresh', tone: 'muted' },
    GROUP_CHALLENGE_STARTED: { icon: 'gavel', tone: 'accent' },
    GROUP_CHALLENGE_CANCELED: { icon: 'gavel', tone: 'muted' },
    GROUP_JUDGMENT: { icon: 'gavel', tone: 'dark' },
    GROUP_TRIAL_OPENED: { icon: 'gavel', tone: 'dark' },
    MISSION_DEADLINE: { icon: 'clock', tone: 'accent' },
    MONTHLY_REPORT: { icon: 'book', tone: 'muted' },
    PAYMENT_DUE: { icon: 'calendar', tone: 'muted' },
};

const DEFAULT_VISUAL = { icon: 'bell', tone: 'muted' };

function startOfDay(date) {
    return new Date(date.getFullYear(), date.getMonth(), date.getDate());
}

function pad(value) {
    return String(value).padStart(2, '0');
}

function isoDate(date) {
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

/** 알림 목록을 오늘 / 어제 / 날짜 로 묶는다. 입력 순서(최신순)를 유지한다. */
export function groupByDay(items, now) {
    const today = startOfDay(now).getTime();
    const yesterday = today - 86400000;
    const groups = [];
    const index = new Map();

    for (const item of items) {
        const created = new Date(item.createdAt);
        const day = startOfDay(created).getTime();
        const label = day === today ? '오늘' : day === yesterday ? '어제' : isoDate(created);
        if (!index.has(label)) {
            const group = { label, items: [] };
            index.set(label, group);
            groups.push(group);
        }
        index.get(label).items.push(item);
    }

    return groups;
}

/** 목록에 쓰는 상대 시간. 어제 이전은 시각을 함께 보여준다. */
export function formatRelativeTime(createdAt, now) {
    const created = new Date(createdAt);
    const diffMinutes = Math.floor((now.getTime() - created.getTime()) / 60000);

    if (diffMinutes < 1) {
        return '방금';
    }
    if (diffMinutes < 60) {
        return `${diffMinutes}분 전`;
    }

    const today = startOfDay(now).getTime();
    const day = startOfDay(created).getTime();
    const time = `${pad(created.getHours())}:${pad(created.getMinutes())}`;

    if (day === today) {
        return `${Math.floor(diffMinutes / 60)}시간 전`;
    }
    if (day === today - 86400000) {
        return `어제 ${time}`;
    }
    return `${isoDate(created)} ${time}`;
}

export function notificationVisual(type) {
    return VISUALS[type] ?? DEFAULT_VISUAL;
}

/**
 * 알림의 딥링크를 라우터에 넘겨도 되는지 판정한다.
 *
 * deep_link_url 은 DB 값이라 그대로 router.push 하면 오픈 리다이렉트가 된다.
 * '/' 로 시작하는 것만으로는 부족하다 — '//evil.com' 은 프로토콜 상대 URL 로 해석되고
 * '/\evil.com' 도 일부 브라우저가 '//' 로 정규화한다. (stores/auth.js 의 isSafeRedirectPath 와 같은 방어)
 *
 * @returns {string|null} 안전하면 경로, 아니면 null
 */
export function resolveDeepLink(url) {
    if (typeof url !== 'string' || url === '') {
        return null;
    }
    if (!url.startsWith('/') || url.startsWith('//') || url.startsWith('/\\')) {
        return null;
    }
    return url;
}
