package com.kb.tangtang.config;

import com.kb.tangtang.common.docs.SwaggerAccessInterceptor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Swagger 문서가 <b>URL 수준에서</b> 실제로 잠기는지 고정한다.
 *
 * <p>{@code SwaggerAccessInterceptorTest} 는 {@code preHandle} 을 직접 호출해 인증 판단만 본다.
 * 그래서 <b>인터셉터가 그 URL 에 걸리기는 하는가</b>는 검증 범위 밖이었고, 실제로
 * {@code /v2/api-docs/} (끝 슬래시 하나)가 401 이 아니라 <b>200 으로 명세 전문을 뱉었다</b>
 * — 핸들러 매핑은 끝 슬래시를 받아주는데 인터셉터 패턴은 정확 매칭이라 안 걸린 탓이다.
 *
 * <p>이 테스트는 {@link ServletConfig#SWAGGER_PATH_PATTERNS} 와
 * {@link ServletConfig#configurePathMatch} 를 <b>그대로 가져다 쓴다.</b> 운영 설정을 되돌리면
 * 여기가 깨진다. ServletConfig 전체는 DB·JWT 등 루트 컨텍스트를 요구하므로 띄우지 않고,
 * springfox 2.9.2 와 같은 매핑을 가진 스텁 컨트롤러로 최소 재현한다.
 */
class SwaggerAccessPathTest {

    private static final String CREDENTIALS = "Basic " + Base64.getEncoder()
            .encodeToString("tangtang:secret".getBytes());

    /** 무인증으로 두드려 보는 URL. 끝 슬래시·상위 경로가 핵심이다. */
    private static final String[] HOSTILE_URIS = {
            "/v2/api-docs",
            "/v2/api-docs/",          // 실제로 뚫렸던 URL
            "/v2/api-docs//",
            "/swagger-resources",
            "/swagger-resources/",
            "/swagger-resources/configuration/ui",
            "/swagger-ui.html",
            "/swagger-ui.html/",
            "/configuration",
            "/configuration/ui",
            "/webjars",
            "/webjars/springfox-swagger-ui/swagger-ui.min.js"};

    /** springfox 2.9.2 Swagger2Controller · ApiResourceController 와 같은 매핑. */
    @RestController
    static class SpringfoxStub {

        @GetMapping(value = "/v2/api-docs", produces = MediaType.APPLICATION_JSON_VALUE)
        String apiDocs() {
            return "{\"swagger\":\"2.0\",\"LEAKED_SPEC\":true}";
        }

        @GetMapping(value = "/swagger-resources", produces = MediaType.APPLICATION_JSON_VALUE)
        String resources() {
            return "[{\"name\":\"LEAKED_GROUPS\"}]";
        }
    }

    @EnableWebMvc
    static class Cfg implements WebMvcConfigurer {

        /** 운영 설정의 메서드를 그대로 호출한다 — 규칙을 여기에 복사하지 않는다. */
        private final ServletConfig production = new ServletConfig();

        @Bean
        SwaggerAccessInterceptor interceptor() {
            return new SwaggerAccessInterceptor("docker", "tangtang", "secret");
        }

        @Bean
        SpringfoxStub springfoxStub() {
            return new SpringfoxStub();
        }

        @Override
        public void addInterceptors(InterceptorRegistry registry) {
            registry.addInterceptor(interceptor())
                    .addPathPatterns(ServletConfig.SWAGGER_PATH_PATTERNS);
        }

        @Override
        public void configurePathMatch(PathMatchConfigurer configurer) {
            production.configurePathMatch(configurer);
        }
    }

    private static MockMvc mvc() {
        AnnotationConfigWebApplicationContext ctx = new AnnotationConfigWebApplicationContext();
        ctx.setServletContext(new MockServletContext());
        ctx.register(Cfg.class);
        ctx.refresh();
        return MockMvcBuilders.webAppContextSetup((WebApplicationContext) ctx).build();
    }

    @Test
    @DisplayName("무인증 접근은 어떤 변형 URL 로도 문서 본문을 주지 않는다")
    void 무인증_접근은_본문을_주지_않는다() throws Exception {
        MockMvc mvc = mvc();
        for (String uri : HOSTILE_URIS) {
            MockHttpServletResponse response = mvc.perform(get(uri)).andReturn().getResponse();
            String body = response.getContentAsString();

            assertNotEquals(200, response.getStatus(),
                    uri + " — 200 이면 인증이 통째로 우회된 것이다");
            assertFalse(body.contains("LEAKED_SPEC") || body.contains("LEAKED_GROUPS"),
                    uri + " — 응답 본문에 문서가 실려 나갔다: " + body);
        }
    }

    @Test
    @DisplayName("끝 슬래시 URL 은 핸들러까지 닿지 못한다 (404) — 근본 수정 확인")
    void 끝_슬래시는_매핑되지_않는다() throws Exception {
        assertEquals(404, mvc().perform(get("/v2/api-docs/")).andReturn().getResponse().getStatus());
    }

    @Test
    @DisplayName("올바른 자격증명이면 문서는 정상적으로 열린다")
    void 정상_자격증명은_통과한다() throws Exception {
        MockMvc mvc = mvc();
        for (String uri : new String[]{"/v2/api-docs", "/swagger-resources"}) {
            assertEquals(200,
                    mvc.perform(get(uri).header("Authorization", CREDENTIALS))
                            .andReturn().getResponse().getStatus(),
                    uri + " — 잠그려다 정상 사용까지 막으면 안 된다");
        }
    }

    @Test
    @DisplayName("모든 Swagger 패턴에는 '/**' 짝이 있다 — 정확 매칭만 남으면 다시 뚫린다")
    void 모든_패턴에_와일드카드_짝이_있다() {
        for (String pattern : ServletConfig.SWAGGER_PATH_PATTERNS) {
            if (pattern.endsWith("/**")) {
                continue;
            }
            boolean paired = false;
            for (String candidate : ServletConfig.SWAGGER_PATH_PATTERNS) {
                if (candidate.equals(pattern + "/**")) {
                    paired = true;
                    break;
                }
            }
            assertTrue(paired, pattern + " 의 '/**' 짝이 없다");
        }
    }
}
