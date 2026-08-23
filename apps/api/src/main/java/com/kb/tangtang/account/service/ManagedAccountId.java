package com.kb.tangtang.account.service;

/**
 * 연결 계좌 관리 목록 한 행의 표시용 식별자 (#467).
 *
 * <p>이 화면은 서로 다른 세 테이블을 한 목록에 섞는다 — 은행·예적금·증권·페이머니는
 * {@code tbl_connected_account}, 대출은 {@code tbl_loan}, 카드는 {@code tbl_card}.
 * 각자의 PK 를 그대로 쓰면 <b>id 가 겹친다</b>(대출 7번과 카드 7번). 목록 키가 겹치면 화면이 행을
 * 잘못 묶고, {@code DELETE /accounts/{id}} 는 어느 테이블을 끌지 알 수 없다.
 *
 * <p>그래서 종류를 id 안에 실어 보낸다. <b>이 값은 응답을 만들 때마다 계산하는 임시값이라 DB 어디에도
 * 저장되지 않는다</b> — 규칙을 바꿔도 기존 데이터에 영향이 없다(그래서 마이그레이션 없이 도입했다).
 *
 * <pre>
 *   은행·예적금·증권·페이머니 →  양수, tbl_connected_account.id 그대로
 *   대출                     →  -(1_000_000_000 + tbl_loan.id)
 *   카드                     →  -(2_000_000_000 + tbl_card.id)
 * </pre>
 *
 * <p>음수를 쓰는 이유는 양수 구간을 실제 PK 에 그대로 내주기 위해서다. 기준값은 10억 간격이라
 * 한 사용자의 대출·카드 PK 가 그만큼 커지지 않는 한 겹치지 않는다.
 */
public final class ManagedAccountId {

    /** 목록 행이 어느 테이블에서 왔는지. */
    public enum Kind {
        ACCOUNT, LOAN, CARD
    }

    private static final long LOAN_BASE = 1_000_000_000L;
    private static final long CARD_BASE = 2_000_000_000L;

    private ManagedAccountId() {
    }

    public static long ofLoan(long loanId) {
        return -(LOAN_BASE + loanId);
    }

    public static long ofCard(long cardId) {
        return -(CARD_BASE + cardId);
    }

    /** 양수는 전부 tbl_connected_account 행이다. */
    public static Kind kindOf(long managedId) {
        if (managedId >= 0) {
            return Kind.ACCOUNT;
        }
        return -managedId >= CARD_BASE ? Kind.CARD : Kind.LOAN;
    }

    /**
     * 원래 테이블의 PK. {@link #kindOf}가 ACCOUNT 면 그 값 그대로다.
     *
     * <p>범위를 벗어난 값(예: -5 같은 옛 형식이나 화면이 지어낸 값)은 여기서 0 이하가 되어
     * 호출부의 조회가 자연스럽게 실패한다 — 별도 검증 없이 NOT_FOUND 로 떨어진다.
     */
    public static long rawId(long managedId) {
        switch (kindOf(managedId)) {
            case CARD:
                return -managedId - CARD_BASE;
            case LOAN:
                return -managedId - LOAN_BASE;
            default:
                return managedId;
        }
    }
}
