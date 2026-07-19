package server.MATE.toss.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import server.MATE.toss.client.promotion.TossPromotionApiClient;
import server.MATE.toss.dto.response.TossPromotionExecutionStatus;

@ExtendWith(MockitoExtension.class)
class TossHttpPromotionGatewayTest {

    @Mock
    private TossPromotionApiClient tossPromotionApiClient;

    private TossHttpPromotionGateway gateway;

    @BeforeEach
    void setUp() {
        gateway = new TossHttpPromotionGateway(tossPromotionApiClient);
    }

    @Test
    void mapsSuccessToSucceeded() {
        given(tossPromotionApiClient.getExecutionStatus(777L, "promo-code", "reward-key"))
                .willReturn(TossPromotionExecutionStatus.SUCCESS);

        assertThat(gateway.getExecutionStatus(777L, "promo-code", "reward-key"))
                .isEqualTo(PromotionGatewayExecutionStatus.SUCCEEDED);
    }

    @Test
    void mapsPendingToPending() {
        given(tossPromotionApiClient.getExecutionStatus(777L, "promo-code", "reward-key"))
                .willReturn(TossPromotionExecutionStatus.PENDING);

        assertThat(gateway.getExecutionStatus(777L, "promo-code", "reward-key"))
                .isEqualTo(PromotionGatewayExecutionStatus.PENDING);
    }

    @Test
    void mapsFailedToFailed() {
        given(tossPromotionApiClient.getExecutionStatus(777L, "promo-code", "reward-key"))
                .willReturn(TossPromotionExecutionStatus.FAILED);

        assertThat(gateway.getExecutionStatus(777L, "promo-code", "reward-key"))
                .isEqualTo(PromotionGatewayExecutionStatus.FAILED);
    }

    @Test
    void throwsIllegalStateExceptionWhenStatusIsNull() {
        given(tossPromotionApiClient.getExecutionStatus(777L, "promo-code", "reward-key"))
                .willReturn(null);

        assertThatThrownBy(() -> gateway.getExecutionStatus(777L, "promo-code", "reward-key"))
                .isInstanceOf(IllegalStateException.class);
    }
}
