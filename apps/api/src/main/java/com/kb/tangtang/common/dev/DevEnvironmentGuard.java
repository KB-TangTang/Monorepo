package com.kb.tangtang.common.dev;

import com.kb.tangtang.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 개발 전용 API 의 실행 환경 가드 (이슈 #152).
 *
 * <p>DEV API 는 <b>로컬에서만</b> 동작한다. 판단 기준은 {@code app.env} 이며,
 * {@code RootConfig} 가 환경별 설정 파일을 고를 때 쓰는 값과 같다 —
 * 로컬은 APP_ENV 가 없어 기본값 {@code local}, 도커는 compose 가 {@code docker} 를 주입한다.
 * {@code DevMissionService.ensureLocalEnvironment} 와 같은 규칙·같은 에러 코드다.
 *
 * <p>전용 on/off 플래그를 두지 않는 이유는 <b>끄고 켤 일이 없기 때문</b>이다. 배포 환경에서
 * 쓸 계획이 없는 도구에 스위치를 달면 로컬에서 쓸 때마다 설정을 고쳐야 하고, 스위치가 켜진 채
 * 배포될 위험만 새로 생긴다. 환경으로 갈리는 것은 환경으로 판단한다.
 *
 * <p>⚠ <b>이 빈이 별도로 존재하는 이유</b>는 컨트롤러에서 {@code @Value} 가 풀리지 않아서다.
 * 서블릿 컨텍스트에는 PropertySourcesPlaceholderConfigurer 가 없다(그 빈은 RootConfig 에만 있다).
 * 루트 컨텍스트의 빈이 값을 들고 있어야 자식 컨텍스트가 주입받을 수 있다
 * ({@link com.kb.tangtang.common.storage.ImageStorageProperties} 와 같은 이유·같은 경로다).
 */
@Component
public class DevEnvironmentGuard {

    private static final String LOCAL = "local";

    private final String appEnv;

    public DevEnvironmentGuard(@Value("${app.env:local}") String appEnv) {
        this.appEnv = appEnv;
    }

    /** 로컬이 아니면 막는다. */
    public void ensureLocal() {
        if (!LOCAL.equalsIgnoreCase(appEnv)) {
            throw new BusinessException("DEV_API_DISABLED",
                    "개발용 API는 로컬 환경에서만 사용할 수 있습니다.");
        }
    }
}
