package server.MATE.toss.client.iap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import server.MATE.toss.client.http.TossHttpClient;
import server.MATE.toss.dto.request.IapOrderStatusRequest;
import server.MATE.toss.dto.response.IapOrderStatus;
import server.MATE.toss.dto.response.IapOrderStatusResponse;

@ExtendWith(MockitoExtension.class)
class TossIapApiClientTest {

    private static final String ORDER_STATUS_PATH = "/api-partner/v1/apps-in-toss/order/get-order-status";

    @Mock
    private TossHttpClient tossHttpClient;

    private TossIapApiClient apiClient;

    @BeforeEach
    void setUp() {
        apiClient = new TossIapApiClient(tossHttpClient);
    }

    @Test
    void getOrderStatusSendsUserKeyHeaderAndOrderIdBody() {
        given(tossHttpClient.post(eq(ORDER_STATUS_PATH), any(), ArgumentMatchers.<Consumer<HttpHeaders>>any(), eq(IapOrderStatusResponse.class)))
                .willReturn(new IapOrderStatusResponse("order-1", "sku", "2025-09-12T16:57:12", IapOrderStatus.PURCHASED, null));

        IapOrderStatusResponse response = apiClient.getOrderStatus(777L, "order-1");

        assertThat(response.status()).isEqualTo(IapOrderStatus.PURCHASED);

        ArgumentCaptor<IapOrderStatusRequest> bodyCaptor = ArgumentCaptor.forClass(IapOrderStatusRequest.class);
        ArgumentCaptor<Consumer<HttpHeaders>> headerCaptor = ArgumentCaptor.forClass(Consumer.class);
        verify(tossHttpClient).post(eq(ORDER_STATUS_PATH), bodyCaptor.capture(), headerCaptor.capture(), eq(IapOrderStatusResponse.class));
        assertThat(bodyCaptor.getValue().orderId()).isEqualTo("order-1");

        HttpHeaders headers = new HttpHeaders();
        headerCaptor.getValue().accept(headers);
        assertThat(headers.getFirst("x-toss-user-key")).isEqualTo("777");
    }
}
