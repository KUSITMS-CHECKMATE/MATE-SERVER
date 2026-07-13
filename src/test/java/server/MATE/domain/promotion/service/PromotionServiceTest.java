package server.MATE.domain.promotion.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import server.MATE.toss.exception.TossApiException;
import server.MATE.toss.exception.TossErrorCode;
import server.MATE.toss.gateway.PromotionGatewayExecutionStatus;
import server.MATE.toss.gateway.TossPromotionGateway;

@ExtendWith(MockitoExtension.class)
class PromotionServiceTest {

    @Mock
    private PromotionPrepareService promotionPrepareService;

    @Mock
    private PromotionIssueStateService promotionIssueStateService;

    @Mock
    private PromotionExecuteStateService promotionExecuteStateService;

    @Mock
    private PromotionFailureStateService promotionFailureStateService;

    @Mock
    private PromotionExecutionResultApplier promotionExecutionResultApplier;

    @Mock
    private TossPromotionGateway tossPromotionGateway;

    private PromotionService promotionService;

    @BeforeEach
    void setUp() {
        promotionService = new PromotionService(
                promotionPrepareService,
                promotionIssueStateService,
                promotionExecuteStateService,
                promotionFailureStateService,
                promotionExecutionResultApplier,
                tossPromotionGateway
        );
    }

    @Test
    @DisplayName("준비 결과가 ready면 key 발급, execute, result 조회 순서로 상태를 반영한다")
    void grantsRewardSuccessfully() {
        given(promotionPrepareService.prepare(10L, 20L, 30L, 300))
                .willReturn(PromotionPrepareService.PromotionPreparation.ready(1L, 777L, "promo-answer", 300));
        given(tossPromotionGateway.issueKey(777L)).willReturn("reward-key");
        given(tossPromotionGateway.getExecutionStatus(777L, "promo-answer", "reward-key"))
                .willReturn(PromotionGatewayExecutionStatus.SUCCEEDED);

        promotionService.grant(10L, 20L, 30L, 300);

        verify(promotionIssueStateService).markKeyIssued(1L, "promo-answer", "reward-key");
        verify(tossPromotionGateway).execute(777L, "promo-answer", "reward-key", 300);
        verify(promotionExecuteStateService).markExecuted(1L);
        verify(promotionExecutionResultApplier).apply(1L, PromotionGatewayExecutionStatus.SUCCEEDED);
    }

    @Test
    @DisplayName("준비 결과가 skip이면 외부 provider를 호출하지 않는다")
    void skipsWhenPreparationReturnsSkip() {
        given(promotionPrepareService.prepare(10L, 20L, 30L, 300))
                .willReturn(PromotionPrepareService.PromotionPreparation.skip());

        promotionService.grant(10L, 20L, 30L, 300);

        verify(promotionPrepareService).prepare(10L, 20L, 30L, 300);
    }

    @Test
    @DisplayName("준비 결과가 fail이면 실패 상태만 반영한다")
    void marksFailedWhenPreparationFails() {
        given(promotionPrepareService.prepare(10L, 20L, 30L, 300))
                .willReturn(PromotionPrepareService.PromotionPreparation.failure(
                        1L,
                        PromotionPrepareService.LOCAL_ERROR_CODE_NOT_LINKED,
                        "연결된 토스 계정을 찾을 수 없습니다."
                ));

        promotionService.grant(10L, 20L, 30L, 300);

        verify(promotionFailureStateService).markFailed(
                1L,
                PromotionPrepareService.LOCAL_ERROR_CODE_NOT_LINKED,
                "연결된 토스 계정을 찾을 수 없습니다."
        );
    }

