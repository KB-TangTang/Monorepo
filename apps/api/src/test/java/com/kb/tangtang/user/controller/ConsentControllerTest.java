package com.kb.tangtang.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.common.exception.CommonExceptionAdvice;
import com.kb.tangtang.user.domain.ConsentScope;
import com.kb.tangtang.user.dto.MyConsentDto;
import com.kb.tangtang.user.service.ConsentCatalog;
import com.kb.tangtang.user.service.ConsentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ConsentControllerTest {

    @Mock private ConsentService consentService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * standaloneSetup 에는 LoginUserArgumentResolver 가 없다.
     * 인터셉터도 타지 않으므로 @LoginUser 자리에 고정 사용자 ID 를 넣어주는 리졸버를 직접 단다.
     */
    @BeforeEach
    void setUp() {
        ConsentCatalog catalog = new ConsentCatalog("v1.0", "u/terms", "u/privacy",
                "u/financial", "u/third", "u/ai", "u/marketing");
        ConsentController controller = new ConsentController(consentService, catalog);

        HandlerMethodArgumentResolver loginUser = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(org.springframework.core.MethodParameter parameter) {
                return parameter.hasParameterAnnotation(com.kb.tangtang.common.auth.LoginUser.class);
            }
            @Override
            public Object resolveArgument(org.springframework.core.MethodParameter parameter,
                                          ModelAndViewContainer mavContainer,
                                          org.springframework.web.context.request.NativeWebRequest webRequest,
                                          org.springframework.web.bind.support.WebDataBinderFactory binderFactory) {
                return 7L;
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(loginUser)
                .setControllerAdvice(new CommonExceptionAdvice())
                .build();
    }

    @Test
    @DisplayName("카탈로그는 항목 목록과 약관 버전을 공통 래퍼로 돌려준다")
    void catalog() throws Exception {
        mockMvc.perform(get("/api/consents/catalog").param("scope", "SIGNUP"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.termsVersion").value("v1.0"))
                .andExpect(jsonPath("$.data.items.length()").value(5))
                .andExpect(jsonPath("$.data.items[0].type").value("TERMS"))
                .andExpect(jsonPath("$.data.items[0].termsUrl").value("u/terms"));
    }

    @Test
    @DisplayName("알 수 없는 scope 는 400 으로 떨어진다")
    void catalogInvalidScope() throws Exception {
        mockMvc.perform(get("/api/consents/catalog").param("scope", "NOPE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("CONSENT_TYPE_INVALID"));
    }

    @Test
    @DisplayName("저장은 로그인 사용자 ID 로 서비스를 부르고 needsConsent 를 돌려준다")
    void submit() throws Exception {
        when(consentService.submit(eq(7L), eq(ConsentScope.SIGNUP), any())).thenReturn(false);

        String body = objectMapper.writeValueAsString(Map.of(
                "scope", "SIGNUP",
                "agreements", List.of(
                        Map.of("type", "TERMS", "agreed", true),
                        Map.of("type", "PRIVACY", "agreed", true),
                        Map.of("type", "FINANCIAL_DATA", "agreed", true))));

        mockMvc.perform(post("/api/consents").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.needsConsent").value(false));
    }

    @Test
    @DisplayName("필수 미동의는 400 + 코드로 내려간다")
    void submitRequiredMissing() throws Exception {
        when(consentService.submit(anyLong(), any(), any()))
                .thenThrow(new BusinessException("CONSENT_REQUIRED_MISSING", "필수 항목에 동의해야 합니다."));

        String body = objectMapper.writeValueAsString(Map.of(
                "scope", "SIGNUP",
                "agreements", List.of(Map.of("type", "TERMS", "agreed", true))));

        mockMvc.perform(post("/api/consents").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CONSENT_REQUIRED_MISSING"));
    }

    @Test
    @DisplayName("내 동의 현황을 돌려준다")
    void myConsents() throws Exception {
        when(consentService.myConsents(7L)).thenReturn(List.of(
                MyConsentDto.builder().type("TERMS").required(true).label("서비스 이용약관")
                        .termsUrl("u/terms").agreed(true).withdrawable(false).termsVersion("v1.0").build()));

        mockMvc.perform(get("/api/consents/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].type").value("TERMS"))
                .andExpect(jsonPath("$.data.items[0].agreed").value(true))
                .andExpect(jsonPath("$.data.items[0].withdrawable").value(false));
    }

    @Test
    @DisplayName("철회는 needsConsent 를 돌려준다")
    void withdraw() throws Exception {
        when(consentService.withdraw(7L, "FINANCIAL_DATA")).thenReturn(true);

        mockMvc.perform(post("/api/consents/FINANCIAL_DATA/withdraw"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.needsConsent").value(true));
    }
}
