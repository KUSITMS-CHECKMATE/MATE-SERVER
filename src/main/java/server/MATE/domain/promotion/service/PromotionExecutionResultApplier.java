package server.MATE.domain.promotion.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import server.MATE.toss.gateway.PromotionGatewayExecutionStatus;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromotionExecutionResultApplier {

    private static final String ERROR_CODE_EXECUTION_FAILED = "PROMOTION_EXECUTION_FAILED";
    private static final String ERROR_REASON_EXECUTION_FAILED = "Promotion execution result is FAILED.";

    private final PromotionExecuteStateService promotionExecuteStateService;
    private final PromotionFailureStateService promotionFailureStateService;

    public void apply(Long rewardId, PromotionGatewayExecutionStatus status) {
        if (status == null) {
            log.warn("PROMOTION status is null. rewardId={}", rewardId);
            return;
        }
        switch (status) {
            case SUCCEEDED -> {
                promotionExecuteStateService.markSucceeded(rewardId);
                log.info("PROMOTION succeeded. rewardId={}", rewardId);
            }
            case PENDING -> {
                promotionExecuteStateService.markPending(rewardId);
                log.info("PROMOTION pending. rewardId={}", rewardId);
            }
            case FAILED -> {
                promotionFailureStateService.markFailed(rewardId, ERROR_CODE_EXECUTION_FAILED, ERROR_REASON_EXECUTION_FAILED);
                log.warn("PROMOTION failed by result. rewardId={}", rewardId);
            }
        }
    }
}
