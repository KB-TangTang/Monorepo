package com.kb.tangtang.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * 두 컨텍스트(루트/서블릿) 경계가 지켜지는지 <b>소스 검사</b>로 확인한다.
 *
 * <p>이 테스트가 있는 이유: {@code ChatMessageService}(@Service → 루트 컨텍스트)가 생성자로
 * {@code SimpMessagingTemplate}(@EnableWebSocketMessageBroker → 서블릿 컨텍스트)을 주입받는 코드가
 * 들어왔고, <b>컴파일도 단위 테스트도 전부 통과했다.</b> 실제로는 ContextLoaderListener 단계에서
 * NoSuchBeanDefinitionException 이 나 war 배포 자체가 실패한다 — 컨테이너를 띄우지 않으면
 * 드러나지 않는 사고라 소스 텍스트로 잡는다(프론트의 tests/groupChatView.test.js 와 같은 방식).
 *
 * <p>완벽한 정적 분석이 아니다. 같은 실수(루트 빈이 SimpMessagingTemplate 을 직접 주입)가 다시
 * 들어오면 빨개지는 것이 목적이다. 전송이 필요하면 {@code ChatBroadcaster} 를 주입한다.
 */
class RootContextWiringTest {

    /** RootConfig 의 컴포넌트 스캔이 루트 컨텍스트에 만드는 스테레오타입 */
    private static final Pattern ROOT_STEREOTYPE =
            Pattern.compile("(?m)^\\s*@(Service|Component|Repository)\\b");

    /** 생성자 파라미터로 받는 형태 — ChatMessageService 가 저질렀던 바로 그 모양이다 */
    private static final Pattern CONSTRUCTOR_INJECTION =
            Pattern.compile("public\\s+\\w+\\s*\\([^)]*\\bSimpMessagingTemplate\\b", Pattern.DOTALL);

    /** 필드 주입 형태 */
    private static final Pattern FIELD_INJECTION =
            Pattern.compile("@Autowired[^;{}]*\\bSimpMessagingTemplate\\b", Pattern.DOTALL);

    @Test
    @DisplayName("루트 컨텍스트 빈은 SimpMessagingTemplate 을 직접 주입받지 않는다")
    void rootBeansDoNotInjectSimpMessagingTemplate() {
        List<String> offenders = new ArrayList<>();
        for (Path java : mainSources()) {
            /*
             * config 패키지는 WebConfig#getServletConfigClasses() 로 등록되는 서블릿 컨텍스트
             * 설정이 모여 있어 예외다 — WebSocketConfig 가 여기서 템플릿을 주입받아 루트의
             * ChatBroadcaster 구현에 바인딩한다.
             */
            if (java.toString().replace('\\', '/').contains("/com/kb/tangtang/config/")) {
                continue;
            }
            String source = read(java);
            if (!ROOT_STEREOTYPE.matcher(source).find()) {
                continue;
            }
            if (CONSTRUCTOR_INJECTION.matcher(source).find() || FIELD_INJECTION.matcher(source).find()) {
                offenders.add(java.getFileName().toString());
            }
        }

        if (!offenders.isEmpty()) {
            fail("루트 컨텍스트 빈이 SimpMessagingTemplate 을 직접 주입받고 있다 " + offenders
                    + " — 루트 컨텍스트는 그 빈을 볼 수 없어 war 가 기동하지 못한다. "
                    + "ChatBroadcaster 를 대신 주입할 것.");
        }
    }

    /**
     * 스테레오타입 빈이 생성자를 둘 이상 두면 스프링은 어느 것을 쓸지 고르지 못하고 기본 생성자를
     * 찾다가 실패한다. {@code ChatQueryService} 에 Clock 을 받는 테스트용 생성자를 추가하면서
     * 실제로 배포가 깨졌다 — 단위 테스트는 생성자를 직접 호출하니 전부 통과했다.
     */
    @Test
    @DisplayName("생성자가 둘 이상인 스프링 빈은 하나에 @Autowired 를 붙인다")
    void multipleConstructorBeansMarkOneAsAutowired() {
        List<String> offenders = new ArrayList<>();
        for (Path java : mainSources()) {
            String source = read(java);
            if (!ROOT_STEREOTYPE.matcher(source).find()) {
                continue;
            }
            String className = java.getFileName().toString().replace(".java", "");
            long constructors = Pattern
                    .compile("(?m)^\\s*(public\\s+|protected\\s+|private\\s+)?" + className + "\\s*\\(")
                    .matcher(source)
                    .results()
                    .count();
            if (constructors >= 2 && !source.contains("@Autowired")) {
                offenders.add(java.getFileName().toString());
            }
        }

        if (!offenders.isEmpty()) {
            fail("생성자가 둘 이상인데 @Autowired 표시가 없는 빈이 있다 " + offenders
                    + " — 스프링이 기본 생성자를 찾다가 NoSuchMethodException 으로 기동에 실패한다.");
        }
    }

    @Test
    @DisplayName("WebSocketConfig 는 서블릿 컨텍스트에만 등록된다")
    void webSocketConfigStaysInServletContext() {
        String webConfig = read(sourceRoot().resolve("com/kb/tangtang/config/WebConfig.java"));
        assertTrue(webConfig.contains("getServletConfigClasses")
                        && webConfig.matches("(?s).*getServletConfigClasses.*WebSocketConfig\\.class.*"),
                "WebSocketConfig 는 getServletConfigClasses() 로만 등록해야 한다");

        String webSocketConfig = read(sourceRoot().resolve("com/kb/tangtang/config/WebSocketConfig.java"));
        // 주석에 적힌 "@Configuration 을 붙이지 않는다" 와 구분하려고 줄 맨 앞의 애노테이션만 본다
        assertTrue(!Pattern.compile("(?m)^@Configuration\\b").matcher(webSocketConfig).find(),
                "WebSocketConfig 에 @Configuration 을 붙이면 RootConfig 스캔이 루트 컨텍스트에도 등록한다");
    }

    private List<Path> mainSources() {
        try (Stream<Path> paths = Files.walk(sourceRoot())) {
            return paths.filter(p -> p.toString().endsWith(".java")).collect(java.util.stream.Collectors.toList());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** 테스트 작업 디렉터리는 apps/api 다. 다른 위치에서 돌더라도 찾을 수 있게 한 단계 더 본다 */
    private Path sourceRoot() {
        Path direct = Paths.get("src/main/java");
        if (Files.isDirectory(direct)) {
            return direct;
        }
        return Paths.get("apps/api/src/main/java");
    }

    private String read(Path path) {
        try {
            return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
