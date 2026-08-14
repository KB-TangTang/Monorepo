package com.kb.tangtang.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kb.tangtang.common.auth.JwtAuthInterceptor;
import com.kb.tangtang.common.auth.LoginUserArgumentResolver;
import com.kb.tangtang.common.storage.ImageStorageProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * 서블릿(웹) 컨텍스트 설정.
 * 이 프로젝트는 Vue3 SPA + REST API 구조다. JSP / 뷰 리졸버는 사용하지 않는다.
 * 응답은 전부 JSON 이며 공통 래퍼(ApiResponse)를 사용한다.
 */
@EnableWebMvc
@ComponentScan(
        basePackages = "com.kb.tangtang",
        useDefaultFilters = false,
        includeFilters = @ComponentScan.Filter(
                type = FilterType.ANNOTATION,
                classes = {Controller.class, ControllerAdvice.class}))
public class ServletConfig implements WebMvcConfigurer {

    /*
     * 두 컨텍스트 구조 주의:
     *   RootConfig  가 @Controller·@ControllerAdvice 를 제외한 모든 @Component 를 스캔한다.
     *   → JwtAuthInterceptor · LoginUserArgumentResolver 는 루트 컨텍스트의 빈이다.
     *   ServletConfig(자식)는 부모 컨텍스트의 빈을 주입받을 수 있으므로 아래가 동작한다.
     */
    @Autowired
    private JwtAuthInterceptor jwtAuthInterceptor;

    @Autowired
    private LoginUserArgumentResolver loginUserArgumentResolver;

    /*
     * 서블릿 컨텍스트에는 PropertySourcesPlaceholderConfigurer 가 없어 @Value 가 풀리지 않는다.
     * 값을 루트 컨텍스트의 빈에서 받아온다 — 위 두 빈과 같은 경로다.
     */
    @Autowired
    private ImageStorageProperties imageStorageProperties;

    /** RootConfig 의 ObjectMapper 빈(JavaTimeModule 등록됨) — JwtAuthInterceptor/GoogleOAuthClient 와 동일 설정을 공유한다. */
    @Autowired
    private ObjectMapper objectMapper;

    /*
     * @EnableWebMvc 의 기본 MappingJackson2HttpMessageConverter 는 Jackson2ObjectMapperBuilder 가
     * classpath 에서 JavaTimeModule 을 찾아 자동 등록하지만, WRITE_DATES_AS_TIMESTAMPS 는 기본값(true)
     * 그대로라 LocalDateTime 이 ISO-8601 문자열이 아니라 [2027,8,4,...] 숫자 배열로 내려간다
     * (실측, ConsentControllerTest 참고). RootConfig 의 objectMapper 빈으로 교체해 통일한다.
     */
    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        for (HttpMessageConverter<?> converter : converters) {
            if (converter instanceof MappingJackson2HttpMessageConverter) {
                ((MappingJackson2HttpMessageConverter) converter).setObjectMapper(objectMapper);
            }
        }
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtAuthInterceptor)
                .addPathPatterns("/api/**")
                // 로그인 자체 경로와 헬스체크는 인증 없이 열어둔다
                .excludePathPatterns("/api/health", "/api/auth/**");
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(loginUserArgumentResolver);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                // 로컬 개발은 Vite 프록시(same-origin)라 CORS 를 타지 않지만,
                // 프록시를 끄고 직접 붙이는 경우를 위해 남겨둔다.
                .allowedOrigins("https://monorepo-three-ruby-81.vercel.app", "http://localhost:5173")
                // PATCH 가 빠져 있으면 배포 환경(Vercel↔EC2, 교차 출처)에서만 프리플라이트가 막힌다.
                // 로컬은 Vite 프록시라 same-origin 이어서 끝까지 드러나지 않는다. (2026-08-11 추가)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 운영 배포 시 Vue 빌드 산출물을 webapp/resources 아래에 두고 서빙하기 위한 설정
        registry.addResourceHandler("/resources/**")
                .addResourceLocations("/resources/");

        /*
         * 업로드된 프로필 이미지. 인터셉터는 /api/** 에만 걸려 있으므로 이 경로는 인증 없이 열린다 —
         * 그룹 멤버에게 보이는 값이라 의도된 결과이며, 키에 UUID 가 들어 있어 추측으로 열 수 없다.
         * 경로 끝의 '/' 가 없으면 매핑이 조용히 실패한다.
         */
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + imageStorageProperties.getLocalDir() + "/");

        /*
         * Swagger UI. springfox-swagger-ui 2.9.2 는 정적 파일을 jar 안
         * META-INF/resources 아래에 담아둔다 — 이 두 줄이 없으면 /swagger-ui.html 이 404 다.
         * (/v2/api-docs 와 /swagger-resources 는 springfox 가 컨트롤러로 직접 매핑하므로
         *  리소스 핸들러가 필요 없다)
         */
        registry.addResourceHandler("/swagger-ui.html")
                .addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");
    }

    @Bean
    public MultipartResolver multipartResolver() {
        return new StandardServletMultipartResolver();
    }
}