    @Test
    @DisplayName("result가 pending이면 pending 상태를 반영한다")
    void marksPendingWhenResultIsPending() {
        given(promotionPrepareService.prepare(10L, 20L, 30L, 300))
                .willReturn(PromotionPrepareService.PromotionPreparation.ready(1L, 777L, "promo-answer", 300));
        given(tossPromotionGateway.issueKey(777L)).willReturn("reward-key");
        given(tossPromotionGateway.getExecutionStatus(777L, "promo-answer", "reward-key"))
                .willReturn(PromotionGatewayExecutionStatus.PENDING);

        promotionService.grant(10L, 20L, 30L, 300);

        verify(promotionExecutionResultApplier).apply(1L, PromotionGatewayExecutionStatus.PENDING);
    }

    @Test
    @DisplayName("execute가 4113(이미 사용된 key)이면 즉시 실패 처리하지 않고 결과를 재조회해 반영한다")
    void reconcilesWhenExecuteReportsAlreadyUsedKey() {
        given(promotionPrepareService.prepare(10L, 20L, 30L, 300))
                .willReturn(PromotionPrepareService.PromotionPreparation.ready(1L, 777L, "promo-answer", 300));
        given(tossPromotionGateway.issueKey(777L)).willReturn("reward-key");
        given(tossPromotionGateway.getExecutionStatus(777L, "promo-answer", "reward-key"))
                .willReturn(PromotionGatewayExecutionStatus.SUCCEEDED);
        org.mockito.Mockito.doThrow(new TossApiException(TossErrorCode.TOSS_015, "이미 사용된 key입니다.", null))
                .when(tossPromotionGateway).execute(777L, "promo-answer", "reward-key", 300);

        promotionService.grant(10L, 20L, 30L, 300);

        verify(promotionExecuteStateService).markExecuted(1L);
        verify(promotionExecutionResultApplier).apply(1L, PromotionGatewayExecutionStatus.SUCCEEDED);
        verify(promotionFailureStateService, never()).markFailed(any(), any(), any());
    }

    @Test
    @DisplayName("execute가 4113이 아닌 TossApiException을 던지면 즉시 실패 처리한다")
    void marksFailedWhenExecuteFailsWithOtherTossError() {
        given(promotionPrepareService.prepare(10L, 20L, 30L, 300))
                .willReturn(PromotionPrepareService.PromotionPreparation.ready(1L, 777L, "promo-answer", 300));
        given(tossPromotionGateway.issueKey(777L)).willReturn("reward-key");
        org.mockito.Mockito.doThrow(new TossApiException(TossErrorCode.TOSS_014, "프로모션 머니가 부족해요", null))
                .when(tossPromotionGateway).execute(777L, "promo-answer", "reward-key", 300);

        assertThatThrownBy(() -> promotionService.grant(10L, 20L, 30L, 300))
                .isInstanceOf(TossApiException.class);

        verify(promotionFailureStateService).markFailed(1L, "TOSS_014", "프로모션 머니가 부족해요");
        verify(tossPromotionGateway, never()).getExecutionStatus(any(), any(), any());
    }

    @Test
    @DisplayName("execute는 성공했지만 결과 재조회 자체가 실패하면 FAILED로 단정하지 않고 PENDING으로 남긴다")
    void marksPendingWhenExecutionStatusLookupFailsAfterExecute() {
        given(promotionPrepareService.prepare(10L, 20L, 30L, 300))
                .willReturn(PromotionPrepareService.PromotionPreparation.ready(1L, 777L, "promo-answer", 300));
        given(tossPromotionGateway.issueKey(777L)).willReturn("reward-key");
        given(tossPromotionGateway.getExecutionStatus(777L, "promo-answer", "reward-key"))
                .willThrow(new TossApiException(TossErrorCode.TOSS_001, "네트워크 오류", null));

        promotionService.grant(10L, 20L, 30L, 300);

        verify(promotionExecuteStateService).markExecuted(1L);
        verify(promotionExecuteStateService).markPending(1L);
        verify(promotionExecutionResultApplier, never()).apply(any(), any());
        verify(promotionFailureStateService, never()).markFailed(any(), any(), any());
    }
}
