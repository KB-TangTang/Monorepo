package com.kb.tangtang.common.storage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * `image.storage` 한 줄로 구현체가 갈리는지 확인한다.
 *
 * <p>이 팩토리가 조용히 틀리면 "S3 로 바꿨다고 생각했는데 로컬에 계속 쓰고 있었다" 가 되고,
 * 컨테이너를 새로 만드는 순간 사진이 통째로 사라진다. 분기와 실패 조건을 모두 못 박아 둔다.
 */
class ImageStorageConfigTest {

    private ImageStorage build(String storageType, String bucket) {
        ImageStorageConfig config = new ImageStorageConfig();
        ReflectionTestUtils.setField(config, "storageType", storageType);

        ImageStorageProperties properties = new ImageStorageProperties();
        properties.setLocalDir(System.getProperty("java.io.tmpdir"));
        properties.setLocalBaseUrl("/uploads");
        properties.setS3Bucket(bucket);
        properties.setS3Region("ap-northeast-2");

        return config.imageStorage(properties);
    }

    @Test
    @DisplayName("local 이면 파일시스템 저장소를 만든다")
    void selectsLocal() {
        assertInstanceOf(LocalImageStorage.class, build("local", ""));
    }

    @Test
    @DisplayName("s3 면 S3 저장소를 만든다 — SDK 가 HTTP 클라이언트를 찾는지까지 여기서 드러난다")
    void selectsS3() {
        /*
         * build.gradle 에서 apache-client 를 빼고 url-connection-client 를 넣었다.
         * 그 배선이 어긋나면 S3Client.builder().build() 가
         * "Unable to load an HTTP implementation" 으로 여기서 터진다 — 기동 후가 아니라.
         */
        ImageStorage storage = build("s3", "tangtang-images");

        assertInstanceOf(S3ImageStorage.class, storage);
        assertEquals("https://tangtang-images.s3.ap-northeast-2.amazonaws.com/profile/7/a.jpg",
                storage.urlOf("profile/7/a.jpg"));
        ((S3ImageStorage) storage).close();
    }

    @Test
    @DisplayName("대소문자는 가리지 않는다")
    void ignoresCase() {
        assertInstanceOf(LocalImageStorage.class, build("LOCAL", ""));
    }

    @Test
    @DisplayName("s3 인데 버킷이 비면 기동 시점에 실패한다 — 조용히 로컬로 흘러가면 안 된다")
    void failsWhenBucketMissing() {
        IllegalStateException blank = assertThrows(IllegalStateException.class,
                () -> build("s3", "  "));
        assertTrue(blank.getMessage().contains("image.s3.bucket"));

        assertThrows(IllegalStateException.class, () -> build("s3", null));
    }

    @Test
    @DisplayName("모르는 값이면 기동에 실패한다 — 오타를 기본값으로 덮지 않는다")
    void failsOnUnknownValue() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> build("S#", ""));
        assertTrue(e.getMessage().contains("S#"));
    }
}
