package com.kb.tangtang.transaction.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MerchantNameNormalizerTest {

    @Test
    void nullIsNull() {
        assertNull(MerchantNameNormalizer.normalize(null));
    }

    @Test
    void removesWhitespace() {
        assertEquals("스타벅스강남점", MerchantNameNormalizer.normalize("스타벅스 강남점"));
    }

    @Test
    void removesCorporateMarkers() {
        assertEquals("쿠팡", MerchantNameNormalizer.normalize("(주)쿠팡"));
        assertEquals("쿠팡", MerchantNameNormalizer.normalize("주식회사 쿠팡"));
    }

    @Test
    void lowercasesAscii() {
        assertEquals("starbucks", MerchantNameNormalizer.normalize("Starbucks"));
    }

    @Test
    void removesSpecialCharacters() {
        assertEquals("배달의민족", MerchantNameNormalizer.normalize("배달의민족(주)!!"));
    }

    @Test
    void equivalentInputsNormalizeToSameKey() {
        assertEquals(
                MerchantNameNormalizer.normalize("쿠팡이츠 "),
                MerchantNameNormalizer.normalize(" 쿠팡이츠"));
    }
}
