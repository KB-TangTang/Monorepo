package com.kb.tangtang.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RedisConfigTest {

    @Test
    @DisplayName("설정한 호스트·포트로 커넥션 팩토리를 만든다")
    void createsConnectionFactoryWithConfiguredHostAndPort() {
        RedisConfig config = new RedisConfig("some-host", 6380);

        LettuceConnectionFactory factory = config.redisConnectionFactory();

        assertEquals("some-host", factory.getHostName());
        assertEquals(6380, factory.getPort());
    }

    @Test
    @DisplayName("StringRedisTemplate 은 같은 커넥션 팩토리를 쓴다")
    void templateSharesConnectionFactory() {
        RedisConfig config = new RedisConfig("localhost", 6379);
        LettuceConnectionFactory factory = config.redisConnectionFactory();

        assertNotNull(config.stringRedisTemplate(factory).getConnectionFactory());
        assertEquals(factory, config.stringRedisTemplate(factory).getConnectionFactory());
    }
}
