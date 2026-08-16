package com.kb.tangtang.challenge.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kb.tangtang.challenge.dto.ChallengeGroupCreateRequestDto;
import com.kb.tangtang.challenge.dto.ChallengeGroupCreatedDto;
import com.kb.tangtang.challenge.dto.ChallengeGroupDto;
import com.kb.tangtang.challenge.dto.GroupMemberDto;
import com.kb.tangtang.challenge.dto.InviteCodePreviewDto;
import com.kb.tangtang.challenge.service.ChallengeGroupService;
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

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ChallengeGroupControllerTest {

    private static final long USER_ID = 1L;

    private final StubService stubService = new StubService();

    private MockMvc mockMvc() {
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return MockMvcBuilders
                .standaloneSetup(new ChallengeGroupController(stubService))
                .setControllerAdvice(new CommonExceptionAdvice())
                .setCustomArgumentResolvers(loginUserResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    @DisplayName("생성 응답은 groupId 와 초대 코드를 준다")
    void create() throws Exception {
        mockMvc().perform(post("/api/group-challenges")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"groupName\":\"커피값 줄이기\",\"limitAmount\":0,"
                                + "\"evalType\":\"DAILY\",\"startDate\":\"2026-08-12\","
                                + "\"endDate\":\"2026-08-14\",\"memo\":\"꼴찌가 커피 쏘기\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.groupId").value(7))
                .andExpect(jsonPath("$.data.inviteCode").value("AB2C3"));

        assertEquals("커피값 줄이기", stubService.lastCreateRequest.getGroupName());
        assertEquals(0, stubService.lastCreateRequest.getLimitAmount(),
                "0원이 그대로 서비스까지 도달해야 한다 — 무지출 챌린지");
        assertEquals("꼴찌가 커피 쏘기", stubService.lastCreateRequest.getMemo());
    }

    @Test
    @DisplayName("목록의 날짜는 ISO 문자열로, 파생값은 camelCase 로 내려간다")
    void findMyGroups() throws Exception {
        mockMvc().perform(get("/api/group-challenges"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].groupName").value("커피값 줄이기"))
                .andExpect(jsonPath("$.data[0].startDate").value("2026-08-12"))
                .andExpect(jsonPath("$.data[0].totalDays").value(3))
                .andExpect(jsonPath("$.data[0].maxLives").value(3))
                .andExpect(jsonPath("$.data[0].currentDay").value(1))
                .andExpect(jsonPath("$.data[0].memberCount").value(1))
                .andExpect(jsonPath("$.data[0].owner").value(true))
                .andExpect(jsonPath("$.data[0].members[0].nickname").value("절약왕"))
                .andExpect(jsonPath("$.data[0].pendingTrialCount").value(0));
    }

    @Test
    @DisplayName("status 는 콤마로 여러 개를 넘길 수 있다 — 「종료됨」 탭이 두 상태를 함께 본다")
    void findMyGroupsWithMultipleStatuses() throws Exception {
        mockMvc().perform(get("/api/group-challenges").param("status", "JUDGING,CLOSED"))
                .andExpect(status().isOk());

        assertEquals(List.of("JUDGING", "CLOSED"), stubService.lastStatuses);
    }

    @Test
    @DisplayName("참여할 수 없는 초대 코드도 200 이고 사유가 함께 온다")
    void previewInviteCode() throws Exception {
        mockMvc().perform(get("/api/group-challenges/invite-codes/AB2C3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.joinable").value(false))
                .andExpect(jsonPath("$.data.reason").value("FULL"))
                .andExpect(jsonPath("$.data.challenge.groupName").value("커피값 줄이기"))
                .andExpect(jsonPath("$.data.challenge.memo").value("꼴찌가 커피 쏘기"));

        assertEquals("AB2C3", stubService.lastInviteCode);
    }

    @Test
    @DisplayName("업무 규칙 위반은 400 + code 로 내려간다")
    void joinBlocked() throws Exception {
        stubService.joinFailure = new BusinessException("GROUP_FULL", "정원이 가득 찼습니다.");

        mockMvc().perform(post("/api/group-challenges/7/members"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("GROUP_FULL"))
                .andExpect(jsonPath("$.message").value("정원이 가득 찼습니다."));
    }

    /* ══ 스텁 ══════════════════════════════════════════════ */

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

    private static ChallengeGroupDto group() {
        return ChallengeGroupDto.builder()
                .id(7L)
                .adminId(USER_ID)
                .groupName("커피값 줄이기")
                .limitAmount(0)
                .evalType("DAILY")
                .maxMembers(6)
                .startDate(LocalDate.of(2026, 8, 12))
                .endDate(LocalDate.of(2026, 8, 14))
                .inviteCode("AB2C3")
                .status("ACTIVE")
                .memo("꼴찌가 커피 쏘기")
                .livesCount(3)
                .totalDays(3)
                .currentDay(1)
                .maxLives(3)
                .memberCount(1)
                .owner(true)
                .member(true)
                .joinable(false)
                .members(List.of(GroupMemberDto.builder()
                        .userId(USER_ID)
                        .nickname("절약왕")
                        .owner(true)
                        .build()))
                .build();
    }

    private static class StubService extends ChallengeGroupService {
        private ChallengeGroupCreateRequestDto lastCreateRequest;
        private List<String> lastStatuses;
        private String lastInviteCode;
        private BusinessException joinFailure;

        StubService() {
            super(null, null, null, null);
        }

        @Override
        public ChallengeGroupCreatedDto create(long userId, ChallengeGroupCreateRequestDto request) {
            this.lastCreateRequest = request;
            return ChallengeGroupCreatedDto.builder().groupId(7L).inviteCode("AB2C3").build();
        }

        @Override
        public List<ChallengeGroupDto> findMyGroups(long userId, List<String> statuses) {
            this.lastStatuses = statuses;
            return List.of(group());
        }

        @Override
        public ChallengeGroupDto findDetail(long userId, long groupId) {
            return group();
        }

        @Override
        public InviteCodePreviewDto previewInviteCode(long userId, String inviteCode) {
            this.lastInviteCode = inviteCode;
            return InviteCodePreviewDto.builder()
                    .challenge(findDetail(userId, 7L))
                    .joinable(false)
                    .reason("FULL")
                    .build();
        }

        @Override
        public ChallengeGroupDto join(long userId, long groupId) {
            if (joinFailure != null) {
                throw joinFailure;
            }
            return findDetail(userId, groupId);
        }
    }
}
