package server.MATE.domain.promotion.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import server.MATE.toss.dto.request.TossPromotionExecuteRequest;
import server.MATE.toss.dto.request.TossPromotionGetKeyRequest;
import server.MATE.toss.dto.request.TossPromotionResultRequest;
import server.MATE.toss.dto.response.TossPromotionExecutionStatus;
import server.MATE.toss.exception.TossApiException;
import server.MATE.toss.gateway.TossPromotionGateway;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromotionService {

    private static final String ERROR_CODE_GATEWAY = "PROMOTION_GATEWAY_ERROR";
    private static final String ERROR_CODE_EXECUTION_FAILED = "PROMOTION_EXECUTION_FAILED";
    private static final String ERROR_REASON_EXECUTION_FAILED = "Promotion execution result is FAILED.";

    private final PromotionPrepareService promotionPrepareService;
    private final PromotionIssueStateService promotionIssueStateService;
    private final PromotionExecuteStateService promotionExecuteStateService;
    private final PromotionFailureStateService promotionFailureStateService;
    private final TossPromotionGateway tossPromotionGateway;

    public void grant(Long participationId, Long testId, Long testerId, Integer rewardAmount) {
        PromotionPrepareService.PromotionPreparation preparation =
                promotionPrepareService.prepare(participationId, testId, testerId, rewardAmount);

        if (preparation.skipped()) {
            log.debug("PROMOTION skipped. participationId={}, testId={}, testerId={}",
                    participationId, testId, testerId);
            return;
        }
        if (preparation.shouldFail()) {
            promotionFailureStateService.markFailed(
                    preparation.rewardId(),
                    preparation.errorCode(),
                    preparation.errorReason()
            );
            log.warn("PROMOTION preparation failed. participationId={}, testId={}, testerId={}, code={}",
                    participationId, testId, testerId, preparation.errorCode());
            return;
        }

        try {
            var keyResponse = tossPromotionGateway.getKey(
                    new TossPromotionGetKeyRequest(preparation.tossUserKey())
            );
            promotionIssueStateService.markKeyIssued(
                    preparation.rewardId(),
                    preparation.promotionCode(),
                    keyResponse.key()
            );
            log.info("PROMOTION key issued. participationId={}, rewardId={}",
                    participationId, preparation.rewardId());

            tossPromotionGateway.executePromotion(new TossPromotionExecuteRequest(
                    preparation.tossUserKey(),
                    preparation.promotionCode(),
                    keyResponse.key(),
                    preparation.rewardAmount()
            ));
            promotionExecuteStateService.markExecuted(preparation.rewardId());
            log.info("PROMOTION execute requested. participationId={}, rewardId={}",
                    participationId, preparation.rewardId());

            var result = tossPromotionGateway.getExecutionResult(new TossPromotionResultRequest(
                    preparation.tossUserKey(),
                    preparation.promotionCode(),
                    keyResponse.key()
            ));

            if (result.status() == TossPromotionExecutionStatus.SUCCESS) {
                promotionExecuteStateService.markSucceeded(preparation.rewardId());
                log.info("PROMOTION succeeded. participationId={}, rewardId={}",
                        participationId, preparation.rewardId());
                return;
            }
            if (result.status() == TossPromotionExecutionStatus.PENDING) {
                promotionExecuteStateService.markPending(preparation.rewardId());
                log.info("PROMOTION pending. participationId={}, rewardId={}",
                        participationId, preparation.rewardId());
                return;
            }

            promotionFailureStateService.markFailed(
                    preparation.rewardId(),
                    ERROR_CODE_EXECUTION_FAILED,
                    ERROR_REASON_EXECUTION_FAILED
            );
            log.warn("PROMOTION failed by result. participationId={}, rewardId={}",
                    participationId, preparation.rewardId());
        } catch (TossApiException e) {
            promotionFailureStateService.markFailed(
                    preparation.rewardId(),
                    e.getErrorCode().getCode(),
                    e.getMessage()
            );
            log.warn("PROMOTION gateway failed with toss exception. participationId={}, code={}",
                    participationId, e.getErrorCode().getCode(), e);
            throw e;
        } catch (RuntimeException e) {
            promotionFailureStateService.markFailed(
                    preparation.rewardId(),
                    ERROR_CODE_GATEWAY,
                    e.getMessage()
            );
            log.warn("PROMOTION gateway failed. participationId={}", participationId, e);
            throw e;
        }
    }
}
