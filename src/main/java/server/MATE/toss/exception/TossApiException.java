package server.MATE.toss.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import server.MATE.global.common.exception.BaseException;
import server.MATE.toss.exception.parser.TossErrorContext;
import server.MATE.toss.response.TossErrorResponse;

@Getter
public class TossApiException extends BaseException {

    private final HttpStatusCode statusCode;
    private final TossErrorResponse error;
    private final String responseBody;

    public TossApiException(TossErrorCode errorCode, HttpStatusCode statusCode, TossErrorResponse error, String responseBody) {
        super(errorCode, error.reason());
        this.statusCode = statusCode;
        this.error = error;
        this.responseBody = responseBody;
    }

    public static TossApiException from(HttpStatusCode statusCode, TossErrorContext errorContext, String responseBody) {
        return new TossApiException(
                errorContext.errorCode(),
                statusCode,
                errorContext.errorResponse(),
                responseBody
        );
    }

    public TossApiException(TossErrorCode errorCode, String message, String responseBody) {
        super(errorCode, message);
        this.statusCode = HttpStatus.INTERNAL_SERVER_ERROR;
        this.error = null;
        this.responseBody = responseBody;
    }
}
