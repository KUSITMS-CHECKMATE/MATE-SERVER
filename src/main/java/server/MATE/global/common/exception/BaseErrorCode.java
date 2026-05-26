package server.MATE.global.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum BaseErrorCode implements ErrorCode {

    // Common
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

    // Auth
    AUTH_001(HttpStatus.UNAUTHORIZED, "AUTH_001", "유효하지 않은 액세스 토큰입니다."),
    AUTH_002(HttpStatus.UNAUTHORIZED, "AUTH_002", "만료된 토큰입니다."),
    AUTH_003(HttpStatus.UNAUTHORIZED, "AUTH_003", "허용되지 않은 토큰 타입입니다."),
    AUTH_004(HttpStatus.UNAUTHORIZED, "AUTH_004", "사용자를 찾을 수 없습니다."),
    AUTH_005(HttpStatus.INTERNAL_SERVER_ERROR, "AUTH_005", "JWT 설정 정보를 찾을 수 없습니다."),
    AUTH_006(HttpStatus.INTERNAL_SERVER_ERROR, "AUTH_006", "토스 복호화 설정 정보를 찾을 수 없습니다."),
    AUTH_007(HttpStatus.UNAUTHORIZED, "AUTH_007", "토스 사용자 정보를 복호화할 수 없습니다."),
    AUTH_008(HttpStatus.UNAUTHORIZED, "AUTH_008", "토스 사용자 식별 정보가 누락되었습니다."),
    AUTH_009(HttpStatus.INTERNAL_SERVER_ERROR, "AUTH_009", "리프레시 토큰을 저장할 수 없습니다."),
    AUTH_010(HttpStatus.INTERNAL_SERVER_ERROR, "AUTH_010", "토큰 암호화 설정 정보를 찾을 수 없습니다."),
    AUTH_011(HttpStatus.INTERNAL_SERVER_ERROR, "AUTH_011", "토큰을 암호화할 수 없습니다."),
    AUTH_012(HttpStatus.INTERNAL_SERVER_ERROR, "AUTH_012", "저장된 토큰을 복호화할 수 없습니다."),
    AUTH_013(HttpStatus.UNAUTHORIZED, "AUTH_013", "유효하지 않은 리프레시 토큰입니다."),

    // Test
    TEST_001(HttpStatus.BAD_REQUEST, "TEST_001", "카테고리는 1개 이상 3개 이하로 선택해야 합니다."),
    TEST_002(HttpStatus.BAD_REQUEST, "TEST_002", "이미지는 최대 10개까지 업로드할 수 있습니다."),
    TEST_003(HttpStatus.BAD_REQUEST, "TEST_003", "지원하지 않는 이미지 형식입니다. JPG, PNG만 허용됩니다."),
    TEST_004(HttpStatus.NOT_FOUND, "TEST_004", "테스트를 찾을 수 없습니다."),
    TEST_005(HttpStatus.FORBIDDEN, "TEST_005", "테스트 메이커만 조회할 수 있습니다."),
    TEST_006(HttpStatus.BAD_REQUEST, "TEST_006", "테스트가 종료된 후에 조회할 수 있습니다."),

    // Test Draft
    DRAFT_001(HttpStatus.NOT_FOUND, "DRAFT_001", "테스트 초안을 찾을 수 없습니다."),
    DRAFT_002(HttpStatus.FORBIDDEN, "DRAFT_002", "테스트 초안에 접근할 권한이 없습니다."),
    DRAFT_003(HttpStatus.BAD_REQUEST, "DRAFT_003", "결제를 생성할 수 없는 테스트 초안입니다."),
    DRAFT_004(HttpStatus.BAD_REQUEST, "DRAFT_004", "게시할 수 없는 테스트 초안입니다."),

    // Question
    QUESTION_001(HttpStatus.BAD_REQUEST, "QUESTION_001", "최소 선택 개수는 1 이상이어야 합니다."),
    QUESTION_002(HttpStatus.BAD_REQUEST, "QUESTION_002", "최대 선택 개수는 최소 선택 개수 이상이어야 합니다."),
    QUESTION_003(HttpStatus.BAD_REQUEST, "QUESTION_003", "최소/최대 선택 개수는 선택지 개수를 초과할 수 없습니다."),
    QUESTION_004(HttpStatus.BAD_REQUEST, "QUESTION_004", "객관식 전환 시 선택지는 최소 2개 이상이어야 합니다."),
    QUESTION_005(HttpStatus.NOT_FOUND, "QUESTION_005", "존재하지 않는 질문입니다."),
    QUESTION_006(HttpStatus.BAD_REQUEST, "QUESTION_006", "트리 테스트 깊이는 최대 4단계까지 가능합니다."),
    QUESTION_008(HttpStatus.BAD_REQUEST, "QUESTION_008", "주관식 5초 테스트에는 객관식 설정을 입력할 수 없습니다."),
    QUESTION_009(HttpStatus.BAD_REQUEST, "QUESTION_009", "단일 선택 객관식에는 최소/최대 선택 개수를 입력할 수 없습니다."),

    // Participation
    PARTICIPATION_001(HttpStatus.NOT_FOUND, "PARTICIPATION_001", "참여 정보를 찾을 수 없습니다."),
    PARTICIPATION_002(HttpStatus.BAD_REQUEST, "PARTICIPATION_002", "참여할 수 없는 테스트입니다."),
    PARTICIPATION_003(HttpStatus.BAD_REQUEST, "PARTICIPATION_003", "이미 참여한 테스트입니다."),
    PARTICIPATION_004(HttpStatus.BAD_REQUEST, "PARTICIPATION_004", "테스트 참여 인원이 마감되었습니다."),

    // Answer
    ANSWER_001(HttpStatus.BAD_REQUEST, "ANSWER_001", "해당 질문 유형에 대한 응답을 등록할 수 없습니다."),
    ANSWER_002(HttpStatus.BAD_REQUEST, "ANSWER_002", "질문이 해당 테스트에 속하지 않습니다."),
    ANSWER_003(HttpStatus.BAD_REQUEST, "ANSWER_003", "이미 응답한 질문입니다."),
    ANSWER_004(HttpStatus.BAD_REQUEST, "ANSWER_004", "유효하지 않은 입력입니다."),
    ANSWER_005(HttpStatus.BAD_REQUEST, "ANSWER_005", "응답이 입력되지 않았습니다."),
    ANSWER_006(HttpStatus.BAD_REQUEST, "ANSWER_006", "선택 개수가 허용 범위를 벗어났습니다."),
    ANSWER_007(HttpStatus.BAD_REQUEST, "ANSWER_007", "점수가 허용 범위를 벗어났습니다."),
    ANSWER_008(HttpStatus.BAD_REQUEST, "ANSWER_008", "모든 문항에 응답해야 합니다."),

    // Payment
    PAYMENT_001(HttpStatus.NOT_FOUND, "PAYMENT_001", "결제 정보를 찾을 수 없습니다."),
    PAYMENT_002(HttpStatus.BAD_REQUEST, "PAYMENT_002", "결제를 생성할 수 없는 상태입니다."),
    PAYMENT_003(HttpStatus.BAD_REQUEST, "PAYMENT_003", "결제를 실행할 수 없는 상태입니다."),
    PAYMENT_004(HttpStatus.BAD_REQUEST, "PAYMENT_004", "환불할 수 없는 상태입니다."),
    PAYMENT_005(HttpStatus.BAD_REQUEST, "PAYMENT_005", "결제 금액 계산에 필요한 값이 누락되었습니다."),

    // Report
    REPORT_001(HttpStatus.BAD_REQUEST, "REPORT_001", "엑셀 보고서는 질문 20개 이하 테스트만 지원합니다."),
    REPORT_002(HttpStatus.BAD_REQUEST, "REPORT_002", "객관식 질문만 엑셀 통계를 다운로드할 수 있습니다."),
    REPORT_003(HttpStatus.BAD_REQUEST, "REPORT_003", "주관식 질문만 엑셀 통계를 다운로드할 수 있습니다."),
    REPORT_004(HttpStatus.BAD_REQUEST, "REPORT_004", "A/B 테스트 질문만 엑셀 통계를 다운로드할 수 있습니다."),
    REPORT_005(HttpStatus.BAD_REQUEST, "REPORT_005", "척도 질문만 엑셀 통계를 다운로드할 수 있습니다."),
    REPORT_006(HttpStatus.BAD_REQUEST, "REPORT_006", "카드 소팅 질문만 엑셀 통계를 다운로드할 수 있습니다."),
    REPORT_007(HttpStatus.BAD_REQUEST, "REPORT_007", "리포트 집계가 완료된 후에 엑셀을 다운로드할 수 있습니다."),

    // File
    FILE_UPLOAD_FAIL(HttpStatus.INTERNAL_SERVER_ERROR, "FILE_001", "파일 업로드에 실패했습니다."),
    FILE_002(HttpStatus.BAD_REQUEST, "FILE_002", "지원하지 않는 파일 형식입니다. 허용되는 확장자를 확인해주세요."),
    FILE_003(HttpStatus.BAD_REQUEST, "FILE_003", "파일 크기가 허용 범위를 초과했습니다. 최대 50MB까지 업로드할 수 있습니다."),

    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
