package com.kb.tangtang.user.mapper;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * DB 없이 XML 자체만 검증한다. 매퍼 XML 이 깨지면 컨텍스트가 통째로 기동하지 못하므로
 * 실행 전에 여기서 걸리게 한다. (UserMapperTest 는 실제 DB 가 필요한 통합 테스트라 별개다)
 */
class UserMapperXmlTest {

    private static final String RESOURCE = "mapper/user/UserMapper.xml";

    @Test
    @DisplayName("User Mapper XML 이 파싱되고 프로필 이미지 갱신문이 등록된다")
    void parsesMapperXml() throws Exception {
        Configuration configuration = new Configuration();

        try (InputStream inputStream = Resources.getResourceAsStream(RESOURCE)) {
            new XMLMapperBuilder(inputStream, configuration, RESOURCE,
                    configuration.getSqlFragments()).parse();
        }

        String namespace = UserMapper.class.getName();
        assertTrue(configuration.hasStatement(namespace + ".findById"));
        assertTrue(configuration.hasStatement(namespace + ".updateProfileImageKey"));
    }
}
