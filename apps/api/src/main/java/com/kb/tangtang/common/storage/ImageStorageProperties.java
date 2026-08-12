package com.kb.tangtang.common.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 저장소 설정 값 홀더.
 *
 * ⚠ **ServletConfig 가 이 빈을 주입받는다.** 서블릿 컨텍스트에는
 * PropertySourcesPlaceholderConfigurer 가 없어 거기서는 @Value 가 풀리지 않는다.
 * 값을 루트 컨텍스트의 빈에 담아두면 자식 컨텍스트가 @Autowired 로 받을 수 있다
 * (JwtAuthInterceptor 를 받는 것과 같은 경로다).
 */
@Component
public class ImageStorageProperties {

    @Value("${image.local.dir}")
    private String localDir;

    @Value("${image.local.base-url}")
    private String localBaseUrl;

    public String getLocalDir() {
        return localDir;
    }

    public void setLocalDir(String localDir) {
        this.localDir = localDir;
    }

    public String getLocalBaseUrl() {
        return localBaseUrl;
    }

    public void setLocalBaseUrl(String localBaseUrl) {
        this.localBaseUrl = localBaseUrl;
    }
}
