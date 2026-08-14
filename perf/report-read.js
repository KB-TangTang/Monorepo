import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Trend } from 'k6/metrics';
import { createAccessToken } from './lib/jwt.js';

/*
 * 월간 리포트 조회 부하 시나리오 (이슈 #188)
 *
 * 재는 것: 거래내역을 집계하는 조회 API 3종의 응답시간·처리량.
 * 안 재는 것: AI 분석(OpenAI 과금) · 계좌 연동(목서버 실호출) · 배치 트리거.
 *
 * 실행 방법은 perf/README.md 참고.
 */

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const JWT_SECRET = __ENV.JWT_SECRET;

// seed_loadtest_local.sql 이 출력한 시작·끝 user_id 를 넣는다.
const USER_ID_FROM = Number(__ENV.USER_ID_FROM || 1);
const USER_ID_TO = Number(__ENV.USER_ID_TO || 50);

/*
 * 조회 대상 월. 기본값은 「지난달」이다.
 *
 * 이번 달을 넣으면 전부 400(REPORT_NOT_AVAILABLE) 이 된다 —
 * 월간 리포트는 완료된 월만 조회할 수 있기 때문이다. 진행 중인 달은 status=CURRENT 로 막힌다.
 */
const YEAR_MONTH = __ENV.YEAR_MONTH || defaultYearMonth();

function defaultYearMonth() {
    const d = new Date();
    d.setMonth(d.getMonth() - 1); // 1월이면 전년 12월로 정상 이월된다
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
}

/*
 * 엔드포인트별로 Trend 를 따로 둔다.
 * http_req_duration 하나만 보면 무거운 추이 조회가 가벼운 요약에 희석돼
 * 「무엇이 느린가」에 답할 수 없다.
 */
const trendSummary = new Trend('tt_summary', true);
const trendCategories = new Trend('tt_categories', true);
const trendTrend = new Trend('tt_spending_trend', true);
const trendMe = new Trend('tt_users_me', true);

export const options = {
    stages: [
        { duration: __ENV.RAMP_UP || '30s', target: Number(__ENV.VUS || 20) },
        { duration: __ENV.PLATEAU || '1m', target: Number(__ENV.VUS || 20) },
        { duration: '15s', target: 0 },
    ],
    thresholds: {
        // 실패율은 엄격하게 본다. 여기가 깨지면 응답시간 숫자는 의미가 없다.
        http_req_failed: ['rate<0.01'],
        // 응답시간은 「지표 확보」가 목적이라 넉넉히 잡는다. 넘으면 병목 후보로 본다.
        'tt_summary': ['p(95)<500'],
        'tt_categories': ['p(95)<500'],
        'tt_spending_trend': ['p(95)<800'],
        'tt_users_me': ['p(95)<200'],
    },
};

/*
 * VU 마다 다른 사용자로 붙는다. 한 사용자에 몰면 그 사용자의 데이터만 캐시에 올라
 * 실제보다 빠른 숫자가 나온다.
 *
 * init 컨텍스트에서는 __VU 가 0 이라 토큰을 만들 수 없다. 첫 반복에서 한 번만 만들어 재사용한다.
 */
let token = null;
let userId = null;

function auth() {
    if (token === null) {
        const span = USER_ID_TO - USER_ID_FROM + 1;
        userId = USER_ID_FROM + ((__VU - 1) % span);
        token = createAccessToken(userId, JWT_SECRET);
    }
    return { headers: { Authorization: `Bearer ${token}` } };
}

function measure(trend, response) {
    trend.add(response.timings.duration);
    return check(response, {
        '200 응답': (r) => r.status === 200,
        'success=true': (r) => {
            try {
                return r.json('success') === true;
            } catch (e) {
                return false;
            }
        },
    });
}

export default function () {
    const params = auth();

    group('월간 리포트', () => {
        measure(
            trendSummary,
            http.get(`${BASE_URL}/api/reports/monthly/summary?yearMonth=${YEAR_MONTH}`, {
                ...params,
                tags: { name: 'summary' },
            }),
        );

        measure(
            trendCategories,
            http.get(`${BASE_URL}/api/reports/monthly/categories?yearMonth=${YEAR_MONTH}`, {
                ...params,
                tags: { name: 'categories' },
            }),
        );

        /*
         * 6개월치를 훑지만 셋 중 가장 "가볍다"(2026-08-14 실측).
         * 기간이 길어도 결과가 월별 합계 몇 줄뿐이라서다.
         * 소분류 단위로 쪼개는 categories 가 그룹 수가 많아 제일 무겁다.
         */
        measure(
            trendTrend,
            http.get(`${BASE_URL}/api/reports/monthly/spending-trend?yearMonth=${YEAR_MONTH}`, {
                ...params,
                tags: { name: 'spending-trend' },
            }),
        );
    });

    // 집계가 없는 단순 조회. 위 숫자들을 해석할 기준선이 된다.
    group('기준선', () => {
        measure(trendMe, http.get(`${BASE_URL}/api/users/me`, { ...params, tags: { name: 'me' } }));
    });

    sleep(1);
}
