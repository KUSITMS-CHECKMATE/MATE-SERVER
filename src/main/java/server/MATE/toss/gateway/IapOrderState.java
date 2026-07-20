package server.MATE.toss.gateway;

public enum IapOrderState {
    PURCHASED,
    PAYMENT_COMPLETED,
    FAILED,
    REFUNDED,
    ORDER_IN_PROGRESS,
    NOT_FOUND,
    MINIAPP_MISMATCH,
    ERROR
}
