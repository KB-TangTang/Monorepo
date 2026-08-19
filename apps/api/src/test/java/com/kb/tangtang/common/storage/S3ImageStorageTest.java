package com.kb.tangtang.common.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 실제 S3 에 붙지 않는다 — {@link S3Client} 를 목으로 두고 <b>SDK 에 무엇을 넘기는지</b>만 본다.
 * 여기서 검증하는 것들이 어긋나면 사진이 안 보이거나(contentType) 업로드가 통째로 실패한다(ACL).
 */
@ExtendWith(MockitoExtension.class)
class S3ImageStorageTest {

    private static final byte[] CONTENT = {1, 2, 3, 4};
    private static final String PROFILE_KEY = "profile/7/abc.jpg";
    private static final String DEFENSE_KEY = "defense/42/def.jpg";

    @Mock
    private S3Client client;

    private S3ImageStorage storage;

    @BeforeEach
    void setUp() {
        ImageStorageProperties properties = new ImageStorageProperties();
        properties.setS3Bucket("tangtang-images");
        properties.setS3Region("ap-northeast-2");
        storage = new S3ImageStorage(client, properties);
    }

    @Test
    @DisplayName("업로드는 버킷·키·contentType 을 채우고 ACL 은 비운다")
    void putsObjectWithJpegContentType() throws Exception {
        ArgumentCaptor<PutObjectRequest> request = ArgumentCaptor.forClass(PutObjectRequest.class);
        ArgumentCaptor<RequestBody> body = ArgumentCaptor.forClass(RequestBody.class);

        String saved = storage.store(CONTENT, PROFILE_KEY);

        assertEquals(PROFILE_KEY, saved, "호출부가 DB 에 넣을 값이라 넘긴 키를 그대로 돌려줘야 한다");
        verify(client).putObject(request.capture(), body.capture());
        assertEquals("tangtang-images", request.getValue().bucket());
        assertEquals(PROFILE_KEY, request.getValue().key());
        assertEquals("image/jpeg", request.getValue().contentType(),
                "빠지면 octet-stream 으로 저장돼 브라우저가 이미지로 그리지 않는다");
        assertNull(request.getValue().acl(),
                "버킷이 ACL 비활성화라 값을 넣으면 AccessControlListNotSupported 로 실패한다");

        try (InputStream in = body.getValue().contentStreamProvider().newStream()) {
            assertArrayEquals(CONTENT, in.readAllBytes());
        }
    }

    @Test
    @DisplayName("삭제는 버킷·키를 그대로 넘긴다")
    void deletesObject() {
        ArgumentCaptor<DeleteObjectRequest> request =
                ArgumentCaptor.forClass(DeleteObjectRequest.class);

        storage.delete(DEFENSE_KEY);

        verify(client).deleteObject(request.capture());
        assertEquals("tangtang-images", request.getValue().bucket());
        assertEquals(DEFENSE_KEY, request.getValue().key());
    }

    @Test
    @DisplayName("키가 null 이면 S3 를 호출하지 않는다 — 사진을 설정한 적 없는 사용자가 이 경로를 탄다")
    void skipsDeleteWhenKeyIsNull() {
        storage.delete(null);

        verify(client, never()).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    @DisplayName("URL 은 가상 호스팅 주소로 조립한다. 프로필·변론 증빙이 같은 규칙을 쓴다")
    void buildsVirtualHostedUrl() {
        assertEquals("https://tangtang-images.s3.ap-northeast-2.amazonaws.com/" + PROFILE_KEY,
                storage.urlOf(PROFILE_KEY));
        assertEquals("https://tangtang-images.s3.ap-northeast-2.amazonaws.com/" + DEFENSE_KEY,
                storage.urlOf(DEFENSE_KEY));
    }

    @Test
    @DisplayName("키가 null 이면 URL 도 null 이다 — 화면이 null 을 보고 이니셜 아바타를 그린다")
    void urlIsNullWhenKeyIsNull() {
        assertNull(storage.urlOf(null));
    }
}
