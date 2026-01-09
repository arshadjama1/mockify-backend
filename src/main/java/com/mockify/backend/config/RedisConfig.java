package com.mockify.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    /**
     * Configures RedisTemplate Bean for:
      - Uses String keys (easy to read/debug in redis-cli)
      - Stores values as JSON (using Jackson)
      - Ensures RedisTemplate is fully initialized before use
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory connectionFactory) {

        RedisTemplate<String, Object> template = new RedisTemplate<>();

        // Set Redis connection (host, port come from application.yml)
        template.setConnectionFactory(connectionFactory);

        // Store keys as readable strings
        template.setKeySerializer(new StringRedisSerializer());

        // Store values as JSON instead of Java binary format
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());

        // Finalize and validate the RedisTemplate configuration
        template.afterPropertiesSet();

        return template;
    }
}
