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
public class PromotionExecuteStateService {

    private final PromotionRewardRepository promotionRewardRepository;
    private final Clock clock;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PromotionReward markExecuted(Long rewardId) {
        PromotionReward reward = findReward(rewardId);
        reward.markExecuted(now());
        return reward;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PromotionReward markPending(Long rewardId) {
        PromotionReward reward = findReward(rewardId);
        reward.markPending();
        return reward;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PromotionReward markSucceeded(Long rewardId) {
        PromotionReward reward = findReward(rewardId);
        reward.markSucceeded(now());
        return reward;
    }

    private PromotionReward findReward(Long rewardId) {
        return promotionRewardRepository.findById(rewardId)
                .orElseThrow(() -> new BaseException(BaseErrorCode.COMMON_999));
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
