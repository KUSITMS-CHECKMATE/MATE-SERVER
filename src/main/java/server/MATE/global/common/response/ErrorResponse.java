package server.MATE.global.common.response;

import server.MATE.global.common.exception.ErrorCode;

public record ErrorResponse(
        boolean success,
        String code,
        String message
) {
    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(false, errorCode.getCode(), errorCode.getMessage());
    }

    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return new ErrorResponse(false, errorCode.getCode(), message);
    }
}
