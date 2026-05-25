package server.MATE.domain.promotion.service;

import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import server.MATE.domain.promotion.entity.PromotionReward;
import server.MATE.domain.promotion.repository.PromotionRewardRepository;
import server.MATE.domain.users.entity.TossAccount;
import server.MATE.domain.users.repository.TossAccountRepository;
import server.MATE.toss.config.TossPromotionProperties;

@Service
@RequiredArgsConstructor
public class PromotionPrepareService {

    static final String LOCAL_ERROR_CODE_NOT_LINKED = "TOSS_ACCOUNT_NOT_LINKED";
    static final String LOCAL_ERROR_CODE_MISSING_PROMOTION = "PROMOTION_CODE_MISSING";

    private final PromotionRewardRepository promotionRewardRepository;
    private final TossAccountRepository tossAccountRepository;
    private final TossPromotionProperties tossPromotionProperties;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PromotionPreparation prepare(Long participationId, Long testId, Long testerId, Integer rewardAmount) {
        if (!tossPromotionProperties.answer().enabled() || rewardAmount == null || rewardAmount <= 0) {
            return PromotionPreparation.skip();
        }

        PromotionReward reward = promotionRewardRepository.findByParticipationId(participationId)
                .orElseGet(() -> promotionRewardRepository.save(PromotionReward.builder()
                        .participationId(participationId)
                        .testId(testId)
                        .testerId(testerId)
                        .rewardAmount(rewardAmount)
                        .build()));

        if (reward.isInFlightOrCompleted()) {
            return PromotionPreparation.skip();
        }

        String promotionCode = tossPromotionProperties.answer().promotionCode();
        if (promotionCode == null || promotionCode.isBlank()) {
            return PromotionPreparation.failure(
                    reward.getId(),
                    LOCAL_ERROR_CODE_MISSING_PROMOTION,
                    "프로모션 코드가 설정되지 않았습니다."
            );
        }

        Optional<TossAccount> tossAccount = tossAccountRepository.findByUserId(testerId)
                .filter(TossAccount::isLinked);
        if (tossAccount.isEmpty()) {
            return PromotionPreparation.failure(
                    reward.getId(),
                    LOCAL_ERROR_CODE_NOT_LINKED,
                    "연결된 토스 계정을 찾을 수 없습니다."
            );
        }

        reward.assignTossUserKey(tossAccount.get().getTossUserKey());
        return PromotionPreparation.ready(reward.getId(), tossAccount.get().getTossUserKey(), promotionCode, rewardAmount);
    }

    public record PromotionPreparation(
            Long rewardId,
            Long tossUserKey,
            String promotionCode,
            Integer rewardAmount,
            String errorCode,
            String errorReason,
            boolean skipped,
            boolean ready
    ) {
        static PromotionPreparation skip() {
            return new PromotionPreparation(null, null, null, null, null, null, true, false);
        }

        static PromotionPreparation ready(Long rewardId, Long tossUserKey, String promotionCode, Integer rewardAmount) {
            return new PromotionPreparation(rewardId, tossUserKey, promotionCode, rewardAmount, null, null, false, true);
        }

        static PromotionPreparation failure(Long rewardId, String errorCode, String errorReason) {
            return new PromotionPreparation(rewardId, null, null, null, errorCode, errorReason, false, false);
        }

        public boolean shouldFail() {
            return !skipped && !ready;
        }
    }
}
