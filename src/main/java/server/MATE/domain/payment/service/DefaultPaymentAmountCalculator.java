package server.MATE.domain.payment.service;

import org.springframework.stereotype.Component;

@Component
public class DefaultPaymentAmountCalculator implements PaymentAmountCalculator {

    @Override
    public int calculate(int goalPpl, int reward) {
        return Math.multiplyExact(goalPpl, reward);
    }
}
