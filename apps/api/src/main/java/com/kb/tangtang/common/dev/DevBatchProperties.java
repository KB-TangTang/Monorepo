package com.kb.tangtang.common.dev;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * DEV 배치 수동 트리거 설정 값 홀더 (이슈 #152).
 *
 * <p>⚠ <b>@Value 를 컨트롤러에 직접 붙이지 못해 이 빈이 있다.</b> 서블릿 컨텍스트에는
 * PropertySourcesPlaceholderConfigurer 가 없어 거기서는 {@code ${...}} 가 풀리지 않는다.
 * 루트 컨텍스트의 빈에 값을 담아 두면 자식 컨텍스트가 @Autowired 로 받을 수 있다
 * ({@link com.kb.tangtang.common.storage.ImageStorageProperties} 와 같은 이유·같은 경로다).
 */
@Component
public class DevBatchProperties {

    @Value("${dev.batch-trigger.enabled}")
    private boolean enabled;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
