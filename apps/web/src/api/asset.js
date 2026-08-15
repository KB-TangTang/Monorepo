import http from './http';
import { formatSyncTime } from '@/utils/account';

/**
 * 자산 현황 API.
 *
 * 요약은 `GET /api/assets/summary`, 추이 상세 화면 전용 조회는 `GET /api/assets/trend`(이슈 #240),
 * 종류별 상세는 `GET /api/assets/accounts|investments|loans`(이슈 #240 후속)를 쓴다.
 */

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
 * GET /api/assets/trend 원본 응답.
 * @param {string} [baseDate] YYYY-MM-DD. 생략하면 서버가 오늘 날짜(Asia/Seoul)를 쓴다.
 */
function fetchNetWorthTrendRaw(baseDate) {
    return http.get('/assets/trend', { params: baseDate ? { baseDate } : undefined });
}

/**
 * NetWorthTrendView 가 쓰는 뷰모델.
 *
 * netWorth·totalDebt 배열의 각 원소는 해당 월 자산 스냅샷이 없으면 null 이다.
 * 가장 최근 달(오늘이 속한 달)만은 항상 값이 있다.
 */
export async function fetchNetWorthTrend(baseDate) {
    const raw = await fetchNetWorthTrendRaw(baseDate);

    return {
        months: raw.trend.map((point) => `${Number(point.yearMonth.split('-')[1])}월`),
        netWorth: raw.trend.map((point) => point.netWorth),
        totalDebt: raw.trend.map((point) => point.totalDebt),
    };
}

/** 여러 계좌 중 가장 최근 동기화 시각. 하나도 없으면 null(formatSyncTime 이 "동기화 이력 없음"으로 처리). */
function latestSyncAt(accounts) {
    const times = accounts.map((account) => account.lastSyncAt).filter(Boolean);
    return times.length ? times.sort().at(-1) : null;
}

/**
 * GET /api/assets/accounts?type= 원본 응답 → 화면 뷰모델.
 * 계좌번호가 없는 계좌(예: 일부 페이머니)는 은행명으로 대체한다 — 실존하지 않는 만기일 등은
 * 지어내지 않는다(tbl_connected_account 에 만기 컬럼이 없다).
 */
function accountDetailToViewModel(raw, type) {
    const { tone } = presentationOf(type);
    return {
        total: raw.total,
        syncedLabel: formatSyncTime(latestSyncAt(raw.accounts)),
        accounts: raw.accounts.map((account) => ({
            code: `account-${account.accountId}`,
            label: account.accountName || account.bankName,
            meta: account.accountNoMasked || account.bankName,
            amount: account.balance,
            badge: account.shortLabel || presentationOf(type).badge,
            tone,
        })),
    };
}

/** AssetCheckingView 가 쓰는 뷰모델. */
export async function fetchCheckingAccountDetail() {
    const raw = await http.get('/assets/accounts', { params: { type: 'DEMAND_DEPOSIT' } });
    return accountDetailToViewModel(raw, 'DEMAND_DEPOSIT');
}

/** AssetSavingsView 가 쓰는 뷰모델. */
export async function fetchSavingsAccountDetail() {
    const raw = await http.get('/assets/accounts', { params: { type: 'SAVINGS' } });
    return accountDetailToViewModel(raw, 'SAVINGS');
}

/** AssetPaymoneyView 가 쓰는 뷰모델. */
export async function fetchPaymoneyDetail() {
    const raw = await http.get('/assets/accounts', { params: { type: 'PAY_MONEY' } });
    return accountDetailToViewModel(raw, 'PAY_MONEY');
}

/**
 * AssetInvestmentView 가 쓰는 뷰모델.
 * gainAmount/returnRate 는 백엔드가 tbl_investment_holding 에 동기화된 손익값을 그대로 내려준다 —
 * 프론트에서 다시 계산하지 않는다(합계 손익만 화면에서 totalValuation - totalCost 로 파생한다).
 */
export async function fetchInvestmentAccountDetail() {
    const raw = await http.get('/assets/investments');
    return {
        totalValuation: raw.totalValuation,
        totalCost: raw.totalCost,
        asOfLabel: formatSyncTime(raw.asOf),
        holdings: raw.holdings.map((holding) => ({
            code: `holding-${holding.accountId}-${holding.symbol}`,
            name: holding.name,
            badge: holding.name.charAt(0),
            tone: presentationOf('SECURITIES').tone,
            quantity: holding.quantity,
            amount: holding.marketValue,
            gainAmount: holding.profitLossAmount,
            returnRate: holding.profitLossRate,
        })),
    };
}

/** AssetLoanView 가 쓰는 뷰모델. */
export async function fetchLoanAccountDetail() {
    const raw = await http.get('/assets/loans');
    const { tone } = presentationOf('LOAN');
    return {
        total: raw.total,
        loans: raw.loans.map((loan) => ({
            code: `loan-${loan.loanId}`,
            label: `${loan.bankName} ${loan.loanType}`,
            meta: `금리 ${loan.interestRate}% · 만기 ${loan.maturityDate}`,
            amount: loan.balance,
            badge: loan.bankName.charAt(0),
            tone,
        })),
    };
}
