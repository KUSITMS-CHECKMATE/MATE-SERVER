package server.MATE.domain.test.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

import java.time.LocalDateTime;

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

    @Test
    @DisplayName("approve() 호출 시 IN_PROGRESS로 전환되고 rejectionReason이 초기화된다")
    void approve_setsInProgressAndClearsRejectionReason() {
        server.MATE.domain.test.entity.Test test = buildTest(TestStatus.REJECTED);
        test.reject("사유");

        test.approve();

        assertThat(test.getTestStatus()).isEqualTo(TestStatus.IN_PROGRESS);
        assertThat(test.getRejectionReason()).isNull();
    }

    @Test
    @DisplayName("reject(reason) 호출 시 REJECTED로 전환되고 사유가 저장된다")
    void reject_setsRejectedAndStoresReason() {
        server.MATE.domain.test.entity.Test test = buildTest(TestStatus.WAITING);

        test.reject("이미지가 흐릿합니다");

        assertThat(test.getTestStatus()).isEqualTo(TestStatus.REJECTED);
        assertThat(test.getRejectionReason()).isEqualTo("이미지가 흐릿합니다");
    }

    @Test
    @DisplayName("reject(reason) 호출 시 앞뒤 공백은 trim된다")
    void reject_trimsReason() {
        server.MATE.domain.test.entity.Test test = buildTest(TestStatus.WAITING);

        test.reject("  사유  ");

        assertThat(test.getRejectionReason()).isEqualTo("사유");
    }

    @Test
    @DisplayName("reject(reason) 호출 시 공백만 있으면 null로 저장된다")
    void reject_blankReason_storesNull() {
        server.MATE.domain.test.entity.Test test = buildTest(TestStatus.WAITING);

        test.reject("   ");

        assertThat(test.getRejectionReason()).isNull();
    }

    @Test
    @DisplayName("reject(null) 호출 시 rejectionReason은 null이다")
    void reject_nullReason_storesNull() {
        server.MATE.domain.test.entity.Test test = buildTest(TestStatus.WAITING);

        test.reject(null);

        assertThat(test.getRejectionReason()).isNull();
    }

    private server.MATE.domain.test.entity.Test buildTest(TestStatus testStatus) {
        return server.MATE.domain.test.entity.Test.builder()
                .makerId(1L)
                .title("테스트")
                .description("설명")
                .serviceName("서비스")
                .serviceDescription("서비스 설명")
                .goalPpl(10)
                .reward(300)
                .testStatus(testStatus)
                .closedAt(LocalDateTime.now().plusDays(7))
                .build();
    }
}
