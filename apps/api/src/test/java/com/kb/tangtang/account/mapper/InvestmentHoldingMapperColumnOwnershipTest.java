package com.kb.tangtang.account.mapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * updatePosition(목서버 동기화)·updatePrice(토스 실시간 시세)의 컬럼 소유권 분리는 지금
 * Javadoc/XML 주석으로만 지켜진다 — 컴파일러가 못 잡는다(QA 지적사항). 이 테스트가 그 안전망이다.
 *
 * 규칙(InvestmentHoldingMapper.xml 참고):
 * - updatePosition 은 last_price 를 **SET 대상으로** 쓰면 안 된다. 단, market_value 등을
 *   재계산하려고 last_price 를 **읽는 것**(우변)은 의도된 동작이라 허용한다.
 * - updatePrice 는 수량·매입원가 등 position 컬럼을 건드리면 안 된다.
 */
class InvestmentHoldingMapperColumnOwnershipTest {

    private static String updateBlock(String xml, String id) {
        Pattern pattern = Pattern.compile(
                "<update id=\"" + id + "\"[^>]*>(.*?)</update>", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(xml);
        assertTrue(matcher.find(), "update id=" + id + " 를 찾지 못했다");
        return matcher.group(1);
    }

    /** SET 절만 잘라낸다 — WHERE 절의 조건(예: symbol = #{symbol})까지 컬럼 검사에 섞이면 안 된다. */
    private static String setClause(String updateBlock) {
        int setIndex = updateBlock.indexOf("SET");
        int whereIndex = updateBlock.indexOf("WHERE");
        assertTrue(setIndex >= 0 && whereIndex > setIndex, "SET/WHERE 절을 못 찾았다");
        return updateBlock.substring(setIndex, whereIndex);
    }

    /** column 뒤에 바로 '=' 가 오면 SET 대상(쓰기)이다. 곱셈·뺄셈 등 우변에서 읽히는 건 걸리지 않는다. */
    private static boolean setsColumn(String setClause, String column) {
        return Pattern.compile("\\b" + column + "\\s*=").matcher(setClause).find();
    }

    private static String readMapperXml() throws IOException {
        try (InputStream in = InvestmentHoldingMapperColumnOwnershipTest.class
                .getResourceAsStream("/mapper/account/InvestmentHoldingMapper.xml")) {
            assertTrue(in != null, "InvestmentHoldingMapper.xml 을 클래스패스에서 못 찾았다");
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    @DisplayName("updatePosition은 last_price를 SET 대상으로 쓰지 않는다(읽기로 참조하는 것은 허용)")
    void updatePositionNeverSetsLastPrice() throws IOException {
        String setClause = setClause(updateBlock(readMapperXml(), "updatePosition"));
        assertFalse(setsColumn(setClause, "last_price"),
                "updatePosition 이 last_price 를 SET 하고 있다 — 그건 updatePrice(토스 실시간 시세)만 써야 한다");
    }

    @Test
    @DisplayName("updatePrice는 수량·매입원가 등 position 컬럼을 건드리지 않는다")
    void updatePriceNeverTouchesPositionColumns() throws IOException {
        String setClause = setClause(updateBlock(readMapperXml(), "updatePrice"));
        for (String column : new String[] {"quantity", "purchase_amount", "average_purchase_price",
                "name", "market_country", "currency"}) {
            assertFalse(setsColumn(setClause, column),
                    "updatePrice 가 position 컬럼(" + column + ")을 SET 하고 있다");
        }
    }
}
