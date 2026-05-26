package server.MATE.domain.promotion.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import server.MATE.domain.promotion.entity.PromotionReward;
import server.MATE.domain.promotion.entity.PromotionRewardStatus;

public interface PromotionRewardRepository extends JpaRepository<PromotionReward, Long> {

    Optional<PromotionReward> findByParticipationId(Long participationId);

    @Query("""
            select coalesce(sum(pr.rewardAmount), 0)
            from PromotionReward pr
            where pr.testerId = :testerId
              and pr.status = :status
            """)
    Integer sumRewardAmountByTesterIdAndStatus(@Param("testerId") Long testerId,
                                               @Param("status") PromotionRewardStatus status);
}
