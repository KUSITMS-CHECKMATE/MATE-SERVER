package server.MATE.domain.auth.store;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import server.MATE.domain.auth.crypto.TokenEncryptionProperties;
import server.MATE.domain.auth.crypto.TokenEncryptor;

@ExtendWith(MockitoExtension.class)
class RedisTossTokenStoreTest {

    private static final String VALID_BASE64_KEY = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedisTossTokenStore redisTossTokenStore;

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        redisTossTokenStore = new RedisTossTokenStore(redisTemplate, new TokenEncryptor(new TokenEncryptionProperties(VALID_BASE64_KEY)));
    }

    @Test
    @DisplayName("access token은 평문으로 조회된다")
    void savesAndFindsAccessTokenAsPlainText() {
        redisTossTokenStore.saveAccessToken(1L, "plain-access", 300_000L);
        when(valueOperations.get("auth:toss:access:1")).thenReturn("plain-access");

        Optional<String> result = redisTossTokenStore.findAccessToken(1L);

        verify(valueOperations).set("auth:toss:access:1", "plain-access", 300_000L, TimeUnit.MILLISECONDS);
        assertThat(result).contains("plain-access");
    }

    @Test
    @DisplayName("refresh token은 Redis raw value가 평문과 다르게 저장되고 조회 시 복호화된다")
    void storesEncryptedRefreshTokenAndDecryptsWhenFinding() {
        redisTossTokenStore.saveRefreshToken(1L, "plain-refresh", 600_000L);

        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(
                org.mockito.ArgumentMatchers.eq("auth:toss:refresh:1"),
                valueCaptor.capture(),
                org.mockito.ArgumentMatchers.eq(600_000L),
                org.mockito.ArgumentMatchers.eq(TimeUnit.MILLISECONDS)
        );

        String rawStoredValue = valueCaptor.getValue();
        assertThat(rawStoredValue).isNotEqualTo("plain-refresh");

        when(valueOperations.get("auth:toss:refresh:1")).thenReturn(rawStoredValue);

        Optional<String> result = redisTossTokenStore.findRefreshToken(1L);

        assertThat(result).contains("plain-refresh");
    }

    @Test
    @DisplayName("access/refresh token 삭제가 정상 동작한다")
    void deletesAccessAndRefreshTokens() {
        redisTossTokenStore.deleteAccessToken(1L);
        redisTossTokenStore.deleteRefreshToken(1L);

        verify(redisTemplate).delete("auth:toss:access:1");
        verify(redisTemplate).delete("auth:toss:refresh:1");
    }

    @Test
    @DisplayName("TTL이 저장 시 반영된다")
    void appliesTtlWhenSavingTokens() {
        redisTossTokenStore.saveAccessToken(1L, "plain-access", 123_000L);
        redisTossTokenStore.saveRefreshToken(1L, "plain-refresh", 456_000L);

        verify(valueOperations).set("auth:toss:access:1", "plain-access", 123_000L, TimeUnit.MILLISECONDS);
        verify(valueOperations).set(
                org.mockito.ArgumentMatchers.eq("auth:toss:refresh:1"),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.eq(456_000L),
                org.mockito.ArgumentMatchers.eq(TimeUnit.MILLISECONDS)
        );
    }
}
