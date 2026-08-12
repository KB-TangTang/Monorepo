package com.kb.tangtang.common.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 이미지 저장소 구현체 선택.
 *
 * `image.storage` 프로퍼티 한 줄로 구현체가 갈린다 — `financial.client=mock|codef` 와 같은 방식
 * (`FinancialClientConfig` 참고, DECISIONS.md 2026-08-05).
 *
 * ⚠ **`LocalImageStorage` 에는 더 이상 `@Component` 를 붙이지 않는다.** 붙인 채로 두면 지금은
 *   구현체가 하나뿐이라 문제가 드러나지 않지만, 나중에 `S3ImageStorage` 를 `@Component` 로
 *   추가하는 순간 `ImageStorage` 빈이 둘이 되어 `NoUniqueBeanDefinitionException` 으로 기동이
 *   실패한다. "구현체 하나 추가하면 끝"이라는 스펙(§9)이 성립하려면 빈 선택이 이 팩토리
 *   한 곳에만 있어야 한다.
 * ⚠ Spring Boot 의 `@ConditionalOnProperty` 는 쓸 수 없다(Spring Legacy 프로젝트다) — 프로퍼티를
 *   직접 읽어 빈을 고른다.
 * ⚠ `financial.client` 와 달리 **모르는 값을 조용히 기본값으로 흘려보내지 않는다.** 오타 하나가
 *   "S3 로 전환했다고 생각했는데 실제로는 로컬에 계속 쓰고 있었다" 같은, 알아채기 어려운 사고로
 *   이어질 수 있어 기동 시점에 즉시 실패시킨다.
 */
@Configuration
public class ImageStorageConfig {

    /** local (s3 는 아직 미구현 — §9 S3 전환 절차 참고) */
    @Value("${image.storage}")
    private String storageType;

    @Bean
    public ImageStorage imageStorage(ImageStorageProperties properties) {
        if ("local".equalsIgnoreCase(storageType)) {
            return new LocalImageStorage(properties);
        }
        throw new IllegalStateException(
                "지원하지 않는 image.storage 값입니다: '" + storageType + "' (지원: local)");
    }
}
