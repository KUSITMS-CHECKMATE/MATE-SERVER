package server.MATE.domain.payment.entity;

public enum PayStatus {
    PAY_STANDBY,
    PAY_CREATED,
    PAY_SUCCEEDED,
    PAY_FAILED,
    REFUND_PENDING,
    REFUNDED,
    REFUND_FAILED
}
