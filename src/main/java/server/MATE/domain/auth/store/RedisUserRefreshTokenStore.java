package server.MATE.domain.auth.store;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisUserRefreshTokenStore extends RedisTokenStore implements UserRefreshTokenStore {

    public RedisUserRefreshTokenStore(StringRedisTemplate redisTemplate) {
        super(redisTemplate);
    }

    @Override
    public void save(Long userId, String refreshToken, long ttlMillis) {
        saveValue(key(userId), refreshToken, ttlMillis);
    }

    @Override
    public java.util.Optional<String> find(Long userId) {
        return findValue(key(userId));
    }

    @Override
    public void delete(Long userId) {
        deleteValue(key(userId));
    }

    private String key(Long userId) {
        return "auth:refresh:token:" + userId;
    }
}
