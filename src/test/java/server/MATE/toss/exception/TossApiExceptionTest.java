package server.MATE.toss.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import server.MATE.toss.exception.parser.TossErrorContext;
import server.MATE.toss.response.TossErrorResponse;

class TossApiExceptionTest {

    @Test
    void createExceptionFromTossErrorContext() {
        TossErrorContext context = new TossErrorContext(
                TossErrorCode.TOSS_003,
                new TossErrorResponse("INVALID_AUTHORIZATION_CODE", "인가 코드가 유효하지 않습니다.")
        );

        TossApiException exception = TossApiException.from(HttpStatus.BAD_REQUEST, context, "{}");

        assertThat(exception.getErrorCode()).isEqualTo(TossErrorCode.TOSS_003);
        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(exception.getError()).isEqualTo(context.errorResponse());
        assertThat(exception.getResponseBody()).isEqualTo("{}");
        assertThat(exception.getMessage()).isEqualTo("인가 코드가 유효하지 않습니다.");
    }

    @Test
    void isAlreadyUsedPromotionKeyTrueForToss015() {
        TossApiException exception = new TossApiException(TossErrorCode.TOSS_015, "이미 사용된 key입니다.", null);

        assertThat(exception.isAlreadyUsedPromotionKey()).isTrue();
    }

    @Test
    void isAlreadyUsedPromotionKeyFalseForOtherCodes() {
        TossApiException exception = new TossApiException(TossErrorCode.TOSS_014, "프로모션 머니가 부족해요", null);

        assertThat(exception.isAlreadyUsedPromotionKey()).isFalse();
    }
}
