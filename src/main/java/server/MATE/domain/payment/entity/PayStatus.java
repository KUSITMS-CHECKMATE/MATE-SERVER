package server.MATE.domain.payment.entity;

public enum PayStatus {
    PAY_SUCCEEDED,
    PAY_CANCELLED,
    PAY_FAILED,
    REFUND_PENDING,
    REFUND_REJECTED,
    REFUNDED
}
