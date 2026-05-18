package server.MATE.domain.auth.store;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;

public abstract class RedisTokenStore {

    private final StringRedisTemplate redisTemplate;

    protected RedisTokenStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    protected void saveValue(String key, String value, long ttlMillis) {
        redisTemplate.opsForValue().set(key, value, ttlMillis, TimeUnit.MILLISECONDS);
    }

    protected void saveValue(String key, String value) {
        redisTemplate.opsForValue().set(key, value);
    }

    protected Optional<String> findValue(String key) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(key));
    }

    protected void deleteValue(String key) {
        redisTemplate.delete(key);
    }
}
