package com.kb.tangtang.common.storage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalImageStorageTest {

    private static final byte[] CONTENT = {1, 2, 3, 4};
    private static final String KEY = "profile/7/abc.jpg";

    private LocalImageStorage storage(Path dir) {
        ImageStorageProperties properties = new ImageStorageProperties();
        properties.setLocalDir(dir.toString());
        properties.setLocalBaseUrl("/uploads");
        return new LocalImageStorage(properties);
    }

    @Test
    @DisplayName("키 경로 그대로 파일을 만든다 — 하위 디렉터리도 함께 만든다")
    void storesFile(@TempDir Path dir) throws Exception {
        String saved = storage(dir).store(CONTENT, KEY);

        assertEquals(KEY, saved);
        Path file = dir.resolve(KEY);
        assertTrue(Files.exists(file));
        assertArrayEquals(CONTENT, Files.readAllBytes(file));
    }

    @Test
    @DisplayName("삭제하면 파일이 사라진다")
    void deletesFile(@TempDir Path dir) {
        LocalImageStorage storage = storage(dir);
        storage.store(CONTENT, KEY);

        storage.delete(KEY);

        assertFalse(Files.exists(dir.resolve(KEY)));
    }

    @Test
    @DisplayName("없는 키를 지워도 예외를 던지지 않는다 — 삭제는 멱등이어야 한다")
    void deleteIsIdempotent(@TempDir Path dir) {
        LocalImageStorage storage = storage(dir);

        assertDoesNotThrow(() -> storage.delete("profile/7/none.jpg"));
        assertDoesNotThrow(() -> storage.delete(null));
    }

    @Test
    @DisplayName("URL 은 base-url 과 키를 이어 만든다. 키가 null 이면 null 이다")
    void buildsUrl(@TempDir Path dir) {
        LocalImageStorage storage = storage(dir);

        assertEquals("/uploads/profile/7/abc.jpg", storage.urlOf(KEY));
        assertEquals(null, storage.urlOf(null));
    }

    @Test
    @DisplayName("키에 상위 경로(..)가 섞이면 거부한다 — 저장 디렉터리 밖으로 쓰지 못하게 한다")
    void rejectsTraversal(@TempDir Path dir) {
        LocalImageStorage storage = storage(dir);

        assertThrowsIllegalArgument(() -> storage.store(CONTENT, "../evil.jpg"));
        assertThrowsIllegalArgument(() -> storage.delete("../evil.jpg"));
    }

    private static void assertThrowsIllegalArgument(Runnable runnable) {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                runnable::run);
    }
}
