package com.kb.tangtang.account.service;

import com.kb.tangtang.account.dto.InstitutionDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 기관 카탈로그 검증.
 *
 * 업권을 5종으로 넓히면서(#344) 코드 길이·중복·조회 메서드가 함께 어긋나기 쉬워졌다.
 * 특히 `nameOf()`·`shortLabelOf()` 는 전체 그룹 목록을 공유하므로 새 업권이 빠지면 조용히 실패한다.
 */
class InstitutionCatalogTest {

    private final InstitutionCatalog catalog = new InstitutionCatalog();

    /** `tbl_connected_account.bank_code` 가 VARCHAR(10) 이다. */
    private static final int MAX_CODE_LENGTH = 10;

    private List<InstitutionDto> allInstitutions() {
        List<InstitutionDto> all = new ArrayList<>();
        all.addAll(catalog.banks(List.of()));
        all.addAll(catalog.cards(List.of()));
        all.addAll(catalog.securities(List.of()));
        all.addAll(catalog.loans(List.of()));
        all.addAll(catalog.payMoney(List.of()));
        return all;
    }

    private static List<String> codesOf(List<InstitutionDto> items) {
        return items.stream().map(InstitutionDto::getCode).toList();
    }

    @Test
    @DisplayName("업권 5종의 기관 수가 정해진 대로다")
    void keepsInstitutionCountPerGroup() {
        assertEquals(9, catalog.banks(List.of()).size());
        assertEquals(6, catalog.cards(List.of()).size());
        assertEquals(5, catalog.securities(List.of()).size());
        assertEquals(8, catalog.loans(List.of()).size());
        assertEquals(6, catalog.payMoney(List.of()).size());
    }

    @Test
    @DisplayName("대출·페이머니는 화면 순서를 그대로 유지한다")
    void keepsDeclaredOrderForNewGroups() {
        assertEquals(
                List.of("CP_KB", "CP_HYUNDAI", "CP_SHINHAN", "CP_HANA", "CP_WOORI",
                        "SB_SBI", "SB_OK", "SB_WELCOME"),
                codesOf(catalog.loans(List.of())));
        assertEquals(
                List.of("PAY_KAKAO", "PAY_NAVER", "PAY_TOSS", "PAY_PAYCO", "PAY_KB", "PAY_CPANG"),
                codesOf(catalog.payMoney(List.of())));
    }

    @Test
    @DisplayName("업권을 통틀어 중복 코드가 없다")
    void hasNoDuplicateCodes() {
        List<String> codes = codesOf(allInstitutions());
        Set<String> unique = new HashSet<>(codes);
        assertEquals(codes.size(), unique.size(), "중복 코드: " + codes);
    }

    @Test
    @DisplayName("모든 기관 코드는 10자를 넘지 않는다")
    void keepsCodeWithinColumnLength() {
        for (String code : codesOf(allInstitutions())) {
            assertTrue(code.length() <= MAX_CODE_LENGTH,
                    "bank_code VARCHAR(10) 을 넘는 코드: " + code);
        }
    }

    @Test
    @DisplayName("이름·약칭이 비어 있는 기관이 없다")
    void hasNameAndShortLabelForEveryInstitution() {
        for (InstitutionDto item : allInstitutions()) {
            assertNotNull(item.getName(), item.getCode() + " 의 이름이 없다");
            assertFalse(item.getName().isBlank(), item.getCode() + " 의 이름이 비었다");
            assertNotNull(item.getShortLabel(), item.getCode() + " 의 약칭이 없다");
            assertFalse(item.getShortLabel().isBlank(), item.getCode() + " 의 약칭이 비었다");
        }
    }

    @Test
    @DisplayName("nameOf 는 새 업권 코드도 찾는다")
    void findsNameForEveryGroup() {
        assertEquals("KB국민은행", catalog.nameOf("0004"));
        assertEquals("신한카드", catalog.nameOf("0301"));
        assertEquals("삼성증권", catalog.nameOf("0240"));
        assertEquals("KB증권", catalog.nameOf("0218"));
        assertEquals("KB캐피탈", catalog.nameOf("CP_KB"));
        assertEquals("웰컴저축은행", catalog.nameOf("SB_WELCOME"));
        assertEquals("카카오페이", catalog.nameOf("PAY_KAKAO"));
        assertEquals("쿠팡페이", catalog.nameOf("PAY_CPANG"));
    }

    @Test
    @DisplayName("shortLabelOf 는 새 업권 코드도 찾는다")
    void findsShortLabelForEveryGroup() {
        assertEquals("KB", catalog.shortLabelOf("0004"));
        assertEquals("신한", catalog.shortLabelOf("0301"));
        assertEquals("삼성", catalog.shortLabelOf("0240"));
        assertEquals("KB", catalog.shortLabelOf("0218"));
        assertEquals("현대", catalog.shortLabelOf("CP_HYUNDAI"));
        assertEquals("토스", catalog.shortLabelOf("PAY_TOSS"));
    }

    @Test
    @DisplayName("모르는 코드는 이름 대신 코드 자체를 돌려준다")
    void fallsBackToCodeWhenUnknown() {
        assertEquals("9999", catalog.nameOf("9999"));
        assertEquals("", catalog.shortLabelOf("9999"));
    }

    @Test
    @DisplayName("connectedCodes 에 든 코드만 connected 로 표시한다")
    void marksOnlyConnectedCodes() {
        List<InstitutionDto> loans = catalog.loans(List.of("CP_KB"));
        assertTrue(loans.stream().filter(i -> "CP_KB".equals(i.getCode()))
                .findFirst().orElseThrow().isConnected());
        assertEquals(1, loans.stream().filter(InstitutionDto::isConnected).count());

        List<InstitutionDto> pays = catalog.payMoney(List.of("PAY_TOSS", "PAY_CPANG"));
        assertEquals(2, pays.stream().filter(InstitutionDto::isConnected).count());
        /* 다른 업권 코드를 넘겨도 이 업권에는 아무 표시가 붙지 않는다 */
        assertEquals(0, catalog.banks(List.of("PAY_TOSS")).stream()
                .filter(InstitutionDto::isConnected).count());
    }

    @Test
    @DisplayName("KB증권은 증권 업권에서 연결 상태로 표시한다")
    void marksKbSecuritiesAsConnected() {
        InstitutionDto kbSecurities = catalog.securities(List.of("0218")).stream()
                .filter(item -> "0218".equals(item.getCode()))
                .findFirst()
                .orElseThrow();

        assertEquals("KB증권", kbSecurities.getName());
        assertEquals("KB", kbSecurities.getShortLabel());
        assertTrue(kbSecurities.isConnected());
    }
}
