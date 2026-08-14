import http from './http';
import {
    CHECKING_DETAIL,
    SAVINGS_DETAIL,
    INVESTMENT_DETAIL,
    LOAN_DETAIL,
    PAYMONEY_DETAIL,
} from '@/fixtures/asset';

/**
 * 자산 현황 API.
 *
 * 요약·순자산 추이는 백엔드 `GET /api/assets/summary`(이슈 #240)를 쓴다.
 * 계좌 종류별 상세(입출금·예적금·투자·대출·페이머니 상세 화면)는 아직 백엔드 엔드포인트가 없어
 * 계속 목업(fixtures/asset.js)을 반환한다 — 백엔드가 준비되면 이 파일의 fetch*Detail 함수만
 * http 호출로 바꾸면 된다.
 */

function clone(value) {
    return JSON.parse(JSON.stringify(value));
}

/*
 * 백엔드 account_type ↔ 화면 전용 표시값(라우팅 code · 색상 tone · 이니셜 badge).
 * 백엔드는 순수 데이터(금액·개수)만 내려주고, 이런 프레젠테이션 값은 화면 관심사라 프론트가 가진다.
 * tone 5종은 tokens.css v2 의 서로 다른 색상 계열(Ink/Blue/Green/Gold/Red)에 하나씩 대응해
 * 다섯 자산 종류가 항상 구분되는 색으로 보이게 한다 — utils/asset.js 의 TONE_COLORS 참고.
 */
const TYPE_PRESENTATION = {
    DEMAND_DEPOSIT: { code: 'checking', tone: 'navy', badge: 'W' },
    SAVINGS: { code: 'savings', tone: 'blue', badge: 'S' },
    SECURITIES: { code: 'investment', tone: 'teal', badge: 'I' },
    PAY_MONEY: { code: 'paymoney', tone: 'accent', badge: 'P' },
    LOAN: { code: 'loan', tone: 'danger', badge: 'L' },
};

function presentationOf(type) {
    return TYPE_PRESENTATION[type] ?? { code: type.toLowerCase(), tone: 'gray', badge: '?' };
}

/**
 * GET /api/assets/summary 원본 응답.
 * @param {string} [baseDate] YYYY-MM-DD. 생략하면 서버가 오늘 날짜(Asia/Seoul)를 쓴다.
 */
function fetchAssetSummaryRaw(baseDate) {
    return http.get('/assets/summary', { params: baseDate ? { baseDate } : undefined });
}

/** AssetHomeView 가 쓰는 뷰모델로 변환한다. */
export async function fetchAssetSummary(baseDate) {
    const raw = await fetchAssetSummaryRaw(baseDate);

    return {
        netWorth: raw.netWorth,
        // changeAmount 는 전월 자산 스냅샷이 없으면 null 이다(가입 초기 등) — 그 달은 "변화 없음"으로 본다.
        monthOverMonthChange: raw.changeAmount ?? 0,
        // 스파크라인은 값이 있는 점만으로 그린다. null 이 섞이면 min/max 계산이 깨진다.
        trend: raw.trend.map((point) => point.netWorth).filter((value) => value !== null),
        composition: raw.composition.map((item) => ({
            code: presentationOf(item.type).code,
            label: item.label,
            amount: item.amount,
            tone: presentationOf(item.type).tone,
        })),
        accounts: raw.assetGroups.map((item) => ({
            code: presentationOf(item.type).code,
            label: item.label,
            badge: presentationOf(item.type).badge,
            count: item.count,
            amount: item.amount,
            tone: presentationOf(item.type).tone,
        })),
    };
}

/**
 * NetWorthTrendView 가 쓰는 뷰모델. 요약과 같은 API 를 한 번 더 호출한다 —
 * 백엔드에 추이 전용 엔드포인트가 따로 없고, 두 화면이 동시에 열리지 않아 중복 호출 비용이 없다.
 *
 * netWorth·totalDebt 배열의 각 원소는 해당 월 자산 스냅샷이 없으면 null 이다.
 * 가장 최근 달(오늘이 속한 달)만은 항상 값이 있다.
 */
export async function fetchNetWorthTrend(baseDate) {
    const raw = await fetchAssetSummaryRaw(baseDate);

    return {
        months: raw.trend.map((point) => `${Number(point.yearMonth.split('-')[1])}월`),
        netWorth: raw.trend.map((point) => point.netWorth),
        totalDebt: raw.trend.map((point) => point.totalDebt),
    };
}

/** TODO(#240 후속): 백엔드 상세 API 준비되면 http 호출로 교체 */
export async function fetchCheckingAccountDetail() {
    return clone(CHECKING_DETAIL);
}

/** TODO(#240 후속): 백엔드 상세 API 준비되면 http 호출로 교체 */
export async function fetchSavingsAccountDetail() {
    return clone(SAVINGS_DETAIL);
}

/** TODO(#240 후속): 백엔드 상세 API 준비되면 http 호출로 교체 */
export async function fetchInvestmentAccountDetail() {
    return clone(INVESTMENT_DETAIL);
}

/** TODO(#240 후속): 백엔드 상세 API 준비되면 http 호출로 교체 */
export async function fetchLoanAccountDetail() {
    return clone(LOAN_DETAIL);
}

/** TODO(#240 후속): 백엔드 상세 API 준비되면 http 호출로 교체 */
export async function fetchPaymoneyDetail() {
    return clone(PAYMONEY_DETAIL);
}
