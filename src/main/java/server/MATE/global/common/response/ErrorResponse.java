package server.MATE.global.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import server.MATE.global.common.exception.ErrorCode;

public record ErrorResponse(
        boolean success,
        String code,
        String message,
        @JsonInclude(JsonInclude.Include.NON_NULL) String field
) {
    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(false, errorCode.getCode(), errorCode.getMessage(), null);
    }

    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return new ErrorResponse(false, errorCode.getCode(), message, null);
    }

    public static ErrorResponse of(ErrorCode errorCode, String message, String field) {
        return new ErrorResponse(false, errorCode.getCode(), message, field);
    }
}
