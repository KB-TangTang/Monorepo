package com.kb.tangtang.user.service;

import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.common.storage.ImageStorage;
import com.kb.tangtang.user.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 프로필 이미지 키 갱신의 <b>DB 쓰기 부분만</b> 담는 짧은 트랜잭션 (이슈 #318).
 *
 * <p><b>왜 별도 빈인가.</b> {@link UserService#updateProfileImage} 는 S3 업로드를 트랜잭션 밖에서
 * 하려고 {@code @Transactional} 을 뗐다. 그렇다고 같은 클래스의 메서드를 {@code this.apply(...)} 로
 * 부르면 <b>Spring AOP 프록시를 거치지 않아 애노테이션이 통째로 무시된다</b> — 트랜잭션 없이
 * UPDATE 가 나가고 {@link #deleteAfterCommit} 이 기댈 커밋 시점도 사라진다.
 * 자기호출(self-invocation) 함정이라 컴파일러도 테스트도 조용히 지나간다.
 * 그래서 <b>다른 빈</b>으로 빼 프록시를 반드시 타게 한다({@code ProfileImageWriterTest} 가 이걸 지킨다).
 *
 * <p>두 메서드 모두 {@code REQUIRED} 다. {@link UserService#deleteProfileImage} 처럼 이미
 * 트랜잭션 안에서 부르는 쪽은 그 트랜잭션에 합류한다 — 동작이 달라지지 않는다.
 */
@Service
public class ProfileImageWriter {

    private static final Logger log = LoggerFactory.getLogger(ProfileImageWriter.class);

    private final UserMapper userMapper;
    private final ImageStorage imageStorage;

    public ProfileImageWriter(UserMapper userMapper, ImageStorage imageStorage) {
        this.userMapper = userMapper;
        this.imageStorage = imageStorage;
    }

    /**
     * 업로드 경로 — 새 키로 바꾸고 옛 파일을 커밋 뒤에 지운다.
     *
     * @param newKey 이미 저장소에 올라간 키. 여기서는 업로드하지 않는다
     * @param oldKey 지울 옛 키. 없으면 NULL
     */
    @Transactional
    public void apply(long userId, String newKey, String oldKey) {
        if (userMapper.updateProfileImageKey(userId, newKey) == 0) {
            throw new BusinessException("NOT_FOUND", "사용자를 찾을 수 없습니다.");
        }
        deleteAfterCommit(oldKey);
    }

    /** 삭제 경로 — 키를 비우고 옛 파일을 커밋 뒤에 지운다. */
    @Transactional
    public void clear(long userId, String oldKey) {
        if (userMapper.updateProfileImageKey(userId, null) == 0) {
            throw new BusinessException("NOT_FOUND", "사용자를 찾을 수 없습니다.");
        }
        deleteAfterCommit(oldKey);
    }

    /**
     * 옛 이미지 삭제를 <b>커밋이 끝난 뒤</b>로 미룬다. 두 가지를 동시에 막는다.
     *
     * <ol>
     *   <li><b>깨진 사진.</b> 트랜잭션 안에서 지우면 그 뒤 커밋이 실패했을 때 DB 에는 옛 키가
     *       남았는데 파일은 이미 없는 상태가 된다. 화면에 엑스박스가 뜨고 되돌릴 방법이 없다.</li>
     *   <li><b>DB 커넥션 점유.</b> S3 저장소({@code image.storage=s3})에서 삭제는 네트워크 왕복이다.
     *       트랜잭션 안에 두면 커넥션을 쥔 채 S3 응답을 기다리게 되고, 사용자가 몰리면
     *       HikariCP 풀이 마른다.</li>
     * </ol>
     *
     * <p>커밋 뒤에 지우므로 <b>삭제가 실패하면 고아 파일이 남는다.</b> 사용자 요청은 이미 성공한
     * 뒤이고 되돌릴 수도 없으므로 예외를 삼킨다 — 여기서 던지면 커밋된 작업이 500 으로 보인다.
     * 고아 파일은 용량만 차지할 뿐 화면·정합성에 영향이 없다.
     *
     * <p>트랜잭션이 없는 호출(테스트 등)에서는 동기화를 걸 수 없으므로 그 자리에서 지운다.
     */
    private void deleteAfterCommit(String key) {
        if (key == null) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            imageStorage.delete(key);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    imageStorage.delete(key);
                } catch (RuntimeException e) {
                    log.warn("옛 프로필 이미지 삭제 실패 — 고아 파일이 남습니다. key={}", key, e);
                }
            }
        });
    }
}
