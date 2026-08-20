package com.kb.tangtang.account.service;

import com.kb.tangtang.account.dto.InstitutionDto;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 연동 지원 금융기관 목록.
 *
 * 테이블을 만들지 않고 코드에 둔다 — 기관 목록은 거의 바뀌지 않고, 바뀌어도 배포로 충분하다.
 * 동의 항목을 `ConsentCatalog` 에 둔 것과 같은 판단이다.
 *
 * 은행·카드·증권의 코드값은 CODEF organization 코드다. 실 CODEF 로 전환해도 그대로 쓴다.
 * 반면 <b>대출·페이머니 두 업권은 CODEF 코드를 확인하지 못해 자체 코드를 쓴다.</b>
 * 실 CODEF 로 전환하면 매핑이 필요하다. `PAY_KB` 는 목서버 `financial_institution` 시드에
 * 이미 있는 코드와 같은 값으로 맞춘 것이다.
 *
 * 코드는 <b>10자를 넘기지 않는다</b> — `tbl_connected_account.bank_code` 가 `VARCHAR(10)` 이다.
 * 쿠팡페이를 `PAY_CPANG` 로 줄인 이유가 이것이다.
 */
@Component
public class InstitutionCatalog {

    /** 기관 코드 → 이름·약칭. 화면 순서를 유지해야 하므로 LinkedHashMap 을 쓴다. */
    private static final Map<String, String[]> BANKS = new LinkedHashMap<>();
    private static final Map<String, String[]> CARDS = new LinkedHashMap<>();
    private static final Map<String, String[]> SECURITIES = new LinkedHashMap<>();
    private static final Map<String, String[]> LOANS = new LinkedHashMap<>();
    private static final Map<String, String[]> PAY_MONEY = new LinkedHashMap<>();

    /**
     * 전체 그룹. `nameOf()`·`shortLabelOf()` 가 공유한다.
     *
     * 같은 그룹 목록을 두 메서드에 각각 적어 두면 업권을 늘릴 때 한쪽만 고쳐 조용히 어긋난다 —
     * 실제로 그 구조였기에 목록을 여기 하나로 모았다.
     */
    private static final List<Map<String, String[]>> ALL_GROUPS =
            List.of(BANKS, CARDS, SECURITIES, LOANS, PAY_MONEY);

