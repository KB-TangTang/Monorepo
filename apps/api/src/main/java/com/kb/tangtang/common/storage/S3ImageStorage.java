package com.kb.tangtang.common.storage;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * AWS S3 저장소 (image.storage=s3). 배포 환경에서 쓴다.
 *
 * <p>{@link LocalImageStorage} 와 <b>키 규칙이 같다.</b> 그래서 저장소를 바꿔도 DB 의 기존 행을
 * 변환할 필요가 없다 — DB 에는 URL 이 아니라 키만 들어 있다({@link ImageStorage} 참고).
 * 현재 두 종류의 키가 이 저장소를 함께 쓴다.
 * <ul>
 *   <li>{@code profile/{userId}/{uuid}.jpg} — 프로필 이미지 (이슈 #150)</li>
 *   <li>{@code defense/{indictmentId}/{uuid}.jpg} — 변론 증빙 사진, 최대 3장 (이슈 #170)</li>
 * </ul>
 * <b>버킷 정책이 두 프리픽스를 모두 공개해야 한다.</b> {@code profile/*} 만 열면 증빙 사진이
 * 403 으로 안 보인다(이슈 #162 본문의 정책 예시가 프로필만 상정하고 있었다).
 *
 * <p>⚠ <b>업로드에 ACL(public-read)을 설정하지 않는다.</b> 버킷을 「ACL 비활성화」로 만들었기
 * 때문에 설정하면 {@code AccessControlListNotSupported} 로 실패한다. 공개는 버킷 정책이 맡는다.
 *
 * <p>⚠ <b>{@code contentType} 을 반드시 지정한다.</b> 빼면 S3 가
 * {@code application/octet-stream} 으로 저장해 브라우저가 이미지로 렌더링하지 않고 다운로드한다.
 * 서버가 항상 JPEG 로 다시 구워서 넣으므로({@link ImageProcessor}) 값은 상수다.
 *
 * <p>⚠ <b>{@code @Component} 를 붙이지 않는다.</b> 빈 등록은 {@link ImageStorageConfig} 한 곳에서만
 * 한다 — 여기에 붙이면 {@code LocalImageStorage} 와 함께 {@code ImageStorage} 빈이 둘이 되어
 * {@code NoUniqueBeanDefinitionException} 으로 기동이 실패한다.
 */
public class S3ImageStorage implements ImageStorage {

    /** 저장되는 것은 항상 JPEG 다 — ImageProcessor 가 원본 포맷과 무관하게 다시 굽는다. */
    private static final String CONTENT_TYPE = "image/jpeg";

    private final S3Client client;
    private final String bucket;
    private final String baseUrl;

    public S3ImageStorage(S3Client client, ImageStorageProperties properties) {
        this.client = client;
        this.bucket = properties.getS3Bucket();
        this.baseUrl = "https://" + properties.getS3Bucket()
                + ".s3." + properties.getS3Region() + ".amazonaws.com";
    }

    @Override
    public String store(byte[] content, String key) {
        client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(CONTENT_TYPE)
                        .build(),
                RequestBody.fromBytes(content));
        return key;
    }

    /**
     * {@inheritDoc}
     *
     * <p>S3 의 DeleteObject 는 <b>없는 키를 지워도 204 를 준다.</b> 멱등 요구가 저절로 지켜지므로
     * 존재 확인을 먼저 하지 않는다 — 확인하면 왕복이 두 번이 되고 그 사이 경합도 생긴다.
     */
    @Override
    public void delete(String key) {
        if (key == null) {
            return;
        }
        client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build());
    }

    /**
     * {@inheritDoc}
     *
     * <p>정적 URL 이다. 서명(presigned) URL 을 쓰지 않는 이유는 만료가 붙으면 브라우저 캐시가
     * 무효화되고, 화면이 URL 을 들고 있는 동안 만료될 수 있어서다. 대신 키에 UUID 가 들어가
     * 주소를 모르면 접근할 수 없다. (2026-08-19 결정 — 이슈 #162)
     */
    @Override
    public String urlOf(String key) {
        return key == null ? null : baseUrl + "/" + key;
    }

    /**
     * Spring 이 컨텍스트 종료 시 호출한다 — {@code @Bean} 은 기본으로 {@code close()} 를 추론해
     * 소멸 메서드로 잡는다({@code destroyMethod = INFER_METHOD}).
     *
     * <p>war 재배포 때 SDK 가 쥔 자원이 남으면 톰캣이 옛 웹앱 클래스로더를 놓지 못한다.
     */
    public void close() {
        client.close();
    }
}
