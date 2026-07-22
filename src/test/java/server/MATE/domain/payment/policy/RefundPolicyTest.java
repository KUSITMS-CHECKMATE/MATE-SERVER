package server.MATE.domain.payment.policy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import server.MATE.domain.test.entity.TestStatus;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class RefundPolicyTest {

    private final RefundPolicy refundPolicy = new RefundPolicy();

    @Test
    @DisplayName("달성률 20% 미만이고 수동종료/환불포기 없으면 환불 대상이다")
    void eligibleWhenUnder20PercentAndNoBlockingConditions() {
        server.MATE.domain.test.entity.Test test = testWith(100, 10);

        assertThat(refundPolicy.isEligibleForRefund(test)).isTrue();
    }

    @Test
    @DisplayName("달성률 0%도 환불 대상이다")
    void eligibleWhenZeroParticipants() {
        server.MATE.domain.test.entity.Test test = testWith(100, 0);

        assertThat(refundPolicy.isEligibleForRefund(test)).isTrue();
    }

    @Test
    @DisplayName("달성률 정확히 20%이면 환불 불가이다")
    void ineligibleWhenExactly20Percent() {
        server.MATE.domain.test.entity.Test test = testWith(100, 20);

        assertThat(refundPolicy.isEligibleForRefund(test)).isFalse();
    }

    @Test
    @DisplayName("달성률 20% 이상이면 환불 불가이다")
    void ineligibleWhenAbove20Percent() {
        server.MATE.domain.test.entity.Test test = testWith(100, 60);

        assertThat(refundPolicy.isEligibleForRefund(test)).isFalse();
    }

    @Test
    @DisplayName("메이커 수동 종료이면 달성률 무관 환불 불가이다")
    void ineligibleWhenClosedByMaker() {
        server.MATE.domain.test.entity.Test test = testWith(100, 5);
        test.markClosedByMaker();

        assertThat(refundPolicy.isEligibleForRefund(test)).isFalse();
    }

    @Test
    @DisplayName("환불 포기(진행 의사 선택)이면 환불 불가이다")
    void ineligibleWhenRefundWaived() {
        server.MATE.domain.test.entity.Test test = testWith(100, 5);
        test.waiveRefund();

        assertThat(refundPolicy.isEligibleForRefund(test)).isFalse();
    }

    private server.MATE.domain.test.entity.Test testWith(int goalPpl, long pplCount) {
        server.MATE.domain.test.entity.Test test = server.MATE.domain.test.entity.Test.builder()
                .makerId(1L)
                .title("테스트")
                .goalPpl(goalPpl)
                .reward(500)
                .testStatus(TestStatus.COMPLETED)
                .closedAt(LocalDateTime.now().plusDays(30))
                .build();
        ReflectionTestUtils.setField(test, "pplCount", pplCount);
        return test;
    }
}
