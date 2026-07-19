package server.MATE.domain.promotion.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import server.MATE.domain.promotion.entity.PromotionReward;
import server.MATE.domain.promotion.entity.PromotionRewardStatus;
import server.MATE.global.config.ClockConfig;
import server.MATE.global.config.JpaAuditingConfig;
import server.MATE.global.config.QuerydslConfig;

@DataJpaTest
@Import({QuerydslConfig.class, ClockConfig.class, JpaAuditingConfig.class})
class PromotionRewardRepositoryTest {

    @Autowired
    private PromotionRewardRepository promotionRewardRepository;

    @Autowired
    private TestEntityManager em;

    @Test
    @DisplayName("clearTossUserKeyByTesterId: 대상 testerId의 tossUserKey만 null 처리한다")
    void clearsTossUserKeyForMatchingTesterId() {
        PromotionReward target = em.persistAndFlush(PromotionReward.builder()
                .participationId(1L)
                .testId(10L)
                .testerId(100L)
                .tossUserKey(777L)
                .rewardAmount(1000)
                .build());
        PromotionReward other = em.persistAndFlush(PromotionReward.builder()
                .participationId(2L)
                .testId(10L)
                .testerId(200L)
                .tossUserKey(888L)
                .rewardAmount(1000)
                .build());
        em.clear();

        int updated = promotionRewardRepository.clearTossUserKeyByTesterId(100L);

        assertThat(updated).isEqualTo(1);
        assertThat(promotionRewardRepository.findById(target.getId()).orElseThrow().getTossUserKey()).isNull();
        assertThat(promotionRewardRepository.findById(other.getId()).orElseThrow().getTossUserKey()).isEqualTo(888L);
    }

    @Test
    @DisplayName("clearTossUserKeyByTesterId: 진행중 상태(KEY_ISSUED/PENDING/EXECUTED)의 tossUserKey는 유지한다")
    void keepsTossUserKeyForInFlightStatuses() {
        PromotionReward keyIssued = em.persistAndFlush(PromotionReward.builder()
                .participationId(11L)
                .testId(10L)
                .testerId(300L)
                .tossUserKey(111L)
                .rewardAmount(1000)
                .status(PromotionRewardStatus.KEY_ISSUED)
                .build());
        PromotionReward pending = em.persistAndFlush(PromotionReward.builder()
                .participationId(12L)
                .testId(10L)
                .testerId(300L)
                .tossUserKey(222L)
                .rewardAmount(1000)
                .status(PromotionRewardStatus.PENDING)
                .build());
        PromotionReward executed = em.persistAndFlush(PromotionReward.builder()
                .participationId(13L)
                .testId(10L)
                .testerId(300L)
                .tossUserKey(333L)
                .rewardAmount(1000)
                .status(PromotionRewardStatus.EXECUTED)
                .build());
        PromotionReward succeeded = em.persistAndFlush(PromotionReward.builder()
                .participationId(14L)
                .testId(10L)
                .testerId(300L)
                .tossUserKey(444L)
                .rewardAmount(1000)
                .status(PromotionRewardStatus.SUCCEEDED)
                .build());
        em.clear();

        int updated = promotionRewardRepository.clearTossUserKeyByTesterId(300L);

        assertThat(updated).isEqualTo(1);
        assertThat(promotionRewardRepository.findById(keyIssued.getId()).orElseThrow().getTossUserKey()).isEqualTo(111L);
        assertThat(promotionRewardRepository.findById(pending.getId()).orElseThrow().getTossUserKey()).isEqualTo(222L);
        assertThat(promotionRewardRepository.findById(executed.getId()).orElseThrow().getTossUserKey()).isEqualTo(333L);
        assertThat(promotionRewardRepository.findById(succeeded.getId()).orElseThrow().getTossUserKey()).isNull();
    }
}
