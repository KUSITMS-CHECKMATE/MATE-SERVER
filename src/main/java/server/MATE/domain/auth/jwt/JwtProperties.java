package server.MATE.domain.auth.jwt;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

import java.util.EnumMap;
import java.util.Map;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String secret;

    private Map<TokenType, TokenProperty> tokens = new EnumMap<>(TokenType.class);

    public long getExpiration(TokenType tokenType) {
        TokenProperty property = tokens.get(tokenType);
        if (property == null) {
            throw new BaseException(BaseErrorCode.AUTH_005);
        }
        return property.getExpiration();
    }

    @Getter
    @Setter
    public static class TokenProperty {
        private long expiration;
    }
}
