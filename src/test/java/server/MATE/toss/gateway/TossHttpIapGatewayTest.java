package server.MATE.toss.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.time.LocalDateTime;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import server.MATE.toss.client.iap.TossIapApiClient;
import server.MATE.toss.dto.response.IapOrderStatus;
import server.MATE.toss.dto.response.IapOrderStatusResponse;

@ExtendWith(MockitoExtension.class)
class TossHttpIapGatewayTest {

    @Mock
    private TossIapApiClient tossIapApiClient;

    private TossHttpIapGateway gateway;

    @BeforeEach
    void setUp() {
        gateway = new TossHttpIapGateway(tossIapApiClient);
    }

    @Test
    void mapsAllTossStatusesToGatewayStates() {
        Map<IapOrderStatus, IapOrderState> expected = Map.of(
                IapOrderStatus.PURCHASED, IapOrderState.PURCHASED,
                IapOrderStatus.PAYMENT_COMPLETED, IapOrderState.PAYMENT_COMPLETED,
                IapOrderStatus.FAILED, IapOrderState.FAILED,
                IapOrderStatus.REFUNDED, IapOrderState.REFUNDED,
                IapOrderStatus.ORDER_IN_PROGRESS, IapOrderState.ORDER_IN_PROGRESS,
                IapOrderStatus.NOT_FOUND, IapOrderState.NOT_FOUND,
                IapOrderStatus.MINIAPP_MISMATCH, IapOrderState.MINIAPP_MISMATCH,
                IapOrderStatus.ERROR, IapOrderState.ERROR
        );

        expected.forEach((tossStatus, gatewayState) -> {
            given(tossIapApiClient.getOrderStatus(777L, "order-1"))
                    .willReturn(new IapOrderStatusResponse("order-1", "sku", "2025-09-12T16:57:12", tossStatus, null));

            assertThat(gateway.getOrderStatus(777L, "order-1").status()).isEqualTo(gatewayState);
        });
    }

    @Test
    void passesThroughSkuAndReason() {
        given(tossIapApiClient.getOrderStatus(777L, "order-1"))
                .willReturn(new IapOrderStatusResponse("order-1", "test_sku", "2025-09-12T16:57:12", IapOrderStatus.FAILED, "user_cancel"));

        IapOrderStatusResult result = gateway.getOrderStatus(777L, "order-1");

        assertThat(result.sku()).isEqualTo("test_sku");
        assertThat(result.reason()).isEqualTo("user_cancel");
    }

    @Test
    void parsesLocalDateTimeFormat() {
        given(tossIapApiClient.getOrderStatus(777L, "order-1"))
                .willReturn(new IapOrderStatusResponse("order-1", "sku", "2025-09-12T16:57:12", IapOrderStatus.PURCHASED, null));

        IapOrderStatusResult result = gateway.getOrderStatus(777L, "order-1");

        assertThat(result.statusDeterminedAt()).isEqualTo(LocalDateTime.of(2025, 9, 12, 16, 57, 12));
    }

    @Test
    void parsesOffsetDateTimeFormatAsKst() {
        given(tossIapApiClient.getOrderStatus(777L, "order-1"))
                .willReturn(new IapOrderStatusResponse("order-1", "sku", "2025-09-12T16:57:12+09:00", IapOrderStatus.PURCHASED, null));

        IapOrderStatusResult result = gateway.getOrderStatus(777L, "order-1");

        assertThat(result.statusDeterminedAt()).isEqualTo(LocalDateTime.of(2025, 9, 12, 16, 57, 12));
    }

    @Test
    void fallsBackToNowWhenStatusDeterminedAtIsNull() {
        given(tossIapApiClient.getOrderStatus(777L, "order-1"))
                .willReturn(new IapOrderStatusResponse("order-1", "sku", null, IapOrderStatus.PURCHASED, null));

        IapOrderStatusResult result = gateway.getOrderStatus(777L, "order-1");

        assertThat(result.statusDeterminedAt()).isNotNull();
    }

    @Test
    void fallsBackToNowWhenStatusDeterminedAtIsMalformed() {
        given(tossIapApiClient.getOrderStatus(777L, "order-1"))
                .willReturn(new IapOrderStatusResponse("order-1", "sku", "not-a-date", IapOrderStatus.PURCHASED, null));

        IapOrderStatusResult result = gateway.getOrderStatus(777L, "order-1");

        assertThat(result.statusDeterminedAt()).isNotNull();
    }
}
