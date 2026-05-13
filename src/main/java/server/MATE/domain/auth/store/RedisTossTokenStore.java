package server.MATE.domain.auth.store;

import java.util.Optional;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisTossTokenStore extends RedisTokenStore implements TossTokenStore {

    public RedisTossTokenStore(StringRedisTemplate redisTemplate) {
        super(redisTemplate);
    }

    @Override
    public void saveAccessToken(Long userId, String accessToken, long ttlMillis) {
        saveValue(accessKey(userId), accessToken, ttlMillis);
    }

    @Override
    public Optional<String> findAccessToken(Long userId) {
        return findValue(accessKey(userId));
    }

    @Override
    public void deleteAccessToken(Long userId) {
        deleteValue(accessKey(userId));
    }

    @Override
    public void saveRefreshToken(Long userId, String refreshToken, long ttlMillis) {
        saveValue(refreshKey(userId), refreshToken, ttlMillis);
    }

    @Override
    public Optional<String> findRefreshToken(Long userId) {
        return findValue(refreshKey(userId));
    }

    @Override
    public void deleteRefreshToken(Long userId) {
        deleteValue(refreshKey(userId));
    }

    private String accessKey(Long userId) {
        return "auth:toss:access:" + userId;
    }

    private String refreshKey(Long userId) {
        return "auth:toss:refresh:" + userId;
    }
}
