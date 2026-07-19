package server.MATE.domain.promotion.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import server.MATE.domain.promotion.entity.PromotionReward;
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
}
