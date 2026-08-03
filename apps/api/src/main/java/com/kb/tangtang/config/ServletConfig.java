package com.kb.tangtang.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

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

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("https://monorepo-three-ruby-81.vercel.app")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 운영 배포 시 Vue 빌드 산출물을 webapp/resources 아래에 두고 서빙하기 위한 설정
        registry.addResourceHandler("/resources/**")
                .addResourceLocations("/resources/");
    }

    @Bean
    public MultipartResolver multipartResolver() {
        return new StandardServletMultipartResolver();
    }
}
