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
 * 코드값은 CODEF organization 코드다. 실 CODEF 로 전환해도 그대로 쓴다.
 */
@Component
public class InstitutionCatalog {

    /** 기관 코드 → 이름·약칭. 화면 순서를 유지해야 하므로 LinkedHashMap 을 쓴다. */
    private static final Map<String, String[]> BANKS = new LinkedHashMap<>();
    private static final Map<String, String[]> CARDS = new LinkedHashMap<>();
    private static final Map<String, String[]> SECURITIES = new LinkedHashMap<>();

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
        SECURITIES.put("0243", new String[]{"한국투자증권", "한투"});
        SECURITIES.put("0247", new String[]{"NH투자증권", "NH"});
        SECURITIES.put("0261", new String[]{"교보증권", "교보"});
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

    /** 기관명. 알 수 없는 코드는 코드 그대로 돌려준다(화면이 빈칸이 되지 않게). */
    public String nameOf(String code) {
        for (Map<String, String[]> group : List.of(BANKS, CARDS, SECURITIES)) {
            String[] found = group.get(code);
            if (found != null) {
                return found[0];
            }
        }
        return code;
    }

    public String shortLabelOf(String code) {
        for (Map<String, String[]> group : List.of(BANKS, CARDS, SECURITIES)) {
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
