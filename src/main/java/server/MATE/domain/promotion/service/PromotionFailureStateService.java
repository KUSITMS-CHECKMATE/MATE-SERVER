package server.MATE.domain.promotion.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import server.MATE.domain.promotion.entity.PromotionReward;
import server.MATE.domain.promotion.repository.PromotionRewardRepository;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PromotionFailureStateService {

    private final PromotionRewardRepository promotionRewardRepository;
    private final Clock clock;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PromotionReward markFailed(Long rewardId, String errorCode, String errorReason) {
        PromotionReward reward = promotionRewardRepository.findById(rewardId)
                .orElseThrow(() -> new BaseException(BaseErrorCode.COMMON_999));
        reward.markFailed(errorCode, errorReason, LocalDateTime.now(clock));
        return reward;
    }
}
