package server.MATE.domain.test.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TestTest {

    @Test
    @DisplayName("closedAt 없이 테스트를 생성하면 TEST_008 예외가 발생한다")
    void createWithoutClosedAt_throwsTest008() {
        BaseException exception = assertThrows(BaseException.class, () -> server.MATE.domain.test.entity.Test.builder()
                .makerId(1L)
                .title("테스트")
                .description("설명")
                .serviceName("서비스")
                .serviceDescription("서비스 설명")
                .build());

        assertThat(exception.getErrorCode()).isEqualTo(BaseErrorCode.TEST_008);
    }
}
