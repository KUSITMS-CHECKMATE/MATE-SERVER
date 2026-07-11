package server.MATE.toss.exception.parser;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpStatusCode;
import server.MATE.toss.exception.TossErrorCode;
import server.MATE.toss.response.TossErrorResponse;

public interface TossErrorResponseParser {

    boolean supports(HttpStatusCode statusCode, String path, String responseBody);

    TossErrorContext parse(HttpStatusCode statusCode, String path, String responseBody);

    default String textOrDefault(JsonNode node, String defaultValue) {
        if (node == null || node.isMissingNode() || node.isNull() || node.asText().isBlank()) {
            return defaultValue;
        }
        return node.asText();
    }

    default TossErrorContext result(TossErrorCode errorCode, TossErrorResponse errorResponse) {
        return new TossErrorContext(errorCode, errorResponse);
    }
}
