package server.MATE.toss.exception.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import server.MATE.toss.exception.TossErrorCode;
import server.MATE.toss.response.TossErrorResponse;

@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
@RequiredArgsConstructor
public class TossLoginErrorResponseParser implements TossErrorResponseParser {

    private static final String LOGIN_API_PREFIX = "/api-partner/v1/apps-in-toss/user/oauth2";
    private static final String GENERATE_TOKEN_PATH = LOGIN_API_PREFIX + "/generate-token";
    private static final String REFRESH_TOKEN_PATH = LOGIN_API_PREFIX + "/refresh-token";
    private static final String LOGIN_ME_PATH = LOGIN_API_PREFIX + "/login-me";
    private static final String UNKNOWN_LOGIN_ERROR_CODE = "TOSS_LOGIN_UNKNOWN_ERROR";
    private static final String INVALID_GRANT = "invalid_grant";
    private static final String INVALID_AUTHORIZATION_CODE = "INVALID_AUTHORIZATION_CODE";
    private static final String INVALID_REFRESH_TOKEN = "INVALID_REFRESH_TOKEN";
    private static final String INVALID_ACCESS_TOKEN = "INVALID_ACCESS_TOKEN";

    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(HttpStatusCode statusCode, String path, String responseBody) {
        return path != null && path.startsWith(LOGIN_API_PREFIX);
    }

    @Override
    public TossErrorContext parse(HttpStatusCode statusCode, String path, String responseBody) {
        if (GENERATE_TOKEN_PATH.equals(path)) {
            return parseGenerateTokenError(statusCode, path, responseBody);
        }
        if (REFRESH_TOKEN_PATH.equals(path)) {
            return parseRefreshTokenError(statusCode, path, responseBody);
        }
        if (LOGIN_ME_PATH.equals(path)) {
            return parseLoginMeError(statusCode, path, responseBody);
        }
        return parseDefaultLoginError(statusCode, path, responseBody);
    }

    private TossErrorContext parseGenerateTokenError(HttpStatusCode statusCode, String path, String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);

            if (root.path("error").isTextual()) {
                String errorCode = root.path("error").asText(UNKNOWN_LOGIN_ERROR_CODE);
                if (INVALID_GRANT.equals(errorCode)) {
                    return result(TossErrorCode.TOSS_003, new TossErrorResponse(
                            INVALID_AUTHORIZATION_CODE,
                            "인가 코드가 만료되었거나 이미 사용되었습니다."
                    ));
                }
                TossErrorResponse errorResponse = new TossErrorResponse(
                        errorCode,
                        root.path("error_description").asText(errorCode)
                );
                return result(TossErrorCode.from(errorResponse.errorCode()), errorResponse);
            }

