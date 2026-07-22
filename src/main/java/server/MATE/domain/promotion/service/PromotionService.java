package server.MATE.domain.promotion.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import server.MATE.toss.exception.TossApiException;
import server.MATE.toss.gateway.PromotionGatewayExecutionStatus;
import server.MATE.toss.gateway.TossPromotionGateway;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromotionService {

    private static final String ERROR_CODE_GATEWAY = "PROMOTION_GATEWAY_ERROR";

    private final PromotionPrepareService promotionPrepareService;
    private final PromotionIssueStateService promotionIssueStateService;
    private final PromotionExecuteStateService promotionExecuteStateService;
    private final PromotionFailureStateService promotionFailureStateService;
    private final PromotionExecutionResultApplier promotionExecutionResultApplier;
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
            String rewardKey = tossPromotionGateway.issueKey(preparation.tossUserKey());
            promotionIssueStateService.markKeyIssued(
                    preparation.rewardId(),
                    preparation.promotionCode(),
                    rewardKey
            );
            log.info("PROMOTION key issued. participationId={}, rewardId={}",
                    participationId, preparation.rewardId());

            executeAndResolve(participationId, preparation, rewardKey);
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

    private void executeAndResolve(Long participationId, PromotionPrepareService.PromotionPreparation preparation, String rewardKey) {
        try {
            tossPromotionGateway.execute(
                    preparation.tossUserKey(),
                    preparation.promotionCode(),
                    rewardKey,
                    preparation.rewardAmount()
            );
            log.info("PROMOTION execute requested. participationId={}, rewardId={}",
                    participationId, preparation.rewardId());
        } catch (TossApiException e) {
            if (!e.isAlreadyUsedPromotionKey()) {
                throw e;
            }
            // 이 rewardKey로 이미 지급이 시도된 상태 -> 실패로 단정하지 않고 실제 결과를 재조회해 확정한다.
            log.warn("PROMOTION execute reported already-used key; reconciling via execution-result. participationId={}, rewardId={}",
                    participationId, preparation.rewardId());
        }
        promotionExecuteStateService.markExecuted(preparation.rewardId());

        resolveExecutionStatus(participationId, preparation, rewardKey);
    }

    private void resolveExecutionStatus(Long participationId, PromotionPrepareService.PromotionPreparation preparation, String rewardKey) {
        PromotionGatewayExecutionStatus status;
        try {
            status = tossPromotionGateway.getExecutionStatus(
                    preparation.tossUserKey(),
                    preparation.promotionCode(),
                    rewardKey
            );
        } catch (RuntimeException e) {
            // execute는 이미 확인된 상태이니 결과 조회 실패를 FAILED로 단정하지 않고 PENDING으로 남겨 스케줄러 재조회에 맡긴다.
            promotionExecuteStateService.markPending(preparation.rewardId());
            log.warn("PROMOTION execution-result lookup failed after execute; leaving PENDING for scheduler retry. participationId={}, rewardId={}",
                    participationId, preparation.rewardId(), e);
            return;
        }
        promotionExecutionResultApplier.apply(preparation.rewardId(), status);
    }
}
