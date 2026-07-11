package server.MATE.toss.exception.parser;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import server.MATE.toss.exception.TossErrorCode;

class TossPromotionErrorResponseParserTest {

    private static final String EXECUTE_PATH = "/api-partner/v1/apps-in-toss/promotion/execute-promotion";
    private static final String GET_KEY_PATH = EXECUTE_PATH + "/get-key";
    private static final String RESULT_PATH = "/api-partner/v1/apps-in-toss/promotion/execution-result";

    private final TossPromotionErrorResponseParser parser = new TossPromotionErrorResponseParser(new ObjectMapper());

    @Test
    void parsesPromotionNotFound() {
        assertThat(parseErrorCode(EXECUTE_PATH, "4100")).isEqualTo(TossErrorCode.TOSS_010);
    }

    @Test
    void parsesPromotionNotRunning() {
        assertThat(parseErrorCode(EXECUTE_PATH, "4109")).isEqualTo(TossErrorCode.TOSS_011);
    }

    @Test
    void parsesExecutionFailed() {
        assertThat(parseErrorCode(EXECUTE_PATH, "4110")).isEqualTo(TossErrorCode.TOSS_012);
    }

    @Test
    void parsesExecutionResultNotFound() {
        assertThat(parseErrorCode(RESULT_PATH, "4111")).isEqualTo(TossErrorCode.TOSS_013);
    }

    @Test
    void parsesInsufficientBudget() {
        assertThat(parseErrorCode(EXECUTE_PATH, "4112")).isEqualTo(TossErrorCode.TOSS_014);
    }

    @Test
    void parsesAlreadyExecutedKey() {
        assertThat(parseErrorCode(EXECUTE_PATH, "4113")).isEqualTo(TossErrorCode.TOSS_015);
    }

    @Test
    void parsesSingleGrantLimitExceeded() {
        assertThat(parseErrorCode(EXECUTE_PATH, "4114")).isEqualTo(TossErrorCode.TOSS_016);
    }

    @Test
    void parsesTotalBudgetLimitExceeded() {
        assertThat(parseErrorCode(EXECUTE_PATH, "4116")).isEqualTo(TossErrorCode.TOSS_017);
    }

    @Test
    void parsesTextualErrorBody() {
        TossErrorContext context = parser.parse(
                HttpStatus.BAD_REQUEST,
                EXECUTE_PATH,
                """
                {
                  "error": "4112"
                }
                """
        );

        assertThat(context.errorCode()).isEqualTo(TossErrorCode.TOSS_014);
        assertThat(context.errorResponse().errorCode()).isEqualTo("4112");
    }

    @Test
    void usesEndpointSpecificFallbackReasonWhenReasonMissing() {
        TossErrorContext context = parser.parse(
                HttpStatus.BAD_REQUEST,
                GET_KEY_PATH,
                """
                {
                  "error": {
                    "errorCode": "4100"
                  }
                }
                """
        );

        assertThat(context.errorResponse().reason()).isEqualTo("토스 프로모션 지급 키 발급에 실패했습니다.");
    }

    @Test
    void parsesMalformedBodyAsFallbackError() {
        TossErrorContext context = parser.parse(
                HttpStatus.BAD_REQUEST,
                EXECUTE_PATH,
                "not-json"
        );

        assertThat(context.errorCode()).isEqualTo(TossErrorCode.TOSS_001);
        assertThat(context.errorResponse().errorCode()).isEqualTo("TOSS_PROMOTION_UNKNOWN_ERROR");
    }

    @Test
    void supportsOnlyTossPromotionApiPath() {
        assertThat(parser.supports(HttpStatus.BAD_REQUEST, EXECUTE_PATH, "{}")).isTrue();
        assertThat(parser.supports(HttpStatus.BAD_REQUEST, "/api-partner/v1/apps-in-toss/user/oauth2/login-me", "{}")).isFalse();
    }

    private TossErrorCode parseErrorCode(String path, String numericErrorCode) {
        TossErrorContext context = parser.parse(
                HttpStatus.BAD_REQUEST,
                path,
                """
                {
                  "error": {
                    "errorCode": "%s",
                    "reason": "테스트 사유"
                  }
                }
                """.formatted(numericErrorCode)
        );
        return context.errorCode();
    }
}
