package server.MATE.domain.auth.store;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import server.MATE.domain.auth.crypto.TokenEncryptionProperties;
import server.MATE.domain.auth.crypto.TokenEncryptor;
@SpringBootTest(classes = RedisTokenStoreContextTest.TestConfig.class)
@ActiveProfiles("test")
class RedisTokenStoreContextTest {

    private static final String VALID_BASE64_KEY = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    @TestConfiguration
    @Import({
            RedisTossTokenStore.class,
            RedisUserRefreshTokenStore.class,
            TokenEncryptor.class
    })
    static class TestConfig {

        @Bean
        TokenEncryptionProperties tokenEncryptionProperties() {
            return new TokenEncryptionProperties(VALID_BASE64_KEY);
        }
    }

    @MockitoBean
    private StringRedisTemplate redisTemplate;

    @MockitoBean
    @SuppressWarnings("unchecked")
    private ValueOperations<String, String> valueOperations;

    @Autowired
    private RedisTossTokenStore redisTossTokenStore;

    @Autowired
    private RedisUserRefreshTokenStore redisUserRefreshTokenStore;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("RedisTossTokenStore는 access token key와 TTL을 예상한 방식으로 사용한다")
    void savesAccessTokenWithExpectedKeyAndTtl() {
        redisTossTokenStore.saveAccessToken(1L, "access-token", 123_000L);

        verify(valueOperations).set("auth:toss:access:1", "access-token", 123_000L, TimeUnit.MILLISECONDS);
    }

    @Test
    @DisplayName("RedisTossTokenStore는 refresh token을 암호화해서 저장하고 복호화해서 조회한다")
    void savesEncryptedRefreshTokenAndFindsDecryptedValue() {
        redisTossTokenStore.saveRefreshToken(1L, "refresh-token", 456_000L);

        org.mockito.ArgumentCaptor<String> storedValue = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(
                org.mockito.ArgumentMatchers.eq("auth:toss:refresh:1"),
                storedValue.capture(),
                org.mockito.ArgumentMatchers.eq(456_000L),
                org.mockito.ArgumentMatchers.eq(TimeUnit.MILLISECONDS)
        );

        assertThat(storedValue.getValue()).isNotEqualTo("refresh-token");
        when(valueOperations.get("auth:toss:refresh:1")).thenReturn(storedValue.getValue());

        Optional<String> foundRefreshToken = redisTossTokenStore.findRefreshToken(1L);

        assertThat(foundRefreshToken).contains("refresh-token");
    }

    @Test
    @DisplayName("RedisUserRefreshTokenStore는 사용자 refresh token key와 TTL을 예상한 방식으로 사용한다")
    void savesUserRefreshTokenWithExpectedKeyAndTtl() {
        redisUserRefreshTokenStore.save(1L, "mate-refresh-token", 789_000L);

        verify(valueOperations).set("auth:refresh:token:1", "mate-refresh-token", 789_000L, TimeUnit.MILLISECONDS);
    }
}
