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
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@Component
@RequiredArgsConstructor
public class TossPromotionErrorResponseParser implements TossErrorResponseParser {

    private static final String PROMOTION_API_PREFIX = "/api-partner/v1/apps-in-toss/promotion";
    private static final String GET_KEY_PATH = PROMOTION_API_PREFIX + "/execute-promotion/get-key";
    private static final String EXECUTE_PATH = PROMOTION_API_PREFIX + "/execute-promotion";
    private static final String RESULT_PATH = PROMOTION_API_PREFIX + "/execution-result";
    private static final String UNKNOWN_PROMOTION_ERROR_CODE = "TOSS_PROMOTION_UNKNOWN_ERROR";

    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(HttpStatusCode statusCode, String path, String responseBody) {
        return path != null && path.startsWith(PROMOTION_API_PREFIX);
    }

    @Override
    public TossErrorContext parse(HttpStatusCode statusCode, String path, String responseBody) {
        String defaultReason = defaultReasonFor(path);
        if (responseBody == null || responseBody.isBlank()) {
            return result(TossErrorCode.TOSS_001, new TossErrorResponse(UNKNOWN_PROMOTION_ERROR_CODE, defaultReason));
        }

        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode error = root.path("error");

            if (error.isObject()) {
                TossErrorResponse errorResponse = new TossErrorResponse(
                        textOrDefault(error.path("errorCode"), UNKNOWN_PROMOTION_ERROR_CODE),
                        textOrDefault(error.path("reason"), defaultReason)
                );
                return result(TossErrorCode.from(errorResponse.errorCode()), errorResponse);
            }

            if (error.isTextual()) {
                String errorCode = textOrDefault(error, UNKNOWN_PROMOTION_ERROR_CODE);
                TossErrorResponse errorResponse = new TossErrorResponse(
                        errorCode,
                        textOrDefault(root.path("error_description"), defaultReason)
                );
                return result(TossErrorCode.from(errorCode), errorResponse);
            }
        } catch (Exception e) {
            log.warn("토스 프로모션 에러 응답 파싱 실패: path={}, status={}", path, statusCode.value(), e);
        }

        return result(TossErrorCode.TOSS_001, new TossErrorResponse(UNKNOWN_PROMOTION_ERROR_CODE, defaultReason));
    }

    private String defaultReasonFor(String path) {
        if (GET_KEY_PATH.equals(path)) {
            return "토스 프로모션 지급 키 발급에 실패했습니다.";
        }
        if (EXECUTE_PATH.equals(path)) {
            return "토스 프로모션 지급 실행에 실패했습니다.";
        }
        if (RESULT_PATH.equals(path)) {
            return "토스 프로모션 지급 결과 조회에 실패했습니다.";
        }
        return "토스 프로모션 API 호출에 실패했습니다.";
    }
}
