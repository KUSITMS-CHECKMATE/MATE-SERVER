package server.MATE.domain.auth.jwt;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

import java.util.EnumMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtProviderTest {

    private final JwtProvider jwtProvider = new JwtProvider(createJwtProperties());

    @Test
    @DisplayName("토큰을 생성하면 subject, role, tokenType claim을 다시 추출할 수 있다")
    void generateTokenAndExtractClaims() {
        String token = jwtProvider.generateToken(1L, "USER", TokenType.ACCESS);

        Claims claims = jwtProvider.parseClaims(token);

        assertThat(claims.getSubject()).isEqualTo("1");
        assertThat(claims.get("role", String.class)).isEqualTo("USER");
        assertThat(claims.get("tokenType", String.class)).isEqualTo("ACCESS");
        assertThat(jwtProvider.extractUserId(token)).isEqualTo(1L);
        assertThat(jwtProvider.extractTokenType(token)).isEqualTo(TokenType.ACCESS);
    }

    @Test
    @DisplayName("토큰 타입이 기대값과 같으면 예외가 발생하지 않는다")
    void validateTokenTypeWhenExpectedTokenTypeMatches() {
        String token = jwtProvider.generateToken(1L, "USER", TokenType.ACCESS);

        jwtProvider.validateTokenTypeOrThrow(token, TokenType.ACCESS);
    }

    @Test
    @DisplayName("토큰 타입이 기대값과 다르면 AUTH_003 예외를 던진다")
    void validateTokenTypeWhenExpectedTokenTypeDoesNotMatch() {
        String token = jwtProvider.generateToken(1L, "USER", TokenType.REFRESH);

        assertThatThrownBy(() -> jwtProvider.validateTokenTypeOrThrow(token, TokenType.ACCESS))
                .isInstanceOf(BaseException.class)
                .extracting(exception -> ((BaseException) exception).getErrorCode())
                .isEqualTo(BaseErrorCode.AUTH_003);
    }

    private JwtProperties createJwtProperties() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("mate-local-jwt-secret-key-must-be-at-least-32bytes");

        JwtProperties.TokenProperty access = new JwtProperties.TokenProperty();
        access.setExpiration(3_600_000L);

        JwtProperties.TokenProperty refresh = new JwtProperties.TokenProperty();
        refresh.setExpiration(1_209_600_000L);

        Map<TokenType, JwtProperties.TokenProperty> tokens = new EnumMap<>(TokenType.class);
        tokens.put(TokenType.ACCESS, access);
        tokens.put(TokenType.REFRESH, refresh);
        properties.setTokens(tokens);

        return properties;
    }
}
