package server.MATE.domain.promotion.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import server.MATE.toss.dto.request.TossPromotionExecuteRequest;
import server.MATE.toss.dto.request.TossPromotionGetKeyRequest;
import server.MATE.toss.dto.request.TossPromotionResultRequest;
import server.MATE.toss.dto.response.TossPromotionExecuteResponse;
import server.MATE.toss.dto.response.TossPromotionExecutionStatus;
import server.MATE.toss.dto.response.TossPromotionKeyResponse;
import server.MATE.toss.dto.response.TossPromotionResultResponse;
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
    private TossPromotionGateway tossPromotionGateway;

    private PromotionService promotionService;

    @BeforeEach
    void setUp() {
        promotionService = new PromotionService(
                promotionPrepareService,
                promotionIssueStateService,
                promotionExecuteStateService,
                promotionFailureStateService,
                tossPromotionGateway
        );
    }

    @Test
    @DisplayName("준비 결과가 ready면 key 발급, execute, result 조회 순서로 상태를 반영한다")
    void grantsRewardSuccessfully() {
        given(promotionPrepareService.prepare(10L, 20L, 30L, 300))
                .willReturn(PromotionPrepareService.PromotionPreparation.ready(1L, 777L, "promo-answer", 300));
        given(tossPromotionGateway.getKey(any(TossPromotionGetKeyRequest.class)))
                .willReturn(new TossPromotionKeyResponse("reward-key"));
        given(tossPromotionGateway.executePromotion(any(TossPromotionExecuteRequest.class)))
                .willReturn(new TossPromotionExecuteResponse("reward-key"));
        given(tossPromotionGateway.getExecutionResult(any(TossPromotionResultRequest.class)))
                .willReturn(new TossPromotionResultResponse(TossPromotionExecutionStatus.SUCCESS));

        promotionService.grant(10L, 20L, 30L, 300);

        verify(promotionIssueStateService).markKeyIssued(1L, "promo-answer", "reward-key");
        verify(promotionExecuteStateService).markExecuted(1L);
        verify(promotionExecuteStateService).markSucceeded(1L);
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
        given(tossPromotionGateway.getKey(any(TossPromotionGetKeyRequest.class)))
                .willReturn(new TossPromotionKeyResponse("reward-key"));
        given(tossPromotionGateway.executePromotion(any(TossPromotionExecuteRequest.class)))
                .willReturn(new TossPromotionExecuteResponse("reward-key"));
        given(tossPromotionGateway.getExecutionResult(any(TossPromotionResultRequest.class)))
                .willReturn(new TossPromotionResultResponse(TossPromotionExecutionStatus.PENDING));

        promotionService.grant(10L, 20L, 30L, 300);

        verify(promotionExecuteStateService).markPending(1L);
    }
}
