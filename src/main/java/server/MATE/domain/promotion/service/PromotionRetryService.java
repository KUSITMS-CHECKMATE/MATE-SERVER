package server.MATE.domain.promotion.service;

import java.util.Set;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import server.MATE.domain.promotion.dto.response.PromotionRetryResponse;
import server.MATE.domain.promotion.entity.PromotionReward;
import server.MATE.domain.promotion.entity.PromotionRewardStatus;
import server.MATE.domain.promotion.repository.PromotionRewardRepository;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;
import server.MATE.toss.exception.TossErrorCode;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromotionRetryService {

    // 토스가 지급을 실행하지 않은 것이 확실한 실패만 재지급한다.
    // 그 외(예: execute 타임아웃)는 토스 쪽에서 이미 지급됐을 수 있어 새 키로 재지급하면 중복 지급된다.
    static final Set<String> RETRYABLE_ERROR_CODES = Set.of(
            TossErrorCode.TOSS_011.getCode(),
            TossErrorCode.TOSS_012.getCode(),
            PromotionPrepareService.LOCAL_ERROR_CODE_NOT_LINKED,
            PromotionPrepareService.LOCAL_ERROR_CODE_MISSING_PROMOTION
    );

    private final PromotionRewardRepository promotionRewardRepository;
    private final PromotionService promotionService;

    // grant 내부 단계가 각자 REQUIRES_NEW로 커밋되므로 이 메서드는 트랜잭션 없이 호출한다.
    public PromotionRetryResponse retry(Long rewardId) {
        PromotionReward reward = promotionRewardRepository.findById(rewardId)
                .orElseThrow(() -> new BaseException(BaseErrorCode.PROMOTION_001));

        if (reward.getStatus() != PromotionRewardStatus.FAILED) {
            throw new BaseException(BaseErrorCode.PROMOTION_002);
        }
        if (reward.getLastErrorCode() == null || !RETRYABLE_ERROR_CODES.contains(reward.getLastErrorCode())) {
            throw new BaseException(BaseErrorCode.PROMOTION_003);
        }
        // 위 검사 이후 다른 요청이 먼저 재지급했을 수 있어, 상태와 사유를 update 조건으로 다시 확인한다.
        if (promotionRewardRepository.resetFailedToReady(rewardId, RETRYABLE_ERROR_CODES) == 0) {
            throw new BaseException(BaseErrorCode.PROMOTION_002);
        }

        // 재지급 시 rewardKey/promotionCode가 덮어써지므로 이전 시도를 추적할 수 있게 남긴다.
        log.info("PROMOTION retry requested. rewardId={}, participationId={}, previousErrorCode={}, previousPromotionCode={}, previousRewardKey={}",
                rewardId, reward.getParticipationId(), reward.getLastErrorCode(),
                reward.getPromotionCode(), reward.getRewardKey());
        promotionService.grant(
                reward.getParticipationId(),
                reward.getTestId(),
                reward.getTesterId(),
                reward.getRewardAmount()
        );

        PromotionReward result = promotionRewardRepository.findById(rewardId)
                .orElseThrow(() -> new BaseException(BaseErrorCode.PROMOTION_001));
        log.info("PROMOTION retry finished. rewardId={}, status={}, errorCode={}",
                rewardId, result.getStatus(), result.getLastErrorCode());
        return PromotionRetryResponse.from(result);
    }
}
