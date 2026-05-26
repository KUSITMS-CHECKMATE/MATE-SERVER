package server.MATE.domain.payment.policy;

public interface PaymentAmountCalculator {

    int testerReward(int goalPpl, int reward);

    PaymentAmountBreakdown breakdown(int goalPpl, int reward);

    default int totalAmount(int goalPpl, int reward) {
        return breakdown(goalPpl, reward).totalAmount();
    }
}
