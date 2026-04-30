package server.MATE.toss.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import server.MATE.global.common.exception.ErrorCode;

@Getter
@RequiredArgsConstructor
public enum TossErrorCode implements ErrorCode {

    TOSS_001(HttpStatus.BAD_GATEWAY, "TOSS_001", "토스 API 호출에 실패했습니다."),
    TOSS_002(HttpStatus.INTERNAL_SERVER_ERROR, "TOSS_002", "토스 API 응답을 해석할 수 없습니다."),
    TOSS_003(HttpStatus.UNAUTHORIZED, "TOSS_003", "토스 인가 코드가 유효하지 않습니다."),
    TOSS_004(HttpStatus.UNAUTHORIZED, "TOSS_004", "토스 refresh token이 유효하지 않습니다."),
    TOSS_005(HttpStatus.UNAUTHORIZED, "TOSS_005", "토스 access token이 유효하지 않습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    public static TossErrorCode from(String externalErrorCode) {
        if (externalErrorCode == null || externalErrorCode.isBlank()) {
            return TOSS_001;
        }

        return switch (externalErrorCode) {
            case "INVALID_AUTHORIZATION_CODE", "invalid_grant" -> TOSS_003;
            case "INVALID_REFRESH_TOKEN" -> TOSS_004;
            case "INVALID_ACCESS_TOKEN", "invalid_token", "UNAUTHORIZED", "ACCESS_DENIED" -> TOSS_005;
            default -> TOSS_001;
        };
    }
}
