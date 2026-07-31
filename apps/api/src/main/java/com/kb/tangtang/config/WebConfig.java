package com.kb.tangtang.config;

import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

import javax.servlet.Filter;
import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletRegistration;

public class WebConfig extends AbstractAnnotationConfigDispatcherServletInitializer {

    // 업로드 임시 경로: OS 에 종속되지 않도록 시스템 임시 디렉터리를 사용한다.
    // 운영에서 별도 경로가 필요하면 -Dtangtang.upload.dir=/path 로 지정한다.
    private static final String LOCATION =
            System.getProperty("tangtang.upload.dir", System.getProperty("java.io.tmpdir"));
    private static final long MAX_FILE_SIZE = 1024 * 1024 * 10L;
    private static final long MAX_REQUEST_SIZE = 1024 * 1024 * 20L;
    private static final int FILE_SIZE_THRESHOLD = 1024 * 1024 * 5;

    @Override
    protected Class<?>[] getRootConfigClasses() {
        return new Class[]{RootConfig.class};
    }

    @Override
    protected Class<?>[] getServletConfigClasses() {
        return new Class[]{ServletConfig.class};
    }

    @Override
    protected String[] getServletMappings() {
        return new String[]{"/"};
    }

    @Override
    protected Filter[] getServletFilters() {
        CharacterEncodingFilter encodingFilter = new CharacterEncodingFilter();
        encodingFilter.setEncoding("UTF-8");
        encodingFilter.setForceEncoding(true);
        return new Filter[]{encodingFilter};
    }

    @Override
    protected void customizeRegistration(ServletRegistration.Dynamic registration) {
        // 매핑되지 않은 요청도 예외로 던져 CommonExceptionAdvice 가 JSON 404 를 반환하게 한다
        registration.setInitParameter("throwExceptionIfNoHandlerFound", "true");
        registration.setMultipartConfig(new MultipartConfigElement(
                LOCATION, MAX_FILE_SIZE, MAX_REQUEST_SIZE, FILE_SIZE_THRESHOLD));
        // SSE(알림) 사용을 위한 비동기 지원
        registration.setAsyncSupported(true);
    }
}
