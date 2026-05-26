package server.MATE.domain.payment.policy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultPaymentAmountCalculatorTest {

    private final DefaultPaymentAmountCalculator calculator = new DefaultPaymentAmountCalculator();

    @Test
    @DisplayName("testerReward는 테스터 리워드 원금을 계산한다")
    void calculatesTesterRewardAmount() {
        assertThat(calculator.testerReward(100, 300)).isEqualTo(30000);
    }

    @Test
    @DisplayName("breakdown은 리워드 원금에 수수료와 VAT를 더한 결과를 반환한다")
    void calculatesPaymentBreakdown() {
        PaymentAmountBreakdown breakdown = calculator.breakdown(30, 200);

        assertThat(breakdown.testerRewardAmount()).isEqualTo(6000);
        assertThat(breakdown.feeAmount()).isEqualTo(4000);
        assertThat(breakdown.vatAmount()).isEqualTo(1000);
        assertThat(breakdown.totalAmount()).isEqualTo(11000);
        assertThat(calculator.totalAmount(30, 200)).isEqualTo(11000);
    }
}
