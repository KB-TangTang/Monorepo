package com.kb.tangtang.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 도커 배포에서 해결되지 않는 프로퍼티 플레이스홀더가 없는지 검사한다.
 *
 * 2026-08-19 실제 장애: {@code TossStockClientConfig} 가 {@code @Value("${toss.client-id}")} 를
 * 기본값 없이 선언했는데 {@code application-docker.properties} 에 그 키가 없었다. 로컬은
 * {@code application-local.properties} 에 값이 있어 멀쩡했고, 도커에서만
 * {@code PropertySourcesPlaceholderConfigurer} 가 예외를 던져 **ROOT 컨텍스트 기동이 통째로 실패**했다.
 * 토스와 아무 상관 없는 API 까지 전부 Tomcat 404 가 됐고, 컴파일러도 단위 테스트도 이걸 잡지 못했다.
 *
 * 그래서 여기서 「도커에서 로드되는 두 파일(application.properties + application-docker.properties)」
 * 만으로 모든 필수 플레이스홀더가 해결되는지 확인한다. local 파일은 일부러 보지 않는다 —
 * 그 파일은 git 에 없고 도커에도 로드되지 않아, 여기에 기대는 순간 같은 사고가 반복된다.
 *
 * 기본값이 있는 플레이스홀더({@code ${key:default}})는 검사 대상이 아니다. 정의가 없어도 뜬다.
 */
class DockerPropertyPlaceholderTest {

    /** 기본값이 없는 플레이스홀더만 잡는다 — ':' 가 들어가면 기본값이 있는 것이라 기동을 막지 못한다. */
    private static final Pattern REQUIRED_PLACEHOLDER =
            Pattern.compile("\\$\\{([A-Za-z][A-Za-z0-9._-]*)}");

    /** 값 쪽이 아니라 코드가 요구하는 키만 본다 — 애노테이션 속성 문자열에 등장하는 것들. */
    private static final Pattern ANNOTATION_PLACEHOLDER_HOLDER =
            Pattern.compile("@(?:Value|Scheduled)\\s*\\(([^)]*)\\)", Pattern.DOTALL);

    @Test
    @DisplayName("기본값 없는 @Value 플레이스홀더는 전부 도커에서 로드되는 프로퍼티에 정의돼 있다")
    void everyRequiredPlaceholderIsDefinedForDocker() throws IOException {
        Set<String> defined = dockerProfileKeys();
        Set<String> missing = new TreeSet<>();

        for (Path source : mainJavaSources()) {
            String code = Files.readString(source, StandardCharsets.UTF_8);
            Matcher holder = ANNOTATION_PLACEHOLDER_HOLDER.matcher(code);
            while (holder.find()) {
                Matcher placeholder = REQUIRED_PLACEHOLDER.matcher(holder.group(1));
                while (placeholder.find()) {
                    String key = placeholder.group(1);
                    if (!defined.contains(key)) {
                        missing.add(key + "  (" + source.getFileName() + ")");
                    }
                }
            }
        }

        if (!missing.isEmpty()) {
            throw new AssertionError(String.join("\n",
                    "도커 환경에서 해결되지 않는 플레이스홀더가 있다 — 이대로 배포하면 컨텍스트가 뜨지 않고",
                    "모든 API 가 404 가 된다. application-docker.properties 에 키를 추가하거나",
                    "@Value 에 기본값(${key:})을 주어야 한다.",
                    "",
                    String.join("\n", missing)));
        }
    }

    /** 도커에서 실제로 로드되는 파일 두 개(RootConfig 의 @PropertySource, APP_ENV=docker)만 읽는다. */
    private static Set<String> dockerProfileKeys() throws IOException {
        Properties properties = new Properties();
        for (String resource : List.of("/application.properties", "/application-docker.properties")) {
            try (InputStream in = DockerPropertyPlaceholderTest.class.getResourceAsStream(resource)) {
                if (in == null) {
                    throw new AssertionError(resource + " 를 클래스패스에서 못 찾았다");
                }
                properties.load(in);
            }
        }
        return properties.stringPropertyNames();
    }

    private static List<Path> mainJavaSources() throws IOException {
        Path root = Paths.get("src/main/java");
        if (!Files.isDirectory(root)) {
            throw new AssertionError("src/main/java 를 찾지 못했다 — 테스트 작업 디렉터리 확인 필요: "
                    + Paths.get("").toAbsolutePath());
        }
        try (Stream<Path> paths = Files.walk(root)) {
            return paths.filter(path -> path.toString().endsWith(".java")).collect(Collectors.toList());
        }
    }
}
