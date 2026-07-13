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
    TOSS_005(HttpStatus.UNAUTHORIZED, "TOSS_005", "토스 access token이 유효하지 않습니다."),
    TOSS_006(HttpStatus.BAD_GATEWAY, "TOSS_006", "토스 내부 서버 오류가 발생했습니다."),
    TOSS_007(HttpStatus.NOT_FOUND, "TOSS_007", "토스 userKey를 찾을 수 없습니다."),
    TOSS_008(HttpStatus.NOT_FOUND, "TOSS_008", "토스 사용자 정보를 찾을 수 없습니다."),
    TOSS_009(HttpStatus.TOO_MANY_REQUESTS, "TOSS_009", "토스 인증서 조회 가능 횟수를 초과했습니다."),
    TOSS_010(HttpStatus.BAD_GATEWAY, "TOSS_010", "토스 프로모션 정보를 찾을 수 없습니다."),
    TOSS_011(HttpStatus.BAD_GATEWAY, "TOSS_011", "토스 프로모션이 실행 중이 아닙니다."),
    // 4110: 토스 문서상 재지급 권장 코드지만 자동 retry는 미구현, 즉시 실패 처리한다.
    TOSS_012(HttpStatus.BAD_GATEWAY, "TOSS_012", "토스 프로모션 실행에 실패했습니다."),
    // 4111: eventual consistency로 인한 지연일 수 있음, 즉시 실패 처리한다.
    TOSS_013(HttpStatus.BAD_GATEWAY, "TOSS_013", "토스 프로모션 지급 내역을 찾을 수 없습니다."),
    TOSS_014(HttpStatus.BAD_GATEWAY, "TOSS_014", "토스 프로모션 예산이 부족합니다."),
    TOSS_015(HttpStatus.CONFLICT, "TOSS_015", "토스 프로모션 실행 키가 이미 사용되었습니다."),
    TOSS_016(HttpStatus.BAD_GATEWAY, "TOSS_016", "토스 프로모션 1회 지급 금액 한도를 초과했습니다."),
    TOSS_017(HttpStatus.BAD_GATEWAY, "TOSS_017", "토스 프로모션 총 예산 한도를 초과했습니다.");

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
            case "INTERNAL_ERROR" -> TOSS_006;
            case "USER_KEY_NOT_FOUND" -> TOSS_007;
            case "USER_NOT_FOUND" -> TOSS_008;
            case "BAD_REQUEST_RETRIEVE_CERT_RESULT_EXCEEDED_LIMIT" -> TOSS_009;
            case "4100" -> TOSS_010;
            case "4104", "4105", "4108", "4109" -> TOSS_011;
            case "4110" -> TOSS_012;
            case "4111" -> TOSS_013;
            case "4112" -> TOSS_014;
            case "4113" -> TOSS_015;
            case "4114" -> TOSS_016;
            case "4116" -> TOSS_017;
            default -> TOSS_001;
        };
    }
}
