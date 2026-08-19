package com.kb.tangtang.common.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

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
     */
    private S3Client s3Client(ImageStorageProperties properties) {
        if (properties.getS3Bucket() == null || properties.getS3Bucket().isBlank()) {
            throw new IllegalStateException(
                    "image.storage=s3 인데 image.s3.bucket 이 비어 있습니다. "
                            + "(도커라면 .env 의 AWS_S3_BUCKET 을 확인하세요)");
        }
        return S3Client.builder()
                .region(Region.of(properties.getS3Region()))
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .build();
    }
}
