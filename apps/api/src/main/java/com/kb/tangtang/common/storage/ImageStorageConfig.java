package com.kb.tangtang.common.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.time.Duration;

/**
 * 이미지 저장소 구현체 선택.
 *
 * `image.storage` 프로퍼티 한 줄로 구현체가 갈린다 — `financial.client=mock|codef` 와 같은 방식
 * (`FinancialClientConfig` 참고, DECISIONS.md 2026-08-05).
 *
 * ⚠ **구현체에 `@Component` 를 붙이지 않는다.** 붙이면 `ImageStorage` 빈이 둘이 되어
 *   `NoUniqueBeanDefinitionException` 으로 기동이 실패한다. 빈 선택은 이 팩토리 한 곳에만 둔다.
 * ⚠ Spring Boot 의 `@ConditionalOnProperty` 는 쓸 수 없다(Spring Legacy 프로젝트다) — 프로퍼티를
 *   직접 읽어 빈을 고른다. 같은 이유로 **`S3Client` 를 별도 `@Bean` 으로 빼지 않는다.** 빼면
 *   `image.storage=local` 인 팀원 환경에서도 S3 클라이언트가 만들어지면서 리전·자격증명이
 *   없다고 기동이 실패한다. s3 분기 안에서만 만든다.
 * ⚠ `financial.client` 와 달리 **모르는 값을 조용히 기본값으로 흘려보내지 않는다.** 오타 하나가
 *   "S3 로 전환했다고 생각했는데 실제로는 로컬에 계속 쓰고 있었다" 같은, 알아채기 어려운 사고로
 *   이어질 수 있어 기동 시점에 즉시 실패시킨다.
 */
@Configuration
public class ImageStorageConfig {

    /**
     * S3 호출 1회(재시도 1번분)의 상한. 업로드는 서버 메모리 → 같은 리전 S3 라
     * 정상이면 1초 안에 끝난다. 10배 여유를 두되 SDK 기본값 30초보다는 훨씬 짧게 잡는다.
     */
    private static final Duration ATTEMPT_TIMEOUT = Duration.ofSeconds(5);

    /** 재시도까지 포함한 한 요청의 총 상한. 이 값이 사진 1장이 묶어 둘 수 있는 최대 시간이다. */
    private static final Duration CALL_TIMEOUT = Duration.ofSeconds(10);

    /** local | s3 */
    @Value("${image.storage}")
    private String storageType;

    @Bean
    public ImageStorage imageStorage(ImageStorageProperties properties) {
        if ("local".equalsIgnoreCase(storageType)) {
            return new LocalImageStorage(properties);
        }
        if ("s3".equalsIgnoreCase(storageType)) {
            return new S3ImageStorage(s3Client(properties), properties);
        }
        throw new IllegalStateException(
                "지원하지 않는 image.storage 값입니다: '" + storageType + "' (지원: local, s3)");
    }

    /**
     * 자격증명은 <b>기본 탐색 체인</b>에 맡긴다(환경변수 → ~/.aws → EC2 IAM 역할 순).
     * 키를 프로퍼티로 받지 않는 이유는, 받는 순간 시크릿이 설정 파일에 적힐 자리가 생기고
     * EC2 에서 IAM 역할로 넘어갈 때 코드를 다시 고쳐야 하기 때문이다.
     * 도커는 `.env` → `docker-compose.yml` 이 `AWS_ACCESS_KEY_ID`·`AWS_SECRET_ACCESS_KEY` 를
     * 컨테이너 환경변수로 넣어 주므로 그대로 잡힌다.
     *
     * <p>HTTP 클라이언트를 명시하는 이유는 `build.gradle` 주석에 있다 — 기본값인 apache-client 를
     * commons-logging 때문에 빼 두어서 SDK 가 스스로 찾지 못한다.
     *
     * <p><b>타임아웃을 기본값에 맡기지 않는다.</b> SDK 기본 소켓 타임아웃은 읽기·쓰기 각 30초이고
     * (`SdkHttpConfigurationOption`) 그 위에 기본 재시도가 붙는다. 그런데 업로드는 지금
     * {@code DefenseService#registerDefense} · {@code UserService#updateProfileImage} 의
     * <b>DB 트랜잭션 안에서</b> 일어난다 — S3 가 느려지면 HikariCP 커넥션(풀 크기 10,
     * {@code RootConfig#dataSource})이 그만큼 묶이고, 변론 기능만이 아니라 같은 풀을 쓰는
     * <b>API 전체</b>가 대기한다. 상한을 못 박아 최악의 경우를 「수 분」에서 「사진당 10초」로 줄인다.
     *
     * <p>업로드를 트랜잭션 밖으로 빼는 것이 근본 해결이지만, 그건 서비스 두 개의 트랜잭션 경계를
     * 바꾸는 구조 변경이라 별도 이슈로 뺐다(`docs.local/state/BACKLOG.md`). 그때까지의 방어선이다.
     */
    private S3Client s3Client(ImageStorageProperties properties) {
        if (properties.getS3Bucket() == null || properties.getS3Bucket().isBlank()) {
            throw new IllegalStateException(
                    "image.storage=s3 인데 image.s3.bucket 이 비어 있습니다. "
                            + "(도커라면 .env 의 AWS_S3_BUCKET 을 확인하세요)");
        }
        return S3Client.builder()
                .region(Region.of(properties.getS3Region()))
                .httpClientBuilder(UrlConnectionHttpClient.builder()
                        .socketTimeout(ATTEMPT_TIMEOUT))
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        .apiCallAttemptTimeout(ATTEMPT_TIMEOUT)
                        .apiCallTimeout(CALL_TIMEOUT)
                        .build())
                .build();
    }
}
