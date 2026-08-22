package com.kb.tangtang.common.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Paths;

/**
 * 저장소 설정 값 홀더.
 *
 * ⚠ **ServletConfig 가 이 빈을 주입받는다.** 서블릿 컨텍스트에는
 * PropertySourcesPlaceholderConfigurer 가 없어 거기서는 @Value 가 풀리지 않는다.
 * 값을 루트 컨텍스트의 빈에 담아두면 자식 컨텍스트가 @Autowired 로 받을 수 있다
 * (JwtAuthInterceptor 를 받는 것과 같은 경로다).
 */
@Component
public class ImageStorageProperties {

    @Value("${image.local.dir}")
    private String localDir;

    @Value("${image.local.base-url}")
    private String localBaseUrl;

    /**
     * S3 버킷 이름. **`image.storage=local` 일 때는 비어 있는 것이 정상이다** — 그래서 기본값을
     * 빈 문자열로 둔다. 여기서 값을 필수로 만들면 S3 를 쓰지 않는 팀원 전원이 버킷 이름을
     * 채워야 기동된다. 비어 있는지 검사는 s3 를 실제로 고를 때 {@link ImageStorageConfig} 가 한다.
     */
    @Value("${image.s3.bucket:}")
    private String s3Bucket;

    /** S3 리전. 버킷과 달리 팀이 서울 리전으로 고정이라 기본값을 둔다. */
    @Value("${image.s3.region:ap-northeast-2}")
    private String s3Region;

    /**
     * 정규화된 절대경로 문자열을 돌려준다. **정규화는 이 한 곳에서만 한다** —
     * ServletConfig(정적 리소스 핸들러)와 LocalImageStorage 가 각자 정규화하면, Windows 의
     * java.io.tmpdir 이 `\` 로 끝나는 탓에 `image.local.dir=${java.io.tmpdir}/tangtang-images`
     * 조합에서 `C:\...\Temp\/tangtang-images` 처럼 구분자가 섞인 문자열이 만들어지고, 두 곳이
     * 서로 다른 시점에 정규화하면 같은 디렉터리를 가리키면서도 문자열이 어긋날 수 있다.
     */
    public String getLocalDir() {
        return Paths.get(localDir).toAbsolutePath().normalize().toString();
    }

    public void setLocalDir(String localDir) {
        this.localDir = localDir;
    }

    public String getLocalBaseUrl() {
        return localBaseUrl;
    }

    public void setLocalBaseUrl(String localBaseUrl) {
        this.localBaseUrl = localBaseUrl;
    }

    public String getS3Bucket() {
        return s3Bucket;
    }

    public void setS3Bucket(String s3Bucket) {
        this.s3Bucket = s3Bucket;
    }

    public String getS3Region() {
        return s3Region;
    }

    public void setS3Region(String s3Region) {
        this.s3Region = s3Region;
    }
}
