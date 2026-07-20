package server.MATE.toss.gateway;

public interface TossIapGateway {
    IapOrderStatusResult getOrderStatus(Long tossUserKey, String orderId);
}
