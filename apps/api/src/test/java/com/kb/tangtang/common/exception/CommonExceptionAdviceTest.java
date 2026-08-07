package com.kb.tangtang.common.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 전역 예외 처리 검증.
 *
 * 매핑되지 않은 경로는 WebConfig 가 throwExceptionIfNoHandlerFound=true 를 켜 둔 덕에
 * NoHandlerFoundException 으로 올라온다. 그 예외가 handleAll(Exception) 이 아니라
 * handle404 로 잡혀 404/NOT_FOUND 를 주는지 확인한다.
 */
class CommonExceptionAdviceTest {

    @Controller
    static class DummyController {
        @GetMapping("/api/dummy")
        @ResponseBody
        String dummy() {
            return "ok";
        }
    }

    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new DummyController())
            .setControllerAdvice(new CommonExceptionAdvice())
            // WebConfig#customizeRegistration 과 같은 조건을 만든다
            .addDispatcherServletCustomizer(ds -> ds.setThrowExceptionIfNoHandlerFound(true))
            .build();

    @Test
    @DisplayName("매핑되지 않은 경로는 404 NOT_FOUND 를 준다 (500 이 아니다)")
    void unmappedPathReturns404() throws Exception {
        mockMvc.perform(get("/api/accounts/nonexistent-xyz"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }
}
