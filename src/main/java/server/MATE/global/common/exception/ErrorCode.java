package server.MATE.global.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 커스텀 에러 코드 네이밍 가이드: DOMAIN_NNN (예: AUTH_001, USER_001)

    COMMON_001(HttpStatus.NOT_FOUND, "COMMON_001", "요청하신 리소스를 찾을 수 없습니다."),
    COMMON_002(HttpStatus.BAD_REQUEST, "COMMON_002", "요청 값이 올바르지 않습니다."),
    COMMON_003(HttpStatus.BAD_REQUEST, "COMMON_003", "요청 본문을 읽을 수 없습니다."),
    COMMON_004(HttpStatus.BAD_REQUEST, "COMMON_004", "필수 요청 파라미터가 누락되었습니다."),
    COMMON_005(HttpStatus.BAD_REQUEST, "COMMON_005", "요청 값의 타입이 올바르지 않습니다."),
    COMMON_006(HttpStatus.METHOD_NOT_ALLOWED, "COMMON_006", "지원하지 않는 HTTP 메서드입니다."),
    COMMON_007(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "COMMON_007", "지원하지 않는 Content-Type 입니다."),
    COMMON_008(HttpStatus.UNAUTHORIZED, "COMMON_008", "인증이 필요합니다."),
    COMMON_009(HttpStatus.FORBIDDEN, "COMMON_009", "접근 권한이 없습니다."),
    COMMON_999(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_999", "서버 내부 오류가 발생했습니다."),

    // Test
    TEST_001(HttpStatus.BAD_REQUEST, "TEST_001", "카테고리는 1개 이상 3개 이하로 선택해야 합니다."),
    TEST_002(HttpStatus.BAD_REQUEST, "TEST_002", "이미지는 최대 10개까지 업로드할 수 있습니다."),
    TEST_003(HttpStatus.BAD_REQUEST, "TEST_003", "지원하지 않는 이미지 형식입니다. JPG, PNG만 허용됩니다."),

    // File
    FILE_UPLOAD_FAIL(HttpStatus.INTERNAL_SERVER_ERROR, "FILE_001", "파일 업로드에 실패했습니다.");
    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
