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
@Order(Ordered.LOWEST_PRECEDENCE)
@Component
@RequiredArgsConstructor
public class DefaultTossErrorResponseParser implements TossErrorResponseParser {

    private static final String UNKNOWN_ERROR_CODE = "TOSS_UNKNOWN_ERROR";

    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(HttpStatusCode statusCode, String path, String responseBody) {
        return true;
    }

    @Override
    public TossErrorContext parse(HttpStatusCode statusCode, String path, String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode error = root.path("error");

            if (error.isObject()) {
                TossErrorResponse errorResponse = new TossErrorResponse(
                        textOrDefault(error.path("errorCode"), UNKNOWN_ERROR_CODE),
                        textOrDefault(error.path("reason"), "Toss API request failed.")
                );
                return result(TossErrorCode.from(errorResponse.errorCode()), errorResponse);
            }

            if (error.isTextual()) {
                TossErrorResponse errorResponse = new TossErrorResponse(
                        error.asText(UNKNOWN_ERROR_CODE),
                        textOrDefault(root.path("error_description"), error.asText())
                );
                return result(TossErrorCode.from(errorResponse.errorCode()), errorResponse);
            }
        } catch (Exception e) {
            log.warn("토스 에러 응답 파싱 실패: path={}, status={}", path, statusCode.value(), e);
        }

        return result(TossErrorCode.TOSS_001, new TossErrorResponse(
                UNKNOWN_ERROR_CODE,
                "Toss API request failed with status " + statusCode.value() + "."
        ));
    }

    private String textOrDefault(JsonNode node, String defaultValue) {
        if (node == null || node.isMissingNode() || node.isNull() || node.asText().isBlank()) {
            return defaultValue;
        }
        return node.asText();
    }

    private TossErrorContext result(TossErrorCode errorCode, TossErrorResponse errorResponse) {
        return new TossErrorContext(errorCode, errorResponse);
    }
}