    static {
        BANKS.put("0004", new String[]{"KB국민은행", "KB"});
        BANKS.put("0090", new String[]{"카카오뱅크", "kb"});
        BANKS.put("0092", new String[]{"토스뱅크", "toss"});
        BANKS.put("0088", new String[]{"신한은행", "신한"});
        BANKS.put("0020", new String[]{"우리은행", "우리"});
        BANKS.put("0081", new String[]{"하나은행", "하나"});
        BANKS.put("0011", new String[]{"NH농협은행", "NH"});
        BANKS.put("0089", new String[]{"케이뱅크", "K"});
        BANKS.put("0003", new String[]{"IBK기업은행", "IBK"});

        CARDS.put("0301", new String[]{"신한카드", "신한"});
        CARDS.put("0381", new String[]{"KB국민카드", "KB"});
        CARDS.put("0361", new String[]{"BC카드", "BC"});
        CARDS.put("0364", new String[]{"삼성카드", "삼성"});
        CARDS.put("0366", new String[]{"현대카드", "현대"});
        CARDS.put("0371", new String[]{"롯데카드", "롯데"});

        SECURITIES.put("0240", new String[]{"삼성증권", "삼성"});
        SECURITIES.put("0218", new String[]{"KB증권", "KB"});
        SECURITIES.put("0243", new String[]{"한국투자증권", "한투"});
        SECURITIES.put("0247", new String[]{"NH투자증권", "NH"});
        SECURITIES.put("0261", new String[]{"교보증권", "교보"});

        /*
         * 대출 — 할부금융(캐피탈)·저축은행.
         *
         * 마이데이터 기준으로 대출은 독립 업권이 아니다. 은행 업권 API 가 수신·대출 계좌를
         * 함께 준다. 독립 업권으로 존재하는 것은 할부금융(캐피탈)·저축은행이라
         * 이 칩에는 그쪽을 담는다.
         */
        LOANS.put("CP_KB", new String[]{"KB캐피탈", "KB"});
        LOANS.put("CP_HYUNDAI", new String[]{"현대캐피탈", "현대"});
        LOANS.put("CP_SHINHAN", new String[]{"신한캐피탈", "신한"});
        LOANS.put("CP_HANA", new String[]{"하나캐피탈", "하나"});
        LOANS.put("CP_WOORI", new String[]{"우리금융캐피탈", "우리"});
        LOANS.put("SB_SBI", new String[]{"SBI저축은행", "SBI"});
        LOANS.put("SB_OK", new String[]{"OK저축은행", "OK"});
        LOANS.put("SB_WELCOME", new String[]{"웰컴저축은행", "웰컴"});

        /*
         * 페이머니 — 전자금융업자(선불충전 잔액을 가진 곳).
         *
         * 삼성페이는 선불충전 잔액이 없어(카드 등록 방식) 여기에 넣지 않는다.
         */
        PAY_MONEY.put("PAY_KAKAO", new String[]{"카카오페이", "카카오"});
        PAY_MONEY.put("PAY_NAVER", new String[]{"네이버페이", "네이버"});
        PAY_MONEY.put("PAY_TOSS", new String[]{"토스페이", "토스"});
        PAY_MONEY.put("PAY_PAYCO", new String[]{"페이코", "페이코"});
        PAY_MONEY.put("PAY_KB", new String[]{"KB Pay", "KB"});
        PAY_MONEY.put("PAY_CPANG", new String[]{"쿠팡페이", "쿠팡"});
    }

    public List<InstitutionDto> banks(List<String> connectedCodes) {
        return build(BANKS, connectedCodes);
    }

    public List<InstitutionDto> cards(List<String> connectedCodes) {
        return build(CARDS, connectedCodes);
    }

    public List<InstitutionDto> securities(List<String> connectedCodes) {
        return build(SECURITIES, connectedCodes);
    }

    public List<InstitutionDto> loans(List<String> connectedCodes) {
        return build(LOANS, connectedCodes);
    }

    public List<InstitutionDto> payMoney(List<String> connectedCodes) {
        return build(PAY_MONEY, connectedCodes);
    }

    /**
     * 대출 기관코드인지. 계좌 선택 화면이 "자동 연동" 미리보기를 만들 때 쓴다(#334) —
     * 대출·페이머니는 fetchAccounts() 가 다루는 은행 엔드포인트가 아니라 별도 엔드포인트라
     * 계좌 목록에 안 뜨고, 그래서 이 코드로 직접 걸러 미리보기를 따로 만든다.
     */
    public boolean isLoanCode(String code) {
        return LOANS.containsKey(code);
    }

    /** 페이머니 기관코드인지. isLoanCode() 와 같은 이유. */
    public boolean isPayMoneyCode(String code) {
        return PAY_MONEY.containsKey(code);
    }

    /** 기관명. 알 수 없는 코드는 코드 그대로 돌려준다(화면이 빈칸이 되지 않게). */
    public String nameOf(String code) {
        for (Map<String, String[]> group : ALL_GROUPS) {
            String[] found = group.get(code);
            if (found != null) {
                return found[0];
            }
        }
        return code;
    }

    public String shortLabelOf(String code) {
        for (Map<String, String[]> group : ALL_GROUPS) {
            String[] found = group.get(code);
            if (found != null) {
                return found[1];
            }
        }
        return "";
    }

    private static List<InstitutionDto> build(Map<String, String[]> source,
                                              List<String> connectedCodes) {
        return source.entrySet().stream()
                .map(entry -> InstitutionDto.builder()
                        .code(entry.getKey())
                        .name(entry.getValue()[0])
                        .shortLabel(entry.getValue()[1])
                        .connected(connectedCodes.contains(entry.getKey()))
                        .build())
                .toList();
    }
}
