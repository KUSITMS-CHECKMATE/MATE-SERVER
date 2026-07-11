package server.MATE.domain.promotion.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import server.MATE.toss.gateway.PromotionGatewayExecutionStatus;

@ExtendWith(MockitoExtension.class)
class PromotionExecutionResultApplierTest {

    @Mock
    private PromotionExecuteStateService promotionExecuteStateService;

    @Mock
    private PromotionFailureStateService promotionFailureStateService;

    private PromotionExecutionResultApplier applier;

    @BeforeEach
    void setUp() {
        applier = new PromotionExecutionResultApplier(promotionExecuteStateService, promotionFailureStateService);
    }

    @Test
    @DisplayName("SUCCEEDED면 markSucceeded를 호출한다")
    void appliesSucceeded() {
        applier.apply(1L, PromotionGatewayExecutionStatus.SUCCEEDED);

        verify(promotionExecuteStateService).markSucceeded(1L);
        verifyNoInteractions(promotionFailureStateService);
    }

    @Test
    @DisplayName("PENDING이면 markPending을 호출한다")
    void appliesPending() {
        applier.apply(1L, PromotionGatewayExecutionStatus.PENDING);

        verify(promotionExecuteStateService).markPending(1L);
        verifyNoInteractions(promotionFailureStateService);
    }

    @Test
    @DisplayName("FAILED면 markFailed를 호출한다")
    void appliesFailed() {
        applier.apply(1L, PromotionGatewayExecutionStatus.FAILED);

        verify(promotionFailureStateService).markFailed(1L, "PROMOTION_EXECUTION_FAILED", "Promotion execution result is FAILED.");
    }
}
