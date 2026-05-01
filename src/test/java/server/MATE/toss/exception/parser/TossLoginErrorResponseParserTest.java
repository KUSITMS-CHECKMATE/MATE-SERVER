package server.MATE.toss.exception.parser;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import server.MATE.toss.exception.TossErrorCode;

class TossLoginErrorResponseParserTest {

    private static final String LOGIN_API_PREFIX = "/api-partner/v1/apps-in-toss/user/oauth2";

    private final TossLoginErrorResponseParser parser = new TossLoginErrorResponseParser(new ObjectMapper());

    @Test
    void parseGenerateTokenInvalidGrantAsInvalidAuthorizationCode() {
        TossErrorContext context = parser.parse(
                HttpStatus.BAD_REQUEST,
                LOGIN_API_PREFIX + "/generate-token",
                """
                {
                  "error": "invalid_grant"
                }
                """
        );

        assertThat(context.errorCode()).isEqualTo(TossErrorCode.TOSS_003);
        assertThat(context.errorResponse().errorCode()).isEqualTo("INVALID_AUTHORIZATION_CODE");
        assertThat(context.errorResponse().reason()).isEqualTo("인가 코드가 만료되었거나 이미 사용되었습니다.");
    }

    @Test
    void parseRefreshTokenInvalidGrantAsInvalidRefreshToken() {
        TossErrorContext context = parser.parse(
                HttpStatus.BAD_REQUEST,
                LOGIN_API_PREFIX + "/refresh-token",
                """
                {
                  "error": "invalid_grant"
                }
                """
        );

        assertThat(context.errorCode()).isEqualTo(TossErrorCode.TOSS_004);
        assertThat(context.errorResponse().errorCode()).isEqualTo("INVALID_REFRESH_TOKEN");
        assertThat(context.errorResponse().reason()).isEqualTo("refresh token이 만료되었거나 유효하지 않습니다.");
    }

    @Test
    void supportsOnlyTossLoginApiPath() {
        assertThat(parser.supports(HttpStatus.BAD_REQUEST, LOGIN_API_PREFIX + "/generate-token", "{}")).isTrue();
        assertThat(parser.supports(HttpStatus.BAD_REQUEST, "/api-partner/v1/payments/confirm", "{}")).isFalse();
    }
}
