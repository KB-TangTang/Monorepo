package com.kb.tangtang.config;

import com.kb.tangtang.common.docs.SwaggerTags;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.ApiKey;
import springfox.documentation.service.AuthorizationScope;
import springfox.documentation.service.SecurityReference;
import springfox.documentation.service.Tag;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * Swagger(Springfox 2.9.2) 설정. 문서는 {@code /swagger-ui.html} 에서 본다.
 *
 * <p>⚠ <b>이 클래스에 {@code @Configuration} 을 붙이지 말 것.</b> {@code RootConfig} 가
 * {@code com.kb.tangtang} 전체를 스캔하면서 {@code @Controller}·{@code @ControllerAdvice} 만
 * 제외하므로, {@code @Configuration} 을 붙이면 <b>루트 컨텍스트에도 함께 등록</b>돼
 * Docket 이 두 번 만들어진다. {@code ServletConfig} 가 애노테이션 없이
 * {@code WebConfig#getServletConfigClasses()} 에 직접 등록되는 것과 같은 이유다.
 *
 * <p>springfox 가 문서를 만들려면 {@code DispatcherServlet} 의 핸들러 매핑을 읽어야 하므로
 * 이 설정은 <b>반드시 서블릿 컨텍스트</b>에 있어야 한다. 루트에 두면 문서가 비어서 나온다.
 *
 * <p>인증 인터셉터는 {@code /api/**} 에만 걸려 있어({@code ServletConfig#addInterceptors})
 * {@code /swagger-ui.html} · {@code /v2/api-docs} 는 별도 whitelist 없이 열린다.
 * 바꿔 말하면 <b>문서가 인증 없이 공개</b>된다. 시연 단계라 그대로 두지만 운영 노출 시 재검토한다.
 *
 * @see <a href="file:../../../../../../../../.claude/context/DECISIONS.md">DECISIONS.md 2026-08-13 (4)</a>
 */
@EnableSwagger2
public class SwaggerConfig {

    private static final String API_TITLE = "탕탕(지갑재판소) API";
    private static final String API_VERSION = "1.0";
    private static final String API_DESCRIPTION =
            "새는 돈을 법정에 세우는 자산관리 서비스. 모든 응답은 공통 래퍼 ApiResponse 로 감싸진다.\n\n"
                    + "성공: { \"success\": true, \"data\": { ... } }\n"
                    + "실패: { \"success\": false, \"code\": \"NOT_FOUND\", \"message\": \"...\" }\n\n"
                    + "우측 상단 Authorize 에 `Bearer {accessToken}` 을 넣으면 인증이 필요한 API 를 그대로 호출할 수 있다.";

    /** 정식 API. 개발 전용 컨트롤러(/api/dev/**)는 아래 devApi() 로 분리한다. */
    @Bean
    public Docket api() {
        return baseDocket()
                .groupName("01. 서비스 API")
                /*
                 * 화면에 그려지는 섹션 목록이다. 순서도 여기서 정해진다.
                 * 이름은 SwaggerTags 상수를 그대로 쓴다 — 문자열을 다시 적으면 오타 하나로
                 * 설명 없는 빈 섹션이 하나 더 생긴다.
                 * 태그를 추가할 일이 있으면 SwaggerTags 의 주석을 먼저 읽는다.
                 */
                .tags(
                        new Tag(SwaggerTags.HEALTH, "서버 기동 확인. 인증이 필요 없다"),
                        new Tag(SwaggerTags.USER,
                                "구글 로그인 · 토큰 재발급 · 내 정보 · 닉네임 · 프로필 이미지 · 약관 동의 · 탈퇴 · 튜토리얼"),
                        new Tag(SwaggerTags.ACCOUNT, "CODEF 기관 인증 · 계좌 연결. 연동 5단계 순서대로 나열돼 있다"),
                        new Tag(SwaggerTags.MISSION, "오늘의 미션 · 연속 성공일 · 요주의 카테고리 분석"),
                        new Tag(SwaggerTags.GROUP_CHALLENGE, "생성 · 초대 · 참여 · 조회"),
                        new Tag(SwaggerTags.NOTIFICATION, "목록 · 읽음 처리 · SSE 실시간 스트림"),
                        new Tag(SwaggerTags.REPORT, "소비 추이 · 요약 · AI 분석. 완료된 월만 조회된다"))
                .select()
                .apis(RequestHandlerSelectors.withClassAnnotation(RestController.class))
                // /api/dev/** 는 운영에 나가지 않는 배치 트리거·미션 재배정용이다.
                // 정식 목록에 섞이면 프론트가 쓸 수 있는 API 로 오해한다.
                .paths(PathSelectors.regex("/api/(?!dev/).*"))
                .build();
    }

    /** 개발·시연 전용. DevEnvironmentGuard 가 운영 환경에서 막는다. */
    @Bean
    public Docket devApi() {
        return baseDocket()
                .groupName("02. 개발 전용 API")
                // 서비스 태그를 여기에 선언하면 엔드포인트가 없는 빈 섹션으로 그려진다. 그룹별로 나눠 선언한다.
                .tags(new Tag(SwaggerTags.DEV, "배치 수동 트리거 · 미션 재배정. 로컬에서만 동작한다"))
                .select()
                .apis(RequestHandlerSelectors.withClassAnnotation(RestController.class))
                .paths(PathSelectors.regex("/api/dev/.*"))
                .build();
    }

    private Docket baseDocket() {
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo())
                /*
                 * 서블릿 타입은 핸들러 시그니처에만 있고 실제 요청 파라미터가 아니다.
                 * 빼지 않으면 로그아웃·탈퇴 같은 API 에 request/response 라는 가짜 파라미터가 뜬다.
                 */
                .ignoredParameterTypes(HttpServletRequest.class, HttpServletResponse.class)
                .securitySchemes(List.of(bearerAuth()))
                .securityContexts(List.of(securityContext()));
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title(API_TITLE)
                .description(API_DESCRIPTION)
                .version(API_VERSION)
                .build();
    }

    /**
     * JWT 는 Authorization 헤더로 받는다(JwtAuthInterceptor).
     * Authorize 창에는 `Bearer ` 접두사까지 포함해 넣어야 한다. 인터셉터가 접두사를 직접 잘라낸다.
     */
    private ApiKey bearerAuth() {
        return new ApiKey("Authorization", "Authorization", "header");
    }

    private SecurityContext securityContext() {
        return SecurityContext.builder()
                .securityReferences(defaultAuth())
                // 로그인 전 호출하는 /api/auth/** 와 /api/health 는 토큰이 필요 없지만,
                // 자물쇠 아이콘만 붙을 뿐 호출을 막지는 않으므로 경로를 나누지 않는다.
                .forPaths(PathSelectors.any())
                .build();
    }

    private List<SecurityReference> defaultAuth() {
        AuthorizationScope scope = new AuthorizationScope("global", "accessEverything");
        return List.of(new SecurityReference("Authorization", new AuthorizationScope[]{scope}));
    }
}
