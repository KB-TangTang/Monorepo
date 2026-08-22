package com.kb.tangtang.challenge.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kb.tangtang.challenge.domain.IndictmentStatus;
import com.kb.tangtang.challenge.domain.VerdictDecision;
import com.kb.tangtang.challenge.domain.VerdictMethod;
import com.kb.tangtang.challenge.domain.VerdictTallyRow;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * 판사 탕이 호출 (이슈 #172).
 *
 * <p>여기서 지키는 것은 셋이다 — <b>개인 정보를 보내지 않는가</b>, <b>사유가 300자를 넘지 않는가</b>
 * ({@code ai_verdict_reason VARCHAR(300)} 을 넘기면 UPDATE 가 죽는다), <b>이상한 응답을 판결로
 * 받아들이지 않는가</b>. 마지막 항목이 중요한 이유는 여기서 던져야 배치가 대체 판결로 떨어지기
 * 때문이다 — 조용히 통과시키면 결론 없는 값이 DB CHECK 까지 내려간다.
 */
class OpenAiTangiVerdictClientTest {

    private static final String RESPONSES_URL = "https://api.openai.test/v1/responses";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockRestServiceServer server;
    private OpenAiTangiVerdictClient client;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.createServer(restTemplate);
        client = new OpenAiTangiVerdictClient(restTemplate, objectMapper, "test-key",
                "gpt-5-nano", RESPONSES_URL, 1024, "low");
    }

    private static VerdictTallyRow tie() {
        VerdictTallyRow row = new VerdictTallyRow();
        row.setIndictmentId(1L);
        row.setGroupId(2L);
        row.setUserId(3L);
        row.setNickname("탕구리");
        row.setEvalType("DAILY");
        row.setCategoryName("식비");
        row.setLimitAmount(new BigDecimal("20000"));
        row.setCurrentAmount(new BigDecimal("56000"));
        row.setExceededAmount(new BigDecimal("36000"));
        row.setMessage("식비 한도를 36,000원 초과했어요");
        row.setDefenseContent("팀 회식비를 제가 결제하고 나중에 정산받기로 했어요");
        row.setActualBurdenAmount(new BigDecimal("14000"));
        row.setDeductionAmount(new BigDecimal("42000"));
        row.setGuiltyCount(2);
        row.setInnocentCount(2);
        row.setTotalVoters(4);
        return row;
    }

    private String responseWith(String verdict, String reason) throws Exception {
        String output = objectMapper.writeValueAsString(
                objectMapper.createObjectNode().put("verdict", verdict).put("reason", reason));
        return "{\"output\":[{\"type\":\"message\",\"content\":[{\"type\":\"output_text\",\"text\":"
                + objectMapper.writeValueAsString(output) + "}]}]}";
    }

    /**
     * 요청에 <b>닉네임·식별자가 들어가면 안 된다.</b> 판결에 필요하지 않은데다 외부 모델로
     * 나가는 값이다. 표 분포도 뺀다 — 「2:2 였다」를 알려 주면 사건이 아니라 표를 근거로 삼는다.
     */
    @Test
    @DisplayName("사건 개요와 변론만 보내고 식별 정보·표 분포는 보내지 않는다")
    void sendsCaseSummaryWithoutIdentity() throws Exception {
        server.expect(once(), requestTo(RESPONSES_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-key"))
                .andExpect(content().string(Matchers.containsString("\"model\":\"gpt-5-nano\"")))
                .andExpect(content().string(Matchers.containsString("\"store\":false")))
                .andExpect(content().string(Matchers.containsString("\"type\":\"json_schema\"")))
                .andExpect(content().string(Matchers.containsString("\"strict\":true")))
                .andExpect(content().string(Matchers.containsString("\\\"exceededAmount\\\":36000")))
                .andExpect(content().string(Matchers.containsString("\\\"actualBurdenAmount\\\":14000")))
                .andExpect(content().string(Matchers.not(Matchers.containsString("탕구리"))))
                .andExpect(content().string(Matchers.not(Matchers.containsString("userId"))))
                .andExpect(content().string(Matchers.not(Matchers.containsString("indictmentId"))))
                .andExpect(content().string(Matchers.not(Matchers.containsString("guiltyCount"))))
                .andRespond(withSuccess(responseWith("INNOCENT", "정산받기로 한 회식비라 무죄로 봐요."),
                        MediaType.APPLICATION_JSON));

        VerdictDecision decision = client.judge(tie());

        assertEquals(IndictmentStatus.INNOCENT, decision.getStatus());
        assertEquals(VerdictMethod.AI_JUDGMENT, decision.getMethod());
        assertEquals("정산받기로 한 회식비라 무죄로 봐요.", decision.getAiReason());
        server.verify();
    }

    /** 변론 없이 마감돼 투표로 넘어간 재판. 변론 항목이 통째로 비어도 호출은 성립해야 한다. */
    @Test
    @DisplayName("변론이 없어도 기소만으로 판결을 요청한다")
    void judgesWithoutDefense() throws Exception {
        VerdictTallyRow row = tie();
        row.setDefenseContent(null);
        row.setActualBurdenAmount(null);
        row.setDeductionAmount(null);

        server.expect(once(), requestTo(RESPONSES_URL))
                .andExpect(content().string(Matchers.containsString("\\\"defense\\\":null")))
                .andRespond(withSuccess(responseWith("GUILTY", "설명이 없어 초과 소비로 봤어요."),
                        MediaType.APPLICATION_JSON));

        assertEquals(IndictmentStatus.GUILTY, client.judge(row).getStatus());
        server.verify();
    }

    /**
     * {@code ai_verdict_reason} 은 {@code VARCHAR(300)} 이다. 스키마에 {@code maxLength} 를 걸어도
     * 모델이 넘길 수 있고, 그대로 UPDATE 하면 <b>판결 확정이 통째로 실패해 재판이 끝나지 않는다.</b>
     */
    @Test
    @DisplayName("300자를 넘는 사유는 잘라서 돌려준다")
    void truncatesOverlongReason() throws Exception {
        String tooLong = "가".repeat(400);
        server.expect(once(), requestTo(RESPONSES_URL))
                .andRespond(withSuccess(responseWith("GUILTY", tooLong), MediaType.APPLICATION_JSON));

        assertEquals(VerdictDecision.AI_REASON_MAX_LENGTH, client.judge(tie()).getAiReason().length());
    }

    /** 줄바꿈이 그대로 내려오면 채팅·알림 문구가 여러 줄로 깨진다. */
    @Test
    @DisplayName("사유의 줄바꿈은 공백 한 칸으로 눌러 담는다")
    void collapsesWhitespaceInReason() throws Exception {
        server.expect(once(), requestTo(RESPONSES_URL))
                .andRespond(withSuccess(responseWith("INNOCENT", "병원비예요.\n\n어쩔 수 없었어요."),
                        MediaType.APPLICATION_JSON));

        String reason = client.judge(tie()).getAiReason();

        assertEquals("병원비예요. 어쩔 수 없었어요.", reason);
        assertFalse(reason.contains("\n"));
    }

    /**
     * 아래 셋은 전부 예외로 올라와야 한다. 여기서 삼키면 배치가 대체 판결로 떨어지지 못하고
     * 결론 없는 값이 DB CHECK 까지 내려간다.
     */
    @Test
    @DisplayName("HTTP 오류는 예외로 올린다")
    void httpErrorThrows() {
        server.expect(once(), requestTo(RESPONSES_URL)).andRespond(withServerError());

        assertThrows(RuntimeException.class, () -> client.judge(tie()));
    }

    @Test
    @DisplayName("판결문이 없는 응답은 예외로 올린다")
    void emptyOutputThrows() {
        server.expect(once(), requestTo(RESPONSES_URL))
                .andRespond(withSuccess("{\"output\":[]}", MediaType.APPLICATION_JSON));

        assertThrows(RuntimeException.class, () -> client.judge(tie()));
    }

    @Test
    @DisplayName("모르는 판결값은 예외로 올린다")
    void unknownVerdictThrows() throws Exception {
        server.expect(once(), requestTo(RESPONSES_URL))
                .andRespond(withSuccess(responseWith("MAYBE", "잘 모르겠어요."), MediaType.APPLICATION_JSON));

        assertThrows(RuntimeException.class, () -> client.judge(tie()));
    }

    /**
     * 키가 없으면 <b>HTTP 를 아예 던지지 않는다.</b> 401 을 받으려고 왕복하면 재판마다 타임아웃
     * 시간을 버린다 — 키 설정을 빠뜨린 로컬 환경에서 개표 배치가 매 틱 느려진다.
     */
    @Test
    @DisplayName("API 키가 없으면 호출하지 않고 바로 실패한다")
    void missingApiKeySkipsCall() {
        OpenAiTangiVerdictClient noKey = new OpenAiTangiVerdictClient(
                new RestTemplate(), objectMapper, "  ", "gpt-5-nano", RESPONSES_URL, 1024, "low");

        assertThrows(IllegalStateException.class, () -> noKey.judge(tie()));
    }
}
