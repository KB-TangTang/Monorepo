package com.kb.tangtang.common.controller;

import com.kb.tangtang.common.docs.HealthControllerDocs;
import com.kb.tangtang.common.dto.ApiResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 서버 기동 확인용. 프론트 프록시 설정 검증에도 사용한다.
 * GET /api/health
 */
@RestController
@RequestMapping("/api")
@Log4j2
public class HealthController implements HealthControllerDocs {

    @GetMapping("/health")
    public ApiResponse<Map<String, String>> health() {
        log.info("health check");
        Map<String, String> body = new LinkedHashMap<>();
        body.put("status", "UP");
        body.put("service", "tangtang-api");
        return ApiResponse.ok(body);
    }
}
