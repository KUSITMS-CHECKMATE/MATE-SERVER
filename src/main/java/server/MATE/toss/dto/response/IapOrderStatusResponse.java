package server.MATE.toss.dto.response;

public record IapOrderStatusResponse(
        String orderId,
        String sku,
        String statusDeterminedAt,
        IapOrderStatus status,
        String reason
) {
}
