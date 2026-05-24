package server.MATE.domain.payment.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultPaymentAmountCalculatorTest {

    private final DefaultPaymentAmountCalculator calculator = new DefaultPaymentAmountCalculator();

    @Test
    @DisplayName("목표 인원과 리워드를 곱해 결제 금액을 계산한다")
    void calculatesAmount() {
        assertThat(calculator.calculate(100, 300)).isEqualTo(30000);
    }
}
