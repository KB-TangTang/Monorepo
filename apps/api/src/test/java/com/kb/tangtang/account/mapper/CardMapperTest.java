package com.kb.tangtang.account.mapper;

import com.kb.tangtang.account.domain.Card;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 실 DB 연결이 필요하다. 로컬에서 수동으로 @Disabled 를 지우고 돌린 뒤 반드시 다시 붙인다
 * (db/AGENTS.md, apps/api/AGENTS.md 컨벤션 — DataSourceConnectionTest 참고).
 */
@Disabled("실 DB 연결 필요 — 로컬 수동 검증용")
@SpringJUnitConfig
@ContextConfiguration(classes = com.kb.tangtang.config.RootConfig.class)
class CardMapperTest {

    @Autowired
    private CardMapper cardMapper;

    @Test
    void insertThenUpdate() {
        Card card = Card.builder()
                .userId(1L)
                .institutionCode("0004")
                .institutionName("KB국민은행")
                .cardNoMasked("9490-****-****-2201")
                .productName("KB 챌린지 신용카드")
                .cardProductCode("KB-CH-CREDIT")
                .cardTypeCode("01")
                .cardStatusCode("01")
                .currency("KRW")
                .lastSyncAt(LocalDateTime.now())
                .build();

        int updated = cardMapper.update(card);
        assertEquals(0, updated, "신규 카드는 update 대상이 없어야 한다");

        int inserted = cardMapper.insert(card);
        assertEquals(1, inserted);

        int updatedAgain = cardMapper.update(card);
        assertEquals(1, updatedAgain, "같은 카드번호로 다시 update 하면 1행이어야 한다");
    }
}
