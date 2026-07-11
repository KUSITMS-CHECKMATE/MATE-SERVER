package server.MATE.toss.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import server.MATE.toss.client.http.TossHttpClient;
import server.MATE.toss.dto.response.TossPromotionExecutionStatus;

@ExtendWith(MockitoExtension.class)
class TossHttpPromotionGatewayTest {

    private static final String RESULT_PATH = "/api-partner/v1/apps-in-toss/promotion/execution-result";

    @Mock
    private TossHttpClient tossHttpClient;

    private TossHttpPromotionGateway gateway;

    @BeforeEach
    void setUp() {
        gateway = new TossHttpPromotionGateway(tossHttpClient);
    }

    @Test
    void mapsSuccessToSucceeded() {
        given(tossHttpClient.post(eq(RESULT_PATH), any(), ArgumentMatchers.<Consumer<HttpHeaders>>any(), eq(TossPromotionExecutionStatus.class)))
                .willReturn(TossPromotionExecutionStatus.SUCCESS);

        assertThat(gateway.getExecutionStatus(777L, "promo-code", "reward-key"))
                .isEqualTo(PromotionGatewayExecutionStatus.SUCCEEDED);
    }

    @Test
    void mapsPendingToPending() {
        given(tossHttpClient.post(eq(RESULT_PATH), any(), ArgumentMatchers.<Consumer<HttpHeaders>>any(), eq(TossPromotionExecutionStatus.class)))
                .willReturn(TossPromotionExecutionStatus.PENDING);

        assertThat(gateway.getExecutionStatus(777L, "promo-code", "reward-key"))
                .isEqualTo(PromotionGatewayExecutionStatus.PENDING);
    }

    @Test
    void mapsFailedToFailed() {
        given(tossHttpClient.post(eq(RESULT_PATH), any(), ArgumentMatchers.<Consumer<HttpHeaders>>any(), eq(TossPromotionExecutionStatus.class)))
                .willReturn(TossPromotionExecutionStatus.FAILED);

        assertThat(gateway.getExecutionStatus(777L, "promo-code", "reward-key"))
                .isEqualTo(PromotionGatewayExecutionStatus.FAILED);
    }
}