            JsonNode error = root.path("error");
            if (error.isObject()) {
                TossErrorResponse errorResponse = new TossErrorResponse(
                        textOrDefault(error.path("errorCode"), UNKNOWN_LOGIN_ERROR_CODE),
                        textOrDefault(error.path("reason"), "토스 로그인 access token 발급에 실패했습니다.")
                );
                return result(TossErrorCode.from(errorResponse.errorCode()), errorResponse);
            }
        } catch (Exception e) {
            log.warn("토스 로그인 generate-token 에러 응답 파싱 실패: path={}, status={}", path, statusCode.value(), e);
        }

        return result(TossErrorCode.TOSS_001, new TossErrorResponse(
                UNKNOWN_LOGIN_ERROR_CODE,
                "토스 로그인 access token 발급에 실패했습니다."
        ));
    }

    private TossErrorContext parseRefreshTokenError(HttpStatusCode statusCode, String path, String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);

            if (root.path("error").isTextual()) {
                String errorCode = root.path("error").asText(UNKNOWN_LOGIN_ERROR_CODE);
                if (INVALID_GRANT.equals(errorCode)) {
                    return result(TossErrorCode.TOSS_004, new TossErrorResponse(
                            INVALID_REFRESH_TOKEN,
                            "refresh token이 만료되었거나 유효하지 않습니다."
                    ));
                }
                TossErrorResponse errorResponse = new TossErrorResponse(
                        errorCode,
                        root.path("error_description").asText(errorCode)
                );
                return result(TossErrorCode.from(errorResponse.errorCode()), errorResponse);
            }

            JsonNode error = root.path("error");
            if (error.isObject()) {
                TossErrorResponse errorResponse = new TossErrorResponse(
                        textOrDefault(error.path("errorCode"), UNKNOWN_LOGIN_ERROR_CODE),
                        textOrDefault(error.path("reason"), "토스 로그인 access token 재발급에 실패했습니다.")
                );
                return result(TossErrorCode.from(errorResponse.errorCode()), errorResponse);
            }
        } catch (Exception e) {
            log.warn("토스 로그인 refresh-token 에러 응답 파싱 실패: path={}, status={}", path, statusCode.value(), e);
        }

        return result(TossErrorCode.TOSS_001, new TossErrorResponse(
                UNKNOWN_LOGIN_ERROR_CODE,
                "토스 로그인 access token 재발급에 실패했습니다."
        ));
    }

    private TossErrorContext parseDefaultLoginError(HttpStatusCode statusCode, String path, String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);

            if (root.path("error").isTextual()) {
                String errorCode = root.path("error").asText(UNKNOWN_LOGIN_ERROR_CODE);
                TossErrorResponse errorResponse = new TossErrorResponse(
                        errorCode,
                        root.path("error_description").asText(errorCode)
                );
                return result(TossErrorCode.from(errorResponse.errorCode()), errorResponse);
            }

            JsonNode error = root.path("error");
            if (error.isObject()) {
                TossErrorResponse errorResponse = new TossErrorResponse(
                        textOrDefault(error.path("errorCode"), UNKNOWN_LOGIN_ERROR_CODE),
                        textOrDefault(error.path("reason"), "토스 로그인 API 호출에 실패했습니다.")
                );
                return result(TossErrorCode.from(errorResponse.errorCode()), errorResponse);
            }
        } catch (Exception e) {
            log.warn("토스 로그인 에러 응답 파싱 실패: path={}, status={}", path, statusCode.value(), e);
        }

        return result(TossErrorCode.TOSS_001, new TossErrorResponse(
                UNKNOWN_LOGIN_ERROR_CODE,
                "토스 로그인 API 호출에 실패했습니다."
        ));
    }

    private TossErrorContext parseLoginMeError(HttpStatusCode statusCode, String path, String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);

            if (root.path("error").isTextual()) {
                String errorCode = root.path("error").asText(UNKNOWN_LOGIN_ERROR_CODE);
                if (INVALID_GRANT.equals(errorCode)) {
                    return result(TossErrorCode.TOSS_005, new TossErrorResponse(
                            INVALID_ACCESS_TOKEN,
                            "access token이 만료되었거나 유효하지 않습니다."
                    ));
                }
                TossErrorResponse errorResponse = new TossErrorResponse(
                        errorCode,
                        root.path("error_description").asText(errorCode)
                );
                return result(TossErrorCode.from(errorResponse.errorCode()), errorResponse);
            }

            JsonNode error = root.path("error");
            if (error.isObject()) {
                TossErrorResponse errorResponse = new TossErrorResponse(
                        textOrDefault(error.path("errorCode"), UNKNOWN_LOGIN_ERROR_CODE),
                        textOrDefault(error.path("reason"), "토스 로그인 사용자 정보를 조회할 수 없습니다.")
                );
                return result(TossErrorCode.from(errorResponse.errorCode()), errorResponse);
            }
        } catch (Exception e) {
            log.warn("토스 로그인 login-me 에러 응답 파싱 실패: path={}, status={}", path, statusCode.value(), e);
        }

        return result(TossErrorCode.TOSS_001, new TossErrorResponse(
                UNKNOWN_LOGIN_ERROR_CODE,
                "토스 로그인 사용자 정보를 조회할 수 없습니다."
        ));
    }

}
