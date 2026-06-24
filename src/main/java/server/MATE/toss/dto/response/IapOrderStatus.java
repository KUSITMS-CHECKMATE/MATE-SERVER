package server.MATE.toss.dto.response;

public enum IapOrderStatus {
    PURCHASED,
    PAYMENT_COMPLETED,
    FAILED,
    REFUNDED,
    ORDER_IN_PROGRESS,
    NOT_FOUND,
    MINIAPP_MISMATCH,
    ERROR
}
