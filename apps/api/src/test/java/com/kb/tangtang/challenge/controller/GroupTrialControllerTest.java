package com.kb.tangtang.challenge.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kb.tangtang.challenge.dto.GroupTrialDetailDto;
import com.kb.tangtang.challenge.dto.TrialAccusedDto;
import com.kb.tangtang.challenge.dto.TrialVoteCommentDto;
import com.kb.tangtang.challenge.service.DefenseService;
import com.kb.tangtang.challenge.service.GroupTrialService;
import com.kb.tangtang.challenge.service.VoteService;
import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.common.exception.CommonExceptionAdvice;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 재판 상세 · 투표 (이슈 #171).
 *
 * <p>검증 규칙 자체는 {@code VoteServiceTest} 가 본다. 여기서는 <b>서비스가 만들 수 없는 것</b>
 * — 요청 본문이 서비스까지 그대로 닿는지, 업무 오류가 400 + {@code code} 로 나가는지,
 * 그리고 <b>가려진 값이 0 이 아니라 JSON null 로 나가는지</b> — 만 본다.
 */
class GroupTrialControllerTest {

    private static final long USER_ID = 1L;
    private static final long INDICTMENT_ID = 11L;

    private final StubTrialService stubTrialService = new StubTrialService();
    private final StubVoteService stubVoteService = new StubVoteService();

    private MockMvc mockMvc() {
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return MockMvcBuilders
                .standaloneSetup(new GroupTrialController(stubTrialService, new StubDefenseService(),
                        stubVoteService, null))
                .setControllerAdvice(new CommonExceptionAdvice())
                .setCustomArgumentResolvers(loginUserResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    // ── 투표 ──────────────────────────────────────────────────────────────

    /**
     * 응답 본문이 없다. 던진 뒤 화면은 재판 상세를 다시 읽어 그린다 — 여기서 표수를 돌려주면
     * 「개표 전 비율 비공개」 정책을 이 한 곳에서 뚫게 된다.
     */
    @Test
    @DisplayName("투표는 verdict 와 코멘트를 그대로 서비스에 넘기고 본문 없이 200 을 준다")
    void vote() throws Exception {
        mockMvc().perform(post("/api/group-challenges/trials/11/votes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"verdict\":\"INNOCENT\",\"comment\":\"사정이 있어 보여요\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                /* ApiResponse 가 NON_NULL 이라 data 키 자체가 없다 */
                .andExpect(jsonPath("$.data").doesNotExist());

        assertEquals(USER_ID, stubVoteService.lastUserId);
        assertEquals(INDICTMENT_ID, stubVoteService.lastIndictmentId);
        assertEquals("INNOCENT", stubVoteService.lastVerdict);
        assertEquals("사정이 있어 보여요", stubVoteService.lastComment);
    }

    /** 코멘트는 선택이다. 키가 아예 없어도 400 이 나면 안 된다. */
    @Test
    @DisplayName("코멘트 없이 verdict 만 보내도 통과한다")
    void voteWithoutComment() throws Exception {
        mockMvc().perform(post("/api/group-challenges/trials/11/votes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"verdict\":\"GUILTY\"}"))
                .andExpect(status().isOk());

        assertEquals("GUILTY", stubVoteService.lastVerdict);
        assertNull(stubVoteService.lastComment);
    }

    /**
     * {@code verdict} 를 enum 으로 받으면 Jackson 이 먼저 죽어 우리 코드가 아니라
     * 역직렬화 오류가 나간다 — 화면이 {@code INVALID_VERDICT} 로 분기할 수 없다.
     * DTO 가 String 인 이유이므로 알 수 없는 값도 서비스까지 닿아야 한다.
     */
    @Test
    @DisplayName("알 수 없는 verdict 도 서비스까지 닿아 INVALID_VERDICT 로 나간다")
    void voteWithUnknownVerdictReachesService() throws Exception {
        stubVoteService.failure = new BusinessException("INVALID_VERDICT", "유죄 또는 무죄를 선택해주세요.");

        mockMvc().perform(post("/api/group-challenges/trials/11/votes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"verdict\":\"ABSTAIN\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("INVALID_VERDICT"));

        assertEquals("ABSTAIN", stubVoteService.lastVerdict);
    }

    @Test
    @DisplayName("업무 규칙 위반은 400 + code 로 내려간다")
    void voteBlocked() throws Exception {
        stubVoteService.failure = new BusinessException("VOTE_ALREADY_EXISTS", "이미 투표했습니다.");

        mockMvc().perform(post("/api/group-challenges/trials/11/votes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"verdict\":\"GUILTY\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VOTE_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.message").value("이미 투표했습니다."));
    }

    // ── 재판 상세의 노출 정책 ────────────────────────────────────────────

    /**
     * <b>가린 값은 0 이 아니라 null 로 나가야 한다.</b> DTO 를 {@code int} 로 되돌리면
     * 컴파일은 통과하고 화면에는 「0 : 0」 이 그려진다 — 투표 중인 재판이 만장일치 0표처럼 보인다.
     */
    @Test
    @DisplayName("투표 중에는 표 분포와 코멘트가 JSON null 로 내려간다")
    void votingHidesDistribution() throws Exception {
        stubTrialService.detail = detail("VOTING", null, null, null);

        mockMvc().perform(get("/api/group-challenges/trials/11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("VOTING"))
                /* 진행 상황(몇 명이 던졌나)까지만 공개한다 */
                .andExpect(jsonPath("$.data.voteCount").value(3))
                .andExpect(jsonPath("$.data.totalVoters").value(5))
                .andExpect(jsonPath("$.data.guiltyCount").value(nullValue()))
                .andExpect(jsonPath("$.data.innocentCount").value(nullValue()))
                .andExpect(jsonPath("$.data.comments").value(nullValue()));
    }

    /**
     * 개표 후에도 <b>투표자를 특정할 수 있는 값은 함께 내려가지 않는다.</b>
     * 코멘트 항목에 {@code userId} · {@code nickname} · {@code verdict} 가 생기면
     * 익명 코멘트가 곧 표 공개가 된다.
     */
    @Test
    @DisplayName("개표 후 코멘트는 문장과 시각만 담는다")
    void verdictRevealsAnonymousComments() throws Exception {
        stubTrialService.detail = detail("GUILTY", 4, 1,
                List.of(TrialVoteCommentDto.builder()
                        .comment("이번엔 좀 심했어")
                        .createdAt(LocalDateTime.of(2026, 8, 19, 10, 0))
                        .build()));

        mockMvc().perform(get("/api/group-challenges/trials/11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.guiltyCount").value(4))
                .andExpect(jsonPath("$.data.innocentCount").value(1))
                .andExpect(jsonPath("$.data.comments[0].comment").value("이번엔 좀 심했어"))
                .andExpect(jsonPath("$.data.comments[0].createdAt").value("2026-08-19T10:00:00"))
                .andExpect(jsonPath("$.data.comments[0].userId").doesNotExist())
                .andExpect(jsonPath("$.data.comments[0].nickname").doesNotExist())
                .andExpect(jsonPath("$.data.comments[0].verdict").doesNotExist());
    }

    /* ══ 스텁 ══════════════════════════════════════════════ */

    private static GroupTrialDetailDto detail(String status, Integer guiltyCount,
                                              Integer innocentCount,
                                              List<TrialVoteCommentDto> comments) {
        return GroupTrialDetailDto.builder()
                .indictmentId(INDICTMENT_ID)
                .groupId(3L)
                .groupName("배달 소비 줄이기")
                .status(status)
                .accused(TrialAccusedDto.builder()
                        .userId(9L)
                        .nickname("지판")
                        .mine(false)
                        .build())
                .voteCount(3)
                .totalVoters(5)
                .guiltyCount(guiltyCount)
                .innocentCount(innocentCount)
                .comments(comments)
                .build();
    }

    private HandlerMethodArgumentResolver loginUserResolver() {
        return new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(com.kb.tangtang.common.auth.LoginUser.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return USER_ID;
            }
        };
    }

    private static class StubTrialService extends GroupTrialService {
        private GroupTrialDetailDto detail;

        StubTrialService() {
            super(null, null, null, null, null, 6, 24);
        }

        @Override
        public GroupTrialDetailDto findTrialDetail(long userId, long indictmentId) {
            return detail;
        }
    }

    private static class StubDefenseService extends DefenseService {
        StubDefenseService() {
            super(null, null, null, null, null, null, 6);
        }
    }

    private static class StubVoteService extends VoteService {
        private long lastUserId;
        private long lastIndictmentId;
        private String lastVerdict;
        private String lastComment;
        private BusinessException failure;

        StubVoteService() {
            super(null, null, 6, 24);
        }

        @Override
        public void vote(long userId, long indictmentId, String verdict, String comment) {
            this.lastUserId = userId;
            this.lastIndictmentId = indictmentId;
            this.lastVerdict = verdict;
            this.lastComment = comment;
            if (failure != null) {
                throw failure;
            }
        }
    }
}
