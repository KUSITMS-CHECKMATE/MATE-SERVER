package server.MATE.domain.payment.policy;

import org.springframework.stereotype.Component;

@Component
public class DefaultPaymentAmountCalculator implements PaymentAmountCalculator {

    @Override
    public int testerReward(int goalPpl, int reward) {
        return breakdown(goalPpl, reward).testerRewardAmount();
    }

    @Override
    public PaymentAmountBreakdown breakdown(int goalPpl, int reward) {
        int testerRewardAmount = Math.multiplyExact(goalPpl, reward);
        int feeAmount = ceilDivide(Math.multiplyExact(testerRewardAmount, 2), 3);
        int supplyAmount = Math.addExact(testerRewardAmount, feeAmount);
        int vatAmount = ceilDivide(supplyAmount, 10);
        int totalAmount = Math.addExact(supplyAmount, vatAmount);

        return new PaymentAmountBreakdown(testerRewardAmount, feeAmount, vatAmount, totalAmount);
    }

    private int ceilDivide(int dividend, int divisor) {
        return Math.floorDiv(Math.addExact(dividend, divisor - 1), divisor);
    }
}
