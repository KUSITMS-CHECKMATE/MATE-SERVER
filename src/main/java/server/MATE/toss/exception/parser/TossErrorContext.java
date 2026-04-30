package server.MATE.toss.exception.parser;

import server.MATE.toss.exception.TossErrorCode;
import server.MATE.toss.response.TossErrorResponse;

public record TossErrorContext(
        TossErrorCode errorCode,
        TossErrorResponse errorResponse
) {
}
