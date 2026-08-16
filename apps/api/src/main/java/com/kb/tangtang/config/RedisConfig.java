package com.kb.tangtang.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 그룹 채팅용 Redis 연결 (이슈 #174).
 *
 * <p>루트 컨텍스트의 빈이다. RootConfig 가 com.kb.tangtang 전체를 스캔하므로 여기에 둔다.
 * 채팅 메시지는 전부 JSON 문자열이라 StringRedisTemplate 하나면 충분하다 —
 * 값 직렬화기를 갈아끼울 일이 없어 RedisTemplate<String, Object> 를 따로 두지 않는다.
 *
 * <p>spring-boot-starter-data-redis 를 쓰지 않는다(평가 감점). 자동 설정이 없으므로
 * 커넥션 팩토리를 직접 만든다.
 */
@Configuration
public class RedisConfig {

    private final String host;
    private final int port;

    public RedisConfig(@Value("${redis.host:localhost}") String host,
                       @Value("${redis.port:6379}") int port) {
        this.host = host;
        this.port = port;
    }

    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory(new RedisStandaloneConfiguration(host, port));
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(LettuceConnectionFactory factory) {
        return new StringRedisTemplate(factory);
    }
}
