package com.kb.tangtang.challenge.service;

import com.kb.tangtang.challenge.domain.Defense;
import com.kb.tangtang.challenge.mapper.DefenseMapper;
import com.kb.tangtang.challenge.mapper.IndictmentMapper;
import com.kb.tangtang.common.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 변론 등록의 <b>DB 쓰기 부분만</b> 담는 짧은 트랜잭션 (이슈 #318).
 *
 * <p><b>왜 별도 빈인가.</b> {@link DefenseService#registerDefense} 는 S3 업로드를 트랜잭션 밖에서
 * 하려고 클래스 수준의 {@code @Transactional} 을 뗐다. 그렇다고 저장 세 문장을 같은 클래스의
 * private 메서드에 {@code @Transactional} 을 달아 {@code this.save(...)} 로 부르면
 * <b>Spring AOP 프록시를 거치지 않아 애노테이션이 통째로 무시된다</b> — 트랜잭션 없이 INSERT 가
 * 나가고, {@code moveToVoting} 이 0행이어도 앞서 넣은 변론·이미지가 되돌아가지 않는다.
 * 자기호출(self-invocation) 함정이라 컴파일러도 테스트도 조용히 지나간다.
 * 그래서 <b>다른 빈</b>으로 빼 프록시를 반드시 타게 한다({@code DefenseWriterTest} 가 이걸 지킨다).
 *
 * <p>여기 세 문장은 한 트랜잭션에 남아야 한다. 상태 전이가 0행이면 예외로 변론 INSERT 까지
 * 함께 되돌리는 것이 {@code moveToVoting} 의 계약이다({@code IndictmentMapper#moveToVoting}).
 */
@Service
public class DefenseWriter {

    private final DefenseMapper defenseMapper;
    private final IndictmentMapper indictmentMapper;

    public DefenseWriter(DefenseMapper defenseMapper, IndictmentMapper indictmentMapper) {
        this.defenseMapper = defenseMapper;
        this.indictmentMapper = indictmentMapper;
    }

    /**
     * 변론 · 증빙 이미지 키 · 상태 전이를 한 트랜잭션에 쓴다.
     *
     * @param imageKeys 이미 저장소에 올라간 키들. 여기서는 업로드하지 않는다 —
     *                  네트워크 I/O 를 트랜잭션 안에 두지 않는 것이 이 클래스가 생긴 이유다
     * @throws BusinessException 상태가 이미 옮겨졌을 때. 이 트랜잭션 전체가 롤백된다
     */
    @Transactional
    public void save(Defense defense, List<String> imageKeys) {
        defenseMapper.insertDefense(defense);
        if (!imageKeys.isEmpty()) {
            defenseMapper.insertDefenseImages(defense.getId(), imageKeys);
        }

        /*
         * 여기까지 왔어도 상태가 이미 옮겨졌을 수 있다 — 호출부의 SELECT 검증과 이 UPDATE 사이에
         * 마감 배치가 끼어들거나 사용자가 제출을 두 번 누르면 그렇다. 0행이면 롤백한다.
         * (변론 INSERT 도 함께 되돌아간다. 트랜잭션이 하나다)
         */
        if (indictmentMapper.moveToVoting(defense.getIndictmentId()) == 0) {
            throw new BusinessException("DEFENSE_NOT_ALLOWED", "지금은 변론할 수 없는 재판입니다.");
        }
    }
}
