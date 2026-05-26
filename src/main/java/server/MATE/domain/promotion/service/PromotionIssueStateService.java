package server.MATE.domain.promotion.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import server.MATE.domain.promotion.entity.PromotionReward;
import server.MATE.domain.promotion.repository.PromotionRewardRepository;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

@Service
@RequiredArgsConstructor
public class PromotionIssueStateService {

    private final PromotionRewardRepository promotionRewardRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PromotionReward markKeyIssued(Long rewardId, String promotionCode, String rewardKey) {
        PromotionReward reward = promotionRewardRepository.findById(rewardId)
                .orElseThrow(() -> new BaseException(BaseErrorCode.COMMON_999));
        reward.issueKey(promotionCode, rewardKey);
        return reward;
    }
}
