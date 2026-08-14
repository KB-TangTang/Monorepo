package com.kb.tangtang.common.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

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

        /* WebConfig.MAX_FILE_SIZE(10MB) 초과 시 서블릿 멀티파트 파서가 실제로 던지는 예외 */
        @GetMapping("/api/dummy/too-large")
        @ResponseBody
        String tooLarge() {
            throw new MaxUploadSizeExceededException(10 * 1024 * 1024L);
        }

        /* 그 밖의 멀티파트 파싱 실패 — 예: file 파트 누락 */
        @GetMapping("/api/dummy/ai-in-progress")
        @ResponseBody
        String aiAnalysisInProgress() {
            throw new BusinessException("AI_ANALYSIS_IN_PROGRESS", "AI analysis is already in progress.",
                    HttpStatus.CONFLICT);
        }

        @GetMapping("/api/dummy/multipart-broken")
        @ResponseBody
        String multipartBroken() {
            throw new MultipartException("파트 파싱 실패");
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

    @Test
    @DisplayName("업로드 용량 초과는 500 이 아니라 400 IMAGE_TOO_LARGE 를 준다")
    void maxUploadSizeExceededReturns400() throws Exception {
        mockMvc.perform(get("/api/dummy/too-large"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("IMAGE_TOO_LARGE"))
                .andExpect(jsonPath("$.message").value("5MB 이하 이미지만 올릴 수 있어요."));
    }

    @Test
    void businessExceptionUsesItsAssignedHttpStatus() throws Exception {
        mockMvc.perform(get("/api/dummy/ai-in-progress"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("AI_ANALYSIS_IN_PROGRESS"));
    }

    @Test
    @DisplayName("그 밖의 멀티파트 파싱 실패(파트 누락 등)는 400 IMAGE_REQUIRED 를 준다")
    void otherMultipartExceptionReturns400() throws Exception {
        mockMvc.perform(get("/api/dummy/multipart-broken"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("IMAGE_REQUIRED"))
                .andExpect(jsonPath("$.message").value("올릴 이미지를 선택해주세요."));
    }
}
