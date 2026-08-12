package com.kb.tangtang.common.storage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 파일시스템 저장소. v1 기본값이다 (image.storage=local).
 *
 * ⚠ **docker-compose 의 api 서비스에 볼륨이 없으면 배포마다 파일이 전부 사라진다.**
 * dev push 자동배포가 `docker compose down && up --build` 를 돌리기 때문이다.
 *
 * ⚠ **`@Component` 를 붙이지 않는다.** 빈 등록은 `ImageStorageConfig` 팩토리 한 곳에서만 한다 —
 * 여기 붙이면 구현체가 하나뿐인 지금은 문제없어 보여도, `S3ImageStorage` 를 `@Component` 로
 * 추가하는 순간 `NoUniqueBeanDefinitionException` 으로 기동이 실패한다.
 */
public class LocalImageStorage implements ImageStorage {

    private final Path root;
    private final String baseUrl;

    public LocalImageStorage(ImageStorageProperties properties) {
        /*
         * getLocalDir() 이 이미 정규화된 절대경로를 돌려준다(ImageStorageProperties 참고) —
         * ServletConfig 의 리소스 핸들러와 같은 값을 보도록 정규화를 그 한 곳에만 둔다.
         */
        this.root = Paths.get(properties.getLocalDir());
        this.baseUrl = properties.getLocalBaseUrl();
    }

    @Override
    public String store(byte[] content, String key) {
        Path target = resolve(key);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, content);
        } catch (IOException e) {
            /* 사용자가 고칠 수 있는 문제가 아니다 — 400 으로 위장하지 않고 500 으로 올린다 */
            throw new UncheckedIOException("이미지를 저장하지 못했습니다: " + key, e);
        }
        return key;
    }

    @Override
    public void delete(String key) {
        if (key == null) {
            return;
        }
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException e) {
            throw new UncheckedIOException("이미지를 삭제하지 못했습니다: " + key, e);
        }
    }

    @Override
    public String urlOf(String key) {
        return key == null ? null : baseUrl + "/" + key;
    }

    /**
     * 키를 실제 경로로 바꾸면서 **저장 디렉터리 밖으로 나가지 못하게** 막는다.
     * 키는 서버가 만들지만, 방어를 저장소 안에 두면 나중에 키를 만드는 곳이 늘어도 안전하다.
     */
    private Path resolve(String key) {
        Path target = root.resolve(key).normalize();
        if (!target.startsWith(root)) {
            throw new IllegalArgumentException("허용되지 않는 키입니다: " + key);
        }
        return target;
    }
}
